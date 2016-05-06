package pt.iscte_iul.guideme;

import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;


public class SettingsActivity extends ActionBarActivity {
    protected SeekBar rangeSB;
    protected TextView rangeTV;
    protected int distanceRange;
    protected String defaultURL;
    protected EditText urlET;
    protected Button saveSetBtn;

    public static final String PREFS_NAME = "AppData";
    public static final int RANGE = 50;
    public static String APIURL = "http://192.168.1.104:3000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        rangeSB = (SeekBar) findViewById(R.id.seekBarRange);
        rangeTV = (TextView) findViewById(R.id.textViewRange);
        urlET = (EditText) findViewById(R.id.editTextURL);
        saveSetBtn = (Button) findViewById(R.id.buttonSettingsSave);

        SharedPreferences appData = getSharedPreferences(PREFS_NAME, 0);
        //try to read from SharedPreferences
        distanceRange = appData.getInt("range", RANGE);
        defaultURL = appData.getString("URL", APIURL);
        Log.i("guideMe", "Range is = " + distanceRange);

        rangeSB.setProgress(distanceRange);
        rangeTV.setText(distanceRange + " Kms");
        urlET.setText(defaultURL);

        rangeSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                rangeTV.setText(i + " Kms");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                distanceRange = seekBar.getProgress();
                Log.i("guideMe", "Distance range = " + distanceRange);
            }
        });

        saveSetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences appData = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = appData.edit();
                editor.putInt("range", distanceRange);
                editor.putString("URL", urlET.getText().toString());
                editor.commit();
                finish();
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
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
