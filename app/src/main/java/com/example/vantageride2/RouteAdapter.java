package com.example.vantageride2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    private final List<Route> routeList;
    private final boolean isDriver; // 🚀 Added to control delete button visibility
    private final OnRouteClickListener listener;

    // Interface to handle clicks back in the Fragment
    public interface OnRouteClickListener {
        void onDeleteClick(Route route);

        void onRouteClick(Route route); // 🚀 Added to fix the @Override error!
    }

    // 🚀 Updated constructor to accept the boolean
    public RouteAdapter(List<Route> routeList, boolean isDriver, OnRouteClickListener listener) {
        this.routeList = routeList;
        this.isDriver = isDriver;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_card, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        Route route = routeList.get(position);

        // Format the Route Path with the arrow
        String fullPath = route.getSource() + " ➔ " + route.getDest();
        holder.tvRoutePath.setText(fullPath);

        // Format Distance
        holder.tvDistance.setText(route.getKm() + " KM");

        // Format Pricing
        holder.tvPrice4s.setText("4S: ₹" + route.getPrice4());
        holder.tvPrice6s.setText("6S: ₹" + route.getPrice6());

        // 🚀 Hide or show the delete button based on who is viewing it
        if (isDriver) {
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

        // Handle Delete Button Click
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(route));

        // 🚀 Handle the whole card click for the Customer Booking
        holder.itemView.setOnClickListener(v -> listener.onRouteClick(route));
    }

    @Override
    public int getItemCount() {
        return routeList.size();
    }

    // ViewHolder class to map the UI elements
    static class RouteViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoutePath, tvDistance, tvPrice4s, tvPrice6s;
        ImageButton btnDelete;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoutePath = itemView.findViewById(R.id.tv_route_path);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvPrice4s = itemView.findViewById(R.id.tv_price_4s);
            tvPrice6s = itemView.findViewById(R.id.tv_price_6s);
            btnDelete = itemView.findViewById(R.id.btn_delete_route);
        }
    }
}