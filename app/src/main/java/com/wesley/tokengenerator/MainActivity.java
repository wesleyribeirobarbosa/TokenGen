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
    private String key;
    private Timer myTimer = new Timer();
    private ProgressBar mProgressBar;
    private CountDownTimer mCountDownTimer;

    private int prgss=0;
    int delay = 0;   // delay nulo pra chamada do GenerateOtp.
    int interval = 30000;  // intervalo de 30 segundos.

    Handler handler = new Handler();

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
        //Shared Preferences utilizado para armazenar o QR Token
        sharedPreferences = getSharedPreferences("KEY-TOKEN", Context.MODE_PRIVATE);
        //Inicialização dos componentes da UI
        initializeComponents();
        mProgressBar.setVisibility(View.INVISIBLE);
        key = sharedPreferences.getString("KEY-TOKEN", "empty");
        //Verifica se existe um QR Token armazenado para iniciar a geração do OTP Code ou não.
        if(!key.equals("empty")){
            btnGetKey.setVisibility(View.INVISIBLE);
            btnGetKey.setEnabled(false);
            txtQRCodeReturn.setText("QR Token: " + key);
            mProgressBar.setVisibility(View.VISIBLE);
            //Como existe um QR Token armazenado, já inicia a aplicação com a chamada temporizada da geração do OTP Code
            myTimer.scheduleAtFixedRate(new generateOtpToken(), delay, interval);
        }
    }

    //Metodo responsável pelo incremento da barra que demonstra o tempo restante para atualização do OTP Code
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
                //Seta QR Token como vazio para forçar a exigência de nova leitura do QR Code
                editor.putString("KEY-TOKEN","empty");
                editor.apply();
                btnGetKey.setEnabled(true);
                btnGetKey.setVisibility(View.VISIBLE);
                txtCodeOTP.setText("");
                prgss=0;
                txtQRCodeReturn.setText("");
                mCountDownTimer.cancel();
                mProgressBar.setVisibility(View.INVISIBLE);
                myTimer.cancel();
            }
        });
        txtCodeOTP = (TextView) findViewById(R.id.txtQRCode);
    }

    //Retorno da leitura do QR Code
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
                    //Dispara a chamada do temporizador que gerencia o tempo de atualização do Código gerado
                    myTimer.scheduleAtFixedRate(new generateOtpToken(), delay, interval);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Alert("Erro ao interpretar código!");
                }
            }else{
                Alert("Leitura Cancelada!");
            }
        }else{
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class generateOtpToken extends TimerTask  {
        /**
         * Como a utilização de Libs Nativas requer a interface entre linguagens diferentes,
         * o que gera um determinado Overhead, esta sendo utilizado o conceito de paralelismo
         * (nova thread) para gerenciar não só a temporização, mas também o processamento que
         * envolve a chamada do método "generateOtp" da lib externa e os cálculos necessários
         * para a conversão do Hash de 20 bytes para o Códido OTP final de 6 caracteres.
         */

        public void run() {
            // colocar tarefas aqui ...
            byte[] byteArray = new byte[20];
            //Chamada do metodo que gera o Hash de 20 bytes a partir do Token interpretado na leitura do QR Code.
            byteArray = generateOtp(key);
            //pega os 4 bytes úteis para a decodificação.
            byte[] newArray = Arrays.copyOfRange(byteArray, 10, 14);
            String hexaValues = "";
            int valueInt = 0;
            for (int iterator = 0; iterator < newArray.length; iterator++) {
                //Pega apenas o módulo do valor, desconsiderando possíveis valores negativos
                valueInt = Math.abs(newArray[iterator]);
                //"Mascara" feita para garantir o resultado de no máximo 31bits, truncando o primeiro bit (MSB).
                if (iterator == 0) {
                    if (valueInt > 127) {
                        valueInt = valueInt - 128;
                    }
                }
                //Validação para que valores menores que 16 ainda sejam exibidos com 0 a esquerda (1 byte completo)
                if (valueInt < 16) {
                    hexaValues = hexaValues + "0" + Integer.toHexString(valueInt);
                } else
                    hexaValues = hexaValues + Integer.toHexString(Math.abs(newArray[iterator]));
            }
            int decimalCode = Integer.parseInt(hexaValues, 16);
            final String decimalCodeString = Integer.toString(decimalCode);
            //Pega apenas os últimos 6 caracteres do código decimal gerado
            final String otpFinalCode = decimalCodeString.substring(decimalCodeString.length() - 6, decimalCodeString.length());
            //Handler para a interação desta Thread com a View, na Thread principal
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
     * Método nativo implementado na lib "otpjni.cpp"
     */
    public native byte[] generateOtp(String key);
}
