package pt.up.fe.labtablet.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import pt.up.fe.labtablet.R;
import pt.up.fe.labtablet.api.AsyncTaskHandler;
import pt.up.fe.labtablet.api.AsyncWeatherFetcher;
import pt.up.fe.labtablet.api.LTLocationListener;
import pt.up.fe.labtablet.models.Descriptor;
import pt.up.fe.labtablet.utils.FileMgr;
import pt.up.fe.labtablet.utils.Utils;

public class FieldModeActivity extends Activity implements SensorEventListener {

    private ActionBar mActionBar;
    private Button bt_photo;
    private Button bt_sketch;
    private Button bt_audio;
    private Button bt_location;
    private Button bt_note;

    private Button bt_temperature_sample;
    private Button bt_network_temperature_sample;
    private Button bt_luminosity_sample;
    private Button bt_magnetic_sample;
    private ImageButton ib_refresh_samples;

    private TextView tv_title;
    private Switch sw_gps;

    private SensorManager sensorManager;
    private LTLocationListener locationListener;
    private MediaRecorder recorder;
    private BroadcastReceiver mBatInfoReceiver;
    private SensorsOnClickListener sensorClickListener;

    private String favorite_name;
    private String temperature_value;
    private String luminosity_value;
    private String magnetic_value;
    private String real_magnetic_value;
    private String audio_filename;
    private String photo_filename;

    private boolean recording;
    private Uri capturedImageUri;
    private String path;
    private boolean isCollecting;
    private long lastUpdateMillis;

    private ArrayList<Descriptor> metadata;
    private ProgressBar pb_update;
    private ProgressBar pb_location;

    Intent locatorService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_mode);
        capturedImageUri= null;
        isCollecting = false;
        Intent intent = getIntent();
        favorite_name = intent.getStringExtra("favorite_name");
        sensorClickListener = new SensorsOnClickListener();
        metadata = new ArrayList<Descriptor>();

        path = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + getResources().getString(R.string.app_name)
                + "/" + favorite_name + "/"
                + "meta";

        //MKDIR meta
        FileMgr.makeMetaDir(getApplication(), path);

        atatchButtons();

        tv_title = (TextView) findViewById(R.id.tv_title);
        tv_title.setText(favorite_name);

        mActionBar = this.getActionBar();
        mActionBar.setTitle(favorite_name);
        mActionBar.setSubtitle(getResources().getString(R.string.title_activity_field_mode));
        mActionBar.setDisplayHomeAsUpEnabled(false);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if(mBatInfoReceiver!= null) {
            registerBatInforReceiver();
        }

        LTLocationListener.kmlCreatedInterface interfaceKml = new LTLocationListener.kmlCreatedInterface() {
            @Override
            public void kmlCreated(Descriptor kmlDescriptor) {
                metadata.add(kmlDescriptor);
            }
        };
        locationListener = new LTLocationListener(FieldModeActivity.this, path, favorite_name, interfaceKml);

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(locationListener == null) {
                    return;
                }

                if(!isCollecting) {
                    isCollecting = true;
                    if(sw_gps.isChecked()) {locationListener.notifyCollectStarted();}
                } else {
                    isCollecting = false;
                    if(!sw_gps.isChecked()) { locationListener.notifyCollectStopped(); }
                }
            }
        });

        bt_sketch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FieldModeActivity.this, FingerPaintActivity.class);
                intent.putExtra("folderName", favorite_name);
                startActivityForResult(intent, Utils.SKETCH_INTENT_REQUEST);
            }
        });

        bt_note.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(FieldModeActivity.this);
                builder.setTitle(getResources().getString(R.string.new_text_record));

                // Set up the input
                final EditText input = new EditText(FieldModeActivity.this);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton(getResources().getString(R.string.form_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(input.getText().toString() == "") {
                            return;
                        }
                        Descriptor desc = new Descriptor();
                        desc.setValue(input.getText().toString());
                        desc.setTag(Utils.TEXT_TAGS);
                        metadata.add(desc);
                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        bt_audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(recording) {
                    pb_update.setIndeterminate(false);
                    bt_audio.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_voice, 0, 0, 0);
                    recording = false;
                    bt_audio.setText(getResources().getString(R.string.record));
                    recorder.stop();
                    recorder.release();
                    recorder = null;
                    Descriptor mDesc = new Descriptor();
                    mDesc.setTag(Utils.AUDIO_TAGS);
                    mDesc.setValue(Uri.parse(audio_filename).getLastPathSegment());
                    mDesc.setFilePath(audio_filename);
                    metadata.add(mDesc);

                } else {
                    recording = true;
                    pb_update.setIndeterminate(true);
                    pb_update.setVisibility(View.VISIBLE);
                    bt_audio.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_voice_busy, 0, 0, 0);
                    bt_audio.setText(getResources().getString(R.string.stop));

                    try {
                        recorder = new MediaRecorder();
                        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        audio_filename = path + "/recording_" + new Date().getTime() + ".mp3";
                        recorder.setOutputFile(audio_filename);
                        recorder.prepare();
                        recorder.start();
                    } catch (IOException e) {
                        Log.e("audio", e.getMessage());
                    }

                }
            }
        });

        bt_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!startService()) {
                    Log.e("LOCATIONListener", "error staring service");
                }
            }
        });

        bt_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                photo_filename = cal.getTimeInMillis()+".jpg";
                File file = new File(path,  photo_filename);
                if(!file.exists()){
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    file.delete();
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                capturedImageUri = Uri.fromFile(file);
                Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
                startActivityForResult(i, Utils.CAMERA_INTENT_REQUEST);
            }
        });


        ib_refresh_samples.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if((System.currentTimeMillis() - lastUpdateMillis) < Utils.SAMPLE_MILLIS) {
                    return;
                }
                bt_network_temperature_sample.setText(getResources().getString(R.string.loading));
                lastUpdateMillis = System.currentTimeMillis();
                new AsyncWeatherFetcher(new AsyncTaskHandler<Integer>() {
                    @Override
                    public void onSuccess(Integer result) {
                        bt_network_temperature_sample.setEnabled(true);
                        bt_network_temperature_sample.setText(result + getResources().getString(R.string.network_temp));
                        lastUpdateMillis = System.currentTimeMillis();
                    }

                    @Override
                    public void onFailure(Exception error) {
                        bt_network_temperature_sample.setEnabled(false);
                        bt_network_temperature_sample.setText(getResources().getString(R.string.not_available));
                    }

                    @Override
                    public void onProgressUpdate(int value) {

                    }
                }).execute(getApplication());

                bt_temperature_sample.setText(temperature_value + getResources().getString(R.string.battery_temp));
                bt_temperature_sample.setEnabled(true);

                bt_luminosity_sample.setText(luminosity_value + " (Lx)");
                bt_luminosity_sample.setEnabled(true);

                bt_magnetic_sample.setText(magnetic_value + " (uT)");
                bt_magnetic_sample.setEnabled(true);
            }
        });
    }

    private void registerBatInforReceiver() {
        mBatInfoReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent intent) {
                int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
                temperature_value = "" + temperature;
            }
        };
        registerReceiver(mBatInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void atatchButtons() {
        bt_audio = (Button) findViewById(R.id.bt_audio);
        bt_sketch = (Button) findViewById(R.id.bt_sketch);
        bt_photo = (Button) findViewById(R.id.bt_camera);
        bt_location = (Button) findViewById(R.id.bt_one_time_position);
        bt_note = (Button) findViewById(R.id.bt_text);

        bt_temperature_sample = (Button) findViewById(R.id.bt_temperature_sample);
        bt_luminosity_sample = (Button) findViewById(R.id.bt_luminosity);
        bt_magnetic_sample = (Button) findViewById(R.id.bt_magnetic);
        bt_network_temperature_sample = (Button) findViewById(R.id.bt_network_temperature_sample);
        ib_refresh_samples = (ImageButton) findViewById(R.id.ib_refresh_samples);

        bt_network_temperature_sample.setOnClickListener(sensorClickListener);
        bt_temperature_sample.setOnClickListener(sensorClickListener);
        bt_magnetic_sample.setOnClickListener(sensorClickListener);
        bt_luminosity_sample.setOnClickListener(sensorClickListener);

        sw_gps = (Switch) findViewById(R.id.sw_gps);
        pb_update = (ProgressBar) findViewById(R.id.pb_recording);
        pb_location = (ProgressBar) findViewById(R.id.pb_location);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.field_mode, menu);
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        switch (type) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                magnetic_value =  Math.round(event.values[0]) + ", " + Math.round(event.values[1]) + ", " + Math.round(event.values[2]);
                real_magnetic_value = event.values[0] + ", " + event.values[1] + ", " + event.values[2];
                break;
            case Sensor.TYPE_LIGHT:
                luminosity_value = "" + event.values[0];
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
                SensorManager.SENSOR_DELAY_NORMAL);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);

        registerBatInforReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mBatInfoReceiver != null) {
            unregisterReceiver(mBatInfoReceiver);
            mBatInfoReceiver = null;
        }
        sensorManager.unregisterListener(this);
    }


    public class SensorsOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Descriptor desc;
            switch (view.getId()) {
                case R.id.bt_network_temperature_sample:
                    desc = new Descriptor();
                    desc.setValue(bt_network_temperature_sample.getText().toString());
                    desc.setTag(Utils.TEMP_TAGS);
                    metadata.add(desc);
                    Toast.makeText(getApplication(), getResources().getString(R.string.net_temp_saved), Toast.LENGTH_SHORT).show();
                    break;

                case R.id.bt_temperature_sample:
                    desc = new Descriptor();
                    desc.setValue(bt_temperature_sample.getText().toString());
                    desc.setTag(Utils.TEMP_TAGS);
                    metadata.add(desc);
                    Toast.makeText(getApplication(), getResources().getString(R.string.temp_saved), Toast.LENGTH_SHORT).show();
                    break;

                case R.id.bt_magnetic:
                    desc = new Descriptor();
                    desc.setValue(real_magnetic_value);
                    desc.setTag(Utils.MAGNETIC_TAGS);
                    metadata.add(desc);
                    Toast.makeText(getApplication(), getResources().getString(R.string.mag_saved), Toast.LENGTH_SHORT).show();
                    break;

                case R.id.bt_luminosity:
                    desc = new Descriptor();
                    desc.setValue(bt_luminosity_sample.getText().toString());
                    desc.setTag(Utils.TEXT_TAGS);
                    metadata.add(desc);
                    Toast.makeText(getApplication(), getResources().getString(R.string.lum_saved), Toast.LENGTH_SHORT).show();
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //get the picture filename and update records
        if (requestCode == Utils.SKETCH_INTENT_REQUEST) {
            if(resultCode == RESULT_OK){
                String filePath = data.getStringExtra("result");
                Descriptor desc = new Descriptor();
                desc.setValue(Uri.parse(filePath).getLastPathSegment());
                desc.setFilePath(filePath);
                desc.setTag(Utils.PICTURE_TAGS);
                metadata.add(desc);
            }
            //other result different from OK shall not be added to the metadata
        } else if (requestCode == Utils.CAMERA_INTENT_REQUEST) {
            Descriptor desc = new Descriptor();
            desc.setFilePath(capturedImageUri.getPath());
            desc.setValue(capturedImageUri.getLastPathSegment());
            desc.setTag(Utils.PICTURE_TAGS);
            metadata.add(desc);
        } else if (requestCode == Utils.METADATA_VALIDATION) {
            //go back to field mode
            if(data == null) {
                return;
            }
            //save metadata
            if(!data.getExtras().containsKey("descriptors")) {
                Toast.makeText(this, "No descriptors received", Toast.LENGTH_SHORT).show();
            } else {
                String descriptorsJson = data.getStringExtra("descriptors");
                ArrayList<Descriptor> itemDescriptors = new Gson().fromJson(descriptorsJson, Utils.ARRAY_DESCRIPTORS);
                FileMgr.addDescriptors(favorite_name, itemDescriptors, this);
                Toast.makeText(this, getResources().getString(R.string.metadata_added_success), Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() != R.id.action_field_mode_end) {
            return super.onOptionsItemSelected(item);
        }

        if(isCollecting) {
            Toast.makeText(this, getResources().getString(R.string.cant_end_is_collecting), Toast.LENGTH_LONG).show();
            return super.onOptionsItemSelected(item);
        } if (recording) {
            Toast.makeText(this, getResources().getString(R.string.cant_end_is_recording), Toast.LENGTH_LONG).show();
            return super.onOptionsItemSelected(item);
        }

        if (metadata.size() == 0) {
            finish();
        } else {
            SharedPreferences settings = getSharedPreferences(favorite_name, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            String baseDescriptorsJson = settings.getString(favorite_name, "");

            if (baseDescriptorsJson.equals("")) {
                editor.putString(favorite_name, new Gson().toJson(metadata, Utils.ARRAY_DESCRIPTORS));
                editor.commit();
            } else {
                ArrayList<Descriptor> baseDescriptors =
                        new Gson().fromJson(baseDescriptorsJson, Utils.ARRAY_DESCRIPTORS);
                baseDescriptors.addAll(metadata);
                editor.remove(favorite_name);
                editor.putString(favorite_name, new Gson().toJson(baseDescriptors, Utils.ARRAY_DESCRIPTORS));
                editor.commit();
            }

            //Proceed to validate collected metadata
            Intent intent = new Intent(FieldModeActivity.this, ValidateMetadataActivity.class);
            intent.putExtra("descriptors", new Gson().toJson(metadata, Utils.ARRAY_DESCRIPTORS));
            intent.putExtra("favorite_name", favorite_name);
            startActivityForResult(intent, Utils.METADATA_VALIDATION);
        }

        return super.onOptionsItemSelected(item);

    }

    public boolean stopService() {
        if (this.locatorService != null) {
            this.locatorService = null;
        }
        return true;
    }

    public boolean startService() {
        try {
            new FetchCordinates().execute();
            return true;
        } catch (Exception error) {
            return false;
        }
    }


    public class FetchCordinates extends AsyncTask<String, Integer, String> {
        public double lati = 0.0;
        public double longi = 0.0;


        public LocationManager mLocationManager;
        public mLocationListener mLocationListener;

        @Override
        protected void onPreExecute() {
            mLocationListener = new mLocationListener();
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0, 0,
                    mLocationListener);

            pb_location.setIndeterminate(true);
            pb_location.setVisibility(View.VISIBLE);

        }

        @Override
        protected void onCancelled(){
            System.out.println("Cancelled by user!");
            pb_location.setIndeterminate(false);
            mLocationManager.removeUpdates(mLocationListener);
        }

        @Override
        protected void onPostExecute(String result) {
            pb_location.setIndeterminate(false);

            Descriptor desc = new Descriptor();
            desc.setValue(lati + "," + longi);
            desc.setTag(Utils.GEO_TAGS);
            metadata.add(desc);

            Toast.makeText(FieldModeActivity.this,
                    "LAT:" + lati + " LNG:" + longi,
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... params) {
            while (this.lati == 0.0) {

            }
            return null;
        }

        public class mLocationListener implements LocationListener {

            @Override
            public void onLocationChanged(Location location) {

                try {
                    lati = location.getLatitude();
                    longi = location.getLongitude();
                } catch (Exception e) {
                    pb_update.setIndeterminate(false);
                    Toast.makeText(getApplicationContext(),"Unable to get Location"
                            , Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.i("OnProviderDisabled", "OnProviderDisabled");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.i("onProviderEnabled", "onProviderEnabled");
            }

            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
                Log.i("onStatusChanged", "onStatusChanged");

            }

        }

    }


}