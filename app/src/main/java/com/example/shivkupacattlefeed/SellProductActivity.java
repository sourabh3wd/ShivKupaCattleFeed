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
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SellProductActivity extends AppCompatActivity {

    private AutoCompleteTextView actvProductSelect;
    private EditText etSellQuantity, etSellRate, etCashReceived, etFarmerName, etFarmerMobile, etDueDate;
    private TextView tvTotalBill, tvChangeReturn;
    private CheckBox cbSellInKg;
    private RadioGroup rgPaymentMethod;
    private View tilCashReceived, dividerCash, tilDueDate;
    private LinearLayout llCartItems;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private final List<Product> productList = new ArrayList<>();
    private final List<CartItem> cartList = new ArrayList<>();
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
        cbSellInKg = findViewById(R.id.cbSellInKg);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        tilCashReceived = findViewById(R.id.tilCashReceived);
        dividerCash = findViewById(R.id.dividerCash);
        tilDueDate = findViewById(R.id.tilDueDate);
        llCartItems = findViewById(R.id.llCartItems);
        Button btnAddToList = findViewById(R.id.btnAddToList);
        Button btnConfirmSell = findViewById(R.id.btnConfirmSell);
        ImageButton btnBack = findViewById(R.id.btnBack);

        adapter = new ProductSellAdapter(this, productList, actvProductSelect);
        actvProductSelect.setAdapter(adapter);

        loadProducts();

        actvProductSelect.setOnItemClickListener((parent, view, position, id) -> {
            selectedProduct = (Product) parent.getItemAtPosition(position);
            if (selectedProduct != null) {
                etSellRate.setText(selectedProduct.price);
                etSellQuantity.requestFocus();
            }
        });

        actvProductSelect.setOnClickListener(v -> actvProductSelect.showDropDown());

        btnAddToList.setOnClickListener(v -> addToCart());

        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCredit) {
                tilDueDate.setVisibility(View.VISIBLE);
                etCashReceived.setHint("काही जमा रक्कम (Optional ₹)");
            } else {
                tilDueDate.setVisibility(View.GONE);
                etCashReceived.setHint("दिलेली रक्कम (Cash Received ₹)");
            }
            calculateFinalBill();
        });

        etDueDate.setOnClickListener(v -> showDatePicker());

        etCashReceived.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { calculateFinalBill(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnConfirmSell.setOnClickListener(v -> confirmSale());
        btnBack.setOnClickListener(v -> finish());
    }

    private void addToCart() {
        if (selectedProduct == null) {
            Toast.makeText(this, "कृपया माल निवडा", Toast.LENGTH_SHORT).show();
            return;
        }
        String qtyStr = etSellQuantity.getText().toString();
        String rateStr = etSellRate.getText().toString();
        if (qtyStr.isEmpty() || rateStr.isEmpty()) return;

        double qty = Double.parseDouble(qtyStr);
        double rate = Double.parseDouble(rateStr);
        boolean inKg = cbSellInKg.isChecked();

        CartItem item = new CartItem(selectedProduct, qty, rate, inKg);
        cartList.add(item);
        updateCartUI();
        
        // Reset product selection for next item
        actvProductSelect.setText("");
        selectedProduct = null;
        etSellQuantity.setText("1");
        etSellRate.setText("");
        cbSellInKg.setChecked(false);
    }

    private void updateCartUI() {
        llCartItems.removeAllViews();
        double total = 0;
        for (int i = 0; i < cartList.size(); i++) {
            final int index = i;
            CartItem item = cartList.get(i);
            View v = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, llCartItems, false);
            TextView t1 = v.findViewById(android.R.id.text1);
            TextView t2 = v.findViewById(android.R.id.text2);
            
            double itemTotal = item.qty * item.rate;
            total += itemTotal;
            
            t1.setText(item.product.brand + " " + item.product.category + " (" + item.qty + (item.inKg ? " Kg" : " " + item.product.unit) + ")");
            t2.setText("दर: ₹" + item.rate + " | एकूण: ₹" + (long)itemTotal + " [काढून टाका]");
            t2.setTextColor(android.graphics.Color.RED);
            
            v.setOnClickListener(view -> {
                cartList.remove(index);
                updateCartUI();
            });
            llCartItems.addView(v);
        }
        tvTotalBill.setText("एकूण बिल: ₹ " + (long)total);
        calculateFinalBill();
    }

    private void calculateFinalBill() {
        try {
            double total = 0;
            for (CartItem item : cartList) total += (item.qty * item.rate);
            
            double cash = Double.parseDouble(etCashReceived.getText().toString().isEmpty() ? "0" : etCashReceived.getText().toString());
            if (rgPaymentMethod.getCheckedRadioButtonId() == R.id.rbCash) {
                double change = cash - total;
                tvChangeReturn.setText("परत द्यायचे: ₹ " + (long)Math.max(0, change));
            } else {
                tvChangeReturn.setText("उधारी शिल्लक: ₹ " + (long)Math.max(0, total - cash));
            }
        } catch (Exception e) {}
    }

    private void loadProducts() {
        String uid = mAuth.getUid();
        mDatabase.child("products").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product p = data.getValue(Product.class);
                    if (p != null && uid.equals(p.dealerId)) productList.add(p);
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void confirmSale() {
        if (cartList.isEmpty()) {
            Toast.makeText(this, "किमान एक माल जोडा", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = etFarmerName.getText().toString().trim();
        String mobile = etFarmerMobile.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "शेतकऱ्याचे नाव टाका", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalBill = 0;
        for (CartItem item : cartList) totalBill += (item.qty * item.rate);
        
        double cashRec = etCashReceived.getText().toString().isEmpty() ? 0 : Double.parseDouble(etCashReceived.getText().toString());
        boolean isCredit = rgPaymentMethod.getCheckedRadioButtonId() == R.id.rbCredit;
        double balance = isCredit ? Math.max(0, totalBill - cashRec) : 0;

        StringBuilder productsInfo = new StringBuilder();
        StringBuilder wsMsg = new StringBuilder("*शिवकृपा कॅटल फीड विक्री पावती*\n\n");
        wsMsg.append("शेतकरी: ").append(name).append("\n");
        wsMsg.append("---------------------------\n");

        for (CartItem item : cartList) {
            String unit = item.inKg ? " Kg" : " " + item.product.unit;
            productsInfo.append(item.product.brand).append(" ").append(item.product.category)
                    .append(" (").append(item.qty).append(unit).append("), ");

            wsMsg.append("• ").append(item.product.brand).append(" ").append(item.product.category)
                    .append("\n  ").append(item.qty).append(unit)
                    .append(" x ₹").append(item.rate).append(" = ₹").append((long)(item.qty * item.rate)).append("\n");

            // Update Stock
            double sellUnits = item.inKg ? item.qty / Double.parseDouble(item.product.weightPerUnit) : item.qty;
            mDatabase.child("products").child(item.product.id).child("quantity")
                    .setValue(String.valueOf(Double.parseDouble(item.product.quantity) - sellUnits));
        }

        wsMsg.append("---------------------------\n");
        wsMsg.append("*एकूण बिल: ₹ ").append((long)totalBill).append("*\n");
        wsMsg.append("जमा रक्कम: ₹ ").append((long)Math.min(cashRec, totalBill)).append("\n");
        if (balance > 0) {
            wsMsg.append("शिल्लक (उधारी): ₹ ").append((long)balance).append("\n");
            if (!etDueDate.getText().toString().isEmpty()) {
                wsMsg.append("देय तारीख: ").append(etDueDate.getText().toString()).append("\n");
            }
        }
        wsMsg.append("\n*धन्यवाद! पुन्हा भेट द्या.*");

        String orderId = mDatabase.child("orders").push().getKey();
        Order order = new Order(orderId, "MULTIPLE", productsInfo.toString(), "Walk-in", name, mobile, mAuth.getUid(), 
                "List", String.valueOf((long)totalBill), String.valueOf((long)Math.min(cashRec, totalBill)), 
                String.valueOf((long)balance), "Sold", (isCredit ? "Credit" : "Cash"), etDueDate.getText().toString(), System.currentTimeMillis());

        mDatabase.child("orders").child(orderId).setValue(order).addOnCompleteListener(task -> {
            Toast.makeText(this, "विक्री यशस्वी!", Toast.LENGTH_SHORT).show();
            if (!mobile.isEmpty()) {
                sendWhatsApp(mobile, wsMsg.toString());
            }
            finish();
        });
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

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> 
            etDueDate.setText(dayOfMonth + "/" + (month + 1) + "/" + year), 
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private static class CartItem {
        Product product;
        double qty, rate;
        boolean inKg;
        CartItem(Product p, double q, double r, boolean k) {
            this.product = p; this.qty = q; this.rate = r; this.inKg = k;
        }
    }

    private class ProductSellAdapter extends ArrayAdapter<Product> {
        private final AutoCompleteTextView targetView;
        public ProductSellAdapter(@NonNull Context context, @NonNull List<Product> products, AutoCompleteTextView targetView) {
            super(context, android.R.layout.simple_dropdown_item_1line, products);
            this.targetView = targetView;
        }
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView tv = (TextView) super.getView(position, convertView, parent);
            Product p = getItem(position);
            
            // नावासोबत वजन आणि शिल्लक स्टॉक दाखवा
            String displayText = p.brand + " - " + p.category;
            if (p.unit != null && (p.unit.contains("बॅग") || p.unit.contains("Bag"))) {
                displayText += " (" + p.weightPerUnit + " kg)";
            }
            displayText += " [स्टॉक: " + p.quantity + " " + p.unit + "]";
            
            tv.setText(displayText);
            
            String finalDisplayText = displayText;
            tv.setOnClickListener(v -> {
                selectedProduct = p;
                targetView.setText(p.brand + " - " + p.category + " (" + p.weightPerUnit + " kg)", false);
                etSellRate.setText(p.price);
                targetView.dismissDropDown();
            });
            return tv;
        }
        @NonNull @Override public Filter getFilter() {
            return new Filter() {
                @Override protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults r = new FilterResults(); r.values = productList; r.count = productList.size(); return r;
                }
                @Override protected void publishResults(CharSequence constraint, FilterResults results) { notifyDataSetChanged(); }
            };
        }
    }
}
