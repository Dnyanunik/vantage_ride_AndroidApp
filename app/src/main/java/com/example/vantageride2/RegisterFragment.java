package com.example.vantageride2;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.vantageride2.databinding.FragmentRegisterBinding;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private OkHttpClient client;
    private String selectedRole = "customer"; // Default

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        client = new OkHttpClient();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. ROLE TOGGLE LOGIC
        binding.btnRoleCustomer.setOnClickListener(v -> setRole("customer"));
        binding.btnRoleDriver.setOnClickListener(v -> setRole("driver"));

        // 2. SUBMIT BUTTON
        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String username = binding.etUsername.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String phone = binding.etPhone.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            // Validate Base Data
            if (name.isEmpty() || username.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "All primary fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Driver Specific Variables
            String licenseNumber = "";
            String vehicleModel = "";
            String plateNumber = "";

            // Validate Driver Data if Driver role is selected
            if (selectedRole.equals("driver")) {
                licenseNumber = binding.etLicenseNumber.getText().toString().trim();
                vehicleModel = binding.etVehicleModel.getText().toString().trim();
                plateNumber = binding.etPlateNumber.getText().toString().trim();

                if (licenseNumber.isEmpty() || vehicleModel.isEmpty() || plateNumber.isEmpty()) {
                    Toast.makeText(getContext(), "Vehicle Manifest fields are required for Drivers", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            binding.btnRegister.setText("ENLISTING...");
            binding.btnRegister.setEnabled(false);

            registerUserInSupabase(name, username, email, phone, password, selectedRole, licenseNumber, vehicleModel, plateNumber);
        });

        // 3. GO TO LOGIN
        binding.tvGoToLogin.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().finish();
        });
    }

    private void setRole(String role) {
        selectedRole = role;

        // Reset both buttons
        dimButton(binding.btnRoleCustomer);
        dimButton(binding.btnRoleDriver);

        // Highlight selected button and manage layout visibility
        if (role.equals("customer")) {
            highlightButton(binding.btnRoleCustomer);
            binding.llDriverFields.setVisibility(View.GONE); // Hide Driver fields

            // Clear out driver fields so stale data isn't accidentally sent
            binding.etLicenseNumber.setText("");
            binding.etVehicleModel.setText("");
            binding.etPlateNumber.setText("");

        } else if (role.equals("driver")) {
            highlightButton(binding.btnRoleDriver);
            binding.llDriverFields.setVisibility(View.VISIBLE); // Show Driver fields
        }
    }

    private void highlightButton(MaterialButton btn) {
        btn.setTextColor(Color.parseColor("#00E5FF"));
        btn.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#00E5FF")));
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A00E5FF")));
    }

    private void dimButton(MaterialButton btn) {
        btn.setTextColor(Color.parseColor("#8892A3"));
        btn.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")));
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
    }

    private void registerUserInSupabase(String name, String username, String email, String phone, String password,
                                        String role, String license, String model, String plate) {
        JSONObject jsonBody = new JSONObject();
        JSONObject metadata = new JSONObject();
        try {
            // Base profile data
            metadata.put("full_name", name);
            metadata.put("username", username);
            metadata.put("phone_number", phone);
            metadata.put("role", role);

            // If it's a driver, inject the driver_details into the metadata
            if (role.equals("driver")) {
                metadata.put("license_number", license);
                metadata.put("vehicle_model", model);
                metadata.put("vehicle_plate", plate);
            }

            jsonBody.put("email", email);
            jsonBody.put("password", password);
            jsonBody.put("data", metadata);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        String authUrl = SupabaseConfig.SUPABASE_URL + "/auth/v1/signup";

        Request request = new Request.Builder()
                .url(authUrl)
                .post(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUIThread(() -> {
                    Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
                    resetButton();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body().string();

                runOnUIThread(() -> {
                    resetButton();
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Registration Successful!", Toast.LENGTH_SHORT).show();
                        Log.d("SUPABASE_SIGNUP", "Success: " + responseData);
                        if (getActivity() != null) getActivity().finish();
                    } else {
                        Toast.makeText(getContext(), "Registration Failed. Check details.", Toast.LENGTH_LONG).show();
                        Log.e("SUPABASE_SIGNUP", "Error: " + responseData);
                    }
                });
            }
        });
    }

    private void runOnUIThread(Runnable action) {
        if (getActivity() != null) {
            new Handler(Looper.getMainLooper()).post(action);
        }
    }

    private void resetButton() {
        binding.btnRegister.setText("ENLIST IN FLEET");
        binding.btnRegister.setEnabled(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}