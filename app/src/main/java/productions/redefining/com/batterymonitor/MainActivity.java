package productions.redefining.com.batterymonitor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.button;

public class MainActivity extends AppCompatActivity {

    public LocationManager lm;
    public Location location;


    public Long getBatteryCapacity(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager mBatteryManager = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
            Long chargeCounter = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
            return chargeCounter;
        }
        return Long.valueOf(0);
    }

    public double calculationTemperature(Long capacity, float voltage) {

        //theoretical value
        double theoreticalValue = ((-0.0011) * (voltage) * (voltage)) + (.9129 * voltage) - 186.4419095;

        double c_ratio = ((capacity / 1000000.0 ) / (theoreticalValue)) * 100.0;

        double final_temp = ((1.1957) * c_ratio) - 13.344;

        return final_temp;
    }

    public float batteryVoltage(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        float voltage = ((float) intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)) / 10;
        return voltage;

    }

    protected PowerManager.WakeLock mWakeLock;
    public void wasteResources() {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = 1F;
        getWindow().setAttributes(layout);

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();

    }

    public void GPSLocation() {

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    private double calculateAverage(List <Double> list) {
        Double sum = 0.0;
        if(!list.isEmpty()) {
            for (Double mark : list) {
                sum += mark;
            }
            return sum.doubleValue() / list.size();
        }
        return sum;
    }

    Button tempButton;
    TextView temperatureToDisplay;
    TextView loadingStatus;

    public void changeLoadingText() {
        if (loadingStatus.getText().toString() == "Loading...") {
            loadingStatus.setText("Loading.");
        }
        else if (loadingStatus.getText().toString() == "Loading..") {
            loadingStatus.setText("Loading...");
        }
        else if (loadingStatus.getText().toString() == "Loading.") {
            loadingStatus.setText("Loading..");
        }
        else {
            loadingStatus.setText("Loading.");
        }

    }

    public long time_at_run = System.currentTimeMillis();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wasteResources();

        tempButton = (Button) findViewById(R.id.tempButton);
        temperatureToDisplay = (TextView) findViewById(R.id.temperature);
        loadingStatus = (TextView) findViewById(R.id.status);

        final List<Double> temperatures = new ArrayList<Double>();

        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {

                Long capacity = (getBatteryCapacity(getApplicationContext()));
                float voltage = batteryVoltage(getApplicationContext());


                double instanteous_temp = calculationTemperature(capacity, voltage);
                temperatures.add(instanteous_temp);



                if ( (System.currentTimeMillis() - time_at_run) < 5000) {
                    handler.postDelayed(this, 500);
                    //waste processor
                    changeLoadingText();
                    GPSLocation();
                }
                else {

                    //display temperature
                    tempButton.setEnabled(true);
                    tempButton.setBackground(getResources().getDrawable(R.drawable.circular));
                    //get average temperature
                    double average_temperature =  calculateAverage(temperatures);
                    String stringAvTemp  = Double.toString(average_temperature);
                    temperatureToDisplay.setText(stringAvTemp.substring(0, 4));
                    loadingStatus.setText("");

                }
            }
        };

        tempButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                tempButton.setEnabled(false);
                tempButton.setBackground(getResources().getDrawable(R.drawable.circular_deactivated));



                temperatures.clear();
                time_at_run = System.currentTimeMillis();
                handler.postDelayed(runnable, 500);



            }
        });







    }


}
