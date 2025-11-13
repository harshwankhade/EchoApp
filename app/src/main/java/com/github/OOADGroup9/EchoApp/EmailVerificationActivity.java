package com.github.OOADGroup9.EchoApp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerificationActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private TextView verificationMessage;
    private Button verifyButton, goToLoginButton;
    private ProgressBar progressBar;
    private Handler handler;
    private static final int CHECK_INTERVAL = 2000; // Check every 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        handler = new Handler();

        verificationMessage = findViewById(R.id.verification_message);
        verifyButton = findViewById(R.id.verify_button);
        goToLoginButton = findViewById(R.id.go_to_login_button);
        progressBar = findViewById(R.id.verification_progress_bar);

        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            verificationMessage.setText("A verification email has been sent to:\n" + userEmail + "\n\nPlease verify your email to continue.");
        }

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkEmailVerification();
            }
        });

        goToLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToLoginActivity();
            }
        });

        // Auto-check verification every 2 seconds
        startAutoVerificationCheck();
    }

    private void checkEmailVerification() {
        progressBar.setVisibility(View.VISIBLE);

        currentUser.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);

                if (task.isSuccessful()) {
                    if (currentUser.isEmailVerified()) {
                        Toast.makeText(EmailVerificationActivity.this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                        SendUserToSettingsActivity();
                    } else {
                        Toast.makeText(EmailVerificationActivity.this, "Email not verified yet. Please check your email.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(EmailVerificationActivity.this, "Error checking verification status.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void startAutoVerificationCheck() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentUser != null) {
                    currentUser.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful() && currentUser.isEmailVerified()) {
                                Toast.makeText(EmailVerificationActivity.this, "Email verified! Proceeding to profile setup.", Toast.LENGTH_SHORT).show();
                                SendUserToSettingsActivity();
                                handler.removeCallbacksAndMessages(null);
                            } else {
                                // Continue checking
                                startAutoVerificationCheck();
                            }
                        }
                    });
                }
            }
        }, CHECK_INTERVAL);
    }

    private void SendUserToSettingsActivity() {
        Intent settingsIntent = new Intent(EmailVerificationActivity.this, SettingsActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(settingsIntent);
        finish();
    }

    private void SendUserToLoginActivity() {
        mAuth.signOut();
        Intent loginIntent = new Intent(EmailVerificationActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending callbacks
        handler.removeCallbacksAndMessages(null);
    }
}

