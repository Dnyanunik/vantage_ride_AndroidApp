package com.example.vantageride2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// 🚀 ADDED: Required import for your button
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<BookingHistoryModel> historyList;

    // 🚀 ADDED: These variables let the adapter know about the user and fragment
    private String userRole;
    private RideHistoryFragment fragment;

    // 🚀 FIXED: The constructor now asks for the role and fragment
    public HistoryAdapter(List<BookingHistoryModel> historyList, String userRole, RideHistoryFragment fragment) {
        this.historyList = historyList;
        this.userRole = userRole;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingHistoryModel item = historyList.get(position);

        // Avoid crashes if the ID is unexpectedly short
        String shortId = (item.getId() != null && item.getId().length() >= 6)
                ? item.getId().substring(0, 6)
                : "Unknown";
        holder.tvId.setText("Mission #" + shortId);

        holder.tvName.setText(item.getContactName());
        holder.tvPhone.setText(item.getContactPhone());
        holder.tvDeparture.setText(item.getSourceLocation());
        holder.tvDestination.setText(item.getDestinationLocation());
        holder.tvDate.setText(item.getPickupTime());

        String status = item.getStatus().toLowerCase();

        // Set status text and styling
        if (status.equals("completed")) {
            holder.tvStatus.setText("COMPLETED");
            holder.tvStatus.setTextColor(0xFF00E676); // Green
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
        } else if (status.equals("pending")) {
            holder.tvStatus.setText("PENDING");
            holder.tvStatus.setTextColor(0xFFCBA328); // Gold
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        } else if (status.equals("rejected") || status.equals("cancelled")) {
            holder.tvStatus.setText("CANCELLED");
            holder.tvStatus.setTextColor(0xFFFF3D00); // Red
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_rejected);
        } else if (status.equals("accepted")) {
            holder.tvStatus.setText("ACCEPTED");
            holder.tvStatus.setTextColor(0xFF2979FF); // Blue
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }

        // 🚀 The Mark Completed Logic
// We use ignoreCase and trim() to catch "Driver", "DRIVER", or " driver "
        if (userRole != null && userRole.trim().equalsIgnoreCase("driver") && "accepted".equals(status)) {
            holder.btnMarkCompleted.setVisibility(View.VISIBLE);
            holder.btnMarkCompleted.setOnClickListener(v -> {
                // Ensure the fragment isn't null before calling it
                if (fragment != null) {
                    fragment.updateBookingStatus(item.getId(), "completed");
                }
            });
        } else {
            // ALWAYS reset recycled views!
            holder.btnMarkCompleted.setVisibility(View.GONE);
            holder.btnMarkCompleted.setOnClickListener(null);
        }
    }
    @Override
    public int getItemCount() { return historyList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvStatus, tvName, tvPhone, tvDeparture, tvDestination, tvDate;

        // 🚀 ADDED: Define the button
        MaterialButton btnMarkCompleted;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tv_booking_id);
            tvStatus = itemView.findViewById(R.id.tv_status_badge);
            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvPhone = itemView.findViewById(R.id.tv_contact_phone);
            tvDeparture = itemView.findViewById(R.id.tv_departure);
            tvDestination = itemView.findViewById(R.id.tv_destination);
            tvDate = itemView.findViewById(R.id.tv_date);

            // 🚀 ADDED: Find the button in your XML
            btnMarkCompleted = itemView.findViewById(R.id.btn_mark_completed);
        }
    }
}