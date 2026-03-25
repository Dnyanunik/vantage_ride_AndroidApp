package com.example.vantageride2;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileFragment extends Fragment {

    private TextInputEditText etUsername, etFullName, etPhone, etCarName;
    private TextView tvRoleBadge, tvVerificationBadge;
    private LinearLayout layoutCarName;
    private MaterialButton btnSave;
    private ImageView ivAvatar;
    private FloatingActionButton fabChangePhoto;
    private OkHttpClient client;

    private static final String PREF_NAME = "FleetPrefs";
    private static final String KEY_ACCESS_TOKEN = "ACCESS_TOKEN";

    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout layoutSkeletonProfile, layoutProfileContent;

    private String currentUserId = "";
    private String currentAccessToken = "";

    private ActivityResultLauncher<String> imagePickerLauncher;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        client = new OkHttpClient();

        // 1. Init Data Views
        etUsername = view.findViewById(R.id.et_username);
        etFullName = view.findViewById(R.id.et_full_name);
        etPhone = view.findViewById(R.id.et_phone);
        etCarName = view.findViewById(R.id.et_car_name);
        tvRoleBadge = view.findViewById(R.id.tv_role_badge);
        tvVerificationBadge = view.findViewById(R.id.tv_verification_badge);
        layoutCarName = view.findViewById(R.id.layout_car_name);
        btnSave = view.findViewById(R.id.btn_save_profile);
        ivAvatar = view.findViewById(R.id.iv_avatar);
        fabChangePhoto = view.findViewById(R.id.fab_change_photo);

        // 2. Init Refresh & Skeleton Views
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_profile);
        layoutSkeletonProfile = view.findViewById(R.id.layout_skeleton_profile);
        layoutProfileContent = view.findViewById(R.id.layout_profile_content);

        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);

        // 3. Setup Image Picker Contract
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        Toast.makeText(getContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
                        uploadImageToSupabase(uri);
                    }
                }
        );

        // 4. Listeners
        swipeRefreshLayout.setOnRefreshListener(() -> verifyUserAndFetchProfile(false));
        btnSave.setOnClickListener(v -> saveProfileData());

        // Launch gallery picker when FAB is clicked
        fabChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // 5. Initial Load (Show Skeleton)
        verifyUserAndFetchProfile(true);
    }

    private void verifyUserAndFetchProfile(boolean showSkeleton) {
        if (showSkeleton) showLoadingState();

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentAccessToken = prefs.getString(KEY_ACCESS_TOKEN, null);

        if (currentAccessToken == null || currentAccessToken.isEmpty()) {
            showError("No active session found. Please login again.");
            hideLoadingState();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String authUrl = SupabaseConfig.SUPABASE_URL + "/auth/v1/user";

        Request authRequest = new Request.Builder()
                .url(authUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentAccessToken)
                .build();

        client.newCall(authRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    showError("Network error verifying session.");
                    hideLoadingState();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    requireActivity().runOnUiThread(() -> {
                        showError("Session expired. Please log in again.");
                        hideLoadingState();
                        swipeRefreshLayout.setRefreshing(false);
                    });
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JSONObject userObj = new JSONObject(responseBody);
                    currentUserId = userObj.getString("id");

                    fetchProfileData(currentUserId, currentAccessToken);

                } catch (Exception e) {
                    Log.e("ProfileError", "Auth Parsing error", e);
                    requireActivity().runOnUiThread(() -> {
                        showError("Failed to parse user session.");
                        hideLoadingState();
                        swipeRefreshLayout.setRefreshing(false);
                    });
                }
            }
        });
    }

    private void fetchProfileData(String userId, String accessToken) {
        String profileUrl = SupabaseConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=*";

        Request profileRequest = new Request.Builder()
                .url(profileUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(profileRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    showError("Network error fetching profile.");
                    hideLoadingState();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!isAdded() || response.body() == null) return;

                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseBody);

                        if (jsonArray.length() > 0) {
                            JSONObject profileData = jsonArray.getJSONObject(0);

                            String username = profileData.optString("username", "");
                            String fullName = profileData.optString("full_name", "");
                            String phone = profileData.optString("phone_number", "");
                            String role = profileData.optString("role", "customer");
                            boolean isVerified = profileData.optBoolean("is_verified", false);
                            String carName = profileData.optString("car_name", "");
                            String avatarUrl = profileData.optString("avatar_url", "");

                            requireActivity().runOnUiThread(() -> {
                                etUsername.setText(username);
                                etFullName.setText(fullName);
                                etPhone.setText(phone);
                                tvRoleBadge.setText(role.toUpperCase());

                                if (isVerified) {
                                    tvVerificationBadge.setText("VERIFIED");
                                    tvVerificationBadge.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                                } else {
                                    tvVerificationBadge.setText("UNVERIFIED");
                                }

                                if ("driver".equalsIgnoreCase(role)) {
                                    layoutCarName.setVisibility(View.VISIBLE);
                                    etCarName.setText(carName);
                                } else {
                                    layoutCarName.setVisibility(View.GONE);
                                }

                                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                    SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                                    prefs.edit().putString("AVATAR_URL", avatarUrl).apply();
                                }

                                loadAvatarImage(avatarUrl);
                                hideLoadingState();
                                swipeRefreshLayout.setRefreshing(false);
                            });
                        }
                    } catch (Exception e) {
                        Log.e("ProfileError", "Profile Parsing error", e);
                        requireActivity().runOnUiThread(() -> {
                            showError("Failed to load profile details.");
                            hideLoadingState();
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }
                }
            }
        });
    }

    // --- UPLOAD IMAGE LOGIC ---
    private void uploadImageToSupabase(Uri imageUri) {
        if (currentUserId.isEmpty() || currentAccessToken.isEmpty()) return;

        byte[] imageBytes = getBytesFromUri(imageUri);
        if (imageBytes == null) {
            showError("Could not process image.");
            return;
        }

        String fileName = currentUserId + "_avatar.jpg";
        String uploadUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/avatars/" + fileName;
        RequestBody body = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentAccessToken)
                .addHeader("Content-Type", "image/jpeg")
                .addHeader("x-upsert", "true")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> showError("Network error. Image upload failed."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Image is in storage. Now tell the database.
                    Log.d("UPLOAD_DEBUG", "Storage upload successful. Linking to profile...");
                    updateProfileAvatarUrl(fileName);
                } else {
                    String errorMsg = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e("UploadError", errorMsg);
                    requireActivity().runOnUiThread(() -> showError("Failed to upload image."));
                }
            }
        }); // <-- THIS WAS THE MISSING BRACKET IN YOUR CODE!
    }

    public void updateProfileAvatarUrl(String newFileName) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("avatar_url", newFileName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
        String updateUrl = SupabaseConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + currentUserId;

        Request request = new Request.Builder()
                .url(updateUrl)
                .patch(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentAccessToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> showError("Image uploaded, but profile link failed."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                requireActivity().runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Avatar updated!", Toast.LENGTH_SHORT).show();

                        // 1. Update the image in the fragment
                        loadAvatarImage(newFileName);

                        // 2. Save to SharedPreferences for the drawer
                        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                        prefs.edit().putString("AVATAR_URL", newFileName).apply();

                        // 3. Trigger MainActivity to update the Drawer Header instantly
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).refreshHeaderAvatar();
                        }
                    } else {
                        showError("Failed to link avatar to profile.");
                    }
                });
            }
        });
    }

    private byte[] getBytesFromUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadAvatarImage(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            String finalUrl = avatarUrl;
            if (!avatarUrl.startsWith("http")) {
                finalUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/avatars/" + avatarUrl;
            }
            finalUrl += "?t=" + System.currentTimeMillis();

            Glide.with(this)
                    .load(finalUrl)
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_report_image)
                    .circleCrop()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(ivAvatar);
        } else {
            ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }
    }

    private void saveProfileData() {
        if (currentUserId.isEmpty() || currentAccessToken.isEmpty()) {
            showError("Cannot save: Missing user session.");
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("SAVING...");

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("username", etUsername.getText().toString().trim());
            jsonBody.put("full_name", etFullName.getText().toString().trim());
            jsonBody.put("phone_number", etPhone.getText().toString().trim());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
        String updateUrl = SupabaseConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + currentUserId;

        Request request = new Request.Builder()
                .url(updateUrl)
                .patch(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentAccessToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("SAVE CHANGES");
                    showError("Network error. Could not save changes.");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                requireActivity().runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("SAVE CHANGES");

                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                        prefs.edit().putString("USER_NAME", etUsername.getText().toString().trim()).apply();
                    } else {
                        showError("Failed to update profile. Database Error.");
                    }
                });
            }
        });
    }

    private void showError(String message) {
        if (getActivity() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoadingState() {
        layoutSkeletonProfile.setVisibility(View.VISIBLE);
        layoutProfileContent.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        layoutSkeletonProfile.setVisibility(View.GONE);
        layoutProfileContent.setVisibility(View.VISIBLE);
    }
}