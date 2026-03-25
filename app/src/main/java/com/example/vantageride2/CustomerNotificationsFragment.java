package com.example.vantageride2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CustomerNotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private CustomerNotificationAdapter adapter;
    private List<BookingHistoryModel> updateList;

    private OkHttpClient client = new OkHttpClient();
    private String currentUserId;
    private String currentToken;

    private Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private Runnable autoRefreshRunnable;

    public CustomerNotificationsFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customer_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvNotifications = view.findViewById(R.id.rv_notifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        updateList = new ArrayList<>();
        adapter = new CustomerNotificationAdapter(updateList);
        rvNotifications.setAdapter(adapter);

        SharedPreferences prefs = requireActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("USER_ID", "");
        currentToken = prefs.getString("ACCESS_TOKEN", "");
    }

    @Override
    public void onResume() {
        super.onResume();
        startLiveUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    private void startLiveUpdates() {
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchCustomerUpdates();
                autoRefreshHandler.postDelayed(this, 10000); // Poll every 10 seconds
            }
        };
        autoRefreshHandler.post(autoRefreshRunnable);
    }

    private void fetchCustomerUpdates() {
        List<BookingHistoryModel> freshList = new ArrayList<>();

        // 🚀 API CALL 1: Fetch Standard Bookings
        String urlStandard = SupabaseConfig.SUPABASE_URL + "/rest/v1/bookings" +
                "?select=id,source_location,destination_location,status,driver:profiles!bookings_driver_id_fkey(full_name,phone_number)" +
                "&customer_id=eq." + currentUserId + "&order=created_at.desc&limit=10";

        Request reqStandard = new Request.Builder()
                .url(urlStandard)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .build();

        client.newCall(reqStandard).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    parseAndAddToList(response.body().string(), freshList, false);
                }

                // 🚀 API CALL 2: Fetch Package Bookings immediately after Standard Bookings
                fetchPackageBookings(freshList);
            }
        });
    }

    private void fetchPackageBookings(List<BookingHistoryModel> freshList) {
        // Notice the table name is 'package_bookings' and the foreign key relationship name changes
        String urlPackages = SupabaseConfig.SUPABASE_URL + "/rest/v1/package_bookings" +
                "?select=id,source_location,destination_location,status,driver:profiles!package_bookings_driver_id_fkey(full_name,phone_number)" +
                "&customer_id=eq." + currentUserId + "&order=created_at.desc&limit=10";

        Request reqPackages = new Request.Builder()
                .url(urlPackages)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .build();

        client.newCall(reqPackages).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                updateUI(freshList); // Update UI even if packages fail
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    parseAndAddToList(response.body().string(), freshList, true);
                }
                updateUI(freshList); // Update UI with both sets of data
            }
        });
    }

    // 🚀 Helper method to process the JSON from both tables cleanly
    private void parseAndAddToList(String jsonResponse, List<BookingHistoryModel> freshList, boolean isPackage) {
        if (getActivity() == null) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        String dismissedIds = prefs.getString("DISMISSED_NOTIFICATIONS", "");

        SharedPreferences statusMemory = requireActivity().getSharedPreferences("RideStatusMemory", Context.MODE_PRIVATE);
        SharedPreferences.Editor memoryEditor = statusMemory.edit();

        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String bookingId = obj.getString("id");

                // Package bookings default to pending, ensure safe parsing
                String currentStatus = obj.optString("status", "pending").toLowerCase();

                // --- LIVE NOTIFICATION LOGIC ---
                String previousStatus = statusMemory.getString(bookingId, "unknown");

                if (!previousStatus.equals("unknown") && !previousStatus.equals(currentStatus)) {
                    String rideTypeStr = isPackage ? "Package Ride" : "City Ride";

                    if (currentStatus.equals("accepted")) {
                        String driverName = obj.optJSONObject("driver") != null ? obj.getJSONObject("driver").optString("full_name", "A pilot") : "A pilot";
                        showSystemNotification(rideTypeStr + " Accepted! 🚗", driverName + " is assigned to your trip.");
                    } else if (currentStatus.equals("rejected") || currentStatus.equals("cancelled")) {
                        showSystemNotification(rideTypeStr + " Cancelled ❌", "Unfortunately, the pilot cancelled. Please book another trip.");
                    } else if (currentStatus.equals("completed")) {
                        showSystemNotification("Happy Journey! 🎉", "Thank you for using Vantage Ride!");
                    }
                }

                memoryEditor.putString(bookingId, currentStatus);
                // ----------------------------------

                // Skip if the user swiped/dismissed this notification
                if (dismissedIds.contains(bookingId)) continue;

                BookingHistoryModel update = new BookingHistoryModel();
                update.setId(bookingId);

                // Add a small package emoji indicator if it's an outstation package
                String prefix = isPackage ? "📦 " : "";
                update.setSourceLocation(prefix + obj.getString("source_location"));
                update.setDestinationLocation(obj.getString("destination_location"));
                update.setStatus(currentStatus);

                if (obj.has("driver") && !obj.isNull("driver")) {
                    JSONObject driver = obj.getJSONObject("driver");
                    update.setContactName(driver.optString("full_name", "Assigned Pilot"));
                    update.setContactPhone(driver.optString("phone_number", ""));
                }
                freshList.add(update);
            }
            memoryEditor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI(List<BookingHistoryModel> freshList) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                updateList.clear();
                updateList.addAll(freshList);
                adapter.notifyDataSetChanged();
            });
        }
    }

    private void showSystemNotification(String title, String message) {
        if (getContext() == null) return;

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "VantageRide_Updates";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Ride Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Make sure you have this icon or replace it with your app icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}