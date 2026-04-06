package com.example.shivkupacattlefeed;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.Executor;

public class PinActivity extends AppCompatActivity {

    private EditText etPin, etConfirmPin;
    private TextInputLayout tilConfirmPin;
    private Button btnSubmitPin;
    private MaterialCardView btnBiometric;
    private TextView tvPinTitle, tvPinSubtitle;
    private String savedPin;
    private boolean isSettingPin = false;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        etPin = findViewById(R.id.etPin);
        etConfirmPin = findViewById(R.id.etConfirmPin);
        tilConfirmPin = findViewById(R.id.tilConfirmPin);
        btnSubmitPin = findViewById(R.id.btnSubmitPin);
        btnBiometric = findViewById(R.id.btnBiometric);
        tvPinTitle = findViewById(R.id.tvPinTitle);
        tvPinSubtitle = findViewById(R.id.tvPinSubtitle);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        savedPin = prefs.getString("user_pin", null);

        if (savedPin == null) {
            isSettingPin = true;
            tvPinTitle.setText("सुरक्षा PIN सेट करा");
            tvPinSubtitle.setText("तुमचे खाते सुरक्षित ठेवण्यासाठी ४ अंकी PIN तयार करा");
            tilConfirmPin.setVisibility(View.VISIBLE);
            btnSubmitPin.setText("PIN सेट करा");
            btnBiometric.setVisibility(View.GONE);

            // ऑटो-फोकस: जेव्हा ४ अंक पूर्ण होतील तेव्हा Confirm PIN वर जा
            etPin.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 4) {
                        etConfirmPin.requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        } else {
            isSettingPin = false;
            tvPinTitle.setText("तुमचा PIN टाका");
            tvPinSubtitle.setText("ॲपमध्ये प्रवेश करण्यासाठी तुमचा ४ अंकी कोड किंवा फिंगरप्रिंट वापरा");
            tilConfirmPin.setVisibility(View.GONE);
            btnSubmitPin.setText("सुरू करा");
            checkBiometricSupport();
        }

        btnSubmitPin.setOnClickListener(v -> {
            String pin = etPin.getText().toString().trim();
            if (pin.length() != 4) {
                Toast.makeText(this, "कृपया ४ अंकी PIN टाका", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isSettingPin) {
                String confirmPin = etConfirmPin.getText().toString().trim();
                if (!pin.equals(confirmPin)) {
                    Toast.makeText(this, "PIN जुळत नाही! पुन्हा तपासा", Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.edit().putString("user_pin", pin).apply();
                Toast.makeText(this, "PIN यशस्वीपणे सेट झाला!", Toast.LENGTH_SHORT).show();
                goToMain();
            } else {
                if (pin.equals(savedPin)) {
                    goToMain();
                } else {
                    Toast.makeText(this, "चुकीचा PIN! पुन्हा प्रयत्न करा", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnBiometric.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));

        setupBiometric();
    }

    private void checkBiometricSupport() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                btnBiometric.setVisibility(View.VISIBLE);
                btnBiometric.post(() -> biometricPrompt.authenticate(promptInfo));
                break;
            default:
                btnBiometric.setVisibility(View.GONE);
                break;
        }
    }

    private void setupBiometric() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(PinActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                goToMain();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(PinActivity.this, "फिंगरप्रिंट ओळखता आली नाही", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("बायोमॅट्रिक लॉगिन")
                .setSubtitle("तुमच्या फिंगरप्रिंटचा वापर करून लॉगिन करा")
                .setNegativeButtonText("PIN वापरा")
                .build();
    }

    private void goToMain() {
        MainActivity.isPinVerified = true; // पिन व्हेरिफाय झाला आहे असे मार्क करा
        Intent intent = new Intent(this, MainActivity.class);
        String role = getIntent().getStringExtra("ROLE");
        intent.putExtra("ROLE", role);
        startActivity(intent);
        finish();
    }
}
