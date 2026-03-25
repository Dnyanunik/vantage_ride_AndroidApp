package com.example.vantageride2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

public class NoInternetFragment extends Fragment {

    public NoInternetFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // This links to the layout we created earlier!
        return inflater.inflate(R.layout.fragment_no_internet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton retryButton = view.findViewById(R.id.button_retry);

        // When the user clicks Retry, tell MainActivity to check the network again
        retryButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).checkNetworkAndLoadData();
            }
        });
    }
}