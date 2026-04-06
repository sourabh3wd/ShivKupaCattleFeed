package com.example.shivkupacattlefeed;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etMobile, etPassword;
    private Button btnLogin;
    private TextView btnGoToRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        
        // जर युजर आधीच लॉगिन असेल, तर त्याला थेट PIN स्क्रीनवर पाठवा
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, PinActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_login);

        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnSubmit);
        btnGoToRegister = findViewById(R.id.tvToggleMode);

        btnLogin.setOnClickListener(v -> loginUser());
        
        if (btnGoToRegister != null) {
            btnGoToRegister.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            });
        }
    }

    private void loginUser() {
        if (etMobile == null || etPassword == null) return;

        String mobile = etMobile.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (mobile.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "मोबाईल नंबर आणि पासवर्ड टाका", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mobile.length() != 10) {
            etMobile.setError("मोबाईल नंबर १० अंकी असावा");
            etMobile.requestFocus();
            return;
        }

        String email = mobile + "@shivkupa.com";

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // लॉगिन यशस्वी झाल्यावर आता थेट Main वर न जाता PIN वर जाईल
                startActivity(new Intent(LoginActivity.this, PinActivity.class));
                finish();
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "लॉगिन अयशस्वी";
                Toast.makeText(LoginActivity.this, "लॉगिन अयशस्वी: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
