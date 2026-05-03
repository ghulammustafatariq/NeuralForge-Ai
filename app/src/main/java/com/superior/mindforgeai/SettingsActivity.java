package com.superior.mindforgeai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etApiKey;
    private SwitchMaterial switchDarkMode;
    private AutoCompleteTextView dropdownStyle;
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;
    private View island;
    private NestedScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("MindForgePrefs", MODE_PRIVATE);

        island = findViewById(R.id.settings_island);
        scrollView = findViewById(R.id.settingsScrollView);
        
        // Immediate hide to prevent flicker before animation
        if (island != null) island.setAlpha(0f);
        if (scrollView != null) {
            View content = scrollView.getChildAt(0);
            if (content instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) content;
                for (int i = 0; i < group.getChildCount(); i++) {
                    group.getChildAt(i).setAlpha(0f);
                }
            }
        }

        handleWindowInsets();

        ImageButton btnBack = findViewById(R.id.btnBack);
        etName = findViewById(R.id.etSettingsName);
        etEmail = findViewById(R.id.etSettingsEmail);
        etApiKey = findViewById(R.id.etApiKey);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        dropdownStyle = findViewById(R.id.dropdownStyle);
        MaterialButton btnSave = findViewById(R.id.btnSaveSettings);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        String[] styles = {"Balanced", "Academic", "Intuitive", "Entry Level"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, styles);
        dropdownStyle.setAdapter(adapter);

        loadSettings();

        btnBack.setOnClickListener(v -> finish());

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("darkMode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        btnSave.setOnClickListener(v -> {
            saveSettings();
            Toast.makeText(this, "Neural configuration synchronized", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnLogout.setOnClickListener(v -> {
            prefs.edit().putBoolean("isLoggedIn", false).apply();
            mAuth.signOut();

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
            googleSignInClient.signOut();

            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Small delay to allow activity transition to stabilize
        island.postDelayed(this::runEntranceAnimations, 150);
    }

    private void handleWindowInsets() {
        View root = findViewById(R.id.settingsRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
                
                if (island != null) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) island.getLayoutParams();
                    lp.topMargin = systemBars.top + (int)(24 * getResources().getDisplayMetrics().density);
                    island.setLayoutParams(lp);
                }
                return insets;
            });
        }
    }

    private void runEntranceAnimations() {
        if (isFinishing()) return;

        // Animate Dynamic Island (Fall Down)
        if (island != null) {
            island.setTranslationY(-50f);
            island.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(500)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
        
        // Animate ScrollView Content (Fade In Up)
        if (scrollView != null) {
            View content = scrollView.getChildAt(0);
            if (content instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) content;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    child.setTranslationY(30f);
                    child.animate()
                            .translationY(0)
                            .alpha(1f)
                            .setDuration(600)
                            .setStartDelay(100 + (i * 80))
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                }
            }
        }
    }

    private void loadSettings() {
        etName.setText(prefs.getString("userName", "Neural Architect"));
        etEmail.setText(prefs.getString("userEmail", "architect@forge.ai"));

        String apiKey = prefs.getString("apiKey", "");
        if (apiKey.isEmpty()) {
            apiKey = "sk-c63f425226ce4ffba7b80c3daa758105";
        }
        etApiKey.setText(apiKey);

        switchDarkMode.setChecked(prefs.getBoolean("darkMode", false));
        dropdownStyle.setText(prefs.getString("defaultStyle", "Balanced"), false);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("userName", etName.getText().toString().trim());
        editor.putString("userEmail", etEmail.getText().toString().trim());
        editor.putString("apiKey", etApiKey.getText().toString().trim());
        editor.putBoolean("darkMode", switchDarkMode.isChecked());
        editor.putString("defaultStyle", dropdownStyle.getText().toString());
        editor.apply();
    }
}
