package com.example.vantageride2;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;

public class AboutUsFragment extends DialogFragment {

    public AboutUsFragment() {
        // Required empty public constructor
    }

    // This makes the dialog look nice and wide instead of squished
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about_us, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvVersion = view.findViewById(R.id.tv_app_version);
        MaterialButton btnClose = view.findViewById(R.id.btn_close_about);

        // 🚀 DYNAMIC VERSION TRICK: Automatically grab the version from your build.gradle
        try {
            if (getActivity() != null) {
                PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                String version = pInfo.versionName;
                tvVersion.setText("Version " + version);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            tvVersion.setText("Version 1.0.0"); // Fallback just in case
        }

        // Close the dialog when clicked
        btnClose.setOnClickListener(v -> dismiss());
    }
}