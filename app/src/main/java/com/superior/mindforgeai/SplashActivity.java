package com.superior.mindforgeai;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("MindForgePrefs", MODE_PRIVATE);
        if (prefs.getBoolean("darkMode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        setupAnimatedPattern();

        // Entrance Animations for Logo and Text
        View logo = findViewById(R.id.splashLogo);
        View title = findViewById(R.id.splashTitle);
        View subtitle = findViewById(R.id.splashSubtitle);

        if (logo != null) {
            logo.setAlpha(0f);
            logo.setScaleX(0.7f);
            logo.setScaleY(0.7f);
            logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(1200).setStartDelay(200).start();
        }

        if (title != null) {
            title.setAlpha(0f);
            title.setTranslationY(40f);
            title.animate().alpha(1f).translationY(0f).setDuration(1000).setStartDelay(500).start();
        }

        if (subtitle != null) {
            subtitle.setAlpha(0f);
            subtitle.animate().alpha(0.8f).setDuration(1000).setStartDelay(800).start();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
            
            if (currentUser != null || isLoggedIn) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 3000);
    }

    private void setupAnimatedPattern() {
        final ImageView patternView = findViewById(R.id.patternBackground);
        if (patternView == null) return;

        patternView.post(() -> {
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.bg_dot_pattern);
            if (drawable == null) return;

            int size = (int) (20 * getResources().getDisplayMetrics().density);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);

            patternView.setImageBitmap(createLargeTiledBitmap(bitmap, 2000, 2000));
            
            ValueAnimator animator = ValueAnimator.ofFloat(0f, -size * 5f);
            animator.setDuration(10000);
            animator.setInterpolator(new LinearInterpolator());
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.RESTART);
            animator.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                patternView.setTranslationX(value);
                patternView.setTranslationY(value);
            });
            animator.start();
        });
    }

    private Bitmap createLargeTiledBitmap(Bitmap tile, int width, int height) {
        Bitmap largeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(largeBitmap);
        Paint paint = new Paint();
        BitmapShader shader = new BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        paint.setShader(shader);
        canvas.drawRect(0, 0, width, height, paint);
        return largeBitmap;
    }
}
