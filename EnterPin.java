package ideanity.oceans.antitheftapp;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

import static ideanity.oceans.antitheftapp.SetPin.MyPREFERENCES;

public class EnterPin extends AppCompatActivity {

    EditText etEnterPin;
    SharedPreferences sharedpreferences;
    MediaPlayer mPlayer;

    @Override
    public void onBackPressed() {
        // Block back button
    }

    @Override
    protected void onPause() {
        super.onPause();
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.moveTaskToFront(getTaskId(), 0);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_enter_pin);

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        final String password = sharedpreferences.getString("passwordKey", "");

        // Show soft keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }

        // Play siren
        mPlayer = MediaPlayer.create(this, R.raw.siren);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        }
        if (mPlayer != null) {
            mPlayer.start();
            mPlayer.setLooping(true);
        }

        etEnterPin = findViewById(R.id.etEnterPin);
        etEnterPin.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                String pin = etEnterPin.getText().toString();
                if (pin.equals(password)) {
                    stopSiren();
                    startActivity(new Intent(EnterPin.this, HomeActivity.class));
                    finish();
                    return true;
                } else {
                    etEnterPin.getText().clear();
                    etEnterPin.setError("Wrong Pin!");
                    etEnterPin.requestFocus();
                }
            }
            return false;
        });

        // Try biometric login
        showBiometricPrompt();
    }

    private void stopSiren() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void showBiometricPrompt() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    stopSiren();
                    startActivity(new Intent(EnterPin.this, HomeActivity.class));
                    finish();
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(getApplicationContext(), "Biometric error: " + errString, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(getApplicationContext(), "Fingerprint not recognized. Try again or use PIN.", Toast.LENGTH_SHORT).show();
                }
            });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Fingerprint Authentication")
                    .setSubtitle("Authenticate to stop the siren")
                    .setNegativeButtonText("Use PIN instead")
                    .build();

            biometricPrompt.authenticate(promptInfo);
        } else {
            Toast.makeText(this, "Fingerprint authentication not available", Toast.LENGTH_SHORT).show();
        }
    }
}
