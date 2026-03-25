package com.example.vantageride2;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ManageFleetFragment extends Fragment {

    private TextInputEditText etUnitName, etTariff, etMinPkg, etDescription;
    private AutoCompleteTextView actvClassification;
    private MaterialButton btnPublish;
    private ImageView ivVehiclePreview;
    private LinearLayout layoutUploadPlaceholder;

    private Uri selectedImageUri;
    private final OkHttpClient client = new OkHttpClient();
    private String currentUserId, currentToken;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivVehiclePreview.setImageURI(uri);
                    ivVehiclePreview.setVisibility(View.VISIBLE);
                    layoutUploadPlaceholder.setVisibility(View.GONE);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_fleet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etUnitName = view.findViewById(R.id.et_unit_name);
        actvClassification = view.findViewById(R.id.actv_classification);
        etTariff = view.findViewById(R.id.et_tariff);
        etMinPkg = view.findViewById(R.id.et_min_pkg);
        etDescription = view.findViewById(R.id.et_description);
        btnPublish = view.findViewById(R.id.btn_publish_fleet);
        ivVehiclePreview = view.findViewById(R.id.iv_vehicle_preview);
        layoutUploadPlaceholder = view.findViewById(R.id.layout_upload_placeholder);
        MaterialCardView cardUpload = view.findViewById(R.id.card_upload_image);

        SharedPreferences prefs = requireActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("USER_ID", "");
        currentToken = prefs.getString("ACCESS_TOKEN", "");

        String[] carTypes = {"Hatchback", "Premium Sedan", "SUV (7 Seater)", "Luxury SUV"};
        actvClassification.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, carTypes));

        cardUpload.setOnClickListener(v -> mGetContent.launch("image/*"));

        btnPublish.setOnClickListener(v -> startInitializationProcess());
    }

    private void startInitializationProcess() {
        String name = etUnitName.getText().toString().trim();
        String type = actvClassification.getText().toString().trim();
        String tariff = etTariff.getText().toString().trim();

        if (name.isEmpty() || type.isEmpty() || tariff.isEmpty() || selectedImageUri == null) {
            Toast.makeText(getContext(), "All fields including Optic Scan (Image) are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPublish.setEnabled(false);
        btnPublish.setText("UPLOADING IMAGE...");

        uploadImageToStorage(name, type, tariff);
    }

    private void uploadImageToStorage(String name, String type, String tariff) {
        // Create a unique file name
        String fileName = "fleet_" + System.currentTimeMillis() + ".jpg";

        // Supabase Storage Upload URL
        String uploadUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/fleet_images/vehicles/" + fileName;

        try {
            // Read image bytes
            InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedImageUri);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            RequestBody requestBody = RequestBody.create(bytes, MediaType.parse("image/jpeg"));

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + currentToken)
                    .addHeader("Content-Type", "image/jpeg")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    resetButton("Upload Failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // Success! Generate the PUBLIC URL for the database
                        String publicUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/fleet_images/vehicles/" + fileName;
                        saveFleetManifestToDb(name, type, tariff, publicUrl);
                    } else {
                        resetButton("Storage Error: " + response.code());
                    }
                }
            });

        } catch (Exception e) {
            resetButton("File Processing Error");
        }
    }

    private void saveFleetManifestToDb(String name, String type, String tariff, String publicImageUrl) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> btnPublish.setText("SYNCING MANIFEST..."));
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("driver_id", currentUserId);
            jsonBody.put("name", name);
            jsonBody.put("type", type);
            jsonBody.put("rate_per_km", Double.parseDouble(tariff));
            jsonBody.put("min_package_km", Integer.parseInt(etMinPkg.getText().toString()));
            jsonBody.put("description", etDescription.getText().toString());
            jsonBody.put("image_url", publicImageUrl); // 🚀 This is the real web URL now
            jsonBody.put("is_active", true);
        } catch (Exception e) { e.printStackTrace(); }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(SupabaseConfig.SUPABASE_URL + "/rest/v1/fleet")
                .post(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                resetButton("Database Error: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Unit Initialized in Network!", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    });
                } else {
                    resetButton("DB Sync Failed: " + response.code());
                }
            }
        });
    }

    private void resetButton(String msg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                btnPublish.setEnabled(true);
                btnPublish.setText("INITIALIZE UNIT (PUBLISH)");
            });
        }
    }
}