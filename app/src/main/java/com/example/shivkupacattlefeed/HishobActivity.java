package com.example.shivkupacattlefeed;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;

public class HishobActivity extends AppCompatActivity {

    private TextView tvHishobTitle;
    private LinearLayout llHishobDetails;
    private MaterialCardView cardDailySales, cardStockReport, cardUdharReport;
    private DatabaseReference mDatabaseOrders, mDatabaseProducts;
    private String dealerId;
    private ImageButton btnBack;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hishob);

        tvHishobTitle = findViewById(R.id.tvHishobTitle);
        llHishobDetails = findViewById(R.id.llHishobDetails);
        cardDailySales = findViewById(R.id.cardDailySalesReport);
        cardStockReport = findViewById(R.id.cardStockReport);
        cardUdharReport = findViewById(R.id.cardUdharReport);
        btnBack = findViewById(R.id.btnBack);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        btnBack.setOnClickListener(v -> finish());
        setupBottomNavigation();

        dealerId = FirebaseAuth.getInstance().getUid();
        mDatabaseOrders = FirebaseDatabase.getInstance().getReference("orders");
        mDatabaseProducts = FirebaseDatabase.getInstance().getReference("products");

        cardDailySales.setOnClickListener(v -> showDatePickerAndFetchReport());
        cardStockReport.setOnClickListener(v -> fetchStockReport());
        cardUdharReport.setOnClickListener(v -> fetchUdharReport());
        
        fetchDailyReport(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_hishob);
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_hishob) return true;
            if (id == R.id.nav_home) { finish(); return true; }
            
            Intent intent = null;
            if (id == R.id.nav_reports) intent = new Intent(this, ReportsActivity.class);
            else if (id == R.id.nav_daily_report) intent = new Intent(this, ReceivedOrdersActivity.class);
            else if (id == R.id.nav_udhar) intent = new Intent(this, UdharRegisterActivity.class);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
    }

    private void showDatePickerAndFetchReport() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(year, month, dayOfMonth);
            String selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(cal.getTime());
            fetchDailyReport(selectedDate);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private double parseAmount(String s) {
        try {
            if (s == null || s.isEmpty()) return 0;
            return Double.parseDouble(s.replace("₹", "").replace(",", "").replace("-", "").trim());
        } catch (Exception e) { return 0; }
    }

    private void fetchDailyReport(String date) {
        tvHishobTitle.setVisibility(View.VISIBLE);
        tvHishobTitle.setText("विक्री रिपोर्ट: " + date);
        llHishobDetails.removeAllViews();

        mDatabaseOrders.orderByChild("dealerId").equalTo(dealerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalSale = 0, totalCash = 0;
                Map<String, Double> items = new HashMap<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Order o = data.getValue(Order.class);
                    if (o != null) {
                        String oDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date(o.timestamp));
                        if (date.equals(oDate)) {
                            boolean isPayment = "पैसे जमा (Payment)".equals(o.productName);
                            if (!isPayment) {
                                totalSale += (long) parseAmount(o.totalPrice);
                            }
                            totalCash += (long) parseAmount(o.paidAmount);
                            
                            if (!isPayment) {
                                String name = o.productName;
                                items.put(name, items.getOrDefault(name, 0.0) + 1.0); // count of orders for item
                            }
                        }
                    }
                }
                
                addDetailHeader("एकूण विक्री: ₹ " + totalSale);
                addDetailHeader("एकूण जमा: ₹ " + totalCash);

                if (items.isEmpty()) {
                    addDetailRow("आज कोणतीही विक्री झाली नाही", "");
                } else {
                    for (String name : items.keySet()) {
                        addDetailRow(name, "व्यवहार संख्या: " + (long)items.get(name).doubleValue());
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchStockReport() {
        tvHishobTitle.setVisibility(View.VISIBLE);
        tvHishobTitle.setText("स्टॉक स्थिती (Stock Status)");
        llHishobDetails.removeAllViews();

        mDatabaseProducts.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product p = data.getValue(Product.class);
                    if (p != null && dealerId.equals(p.dealerId)) {
                        String weightStr = (p.weightPerUnit != null && !p.weightPerUnit.isEmpty()) ? " [" + p.weightPerUnit + " kg]" : "";
                        String status = "शिल्लक: " + p.quantity + " " + p.unit;
                        addDetailRow(p.brand + " " + p.category + weightStr, status);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchUdharReport() {
        tvHishobTitle.setVisibility(View.VISIBLE);
        tvHishobTitle.setText("उधारी रिपोर्ट (Udhar List)");
        llHishobDetails.removeAllViews();

        mDatabaseOrders.orderByChild("dealerId").equalTo(dealerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Double> udharMap = new HashMap<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Order o = data.getValue(Order.class);
                    if (o != null) {
                        double balance = parseAmount(o.balanceAmount);
                        
                        String mobile = (o.farmerMobile != null && !o.farmerMobile.isEmpty()) ? o.farmerMobile : "NoMobile";
                        String key = (o.farmerName != null ? o.farmerName : "Unknown") + "_" + mobile;
                        
                        udharMap.put(key, udharMap.getOrDefault(key, 0.0) + balance);
                    }
                }
                
                boolean hasUdhar = false;
                for (String key : udharMap.keySet()) {
                    double totalBal = udharMap.get(key);
                    if (totalBal != 0) {
                        String name = key.split("_")[0];
                        String status = totalBal > 0 ? "बाकी: ₹ " + (long)totalBal : "जमा: ₹ " + (long)Math.abs(totalBal);
                        addDetailRow(name, status);
                        hasUdhar = true;
                    }
                }
                if (!hasUdhar) addDetailRow("कोणाचीही उधारी बाकी नाही", "");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addDetailHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setPadding(16, 16, 16, 16);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(getResources().getColor(R.color.primary));
        llHishobDetails.addView(tv);
    }

    private void addDetailRow(String title, String sub) {
        View view = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, llHishobDetails, false);
        TextView t1 = view.findViewById(android.R.id.text1);
        TextView t2 = view.findViewById(android.R.id.text2);
        t1.setText(title);
        t1.setTextSize(16);
        t2.setText(sub);
        t2.setTextColor(getResources().getColor(R.color.primary));
        llHishobDetails.addView(view);
    }
}