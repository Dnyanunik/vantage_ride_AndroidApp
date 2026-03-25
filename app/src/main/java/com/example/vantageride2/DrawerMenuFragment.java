package com.example.vantageride2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.navigation.NavigationView;

public class DrawerMenuFragment extends Fragment {

    // Declare views at the class level so we can update them later
    private ImageView imgHeaderAvatar;
    private TextView tvHeaderName;
    private TextView tvHeaderRole;
    private Menu menu;

    public DrawerMenuFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drawer_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavigationView navView = view.findViewById(R.id.nav_view);
        menu = navView.getMenu();

        // Adjust for system status bar
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        // Setup Header View references
        View headerView = navView.getHeaderView(0);
        imgHeaderAvatar = headerView.findViewById(R.id.img_header_avatar);
        tvHeaderName = headerView.findViewById(R.id.tv_header_name);
        tvHeaderRole = headerView.findViewById(R.id.tv_header_role);

        // 1. Initial Load of Header Data
        updateHeaderUI();

        // 2. Click Listeners when a user clicks an item
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_logout) {
                logoutUser();
            } else if(id == R.id.nav_profile) {
                // 1. Close the drawer first
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).closeDrawer();
                }

                // 2. Wait a tiny fraction of a second to prevent stuttering
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).loadFragmentWithAnimation(new ProfileFragment());
                    }
                }, 250);
            } else if(id == R.id.nav_home){
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();

                    // Call the reset method we just created
                    mainActivity.resetToHome();

                    // Optional: Force a refresh of the activity to ensure 'hidden' views reappear
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
            }else if(id == R.id.nav_ride_history){
                // We use getParentFragmentManager() because we are inside a Fragment
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new RideHistoryFragment())
                        .addToBackStack(null)
                        .commit();
            }else if (item.getItemId() == R.id.nav_about_us) {

                // Create and show the About Us popup
                AboutUsFragment aboutUsDialog = new AboutUsFragment();
                aboutUsDialog.show(requireActivity().getSupportFragmentManager(), "AboutUsDialog");

                return true; // (Or 'break;' if you are inside a switch statement)
            } else if (item.getItemId() == R.id.nav_manage_fleet) {
                // 🚀 1. Create the fragment instance
                ManageFleetFragment manageFleetFragment = new ManageFleetFragment();

                // 🚀 2. Execute the fragment transaction
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out) // Smooth transition
                        .replace(R.id.fragment_container, manageFleetFragment) // R.id.fragment_container is your FrameLayout ID
                        .addToBackStack(null) // Allows the user to go back using the back button
                        .commit();

                // 🚀 3. Close the drawer if you are using one
                  closeDrawer();

                return true;
            }else if (item.getItemId() == R.id.nav_create_package) {

                // Call the Routes fragment
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new FlightPathsFragment())
                        .addToBackStack(null)
                        .commit();

            }else{
                // Handle other clicks
                Toast.makeText(getContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
            }

            closeDrawer();
            return true;
        });
    }

    // --- NEW: Public Method to Refresh the Header Instantly ---
    // --- NEW: Public Method to Refresh the Header Instantly ---
    public void updateHeaderUI() {
        if (getContext() == null || !isAdded()) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false);
        String role = prefs.getString("USER_ROLE", "customer");
        String userName = prefs.getString("USER_NAME", "Guest");
        String avatarUrl = prefs.getString("AVATAR_URL", "");

        // 🚀 DEBUG: Print the role to Logcat so you can verify it's saving correctly
        Log.d("DRAWER_MENU", "Is Logged In: " + isLoggedIn + " | User Role: " + role);

        if (isLoggedIn) {
            tvHeaderName.setText(userName);
            tvHeaderRole.setText(role.toUpperCase() + " ACCOUNT");

            // 🚀 FIX 1: Use equalsIgnoreCase so "DRIVER", "Driver", and "driver" all work
            boolean isDriver = "driver".equalsIgnoreCase(role);
            menu.setGroupVisible(R.id.group_driver, isDriver);

            // 🚀 FIX 2: Explicitly ensure the main menu and logout are visible when logged in
            menu.setGroupVisible(R.id.group_main, true);
            menu.findItem(R.id.nav_logout).setVisible(true);

            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String finalUrl = avatarUrl;

                // Using SupabaseConfig instead of hardcoding to keep it clean
                if (!avatarUrl.startsWith("http")) {
                    finalUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/avatars/" + avatarUrl;
                }

                // Add cache buster to force Glide to download the new image instantly
                finalUrl += "?t=" + System.currentTimeMillis();

                Log.d("IMAGE_DEBUG", "Loading Drawer Header: " + finalUrl);

                Glide.with(this)
                        .load(finalUrl)
                        .placeholder(android.R.drawable.ic_menu_myplaces)
                        .error(android.R.drawable.ic_menu_report_image)
                        .circleCrop()
                        .skipMemoryCache(true) // SKIP MEMORY CACHE ensures the old photo is ignored
                        .diskCacheStrategy(DiskCacheStrategy.NONE) // FORCE FRESH DOWNLOAD
                        .into(imgHeaderAvatar);
            } else {
                Log.e("IMAGE_DEBUG", "No avatar URL found in SharedPreferences");
                imgHeaderAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
            }

        } else {
            tvHeaderName.setText("Guest");
            tvHeaderRole.setText("Please Login");
            imgHeaderAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);

            // Hide everything for guests
            menu.setGroupVisible(R.id.group_main, false);
            menu.setGroupVisible(R.id.group_driver, false);
            menu.findItem(R.id.nav_logout).setVisible(false);
        }
    }
    private void logoutUser() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(requireActivity(), activity_welcome.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void closeDrawer() {
        DrawerLayout drawerLayout = requireActivity().findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
}