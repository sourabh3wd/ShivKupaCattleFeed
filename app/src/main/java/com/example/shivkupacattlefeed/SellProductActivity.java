package com.example.shivkupacattlefeed;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SellProductActivity extends AppCompatActivity {

    private AutoCompleteTextView actvProductSelect;
    private EditText etSellQuantity, etSellRate, etCashReceived, etFarmerName, etFarmerMobile, etDueDate;
    private TextView tvTotalBill, tvChangeReturn, tvUnitLabel;
    private CheckBox cbSellInKg;
    private RadioGroup rgPaymentMethod;
    private View tilCashReceived, dividerCash, tilDueDate, cvSelectedProductPhoto;
    private ImageView ivSelectedProduct;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private final List<Product> productList = new ArrayList<>();
    private Product selectedProduct;
    private ProductSellAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sell_product);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etFarmerName = findViewById(R.id.etFarmerName);
        etFarmerMobile = findViewById(R.id.etFarmerMobile);
        actvProductSelect = findViewById(R.id.actvProductSelect);
        etSellQuantity = findViewById(R.id.etSellQuantity);
        etSellRate = findViewById(R.id.etSellRate);
        etCashReceived = findViewById(R.id.etCashReceived);
        etDueDate = findViewById(R.id.etDueDate);
        tvTotalBill = findViewById(R.id.tvTotalBill);
        tvChangeReturn = findViewById(R.id.tvChangeReturn);
        tvUnitLabel = findViewById(R.id.tvUnitLabel);
        cbSellInKg = findViewById(R.id.cbSellInKg);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        tilCashReceived = findViewById(R.id.tilCashReceived);
        dividerCash = findViewById(R.id.dividerCash);
        tilDueDate = findViewById(R.id.tilDueDate);
        cvSelectedProductPhoto = findViewById(R.id.cvSelectedProductPhoto);
        ivSelectedProduct = findViewById(R.id.ivSelectedProduct);
        Button btnConfirmSell = findViewById(R.id.btnConfirmSell);
        ImageButton btnBack = findViewById(R.id.btnBack);

        adapter = new ProductSellAdapter(this, productList, actvProductSelect);
        actvProductSelect.setAdapter(adapter);

        loadProducts();

        // डॅशबोर्डवरून आलेल्या मालाची माहिती आपोआप भरण्यासाठी
        String preSelectedProductId = getIntent().getStringExtra("PRODUCT_ID");

        actvProductSelect.setOnClickListener(v -> actvProductSelect.showDropDown());

        cbSellInKg.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (selectedProduct != null) {
                updatePriceAndBill();
            }
        });

        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCredit) {
                tilCashReceived.setVisibility(View.VISIBLE);
                dividerCash.setVisibility(View.VISIBLE);
                tvChangeReturn.setVisibility(View.VISIBLE);
                tilDueDate.setVisibility(View.VISIBLE);
                etCashReceived.setHint("काही जमा रक्कम (Optional ₹)");
            } else {
                tilCashReceived.setVisibility(View.VISIBLE);
                dividerCash.setVisibility(View.VISIBLE);
                tvChangeReturn.setVisibility(View.VISIBLE);
                tilDueDate.setVisibility(View.GONE);
                etCashReceived.setHint("दिलेली रक्कम (Cash Received ₹)");
            }
        });

        etDueDate.setOnClickListener(v -> showDatePicker());

        setupTextWatchers();

        btnConfirmSell.setOnClickListener(v -> confirmSale());
        btnBack.setOnClickListener(v -> finish());
    }

    private void updatePriceAndBill() {
        if (selectedProduct == null) return;

        // फोटो दाखवा
        if (selectedProduct.imageUrl != null && !selectedProduct.imageUrl.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(selectedProduct.imageUrl, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivSelectedProduct.setImageBitmap(decodedByte);
                cvSelectedProductPhoto.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                cvSelectedProductPhoto.setVisibility(View.GONE);
            }
        } else {
            cvSelectedProductPhoto.setVisibility(View.GONE);
        }

        try {
            double basePrice = Double.parseDouble(selectedProduct.price);
            String weightStr = selectedProduct.weightPerUnit;
            double weight = (weightStr != null && !weightStr.isEmpty()) ? Double.parseDouble(weightStr) : 1.0;

            if (cbSellInKg.isChecked()) {
                double pricePerKg = basePrice / weight;
                etSellRate.setText(String.format(Locale.getDefault(), "%.2f", pricePerKg));
                tvUnitLabel.setText("किलो (Kg)");
            } else {
                etSellRate.setText(selectedProduct.price);
                tvUnitLabel.setText(selectedProduct.unit);
            }
            calculateBill();
        } catch (Exception e) {
            Log.e("SellProduct", "Error updating price: " + e.getMessage());
        }
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> 
            etDueDate.setText(String.format(Locale.getDefault(), "%d/%d/%d", dayOfMonth, month + 1, year)), 
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateBill();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        etSellQuantity.addTextChangedListener(watcher);
        etSellRate.addTextChangedListener(watcher);
        etCashReceived.addTextChangedListener(watcher);
    }

    private void loadProducts() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        mDatabase.child("products").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                String preSelectedId = getIntent().getStringExtra("PRODUCT_ID");
                
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product p = data.getValue(Product.class);
                    if (p != null && uid.equals(p.dealerId)) {
                        productList.add(p);
                        // जर डॅशबोर्डवरून माल पाठवला असेल, तर तो इथे सेट करा
                        if (preSelectedId != null && preSelectedId.equals(p.id)) {
                            selectedProduct = p;
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                
                // जर माल सापडला असेल, तर सर्व रकाने आपोआप भरा
                if (selectedProduct != null) {
                    actvProductSelect.setText(selectedProduct.brand + " - " + selectedProduct.category, false);
                    etSellQuantity.setText("1"); // बाय डिफॉल्ट १ बॅग
                    updatePriceAndBill();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void calculateBill() {
        try {
            double qty = Double.parseDouble(etSellQuantity.getText().toString().isEmpty() ? "0" : etSellQuantity.getText().toString());
            double rate = Double.parseDouble(etSellRate.getText().toString().isEmpty() ? "0" : etSellRate.getText().toString());
            double total = qty * rate;
            tvTotalBill.setText(String.format(Locale.getDefault(), "एकूण बिल: ₹ %d", (long)total));

            double cash = Double.parseDouble(etCashReceived.getText().toString().isEmpty() ? "0" : etCashReceived.getText().toString());
            if (cash > 0) {
                double change = cash - total;
                if (rgPaymentMethod.getCheckedRadioButtonId() == R.id.rbCash) {
                    tvChangeReturn.setText(String.format(Locale.getDefault(), "परत द्यायचे: ₹ %d", (long) (change > 0 ? change : 0)));
                } else {
                    tvChangeReturn.setText(String.format(Locale.getDefault(), "उधारी शिल्लक: ₹ %d", (long) (total - cash > 0 ? total - cash : 0)));
                }
                tvChangeReturn.setTextColor(change < 0 ? 0xFFFF0000 : 0xFF2E7D32);
            } else {
                tvChangeReturn.setText(rgPaymentMethod.getCheckedRadioButtonId() == R.id.rbCash ? "परत द्यायचे: ₹ 0" : String.format(Locale.getDefault(), "उधारी शिल्लक: ₹ %d", (long)total));
            }
        } catch (Exception e) {
            tvTotalBill.setText("एकूण बिल: ₹ 0");
        }
    }

    private void confirmSale() {
        if (selectedProduct == null) {
            Toast.makeText(this, "कृपया माल निवडा", Toast.LENGTH_SHORT).show();
            return;
        }

        final String farmerName = etFarmerName.getText().toString().trim();
        final String farmerMobile = etFarmerMobile.getText().toString().trim();
        final String qtyStr = etSellQuantity.getText().toString();
        final String dueDate = etDueDate.getText().toString().trim();
        final String cashRecStr = etCashReceived.getText().toString().trim();
        final boolean isCredit = rgPaymentMethod.getCheckedRadioButtonId() == R.id.rbCredit;

        if (farmerName.isEmpty()) {
            Toast.makeText(this, "कृपया शेतकऱ्याचे नाव टाका", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCredit && dueDate.isEmpty()) {
            Toast.makeText(this, "कृपया पैसे देण्याची तारीख निवडा", Toast.LENGTH_SHORT).show();
            return;
        }

        if (qtyStr.isEmpty()) return;

        try {
            final double sellQty = Double.parseDouble(qtyStr);
            final double currentQtyUnits = Double.parseDouble(selectedProduct.quantity);
            String weightStr = selectedProduct.weightPerUnit;
            double weightPerUnit = (weightStr != null && !weightStr.isEmpty()) ? Double.parseDouble(weightStr) : 1.0;

            final double totalSellInUnits;
            if (cbSellInKg.isChecked()) {
                totalSellInUnits = sellQty / weightPerUnit;
            } else {
                totalSellInUnits = sellQty;
            }

            if (totalSellInUnits > currentQtyUnits) {
                Toast.makeText(this, "शिल्लक स्टॉक पेक्षा जास्त माल टाकता येणार नाही!", Toast.LENGTH_LONG).show();
                return;
            }

            final double totalBill;
            String billText = tvTotalBill.getText().toString().replace("एकूण बिल: ₹ ", "").trim();
            totalBill = Double.parseDouble(billText.isEmpty() ? "0" : billText);
            
            final double rawCashRec = cashRecStr.isEmpty() ? 0 : Double.parseDouble(cashRecStr);
            // जर रोख पैसे जास्त दिले असतील, तर आपण फक्त बिलाच्या रकमेपर्यंतच 'Paid' म्हणून नोंदवू 
            // कारण उरलेले पैसे आपण लगेच परत दिले आहेत.
            final double paidAmt = Math.min(rawCashRec, totalBill);
            
            if (!isCredit && rawCashRec < totalBill) {
                Toast.makeText(this, "रोख विक्रीसाठी पूर्ण रक्कम जमा करणे आवश्यक आहे किंवा 'उधार' निवडा.", Toast.LENGTH_LONG).show();
                return;
            }

            final double balance = isCredit ? Math.max(0, totalBill - rawCashRec) : 0;
            final String paymentMethod = isCredit ? "Credit (Udhar)" : "Cash";
            final String orderId = mDatabase.child("orders").push().getKey();
            
            String productNameWithWeight = selectedProduct.brand + " " + selectedProduct.category;
            if (selectedProduct.weightPerUnit != null && !selectedProduct.weightPerUnit.isEmpty()) {
                productNameWithWeight += " (" + selectedProduct.weightPerUnit + "kg)";
            }

            Order order = new Order(orderId, selectedProduct.id, productNameWithWeight,
                    "Walk-in", farmerName, farmerMobile, mAuth.getUid(), qtyStr, String.valueOf((long)totalBill), 
                    String.valueOf((long)paidAmt), String.valueOf((long)balance), "Sold", paymentMethod, dueDate, System.currentTimeMillis());

            // जर जमा केलेली रक्कम बिलापेक्षा जास्त असेल तर मेसेज दाखवा
            if (paidAmt > totalBill) {
                new AlertDialog.Builder(this)
                        .setTitle("जास्त रक्कम जमा?")
                        .setMessage("बिलापेक्षा (₹ " + (long)totalBill + ") जास्त रक्कम (₹ " + (long)paidAmt + ") जमा करत आहात. खात्री आहे का?")
                        .setPositiveButton("हो, जमा करा", (dialog, which) -> executeSale(order, selectedProduct, totalSellInUnits, farmerName, farmerMobile, totalBill, paidAmt, balance, isCredit, dueDate, qtyStr))
                        .setNegativeButton("नाही, बदला", null)
                        .show();
            } else {
                executeSale(order, selectedProduct, totalSellInUnits, farmerName, farmerMobile, totalBill, paidAmt, balance, isCredit, dueDate, qtyStr);
            }
        } catch (Exception e) {
            Toast.makeText(this, "काहीतरी चूक झाली आहे!", Toast.LENGTH_SHORT).show();
        }
    }

    private void executeSale(Order order, Product selectedProduct, double totalSellInUnits, String farmerName, String farmerMobile, double totalBill, double paidAmt, double balance, boolean isCredit, String dueDate, String qtyStr) {
        if (order.orderId == null) return;
        
        mDatabase.child("orders").child(order.orderId).setValue(order).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                double currentQtyUnits = Double.parseDouble(selectedProduct.quantity);
                mDatabase.child("products").child(selectedProduct.id).child("quantity").setValue(String.valueOf(currentQtyUnits - totalSellInUnits));
                
                String today = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
                String unitName = cbSellInKg.isChecked() ? "Kg" : selectedProduct.unit;
                String msg = "📜 *खरेदी पावती: शिवकृपा कॅटल फीड*\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "📅 *तारीख:* " + today + "\n" +
                        "👤 *ग्राहक:* *" + farmerName + "*\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "🌾 *माल:* " + selectedProduct.brand + " " + selectedProduct.category + "\n" +
                        "⚖️ *नग:* " + qtyStr + " " + unitName + "\n" +
                        "💰 *दर:* ₹ " + etSellRate.getText().toString() + "\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "💵 *एकूण बिल:*  *₹ " + (long)totalBill + "*\n" +
                        "📥 *जमा रक्कम:* ₹ " + (long)paidAmt + "\n" +
                        "🚩 *बाकी उधारी:* *₹ " + (long)balance + "*\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        (isCredit ? "⏰ *पैसे देण्याची तारीख:* *" + dueDate + "*" : "✅ *व्यवहार:* रोख (Cash)") + "\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "🙏 *धन्यवाद! पुन्हा भेटूया.*";
                
                sendWhatsApp(farmerMobile, msg);
                
                Toast.makeText(this, "विक्री यशस्वी!", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void sendWhatsApp(String mobile, String message) {
        if (mobile.isEmpty()) return;
        try {
            if (!mobile.startsWith("91") && mobile.length() == 10) mobile = "91" + mobile;
            Intent i = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=" + mobile + "&text=" + URLEncoder.encode(message, "UTF-8");
            i.setData(Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp पाठवता आले नाही", Toast.LENGTH_SHORT).show();
        }
    }

    private class ProductSellAdapter extends ArrayAdapter<Product> {
        private final AutoCompleteTextView targetView;

        public ProductSellAdapter(@NonNull Context context, @NonNull List<Product> products, AutoCompleteTextView targetView) {
            super(context, R.layout.item_dropdown_with_delete, products);
            this.targetView = targetView;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_dropdown_with_delete, parent, false);
            }

            Product p = getItem(position);
            TextView tvName = convertView.findViewById(R.id.tvItemName);
            ImageButton btnDelete = convertView.findViewById(R.id.btnDeleteItem);

            if (p != null) {
                String weightInfo = (p.weightPerUnit != null && !p.weightPerUnit.isEmpty()) ? " [" + p.weightPerUnit + " kg]" : "";
                String displayName = p.brand + " - " + p.category + weightInfo + " (साठा: " + p.quantity + " " + p.unit + ")";
                tvName.setText(displayName);
            }

            View.OnClickListener selectListener = v -> {
                if (p != null) {
                    selectedProduct = p;
                    String weightInfo = (p.weightPerUnit != null && !p.weightPerUnit.isEmpty()) ? " (" + p.weightPerUnit + " kg)" : "";
                    targetView.setText(p.brand + " - " + p.category + weightInfo, false);
                    updatePriceAndBill();
                    targetView.dismissDropDown();
                }
            };

            tvName.setOnClickListener(selectListener);
            convertView.setOnClickListener(selectListener);

            btnDelete.setOnClickListener(v -> 
                new AlertDialog.Builder(getContext())
                        .setTitle("काढून टाका")
                        .setMessage("हा माल लिस्टधून काढून टाकायचा का?")
                        .setPositiveButton("हो", (dialog, which) -> {
                            productList.remove(position);
                            notifyDataSetChanged();
                        })
                        .setNegativeButton("नाही", null)
                        .show()
            );

            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = productList;
                    results.count = productList.size();
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
