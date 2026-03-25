package com.example.vantageride2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class DriverNotificationAdapter extends RecyclerView.Adapter<DriverNotificationAdapter.ViewHolder> {

    private List<BookingHistoryModel> missionList;
    private DriverNotificationsFragment fragment;

    public DriverNotificationAdapter(List<BookingHistoryModel> missionList, DriverNotificationsFragment fragment) {
        this.missionList = missionList;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_driver_mission, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingHistoryModel mission = missionList.get(position);

        holder.tvName.setText(mission.getContactName());
        holder.tvPhone.setText(mission.getContactPhone());
        holder.tvDeparture.setText(mission.getSourceLocation());
        holder.tvDestination.setText(mission.getDestinationLocation());

        holder.btnAccept.setOnClickListener(v -> fragment.respondToMission(mission.getId(), "accepted"));
        holder.btnReject.setOnClickListener(v -> fragment.respondToMission(mission.getId(), "rejected"));
    }

    @Override
    public int getItemCount() { return missionList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvDeparture, tvDestination;
        MaterialButton btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvPhone = itemView.findViewById(R.id.tv_contact_phone);
            tvDeparture = itemView.findViewById(R.id.tv_departure);
            tvDestination = itemView.findViewById(R.id.tv_destination);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}