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
    private TextView tvSummarySale, tvSummaryUdhar, tvSummaryTodayUdhar;
    private EditText etSearch;
    private MaterialButton btnSellProduct;
    private BottomNavigationView bottomNavigation;

    public static boolean isPinVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // जर पिन व्हेरिफाय झाला नसेल, तर पिन स्क्रीनवर पाठवा
        if (!isPinVerified) {
            Intent intent = new Intent(this, PinActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        rvProducts = findViewById(R.id.rvProducts);
        tvSummarySale = findViewById(R.id.tvSummarySale);
        tvSummaryTodayUdhar = findViewById(R.id.tvSummaryTodayUdhar);
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
            Intent intent = new Intent(MainActivity.this, SellProductActivity.class);
            startActivity(intent);
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
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_hishob) {
                Intent intent = new Intent(MainActivity.this, HishobActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_reports) {
                Intent intent = new Intent(MainActivity.this, ReportsActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_stock) {
                Intent intent = new Intent(MainActivity.this, AddProductActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_udhar) {
                Intent intent = new Intent(MainActivity.this, UdharRegisterActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void checkUserRole() {
        mDatabase.child("users").child(dealerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userRole = snapshot.child("role").getValue(String.class);
                    fetchProducts();
                    updateSummary();
                    checkLowStock();
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
                List<Product> lowStockList = new ArrayList<>();
                List<Product> normalStockList = new ArrayList<>();
                
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product p = data.getValue(Product.class);
                    if (p != null && dealerId.equals(p.dealerId)) {
                        try {
                            double qty = Double.parseDouble(p.quantity);
                            if (qty <= 5) {
                                lowStockList.add(p);
                            } else {
                                normalStockList.add(p);
                            }
                        } catch (Exception e) {
                            normalStockList.add(p);
                        }
                    }
                }
                // कमी स्टॉक असलेला माल सर्वात वर दिसेल
                productList.addAll(lowStockList);
                productList.addAll(normalStockList);
                productAdapter.updateList(productList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateSummary() {
        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        mDatabase.child("orders").orderByChild("dealerId").equalTo(dealerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalTodaySale = 0;
                double totalTodayUdhar = 0;
                Map<String, Double> farmerBalances = new HashMap<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Order o = data.getValue(Order.class);
                    if (o != null) {
                        double price = 0;
                        double paid = 0;
                        double bal = 0;
                        
                        try {
                            price = Double.parseDouble(o.totalPrice != null ? o.totalPrice.replace("₹", "").replace(",", "").trim() : "0");
                            paid = Double.parseDouble(o.paidAmount != null ? o.paidAmount.replace("₹", "").replace(",", "").trim() : "0");
                            bal = Double.parseDouble(o.balanceAmount != null ? o.balanceAmount.replace("₹", "").replace(",", "").trim() : "0");
                        } catch (Exception e) {}

                        String oDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date(o.timestamp));
                        
                        if (today.equals(oDate)) {
                            // आजची विक्री = आज विकलेल्या मालाची एकूण किंमत
                            totalTodaySale += price;
                            // आजची उधारी = आजच्या सर्व व्यवहारांची बेरीज (विक्री आणि जमा)
                            totalTodayUdhar += bal;
                        }

                        // सर्वकालीन उधारीसाठी ग्रुपिंग
                        String key = (o.farmerMobile != null && !o.farmerMobile.isEmpty()) ? o.farmerMobile : (o.farmerName != null ? o.farmerName : "Unknown");
                        Double currentBal = farmerBalances.get(key);
                        if (currentBal == null) currentBal = 0.0;
                        farmerBalances.put(key, currentBal + bal);
                    }
                }

                long grandTotalUdhar = 0;
                for (Double fBal : farmerBalances.values()) {
                    if (fBal > 0) {
                        grandTotalUdhar += fBal;
                    }
                }

                tvSummarySale.setText("₹ " + (long)totalTodaySale);
                // आजची उधारी ० पेक्षा कमी दाखवू नका (जर जमा जास्त असेल तरी ० दाखवा)
                tvSummaryTodayUdhar.setText("₹ " + (long)Math.max(0, totalTodayUdhar));
                tvSummaryUdhar.setText("एकूण उधारी: ₹ " + grandTotalUdhar);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkLowStock() {
        mDatabase.child("products").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> lowStockItems = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product p = data.getValue(Product.class);
                    if (p != null && dealerId.equals(p.dealerId)) {
                        int qty = (int) Double.parseDouble(p.quantity);
                        if (qty <= 5) {
                            lowStockItems.add(p.brand + " (" + qty + " शिल्लक)");
                        }
                    }
                }
                if (!lowStockItems.isEmpty()) {
                    showLowStockDialog(lowStockItems);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showLowStockDialog(List<String> items) {
        StringBuilder sb = new StringBuilder("खालील माल संपत आला आहे:\n\n");
        for (String item : items) sb.append("• ").append(item).append("\n");
        
        new AlertDialog.Builder(this)
                .setTitle("स्टॉक अलर्ट (Low Stock)")
                .setMessage(sb.toString())
                .setPositiveButton("ठीक आहे", null)
                .show();
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
