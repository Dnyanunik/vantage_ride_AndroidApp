package com.example.vantageride2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RideHistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private LinearLayout layoutLoading;
    private TextView tvEmptyState, tvTitle, tvSubtitle;
    private HistoryAdapter adapter;
    private List<BookingHistoryModel> historyList;

    private OkHttpClient client;
    private String currentUserId;
    private String currentUserRole;
    private String currentToken;

    // 🚀 Memory map to track which table each booking belongs to
    private Map<String, Boolean> rideTypeMap = new HashMap<>();

    public RideHistoryFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ride_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvHistory = view.findViewById(R.id.rv_history);
        layoutLoading = view.findViewById(R.id.layout_loading);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        tvTitle = view.findViewById(R.id.tv_history_title);
        tvSubtitle = view.findViewById(R.id.tv_history_subtitle);

        client = new OkHttpClient();
        loadUserData();

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList, currentUserRole, this);
        rvHistory.setAdapter(adapter);

        if (currentUserRole.equalsIgnoreCase("driver")) {
            tvTitle.setText("DRIVER HISTORY");
            tvSubtitle.setText("Track your accepted and completed missions.");
        } else {
            tvTitle.setText("MY BOOKING HISTORY");
            tvSubtitle.setText("Track your upcoming and past rides.");
        }

        fetchHistoryLogs();
    }

    private void loadUserData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("USER_ID", "");
        currentUserRole = prefs.getString("USER_ROLE", "customer");
        currentToken = prefs.getString("ACCESS_TOKEN", SupabaseConfig.SUPABASE_ANON_KEY);
    }

    private void fetchHistoryLogs() {
        loadUserData();

        if (currentUserId == null || currentUserId.isEmpty()) {
            showEmptyState("User ID missing. Please sign out and sign in again.");
            return;
        }

        layoutLoading.setVisibility(View.VISIBLE);
        rvHistory.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);

        List<BookingHistoryModel> tempList = new ArrayList<>();
        rideTypeMap.clear();

        fetchStandardHistory(tempList);
    }

    // --- 1. FETCH STANDARD BOOKINGS ---
    private void fetchStandardHistory(List<BookingHistoryModel> tempList) {
        String columnToFilter = currentUserRole.equalsIgnoreCase("driver") ? "driver_id" : "customer_id";
        String profileJoinConstraint = currentUserRole.equalsIgnoreCase("driver")
                ? "profiles!bookings_customer_id_fkey"
                : "profiles!bookings_driver_id_fkey";

        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/bookings" +
                "?select=*,contact:" + profileJoinConstraint + "(full_name,phone_number)" +
                "&" + columnToFilter + "=eq." + currentUserId +
                "&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                fetchPackageHistory(tempList); // Try packages even if standard fails
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    parseAndAdd(response.body().string(), tempList, false);
                }
                fetchPackageHistory(tempList); // Chain the next call
            }
        });
    }

    // --- 2. FETCH PACKAGE BOOKINGS ---
    private void fetchPackageHistory(List<BookingHistoryModel> tempList) {
        String columnToFilter = currentUserRole.equalsIgnoreCase("driver") ? "driver_id" : "customer_id";
        String profileJoinConstraint = currentUserRole.equalsIgnoreCase("driver")
                ? "profiles!package_bookings_customer_id_fkey"
                : "profiles!package_bookings_driver_id_fkey";

        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/package_bookings" +
                "?select=*,contact:" + profileJoinConstraint + "(full_name,phone_number)" +
                "&" + columnToFilter + "=eq." + currentUserId +
                "&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                updateUI(tempList);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    parseAndAdd(response.body().string(), tempList, true);
                }
                updateUI(tempList);
            }
        });
    }

    // --- 3. PARSE JSON & MERGE LISTS ---
    private void parseAndAdd(String jsonResponse, List<BookingHistoryModel> tempList, boolean isPackage) {
        try {
            JSONArray array = new JSONArray(jsonResponse);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.getString("id");

                // Track this ID so we know which table it came from
                rideTypeMap.put(id, isPackage);

                String contactName = "Guest Passenger";
                String contactPhone = "--";

                if (obj.has("contact") && !obj.isNull("contact")) {
                    JSONObject contactObj = obj.getJSONObject("contact");
                    contactName = contactObj.optString("full_name", "Unknown User");
                    contactPhone = contactObj.optString("phone_number", "No Phone");
                }

                // Add 📦 emoji if it's an outstation package
                String prefix = isPackage ? "📦 " : "";

                // Handle missing pickup_time gracefully
                String pickupTime = obj.optString("pickup_time", "No time set");

                tempList.add(new BookingHistoryModel(
                        id,
                        obj.getString("status"),
                        contactName,
                        contactPhone,
                        prefix + obj.getString("source_location"),
                        obj.getString("destination_location"),
                        pickupTime
                ));
            }
        } catch (Exception e) {
            Log.e("HistoryError", "Parse Error: " + e.getMessage());
        }
    }

    private void updateUI(List<BookingHistoryModel> tempList) {
        if (!isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            historyList.clear();

            // Note: Since they are fetched separately, standard rides appear first, then packages.
            // This is perfectly fine for a general history overview!
            historyList.addAll(tempList);
            layoutLoading.setVisibility(View.GONE);

            if (historyList.isEmpty()) {
                tvEmptyState.setText("No history found.");
                tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                rvHistory.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void showEmptyState(String message) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            layoutLoading.setVisibility(View.GONE);
            rvHistory.setVisibility(View.GONE);
            tvEmptyState.setText(message);
            tvEmptyState.setVisibility(View.VISIBLE);
        });
    }

    // --- 4. UPDATE BOOKING STATUS (MARK COMPLETED) ---
    public void updateBookingStatus(String bookingId, String newStatus) {
        // 🚀 Check our memory map to figure out which table to patch!
        boolean isPackage = false;
        if (rideTypeMap.containsKey(bookingId)) {
            isPackage = rideTypeMap.get(bookingId);
        }

        String tableName = isPackage ? "package_bookings" : "bookings";
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/" + tableName + "?id=eq." + bookingId;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("status", newStatus);
        } catch (Exception e) { e.printStackTrace(); }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Ride Marked as " + newStatus.toUpperCase(), Toast.LENGTH_SHORT).show();
                        fetchHistoryLogs(); // Refresh the list!
                    });
                }
            }
        });
    }
}