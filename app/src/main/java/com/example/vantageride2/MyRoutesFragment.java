package com.example.vantageride2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyRoutesFragment extends Fragment {

    private RecyclerView rvCustomerPackages;
    private LinearLayout layoutEmptyState;
    private SwipeRefreshLayout swipeRefreshPackages;
    private RouteAdapter routeAdapter;
    private List<Route> availableRoutesList;
    private OkHttpClient client;

    public MyRoutesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_routes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCustomerPackages = view.findViewById(R.id.rv_customer_packages);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        swipeRefreshPackages = view.findViewById(R.id.swipe_refresh_packages);

        availableRoutesList = new ArrayList<>();
        client = new OkHttpClient();

        swipeRefreshPackages.setOnRefreshListener(() -> {
            fetchPublishedRoutes();
        });

        routeAdapter = new RouteAdapter(availableRoutesList, false, new RouteAdapter.OnRouteClickListener() {
            @Override
            public void onDeleteClick(Route route) {
                // Not used here
            }

            @Override
            public void onRouteClick(Route route) {
                double discountMultiplier = 0.90;
                double discountedPrice4s = route.getPrice4() * discountMultiplier;
                double discountedPrice6s = route.getPrice6() * discountMultiplier;

                Bundle bundle = new Bundle();
                bundle.putString("ROUTE_ID", String.valueOf(route.getId()));
                bundle.putString("ROUTE_SOURCE", route.getSource());
                bundle.putString("ROUTE_DEST", route.getDest());
                bundle.putString("ROUTE_DISTANCE", route.getKm());

                // 🚀 PASSING THE TARGET DRIVER ID SO ONLY THEY GET NOTIFIED
                bundle.putString("TARGET_DRIVER_ID", route.getDriverId());

                bundle.putDouble("ORIGINAL_PRICE_4S", route.getPrice4());
                bundle.putDouble("DISCOUNT_PRICE_4S", discountedPrice4s);

                bundle.putDouble("ORIGINAL_PRICE_6S", route.getPrice6());
                bundle.putDouble("DISCOUNT_PRICE_6S", discountedPrice6s);

                BookRideFragment bookRideBottomSheet = new BookRideFragment();
                bookRideBottomSheet.setArguments(bundle);
                bookRideBottomSheet.show(requireActivity().getSupportFragmentManager(), "BookRideBottomSheet");
            }
        });

        rvCustomerPackages.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCustomerPackages.setAdapter(routeAdapter);

        fetchPublishedRoutes();
    }

    private void fetchPublishedRoutes() {
        swipeRefreshPackages.setRefreshing(true);

        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/routes?select=*,source_loc:maharashtra_locations!routes_source_location_id_fkey(*),dest_loc:maharashtra_locations!routes_dest_location_id_fkey(*)";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SUPABASE_ERROR", "Failed to fetch routes: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefreshPackages.setRefreshing(false);
                        Toast.makeText(getContext(), "Network Error: Could not fetch packages", Toast.LENGTH_SHORT).show();
                        checkEmptyState();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();

                    try {
                        JSONArray jsonArray = new JSONArray(jsonResponse);
                        availableRoutesList.clear();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);

                            String id = obj.optString("id", "0");
                            String km = obj.optString("km", "0");
                            double price4s = obj.optDouble("price4", 0.0);
                            double price6s = obj.optDouble("price6", 0.0);

                            // 🚀 GRABBING THE DRIVER ID FROM THE ROUTE
                            String driverId = obj.optString("driver_id", "");

                            JSONObject sourceObj = obj.optJSONObject("source_loc");
                            JSONObject destObj = obj.optJSONObject("dest_loc");

                            // 🚀 FIXED: Now looking for "city_name" instead of "name"
                            String sourceName = (sourceObj != null) ? sourceObj.optString("city_name", "Unknown") : "Unknown";
                            String destName = (destObj != null) ? destObj.optString("city_name", "Unknown") : "Unknown";

                            // 🚀 We are now passing driverId into your Route object!
                            availableRoutesList.add(new Route(id, sourceName, destName, km, price4s, price6s, driverId));
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                swipeRefreshPackages.setRefreshing(false);
                                routeAdapter.notifyDataSetChanged();
                                checkEmptyState();
                            });
                        }

                    } catch (JSONException e) {
                        Log.e("SUPABASE_ERROR", "JSON Parse error: " + e.getMessage());
                        if (getActivity() != null) getActivity().runOnUiThread(() -> swipeRefreshPackages.setRefreshing(false));
                    }
                } else {
                    Log.e("SUPABASE_ERROR", "Response failed: " + response.code());
                    if (getActivity() != null) getActivity().runOnUiThread(() -> swipeRefreshPackages.setRefreshing(false));
                }
            }
        });
    }

    private void checkEmptyState() {
        if (availableRoutesList.isEmpty()) {
            rvCustomerPackages.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvCustomerPackages.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
}