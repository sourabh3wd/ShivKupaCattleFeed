package com.example.shivkupacattlefeed;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UdharRegisterActivity extends AppCompatActivity {

    private TextView tvTotalUdhar;
    private RecyclerView rvUdharList;
    private DatabaseReference mDatabase;
    private String dealerId;
    private List<FarmerUdhar> farmerUdharList = new ArrayList<>();
    private UdharAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udhar_register);

        tvTotalUdhar = findViewById(R.id.tvTotalUdhar);
        rvUdharList = findViewById(R.id.rvUdharList);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        dealerId = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("orders");

        rvUdharList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UdharAdapter(farmerUdharList);
        rvUdharList.setAdapter(adapter);

        fetchUdharData();
    }

    private void fetchUdharData() {
        mDatabase.orderByChild("dealerId").equalTo(dealerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, FarmerUdhar> udharMap = new HashMap<>();
                long grandTotalUdhar = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Order order = data.getValue(Order.class);
                    if (order != null) {
                        double balance = 0;
                        try {
                            String balStr = order.balanceAmount != null ? order.balanceAmount : "0";
                            balStr = balStr.replace("₹", "").replace(",", "").trim();
                            if (!balStr.isEmpty()) {
                                balance = Double.parseDouble(balStr);
                            }
                        } catch (Exception e) {
                            balance = 0;
                        }

                        // ग्रुप करण्यासाठी मोबाईल किंवा नाव वापरा
                        String key = (order.farmerMobile != null && !order.farmerMobile.isEmpty()) ? order.farmerMobile : (order.farmerName != null ? order.farmerName : "Unknown");
                        
                        FarmerUdhar fu = udharMap.get(key);
                        if (fu == null) {
                            fu = new FarmerUdhar(order.farmerName != null ? order.farmerName : "अज्ञात ग्राहक", order.farmerMobile != null ? order.farmerMobile : "");
                            udharMap.put(key, fu);
                        }
                        
                        // सर्व व्यवहारांची बेरीज करा (खरेदी + आणि पैसे जमा -)
                        fu.totalBalance += (long) balance;
                    }
                }

                farmerUdharList.clear();
                for (FarmerUdhar fu : udharMap.values()) {
                    // जर उधारी असेल किंवा पैसे शिल्लक (Advance) असतील तर लिस्टमध्ये दाखवा
                    if (fu.totalBalance != 0) {
                        farmerUdharList.add(fu);
                        if (fu.totalBalance > 0) {
                            grandTotalUdhar += fu.totalBalance;
                        }
                    }
                }
                
                tvTotalUdhar.setText("₹ " + grandTotalUdhar);
                adapter.notifyDataSetChanged();
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
            holder.tvMobile.setText(fu.mobile);
            holder.tvBalance.setText("₹ " + fu.totalBalance);

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
        // Repayment is an entry with negative balanceAmount to reduce the total udhar
        Order payment = new Order(paymentId, "PAYMENT", "पैसे जमा (Payment)", 
                fu.name, fu.name, fu.mobile, dealerId, "0", "0",
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
