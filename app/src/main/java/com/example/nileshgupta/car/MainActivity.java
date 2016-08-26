package com.example.nileshgupta.car;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;


public class MainActivity extends Activity implements SwipeInterface {
    private TextView txtSpeechInput;
    private ImageButton btnSpeak;
	public static final String DATA_PATH = Environment
			.getExternalStorageDirectory().toString() + "/SimpleAndroidOCR/";
	
    private final int REQ_CODE_SPEECH_INPUT = 98;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutStream;
    BroadcastReceiver mReceiver;
    String msg;
    static int counter = 0;
    String control = "0";
    volatile boolean stopWorker;
    String Device = "HC-05";
    public static final String lang = "eng";
    protected ImageButton _button;
    protected String _path;
    protected boolean _taken;
    protected static final String PHOTO_TAKEN = "photo_taken";
    private static final String TAG = "MainActivity.java";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

		String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

		for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
					System.out.println("ERROR: Creation of directory " + path + " on sdcard failed");
					return;
				} else {
					System.out.println("Created directory " + path + " on sdcard");
				}
			}

		}
		
		// lang.traineddata file with the app (in assets folder)
		// You can get them at:
		// http://code.google.com/p/tesseract-ocr/downloads/list
		// This area needs work and optimization
		if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
			try {

				AssetManager assetManager = getAssets();
				InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
				//GZIPInputStream gin = new GZIPInputStream(in);
				OutputStream out = new FileOutputStream(DATA_PATH
						+ "tessdata/" + lang + ".traineddata");

				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				//while ((lenf = gin.read(buff)) > 0) {
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
                }
                in.close();
				//gin.close();
				out.close();
				
				System.out.println( "Copied " + lang + " traineddata");
			} catch (IOException e) {
				System.out.println("Was unable to copy " + lang + " traineddata " + e.toString());
			}
		}
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);
       TextView click = (TextView) findViewById(R.id.abc);
        _button = (ImageButton) findViewById(R.id.camera);
        _button.setOnClickListener(new ButtonClickHandler());

        _path = DATA_PATH + "/ocr.jpg";
        click.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                msg = "STOP";
                txtSpeechInput.setText(msg);
                if (counter == 1)
                    try {

                        sendData();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                return false;
            }
        });
        RelativeLayout swipe_layout = (RelativeLayout) findViewById(R.id.textView);
        com.example.nileshgupta.car.ActivitySwipeDetector swipe = new com.example.nileshgupta.car.ActivitySwipeDetector(this);
        swipe_layout.setOnTouchListener(swipe);

        try {
            findBT();
        } catch (IOException e) {
            e.printStackTrace();
        }
        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
        // hide the action bar
        //getActionBar().hide();
        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });
    }

    public class ButtonClickHandler implements View.OnClickListener {
        public void onClick(View view) {
            Log.v(TAG, "Starting Camera app");
            startCameraActivity();
        }
    }
    protected void startCameraActivity() {
        File file = new File(_path);
        Uri outputFileUri = Uri.fromFile(file);

        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

        startActivityForResult(intent, 0);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(MainActivity.PHOTO_TAKEN, _taken);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.i(TAG, "onRestoreInstanceState()");
        if (savedInstanceState.getBoolean(MainActivity.PHOTO_TAKEN)) {
            onPhotoTaken();
        }
    }

    protected void onPhotoTaken() {
        _taken = true;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;

        Bitmap bitmap = BitmapFactory.decodeFile(_path, options);

        try {
            ExifInterface exif = new ExifInterface(_path);
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            Log.v(TAG, "Orient: " + exifOrientation);

            int rotate = 0;

            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            Log.v(TAG, "Rotation: " + rotate);

            if (rotate != 0) {

                // Getting width & height of the given image.
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
            }

            // Convert to ARGB_8888, required by tess
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        } catch (IOException e) {
            Log.e(TAG, "Couldn't correct orientation: " + e.toString());
        }

        // _image.setImageBitmap( bitmap );

        Log.v(TAG, "Before baseApi");

        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setDebug(true);
        baseApi.init(DATA_PATH, lang);
        baseApi.setImage(bitmap);

        String recognizedText = baseApi.getUTF8Text();

        baseApi.end();

        // You now have the text in recognizedText var, you can do anything with it.
        // We will display a stripped out trimmed alpha-numeric version of it (if lang is eng)
        // so that garbage doesn't make it to the display.

        Log.v(TAG, "OCRED TEXT: " + recognizedText);

        if ( lang.equalsIgnoreCase("eng") ) {
            recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9]+", " ");
        }

        recognizedText = recognizedText.trim();

        if ( recognizedText.length() != 0 ) {
            //_field.setText(_field.getText().toString().length() == 0 ? recognizedText : _field.getText() + " " + recognizedText);
            //_field.setSelection(_field.getText().toString().length());
            txtSpeechInput.setText(recognizedText);
            msg= recognizedText;
            if(counter==1){
                try {
                    sendData();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        // Cycle done.
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopWorker = true;
        try {
            if (mmOutStream != null) {
                mmOutStream.close();
                mmSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG).show();
        if(mReceiver!=null)
            unregisterReceiver(mReceiver);


    }

    void findBT() throws IOException {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "No bluetooth adapter available", Toast.LENGTH_LONG).show();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
            SystemClock.sleep(7000);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            counter = 0;
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(Device)) {
                    counter = 1;
                    mmDevice = device;
                    Toast.makeText(getApplicationContext(), "Bluetooth device already paired", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(), "Checking if in range", Toast.LENGTH_SHORT).show();
                    ConnectThread mConnectThread = new ConnectThread(device);
                    mConnectThread.start();
                    break;
                }
            }

        }

        if (counter == 0 && counter!=1) {
            Toast.makeText(getApplicationContext(), "Bluetooth device not found", Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), "Scanning for other devices", Toast.LENGTH_LONG).show();
            ScanDevice mscaneddevices = new ScanDevice();
            mscaneddevices.start();
        }



    }

/*    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard //SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutStream = mmSocket.getOutputStream();
    }*/

    void sendData() throws IOException {
        if (msg.equalsIgnoreCase("STOP")) {
            control = "6";
        } else if (msg .equalsIgnoreCase( "START")) {
            control = "1";
        } else if (msg.equalsIgnoreCase("BACK")) {
            control = "2";
        } else if (msg.equalsIgnoreCase("RIGHT")) {
            control = "3";
        } else if (msg.equalsIgnoreCase("LEFT")) {
            control = "4";
        }else if (msg.equalsIgnoreCase("SHOOT")||msg.equalsIgnoreCase("5")) {
        control = "5";
    }

            ConnectedThread  mConnectedThread = new ConnectedThread();
            mConnectedThread.start();
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && null != data) {

                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                msg = result.get(0).toUpperCase();
                txtSpeechInput.setText(msg);

                if (counter == 1) {
                    try {
                        sendData();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                txtSpeechInput.setText(msg);
            }

        }
        if (resultCode == -1) {
            onPhotoTaken();
        } else {
            Log.v(TAG, "User cancelled");
        }


    }

    private class ConnectThread extends Thread {
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            try {
                mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.e("ardino", "                                                                         1");
            } catch (IOException e) {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        Toast.makeText(getApplicationContext(), "Device is not in range", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }

        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        Toast.makeText(getApplicationContext(), "Device connected", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("ardino","                                                                                                        2");
            } catch (IOException connectException) {

                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Device is not in range2", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            try {
                mmOutStream=mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }
    @Override
    public void bottom2top(View v) throws IOException {
        switch(v.getId()){
            case R.id.textView:
                msg = "START";
                txtSpeechInput.setText(msg);
                if(counter==1)
                sendData();
                break;
        }
    }

    @Override
    public void left2right(View v) throws IOException {
        switch(v.getId()){
            case R.id.textView:
                msg = "RIGHT";
                txtSpeechInput.setText(msg);
               if(counter==1)
                sendData();
                break;
        }

    }

    @Override
    public void right2left(View v) throws IOException {

        switch(v.getId()){
            case R.id.textView:
                msg = "LEFT";
                txtSpeechInput.setText(msg);
                if(counter==1)
                sendData();
                break;
        }


    }

    @Override
    public void top2bottom(View v) throws IOException {
        switch(v.getId()){
            case R.id.textView:
                msg = "BACK";
                txtSpeechInput.setText(msg);
                if(counter==1)
                sendData();
                break;
        }


    }


    private class ScanDevice extends Thread {

        public void run() {
            mBluetoothAdapter.startDiscovery();
            mReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    //Finding devices
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        // Get the BluetoothDevice object from the Intent
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device.getName().equals(Device)) {
                            mmDevice = device;
                            Toast.makeText(getApplicationContext(), "Bluetooth device trying to connect", Toast.LENGTH_SHORT).show();
                            pairDevice(device);
                            counter=1;
                        }

                    }
                }
            };

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);

        }

        private void pairDevice(BluetoothDevice device) {
            try {
                Method method = device.getClass().getMethod("createBond", (Class[]) null);
                method.invoke(device, (Object[]) null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            registerReceiver(mPairReceiver, intent);
        }

        private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                    if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                        try {
                            findBT();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                }
            }
        };
    }

    private class ConnectedThread extends Thread {
        public void run() {
            final int[] c = {0};
            try {
                mmOutStream.write(control.getBytes());
            } catch (IOException | NullPointerException e) {
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        c[0] =1;
                        Toast.makeText(getApplicationContext(), "Sending data failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            if(c[0] ==0){
                Toast.makeText(getApplicationContext(), "Command Accepted", Toast.LENGTH_LONG).show();
            }


        }

    }
}
