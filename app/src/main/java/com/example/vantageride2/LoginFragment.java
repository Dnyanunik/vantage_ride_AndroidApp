package com.example.vantageride2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vantageride2.databinding.FragmentLoginBinding;

import org.json.JSONArray;
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

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private OkHttpClient client;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        client = new OkHttpClient();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please enter credentials", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnLogin.setText("AUTHENTICATING...");
            binding.btnLogin.setEnabled(false);

            loginUserToSupabase(email, password);
        });

        binding.tvGoToSignup.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUserToSupabase(String email, String password) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("password", password);
        } catch (JSONException e) { e.printStackTrace(); }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        String authUrl = SupabaseConfig.SUPABASE_URL + "/auth/v1/token?grant_type=password";

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
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        String accessToken = jsonObject.getString("access_token");
                        String refreshToken = jsonObject.getString("refresh_token");
                        JSONObject userObj = jsonObject.getJSONObject("user");
                        String userId = userObj.getString("id");

                        fetchProfileData(userId, accessToken, refreshToken);

                    } catch (JSONException e) {
                        runOnUIThread(() -> {
                            Toast.makeText(getContext(), "Login Error", Toast.LENGTH_SHORT).show();
                            resetButton();
                        });
                    }
                } else {
                    runOnUIThread(() -> {
                        Toast.makeText(getContext(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
                        resetButton();
                    });
                }
            }
        });
    }

    private void fetchProfileData(String userId, String accessToken, String refreshToken) {
        String profileUrl = SupabaseConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=username,avatar_url,role,full_name";

        Request request = new Request.Builder()
                .url(profileUrl)
                .get()
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUIThread(() -> resetButton());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body().string();
                Log.d("DB_RESPONSE", "Raw JSON: " + responseData);

                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(responseData);
                        if (jsonArray.length() > 0) {
                            JSONObject profileObj = jsonArray.getJSONObject(0);

                            String avatarUrl = profileObj.optString("avatar_url", "");
                            String userName = profileObj.optString("username", "Guest");
                            String fullName = profileObj.optString("full_name", userName);

                            String trueRole = "customer";
                            if (profileObj.has("role") && !profileObj.isNull("role")) {
                                trueRole = profileObj.getString("role").trim();
                            }
                            if (trueRole.isEmpty() || trueRole.equalsIgnoreCase("null")) {
                                trueRole = "customer";
                            }

                            final String finalRole = trueRole;

                            runOnUIThread(() -> {
                                saveSession(accessToken, refreshToken, userId, finalRole, avatarUrl, fullName);

                                if (getActivity() != null) {
                                    Intent intent = new Intent(getActivity(), MainActivity.class);

                                    // 🚀 CHECK ROLE: If admin, pass a flag to MainActivity
                                    if (finalRole.equalsIgnoreCase("admin")) {
                                        intent.putExtra("OPEN_ADMIN_DASHBOARD", true);
                                    }

                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    getActivity().finish();
                                }
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("DB_ERROR", e.getMessage());
                        runOnUIThread(() -> resetButton());
                    }
                }
            }
        });
    }

    private void saveSession(String accessToken, String refreshToken, String userId, String role, String avatarUrl, String fullName) {
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putString("ACCESS_TOKEN", accessToken);
            editor.putString("REFRESH_TOKEN", refreshToken);
            editor.putString("USER_ID", userId);
            editor.putString("USER_ROLE", role);
            editor.putString("USER_NAME", fullName);
            editor.putString("AVATAR_URL", avatarUrl);
            editor.putBoolean("IS_LOGGED_IN", true);
            editor.apply();
        }
    }

    private void runOnUIThread(Runnable action) {
        if (isAdded() && getActivity() != null) {
            new Handler(Looper.getMainLooper()).post(action);
        }
    }

    private void resetButton() {
        if (binding != null) {
            binding.btnLogin.setText("INITIALIZE SESSION");
            binding.btnLogin.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}