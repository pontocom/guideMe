package pt.iscte_iul.guideme;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class SplashActivity extends Activity implements LocationListener {
    protected LocationManager locationManager;
    public double latitude, longitude;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        // podem ser usados outros providers (GPS_PROVIDER, etc.)!!!

        dialog = new ProgressDialog(SplashActivity.this);
        dialog.setMessage("Getting location... please wait!");
        dialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            Log.i("guideMe", "Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            locationManager.removeUpdates(this);

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    dialog.dismiss();

                    Intent i = new Intent(SplashActivity.this, MainActivity.class);
                    i.putExtra("latitude", latitude);
                    i.putExtra("longitude", longitude);
                    startActivity(i);
                    finish();
                }
            }, 2000);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude", "disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
}
