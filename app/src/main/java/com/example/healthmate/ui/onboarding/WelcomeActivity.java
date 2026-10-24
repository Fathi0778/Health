package com.example.healthmate.ui.onboarding;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.healthmate.MainActivity;
import com.example.healthmate.R;
import com.example.healthmate.data.prefs.GoalPreferences;

/*
 * Welcome screen, only shown the first time the app opens.
 * "Get Started" saves a flag in SharedPreferences and opens the Home screen.
 */
public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // white status bar icons on top of the blue background
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_welcome);

        View root = findViewById(R.id.welcome_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        findViewById(R.id.button_get_started).setOnClickListener(v -> {
            new GoalPreferences(this).setOnboardingComplete(true);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
