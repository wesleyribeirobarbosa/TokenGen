package com.wesley.tokengenerator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private Button btnGetKey;
    private Button btnResetData;
    private TextView txtQRCode;
    private final Activity activity = this;
    private SharedPreferences sharedPreferences;

    //Carrega a Lib responsável pela chamada do método de geração do Hash
    static {
        System.loadLibrary("otpjni");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences("KEY-TOKEN", Context.MODE_PRIVATE);
        initializeComponents();
        String keyToken = sharedPreferences.getString("KEY-TOKEN", "empty");
        if(!keyToken.equals("empty")){
            btnGetKey.setVisibility(View.INVISIBLE);
            btnGetKey.setEnabled(false);
            txtQRCode.setText(keyToken);
        }
    }

    private void initializeComponents() {

        btnGetKey = (Button) findViewById(R.id.btnGetKey);
        btnGetKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(activity);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                integrator.setPrompt("Camera Scan");
                integrator.setCameraId(0);
                integrator.initiateScan();
            }
        });
        btnResetData = (Button) findViewById(R.id.btnResetData);
        btnResetData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("KEY-TOKEN","empty");
                editor.apply();
                btnGetKey.setEnabled(true);
                btnGetKey.setVisibility(View.VISIBLE);
                txtQRCode.setText("");
            }
        });
        txtQRCode = (TextView) findViewById(R.id.txtQRCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
        if(result != null){
            if(result.getContents() != null){
                try {
                    JSONObject scanQR = new JSONObject(result.getContents());
                    String key  = scanQR.getString("key");
                    txtQRCode.setText(key);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("KEY-TOKEN",key);
                    editor.apply();
                    btnGetKey.setEnabled(false);
                    btnGetKey.setVisibility(View.INVISIBLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Alert("Erro ao interpretar código!");
                }
            }else{
                Alert("Canceled!");
            }
        }else{
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void Alert(String msg){
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * A native method that is implemented by the 'otpjni' library,
     * which is packaged with this application.
     */
    public native String generateOtp(String key);
}
