package com.example.vantageride2;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_shell); // We will create this simple XML next

        // Load the RegisterFragment into the container
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new RegisterFragment())
                    .commit();
        }
    }
}