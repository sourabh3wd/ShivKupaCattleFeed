package com.example.shivkupacattlefeed;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsActivity extends AppCompatActivity {

    private TextView tvReportDetailTitle, tvSelectedDateDisplay;
    private LinearLayout llReportDetailsContainer, llReportHeader, llDateSelection;
    private MaterialCardView cardTableContainer;
    private Spinner spinnerReportType;
    private TableLayout tableReport;
    private com.google.android.material.button.MaterialButton btnExportExcel, btnSelectDate;
    private DatabaseReference mDatabaseOrders;
    private String dealerId;
    private ImageButton btnBack;
    private String selectedDate = "";
    private String currentReportType = ""; 
    private List<String[]> currentReportData = new ArrayList<>();
    private BottomNavigationView bottomNavigation;

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
        bottomNavigation = findViewById(R.id.bottomNavigation);

        btnBack.setOnClickListener(v -> finish());
        btnExportExcel.setOnClickListener(v -> exportToExcel());
        btnSelectDate.setOnClickListener(v -> showDatePickerAndFetchReport());

        dealerId = FirebaseAuth.getInstance().getUid();
        mDatabaseOrders = FirebaseDatabase.getInstance().getReference("orders");

        setupSpinner();
        setupBottomNavigation();
        
        selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        fetchDailyReport(selectedDate);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_reports);
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_reports) return true;
            if (id == R.id.nav_home) { finish(); return true; }
            
            Intent intent = null;
            if (id == R.id.nav_hishob) intent = new Intent(this, HishobActivity.class);
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

    private void setupSpinner() {
        String[] reportTypes = {"--- रिपोर्ट निवडा ---", "आजची विक्री (Daily Sales)", "स्टॉक रिपोर्ट (Stock)", "उधारी यादी (Udhar List)", "जमा पेमेंट (Payments)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, reportTypes);
        spinnerReportType.setAdapter(adapter);

        spinnerReportType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                llDateSelection.setVisibility(View.GONE);
                if (position == 1) {
                    llDateSelection.setVisibility(View.VISIBLE);
                    tvSelectedDateDisplay.setText(selectedDate);
                    fetchDailyReport(selectedDate);
                } else if (position == 2) fetchStockReport();
                else if (position == 3) fetchUdharReport();
                else if (position == 4) fetchPaymentsReport();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchDailyReport(String date) {
        currentReportType = "SALES";
        tvReportDetailTitle.setText("विक्री रिपोर्ट: " + date);
        clearUI();
        String[] headers = {"ग्राहक", "माल", "नग", "एकूण", "जमा", "बाकी"};
        addTableHeader(headers);
        currentReportData.add(headers);

        mDatabaseOrders.orderByChild("dealerId").equalTo(dealerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalSales = 0, totalCash = 0, totalUdhar = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Order o = data.getValue(Order.class);
                    if (o != null) {
                        String oDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date(o.timestamp));
                        if (date.equals(oDate) && !"पैसे जमा (Payment)".equals(o.productName)) {
                            long p = parseLong(o.totalPrice), pd = parseLong(o.paidAmount), b = parseLong(o.balanceAmount);
                            totalSales += p; totalCash += pd; totalUdhar += b;
                            String[] row = {o.farmerName, o.productName, o.quantity, "₹"+p, "₹"+pd, "₹"+b};
                            addTableRow(row); currentReportData.add(row);
                        }
                    }
                }
                updateSummaryText("एकूण विक्री: ₹ " + totalSales + " | जमा: ₹ " + totalCash + " | उधारी: ₹ " + totalUdhar);
                if (currentReportData.size() <= 1) showEmptyMessage("आज कोणतीही विक्री झाली नाही");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchUdharReport() {
        currentReportType = "UDHAR";
        tvReportDetailTitle.setText("उधारी रिपोर्ट (Udhar List)");
        clearUI();
        String[] headers = {"शेतकऱ्याचे नाव", "मोबाईल", "बाकी उधारी (₹)"};
        addTableHeader(headers);
        currentReportData.add(headers);

        mDatabaseOrders.orderByChild("dealerId").equalTo(dealerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Long> udharMap = new HashMap<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Order o = data.getValue(Order.class);
                    if (o != null) {
                        String mobile = (o.farmerMobile != null && !o.farmerMobile.isEmpty()) ? o.farmerMobile : "NoMobile";
                        String key = (o.farmerName != null ? o.farmerName : "Unknown") + "_" + mobile;
                        udharMap.put(key, (udharMap.getOrDefault(key, 0L)) + parseLong(o.balanceAmount));
                    }
                }
                
                List<String> sortedKeys = new ArrayList<>(udharMap.keySet());
                sortedKeys.sort((k1, k2) -> Long.compare(udharMap.get(k2), udharMap.get(k1)));

                for (String key : sortedKeys) {
                    long bal = udharMap.get(key);
                    if (bal != 0) {
                        String name = key.split("_")[0];
                        String mob = key.split("_")[1];
                        if (mob.equals("NoMobile")) mob = "";
                        
                        String balText = bal > 0 ? "₹ " + bal : "जमा: ₹ " + Math.abs(bal);
                        String[] row = {name, (mob.isEmpty() ? "-" : mob), balText};
                        addTableRow(row); currentReportData.add(row);
                        addUdharItemRow(name, mob, bal);
                    }
                }
                if (currentReportData.size() <= 1) showEmptyMessage("सध्या कोणाचीही उधारी बाकी नाही");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchPaymentsReport() {
        currentReportType = "PAYMENTS";
        tvReportDetailTitle.setText("जमा पेमेंट रिपोर्ट");
        clearUI();
        String[] headers = {"तारीख", "शेतकरी", "रक्कम", "शेरा"};
        addTableHeader(headers);
        currentReportData.add(headers);

        mDatabaseOrders.orderByChild("dealerId").equalTo(dealerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Order o = data.getValue(Order.class);
                    if (o != null && "पैसे जमा (Payment)".equals(o.productName)) {
                        String pDate = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(o.timestamp));
                        String amt = o.paidAmount.replace("-", "");
                        String[] row = {pDate, o.farmerName, "₹ " + amt, o.remark};
                        addTableRow(row); currentReportData.add(row);
                    }
                }
                if (currentReportData.size() <= 1) showEmptyMessage("अद्याप कोणतेही पेमेंट जमा झाले नाही");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchStockReport() {
        currentReportType = "STOCK";
        tvReportDetailTitle.setText("स्टॉक रिपोर्ट");
        clearUI();
        String[] headers = {"माल", "कॅटेगरी", "शिल्लक नग"};
        addTableHeader(headers);
        currentReportData.add(headers);

        FirebaseDatabase.getInstance().getReference("products").orderByChild("dealerId").equalTo(dealerId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Product p = data.getValue(Product.class);
                        if (p != null) {
                            String[] row = {p.brand, p.category, p.quantity + " " + p.unit};
                            addTableRow(row); currentReportData.add(row);
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void addUdharItemRow(String name, String mobile, long balance) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_order, llReportDetailsContainer, false);
        ((TextView)v.findViewById(R.id.tvOrderFarmerName)).setText(name);
        ((TextView)v.findViewById(R.id.tvOrderProductName)).setText(balance > 0 ? "बाकी उधारी" : "जादा रक्कम जमा");
        ((TextView)v.findViewById(R.id.tvOrderDetails)).setText("मोबाईल: " + (mobile.isEmpty() ? "-" : mobile) + "\nरक्कम: ₹ " + Math.abs(balance));
        TextView tvStatus = v.findViewById(R.id.tvOrderStatus);
        if (balance > 0) {
            tvStatus.setText("बाकी");
            tvStatus.setBackgroundResource(R.drawable.status_bg_red);
        } else {
            tvStatus.setText("जमा");
            tvStatus.setBackgroundResource(R.drawable.status_bg);
        }
        v.findViewById(R.id.llOrderActions).setVisibility(View.GONE);
        v.setOnClickListener(view -> showPaymentDialog(name, mobile, balance));
        llReportDetailsContainer.addView(v);
    }

    private void showPaymentDialog(String name, String mobile, long balance) {
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null);
        EditText etAmt = dv.findViewById(R.id.etPaymentAmount);
        ((TextView)dv.findViewById(R.id.tvRemainingBalance)).setText("एकूण बाकी: ₹ " + balance);
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(name + " - पैसे जमा").setView(dv)
            .setPositiveButton("जमा करा", (d, w) -> {
                String a = etAmt.getText().toString();
                if (!a.isEmpty()) savePayment(name, mobile, a);
            }).setNegativeButton("रद्द", null).show();
    }

    private void savePayment(String name, String mobile, String amount) {
        String id = mDatabaseOrders.push().getKey();
        String fId = mobile.equals("NoMobile") ? name : mobile;
        Order p = new Order(id, "PAYMENT", "पैसे जमा (Payment)", fId, name, (mobile.equals("NoMobile") ? "" : mobile),
                dealerId, "0", "0", amount, "-" + amount, "Paid", "Report Payment", "", "रिपोर्टमधून जमा", System.currentTimeMillis());
        if (id != null) {
            mDatabaseOrders.child(id).setValue(p).addOnCompleteListener(t -> {
                Toast.makeText(this, "₹ " + amount + " जमा झाले!", Toast.LENGTH_SHORT).show();
                fetchUdharReport();
            });
        }
    }

    private void clearUI() {
        llReportDetailsContainer.removeAllViews();
        tableReport.removeAllViews();
        cardTableContainer.setVisibility(View.VISIBLE);
        currentReportData.clear();
        llReportHeader.setVisibility(View.VISIBLE);
    }

    private void addTableHeader(String[] headers) {
        TableRow tr = new TableRow(this); tr.setBackgroundColor(getResources().getColor(R.color.primary));
        for (String h : headers) {
            TextView tv = new TextView(this); tv.setText(h); tv.setTextColor(android.graphics.Color.WHITE);
            tv.setPadding(15, 10, 15, 10); tv.setTextSize(12); tr.addView(tv);
        }
        tableReport.addView(tr);
    }

    private void addTableRow(String[] cells) {
        TableRow tr = new TableRow(this);
        for (String c : cells) {
            TextView tv = new TextView(this); tv.setText(c); tv.setPadding(15, 10, 15, 10); tv.setTextSize(12); tr.addView(tv);
        }
        tableReport.addView(tr);
    }

    private void updateSummaryText(String text) {
        TextView tv = new TextView(this); tv.setText(text); tv.setTextSize(14);
        tv.setPadding(16, 16, 16, 8); tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(getResources().getColor(R.color.primary));
        llReportDetailsContainer.addView(tv, 0);
    }

    private void showEmptyMessage(String msg) {
        cardTableContainer.setVisibility(View.GONE);
        TextView tv = new TextView(this); tv.setText(msg); tv.setPadding(20, 40, 20, 20);
        tv.setGravity(android.view.Gravity.CENTER); llReportDetailsContainer.addView(tv);
    }

    private long parseLong(String s) {
        try {
            if (s == null || s.isEmpty()) return 0;
            // फक्त ₹ आणि स्वल्पविराम काढणे, मायनस चिन्ह राहू देणे जेणेकरून हिशोब चुकणार नाही
            return Long.parseLong(s.replace("₹", "").replace(",", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void showDatePickerAndFetchReport() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (v, y, m, d) -> {
            cal.set(y, m, d);
            selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(cal.getTime());
            tvSelectedDateDisplay.setText(selectedDate);
            fetchDailyReport(selectedDate);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void exportToExcel() {
        if (currentReportData.isEmpty()) return;
        StringBuilder csv = new StringBuilder();
        for (String[] r : currentReportData) {
            for (int i = 0; i < r.length; i++) {
                csv.append(r[i].replace(",", ""));
                if (i < r.length - 1) csv.append(",");
            }
            csv.append("\n");
        }
        try {
            File f = new File(getExternalFilesDir(null), currentReportType + "_Report.csv");
            FileOutputStream o = new FileOutputStream(f);
            o.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF}); // UTF-8 BOM
            o.write(csv.toString().getBytes("UTF-8")); o.close();
            Uri path = FileProvider.getUriForFile(this, getPackageName() + ".provider", f);
            Intent intent = new Intent(Intent.ACTION_SEND); intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, path); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "शेअर करा"));
        } catch (Exception e) { Toast.makeText(this, "एरर: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
    }
}
