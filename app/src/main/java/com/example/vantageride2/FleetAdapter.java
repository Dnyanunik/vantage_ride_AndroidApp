package com.example.vantageride2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FleetAdapter extends RecyclerView.Adapter<FleetAdapter.FleetViewHolder> {

    private Context context;
    private List<FleetModel> fleetList;

    public FleetAdapter(Context context, List<FleetModel> fleetList) {
        this.context = context;
        this.fleetList = fleetList;
    }

    @NonNull
    @Override
    public FleetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fleet_card, parent, false);
        return new FleetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FleetViewHolder holder, int position) {
        FleetModel car = fleetList.get(position);

        // 1. Set the Texts
        holder.tvCarName.setText(car.getName());

        if (car.getDescription() != null && !car.getDescription().isEmpty()) {
            holder.tvCarDesc.setText(car.getDescription());
        } else {
            holder.tvCarDesc.setText("Premium ride for your journey.");
        }

        // Format the rate to look like your web app (e.g., "₹11 / km")
        holder.tvRateBadge.setText("₹" + (int)car.getRatePerKm() + " / km");

        // 2. Load the Image using Glide
        String imgUrl = car.getImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            // Add your Supabase URL prefix here just like we did for the profile avatars!
            if (!imgUrl.startsWith("http")) {
                imgUrl = "https://anrkwsxutdodupqcxjsg.supabase.co/storage/v1/object/public/fleet_images/" + imgUrl;
            }

            Glide.with(context)
                    .load(imgUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.imgCar);
        } else {
            holder.imgCar.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // 🚀 UPDATED: Show the BookRideFragment as a sleek Bottom Sheet popup!
        // INSIDE FleetAdapter.java -> onBindViewHolder
        holder.btnBookNow.setOnClickListener(v -> {
            android.content.Context context = v.getContext();
            while (context instanceof android.content.ContextWrapper) {
                if (context instanceof androidx.appcompat.app.AppCompatActivity) break;
                context = ((android.content.ContextWrapper) context).getBaseContext();
            }

            if (context instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) context;

                BookRideFragment popup = new BookRideFragment();

                // 🚀 ADD THIS BUNDLE: Pass the car details to the popup!
                // Inside FleetAdapter.java -> setOnClickListener
                Bundle args = new Bundle();
                args.putString("DRIVER_ID", car.getDriverId()); // 🚀 Pass the driver's ID!
                args.putString("CAR_NAME", car.getName());
                args.putString("CAR_TYPE", car.getType());
                args.putDouble("RATE_PER_KM", car.getRatePerKm());
                popup.setArguments(args);

                popup.show(activity.getSupportFragmentManager(), "BookRidePopup");
            }
        });
    }

    // --- ADD THIS METHOD HERE ---
    /**
     * This method is called by your Search logic to update the screen
     * with only the cars that match the search query.
     */
    public void setFilteredList(List<FleetModel> filteredList) {
        this.fleetList = filteredList;
        notifyDataSetChanged();
    }
    // ----------------------------

    @Override
    public int getItemCount() {
        return fleetList.size();
    }

    // ViewHolder connects the XML IDs to Java
    public static class FleetViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCar;
        TextView tvRateBadge, tvCarName, tvCarDesc;
        MaterialButton btnBookNow;

        public FleetViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCar = itemView.findViewById(R.id.img_car);
            tvRateBadge = itemView.findViewById(R.id.tv_rate_badge);
            tvCarName = itemView.findViewById(R.id.tv_car_name);
            tvCarDesc = itemView.findViewById(R.id.tv_car_desc);
            btnBookNow = itemView.findViewById(R.id.btn_book_now);
        }
    }
}