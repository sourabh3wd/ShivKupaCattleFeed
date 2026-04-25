package com.example.shivkupacattlefeed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductMasterActivity extends AppCompatActivity {

    private AutoCompleteTextView actvBrand, actvProduct;
    private RecyclerView rvMaster;
    private DatabaseReference mDatabase;
    private String dealerId;
    private List<ProductMaster> masterList = new ArrayList<>();
    private MasterAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_master);

        dealerId = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("product_master");

        actvBrand = findViewById(R.id.actvMasterBrand);
        actvProduct = findViewById(R.id.actvMasterProduct);
        rvMaster = findViewById(R.id.rvProductMaster);

        rvMaster.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MasterAdapter(masterList);
        rvMaster.setAdapter(adapter);

        findViewById(R.id.btnSaveMaster).setOnClickListener(v -> saveToMaster());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        fetchMasterData();
    }

    private void fetchMasterData() {
        mDatabase.orderByChild("dealerId").equalTo(dealerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                masterList.clear();
                Set<String> brandSuggestions = new HashSet<>();
                Set<String> productSuggestions = new HashSet<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    ProductMaster pm = data.getValue(ProductMaster.class);
                    if (pm != null) {
                        masterList.add(pm);
                        brandSuggestions.add(pm.brand);
                        productSuggestions.add(pm.productName);
                    }
                }
                
                actvBrand.setAdapter(new ArrayAdapter<>(ProductMasterActivity.this, 
                    android.R.layout.simple_dropdown_item_1line, new ArrayList<>(brandSuggestions)));
                actvProduct.setAdapter(new ArrayAdapter<>(ProductMasterActivity.this, 
                    android.R.layout.simple_dropdown_item_1line, new ArrayList<>(productSuggestions)));
                
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveToMaster() {
        MaterialButton btnSave = findViewById(R.id.btnSaveMaster);
        String brand = actvBrand.getText().toString().trim();
        String name = actvProduct.getText().toString().trim();

        if (brand.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "कृपया ब्रँड आणि मालाचे नाव भरा", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        String id = mDatabase.push().getKey();
        ProductMaster pm = new ProductMaster(id, brand, name, dealerId);
        
        if (id != null) {
            mDatabase.child(id).setValue(pm).addOnSuccessListener(aVoid -> {
                btnSave.setEnabled(true);
                Toast.makeText(this, "मास्टरमध्ये सेव झाले", Toast.LENGTH_SHORT).show();
                actvProduct.setText("");
            }).addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                Toast.makeText(this, "एरर आली: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private class MasterAdapter extends RecyclerView.Adapter<MasterAdapter.ViewHolder> {
        List<ProductMaster> list;
        MasterAdapter(List<ProductMaster> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductMaster pm = list.get(position);
            holder.t1.setText(pm.brand);
            holder.t2.setText(pm.productName);
            
            holder.itemView.setOnLongClickListener(v -> {
                mDatabase.child(pm.id).removeValue();
                Toast.makeText(ProductMasterActivity.this, "काढून टाकले", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView t1, t2;
            ViewHolder(View v) {
                super(v);
                t1 = v.findViewById(android.R.id.text1);
                t2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
