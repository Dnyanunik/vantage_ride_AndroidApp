package com.example.vantageride2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BookRideFragment extends BottomSheetDialogFragment {

    // View References
    private AutoCompleteTextView actvPickup, actvDropoff;
    private LinearLayout layoutNormalInfo, layoutPackageInfo, layoutPriceSummary;
    private TextView tvSelectedCarName, tvSelectedCarDetails, tvPackageDistance, tvTitle, tvFinalPackagePrice;
    private RadioGroup rgPackageCarType;
    private RadioButton rb4s, rb6s;
    private MaterialButton btnBookRide;

    // Data & Network
    private final OkHttpClient client = new OkHttpClient();
    private String currentUserId, currentToken, selectedDriverId, selectedRouteId;
    private boolean isPackageMode = false;
    private final List<String> locationList = new ArrayList<>();

    public BookRideFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_book_ride, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Views
        tvTitle = view.findViewById(R.id.tv_title);
        actvPickup = view.findViewById(R.id.actv_pickup);
        actvDropoff = view.findViewById(R.id.actv_dropoff);
        btnBookRide = view.findViewById(R.id.btn_book_ride);
        layoutNormalInfo = view.findViewById(R.id.layout_normal_info);
        tvSelectedCarName = view.findViewById(R.id.tv_selected_car_name);
        tvSelectedCarDetails = view.findViewById(R.id.tv_selected_car_details);
        layoutPackageInfo = view.findViewById(R.id.layout_package_info);
        tvPackageDistance = view.findViewById(R.id.tv_package_distance);
        rgPackageCarType = view.findViewById(R.id.rg_package_car_type);
        rb4s = view.findViewById(R.id.rb_4s);
        rb6s = view.findViewById(R.id.rb_6s);
        layoutPriceSummary = view.findViewById(R.id.layout_price_summary);
        tvFinalPackagePrice = view.findViewById(R.id.tv_final_package_price);

        // 2. Load User Credentials
        SharedPreferences prefs = requireActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("USER_ID", "");
        currentToken = prefs.getString("ACCESS_TOKEN", "");

        // 3. Determine Mode
        if (getArguments() != null && getArguments().containsKey("ROUTE_SOURCE")) {
            setupPackageMode();
        } else {
            setupNormalMode();
        }

        // 4. Set Click Listeners
        btnBookRide.setOnClickListener(v -> submitBooking());
    }

    private void setupNormalMode() {
        isPackageMode = false;
        layoutNormalInfo.setVisibility(View.VISIBLE);
        layoutPackageInfo.setVisibility(View.GONE);
        layoutPriceSummary.setVisibility(View.GONE);

        if (getArguments() != null) {
            selectedDriverId = getArguments().getString("DRIVER_ID");
            tvSelectedCarName.setText(getArguments().getString("CAR_NAME", "Fleet Ride"));
            double rate = getArguments().getDouble("RATE_PER_KM", 0.0);
            tvSelectedCarDetails.setText(getArguments().getString("CAR_TYPE", "SUV") + " • ₹" + rate + "/km");
        }

        fetchActiveServiceLocations();
    }

    private void setupPackageMode() {
        isPackageMode = true;
        tvTitle.setText("Book Travel Package");
        layoutNormalInfo.setVisibility(View.GONE);
        layoutPackageInfo.setVisibility(View.VISIBLE);
        layoutPriceSummary.setVisibility(View.VISIBLE);

        Bundle args = getArguments();
        if (args != null) {
            Log.d("BUNDLE_KEYS", "Keys received: " + args.keySet().toString());

            selectedDriverId = args.getString("TARGET_DRIVER_ID", "");

            // 🚀 FIXED: We use an empty string as the default, not route.getRouteId()
            selectedRouteId = args.getString("ROUTE_ID", "");

            tvPackageDistance.setText("Distance: " + args.getString("ROUTE_DISTANCE", "0") + " KM");

            double disc4 = args.getDouble("DISCOUNT_PRICE_4S", 0.0);
            double orig4 = args.getDouble("ORIGINAL_PRICE_4S", 0.0);
            double disc6 = args.getDouble("DISCOUNT_PRICE_6S", 0.0);
            double orig6 = args.getDouble("ORIGINAL_PRICE_6S", 0.0);

            rb4s.setText(Html.fromHtml("<b>Swift (4S)</b> - ₹" + disc4 + " <strike>₹" + orig4 + "</strike>", Html.FROM_HTML_MODE_LEGACY));
            rb6s.setText(Html.fromHtml("<b>Innova (6S)</b> - ₹" + disc6 + " <strike>₹" + orig6 + "</strike>", Html.FROM_HTML_MODE_LEGACY));

            // Set Initial Price
            tvFinalPackagePrice.setText("₹" + disc4);
            rb4s.setChecked(true);

            // Change Price when RadioButton changes
            rgPackageCarType.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rb_4s) {
                    tvFinalPackagePrice.setText("₹" + disc4);
                } else if (checkedId == R.id.rb_6s) {
                    tvFinalPackagePrice.setText("₹" + disc6);
                }
            });

            actvPickup.setText(args.getString("ROUTE_SOURCE", ""), false);
            actvDropoff.setText(args.getString("ROUTE_DEST", ""), false);
            actvPickup.setEnabled(false);
            actvDropoff.setEnabled(false);
        }
    }

    private void fetchActiveServiceLocations() {
        if (isPackageMode) return;

        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/maharashtra_locations?is_active=eq.true&select=city_name,taluka,district";
        String authHeader = (currentToken != null && !currentToken.isEmpty()) ? currentToken : SupabaseConfig.SUPABASE_ANON_KEY;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + authHeader)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handleUIError("Service locations unavailable");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(responseData);
                        Set<String> uniqueCities = new HashSet<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            uniqueCities.add(obj.optString("city_name", "Unknown"));
                        }
                        locationList.clear();
                        locationList.addAll(uniqueCities);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.select_dialog_item, locationList);
                                actvPickup.setAdapter(adapter);
                                actvDropoff.setAdapter(adapter);
                            });
                        }
                    } catch (Exception e) { Log.e("FETCH_LOC", e.getMessage()); }
                }
            }
        });
    }

    private void submitBooking() {
        String pickup = actvPickup.getText().toString().trim();
        String dropoff = actvDropoff.getText().toString().trim();

        if (pickup.isEmpty() || dropoff.isEmpty()) {
            Toast.makeText(getContext(), "Please select locations!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnBookRide.setEnabled(false);
        btnBookRide.setText("Booking...");

        JSONObject jsonBody = new JSONObject();
        String url = "";

        try {
            if (isPackageMode) {
                // 🛑 CRITICAL CHECK: Ensure Route ID is present
                if (selectedRouteId == null || selectedRouteId.isEmpty()) {
                    handleUIError("Error: Route ID is missing!");
                    return;
                }

                url = SupabaseConfig.SUPABASE_URL + "/rest/v1/package_bookings";

                boolean is4S = rb4s.isChecked();
                String carType = is4S ? "Swift (4S)" : "Innova (6S)";
                double finalPrice = is4S ?
                        getArguments().getDouble("DISCOUNT_PRICE_4S", 0.0) :
                        getArguments().getDouble("DISCOUNT_PRICE_6S", 0.0);

                // Sending all data to Supabase
                jsonBody.put("route_id", selectedRouteId);
                jsonBody.put("customer_id", currentUserId);

                if (selectedDriverId != null && !selectedDriverId.isEmpty()) {
                    jsonBody.put("driver_id", selectedDriverId);
                }

                jsonBody.put("source_location", pickup);
                jsonBody.put("destination_location", dropoff);
                jsonBody.put("distance_km", getArguments().getString("ROUTE_DISTANCE", "0"));
                jsonBody.put("car_type", carType);
                jsonBody.put("final_price", finalPrice);
                jsonBody.put("status", "pending");

                String time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(new Date());
                jsonBody.put("created_at", time);

            } else {
                // NORMAL BOOKING
                url = SupabaseConfig.SUPABASE_URL + "/rest/v1/bookings";
                jsonBody.put("customer_id", currentUserId);
                if (selectedDriverId != null) {
                    jsonBody.put("driver_id", selectedDriverId);
                }
                jsonBody.put("car_name", tvSelectedCarName.getText().toString());
                jsonBody.put("source_location", pickup);
                jsonBody.put("destination_location", dropoff);
                jsonBody.put("status", "pending");
                String time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(new Date());
                jsonBody.put("pickup_time", time);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handleUIError("Network Error");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Ride Booked Successfully!", Toast.LENGTH_SHORT).show();
                            dismiss();
                        } else {
                            try {
                                Log.e("BOOKING_ERROR", "Failed to book: " + response.body().string());
                            } catch (Exception ignored) {}
                            handleUIError("Booking failed. Please try again.");
                        }
                    });
                }
            }
        });
    }

    private void handleUIError(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                btnBookRide.setEnabled(true);
                btnBookRide.setText("Confirm Booking");
            });
        }
    }
}