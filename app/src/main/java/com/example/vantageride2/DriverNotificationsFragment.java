package com.example.vantageride2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class DriverNotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private DriverNotificationAdapter adapter;
    private List<BookingHistoryModel> missionList;

    private OkHttpClient client = new OkHttpClient();
    private String currentUserId;
    private String currentToken;

    // Memory map to remember if a booking ID belongs to a standard ride or a package
    private Map<String, Boolean> rideTypeMap = new HashMap<>();

    public DriverNotificationsFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvNotifications = view.findViewById(R.id.rv_notifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        missionList = new ArrayList<>();
        adapter = new DriverNotificationAdapter(missionList, this);
        rvNotifications.setAdapter(adapter);

        SharedPreferences prefs = requireActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("USER_ID", "");
        currentToken = prefs.getString("ACCESS_TOKEN", "");

        fetchPendingMissions();
    }

    private void fetchPendingMissions() {
        List<BookingHistoryModel> freshList = new ArrayList<>();
        rideTypeMap.clear();

        // API CALL 1: Fetch Standard Bookings
        String urlStandard = SupabaseConfig.SUPABASE_URL + "/rest/v1/bookings" +
                "?select=id,source_location,destination_location,status,contact:profiles!bookings_customer_id_fkey(full_name,phone_number)" +
                "&driver_id=eq." + currentUserId +
                "&status=eq.pending" +
                "&order=created_at.desc";

        Request reqStandard = new Request.Builder()
                .url(urlStandard)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .build();

        client.newCall(reqStandard).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                fetchPackageMissions(freshList);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    parseAndAddToList(response.body().string(), freshList, false);
                }
                fetchPackageMissions(freshList);
            }
        });
    }

    private void fetchPackageMissions(List<BookingHistoryModel> freshList) {
        String urlPackages = SupabaseConfig.SUPABASE_URL + "/rest/v1/package_bookings" +
                "?select=id,source_location,destination_location,status,contact:profiles!package_bookings_customer_id_fkey(full_name,phone_number)" +
                "&driver_id=eq." + currentUserId +
                "&status=eq.pending" +
                "&order=created_at.desc";

        Request reqPackages = new Request.Builder()
                .url(urlPackages)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .build();

        client.newCall(reqPackages).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                updateUI(freshList);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    parseAndAddToList(response.body().string(), freshList, true);
                }
                updateUI(freshList);
            }
        });
    }

    private void parseAndAddToList(String jsonResponse, List<BookingHistoryModel> freshList, boolean isPackage) {
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                BookingHistoryModel mission = new BookingHistoryModel();

                String bookingId = obj.getString("id");
                mission.setId(bookingId);

                rideTypeMap.put(bookingId, isPackage);

                String prefix = isPackage ? "📦 " : "";
                mission.setSourceLocation(prefix + obj.getString("source_location"));
                mission.setDestinationLocation(obj.getString("destination_location"));
                mission.setStatus(obj.getString("status"));

                if (obj.has("contact") && !obj.isNull("contact")) {
                    JSONObject contact = obj.getJSONObject("contact");
                    mission.setContactName(contact.optString("full_name", "Unknown Customer"));
                    mission.setContactPhone(contact.optString("phone_number", "N/A"));
                }
                freshList.add(mission);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateUI(List<BookingHistoryModel> freshList) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                missionList.clear();
                missionList.addAll(freshList);
                adapter.notifyDataSetChanged();
            });
        }
    }

    public void respondToMission(String bookingId, String responseStatus) {
        boolean isPackage = false;
        if (rideTypeMap.containsKey(bookingId)) {
            isPackage = rideTypeMap.get(bookingId);
        }

        String tableName = isPackage ? "package_bookings" : "bookings";
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/" + tableName + "?id=eq." + bookingId;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("status", responseStatus);
        } catch (Exception e) { e.printStackTrace(); }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .addHeader("Content-Type", "application/json")
                // 🚀 This header forces Supabase to return the row if it was successfully updated
                .addHeader("Prefer", "return=representation")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() == null) return;

                String responseBody = response.body() != null ? response.body().string() : "";

                getActivity().runOnUiThread(() -> {
                    if (response.isSuccessful()) {

                        // 🚀 CHECK: Did Supabase silently fail to update the row?
                        if (responseBody.trim().equals("[]")) {
                            Log.e("SupabaseRLS", "Update blocked! The DB updated 0 rows. Check RLS policies.");
                            Toast.makeText(getContext(), "DB Error: Update blocked by Supabase Policy!", Toast.LENGTH_LONG).show();
                            return; // Stop here, don't remove the card!
                        }

                        // Actually successful! Now we can safely remove the card.
                        Toast.makeText(getContext(), "Mission " + responseStatus.toUpperCase(), Toast.LENGTH_SHORT).show();

                        int removeIndex = -1;
                        for (int i = 0; i < missionList.size(); i++) {
                            if (missionList.get(i).getId().equals(bookingId)) {
                                removeIndex = i;
                                break;
                            }
                        }

                        if (removeIndex != -1) {
                            missionList.remove(removeIndex);
                            adapter.notifyItemRemoved(removeIndex);
                        }
                    } else {
                        Toast.makeText(getContext(), "Error saving to database.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}