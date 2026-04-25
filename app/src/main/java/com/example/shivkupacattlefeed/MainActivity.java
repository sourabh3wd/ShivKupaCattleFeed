package com.example.shivkupacattlefeed;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvProducts;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private DatabaseReference mDatabase;
    private String userRole = "";
    private String dealerId = "";
    private TextView tvSummarySale, tvSummaryUdhar;
    private EditText etSearch;
    private MaterialButton btnSellProduct;
    private BottomNavigationView bottomNavigation;

    public static boolean isPinVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isPinVerified) {
            startActivity(new Intent(this, PinActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        rvProducts = findViewById(R.id.rvProducts);
        tvSummarySale = findViewById(R.id.tvSummarySale);
        tvSummaryUdhar = findViewById(R.id.tvSummaryUdhar);
        etSearch = findViewById(R.id.etSearch);
        btnSellProduct = findViewById(R.id.btnSellProduct);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductAdapter(productList, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onSellClick(Product product) {
                Intent intent = new Intent(MainActivity.this, SellProductActivity.class);
                intent.putExtra("PRODUCT_ID", product.id);
                startActivity(intent);
            }

            @Override
            public void onRestockClick(Product product) {
                Intent intent = new Intent(MainActivity.this, AddProductActivity.class);
                intent.putExtra("productId", product.id);
                intent.putExtra("category", product.category);
                intent.putExtra("brand", product.brand);
                intent.putExtra("variant", product.variant);
                intent.putExtra("price", product.price);
                intent.putExtra("quantity", product.quantity);
                intent.putExtra("unit", product.unit);
                intent.putExtra("description", product.description);
                intent.putExtra("weightPerUnit", product.weightPerUnit);
                startActivity(intent);
            }
        });
        rvProducts.setAdapter(productAdapter);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        dealerId = FirebaseAuth.getInstance().getUid();

        checkUserRole();
        setupSearch();
        setupBottomNavigation();

        btnSellProduct.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SellProductActivity.class));
        });

        findViewById(R.id.btnAddProduct).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddProductActivity.class));
        });

        findViewById(R.id.btnProductMaster).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProductMasterActivity.class));
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            
            Intent intent = null;
            if (id == R.id.nav_hishob) intent = new Intent(this, HishobActivity.class);
            else if (id == R.id.nav_daily_report) intent = new Intent(this, ReceivedOrdersActivity.class);
            else if (id == R.id.nav_udhar) intent = new Intent(this, UdharRegisterActivity.class);
            else if (id == R.id.nav_reports) intent = new Intent(this, ReportsActivity.class);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        updateSummary();
    }

    private void checkUserRole() {
        mDatabase.child("users").child(dealerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userRole = snapshot.child("role").getValue(String.class);
                    fetchProducts();
                    updateSummary();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchProducts() {
        mDatabase.child("products").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                List<Product> lowStock = new ArrayList<>(), normalStock = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product p = data.getValue(Product.class);
                    if (p != null && dealerId.equals(p.dealerId)) {
                        try {
                            if (Double.parseDouble(p.quantity) <= 5) lowStock.add(p);
                            else normalStock.add(p);
                        } catch (Exception e) { normalStock.add(p); }
                    }
                }
                productList.addAll(lowStock);
                productList.addAll(normalStock);
                productAdapter.updateList(productList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateSummary() {
        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        mDatabase.child("orders").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalTodaySale = 0;
                Map<String, Double> farmerBalances = new HashMap<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Order o = data.getValue(Order.class);
                    if (o != null && dealerId.equals(o.dealerId)) {
                        double price = 0, bal = 0;
                        try {
                            price = Double.parseDouble(o.totalPrice != null ? o.totalPrice : "0");
                            bal = Double.parseDouble(o.balanceAmount != null ? o.balanceAmount : "0");
                        } catch (Exception e) {}

                        String oDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date(o.timestamp));
                        boolean isPayment = "पैसे जमा (Payment)".equals(o.productName);
                        
                        if (today.equals(oDate) && !isPayment) totalTodaySale += price;

                        String mobile = (o.farmerMobile != null && !o.farmerMobile.isEmpty()) ? o.farmerMobile : "NoMobile";
                        String key = (o.farmerName != null ? o.farmerName : "Unknown") + "_" + mobile;
                        
                        Double currentBal = farmerBalances.get(key);
                        farmerBalances.put(key, (currentBal == null ? 0 : currentBal) + bal);
                    }
                }

                long grandTotalUdhar = 0;
                for (Double fBal : farmerBalances.values()) if (fBal > 0) grandTotalUdhar += fBal;

                tvSummarySale.setText("₹ " + (long)totalTodaySale);
                tvSummaryUdhar.setText("एकूण उधारी: ₹ " + grandTotalUdhar);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String text) {
        List<Product> filteredList = new ArrayList<>();
        for (Product item : productList) {
            if (item.brand.toLowerCase().contains(text.toLowerCase()) || 
                item.category.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        productAdapter.updateList(filteredList);
    }
}
