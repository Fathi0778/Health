package com.example.healthmate.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.healthmate.MainActivity;
import com.example.healthmate.R;
import com.example.healthmate.data.prefs.SessionManager;
import com.example.healthmate.data.repository.AuthRepository;
import com.example.healthmate.utils.ValidationUtils;
import com.example.healthmate.viewmodel.AuthViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/*
 * Login screen. The same screen is used for "Log in" and "Create account";
 * the button at the bottom switches between the two modes.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String STATE_REGISTER_MODE = "register_mode";

    private AuthViewModel viewModel;
    private boolean registerMode;

    private TextView titleText;
    private TextView subtitleText;
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmLayout;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmInput;
    private MaterialButton submitButton;
    private MaterialButton switchButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // white status bar icons on top of the blue background
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_login);

        View root = findViewById(R.id.login_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        titleText = findViewById(R.id.text_login_title);
        subtitleText = findViewById(R.id.text_login_subtitle);
        usernameLayout = findViewById(R.id.input_layout_username);
        passwordLayout = findViewById(R.id.input_layout_password);
        confirmLayout = findViewById(R.id.input_layout_confirm);
        usernameInput = findViewById(R.id.input_username);
        passwordInput = findViewById(R.id.input_password);
        confirmInput = findViewById(R.id.input_confirm);
        submitButton = findViewById(R.id.button_submit);
        switchButton = findViewById(R.id.button_switch_mode);

        // remove the red error when the user types again
        clearErrorOnEdit(usernameInput, usernameLayout);
        clearErrorOnEdit(passwordInput, passwordLayout);
        clearErrorOnEdit(confirmInput, confirmLayout);

        submitButton.setOnClickListener(v -> submit());
        switchButton.setOnClickListener(v -> setRegisterMode(!registerMode));

        // "Done" on the keyboard = press the button
        TextView.OnEditorActionListener done = (v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        };
        passwordInput.setOnEditorActionListener(done);
        confirmInput.setOnEditorActionListener(done);

        boolean register = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_REGISTER_MODE, false);
        setRegisterMode(register);
    }

    private void setRegisterMode(boolean register) {
        registerMode = register;
        titleText.setText(register ? R.string.login_title_register : R.string.login_title_login);
        subtitleText.setText(register ? R.string.login_subtitle_register : R.string.login_subtitle_login);
        submitButton.setText(register ? R.string.login_button_register : R.string.login_button_login);
        switchButton.setText(register ? R.string.login_switch_to_login : R.string.login_switch_to_register);
        confirmLayout.setVisibility(register ? View.VISIBLE : View.GONE);
        passwordInput.setImeOptions(register ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
        usernameLayout.setHelperText(register ? getString(R.string.login_username_helper) : null);
        passwordLayout.setHelperText(register ? getString(R.string.login_password_helper) : null);
        usernameLayout.setError(null);
        passwordLayout.setError(null);
        confirmLayout.setError(null);
    }

    private void submit() {
        String username = text(usernameInput);
        String password = text(passwordInput);

        boolean valid;
        if (registerMode) {
            valid = showError(usernameLayout, ValidationUtils.validateUsername(username))
                    & showError(passwordLayout, ValidationUtils.validatePassword(password))
                    & showError(confirmLayout, ValidationUtils.validatePasswordConfirm(password, text(confirmInput)));
        } else {
            // when logging in, only check that both boxes are filled
            valid = showError(usernameLayout, username.trim().isEmpty() ? ValidationUtils.Error.REQUIRED : ValidationUtils.Error.NONE)
                    & showError(passwordLayout, password.isEmpty() ? ValidationUtils.Error.REQUIRED : ValidationUtils.Error.NONE);
        }
        if (!valid) {
            return;
        }

        submitButton.setEnabled(false);
        (registerMode ? viewModel.register(username, password) : viewModel.login(username, password))
                .observe(this, result -> {
                    submitButton.setEnabled(true);
                    handleResult(result);
                });
    }

    private void handleResult(AuthRepository.AuthResult result) {
        switch (result.status) {
            case SUCCESS:
                Toast.makeText(this, getString(R.string.login_welcome_user,
                        SessionManager.displayName(result.username)), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                break;
            case USERNAME_TAKEN:
                usernameLayout.setError(getString(R.string.login_error_taken));
                break;
            case WRONG_CREDENTIALS:
                passwordLayout.setError(getString(R.string.login_error_wrong));
                break;
            default:
                Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    // shows the message for an error under the box; returns true if there is no error
    private boolean showError(TextInputLayout layout, ValidationUtils.Error error) {
        String message;
        switch (error) {
            case NONE:
                message = null;
                break;
            case USERNAME_INVALID:
                message = getString(R.string.login_error_username);
                break;
            case PASSWORD_TOO_SHORT:
                message = getString(R.string.login_error_password_short);
                break;
            case PASSWORDS_DONT_MATCH:
                message = getString(R.string.login_error_password_match);
                break;
            case REQUIRED:
            default:
                message = getString(R.string.error_required);
                break;
        }
        layout.setError(message);
        return message == null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_REGISTER_MODE, registerMode);
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private static void clearErrorOnEdit(TextInputEditText input, TextInputLayout layout) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null) {
                    layout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}