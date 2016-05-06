package pt.iscte_iul.guideme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;


public class AddPOIActivity extends ActionBarActivity {
    protected TextView latlongView;
    protected TextView POItypeTV;
    protected EditText poinameET;
    protected EditText poidescriptionET;
    protected ImageView poiImageIV;
    protected double lat, lgt;
    protected int selectedType;
    private Bitmap bitmap;

    protected String defaultURL;
    public static final String PREFS_NAME = "AppData";
    public static String APIURL = "http://192.168.1.104:3000";
    public static final String APPNAME = "guideMe";
    private static final int REQUEST_CODE = 1;

    protected String path;
    protected String filename;

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 200;
    private Uri fileUri;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_poi);

        latlongView = (TextView) findViewById(R.id.textViewLatLong);
        POItypeTV = (TextView) findViewById(R.id.textViewPOIType);
        poinameET = (EditText) findViewById(R.id.POInameET);
        poidescriptionET = (EditText) findViewById(R.id.descriptionET);
        poiImageIV = (ImageView) findViewById(R.id.imageView2);

        Ion.getDefault(this).configure().setLogging("ion-sample", Log.DEBUG);

        Intent i = getIntent();
        latlongView.setText("<" + i.getDoubleExtra("latitude",0) + "> , <" + i.getDoubleExtra("longitude", 0) + ">");
        lat = i.getDoubleExtra("latitude",0);
        lgt = i.getDoubleExtra("longitude", 0);

        SharedPreferences appData = getSharedPreferences(PREFS_NAME, 0);
        //try to read from SharedPreferences
        defaultURL = appData.getString("URL", APIURL);
        Log.i(APPNAME, "URL = " + defaultURL);
    }

    public void selectTypeButton(View v) {

        final CharSequence[] items = {"Monuments", "Universities", "Parks"};
        // fazer isto com um Hmap
        final HashMap <Integer, String> typeMap = new HashMap<>();
        for(int i=0; i<items.length; i++) {
            typeMap.put(i+1, items[i].toString());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick a type...");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                POItypeTV.setText(typeMap.get(i+1));
                selectedType = i+1;
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public void addNewPOI(View v) {
        // call the entry point to add a new POI
        new addNewPOI().execute();
    }

    public void clickShowGallery(View v) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void clickShowCamera(View v) {
        File file = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/MyFolder", "myImage"+ ".jpg");

        String mCapturedImagePath = file.getAbsolutePath();
        Toast.makeText(this, "file = " + mCapturedImagePath, Toast.LENGTH_LONG).show();
        Uri outputFileUri = Uri.fromFile(file);


        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

        // start the image capture Intent
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        InputStream stream = null;
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                // recyle unused bitmaps
                if (bitmap != null) {
                    bitmap.recycle();
                }

                // to get the real path from the uri of the image
                path = getRealPathFromURI(this, data.getData());

                // extract the filename (important to save on database
                File f = new File(path);
                filename = f.getName();
                Toast.makeText(this, "Filename = " + filename, Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "Path = " + path, Toast.LENGTH_SHORT).show();

                stream = getContentResolver().openInputStream(data.getData());
                bitmap = BitmapFactory.decodeStream(stream);

                poiImageIV.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            poiImageIV.setImageBitmap((Bitmap) extras.get("data"));

            try {

                Bitmap tempBmp = (Bitmap)extras.get("data");


                /*
                Bitmap tempBmp = (Bitmap)extras.get("data");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                tempBmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] b = baos.toByteArray();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                filename = "IMG_"+ timeStamp + ".jpg";
                path = filename;
                FileOutputStream fileOutStream = openFileOutput(filename, MODE_PRIVATE);
                FileDescriptor fd  = fileOutStream.getFD();
                Toast.makeText(this, "Filename = " + fd.toString(), Toast.LENGTH_SHORT).show();
                fileOutStream.write(b);
                fileOutStream.close();
                */
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (resultCode == RESULT_CANCELED) {
            // User cancelled the image capture
        } else {
            // Image capture failed, advise user
        }
    }

    // grab the name of the media from the Uri
    protected String getName(Uri uri)
    {
        String filename = null;

        try {
            String[] projection = { MediaStore.Images.Media.DISPLAY_NAME };
            Cursor cursor = managedQuery(uri, projection, null, null, null);

            if(cursor != null && cursor.moveToFirst()){
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                filename = cursor.getString(column_index);
            } else {
                filename = null;
            }
        } catch (Exception e) {
            Log.e("guideMe", "Error getting file name: " + e.getMessage());
        }

        return filename;
    }

    /* the next two functions are simply used to obtain the real location of the image */
    public String getRealPathFromURI(Context context, Uri uri) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;
        if ("image".equals(type)) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[] {
                split[1]
        };

        return getDataColumn(context, contentUri, selection, selectionArgs);
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private class addNewPOI extends AsyncTask<String, Void, String> {
        private ProgressDialog dialog = new ProgressDialog(AddPOIActivity.this);

        @Override
        protected void onPreExecute()
        {
            dialog.setMessage("Sending POI to server...");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params)
        {
            String response = null;

            try {
                HttpClient httpclient = new DefaultHttpClient();
                JSONObject JSONpoi = new JSONObject();
                JSONpoi.put("name", poinameET.getText());
                JSONpoi.put("address", ""); // still have to find a way to get the address
                JSONpoi.put("latt", lat);
                JSONpoi.put("logt", lgt);
                JSONpoi.put("description", poidescriptionET.getText());
                JSONpoi.put("image", defaultURL + "/uploads/" + filename);
                JSONpoi.put("type", selectedType);

                HttpPost request = new HttpPost(new URI(defaultURL + "/poi/"));
                StringEntity se = new StringEntity(JSONpoi.toString());
                request.setEntity(se);
                request.setHeader("Accept", "application/json");
                request.setHeader("Content-Type", "application/json");

                Log.i("guideMe", "Sending  = " + defaultURL + "/poi/");

                HttpResponse httpResponse = httpclient.execute(request);

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
                    Toast.makeText(getApplicationContext(), "POI added with success...", Toast.LENGTH_SHORT).show();

                    File f = new File(path);
                    Future uploading = Ion.with(AddPOIActivity.this)
                            .load(defaultURL + "/upload")
                            .setMultipartFile("image", f)
                            .asString()
                            .withResponse()
                            .setCallback(new FutureCallback<Response<String>>() {
                                @Override
                                public void onCompleted(Exception e, Response<String> result) {
                                    try {
                                        JSONObject jobj = new JSONObject(result.getResult());
                                        Toast.makeText(getApplicationContext(), jobj.getString("response"), Toast.LENGTH_SHORT).show();
                                        finish();

                                    } catch (JSONException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            });
                } else {
                    Toast.makeText(getApplicationContext(), "Problems adding POI!!!", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Log.i("guideMe", "An exception has occured in the connection - " + e.toString());
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_poi, menu);
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
