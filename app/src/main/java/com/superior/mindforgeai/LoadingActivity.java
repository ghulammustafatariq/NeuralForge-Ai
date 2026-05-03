package com.superior.mindforgeai;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LoadingActivity extends AppCompatActivity {

    private static final String TAG = "LoadingActivity";

    private ShimmerFrameLayout shimmerContainer;
    private TextView tvLoadingStatus;
    private AppDatabase db;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    private String topic, description, style;
    private int depth;
    private boolean includeCode, includeMath, includeVisuals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loading);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);

        View loadingRoot = findViewById(R.id.loadingRoot);
        if (loadingRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(loadingRoot, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        shimmerContainer = findViewById(R.id.shimmer_view_container);
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus);

        db = AppDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        extractParams();
        runEntranceAnimations();
        startForging();
    }

    private void runEntranceAnimations() {
        Animation fallDown = AnimationUtils.loadAnimation(this, R.anim.fall_down);
        Animation fadeInUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);

        findViewById(R.id.progress_island).startAnimation(fallDown);
        findViewById(R.id.animationContainer).startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up));
        
        LinearLayout textGroup = findViewById(R.id.textGroup);
        for (int i = 0; i < textGroup.getChildCount(); i++) {
            View child = textGroup.getChildAt(i);
            child.setAlpha(0);
            int delay = 300 + (i * 100);
            child.postDelayed(() -> {
                child.setAlpha(1);
                child.startAnimation(fadeInUp);
            }, delay);
        }

        // Pulse animation for the center core
        View pulseView = findViewById(R.id.pulseView);
        if (pulseView != null) {
            ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(
                    pulseView,
                    PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.4f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.4f),
                    PropertyValuesHolder.ofFloat("alpha", 0.1f, 0.0f)
            );
            pulse.setDuration(1500);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.start();
        }
    }

    private void extractParams() {
        Intent intent = getIntent();
        topic = intent.getStringExtra("TOPIC");
        description = intent.getStringExtra("DESCRIPTION");
        style = intent.getStringExtra("STYLE");
        depth = intent.getIntExtra("DEPTH", 50);
        includeCode = intent.getBooleanExtra("INCLUDE_CODE", false);
        includeMath = intent.getBooleanExtra("INCLUDE_MATH", false);
        includeVisuals = intent.getBooleanExtra("INCLUDE_VISUALS", false);

        if (topic == null || topic.isEmpty()) topic = "General Knowledge";
        if (style == null || style.isEmpty()) style = "Beginner";
        if (description == null) description = "";
    }

    private void startForging() {
        shimmerContainer.startShimmer();
        if (tvLoadingStatus != null) {
            tvLoadingStatus.setText("Synchronizing neural pathways...");
        }

        DeepSeekService service = new DeepSeekService();
        service.forgeContent(this, topic, description, style, depth,
                includeCode, includeMath, includeVisuals, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "DeepSeek API call failed", e);
                        runOnUiThread(() -> {
                            shimmerContainer.stopShimmer();
                            Toast.makeText(LoadingActivity.this,
                                    "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "API response received, length: " + responseBody.length());

                        try {
                            if (!response.isSuccessful()) {
                                runOnUiThread(() -> {
                                    shimmerContainer.stopShimmer();
                                    Toast.makeText(LoadingActivity.this,
                                            "API error: " + response.code() + " - " + response.message(),
                                            Toast.LENGTH_LONG).show();
                                    finish();
                                });
                                return;
                            }

                            org.json.JSONObject jsonResponse = new org.json.JSONObject(responseBody);
                            String content = jsonResponse
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");

                            String rawContent = content.trim();
                            if (rawContent.startsWith("```json")) rawContent = rawContent.substring(7);
                            if (rawContent.startsWith("```")) rawContent = rawContent.substring(3);
                            if (rawContent.endsWith("```")) rawContent = rawContent.substring(0, rawContent.length() - 3);
                            rawContent = rawContent.trim();

                            Log.d(TAG, "Parsed content: " + rawContent.substring(0, Math.min(100, rawContent.length())));

                            String finalContent = rawContent;
                            String docId = UUID.randomUUID().toString();
                            String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";

                            saveToLocalAndCloud(docId, uid, topic, description, style, depth, finalContent);

                            runOnUiThread(() -> {
                                shimmerContainer.stopShimmer();
                                Intent intent = new Intent(LoadingActivity.this, ContentActivity.class);
                                intent.putExtra("TOPIC", topic);
                                intent.putExtra("AI_RESULT", finalContent);
                                intent.putExtra("DOC_ID", docId);
                                startActivity(intent);
                                finish();
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse API response", e);
                            runOnUiThread(() -> {
                                shimmerContainer.stopShimmer();
                                Toast.makeText(LoadingActivity.this,
                                        "Failed to parse response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            });
                        } finally {
                            response.close();
                        }
                    }
                });
    }

    private void saveToLocalAndCloud(String docId, String uid, String topic, String desc,
                                     String style, int depth, String aiResult) {
        long now = System.currentTimeMillis();

        HistoryEntity entity = new HistoryEntity(docId, uid, topic, desc, style, depth, aiResult, now);

        new Thread(() -> {
            try {
                db.historyDao().insert(entity);
                Log.d(TAG, "Saved to Room: " + docId);
            } catch (Exception e) {
                Log.e(TAG, "Room save failed", e);
            }
        }).start();

        Map<String, Object> doc = new HashMap<>();
        doc.put("id", docId);
        doc.put("uid", uid);
        doc.put("topic", topic);
        doc.put("description", desc);
        doc.put("style", style);
        doc.put("depth", depth);
        doc.put("aiResult", aiResult);
        doc.put("createdAt", now);

        firestore.collection("users").document(uid)
                .collection("history").document(docId)
                .set(doc)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved to Firestore: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Firestore save failed", e));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shimmerContainer != null) shimmerContainer.startShimmer();
    }

    @Override
    protected void onPause() {
        if (shimmerContainer != null) shimmerContainer.stopShimmer();
        super.onPause();
    }
}
