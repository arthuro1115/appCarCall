package com.example.lg_beta;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.w3c.dom.Text;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button btnconnect;
    //Button btBut;

    private static final int SOLICITA_ATIVACAO = 1;
    private static final int SOLICITA_CONEXAO = 2;
    private static final int MESSAGE_READ = 3;

    ConnectedThread connectedThread;

    Handler handler;
    StringBuilder dadosBluetooth = new StringBuilder();

    BluetoothAdapter meuBluetoothAdapter = null;
    BluetoothDevice meuDevice = null;
    BluetoothSocket meuSocket = null;

    boolean conexao = false;

    private static String MAC = null;

    UUID MEU_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //casting
        btnconnect = (Button) findViewById(R.id.btnconnect);
        // btBut = (Button) findViewById(R.id.btBut);


        meuBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (meuBluetoothAdapter == null) {
            //Dispositivo Não Suportado
            Toast.makeText(getApplicationContext(), "Seu Dispositivo não possui BLUETOOTH", Toast.LENGTH_LONG).show();
        } else if (!meuBluetoothAdapter.isEnabled()) {
            Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(ativaBluetooth, SOLICITA_ATIVACAO);
            Toast.makeText(getApplicationContext(), "Deu Certo", Toast.LENGTH_LONG).show();
        }
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

        //aq
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

                            if (dadosFinais.contains("bateu")) {

                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + "19988430607"));
                                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE},4);
                                    return;
                                }
                                startActivity(intent);



                            }else{

                            }

                        }
                        dadosBluetooth.delete(0,dadosBluetooth.length());
                    }

                }

            }
        };




        //dps aq



        //TROCA DE TELAS
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        //Coloca Home Como principal
        bottomNavigationView.setSelectedItemId(R.id.home);
        //Selecionar Abas
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                switch (menuItem.getItemId()){
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
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
    //Começa
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

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

            } catch (IOException e) { }
        }
    }
}
//termina

