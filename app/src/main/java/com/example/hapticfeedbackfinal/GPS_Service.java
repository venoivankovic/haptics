package com.example.hapticfeedbackfinal;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.jakewharton.threetenabp.AndroidThreeTen;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import static com.example.hapticfeedbackfinal.App.CHANNEL_ID;

public class GPS_Service extends Service {

    private static final String TAG = "GPSIntentService";
    private double myLongitude;
    private double myLatitude;

    Notification notification;

    private double distance;
    LocalDateTime targetTime;
    
    boolean stopLoop;
    boolean calculateDist;


    String targetLongitudeString;
    String targetLatitudeString;

    String decision;
    String oldDecision;

    PowerManager.WakeLock wl;
    PowerManager pm;

    double DIST = 50 ;

    private double targetLongitude;
    private double targetLatitude;

    private FusedLocationProviderClient client;
    private LocationRequest locationRequest;
    private LocationCallback callback;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */

    @SuppressLint({"MissingPermission"})
    @Override
    public void onCreate() {
        super.onCreate();
        calculateDist = false;

        AndroidThreeTen.init(this);
        decision = "";
        oldDecision = "";

        Log.d(TAG, "onCreate");

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "service:myWLTag");
        wl.acquire();

        client = new FusedLocationProviderClient(this);
        callback = new LocationCallback();
        

        Log.d(TAG, "Wakelock acquired");
        //makeTheNotification();
        //locationStuff();

    }

    @SuppressLint("MissingPermission")
    public void locationStuff(){
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setFastestInterval(1000);
        locationRequest.setInterval(1000);
        client.requestLocationUpdates(locationRequest,new LocationCallback(){

            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(stopLoop){
                    client.removeLocationUpdates(this);
                }
                super.onLocationResult(locationResult);
                myLatitude = locationResult.getLastLocation().getLatitude();
                myLongitude = locationResult.getLastLocation().getLongitude();
                sendLocationBroadcast(myLatitude,myLongitude);
                // send gps to main activity
                if(calculateDist){
                    distance = distance(myLatitude,myLongitude,targetLatitude,targetLongitude);
                    System.out.println("Do Location Stuff " + wl.isHeld());
                    long timeDiff = calculateTimeDifference(targetTime);
                    String dec = doHaptic(distance,timeDiff);
                    updateNotification(dec,distance,timeDiff); 
                }
            }
        }, Looper.myLooper());
    }

    public void sendLocationBroadcast(double myLatitude, double myLongitude){
        Intent i = new Intent("location_update");
        i.putExtra("myLat",myLatitude);
        i.putExtra("myLong",myLongitude);
        sendBroadcast(i);
    }


    private void updateNotification(String text, double distance, long timeDiff){
        int value = (int)distance;
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
             notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                     .setContentTitle(text)
                     .setContentText(""+value+"m "+" "+timeDiff+"s ")
                     .setContentIntent(pendingIntent)
                     .setSmallIcon(R.drawable.ic_launcher_background)
                     .setPriority(NotificationManager.IMPORTANCE_HIGH)
                     .setOngoing(true)
                     .setOnlyAlertOnce(true)
                     .build();
        startForeground(1,notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean sentInfo = intent.getBooleanExtra("sentInfo", false);
        if(sentInfo){
            targetLatitude = intent.getDoubleExtra("targetLat",69);
            targetLongitude = intent.getDoubleExtra("targetLong",420);
            targetTime = (LocalDateTime) intent.getSerializableExtra("targetTime");
            String timeString = intent.getStringExtra("timeString");
            int [] parts = convertStringToTimeInt(timeString);
            targetTime = targetTime.withHour(parts[0]);
            targetTime = targetTime.withMinute(parts[1]);
            System.out.println(targetTime.toString());
            distance = distance(myLatitude,myLongitude,targetLatitude,targetLongitude);
            String distanceString = Double.toString(distance);
            updateNotification("Starting GPS",0,0);
            stopLoop = false;
            calculateDist = true;
            locationStuff();
        }else {
            calculateDist = false;
            locationStuff();
        }
        //do heavy work on background thread
        //so handler which updates target time is done here
        return START_NOT_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        stopForeground(true);
        stopSelf();
        System.out.println("release WL " +wl.isHeld());
        Log.d(TAG, "Wakelock released");
        wl.release();
        stopLoop = true;
        System.out.println("release WL " +wl.isHeld());
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double r = 6371;
        return (c * r)*1000;
    }

    public static long calculateTimeDifference(LocalDateTime exp) {
        System.out.println(LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        return ChronoUnit.SECONDS.between(now, exp);
    }

    public void updateTargetTime(){
        //done in new thread
    }



    public void hapticVibrate(int x, int y, int z){
        if(decision != oldDecision){
            long[] pattern = {0, x, y, z};
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern,-1));
            } else {
                v.vibrate(pattern,-1);
            }
        }
    }

    public void testHaptic(int x, int y, int z){
        long[] pattern = {0, x, y, z};
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern,-1));
        } else {
            v.vibrate(pattern,-1);
        }
    }

    private int [] convertStringToTimeInt(String timeString){
        String [] parts = timeString.split(":");
        int [] result = new int[2];
        for (int i = 0; i < parts.length; i++){
            String part = parts[i];
            int partInt = Integer.valueOf(part);
            result[i] = partInt;
        }
        return result;
    }

    public String doHaptic(double dist, long diff) {
        if(distance<16){
            hapticVibrate(1000, 10, 0);
            decision = "You have arrived";
            return decision;
        }

        double speed = dist/diff;
        System.out.println("You need a speed of: " + speed + " m/s to get to the bus on time.");
        if(speed < 0.01 && speed > 0) {
            decision = "Sleep, relax, you have time";
            hapticVibrate(1000, 10, 0);
            oldDecision = decision;
        }
        else if(speed >= 0.01 && speed < 0.1) {
            decision = "Wake up, eat, take shower";
            hapticVibrate(1000, 1000, 1000);
            oldDecision = decision;
        }
        else if(speed >= 0.1 && speed < 0.5) {
            decision = "Get ready";
            hapticVibrate(1400, 1000, 1400);
            oldDecision = decision;
        }
        else if(speed >= 0.5 && speed < 1.0) {
            decision = "Start heading";
            hapticVibrate(1800, 1500, 1800);
            oldDecision = decision;
        }
        else if(speed >= 1.0 && speed < 1.35) {
            decision = "Walk Normally";
            hapticVibrate(1800, 1800, 1800);
            oldDecision = decision;
        }
        else if(speed >= 1.35 && speed < 1.8) {
            decision = "Walk Faster";
            hapticVibrate(2500, 1800, 2500);
            oldDecision = decision;
        }
        else if(speed >= 1.8  && speed < 2.5) {
            decision = "RUN!";
            hapticVibrate(3000, 1800, 3000);
            oldDecision = decision;
        }
        else if(speed >= 2.5 && speed < 12.27) {
            decision = "SPRINT";
            hapticVibrate(5000, 1800, 5000);
            oldDecision = decision;
        }
        else if(speed >= 12.27) {
            decision = "You may or may not be late";
            hapticVibrate(5000, 1800, 5000);
            oldDecision = decision;
        }
        else if(speed < 0){
            decision = "Too late";
            hapticVibrate(5000, 1800, 5000);
            oldDecision = decision;
            stopLoop = true;
        }
        return decision;
    }

}
