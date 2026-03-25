package com.example.vantageride2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
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

public class FleetFragment extends Fragment {

    private RecyclerView recyclerViewFleet;
    private View loadingSkeleton, errorLayout;
    private FleetAdapter fleetAdapter;
    private List<FleetModel> fleetList;
    private List<FleetModel> allFleetList; // Master copy of data
    private OkHttpClient client;
    private TextView tvNoResult;

    public FleetFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Enables the search icon in the fragment
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fleet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        client = new OkHttpClient();

        recyclerViewFleet = view.findViewById(R.id.recyclerViewFleet);
        loadingSkeleton = view.findViewById(R.id.loading_skeleton);
        errorLayout = view.findViewById(R.id.error_layout);
        Button btnRetry = view.findViewById(R.id.btn_retry);
        tvNoResult = view.findViewById(R.id.tv_no_result);

        recyclerViewFleet.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        fleetList = new ArrayList<>();
        allFleetList = new ArrayList<>();
        fleetAdapter = new FleetAdapter(requireContext(), fleetList);
        recyclerViewFleet.setAdapter(fleetAdapter);

        btnRetry.setOnClickListener(v -> fetchFleetDataFromSupabase());

        fetchFleetDataFromSupabase();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear(); // Clear existing menu items
        inflater.inflate(R.menu.global_header_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            searchItem.setActionView(null);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, new SearchFleetFragment())
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Search logic
    private void filterFleet(String text) {
        List<FleetModel> filteredList = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            filteredList.addAll(allFleetList);
        } else {
            String query = text.toLowerCase();
            for (FleetModel car : allFleetList) {
                if (car.getName().toLowerCase().contains(query)
                        || car.getDescription().toLowerCase().contains(query)) {
                    filteredList.add(car);
                }
            }
        }

        if (filteredList.isEmpty()) {
            recyclerViewFleet.setVisibility(View.GONE);
            tvNoResult.setVisibility(View.VISIBLE);
        } else {
            tvNoResult.setVisibility(View.GONE);
            recyclerViewFleet.setVisibility(View.VISIBLE);
        }

        // Make sure your FleetAdapter actually has this method!
        fleetAdapter.setFilteredList(filteredList);
    }

    private void fetchFleetDataFromSupabase() {
        showInternalLoading();

        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/fleet?select=*&is_active=eq.true";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (!isAdded()) return;
                showError();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!isAdded() || response.body() == null) return;

                String jsonResponse = response.body().string();

                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(jsonResponse);
                        List<FleetModel> tempCars = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject carObj = jsonArray.getJSONObject(i);

                            // 🚀 THE FIX: Passing the 7 required arguments to FleetModel
                            tempCars.add(new FleetModel(
                                    carObj.getString("id"),
                                    carObj.optString("driver_id", ""), // Make sure this is in your DB!
                                    carObj.getString("name"),
                                    carObj.optString("type", "Standard"),
                                    carObj.optString("description", "Premium Ride"),
                                    carObj.optString("image_url", ""),
                                    carObj.getDouble("rate_per_km")
                            ));
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (tempCars.isEmpty()) {
                                    showError();
                                } else {
                                    fleetList.clear();
                                    fleetList.addAll(tempCars);

                                    allFleetList.clear();
                                    allFleetList.addAll(tempCars);

                                    fleetAdapter.notifyDataSetChanged();
                                    showData();
                                }
                            });
                        }

                    } catch (Exception e) {
                        Log.e("SupabaseError", "Parsing error", e);
                        showError();
                    }
                } else {
                    showError();
                }
            }
        });
    }

    private void showInternalLoading() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            recyclerViewFleet.setVisibility(View.GONE);
            errorLayout.setVisibility(View.GONE);
            loadingSkeleton.setVisibility(View.VISIBLE);
        });
    }

    private void showData() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            loadingSkeleton.setVisibility(View.GONE);
            errorLayout.setVisibility(View.GONE);
            recyclerViewFleet.setVisibility(View.VISIBLE);
        });
    }

    private void showError() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            loadingSkeleton.setVisibility(View.GONE);
            recyclerViewFleet.setVisibility(View.GONE);
            errorLayout.setVisibility(View.VISIBLE);
        });
    }
}