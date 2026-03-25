package com.example.vantageride2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FlightPathsFragment extends Fragment {

    private AutoCompleteTextView etSource, etDestination;
    private EditText etDistanceKm, etPrice4s, etPrice6s;
    private MaterialButton btnAddRoute;
    private RecyclerView rvRoutes;

    private RouteAdapter adapter;
    private List<Route> routeList = new ArrayList<>();
    private List<Location> locationList = new ArrayList<>();

    // To hold the UUIDs selected from the dropdowns
    private String selectedSourceId = null;
    private String selectedDestId = null;

    private final OkHttpClient client = new OkHttpClient();
    private String currentUserId, currentToken;

    public FlightPathsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_flight_paths, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSource = view.findViewById(R.id.et_source);
        etDestination = view.findViewById(R.id.et_destination);
        etDistanceKm = view.findViewById(R.id.et_distance_km);
        etPrice4s = view.findViewById(R.id.et_price_4s);
        etPrice6s = view.findViewById(R.id.et_price_6s);
        btnAddRoute = view.findViewById(R.id.btn_add_route);
        rvRoutes = view.findViewById(R.id.rv_routes);

        SharedPreferences prefs = requireActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("USER_ID", "");
        currentToken = prefs.getString("ACCESS_TOKEN", "");

        rvRoutes.setLayoutManager(new LinearLayoutManager(getContext()));

        // 🚀 FIXED: Initialized RouteAdapter with 3 arguments to match the updated class
        adapter = new RouteAdapter(routeList, true, new RouteAdapter.OnRouteClickListener() {
            @Override
            public void onDeleteClick(Route route) {
                deleteRouteFromSupabase(route);
            }

            @Override
            public void onRouteClick(Route route) {
                // Not used in Driver Fragment, but required by interface
            }
        });

        rvRoutes.setAdapter(adapter);

        btnAddRoute.setOnClickListener(v -> publishNewRoute());

        // Setup Dropdowns
        setupDropdownListeners();

        // Fetch Data
        fetchLocations();
        fetchDriverRoutes();
    }

    // --- 1. SETUP DROPDOWN LOGIC ---
    private void setupDropdownListeners() {
        etSource.setOnClickListener(v -> etSource.showDropDown());
        etDestination.setOnClickListener(v -> etDestination.showDropDown());

        etSource.setOnItemClickListener((parent, view, position, id) -> {
            Location selectedLocation = (Location) parent.getItemAtPosition(position);
            selectedSourceId = selectedLocation.getId();
        });

        etDestination.setOnItemClickListener((parent, view, position, id) -> {
            Location selectedLocation = (Location) parent.getItemAtPosition(position);
            selectedDestId = selectedLocation.getId();
        });
    }

    // --- 2. FETCH MAHARASHTRA LOCATIONS ---
    private void fetchLocations() {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/maharashtra_locations?is_active=eq.true&select=id,city_name";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Locations", "Failed to fetch");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        locationList.clear();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            locationList.add(new Location(
                                    obj.getString("id"),
                                    obj.getString("city_name")
                            ));
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                ArrayAdapter<Location> dropdownAdapter = new ArrayAdapter<>(
                                        getContext(),
                                        android.R.layout.simple_dropdown_item_1line,
                                        locationList
                                );
                                etSource.setAdapter(dropdownAdapter);
                                etDestination.setAdapter(dropdownAdapter);
                            });
                        }

                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    // --- 3. FETCH ROUTES (READING FROM THE SQL VIEW) ---
    private void fetchDriverRoutes() {
        // 🚀 FIXED URL: Direct join on the routes table to ensure source_loc and dest_loc objects exist
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/routes?driver_id=eq." + currentUserId +
                "&select=*,source_loc:source_location_id(city_name),dest_loc:dest_location_id(city_name)";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("FETCH_ROUTES", "Error: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);

                        // Use a temporary list to avoid data corruption while looping
                        List<Route> tempList = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);

                            String id = obj.optString("id", "0");
                            String km = obj.optString("km", "0");
                            double price4s = obj.optDouble("price4", 0.0);
                            double price6s = obj.optDouble("price6", 0.0);
                            String driverId = obj.optString("driver_id", "");

                            // These blocks match your existing logic!
                            JSONObject sourceObj = obj.optJSONObject("source_loc");
                            JSONObject destObj = obj.optJSONObject("dest_loc");

                            String sourceName = (sourceObj != null) ? sourceObj.optString("city_name", "Unknown") : "Unknown";
                            String destName = (destObj != null) ? destObj.optString("city_name", "Unknown") : "Unknown";

                            tempList.add(new Route(id, sourceName, destName, km, price4s, price6s, driverId));
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                routeList.clear();
                                routeList.addAll(tempList);
                                adapter.notifyDataSetChanged();
                            });
                        }
                    } catch (Exception e) {
                        Log.e("PARSE_ERROR", "Check JSON structure: " + e.getMessage());
                    }
                }
            }
        });
    }
    // --- 4. PUBLISH ROUTE ---
    private void publishNewRoute() {
        String km = etDistanceKm.getText().toString().trim();
        String p4 = etPrice4s.getText().toString().trim();
        String p6 = etPrice6s.getText().toString().trim();

        if (selectedSourceId == null || selectedDestId == null || km.isEmpty() || p4.isEmpty() || p6.isEmpty()) {
            showToast("Please complete all fields and select valid cities.");
            return;
        }

        btnAddRoute.setEnabled(false);
        btnAddRoute.setText("TRANSMITTING...");

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("driver_id", currentUserId);
            jsonBody.put("source_location_id", selectedSourceId);
            jsonBody.put("dest_location_id", selectedDestId);
            jsonBody.put("km", km);
            jsonBody.put("price4", Double.parseDouble(p4));
            jsonBody.put("price6", Double.parseDouble(p6));
        } catch (Exception e) { e.printStackTrace(); }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(SupabaseConfig.SUPABASE_URL + "/rest/v1/routes")
                .post(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                resetAddButton("Network Error");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            etSource.setText("");
                            etDestination.setText("");
                            selectedSourceId = null;
                            selectedDestId = null;
                            etDistanceKm.setText("");
                            etPrice4s.setText("");
                            etPrice6s.setText("");
                            resetAddButton("Route Published");
                            fetchDriverRoutes();
                        });
                    }
                } else {
                    resetAddButton("Publish Failed");
                }
            }
        });
    }

    // --- 5. DELETE ROUTE ---
    private void deleteRouteFromSupabase(Route route) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/routes?id=eq." + route.getId() + "&driver_id=eq." + currentUserId;

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showToast("Route Erased");
                            routeList.remove(route);
                            adapter.notifyDataSetChanged();
                        });
                    }
                }
            }
        });
    }

    private void resetAddButton(String msg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                btnAddRoute.setEnabled(true);
                btnAddRoute.setText("PUBLISH ROUTE");
            });
        }
    }

    private void showToast(String msg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
        }
    }
}