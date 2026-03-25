package com.example.vantageride2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CustomerNotificationAdapter extends RecyclerView.Adapter<CustomerNotificationAdapter.ViewHolder> {

    private List<BookingHistoryModel> updateList;

    public CustomerNotificationAdapter(List<BookingHistoryModel> updateList) {
        this.updateList = updateList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingHistoryModel update = updateList.get(position);

        String route = update.getSourceLocation() + " → " + update.getDestinationLocation();
        String status = (update.getStatus() != null) ? update.getStatus().toLowerCase().trim() : "pending";

        holder.layoutDriverInfo.setVisibility(View.GONE);
        holder.tvMessage.setText(route);

        switch (status) {
            case "pending":
                holder.tvTitle.setText("Searching for a Pilot...");
                holder.tvTitle.setTextColor(Color.parseColor("#FFB300"));
                holder.viewStatusIndicator.setBackgroundColor(Color.parseColor("#FFB300"));
                break;
            case "accepted":
                holder.tvTitle.setText("Pilot Assigned!");
                holder.tvTitle.setTextColor(Color.parseColor("#00E676"));
                holder.viewStatusIndicator.setBackgroundColor(Color.parseColor("#00E676"));
                String driverText = "Pilot: " + update.getContactName() + " • 📞 " + update.getContactPhone();
                holder.tvDriverInfo.setText(driverText);
                holder.layoutDriverInfo.setVisibility(View.VISIBLE);
                break;
            case "completed":
                holder.tvTitle.setText("Ride Completed");
                holder.tvTitle.setTextColor(Color.parseColor("#2979FF"));
                holder.viewStatusIndicator.setBackgroundColor(Color.parseColor("#2979FF"));
                break;
            case "cancelled":
            case "rejected":
                holder.tvTitle.setText("Ride Cancelled");
                holder.tvTitle.setTextColor(Color.parseColor("#FF3D00"));
                holder.viewStatusIndicator.setBackgroundColor(Color.parseColor("#FF3D00"));
                break;
            default:
                holder.tvTitle.setText("Ride Update");
                holder.tvTitle.setTextColor(Color.parseColor("#888888"));
                holder.viewStatusIndicator.setBackgroundColor(Color.parseColor("#888888"));
                break;
        }

        // --- DISMISS LOGIC ---
        holder.btnMarkAsRead.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                String notificationId = updateList.get(currentPos).getId();

                SharedPreferences prefs = v.getContext().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
                String dismissedIds = prefs.getString("DISMISSED_NOTIFICATIONS", "");
                prefs.edit().putString("DISMISSED_NOTIFICATIONS", dismissedIds + notificationId + ",").apply();

                updateList.remove(currentPos);
                notifyItemRemoved(currentPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return updateList != null ? updateList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvDriverInfo, btnMarkAsRead;
        View viewStatusIndicator;
        LinearLayout layoutDriverInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_alert_title);
            tvMessage = itemView.findViewById(R.id.tv_alert_message);
            tvDriverInfo = itemView.findViewById(R.id.tv_driver_info);
            btnMarkAsRead = itemView.findViewById(R.id.btn_mark_read);
            viewStatusIndicator = itemView.findViewById(R.id.view_status_indicator);
            layoutDriverInfo = itemView.findViewById(R.id.layout_driver_info);
        }
    }
}