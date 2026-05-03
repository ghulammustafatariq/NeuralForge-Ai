package com.superior.mindforgeai;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etTopic, etDescription;
    private MaterialButton btnGenerate;
    private ImageButton btnHistoryIsland, btnSettings, btnTheme;

    private ChipGroup chipGroupStyle, chipGroupFocus;
    private Chip chipBeginner, chipProfessor, chipELI5;
    private Chip chipTheory, chipApplication, chipHistory;
    private Slider sliderDepth;

    private FrameLayout dynamicIsland, profileContainer;
    private LinearLayout islandIcons;
    private NestedScrollView scrollView;
    private TextView tvUsernameIsland;

    private boolean isIslandShrunk = false;
    private int fullIslandWidth = 0;
    private int shrunkIslandWidth = 0;
    private ValueAnimator widthAnimator;

    private boolean animationsStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("MindForgePrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("darkMode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);

        initializeViews();
        
        // Immediate hard reset for entrance state
        if (dynamicIsland != null) {
            dynamicIsland.setAlpha(0f);
            dynamicIsland.setTranslationY(-50f);
        }
        if (btnGenerate != null) {
            btnGenerate.setAlpha(0f);
            btnGenerate.setTranslationY(60f);
        }
        hideScrollViewContent();

        handleWindowInsets();
        setupDynamicIsland();
        setupInteractionListeners();
        loadUserProfile();

        if (savedInstanceState == null) {
            // Wait for activity to be fully visible before starting UI dance
            new Handler(Looper.getMainLooper()).postDelayed(this::runEntranceAnimations, 100);
        } else {
            showAllContentImmediate();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isIslandShrunk && !isFinishing()) shrinkIsland();
        }, 4000);
    }

    private void hideScrollViewContent() {
        if (scrollView != null && scrollView.getChildCount() > 0) {
            View content = scrollView.getChildAt(0);
            if (content instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) content;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    child.setAlpha(0f);
                    child.setTranslationY(40f);
                }
            }
        }
    }

    private void showAllContentImmediate() {
        if (dynamicIsland != null) {
            dynamicIsland.setAlpha(1f);
            dynamicIsland.setTranslationY(0);
        }
        if (btnGenerate != null) {
            btnGenerate.setAlpha(1f);
            btnGenerate.setTranslationY(0);
        }
        if (scrollView != null && scrollView.getChildCount() > 0) {
            View content = scrollView.getChildAt(0);
            if (content instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) content;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    child.setAlpha(1f);
                    child.setTranslationY(0);
                }
            }
        }
    }

    private void runEntranceAnimations() {
        if (animationsStarted || isFinishing()) return;
        animationsStarted = true;

        DecelerateInterpolator interpolator = new DecelerateInterpolator(1.5f);

        // 1. Drop the Dynamic Island
        if (dynamicIsland != null) {
            dynamicIsland.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(700)
                    .setInterpolator(interpolator)
                    .start();
        }
        
        // 2. Staggered Entrance for Content
        if (scrollView != null && scrollView.getChildCount() > 0) {
            View content = scrollView.getChildAt(0);
            if (content instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) content;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    child.animate()
                            .translationY(0)
                            .alpha(1f)
                            .setDuration(600)
                            .setStartDelay(200 + (i * 100))
                            .setInterpolator(interpolator)
                            .start();
                }
            }
        }

        // 3. Slide up the Action Button
        if (btnGenerate != null) {
            btnGenerate.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(800)
                    .setStartDelay(700)
                    .setInterpolator(interpolator)
                    .start();
        }
    }

    private void loadUserProfile() {
        SharedPreferences prefs = getSharedPreferences("MindForgePrefs", MODE_PRIVATE);
        String name = prefs.getString("userName", "Neural Architect");
        if (tvUsernameIsland != null) tvUsernameIsland.setText(name);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && !isFinishing()) {
                            String firestoreName = documentSnapshot.getString("name");
                            if (firestoreName != null && !firestoreName.isEmpty()) {
                                tvUsernameIsland.setText(firestoreName);
                                prefs.edit().putString("userName", firestoreName).apply();
                            }
                        }
                    });
            syncHistoryFromFirestore(uid);
        }
    }

    private void syncHistoryFromFirestore(String uid) {
        AppDatabase db = AppDatabase.getInstance(this);
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("history")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    new Thread(() -> {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            try {
                                String id = doc.getString("id");
                                String topic = doc.getString("topic");
                                String description = doc.getString("description");
                                String style = doc.getString("style");
                                int depth = doc.getLong("depth") != null ? doc.getLong("depth").intValue() : 1;
                                String aiResult = doc.getString("aiResult");
                                long createdAt = doc.getLong("createdAt") != null ? doc.getLong("createdAt") : System.currentTimeMillis();

                                if (id != null && topic != null) {
                                    HistoryEntity entity = new HistoryEntity(id, uid, topic, description != null ? description : "",
                                            style != null ? style : "Beginner", depth, aiResult != null ? aiResult : "", createdAt);
                                    db.historyDao().insert(entity);
                                }
                            } catch (Exception e) {
                                Log.e("MainActivity", "Failed to sync history item", e);
                            }
                        }
                    }).start();
                })
                .addOnFailureListener(e -> Log.e("MainActivity", "History sync failed", e));
    }

    private void handleWindowInsets() {
        View mainRoot = findViewById(R.id.mainRoot);
        if (mainRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);

                if (dynamicIsland != null) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) dynamicIsland.getLayoutParams();
                    lp.topMargin = systemBars.top + (int)(16 * getResources().getDisplayMetrics().density);
                    dynamicIsland.setLayoutParams(lp);
                }
                return insets;
            });
        }
    }

    private void initializeViews() {
        etTopic = findViewById(R.id.etTopic);
        etDescription = findViewById(R.id.etDescription);
        btnGenerate = findViewById(R.id.btnGenerate);
        dynamicIsland = findViewById(R.id.dynamic_island);
        profileContainer = findViewById(R.id.profileContainer);
        islandIcons = findViewById(R.id.island_icons_group);
        scrollView = findViewById(R.id.scrollView);
        tvUsernameIsland = findViewById(R.id.tvUsernameIsland);
        btnTheme = findViewById(R.id.btnTheme);
        btnHistoryIsland = findViewById(R.id.btnHistoryIsland);
        btnSettings = findViewById(R.id.btnSettings);

        chipGroupStyle = findViewById(R.id.chipGroupStyle);
        chipGroupFocus = findViewById(R.id.chipGroupFocus);
        chipBeginner = findViewById(R.id.chipBeginner);
        chipProfessor = findViewById(R.id.chipProfessor);
        chipELI5 = findViewById(R.id.chipELI5);
        chipTheory = findViewById(R.id.chipTheory);
        chipApplication = findViewById(R.id.chipApplication);
        chipHistory = findViewById(R.id.chipHistory);
        sliderDepth = findViewById(R.id.sliderDepth);

        if (chipBeginner != null) chipBeginner.setChecked(true);
    }

    private void setupDynamicIsland() {
        if (dynamicIsland != null) {
            dynamicIsland.post(() -> {
                fullIslandWidth = dynamicIsland.getWidth();
                shrunkIslandWidth = dynamicIsland.getHeight();
            });

            dynamicIsland.setOnClickListener(v -> {
                if (isIslandShrunk) expandIsland(); else shrinkIsland();
            });
        }

        if (scrollView != null) {
            scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (Math.abs(scrollY - oldScrollY) > 20) {
                    if (scrollY > oldScrollY + 40 && !isIslandShrunk) shrinkIsland();
                    else if (scrollY < oldScrollY - 40 && isIslandShrunk) expandIsland();
                }
            });
        }
    }

    private void shrinkIsland() {
        if (dynamicIsland == null || isIslandShrunk || fullIslandWidth == 0) return;
        isIslandShrunk = true;
        animateIsland(fullIslandWidth, shrunkIslandWidth, 0f, true);
    }

    private void expandIsland() {
        if (dynamicIsland == null || !isIslandShrunk || fullIslandWidth == 0) return;
        isIslandShrunk = false;
        animateIsland(shrunkIslandWidth, fullIslandWidth, 1f, false);
    }

    private void animateIsland(int startWidth, int endWidth, float targetAlpha, boolean shrinking) {
        if (widthAnimator != null) widthAnimator.cancel();

        widthAnimator = ValueAnimator.ofInt(startWidth, endWidth);
        widthAnimator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams lp = dynamicIsland.getLayoutParams();
            lp.width = (Integer) animation.getAnimatedValue();
            dynamicIsland.setLayoutParams(lp);
        });

        widthAnimator.setDuration(450);
        widthAnimator.setInterpolator(new DecelerateInterpolator());
        widthAnimator.start();

        float targetScale = shrinking ? 0.75f : 1.0f;
        int duration = 350;

        if (tvUsernameIsland != null) tvUsernameIsland.animate().alpha(targetAlpha).scaleX(targetScale).scaleY(targetScale).setDuration(duration).start();
        if (islandIcons != null) islandIcons.animate().alpha(targetAlpha).scaleX(targetScale).scaleY(targetScale).setDuration(duration).start();
        
        if (profileContainer != null) {
            float translationX = shrinking ? (shrunkIslandWidth / 2f) - (profileContainer.getLeft() + (profileContainer.getWidth() / 2f)) : 0;
            profileContainer.animate().translationX(translationX).setDuration(450).setInterpolator(new DecelerateInterpolator()).start();
        }

        setInternalButtonsEnabled(!shrinking);
    }

    private void setInternalButtonsEnabled(boolean enabled) {
        if (btnTheme != null) btnTheme.setEnabled(enabled);
        if (btnSettings != null) btnSettings.setEnabled(enabled);
        if (btnHistoryIsland != null) btnHistoryIsland.setEnabled(enabled);
    }

    private void setupInteractionListeners() {
        if (btnGenerate != null) {
            btnGenerate.setOnClickListener(v -> {
                String topic = etTopic.getText() != null ? etTopic.getText().toString().trim() : "";
                if (topic.isEmpty()) {
                    etTopic.setError("Enter a concept to forge");
                    return;
                }

                String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
                String style = getSelectedStyle();
                int depthValue = (int) (sliderDepth != null ? sliderDepth.getValue() : 3);
                int depthPercent = depthValue * 20;
                boolean includeCode = chipApplication != null && chipApplication.isChecked();
                boolean includeMath = chipTheory != null && chipTheory.isChecked();

                if (!includeCode && !includeMath) {
                    Toast.makeText(this, "Select at least one Focus Area", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, LoadingActivity.class);
                intent.putExtra("TOPIC", topic);
                intent.putExtra("DESCRIPTION", description);
                intent.putExtra("STYLE", style);
                intent.putExtra("DEPTH", depthPercent);
                intent.putExtra("INCLUDE_CODE", includeCode);
                intent.putExtra("INCLUDE_MATH", includeMath);
                intent.putExtra("INCLUDE_VISUALS", chipHistory != null && chipHistory.isChecked());
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        if (btnTheme != null) {
            btnTheme.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("MindForgePrefs", MODE_PRIVATE);
                boolean current = prefs.getBoolean("darkMode", false);
                prefs.edit().putBoolean("darkMode", !current).apply();

                AppCompatDelegate.setDefaultNightMode(!current ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            });
        }

        if (btnSettings != null) btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        
        if (btnHistoryIsland != null) btnHistoryIsland.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private String getSelectedStyle() {
        if (chipGroupStyle == null) return "Beginner";
        int selectedId = chipGroupStyle.getCheckedChipId();
        if (chipProfessor != null && selectedId == chipProfessor.getId()) return "Professor";
        if (chipELI5 != null && selectedId == chipELI5.getId()) return "ELI5";
        return "Beginner";
    }
}
