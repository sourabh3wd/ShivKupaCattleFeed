package com.example.shivkupacattlefeed;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReceivedOrdersActivity extends AppCompatActivity {

    private RecyclerView rvReceivedOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;
    private DatabaseReference mDatabase;
    private String dealerId;
    private TextView tvSelectedDateDisplay, tvTotalDaySale, tvTotalDayCash;
    private String selectedDate;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_received_orders);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        tvSelectedDateDisplay = findViewById(R.id.tvSelectedDateDisplay);
        tvTotalDaySale = findViewById(R.id.tvTotalDaySale);
        tvTotalDayCash = findViewById(R.id.tvTotalDayCash);
        rvReceivedOrders = findViewById(R.id.rvReceivedOrders);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        
        rvReceivedOrders.setLayoutManager(new LinearLayoutManager(this));
        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList);
        rvReceivedOrders.setAdapter(orderAdapter);

        dealerId = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // बाय डिफॉल्ट आजची तारीख सेट करा
        selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        tvSelectedDateDisplay.setText("तारीख: " + selectedDate.replace("-", "/"));

        findViewById(R.id.btnSelectDate).setOnClickListener(v -> showDatePicker());

        setupBottomNavigation();
        fetchOrders();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_daily_report);
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_daily_report) {
                return true;
            } else if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_hishob) {
                startActivity(new android.content.Intent(this, HishobActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION));
                finish();
                return true;
            } else if (id == R.id.nav_udhar) {
                startActivity(new android.content.Intent(this, UdharRegisterActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION));
                finish();
                return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new android.content.Intent(this, ReportsActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION));
                finish();
                return true;
            }
            return false;
        });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(sel.getTime());
            tvSelectedDateDisplay.setText("तारीख: " + selectedDate.replace("-", "/"));
            fetchOrders();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private long parseLong(String s) {
        try {
            if (s == null || s.isEmpty()) return 0;
            return Long.parseLong(s.replace("₹", "").replace(",", "").replace("-", "").trim());
        } catch (Exception e) { return 0; }
    }

    private void fetchOrders() {
        mDatabase.child("orders").orderByChild("dealerId").equalTo(dealerId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                long totalSale = 0;
                long totalCash = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Order order = data.getValue(Order.class);
                    if (order != null) {
                        String oDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date(order.timestamp));
                        if (selectedDate.equals(oDate)) {
                            orderList.add(order);
                            try {
                                boolean isPayment = "पैसे जमा (Payment)".equals(order.productName);
                                if (!isPayment) {
                                    totalSale += parseLong(order.totalPrice);
                                }
                                totalCash += parseLong(order.paidAmount);
                            } catch (Exception e) {}
                        }
                    }
                }
                
                orderList.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

                tvTotalDaySale.setText("₹ " + totalSale);
                tvTotalDayCash.setText("₹ " + totalCash);
                orderAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
        private List<Order> orders;

        public OrderAdapter(List<Order> orders) {
            this.orders = orders;
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Order order = orders.get(position);
            
            holder.tvFarmerName.setText(order.farmerName != null ? order.farmerName : "अनोळखी ग्राहक");
            holder.tvProductName.setText(order.productName);
            
            if ("पैसे जमा (Payment)".equals(order.productName)) {
                // मायनस चिन्ह काढून रक्कम दाखवणे
                String amt = order.paidAmount != null ? order.paidAmount.replace("-", "") : "0";
                holder.tvOrderDetails.setText("जमा रक्कम: ₹ " + amt + (order.remark != null && !order.remark.isEmpty() ? "\nशेरा: " + order.remark : ""));
                holder.tvStatus.setText("उधारी जमा");
                holder.tvStatus.setBackgroundResource(R.drawable.status_bg); // हिरवा बॅकग्राउंड
            } else {
                String details = (order.productId.equals("MULTIPLE") ? "अनेक माल" : "नग: " + (order.quantity != null ? order.quantity : "0")) + " | एकूण: ₹ " + order.totalPrice;
                if (order.remark != null && !order.remark.isEmpty()) {
                    details += "\nशेरा: " + order.remark;
                }
                holder.tvOrderDetails.setText(details);

                long bal = parseLong(order.balanceAmount);
                
                if (bal > 0) {
                    holder.tvStatus.setText("उधार: ₹ " + bal);
                    holder.tvStatus.setBackgroundResource(R.drawable.status_bg_red);
                } else {
                    holder.tvStatus.setText("पूर्ण जमा");
                    holder.tvStatus.setBackgroundResource(R.drawable.status_bg);
                }
            }

            holder.btnDeliver.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvFarmerName, tvProductName, tvOrderDetails, tvStatus;
            View btnDeliver;

            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFarmerName = itemView.findViewById(R.id.tvOrderFarmerName);
                tvProductName = itemView.findViewById(R.id.tvOrderProductName);
                tvOrderDetails = itemView.findViewById(R.id.tvOrderDetails);
                tvStatus = itemView.findViewById(R.id.tvOrderStatus);
                btnDeliver = itemView.findViewById(R.id.btnDeliverOrder);
            }
        }
    }
}
