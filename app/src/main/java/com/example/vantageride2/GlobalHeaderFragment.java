package com.example.vantageride2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.MaterialToolbar;

public class GlobalHeaderFragment extends Fragment {

    private MaterialToolbar toolbar;
    private TextView tvNotificationBadge;

    public GlobalHeaderFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_global_header, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.topAppBar);

        // APPLY TOP PADDING FOR STATUS BAR
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        setupNavigation();
        setupMenu();
        updateThemeIcon();

        // Wait for the menu to inflate before trying to find the custom action views
        toolbar.post(() -> {

            // 1. Setup Profile Custom View & Click Listener
            MenuItem profileItem = toolbar.getMenu().findItem(R.id.action_profile);
            if (profileItem != null && profileItem.getActionView() != null) {
                profileItem.getActionView().setOnClickListener(v -> {
                    Toast.makeText(requireContext(), "Opening Profile...", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.fragment_container, new ProfileFragment())
                            .addToBackStack(null)
                            .commit();
                });
            }

            // 2. Setup Notification Custom View & Click Listener
            MenuItem notifItem = toolbar.getMenu().findItem(R.id.action_notification);
            if (notifItem != null && notifItem.getActionView() != null) {
                View notifActionView = notifItem.getActionView();

                // Find the badge INSIDE the custom notification layout
                tvNotificationBadge = notifActionView.findViewById(R.id.tv_notification_badge);

                // Set click listener on the notification icon
                notifActionView.setOnClickListener(v -> openNotifications());

                // Set default notification count (Update this from your DB later)
                updateNotificationCount(1);
            }

            // 3. Load the avatar image
            updateProfileAvatar();
        });
    }

    private void setupNavigation() {
        toolbar.setNavigationOnClickListener(v -> {
            DrawerLayout drawerLayout = requireActivity().findViewById(R.id.drawer_layout);
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            } else {
                Toast.makeText(requireContext(), "Drawer layout not found in Activity", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupMenu() {
        toolbar.setOnMenuItemClickListener(this::handleMenuClick);
    }

    private boolean handleMenuClick(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, new SearchFleetFragment())
                    .addToBackStack(null)
                    .commit();
            return true;
        } else if (id == R.id.action_theme_toggle) {
            toggleAppTheme();
            return true;
        }
        // action_notification and action_profile are now handled by their custom actionView click listeners above
        return false;
    }

    private void toggleAppTheme() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (mode == Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    private void updateThemeIcon() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        MenuItem themeItem = toolbar.getMenu().findItem(R.id.action_theme_toggle);

        if (themeItem != null) {
            if (mode == Configuration.UI_MODE_NIGHT_YES) {
                themeItem.setIcon(R.drawable.ic_light_mode);
            } else {
                themeItem.setIcon(R.drawable.ic_dark_mode);
            }
        }
    }

    public void updateProfileAvatar() {
        if (toolbar == null || !isAdded()) return;

        MenuItem profileItem = toolbar.getMenu().findItem(R.id.action_profile);
        if (profileItem == null || profileItem.getActionView() == null) return;

        ImageView menuAvatar = profileItem.getActionView().findViewById(R.id.menu_avatar);
        SharedPreferences prefs = requireActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        String avatarUrl = prefs.getString("AVATAR_URL", "");

        if (!avatarUrl.isEmpty() && menuAvatar != null) {
            String finalUrl = avatarUrl;
            if (!avatarUrl.startsWith("http")) {
                finalUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/avatars/" + avatarUrl;
            }
            finalUrl += "?t=" + System.currentTimeMillis();

            Glide.with(this)
                    .load(finalUrl)
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(menuAvatar);
        }
    }

    public void activateSearch(androidx.appcompat.widget.SearchView.OnQueryTextListener listener) {
        if (toolbar == null) return;

        MenuItem searchItem = toolbar.getMenu().findItem(R.id.action_search);
        if (searchItem != null) {
            searchItem.expandActionView();
            View actionView = searchItem.getActionView();
            if (actionView instanceof androidx.appcompat.widget.SearchView) {
                androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) actionView;
                searchView.setQueryHint("Search cars...");
                searchView.setIconified(false);
                searchView.setOnQueryTextListener(listener);
            }
        }
    }

    public void updateNotificationCount(int count) {
        if (tvNotificationBadge == null) return;

        if (count > 0) {
            tvNotificationBadge.setText(String.valueOf(count));
            tvNotificationBadge.setVisibility(View.VISIBLE);
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    private void openNotifications() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        String role = prefs.getString("USER_ROLE", "customer");

        // Hide badge when user opens the notifications
        updateNotificationCount(0);

        Fragment targetFragment = role.equalsIgnoreCase("driver")
                ? new DriverNotificationsFragment()
                : new CustomerNotificationsFragment();

        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, targetFragment)
                .addToBackStack(null)
                .commit();
    }
}