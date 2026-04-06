package com.example.shivkupacattlefeed;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

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

import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

public class ReportsActivity extends AppCompatActivity {

    private TextView tvReportDetailTitle, tvSelectedDateDisplay;
    private LinearLayout llReportDetailsContainer, llReportHeader, llDateSelection;
    private MaterialCardView cardTableContainer;
    private Spinner spinnerReportType;
    private TableLayout tableReport;
    private com.google.android.material.button.MaterialButton btnExportExcel, btnSelectDate;
    private DatabaseReference mDatabaseOrders, mDatabaseProducts, mDatabasePayments;
    private String dealerId;
    private ImageButton btnBack;
    private String selectedDate = "";
    private String currentReportType = ""; // "SALES", "STOCK", "UDHAR", "PAYMENTS"
    private List<String[]> currentReportData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        tvReportDetailTitle = findViewById(R.id.tvReportDetailTitle);
        tvSelectedDateDisplay = findViewById(R.id.tvSelectedDateDisplay);
        llReportDetailsContainer = findViewById(R.id.llReportDetailsContainer);
        llReportHeader = findViewById(R.id.llReportHeader);
        llDateSelection = findViewById(R.id.llDateSelection);
        cardTableContainer = findViewById(R.id.cardTableContainer);
        tableReport = findViewById(R.id.tableReport);
        btnExportExcel = findViewById(R.id.btnExportExcel);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        spinnerReportType = findViewById(R.id.spinnerReportType);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnExportExcel.setOnClickListener(v -> exportToExcel());
        btnSelectDate.setOnClickListener(v -> showDatePickerAndFetchReport());

        dealerId = FirebaseAuth.getInstance().getUid();
        mDatabaseOrders = FirebaseDatabase.getInstance().getReference("orders");
        mDatabaseProducts = FirebaseDatabase.getInstance().getReference("products");
        mDatabasePayments = FirebaseDatabase.getInstance().getReference("payments");

        setupSpinner();

        // Default to Sales report if no type passed
        fetchDailyReport(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
    }

    private void updateShortSummaries() {
        // This can be used to show quick counts on the cards themselves
    }

    private void setupSpinner() {
        String[] reportTypes = {"--- रिपोर्ट निवडा ---", "विक्री (Sales)", "स्टॉक (Stock)", "उधारी (Udhar)", "जमा झालेले पैसे (Payments)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, reportTypes);
        spinnerReportType.setAdapter(adapter);

        spinnerReportType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                llDateSelection.setVisibility(View.GONE);
                if (position == 1) { // Sales
                    llDateSelection.setVisibility(View.VISIBLE);
                    if (selectedDate.isEmpty()) {
                        selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                    }
                    tvSelectedDateDisplay.setText(selectedDate);
                    fetchDailyReport(selectedDate);
                } else if (position == 2) { // Stock
                    fetchStockReport();
                } else if (position == 3) { // Udhar
                    fetchUdharReport();
                } else if (position == 4) { // Payments
                    fetchPaymentsReport();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchPaymentsReport() {
        currentReportType = "PAYMENTS";
        llReportHeader.setVisibility(View.VISIBLE);
        tvReportDetailTitle.setText("जमा पेमेंट रिपोर्ट (Payments List)");
        llReportDetailsContainer.removeAllViews();
        tableReport.removeAllViews();
        cardTableContainer.setVisibility(View.VISIBLE);
        currentReportData.clear();

        String[] headers = {"तारीख", "शेतकऱ्याचे नाव", "जमा केलेली रक्कम (₹)"};
        addTableHeader(headers);
        currentReportData.add(headers);

        mDatabasePayments.orderByChild("dealerId").equalTo(dealerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Payment p = data.getValue(Payment.class);
                    if (p != null) {
                        String pDate = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date(p.timestamp));
                        String[] row = {pDate, p.farmerName, "₹ " + p.amount};
                        addTableRow(row);
                        currentReportData.add(row);
                    }
                }
                if (currentReportData.size() <= 1) {
                    cardTableContainer.setVisibility(View.GONE);
                    addDetailRow("अद्याप कोणतेही पेमेंट जमा झाले नाही", "");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showDatePickerAndFetchReport() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(cal.getTime());
            fetchDailyReport(selectedDate);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void fetchDailyReport(String date) {
        currentReportType = "SALES";
        llReportHeader.setVisibility(View.VISIBLE);
        tvReportDetailTitle.setText("विक्री रिपोर्ट: " + date);
        llReportDetailsContainer.removeAllViews();
        tableReport.removeAllViews();
        cardTableContainer.setVisibility(View.VISIBLE);
        currentReportData.clear();

        // Add Table Headers
        String[] headers = {"ग्राहक", "माल", "नग/बॅग", "एकूण रक्कम", "दिलेला जमा", "बाकी"};
        addTableHeader(headers);
        currentReportData.add(headers);

        mDatabaseOrders.orderByChild("dealerId").equalTo(dealerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalSales = 0;
                long totalCash = 0;
                long totalUdhar = 0;
                
                for (DataSnapshot data : snapshot.getChildren()) {
                    Order o = data.getValue(Order.class);
                    if (o != null) {
                        String oDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date(o.timestamp));
                        if (date.equals(oDate)) {
                            double price = Double.parseDouble(o.totalPrice != null ? o.totalPrice.replace("₹", "").trim() : "0");
                            double paid = Double.parseDouble(o.paidAmount != null ? o.paidAmount.replace("₹", "").trim() : "0");
                            double balance = Double.parseDouble(o.balanceAmount != null ? o.balanceAmount.replace("₹", "").trim() : "0");
                            
                            totalSales += price;
                            totalCash += paid;
                            totalUdhar += balance;

                            String[] row = {o.farmerName, o.productName, o.quantity, "₹" + (long)price, "₹" + (long)paid, "₹" + (long)balance};
                            addTableRow(row);
                            currentReportData.add(row);
                        }
                    }
                }
                
                // Add Summary Header
                TextView tvHeader = new TextView(ReportsActivity.this);
                tvHeader.setText("एकूण विक्री: ₹ " + totalSales + " | जमा: ₹ " + totalCash + " | उधारी: ₹ " + totalUdhar);
                tvHeader.setTextSize(16);
                tvHeader.setPadding(16, 24, 16, 16);
                tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                tvHeader.setTextColor(getResources().getColor(R.color.primary));
                llReportDetailsContainer.addView(tvHeader);

                if (currentReportData.size() <= 1) {
                    cardTableContainer.setVisibility(View.GONE);
                    addDetailRow("आज कोणतीही विक्री झाली नाही", "");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchStockReport() {
        currentReportType = "STOCK";
        llReportHeader.setVisibility(View.VISIBLE);
        tvReportDetailTitle.setText("स्टॉक स्थिती (Stock Status)");
        llReportDetailsContainer.removeAllViews();
        tableReport.removeAllViews();
        cardTableContainer.setVisibility(View.VISIBLE);
        currentReportData.clear();

        String[] headers = {"ब्रँड/माल", "कॅटेगरी", "वजन (kg)", "शिल्लक", "युनिट"};
        addTableHeader(headers);
        currentReportData.add(headers);

        mDatabaseProducts.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product p = data.getValue(Product.class);
                    if (p != null && dealerId.equals(p.dealerId)) {
                        String weight = (p.weightPerUnit != null) ? p.weightPerUnit : "-";
                        String[] row = {p.brand, p.category, weight, p.quantity, p.unit};
                        addTableRow(row);
                        currentReportData.add(row);
                    }
                }
                if (currentReportData.size() <= 1) cardTableContainer.setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchUdharReport() {
        currentReportType = "UDHAR";
        llReportHeader.setVisibility(View.VISIBLE);
        tvReportDetailTitle.setText("उधारी रिपोर्ट (Udhar List)");
        llReportDetailsContainer.removeAllViews();
        tableReport.removeAllViews();
        cardTableContainer.setVisibility(View.VISIBLE);
        currentReportData.clear();

        String[] headers = {"शेतकऱ्याचे नाव", "एकूण बाकी उधारी (₹)"};
        addTableHeader(headers);
        currentReportData.add(headers);

        mDatabaseOrders.orderByChild("dealerId").equalTo(dealerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Double> udharMap = new HashMap<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Order o = data.getValue(Order.class);
                    if (o != null) {
                        double balance = Double.parseDouble(o.balanceAmount != null ? o.balanceAmount.replace("₹", "").trim() : "0");
                        udharMap.put(o.farmerName, udharMap.getOrDefault(o.farmerName, 0.0) + balance);
                    }
                }
                
                boolean hasUdhar = false;
                for (String farmer : udharMap.keySet()) {
                    double totalBal = udharMap.get(farmer);
                    if (totalBal > 0) {
                        String[] row = {farmer, "₹ " + (long)totalBal};
                        addTableRow(row);
                        currentReportData.add(row);
                        
                        addUdharRow(farmer, totalBal);
                        hasUdhar = true;
                    }
                }
                
                if (!hasUdhar) {
                    cardTableContainer.setVisibility(View.GONE);
                    addDetailRow("सध्या कोणाचीही उधारी बाकी नाही", "");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addTableHeader(String[] headers) {
        TableRow tr = new TableRow(this);
        tr.setBackgroundColor(getResources().getColor(R.color.primary));
        for (String h : headers) {
            TextView tv = new TextView(this);
            tv.setText(h);
            tv.setTextColor(android.graphics.Color.WHITE);
            tv.setPadding(20, 15, 20, 15);
            tv.setTextSize(14);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tr.addView(tv);
        }
        tableReport.addView(tr);
    }

    private void addTableRow(String[] cells) {
        TableRow tr = new TableRow(this);
        tr.setPadding(0, 5, 0, 5);
        for (String c : cells) {
            TextView tv = new TextView(this);
            tv.setText(c);
            tv.setPadding(20, 15, 20, 15);
            tv.setTextSize(13);
            tr.addView(tv);
        }
        tableReport.addView(tr);
    }

    private void exportToExcel() {
        if (currentReportData.isEmpty()) {
            Toast.makeText(this, "काही डेटा उपलब्ध नाही!", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvContent = new StringBuilder();
        for (String[] row : currentReportData) {
            for (int i = 0; i < row.length; i++) {
                csvContent.append(row[i].replace(",", "")); // Remove commas to avoid CSV breakage
                if (i < row.length - 1) csvContent.append(",");
            }
            csvContent.append("\n");
        }

        try {
            String fileName = currentReportType + "_Report_" + System.currentTimeMillis() + ".csv";
            File file = new File(getExternalFilesDir(null), fileName);
            FileOutputStream out = new FileOutputStream(file);
            
            // Excel साठी UTF-8 BOM जोडणे (जेणेकरून मराठी अक्षरे नीट दिसतील)
            out.write(0xef);
            out.write(0xbb);
            out.write(0xbf);
            
            out.write(csvContent.toString().getBytes("UTF-8"));
            out.close();

            // Share the file
            Uri path = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, path);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "रिपोर्ट शेअर करा"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "एरर: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addUdharRow(String farmerName, double totalBalance) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_order, llReportDetailsContainer, false);
        TextView tvFarmerName = view.findViewById(R.id.tvOrderFarmerName);
        TextView tvProductName = view.findViewById(R.id.tvOrderProductName);
        TextView tvDetails = view.findViewById(R.id.tvOrderDetails);
        TextView tvStatus = view.findViewById(R.id.tvOrderStatus);
        View actions = view.findViewById(R.id.llOrderActions);

        tvFarmerName.setText(farmerName);
        tvProductName.setText("एकूण बाकी उधारी");
        tvDetails.setText("रक्कम: ₹ " + (long)totalBalance);
        tvStatus.setText("बाकी (Due)");
        tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.RED));
        actions.setVisibility(View.GONE);

        view.setOnClickListener(v -> showPaymentDialog(farmerName, totalBalance));

        llReportDetailsContainer.addView(view);
    }

    private void showPaymentDialog(String farmerName, double totalBalance) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null);
        EditText etAmount = dialogView.findViewById(R.id.etPaymentAmount);
        TextView tvRemaining = dialogView.findViewById(R.id.tvRemainingBalance);
        
        tvRemaining.setText("एकूण बाकी: ₹ " + (long)totalBalance);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(farmerName + " - जमा करा")
                .setView(dialogView)
                .setPositiveButton("जमा करा", (dialog, which) -> {
                    String amtStr = etAmount.getText().toString();
                    if (!amtStr.isEmpty()) {
                        double paidAmt = Double.parseDouble(amtStr);
                        processPayment(farmerName, paidAmt);
                    }
                })
                .setNegativeButton("रद्द करा", null)
                .show();
    }

    private void processPayment(String farmerName, double paidAmt) {
        String paymentId = FirebaseDatabase.getInstance().getReference("payments").push().getKey();
        Payment paymentRecord = new Payment(paymentId, farmerName, dealerId, String.valueOf((long)paidAmt), System.currentTimeMillis());
        
        if (paymentId != null) {
            FirebaseDatabase.getInstance().getReference("payments").child(paymentId).setValue(paymentRecord);
        }

        mDatabaseOrders.orderByChild("dealerId").equalTo(dealerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double remainingToPay = paidAmt;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Order o = data.getValue(Order.class);
                    if (o != null && farmerName.equals(o.farmerName)) {
                        double bal = Double.parseDouble(o.balanceAmount != null ? o.balanceAmount : "0");
                        if (bal > 0 && remainingToPay > 0) {
                            double toDeduct = Math.min(bal, remainingToPay);
                            double newBal = Math.max(0, bal - toDeduct); // Ensure never negative
                            double newPaid = Double.parseDouble(o.paidAmount != null ? o.paidAmount : "0") + toDeduct;
                            
                            data.getRef().child("balanceAmount").setValue(String.valueOf((long)newBal));
                            data.getRef().child("paidAmount").setValue(String.valueOf((long)newPaid));
                            
                            remainingToPay -= toDeduct;
                        }
                    }
                }
                
                // If there's still money left after clearing all orders, you can handle it here if needed
                if (remainingToPay > 0) {
                    // Optional: Log excess payment or alert the user
                    android.widget.Toast.makeText(ReportsActivity.this, "सर्व उधारी पूर्ण झाली! जास्तीचे ₹" + (long)remainingToPay + " परत दिले.", android.widget.Toast.LENGTH_LONG).show();
                } else {
                    android.widget.Toast.makeText(ReportsActivity.this, "₹ " + (long)paidAmt + " जमा झाले!", android.widget.Toast.LENGTH_SHORT).show();
                }
                fetchUdharReport(); // Refresh list
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addDetailRow(String title, String sub) {
        View view = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, llReportDetailsContainer, false);
        TextView t1 = view.findViewById(android.R.id.text1);
        TextView t2 = view.findViewById(android.R.id.text2);
        t1.setText(title);
        t1.setTextSize(16);
        t2.setText(sub);
        t2.setTextColor(getResources().getColor(R.color.primary));
        llReportDetailsContainer.addView(view);
    }
}
