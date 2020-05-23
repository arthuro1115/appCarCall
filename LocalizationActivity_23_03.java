package com.example.lg_beta;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.telephony.SmsManager;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.security.Permission;
import java.util.List;
import java.util.Locale;

public class LocalizationActivity extends AppCompatActivity{

    private static final int MY_PERMISSION_REQUEST_SEND_SMS = 0;
    Button btLocation;
    TextView txLocation;
    TextView txLocation2;

    FusedLocationProviderClient fusedLocationProviderClient;

    boolean sucessLocation = false;

    double sendLatitude = 0;
    double sendLongitude = 0;

    String strSendLatitude;
    String strSendLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localization);

        btLocation = findViewById(R.id.btLocation);
        txLocation = findViewById(R.id.txLocation);
        txLocation2 = findViewById(R.id.txLocation2);

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

        btLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(LocalizationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    //Se a for permitido
                    getLocation();

                    if (sucessLocation == true){

                        sendTextSMS();

                    }

                }else{
                    //Se não for permitido
                    ActivityCompat.requestPermissions(LocalizationActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},44);
                }
            }
        });


        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        //Coloca Home Como principal
        bottomNavigationView.setSelectedItemId(R.id.localization);
        //Selecionar Abas
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                switch (menuItem.getItemId()){
                    case R.id.profile:
                        startActivity(new Intent(getApplicationContext()
                                , ProfileActivity.class));
                        overridePendingTransition(0, 0);
                    case R.id.home:
                        startActivity(new Intent(getApplicationContext()
                                , MainActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.localization:
                        return true;
                }
                return false;
            }
        });

    }

    protected void sendTextSMS() {

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage("19988430607",null,"Latitude:" + strSendLatitude + "" + "Longitude:" + strSendLongitude,null,null );
        Toast.makeText(this, "Enviando Sua Localização",Toast.LENGTH_LONG).show();
    }

    public void onRequestPermissionResult(int requestCode, String permission [], int[] grantResults){

        switch (requestCode){

            case MY_PERMISSION_REQUEST_SEND_SMS:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    Toast.makeText(this, "Permissão Aceita",Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(this, "Permissão Não Aceita",Toast.LENGTH_LONG).show();
                }

            }

        }



    }

    private void getLocation() {
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    //Pegando Localização
                    Location location = task.getResult();
                    if (location != null){
                    try {
                        Geocoder geocoder = new Geocoder(LocalizationActivity.this, Locale.getDefault());

                        //Endereço

                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        //set latitude textview

                        txLocation.setText(Html.fromHtml("<font color='#6200EE'><b>Latitude :</b><br></font>" + addresses.get(0).getLatitude()));

                        //set Longitude
                        txLocation2.setText(Html.fromHtml("<font color='#6200EE'><b>Longitude :</b><br></font>" + addresses.get(0).getLongitude()));

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

}
