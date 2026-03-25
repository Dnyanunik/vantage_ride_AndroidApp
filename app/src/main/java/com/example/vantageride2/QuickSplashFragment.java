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

public class QuickSplashFragment extends Fragment {

    public QuickSplashFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // We reuse the Splash XML for branding consistency
        return inflater.inflate(R.layout.activity_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the skeleton container inside activity_splash.xml
        View skeletonContainer = view.findViewById(R.id.skeleton_container);

        if (skeletonContainer != null) {
            // Apply the same "Pulse" so the transition is seamless
            AlphaAnimation pulse = new AlphaAnimation(1.0f, 0.4f);
            pulse.setDuration(800);
            pulse.setRepeatCount(Animation.INFINITE);
            pulse.setRepeatMode(Animation.REVERSE);
            skeletonContainer.startAnimation(pulse);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop animation to save CPU when the fragment is swapped out for the Fleet
        if (getView() != null) {
            View skeletonContainer = getView().findViewById(R.id.skeleton_container);
            if (skeletonContainer != null) {
                skeletonContainer.clearAnimation();
            }
        }
    }
}