package com.example.shivkupacattlefeed;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddProductActivity extends AppCompatActivity {

    private AutoCompleteTextView actvCategory, actvBrand, actvUnit;
    private EditText etVariant, etPrice, etQuantity, etDescription, etWeightPerUnit;
    private Button btnSave;
    private ImageButton btnBack;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private List<String> categories = new ArrayList<>(Arrays.asList(
            "गोळी (Goli Feed)", "पेंड (Cake/Pend)", "सरकी पेंड (Cotton Seed)", "शेंगदाणा पेंड (Groundnut)",
            "मका भरडा (Maize Crush)", "भुसा (Busa/Chunni)", "तूर चुनी (Tur)", "मुग चुनी (Moong)",
            "उडीद चुनी (Urad)", "खनिज मिश्रण (Mineral)", "मका सुग्रास", "सायलेज (Silage)",
            "बायपास फॅट (Bypass Fat)", "कॅल्शियम (Calcium)"
    ));
    
    private List<String> brands = new ArrayList<>(Arrays.asList(
            "कपिला (Kapila)", "गोदरेज (Godrej)", "गोकुळ (Gokul)", "अमुल (Amul)", "सुग्रास (Sugras)",
            "टाटा (Tata)", "महानंदा (Mahananda)", "हुजुर (Hujur)", "वारणा (Warana)", "सोनाई (Sonai)",
            "नुट्रीलॅब (Nutrilab)", "कार्गिल (Cargill)", "लोकल ब्रँड (Local Brand)"
    ));
    
    private List<String> units = new ArrayList<>(Arrays.asList("बॅग (Bag)", "किलो (Kg)", "नग (Nos)", "लिटर (Ltr)", "टन (Ton)"));

    private CustomDropdownAdapter categoryAdapter, brandAdapter, unitAdapter;

    private String productId = null;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView ivProductPhoto;
    private Button btnTakePhoto;
    private Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("products");

        actvCategory = findViewById(R.id.actvCategory);
        actvBrand = findViewById(R.id.actvBrand);
        actvUnit = findViewById(R.id.actvUnit);
        etVariant = findViewById(R.id.etVariant);
        etPrice = findViewById(R.id.etPrice);
        etQuantity = findViewById(R.id.etQuantity);
        etDescription = findViewById(R.id.etDescription);
        etWeightPerUnit = findViewById(R.id.etWeightPerUnit);
        btnSave = findViewById(R.id.btnSaveProduct);
        btnBack = findViewById(R.id.btnBack);
        ivProductPhoto = findViewById(R.id.ivProductPhoto);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);

        btnTakePhoto.setOnClickListener(v -> dispatchTakePictureIntent());

        categoryAdapter = new CustomDropdownAdapter(this, categories, actvCategory);
        actvCategory.setAdapter(categoryAdapter);
        
        brandAdapter = new CustomDropdownAdapter(this, brands, actvBrand);
        actvBrand.setAdapter(brandAdapter);

        unitAdapter = new CustomDropdownAdapter(this, units, actvUnit);
        actvUnit.setAdapter(unitAdapter);

        setupAutoCompleteBehavior(actvCategory);
        setupAutoCompleteBehavior(actvBrand);
        setupAutoCompleteBehavior(actvUnit);

        // Check for Edit Mode
        if (getIntent().hasExtra("productId")) {
            productId = getIntent().getStringExtra("productId");
            actvCategory.setText(getIntent().getStringExtra("category"), false);
            actvBrand.setText(getIntent().getStringExtra("brand"), false);
            etVariant.setText(getIntent().getStringExtra("variant"));
            etPrice.setText(getIntent().getStringExtra("price"));
            etQuantity.setText(getIntent().getStringExtra("quantity"));
            actvUnit.setText(getIntent().getStringExtra("unit"), false);
            
            String unit = getIntent().getStringExtra("unit");
            if (unit != null && (unit.contains("बॅग") || unit.contains("Bag"))) {
                etWeightPerUnit.setVisibility(View.VISIBLE);
                findViewById(R.id.tilWeightPerUnit).setVisibility(View.VISIBLE);
            }
            
            btnSave.setText("माहिती अपडेट करा");
            ((TextView)findViewById(R.id.tvAddProductTitle)).setText("माल अपडेट करा");
            
            // Show Delete Button in Edit Mode
            Button btnDelete = new Button(this);
            btnDelete.setText("माल काढून टाका (Delete)");
            btnDelete.setBackgroundColor(android.graphics.Color.RED);
            btnDelete.setTextColor(android.graphics.Color.WHITE);
            LinearLayout layout = findViewById(R.id.llAddProductContainer);
            if (layout != null) {
                layout.addView(btnDelete);
                btnDelete.setOnClickListener(v -> deleteProduct());
            }
        }

        actvUnit.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            if (selected.contains("बॅग") || selected.contains("Bag")) {
                etWeightPerUnit.setVisibility(View.VISIBLE);
                findViewById(R.id.tilWeightPerUnit).setVisibility(View.VISIBLE);
            } else {
                etWeightPerUnit.setVisibility(View.GONE);
                findViewById(R.id.tilWeightPerUnit).setVisibility(View.GONE);
                etWeightPerUnit.setText("1");
            }
        });

        fetchExistingSuggestions();

        btnSave.setOnClickListener(v -> saveProduct());
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupAutoCompleteBehavior(AutoCompleteTextView view) {
        view.setOnClickListener(v -> view.showDropDown());
        view.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) view.showDropDown(); });
    }

    private void fetchExistingSuggestions() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> catSet = new HashSet<>(categories);
                Set<String> brandSet = new HashSet<>(brands);

                for (DataSnapshot data : snapshot.getChildren()) {
                    Product p = data.getValue(Product.class);
                    if (p != null) {
                        if (p.category != null && !p.category.isEmpty()) catSet.add(p.category);
                        if (p.brand != null && !p.brand.isEmpty()) brandSet.add(p.brand);
                    }
                }

                categories.clear();
                categories.addAll(catSet);
                Collections.sort(categories);
                categoryAdapter.notifyDataSetChanged();

                brands.clear();
                brands.addAll(brandSet);
                Collections.sort(brands);
                brandAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "कॅमेरा अ‍ॅप सापडले नाही!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            ivProductPhoto.setImageBitmap(imageBitmap);
        }
    }

    private void deleteProduct() {
        new AlertDialog.Builder(this)
                .setTitle("माल काढून टाका")
                .setMessage("तुम्हाला खात्री आहे की हा माल कायमचा काढून टाकायचा आहे?")
                .setPositiveButton("हो", (dialog, which) -> {
                    if (productId != null) {
                        mDatabase.child(productId).removeValue().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "माल यशस्वीपणे काढून टाकला!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }
                })
                .setNegativeButton("नाही", null)
                .show();
    }

    private void saveProduct() {
        String category = actvCategory.getText().toString().trim();
        String brand = actvBrand.getText().toString().trim();
        String variant = etVariant.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String unit = actvUnit.getText().toString().trim();
        String quantityStr = etQuantity.getText().toString().trim();
        String weight = etWeightPerUnit.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String dealerId = mAuth.getUid();

        if (category.isEmpty() || brand.isEmpty() || price.isEmpty() || quantityStr.isEmpty()) {
            Toast.makeText(this, "कृपया महत्त्वाची माहिती भरा", Toast.LENGTH_SHORT).show();
            return;
        }

        if (weight.isEmpty()) weight = "1";

        double newQuantity = Double.parseDouble(quantityStr);
        
        // जर आपण 'Restock' करत असू, तर जुन्या स्टॉकमध्ये नवीन स्टॉक मिळवा
        if (getIntent().hasExtra("productId")) {
            double oldQuantity = Double.parseDouble(getIntent().getStringExtra("quantity") != null ? getIntent().getStringExtra("quantity") : "0");
            // आपण असा विचार करूया की 'Restock' करताना युजर नवीन आलेला माल टाकत आहे
            // जर युजरने एॅडिट मोडमध्ये फक्त माहिती सुधारली असेल, तर तो वेगळा विचार करावा लागेल
            // पण 'Restock' साठी आपण बेरीज करूया.
            newQuantity = oldQuantity + newQuantity;
        }

        String finalQty = String.valueOf(newQuantity);
        String id = (productId != null) ? productId : mDatabase.push().getKey();
        
        String encodedImage = "";
        if (imageBitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] b = baos.toByteArray();
            encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        } else if (getIntent().hasExtra("imageUrl")) {
            encodedImage = getIntent().getStringExtra("imageUrl");
        }

        Product product = new Product(id, category, brand, variant, price, finalQty, unit, weight, description, dealerId, encodedImage);

        if (id != null) {
            mDatabase.child(id).setValue(product).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String msg = (productId != null) ? "माल यशस्वीपणे अपडेट केला! (एकूण स्टॉक: " + finalQty + ")" : "माल यशस्वीपणे ॲड केला!";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }

    private class CustomDropdownAdapter extends ArrayAdapter<String> {
        private AutoCompleteTextView targetView;
        private List<String> allItems;

        public CustomDropdownAdapter(@NonNull Context context, @NonNull List<String> items, AutoCompleteTextView targetView) {
            super(context, R.layout.item_dropdown_with_delete, items);
            this.targetView = targetView;
            this.allItems = items;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_dropdown_with_delete, parent, false);
            }
            
            String item = getItem(position);
            TextView tvName = convertView.findViewById(R.id.tvItemName);
            ImageButton btnDelete = convertView.findViewById(R.id.btnDeleteItem);

            tvName.setText(item);
            btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
            
            View.OnClickListener selectListener = v -> {
                targetView.setText(item, false);
                targetView.dismissDropDown();
                if (targetView == actvUnit) {
                    if (item.contains("बॅग") || item.contains("Bag")) {
                        etWeightPerUnit.setVisibility(View.VISIBLE);
                        findViewById(R.id.tilWeightPerUnit).setVisibility(View.VISIBLE);
                    } else {
                        etWeightPerUnit.setVisibility(View.GONE);
                        findViewById(R.id.tilWeightPerUnit).setVisibility(View.GONE);
                        etWeightPerUnit.setText("1");
                    }
                }
            };
            
            tvName.setOnClickListener(selectListener);
            convertView.setOnClickListener(selectListener);

            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("काढून टाका")
                        .setMessage("तुम्हाला '" + item + "' काढून टाकायचे आहे का?")
                        .setPositiveButton("हो", (dialog, which) -> {
                            remove(item);
                            notifyDataSetChanged();
                        })
                        .setNegativeButton("नाही", null)
                        .show();
            });

            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = allItems;
                    results.count = allItems.size();
                    return results;
                }
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }
            };
        }
    }
}
