package com.example.healthmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.healthmate.data.prefs.GoalPreferences;
import com.example.healthmate.data.prefs.SessionManager;
import com.example.healthmate.ui.auth.LoginActivity;
import com.example.healthmate.ui.onboarding.WelcomeActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

/*
 * The only activity in the app. All screens are fragments shown inside the
 * NavHostFragment, and the bottom navigation bar switches between them.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private View bottomNavDivider;
    private NavController navController;
    private View rootView;
    private boolean onEditScreen;
    private boolean keyboardVisible;
    private int keyboardBottom;
    private Insets systemBars = Insets.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // first time opening the app? show the welcome screen instead
        if (!new GoalPreferences(this).isOnboardingComplete()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        // nobody logged in? show the login screen
        if (!new SessionManager(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavDivider = findViewById(R.id.bottom_nav_divider);

        // keep the screen content away from the status bar, and move it
        // above the keyboard when the keyboard is open
        rootView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            keyboardBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            updateBottomBarAndPadding();
            return insets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            throw new IllegalStateException("NavHostFragment not found in activity_main.xml");
        }
        navController = navHostFragment.getNavController();

        // connects the bottom tabs to the screens in nav_graph.xml
        NavigationUI.setupWithNavController(bottomNavigation, navController);

        // hide the bottom bar on the Edit screen
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            onEditScreen = destination.getId() == R.id.editRecordFragment;
            updateBottomBarAndPadding();
        });
    }

    /*
     * The bottom bar is hidden on the Edit screen and while the keyboard is open.
     * When it is hidden, I add padding at the bottom myself so nothing is
     * covered by the keyboard or the phone's navigation bar.
     */
    private void updateBottomBarAndPadding() {
        if (bottomNavigation == null || rootView == null) {
            return;
        }
        boolean hideBar = onEditScreen || keyboardVisible;
        int visibility = hideBar ? View.GONE : View.VISIBLE;
        bottomNavigation.setVisibility(visibility);
        bottomNavDivider.setVisibility(visibility);

        int bottomPadding;
        if (keyboardVisible) {
            bottomPadding = keyboardBottom;
        } else if (hideBar) {
            bottomPadding = systemBars.bottom;
        } else {
            bottomPadding = 0; // the bottom bar already adds its own padding
        }
        rootView.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);
    }

    // opens one of the bottom tabs (for example R.id.addRecordFragment)
    public void openTab(int menuItemId) {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(menuItemId);
        }
    }

    // shows a short message above the bottom bar
    public Snackbar showMessage(@NonNull CharSequence message) {
        View anchorRoot = findViewById(R.id.main);
        Snackbar snackbar = Snackbar.make(anchorRoot, message, Snackbar.LENGTH_LONG);
        if (bottomNavigation != null && bottomNavigation.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(bottomNavigation);
        }
        snackbar.show();
        return snackbar;
    }

    public Snackbar showMessage(@StringRes int messageRes) {
        return showMessage(getString(messageRes));
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}