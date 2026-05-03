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

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    private TextInputEditText etName, etEmail, etPassword;
    private MaterialButton btnSignup, btnGoogleSignup;
    private TextView tvSignIn;
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
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        Log.e(TAG, "Google sign-in error: " + e.getStatusCode(), e);
                        Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignup = findViewById(R.id.btnSignup);
        btnGoogleSignup = findViewById(R.id.btnGoogleSignup);
        tvSignIn = findViewById(R.id.tvSignIn);

        // Immediate hide for smoother entrance
        View topFade = findViewById(R.id.topFadeView);
        if (topFade != null) {
            topFade.setAlpha(0f);
            topFade.setTranslationY(-30f);
        }
        LinearLayout mainContent = findViewById(R.id.signupMainContent);
        if (mainContent != null) {
            for (int i = 0; i < mainContent.getChildCount(); i++) {
                mainContent.getChildAt(i).setAlpha(0f);
                mainContent.getChildAt(i).setTranslationY(40f);
            }
        }

        btnSignup.setOnClickListener(v -> registerUser());
        btnGoogleSignup.setOnClickListener(v -> signInWithGoogle());
        tvSignIn.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, R.anim.slide_in_right);
        });

        // Small delay to allow activity transition to stabilize
        findViewById(android.R.id.content).postDelayed(this::runEntranceAnimations, 100);
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
        
        LinearLayout mainContent = findViewById(R.id.signupMainContent);
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

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty()) { etName.setError("Name is required"); return; }
        if (email.isEmpty()) { etEmail.setError("Email is required"); return; }
        if (password.isEmpty()) { etPassword.setError("Password is required"); return; }
        if (password.length() < 6) { etPassword.setError("Password must be at least 6 characters"); return; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email"); return;
        }

        showProgress("Creating account...");
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(userId, name, email);
                    } else {
                        hideProgress();
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
                            String name = account.getDisplayName() != null ? account.getDisplayName() : "Neural Architect";
                            String email = account.getEmail() != null ? account.getEmail() : user.getEmail();
                            saveUserToFirestore(user.getUid(), name, email);
                        }
                    } else {
                        hideProgress();
                        Toast.makeText(this, "Auth failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId).set(user)
                .addOnCompleteListener(task -> {
                    hideProgress();
                    if (task.isSuccessful()) {
                        SharedPreferences prefs = getSharedPreferences("MindForgePrefs", MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("isLoggedIn", true)
                                .putString("userName", name)
                                .putString("userEmail", email)
                                .apply();

                        Toast.makeText(this, "Account Created Successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignupActivity.this, MainActivity.class));
                        finishAffinity();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } else {
                        Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show();
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
