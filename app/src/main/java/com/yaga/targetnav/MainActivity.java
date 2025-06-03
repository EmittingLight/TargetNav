package com.yaga.targetnav;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.location.LocationListener;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

public class MainActivity extends AppCompatActivity {

    private TextView gpsText, azimuthText, targetCoords;
    private EditText distanceInput;
    private Button calcButton, openCameraButton;

    private LocationManager locationManager;
    private SensorManager sensorManager;

    private float azimuth = 0f;
    private double currentLat = 0;
    private double currentLon = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        gpsText = findViewById(R.id.gpsText);
        azimuthText = findViewById(R.id.azimuthText);
        targetCoords = findViewById(R.id.targetCoords);
        distanceInput = findViewById(R.id.distanceInput);
        calcButton = findViewById(R.id.calcButton);
        openCameraButton = findViewById(R.id.openCameraButton); // 👈 Новая кнопка

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // 📍 Разрешение на геолокацию
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, locationListener);
        }

        // 📍 Датчики
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI);

        // 📌 Кнопка "Рассчитать координату"
        calcButton.setOnClickListener(v -> {
            String distStr = distanceInput.getText().toString();
            if (distStr.isEmpty()) {
                Toast.makeText(this, "Введите расстояние", Toast.LENGTH_SHORT).show();
                return;
            }

            double distance = Double.parseDouble(distStr);
            double bearing = azimuth;
            double R = 6371000.0; // радиус Земли в метрах
            double φ1 = Math.toRadians(currentLat);
            double λ1 = Math.toRadians(currentLon);
            double θ = Math.toRadians(bearing);
            double δ = distance / R;

            double φ2 = Math.asin(Math.sin(φ1) * Math.cos(δ) + Math.cos(φ1) * Math.sin(δ) * Math.cos(θ));
            double λ2 = λ1 + Math.atan2(Math.sin(θ) * Math.sin(δ) * Math.cos(φ1),
                    Math.cos(δ) - Math.sin(φ1) * Math.sin(φ2));

            double targetLat = Math.toDegrees(φ2);
            double targetLon = Math.toDegrees(λ2);

            targetCoords.setText("Координаты цели:\n" + targetLat + ", " + targetLon);
        });

        // 🎯 Кнопка "Открыть прицел"
        openCameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            currentLat = location.getLatitude();
            currentLon = location.getLongitude();
            gpsText.setText("Широта: " + currentLat + "\nДолгота: " + currentLon);
        }
    };

    private final SensorEventListener sensorListener = new SensorEventListener() {
        float[] gravity;
        float[] geomagnetic;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                gravity = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                geomagnetic = event.values;

            if (gravity != null && geomagnetic != null) {
                float[] R = new float[9];
                float[] I = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
                if (success) {
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(R, orientation);
                    azimuth = (float) Math.toDegrees(orientation[0]);
                    if (azimuth < 0) {
                        azimuth += 360;
                    }
                    azimuthText.setText("Азимут: " + (int) azimuth + "°");
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
}
