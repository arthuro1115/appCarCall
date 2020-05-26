package com.example.lg_beta;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.w3c.dom.Text;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button btnconnect;
    Button btdebug;

    private static final int MY_PERMISSION_REQUEST_SEND_SMS = 0;
    private static final int SOLICITA_ATIVACAO = 1;
    private static final int SOLICITA_CONEXAO = 2;
    private static final int MESSAGE_READ = 3;

    ConnectedThread connectedThread;
    FusedLocationProviderClient fusedLocationProviderClient;

    Handler handler;
    StringBuilder dadosBluetooth = new StringBuilder();

    BluetoothAdapter meuBluetoothAdapter = null;
    BluetoothDevice meuDevice = null;
    BluetoothSocket meuSocket = null;

    boolean conexao = false;
    boolean sucessLocation = false;

    int countSend = 0;

    double sendLatitude = 0;
    double sendLongitude = 0;

    String strSendLatitude;
    String strSendLongitude;

    private static String MAC = null;

    UUID MEU_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Pegar o ID dos Button's
        btnconnect = (Button) findViewById(R.id.btnconnect);
        btdebug = (Button) findViewById(R.id.btdebug);


        meuBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //Checar a permissão do SMS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){

            //Caso A Permissão não seja permitida
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)){

            }else{

                //
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS},MY_PERMISSION_REQUEST_SEND_SMS);

            }

        }
        //aq

        //Verifica se Há um Bluetooth no Smarthphone se tiver solicita Ativação
        if (meuBluetoothAdapter == null) {
            //Dispositivo Não Suportado
            Toast.makeText(getApplicationContext(), "Seu Dispositivo não possui BLUETOOTH", Toast.LENGTH_LONG).show();
        } else if (!meuBluetoothAdapter.isEnabled()) {
            Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(ativaBluetooth, SOLICITA_ATIVACAO);
            Toast.makeText(getApplicationContext(), "Deu Certo", Toast.LENGTH_LONG).show();
        }
        //Quando o botão é pressionado ele solicita a conexão com o bluetooth
        btnconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (conexao) {
                    //desconectar
                    try {
                        meuSocket.close();
                        conexao = false;
                        btnconnect.setText("Conectar");
                        Toast.makeText(getApplicationContext(), "Bluetooth Foi Desconectado", Toast.LENGTH_LONG).show();
                    } catch (IOException erro) {
                        Toast.makeText(getApplicationContext(), "Ocorreu um Erro" + erro, Toast.LENGTH_LONG).show();
                    }
                } else {
                    //conectar
                    Intent abreLista = new Intent(MainActivity.this, ListaDispositivos.class);
                    startActivityForResult(abreLista, SOLICITA_CONEXAO);
                }

            }
        });
        //Botão para enviar um informação para debugar o bluetooth
        btdebug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (conexao) {
                    connectedThread.enviar("sysStart");
                    Toast.makeText(getApplicationContext(), "FOI ENVIADO", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth Não foi conectado", Toast.LENGTH_LONG).show();
                }
            }
        });



        //Processamento de informações recebidas pelo Aplicativo android
        handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.what == MESSAGE_READ) {

                    String recebidos = (String) msg.obj;

                    dadosBluetooth.append(recebidos);

                    int fimInformacao = dadosBluetooth.indexOf("}");

                    if (fimInformacao > 0) {
                        String dadosCompletos = dadosBluetooth.substring(0, fimInformacao);

                        int tamInformacao = dadosCompletos.length();

                        if (dadosBluetooth.charAt(0) == '{') {

                            String dadosFinais = dadosBluetooth.substring(1, tamInformacao);

                            Log.d("RECEBEU", dadosFinais);

                            if (dadosFinais.contains("codloc")) {

                                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                                    //Se a for permitido
                                    getLocation();

                                    if (sucessLocation == true){
                                        sendTextSMS();

                                        if(dadosFinais.contains("codcall")){
                                            emergencyCall();
                                        }

                                    }

                                }else{
                                    //Se não for permitido
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},44);
                                }

                            } else {

                            }

                        }
                        dadosBluetooth.delete(0, dadosBluetooth.length());
                    }

                }

            }
        };



        //TROCA DE TELAS
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        //Coloca Home Como principal
        bottomNavigationView.setSelectedItemId(R.id.home);
        //Selecionar Abas
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.profile:
                        startActivity(new Intent(getApplicationContext()
                                , ProfileActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.home:
                        return true;
                    case R.id.localization:
                        startActivity(new Intent(getApplicationContext()
                                , LocalizationActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                }
                return false;
            }
        });

    }
    //Localização
    private void getLocation() {
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                //Pegando Localização
                Location location = task.getResult();
                if (location != null){
                    try {
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

                        //Endereço

                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                        //Verificar se a Locaziação foi feita
                        sucessLocation = true;

                        //Pega a A longitude e Latitude e armazena
                        sendLatitude = addresses.get(0).getLatitude();
                        sendLongitude = addresses.get(0).getLongitude();

                        //Transforma Double em String
                        strSendLatitude = Double.toString(sendLatitude);
                        strSendLongitude = Double.toString(sendLongitude);


                    } catch (IOException e){

                        e.printStackTrace();
                    }

                }
            }
        });

    }

    //Função SMS
    protected void sendTextSMS() {

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage("19988430607",null,"" + countSend + ".Solicitando Ajuda no LOCAL, Latitude:" + strSendLatitude +
        "" + "Longitude:" + strSendLongitude,null,null );
        Toast.makeText(this, "Enviando Sua Localização",Toast.LENGTH_LONG).show();

        //Mudar mensagem
        countSend = countSend + 1 ;
    }

    //Função Ligação
    protected void emergencyCall(){

        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + "19988430607"));
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 4);
            return;
        }
        startActivity(intent);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){

            case SOLICITA_ATIVACAO:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(getApplicationContext(), "O Bluetooth Foi Ativado",Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(getApplicationContext(), "O App Será Encerrado",Toast.LENGTH_LONG).show();
                    finish();
                }
                break;

            case SOLICITA_CONEXAO:
                if(resultCode == Activity.RESULT_OK) {
                    MAC = data.getExtras().getString(ListaDispositivos.ENDERECO_MAC);
                    //Toast.makeText(getApplicationContext(), "MAC FINAL" + MAC,Toast.LENGTH_LONG).show();
                    meuDevice = meuBluetoothAdapter.getRemoteDevice(MAC);

                    try {
                        meuSocket = meuDevice.createRfcommSocketToServiceRecord(MEU_UUID);

                        meuSocket.connect();

                        conexao = true;

                        connectedThread = new ConnectedThread(meuSocket);
                        connectedThread.start();

                        btnconnect.setText("Desconectar");

                        Toast.makeText(getApplicationContext(), "Você Foi Conectado Com: " + MAC,Toast.LENGTH_LONG).show();


                    } catch (IOException erro){
                        conexao = false;
                        Toast.makeText(getApplicationContext(), "Ocorreu um Erro" + erro,Toast.LENGTH_LONG).show();
                    }


                }else{
                    Toast.makeText(getApplicationContext(), "Falha em Obter o MAC",Toast.LENGTH_LONG).show();

                }
        }


    }


    //Estabelecer o túnel de conexão
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();

            } catch (IOException e) {

            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        //receber os dados q vem do arduino
        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);

                    String dadosBt = new String(mmBuffer, 0 ,numBytes);
                    // Send the obtained bytes to the UI activity.
                    handler.obtainMessage(MESSAGE_READ, numBytes, -1, dadosBt).sendToTarget();

                } catch (IOException e) {

                    break;
                }
            }
        }

        //Enviar informações para o arduino
        public void enviar(String dadosEnviar) {
            byte[] msgBuffer = dadosEnviar.getBytes();
            try {
                mmOutStream.write(msgBuffer);

            } catch (IOException e) {  }
        }
    }
}
//termina
