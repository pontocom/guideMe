package pt.iscte_iul.guideme;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;


public class POIDetailsActivity extends ActionBarActivity {
    protected int idPOI;
    protected String defaultURL;
    TextView nameTV, addressTV, descriptionTV;
    ImageView imageIV;

    public static final String PREFS_NAME = "AppData";
    public static final String APPNAME = "guideMe";
    public static String APIURL = "http://192.168.1.113:3000/";
    public String serviceURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poidetails);

        nameTV = (TextView) findViewById(R.id.nameTextView);
        addressTV = (TextView) findViewById(R.id.addressTextView);
        descriptionTV = (TextView) findViewById(R.id.descriptionTextView);
        imageIV = (ImageView) findViewById(R.id.POIimageView);


        Intent i = getIntent();
        idPOI = i.getIntExtra("id", 0);
        Log.i("guideMe", "Received POI with ID = " + idPOI);

        SharedPreferences appData = getSharedPreferences(PREFS_NAME, 0);
        //try to read from SharedPreferences
        defaultURL = appData.getString("URL", "");

        new getPOIDetails().execute();
    }

    private class getPOIDetails extends AsyncTask<String, Void, String> {
        private ProgressDialog dialog = new ProgressDialog(POIDetailsActivity.this);

        @Override
        protected void onPreExecute()
        {
            dialog.setMessage("Getting details from server...");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params)
        {
            String response = null;

            try {
                HttpClient httpclient = new DefaultHttpClient();
                Log.i("guideMe", "Asking  = " + defaultURL + "/poi/"+idPOI);

                HttpResponse httpResponse = httpclient.execute(new HttpGet(new URI(defaultURL + "/poi/"+idPOI)));

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
                    JSONObject poi = jobj.getJSONArray("poi").getJSONObject(0);

                    nameTV.setText(poi.get("name").toString());
                    addressTV.setText(poi.get("address").toString());
                    descriptionTV.setText(poi.get("description").toString());
                    Picasso.with(getApplicationContext()).load(poi.get("image").toString()).placeholder(R.drawable.guideme_po).into(imageIV);
                }

            } catch (Exception e) {
                Log.i("guideMe", "An exception has occured in the connection - " + e.toString());
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_poidetails, menu);
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
