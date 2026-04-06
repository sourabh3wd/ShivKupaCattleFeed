package com.example.shivkupacattlefeed;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etMobile, etPassword, etConfirmPassword;
    private RadioGroup rgRole;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // XML मधील ID नुसार बदल
        etName = findViewById(R.id.etRegName);
        etMobile = findViewById(R.id.etRegMobile);
        etPassword = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        rgRole = findViewById(R.id.rgRegRole);
        btnRegister = findViewById(R.id.btnRegisterSubmit);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> registerUser());
        
        if (tvGoToLogin != null) {
            tvGoToLogin.setOnClickListener(v -> {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            });
        }
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
        int selectedId = rgRole.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "कृपया तुमची भूमिका निवडा", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton rb = findViewById(selectedId);
        String role = rb.getText().toString().contains("विक्रेता") ? "Dealer" : "Farmer";

        if (name.isEmpty() || mobile.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "सर्व माहिती भरा", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mobile.length() != 10) {
            etMobile.setError("मोबाईल नंबर १० अंकी असावा");
            etMobile.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("पासवर्ड मॅच होत नाही");
            etConfirmPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("पासवर्ड किमान ६ अंकी असावा");
            etPassword.requestFocus();
            return;
        }

        String email = mobile + "@shivkupa.com";

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getUid();
                User user = new User(name, role, mobile, password);
                mDatabase.child("users").child(uid).setValue(user).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "नोंदणी यशस्वी!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    }
                });
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "नोंदणी अयशस्वी";
                Toast.makeText(RegisterActivity.this, "नोंदणी अयशस्वी: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
