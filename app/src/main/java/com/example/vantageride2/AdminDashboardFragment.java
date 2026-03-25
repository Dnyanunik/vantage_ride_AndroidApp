package com.example.vantageride2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

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

public class AdminDashboardFragment extends Fragment {

    private TextView tvTotalUsers, tvTotalDrivers;
    private BarChart barChart;
    private RecyclerView rvFleets, rvRoutes;

    private OkHttpClient client = new OkHttpClient();
    private String currentToken;

    public AdminDashboardFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        currentToken = prefs.getString("ACCESS_TOKEN", "");

        tvTotalUsers = view.findViewById(R.id.tv_total_users);
        tvTotalDrivers = view.findViewById(R.id.tv_total_drivers);
        barChart = view.findViewById(R.id.bar_chart);
        rvFleets = view.findViewById(R.id.rv_admin_fleets);
        rvRoutes = view.findViewById(R.id.rv_admin_routes);

        rvFleets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRoutes.setLayoutManager(new LinearLayoutManager(getContext()));

        fetchDashboardStats();
        setupChart();
        fetchFleets();
        fetchRoutes();
    }

    private void fetchDashboardStats() {
        fetchCount("profiles?role=eq.customer", tvTotalUsers);
        fetchCount("profiles?role=eq.driver", tvTotalDrivers);
    }

    private void fetchCount(String endpoint, TextView targetView) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/" + endpoint + "&select=id";
        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        int count = array.length();
                        if (getActivity() != null) {
                            requireActivity().runOnUiThread(() -> targetView.setText(String.valueOf(count)));
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    private void setupChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(1f, 40f)); // Standard
        entries.add(new BarEntry(2f, 25f)); // Package
        entries.add(new BarEntry(3f, 15f)); // Canceled

        BarDataSet dataSet = new BarDataSet(entries, "Ride Volumes");
        dataSet.setColors(ColorTemplate.PASTEL_COLORS);
        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.invalidate();
    }

    private void fetchFleets() {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/fleet?select=id,name,type,rate_per_km";
        Request req = new Request.Builder().url(url).addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY).build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        List<JSONObject> fleets = new ArrayList<>();
                        for(int i=0; i<array.length(); i++) fleets.add(array.getJSONObject(i));

                        if (getActivity() != null) {
                            requireActivity().runOnUiThread(() -> rvFleets.setAdapter(new PremiumAdminAdapter(fleets, "fleet")));
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    private void fetchRoutes() {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/routes?select=id,km,price4,price6";
        Request req = new Request.Builder().url(url).addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY).build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        List<JSONObject> routes = new ArrayList<>();
                        for(int i=0; i<array.length(); i++) routes.add(array.getJSONObject(i));

                        if (getActivity() != null) {
                            requireActivity().runOnUiThread(() -> rvRoutes.setAdapter(new PremiumAdminAdapter(routes, "routes")));
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    private void confirmAndDelete(String table, String id, int position, PremiumAdminAdapter adapter) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to permanently delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> executeDelete(table, id, position, adapter))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDelete(String table, String id, int position, PremiumAdminAdapter adapter) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/" + table + "?id=eq." + id;
        Request req = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + currentToken)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if(getActivity() != null) requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Deleted Successfully", Toast.LENGTH_SHORT).show();
                        adapter.removeItem(position);
                    } else {
                        Toast.makeText(getContext(), "Failed: Check Admin RLS Policies!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    // --- PREMIUM ADAPTER ---
    private class PremiumAdminAdapter extends RecyclerView.Adapter<PremiumAdminAdapter.ViewHolder> {
        private List<JSONObject> items;
        private String tableName;

        public PremiumAdminAdapter(List<JSONObject> items, String tableName) {
            this.items = items;
            this.tableName = tableName;
        }

        public void removeItem(int position) {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                JSONObject item = items.get(position);
                String id = item.getString("id");

                if (tableName.equals("fleet")) {
                    holder.tvTitle.setText(item.optString("name", "Unknown Fleet"));
                    holder.tvSubtitle.setText("Type: " + item.optString("type", "N/A") + " | Rate: ₹" + item.optString("rate_per_km", "0") + "/km");
                } else {
                    holder.tvTitle.setText("Custom Route: " + item.optString("km", "0") + " KM");
                    holder.tvSubtitle.setText("4-Seater: ₹" + item.optString("price4", "0") + " | 6-Seater: ₹" + item.optString("price6", "0"));
                }

                holder.btnDelete.setOnClickListener(v -> confirmAndDelete(tableName, id, holder.getAdapterPosition(), this));

            } catch (Exception e) { e.printStackTrace(); }
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle, btnDelete;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_item_title);
                tvSubtitle = itemView.findViewById(R.id.tv_item_subtitle);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}