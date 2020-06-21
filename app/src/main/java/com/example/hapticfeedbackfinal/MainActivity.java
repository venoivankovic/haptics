package com.example.hapticfeedbackfinal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.Month;
import org.threeten.bp.temporal.ChronoUnit;

public class MainActivity extends AppCompatActivity {

    Button startService;
    Button endService;
    TextView distanceText;
    TextView timeLeftText;
    TextView decisionText;
    EditText yourTime;


    private BroadcastReceiver broadcastReceiver;

    /*
    *Do gps location stuff in the actual service, activity should be used only for display...
     */


    double myLat;
    double myLong;

    PowerManager.WakeLock wl;
    PowerManager pm;

    double DIST = 30;

    double TARGET_LATITUDE = 45.808619;
    double TARGET_LONGITUDE = 15.990626;
    LocalDateTime TARGET_TIME = LocalDateTime.of(2020, Month.JUNE,18,16,0,0);
    double howFar;
    boolean sendInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startService = (Button) findViewById(R.id.startButton);
        endService = (Button) findViewById(R.id.endButton);
        distanceText = (TextView) findViewById(R.id.distance);
        timeLeftText = (TextView) findViewById(R.id.time);
        decisionText = (TextView) findViewById(R.id.decision2);
        yourTime = (EditText) findViewById(R.id.setTheTime);

        System.out.println(myLat + " start lat");
        System.out.println(myLong + " start long");
        create_GPS_Service();
    }



    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    myLat =intent.getDoubleExtra("myLat",69);
                    myLong =intent.getDoubleExtra("myLong",420);
                    System.out.println("Lat "+myLat);
                    System.out.println("Long "+myLong);
                    if(!runtime_permissions()){
                        enable_buttons();
                    }
                }
            };
        }
        registerReceiver(broadcastReceiver,new IntentFilter("location_update"));
    }

    private void create_GPS_Service(){
        sendInfo = false;
        Intent i =new Intent(getApplicationContext(),GPS_Service.class);
        i.putExtra("sentInfo",sendInfo);
        startService(i);
    }

    private void enable_buttons() {
        startService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println(myLat + " new lat");
                System.out.println(myLong + " new long");
                sendInfo = true;
                Intent i =new Intent(getApplicationContext(),GPS_Service.class);
                //So put bus object here, maybe also station object... see structure.
                // enable buttons on gps receival
                i.putExtra("sentInfo",sendInfo);
                i.putExtra("targetLat", TARGET_LATITUDE);
                i.putExtra("targetLong", TARGET_LONGITUDE);
                i.putExtra("targetTime",TARGET_TIME);
                String timeString = yourTime.getText().toString();
                i.putExtra("timeString",timeString);

                startService(i);
            }
        });

        endService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(),GPS_Service.class);
                stopService(i);
            }
        });

    }

    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},100);

            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                enable_buttons();
            }else {
                runtime_permissions();
            }
        }
    }




}