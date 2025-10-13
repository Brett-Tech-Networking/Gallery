package com.bretttech.gallery.auth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class BiometricAuthManager {

    public interface BiometricAuthListener {
        void onAuthSuccess();
        void onAuthError(String errString);
        void onAuthFailed();
        void onNoSecurityEnrolled();
    }

    private final AppCompatActivity activity;
    private final BiometricAuthListener listener;

    public BiometricAuthManager(AppCompatActivity activity, BiometricAuthListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void authenticate() {
        BiometricManager biometricManager = BiometricManager.from(activity);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

        switch (biometricManager.canAuthenticate(authenticators)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                showBiometricPrompt(authenticators);
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                listener.onAuthError("No biometric features available on this device.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                listener.onAuthError("Biometric features are currently unavailable.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                listener.onNoSecurityEnrolled();
                break;
        }
    }

    private void showBiometricPrompt(int authenticators) {
        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    listener.onAuthError("Authentication error: " + errString);
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                listener.onAuthSuccess();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                listener.onAuthFailed();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Secure Folder Locked")
                .setSubtitle("Authenticate to access")
                .setAllowedAuthenticators(authenticators)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}