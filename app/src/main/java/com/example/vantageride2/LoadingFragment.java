package com.example.vantageride2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class LoadingFragment extends Fragment {

    public LoadingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // This inflates the XML with the "VANTAGE RIDE" text and skeleton cards
        return inflater.inflate(R.layout.fragment_loading, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Find the main skeleton container from your XML
        View skeletonContainer = view.findViewById(R.id.skeleton_container);

        if (skeletonContainer != null) {
            // 2. Create the Heartbeat "Pulse" effect (Fading in and out)
            // 1.0f is fully visible, 0.4f is partially transparent
            AlphaAnimation pulseAnimation = new AlphaAnimation(1.0f, 0.4f);

            pulseAnimation.setDuration(800); // Speed of the pulse (0.8 seconds)
            pulseAnimation.setRepeatCount(Animation.INFINITE); // Keep pulsing forever
            pulseAnimation.setRepeatMode(Animation.REVERSE); // Fade out then fade back in

            // 3. Start the animation on the whole container
            skeletonContainer.startAnimation(pulseAnimation);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up animation when fragment is removed to save memory
        View skeletonContainer = getView() != null ? getView().findViewById(R.id.skeleton_container) : null;
        if (skeletonContainer != null) {
            skeletonContainer.clearAnimation();
        }
    }
}