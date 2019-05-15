package com.wesley.tokengenerator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity{

    private Button btnGetKey;
    private Button btnResetData;
    private TextView txtCodeOTP;
    private TextView txtQRCodeReturn;
    private final Activity activity = this;
    private SharedPreferences sharedPreferences;
    private static int offset = 10;
    private String key;
    int delay = 0;   // delay de 1 seg.
    int interval = 30000;  // intervalo de 1 seg.
    Handler handler = new Handler();
    private Timer myTimer = new Timer();
    private ProgressBar mProgressBar;
    private CountDownTimer mCountDownTimer;
    private int prgss=0;

    //Carrega a Lib responsável pela chamada do método de geração do Hash
    static {
        System.loadLibrary("otpjni");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#144DC7")));
        setTitle("Token Generator");
        sharedPreferences = getSharedPreferences("KEY-TOKEN", Context.MODE_PRIVATE);
        initializeComponents();
        mProgressBar.setVisibility(View.INVISIBLE);
        key = sharedPreferences.getString("KEY-TOKEN", "empty");
        if(!key.equals("empty")){
            btnGetKey.setVisibility(View.INVISIBLE);
            btnGetKey.setEnabled(false);
            txtQRCodeReturn.setText("QR Token: " + key);
            mProgressBar.setVisibility(View.VISIBLE);
            myTimer.scheduleAtFixedRate(new generateOtpToken(), delay, interval);
        }
    }

    private void initCountDownTimer(){
        mCountDownTimer=new CountDownTimer(30000,1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                prgss++;
                mProgressBar.setProgress((int)prgss*100/(30000/1000));
                if((int)prgss*100/(30000/1000)==80){
                    txtCodeOTP.setTextColor(getResources().getColor(R.color.redAlert));
                }
            }

            @Override
            public void onFinish() {
                //Do what you want
                prgss++;
                mProgressBar.setProgress(100);
            }
        };
    }

    private void initializeComponents() {

        initCountDownTimer();
        txtQRCodeReturn = (TextView) findViewById(R.id.txtQRCodeReturn);
        mProgressBar=(ProgressBar)findViewById(R.id.progressbar);
        mProgressBar.setProgress(prgss);
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
                txtCodeOTP.setText("");
                prgss=0;
                mCountDownTimer.cancel();
                mProgressBar.setVisibility(View.INVISIBLE);
                myTimer.cancel();
            }
        });
        txtCodeOTP = (TextView) findViewById(R.id.txtQRCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
        if(result != null){
            if(result.getContents() != null){
                try {
                    mProgressBar.setVisibility(View.VISIBLE);
                    myTimer = new Timer();
                    JSONObject scanQR = new JSONObject(result.getContents());
                    key = scanQR.getString("key");
                    txtQRCodeReturn.setText("QR Token: " + key);
                    prgss=0;
                    txtCodeOTP.setTextColor(getResources().getColor(R.color.gray));
                    mCountDownTimer.start();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("KEY-TOKEN",key);
                    editor.apply();
                    btnGetKey.setEnabled(false);
                    btnGetKey.setVisibility(View.INVISIBLE);
                    myTimer.scheduleAtFixedRate(new generateOtpToken(), delay, interval);

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

    private class generateOtpToken extends TimerTask  {

        public void run() {
            // colocar tarefas aqui ...
            byte[] byteArray = new byte[20];
            byteArray = generateOtp(key);
            byte[] newArray = Arrays.copyOfRange(byteArray, 10, 14);
            String hexaValues = "";
            int valueInt = 0;
            for (int iterator = 0; iterator < newArray.length; iterator++) {
                valueInt = Math.abs(newArray[iterator]);
                if (iterator == 0) {
                    if (valueInt > 127) {
                        valueInt = valueInt - 128;
                    }
                }
                if (valueInt < 16) {
                    hexaValues = hexaValues + "0" + Integer.toHexString(valueInt);
                } else
                    hexaValues = hexaValues + Integer.toHexString(Math.abs(newArray[iterator]));
            }
            int decimalCode = Integer.parseInt(hexaValues, 16);
            final String decimalCodeString = Integer.toString(decimalCode);
            final String otpFinalCode = decimalCodeString.substring(decimalCodeString.length() - 6, decimalCodeString.length());
            handler.post(new Runnable() {
                public void run() {
                    txtCodeOTP.setText(otpFinalCode);
                    prgss=0;
                    txtQRCodeReturn.setText("QR Token: " + key);
                    txtCodeOTP.setTextColor(getResources().getColor(R.color.gray));
                    mCountDownTimer.cancel();
                    initCountDownTimer();
                    mCountDownTimer.start();
                }
            });
        }

    }

    private void Alert(String msg){
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * A native method that is implemented by the 'otpjni' library,
     * which is packaged with this application.
     */
    public native byte[] generateOtp(String key);
}
