package com.wesley.tokengenerator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private Button btnGetAccess;

    private Handler handler = new Handler();

    private Executor executor = new Executor() {
        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#1EA1FF")));
        setTitle(getApplicationContext().getResources().getString(R.string.name));
        initComponents();

    }

    private void initComponents() {

        btnGetAccess = (Button) findViewById(R.id.btnGetAccess);
        btnGetAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBiometricPrompt();
            }
        });
    }

    private void showBiometricPrompt() {
        BiometricPrompt.PromptInfo promptInfo =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Reconhecimento biométrico necessário!")
                        .setSubtitle("Por favor, posicione o dedo sobre o leitor biométrico")
                        .setNegativeButtonText("Cancelar")
                        .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(LoginActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                        "Erro de Autenticação: " + errString, Toast.LENGTH_SHORT)
                        .show();
                //Mata todos processos associados a este aplicativo.
                android.os.Process.killProcess(android.os.Process.myPid());
                //Fecha o aplicativo.
                System.exit(1);
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                BiometricPrompt.CryptoObject authenticatedCryptoObject =
                        result.getCryptoObject();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Falha na autenticação",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });

        // Displays the "log in" prompt.
        biometricPrompt.authenticate(promptInfo);
    }
}
