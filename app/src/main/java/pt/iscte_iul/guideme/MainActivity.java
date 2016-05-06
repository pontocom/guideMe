package pt.iscte_iul.guideme;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class MainActivity extends ActionBarActivity implements OnMapReadyCallback {
    public double latitude, longitude;
    public GoogleMap t_map;
    public Map <Marker, Integer> hmap;
    public static final String PREFS_NAME = "AppData";
    public static final String APPNAME = "guideMe";
    public static final int RANGE = 50;
    public int myRange;
    public String serviceURL;
    public static String APIURL = "http://192.168.1.113:3000";
    protected SharedPreferences appData;
    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_ADD_POI = 2;
    private static final int REQUEST_SETTINGS = 2;

    protected Marker tempMarker;
    protected LatLng latlong;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent i = getIntent();
        latitude = i.getDoubleExtra("latitude", 0);
        longitude = i.getDoubleExtra("longitude", 0);

        appData = getSharedPreferences(PREFS_NAME, 0);
        //try to read from SharedPreferences
        myRange = appData.getInt("range", RANGE);
        serviceURL = appData.getString("URL", APIURL);
        Log.i("guideMe", "Range is = " + myRange);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);
        t_map = mapFragment.getMap();

        t_map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if(marker.getTitle().compareTo("New POI") == 0) {
                    Intent i = new Intent(MainActivity.this, AddPOIActivity.class);
                    i.putExtra("latitude", latlong.latitude);
                    i.putExtra("longitude", latlong.longitude);
                    startActivityForResult(i, REQUEST_ADD_POI);
                } else {
                    Log.i("guideMe", "Click on marker with ID = " + hmap.get(marker));
                    Intent i = new Intent(MainActivity.this, POIDetailsActivity.class);
                    i.putExtra("id", hmap.get(marker));
                    startActivity(i);
                }
            }
        });

        t_map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                if(tempMarker != null) {
                    tempMarker.remove();
                }

                //add a new marker to the map
                tempMarker = t_map.addMarker(new MarkerOptions()
                        .title("New POI")
                        .position(latLng));

                latlong = latLng;

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(APPNAME, "-> onResume()");

        // delete all POIs from map
        if(hmap != null) {
            if(!hmap.isEmpty()) {
                Log.i(APPNAME, "hmap is not empty, so we can delete the POIs");
                for(Map.Entry<Marker,Integer> entry : hmap.entrySet()) {
                    entry.getKey().remove();
                }
                // get and add new POIs
                new getPOIs().execute();
            } else {
                new getPOIs().execute();
                Log.i(APPNAME, "hmap is empty, so do nothing!!!");
            }
        }

        if(tempMarker!=null) {
            tempMarker.remove();
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        LatLng myPos = new LatLng(latitude, longitude);


        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 13));

        new getPOIs().execute();
    }

    private class getPOIs extends AsyncTask<String, Void, String> {
        private ProgressDialog dialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute()
        {
            dialog.setMessage("Getting POIs from server...");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params)
        {
            String response = null;

            try {
                myRange = appData.getInt("range", RANGE);
                Log.i("guideMe", "Using this range = " + myRange);
                HttpClient httpclient = new DefaultHttpClient();
                Log.i("guideMe", "Asking  = " + serviceURL + "/poi/range/"+latitude+"/"+longitude+"/"+myRange*1000);

                HttpResponse httpResponse = httpclient.execute(new HttpGet(new URI(serviceURL + "/poi/range/"+latitude+"/"+longitude+"/"+myRange*1000)));

                BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
                response = reader.readLine();

                Log.i("guideMe", "Response = " + response);

            } catch(Exception e) {
                Log.i("guideMe", "An exception has occured in the connection - " + e.toString());
                return "-ERR";
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result)
        {
            dialog.dismiss();

            try {
                // now, we have to handle all the necessary results and add them to the map
                JSONObject jobj = new JSONObject(result);

                if(jobj.get("status").toString().compareTo("OK")==0) {
                    JSONArray poi = jobj.getJSONArray("poi");
                    hmap = new HashMap<Marker, Integer>();
                    for(int i=0; i<poi.length(); i++) {
                        JSONObject t_poi = poi.getJSONObject(i);
                        Log.i("guideMe", "-> " + t_poi.getString("name"));

                        //add the marker to the map
                        Marker m = t_map.addMarker(new MarkerOptions()
                                .title(t_poi.getString("name"))
                                .snippet(t_poi.getString("address"))
                                .position(new LatLng(t_poi.getDouble("latt"), t_poi.getDouble("logt"))));


                        hmap.put(m, t_poi.getInt("id"));
                    }
                }

            } catch (Exception e) {
                Log.i("guideMe", "An exception has occured in the connection - " + e.toString());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(i, REQUEST_SETTINGS);
            return true;
        }

        if(id == R.id.addpoiid) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_CODE);

        }

        if(id == R.id.refreshmap) {
            // delete all POIs from map
            if(hmap != null) {
                if(!hmap.isEmpty()) {
                    Log.i(APPNAME, "hmap is not empty, so we can delete the POIs");
                    for(Map.Entry<Marker,Integer> entry : hmap.entrySet()) {
                        entry.getKey().remove();
                    }
                    // get and add new POIs
                    new getPOIs().execute();
                } else {
                    Log.i(APPNAME, "hmap is empty, so do nothing!!!");
                    new getPOIs().execute();
                }
            }
            if(tempMarker!=null) {
                tempMarker.remove();
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
