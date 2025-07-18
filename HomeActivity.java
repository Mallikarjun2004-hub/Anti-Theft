package ideanity.oceans.antitheftapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class HomeActivity extends AppCompatActivity implements SensorEventListener {

    Switch motionSwitch, proximitySwitch, chargerSwitch;
    CountDownTimer cdt;
    private SensorManager sensorMan;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Sensor accelerometer;
    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;
    AlertDialog alertDialog;
    private static final int SENSOR_SENSITIVITY = 4;
    private static final int SMS_PERMISSION_REQUEST_CODE = 1;

    TextView rememberPassword;
    int mSwitchSet, pSwitchSet = 0;
    int chargerFlag, chargerFlag1, chargerFlag2 = 0;

    @Override
    public void onResume() {
        super.onResume();
        sensorMan.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorMan.unregisterListener(this);
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_home);

        // Request SMS Permission at Runtime
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        alertDialog = new AlertDialog.Builder(this).create();
        chargerSwitch = findViewById(R.id.sCharger);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                if (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                    chargerFlag = 1;
                } else if (plugged == 0) {
                    chargerFlag1 = 1;
                    chargerFlag = 0;
                    func();
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(receiver, filter);

        chargerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (chargerFlag != 1) {
                    Toast.makeText(HomeActivity.this, "Connect To Charger", Toast.LENGTH_SHORT).show();
                    chargerSwitch.setChecked(false);
                } else {
                    Toast.makeText(HomeActivity.this, "Charger Protection Mode On", Toast.LENGTH_SHORT).show();
                    chargerFlag2 = 1;
                    func();
                }
            } else {
                chargerFlag2 = 0;
            }
        });

        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        motionSwitch = findViewById(R.id.sMotion);
        motionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                alertDialog.setTitle("Will Be Activated In 10 Seconds");
                alertDialog.setMessage("00:10");
                cdt = new CountDownTimer(10000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        alertDialog.setMessage("00:" + (millisUntilFinished / 1000));
                    }

                    @Override
                    public void onFinish() {
                        mSwitchSet = 1;
                        alertDialog.hide();
                        Toast.makeText(HomeActivity.this, "Motion Detection Mode Activated", Toast.LENGTH_SHORT).show();
                    }
                }.start();
                alertDialog.show();
                alertDialog.setCancelable(false);
            } else {
                Toast.makeText(HomeActivity.this, "Motion Switch Off", Toast.LENGTH_SHORT).show();
                mSwitchSet = 0;
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void func() {
        if (chargerFlag == 0 && chargerFlag1 == 1 && chargerFlag2 == 1) {
            startActivity(new Intent(HomeActivity.this, EnterPin.class));
            chargerFlag2 = 0;
            finish();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values.clone();
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            if (mAccel > 0.5) { // Motion detected
                if (mSwitchSet == 1) {
                    startActivity(new Intent(HomeActivity.this, EnterPin.class));
                    sendTheftAlert();
                    finish();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void sendTheftAlert() {
        String[] phoneNumbers = {"+917411450974", "+919380091850","+917337855431"}; // Add numbers here
        String message = "🚨 Alert! Your phone may be stolen! 🚨";

        SmsManager smsManager = SmsManager.getDefault();

        for (String phoneNumber : phoneNumbers) {
            try {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Failed to send SMS to " + phoneNumber, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        Toast.makeText(getApplicationContext(), "Alert SMS Sent!", Toast.LENGTH_LONG).show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS Permission Granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
