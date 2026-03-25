package com.example.vantageride2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    public DrawerLayout drawerLayout;

    private TextView tvGreeting;
    private TextView tvStatus;
    public MaterialButton btnPackages; // 🚀 ADDED: Reference for the Packages button

    private OkHttpClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false);

        if (!isLoggedIn) {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvStatus = findViewById(R.id.tv_status);
        btnPackages = findViewById(R.id.btn_packages);
        client = new OkHttpClient();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.black);
            swipeRefreshLayout.setOnRefreshListener(this::checkNetworkAndLoadData);
        }

        // Click Listener for See Packages Button
        if (btnPackages != null) {
            btnPackages.setOnClickListener(v -> {
                loadFragmentWithAnimation(new MyRoutesFragment());
            });
        }

        // Load initially from preferences to avoid 'customer' blink if DB fetch is slow
        String savedRole = prefs.getString("USER_ROLE", "customer").toUpperCase();
        String defaultStatus = savedRole.equalsIgnoreCase("DRIVER") ? "PILOT STATUS: SECURE" : "STATUS: " + savedRole;

        String cachedName = prefs.getString("CACHED_FIRST_NAME", "Pilot");
        String cachedStatus = prefs.getString("CACHED_STATUS", defaultStatus);

        if (tvGreeting != null) tvGreeting.setText("Welcome, " + cachedName);
        if (tvStatus != null) tvStatus.setText(cachedStatus);

        int currentBackStackCount = getSupportFragmentManager().getBackStackEntryCount();
        setHomeScreenUIVisibility(currentBackStackCount == 0);

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
            setHomeScreenUIVisibility(backStackCount == 0);
        });

        // 🚀 NEW: Check if intent told us to load the Admin Dashboard
        boolean openAdmin = getIntent().getBooleanExtra("OPEN_ADMIN_DASHBOARD", false);

        if (savedInstanceState == null) {
            if (openAdmin || savedRole.equalsIgnoreCase("ADMIN")) {
                // Instantly load the Admin Dashboard instead of normal startup
                loadFragmentWithAnimation(new AdminDashboardFragment());

                // Update UI for Admin
                if (btnPackages != null) btnPackages.setVisibility(View.GONE);
                if (tvGreeting != null) tvGreeting.setText("Welcome, Administrator");
                if (tvStatus != null) tvStatus.setText("STATUS: SYSADMIN");

            } else {
                // Normal standard user (Customer/Driver) startup
                checkNetworkAndLoadData();
                fetchUserDataForGreeting();
            }
        } else {
            refreshHeaderAvatar();
        }

        int initialCount = getSupportFragmentManager().getBackStackEntryCount();
        setHomeScreenUIVisibility(initialCount == 0);
    }
    public void setHomeScreenUIVisibility(boolean isHomeScreen) {
        int visibility = isHomeScreen ? View.VISIBLE : View.GONE;

        if (tvGreeting != null) tvGreeting.setVisibility(visibility);
        if (tvStatus != null) tvStatus.setVisibility(visibility);
        // 🚀 ADDED: Hide/Show the Packages button just like the text
        if (btnPackages != null) btnPackages.setVisibility(visibility);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(isHomeScreen);
        }
    }

    private void fetchUserDataForGreeting() {
        SharedPreferences prefs = getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "");
        String token = prefs.getString("ACCESS_TOKEN", "");

        if (userId.isEmpty() || token.isEmpty()) return;

        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=full_name,role";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MainActivity", "Failed to fetch greeting data", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        Log.d("SupabaseDebug", "Raw User Data: " + responseBody);

                        JSONArray jsonArray = new JSONArray(responseBody);

                        if (jsonArray.length() > 0) {
                            JSONObject profile = jsonArray.getJSONObject(0);
                            String fullName = profile.optString("full_name", "Pilot");

                            // FIX: Safely extract role to prevent null overriding to customer
                            String role = "customer";
                            if (profile.has("role") && !profile.isNull("role")) {
                                role = profile.getString("role").trim();
                            }
                            if (role.isEmpty() || role.equalsIgnoreCase("null")) {
                                role = "customer";
                            }
                            role = role.toUpperCase();

                            String firstName = fullName.contains(" ") ? fullName.split(" ")[0] : fullName;

                            String finalStatus;
                            if(role.equalsIgnoreCase("DRIVER")) {
                                finalStatus = "PILOT STATUS: SECURE";
                            } else {
                                finalStatus = "STATUS: " + role;
                            }

                            prefs.edit()
                                    .putString("CACHED_FIRST_NAME", firstName)
                                    .putString("CACHED_STATUS", finalStatus)
                                    .putString("USER_ROLE", role) // Update actual role to keep sync
                                    .apply();

                            runOnUiThread(() -> {
                                if (tvGreeting != null) {
                                    tvGreeting.setText("Welcome, " + firstName);
                                }
                                if (tvStatus != null) {
                                    tvStatus.setText(finalStatus);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "JSON Parse Error for greeting", e);
                    }
                }
            }
        });
    }

    public void checkNetworkAndLoadData() {
        if (!isNetworkAvailable()) {
            loadFragment(new NoInternetFragment());
            return;
        }

        loadFragment(new LoadingFragment());

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            loadFragment(new QuickSplashFragment());

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                loadFragment(new FleetFragment());
            }, 1200);
        }, 1500);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss();
    }

    public void loadFragmentWithAnimation(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    public void closeDrawer() {
        if (drawerLayout != null && drawerLayout.isOpen()) {
            drawerLayout.closeDrawers();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        }
        return false;
    }

    public void refreshHeaderAvatar() {
        Fragment possibleHeader = getSupportFragmentManager().findFragmentById(R.id.header_container);

        if (possibleHeader instanceof GlobalHeaderFragment) {
            ((GlobalHeaderFragment) possibleHeader).updateProfileAvatar();
        } else {
            Log.e("HEADER_DEBUG", "Could not find GlobalHeaderFragment!");
        }

        ImageView drawerAvatar = findViewById(R.id.img_header_avatar);

        if (drawerAvatar != null) {
            SharedPreferences prefs = getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
            String avatarUrl = prefs.getString("AVATAR_URL", "");

            if (!avatarUrl.isEmpty()) {
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
                        .into(drawerAvatar);
            }
        }
    }

    public void resetToHome() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public void logoutUser() {
        SharedPreferences prefs = getSharedPreferences("FleetPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}