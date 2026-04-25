package com.example.shivkupacattlefeed;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UdharRegisterActivity extends AppCompatActivity {

    private TextView tvTotalUdhar;
    private RecyclerView rvUdharList;
    private EditText etSearchFarmer;
    private DatabaseReference mDatabase;
    private String dealerId;
    private List<FarmerUdhar> farmerUdharList = new ArrayList<>();
    private List<FarmerUdhar> filteredList = new ArrayList<>();
    private UdharAdapter adapter;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udhar_register);

        tvTotalUdhar = findViewById(R.id.tvTotalUdhar);
        rvUdharList = findViewById(R.id.rvUdharList);
        etSearchFarmer = findViewById(R.id.etSearchFarmer);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        dealerId = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("orders");

        rvUdharList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UdharAdapter(filteredList);
        rvUdharList.setAdapter(adapter);

        setupSearch();
        setupBottomNavigation();
        fetchUdharData();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_udhar);
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_udhar) {
                return true;
            } else if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_hishob) {
                startActivity(new Intent(this, HishobActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                finish();
                return true;
            } else if (id == R.id.nav_daily_report) {
                startActivity(new Intent(this, ReceivedOrdersActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                finish();
                return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupSearch() {
        etSearchFarmer.addTextChangedListener(new TextWatcher() {
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
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(farmerUdharList);
        } else {
            String query = text.toLowerCase();
            for (FarmerUdhar item : farmerUdharList) {
                if (item.name.toLowerCase().contains(query) || item.mobile.contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private long parseLong(String s) {
        try {
            if (s == null || s.isEmpty()) return 0;
            return Long.parseLong(s.replace("₹", "").replace(",", "").replace("-", "").trim());
        } catch (Exception e) { return 0; }
    }

    private void fetchUdharData() {
        // पुन्हा 'Real-time' (addValueEventListener) सेट केले आहे, 
        // जेणेकरून पेमेंट केल्यावर किंवा विक्री केल्यावर लिस्ट लगेच अपडेट होईल.
        mDatabase.orderByChild("dealerId").equalTo(dealerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, FarmerUdhar> udharMap = new HashMap<>();
                long grandTotalUdhar = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Order order = data.getValue(Order.class);
                    if (order != null) {
                        // balanceAmount मध्ये विक्रीसाठी पॉझिटिव्ह आणि पेमेंटसाठी नेगेटिव्ह व्हॅल्यू असते
                        // आपण ती थेट बेरीज करत आहोत जेणेकरून लेजर नीट मॅन्टेन होईल
                        long balance = 0;
                        try {
                            String balStr = order.balanceAmount != null ? order.balanceAmount : "0";
                            balStr = balStr.replace("₹", "").replace(",", "").trim();
                            if (!balStr.isEmpty()) {
                                balance = Long.parseLong(balStr);
                            }
                        } catch (Exception e) {
                            balance = 0;
                        }

                        String mobile = (order.farmerMobile != null && !order.farmerMobile.isEmpty()) ? order.farmerMobile : "NoMobile";
                        String name = (order.farmerName != null && !order.farmerName.isEmpty()) ? order.farmerName : "Unknown";
                        String key = name + "_" + mobile;
                        
                        FarmerUdhar fu = udharMap.get(key);
                        if (fu == null) {
                            fu = new FarmerUdhar(name, (mobile.equals("NoMobile") ? "" : mobile));
                            udharMap.put(key, fu);
                        }
                        
                        fu.totalBalance += balance;
                    }
                }

                farmerUdharList.clear();
                for (FarmerUdhar fu : udharMap.values()) {
                    if (fu.totalBalance != 0) {
                        farmerUdharList.add(fu);
                        if (fu.totalBalance > 0) {
                            grandTotalUdhar += fu.totalBalance;
                        }
                    }
                }

                // उधारीनुसार क्रम लावा (मोठी उधारी सर्वात वर)
                Collections.sort(farmerUdharList, (o1, o2) -> Long.compare(o2.totalBalance, o1.totalBalance));
                
                tvTotalUdhar.setText("₹ " + grandTotalUdhar);
                filter(etSearchFarmer.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static class FarmerUdhar {
        String name, mobile;
        long totalBalance;

        FarmerUdhar(String name, String mobile) {
            this.name = name;
            this.mobile = mobile;
            this.totalBalance = 0;
        }
    }

    private class UdharAdapter extends RecyclerView.Adapter<UdharAdapter.ViewHolder> {
        private List<FarmerUdhar> list;

        UdharAdapter(List<FarmerUdhar> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_udhar, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FarmerUdhar fu = list.get(position);
            holder.tvName.setText(fu.name);
            holder.tvMobile.setText((fu.mobile == null || fu.mobile.isEmpty()) ? "मोबाईल नाही" : fu.mobile);
            
            if (fu.totalBalance > 0) {
                holder.tvBalance.setText("बाकी: ₹ " + fu.totalBalance);
                holder.tvBalance.setTextColor(android.graphics.Color.RED);
            } else if (fu.totalBalance < 0) {
                holder.tvBalance.setText("जमा: ₹ " + Math.abs(fu.totalBalance));
                holder.tvBalance.setTextColor(android.graphics.Color.parseColor("#2E7D32")); // हिरवा रंग
            } else {
                holder.tvBalance.setText("हिशोब नील (0)");
                holder.tvBalance.setTextColor(android.graphics.Color.GRAY);
            }

            holder.btnCall.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + fu.mobile));
                startActivity(intent);
            });

            holder.btnPay.setOnClickListener(v -> showPaymentDialog(fu));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvMobile, tvBalance;
            Button btnCall, btnPay;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvFarmerName);
                tvMobile = v.findViewById(R.id.tvFarmerMobile);
                tvBalance = v.findViewById(R.id.tvTotalBalance);
                btnCall = v.findViewById(R.id.btnCallFarmer);
                btnPay = v.findViewById(R.id.btnPayUdhar);
            }
        }
    }

    private void showPaymentDialog(FarmerUdhar fu) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_pay_udhar, null);
        EditText etAmount = view.findViewById(R.id.etPayAmount);
        EditText etRemark = view.findViewById(R.id.etPayRemark);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        tvTitle.setText(fu.name + " कडून पैसे जमा करा");

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("जमा करा", (dialog, which) -> {
                    String amtStr = etAmount.getText().toString();
                    String remarkStr = etRemark.getText().toString();
                    if (!amtStr.isEmpty()) {
                        savePayment(fu, amtStr, remarkStr);
                    }
                })
                .setNegativeButton("रद्द करा", null)
                .show();
    }

    private void savePayment(FarmerUdhar fu, String amount, String remark) {
        String paymentId = mDatabase.push().getKey();
        // farmerId म्हणून शक्य असल्यास मोबाईल वापरावा जेणेकरून लिंकिंग सोपे होईल
        String fId = (fu.mobile != null && !fu.mobile.isEmpty()) ? fu.mobile : fu.name;
        
        Order payment = new Order(paymentId, "PAYMENT", "पैसे जमा (Payment)", 
                fId, fu.name, fu.mobile, dealerId, "0", "0",
                amount, "-" + amount, "Paid", "Udhar Repayment", "", 
                remark, System.currentTimeMillis());

        if (paymentId != null) {
            mDatabase.child(paymentId).setValue(payment).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "पैसे यशस्वीपणे जमा झाले!", Toast.LENGTH_SHORT).show();
                    
                    // Optional: Send WhatsApp confirmation for payment
                    String msg = "नमस्ते " + fu.name + ",\n" +
                            "तुमचे ₹ " + amount + " यशस्वीपणे जमा झाले आहेत.\n" +
                            (remark.isEmpty() ? "" : "शेरा: " + remark + "\n") +
                            "धन्यवाद! - शिवकृपा कॅटल फीड";
                    sendWhatsApp(fu.mobile, msg);
                }
            });
        }
    }

    private void sendWhatsApp(String mobile, String message) {
        if (mobile == null || mobile.isEmpty()) return;
        try {
            if (!mobile.startsWith("91") && mobile.length() == 10) mobile = "91" + mobile;
            Intent i = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=" + mobile + "&text=" + java.net.URLEncoder.encode(message, "UTF-8");
            i.setData(Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            // WhatsApp not installed or error
        }
    }
}
