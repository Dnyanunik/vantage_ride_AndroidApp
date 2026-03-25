package com.example.vantageride2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
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

public class SearchFleetFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvNoFleet;

    private FleetAdapter fleetAdapter;

    private List<FleetModel> fleetList = new ArrayList<>();
    private List<FleetModel> allFleetList = new ArrayList<>();

    private OkHttpClient client;

    public SearchFleetFragment(){}

    // Enable toolbar menu
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_search_fleet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        client = new OkHttpClient();

        recyclerView = view.findViewById(R.id.recyclerViewSearchFleet);
        tvNoFleet = view.findViewById(R.id.tvNoFleet);

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(),2));

        fleetAdapter = new FleetAdapter(requireContext(), fleetList);
        recyclerView.setAdapter(fleetAdapter);

        Fragment header = requireActivity().getSupportFragmentManager().findFragmentById(R.id.header_container);
        if (header instanceof GlobalHeaderFragment) {
            ((GlobalHeaderFragment) header).activateSearch(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterFleet(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterFleet(newText);
                    return true;
                }
            });
        }
        fetchFleetData();
    }


    // Filter logic
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
            recyclerView.setVisibility(View.GONE);
            tvNoFleet.setVisibility(View.VISIBLE);
        } else {
            tvNoFleet.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        // Make sure your FleetAdapter has this method!
        fleetAdapter.setFilteredList(filteredList);
    }

    // Fetch data from Supabase
    private void fetchFleetData(){

        String url = SupabaseConfig.SUPABASE_URL +
                "/rest/v1/fleet?select=*&is_active=eq.true";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization","Bearer "+SupabaseConfig.SUPABASE_ANON_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SearchFleetError", "Network failure", e);
            }

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response) throws IOException {

                if(!isAdded() || response.body()==null) return;

                String json = response.body().string();

                try{
                    JSONArray array = new JSONArray(json);
                    List<FleetModel> temp = new ArrayList<>();

                    for(int i=0; i<array.length(); i++){
                        JSONObject carObj = array.getJSONObject(i);

                        // 🚀 THE FIX: Pass all 7 arguments required by FleetModel!
                        temp.add(new FleetModel(
                                carObj.getString("id"),
                                carObj.optString("driver_id", ""),  // 🚀 New
                                carObj.getString("name"),
                                carObj.optString("type", "Standard"), // 🚀 New
                                carObj.optString("description","Premium Ride"),
                                carObj.optString("image_url",""),
                                carObj.getDouble("rate_per_km")
                        ));
                    }

                    requireActivity().runOnUiThread(() -> {
                        fleetList.clear();
                        fleetList.addAll(temp);

                        allFleetList.clear();
                        allFleetList.addAll(temp);

                        fleetAdapter.notifyDataSetChanged();
                    });

                } catch(Exception e) {
                    Log.e("SearchFleetError","Parsing error", e);
                }
            }
        });
    }
}