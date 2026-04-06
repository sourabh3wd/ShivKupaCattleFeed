package com.example.shivkupacattlefeed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import java.util.List;

public class ReceivedOrdersActivity extends AppCompatActivity {

    private RecyclerView rvReceivedOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;
    private DatabaseReference mDatabase;
    private String dealerId;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_received_orders);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvReceivedOrders = findViewById(R.id.rvReceivedOrders);
        rvReceivedOrders.setLayoutManager(new LinearLayoutManager(this));
        
        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList);
        rvReceivedOrders.setAdapter(orderAdapter);

        dealerId = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        fetchOrders();
    }

    private void fetchOrders() {
        mDatabase.child("orders").orderByChild("dealerId").equalTo(dealerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        orderList.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Order order = data.getValue(Order.class);
                            if (order != null && "Pending".equals(order.status)) {
                                orderList.add(order);
                            }
                        }
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
            
            // Farmer name fetching logic
            mDatabase.child("users").child(order.farmerId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        holder.tvFarmerName.setText(snapshot.getValue(String.class));
                    } else {
                        holder.tvFarmerName.setText(order.farmerMobile);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            holder.tvProductName.setText(order.productName);
            holder.tvOrderDetails.setText("प्रमाण: " + order.quantity + " | एकूण: ₹ " + order.totalPrice);
            holder.tvStatus.setText(order.status);

            holder.btnDeliver.setOnClickListener(v -> {
                mDatabase.child("orders").child(order.orderId).child("status").setValue("Delivered")
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(ReceivedOrdersActivity.this, "माल दिला म्हणून नोंदवले!", Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvFarmerName, tvProductName, tvOrderDetails, tvStatus;
            Button btnDeliver;

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
