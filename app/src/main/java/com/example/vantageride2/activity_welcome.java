package com.example.vantageride2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vantageride2.databinding.ActivityWelcomeBinding;

public class activity_welcome extends AppCompatActivity {

    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Standard ViewBinding setup
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Prepare views for animation (Hide them initially)
        binding.tvHeadline.setAlpha(0f);
        binding.tvHeadline.setTranslationY(50f);
        binding.tvSubtitle.setAlpha(0f);
        binding.tvSubtitle.setTranslationY(50f);
        binding.imgCar.setTranslationX(1000f);
        binding.imgCar.setAlpha(0f);
        binding.btnStartJourney.setAlpha(0f);
        binding.btnStartJourney.setScaleX(0.8f);
        binding.btnStartJourney.setScaleY(0.8f);

        // 2. Trigger the Entrance Animations
        triggerEntranceAnimations();

        View navHost = findViewById(R.id.nav_host_fragment_content_activity_welcome);

        // 3. Click Listeners

        // "Start Your Journey" -> Opens MainActivity
        binding.btnStartJourney.setOnClickListener(v -> {
            Intent intent = new Intent(activity_welcome.this, MainActivity.class);
            startActivity(intent);
        });

        // "Login" Text -> Fades out welcome UI, fades in Login UI
        binding.tvLoginAction.setOnClickListener(v -> {
            // Fade out the welcome text/buttons
            fadeWelcomeUI(0f);

            // Show and fade in the Login Fragment container over the car
            navHost.setAlpha(0f);
            navHost.setVisibility(View.VISIBLE);
            navHost.animate().alpha(1f).setDuration(500).start();
        });

        // 4. Custom Back Button Logic (Crucial for UX)
        OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navHost != null && navHost.getVisibility() == View.VISIBLE) {
                    // Login screen is visible! Reverse the animations.
                    navHost.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                        navHost.setVisibility(View.GONE);
                    }).start();

                    // Fade the welcome text/buttons back in
                    fadeWelcomeUI(1f);
                } else {
                    // Login screen is NOT visible. Let the app close normally.
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);

        // 5. Tap outside to close the Login Fragment
        navHost.setOnClickListener(v -> {
            // Trigger the exact same logic as the system back button!
            if (navHost.getVisibility() == View.VISIBLE) {
                backCallback.handleOnBackPressed();
            }
        });
    }

    // -------------------------------------------------------------------
    // HELPER METHODS
    // -------------------------------------------------------------------

    /**
     * Smoothly fades the Welcome text and buttons in or out.
     * @param targetAlpha 0f to hide, 1f to show.
     */
    private void fadeWelcomeUI(float targetAlpha) {
        int duration = 300;
        binding.tvHeadline.animate().alpha(targetAlpha).setDuration(duration).start();
        binding.tvSubtitle.animate().alpha(targetAlpha).setDuration(duration).start();
        binding.btnStartJourney.animate().alpha(targetAlpha).setDuration(duration).start();
        binding.tvLoginPrompt.animate().alpha(targetAlpha).setDuration(duration).start();
        binding.tvLoginAction.animate().alpha(targetAlpha).setDuration(duration).start();
        binding.llLogo.animate().alpha(targetAlpha).setDuration(duration).start();
        binding.viewIndicatorActive.animate().alpha(targetAlpha).setDuration(duration).start();
        binding.viewIndicatorInactive.animate().alpha(targetAlpha).setDuration(duration).start();

        // UX Detail: Disable clicking on these items when they are invisible
        boolean isVisible = targetAlpha > 0.5f;
        binding.btnStartJourney.setClickable(isVisible);
        binding.tvLoginAction.setClickable(isVisible);
    }

    private void triggerEntranceAnimations() {
        binding.tvHeadline.animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(100).setInterpolator(new DecelerateInterpolator()).start();
        binding.tvSubtitle.animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(300).setInterpolator(new DecelerateInterpolator()).start();
        binding.imgCar.animate().alpha(1f).translationX(0f).setDuration(1200).setStartDelay(400).setInterpolator(new DecelerateInterpolator()).start();
        binding.btnStartJourney.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).setStartDelay(800).setInterpolator(new OvershootInterpolator()).start();
    }
}