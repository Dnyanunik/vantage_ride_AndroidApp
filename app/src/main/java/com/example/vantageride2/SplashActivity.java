package com.example.vantageride2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Make the splash screen truly full-screen (Edge-to-Edge)
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // Hide the top Action Bar just to be safe
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Adjust padding so content doesn't get hidden behind the notch or navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. Find the skeleton container
        View skeletonContainer = findViewById(R.id.skeleton_container);

        // Safety check: Only animate if the container exists in the XML
        if (skeletonContainer != null) {
            // Create a "Pulse" animation to make the placeholders breathe
            AlphaAnimation pulseAnimation = new AlphaAnimation(1.0f, 0.4f); // Dips down to 40% opacity
            pulseAnimation.setDuration(800); // 0.8 seconds per fade
            pulseAnimation.setRepeatCount(Animation.INFINITE);
            pulseAnimation.setRepeatMode(Animation.REVERSE);

            // Start the animation
            skeletonContainer.startAnimation(pulseAnimation);
        }

        // 3. Wait for 3 seconds, then move to MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            if (skeletonContainer != null) {
                // Stop the animation before leaving to free up memory
                skeletonContainer.clearAnimation();
            }

            // Navigate to the main app
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);

            // Close the splash screen so the user can't press 'back' to see it again
            finish();

            // Add a smooth fade transition between screens
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        }, SPLASH_DURATION);
    }
}