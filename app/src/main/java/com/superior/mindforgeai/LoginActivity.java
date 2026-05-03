package com.superior.mindforgeai;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogleLogin, btnDirectEntry;
    private TextView tvSignUp;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;
    private ProgressDialog progressDialog;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    try {
                        GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                                .getResult(ApiException.class);
                        Log.d(TAG, "Google sign-in success: " + account.getEmail());
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        Log.e(TAG, "Google sign-in ApiException: " + e.getStatusCode() + " " + e.getMessage());
                        String msg = "Google sign-in failed";
                        if (e.getStatusCode() == 12500) {
                            msg = "Google Sign-In not configured. Add SHA-1 fingerprint in Firebase Console.";
                        } else if (e.getStatusCode() == 10) {
                            msg = "Developer error: Check SHA-1 and Web Client ID in Firebase Console.";
                        }
                        Toast.makeText(this, msg + " (" + e.getStatusCode() + ")", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e(TAG, "Google sign-in result not OK. Code: " + result.getResultCode());
                    if (result.getResultCode() != RESULT_CANCELED) {
                        Toast.makeText(this, "Google sign-in cancelled or failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnDirectEntry = findViewById(R.id.btnDirectEntry);
        tvSignUp = findViewById(R.id.tvSignUp);

        // Immediate hard reset for entrance state
        View topFade = findViewById(R.id.topFadeView);
        if (topFade != null) {
            topFade.setAlpha(0f);
            topFade.setTranslationY(-30f);
        }
        LinearLayout mainContent = findViewById(R.id.loginMainContent);
        if (mainContent != null) {
            for (int i = 0; i < mainContent.getChildCount(); i++) {
                mainContent.getChildAt(i).setAlpha(0f);
                mainContent.getChildAt(i).setTranslationY(40f);
            }
        }

        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        btnDirectEntry.setOnClickListener(v -> enterDirectly());
        
        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Small delay to ensure activity transition is stable
        findViewById(android.R.id.content).postDelayed(this::runEntranceAnimations, 100);
    }

    private void enterDirectly() {
        SharedPreferences prefs = getSharedPreferences("MindForgePrefs", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("isLoggedIn", true)
                .putString("userName", "Guest Scholar")
                .putString("userEmail", "guest@neural.forge")
                .apply();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void runEntranceAnimations() {
        if (isFinishing()) return;
        
        DecelerateInterpolator interpolator = new DecelerateInterpolator(1.2f);

        View topFade = findViewById(R.id.topFadeView);
        if (topFade != null) {
            topFade.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(600)
                    .setInterpolator(interpolator)
                    .start();
        }

        LinearLayout mainContent = findViewById(R.id.loginMainContent);
        if (mainContent != null) {
            for (int i = 0; i < mainContent.getChildCount(); i++) {
                View child = mainContent.getChildAt(i);
                child.animate()
                        .alpha(1f)
                        .translationY(0)
                        .setDuration(600)
                        .setStartDelay(150 + (i * 80))
                        .setInterpolator(interpolator)
                        .start();
            }
        }
    }

    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            return;
        }

        showProgress("Signing in...");
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    hideProgress();
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
        });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        showProgress("Signing in...");
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveGoogleUserToFirestore(user, account);
                        }
                    } else {
                        hideProgress();
                        Log.e(TAG, "Firebase auth with Google failed", task.getException());
                        Toast.makeText(this, "Auth failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveGoogleUserToFirestore(FirebaseUser user, GoogleSignInAccount account) {
        String userId = user.getUid();
        String name = account.getDisplayName() != null ? account.getDisplayName() : "Neural Architect";
        String email = account.getEmail() != null ? account.getEmail() : user.getEmail();

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId).set(userData)
                .addOnCompleteListener(task -> {
                    hideProgress();
                    if (task.isSuccessful()) {
                        SharedPreferences prefs = getSharedPreferences("MindForgePrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("isLoggedIn", true)
                                .putString("userName", name)
                                .putString("userEmail", email)
                                .apply();

                        Log.d(TAG, "Google user saved to Firestore: " + email);
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } else {
                        Log.e(TAG, "Failed to save Google user to Firestore", task.getException());
                        Toast.makeText(this, "Saved to auth, but profile sync failed", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                });
    }

    private void showProgress(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
