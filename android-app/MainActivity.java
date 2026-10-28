package com.example.rovercontrol;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "RoverControl";
    private static final String DEVICE_NAME = "HC-05";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQ_PERMS = 101;

    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private OutputStream btOut;
    private volatile boolean connected = false;

    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean previewing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        surfaceView = new SurfaceView(this);
        setContentView(surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "No Bluetooth", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        requestNeededPermissions();
    }

    private void requestNeededPermissions() {
        String[] perms;
        if (Build.VERSION.SDK_INT >= 31) {
            perms = new String[]{Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
        } else {
            perms = new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION};
        }
        boolean all = true;
        for (String p : perms) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                all = false;
                break;
            }
        }
        if (!all) {
            requestPermissions(perms, REQ_PERMS);
        } else {
            connectToHc05();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMS) {
            boolean ok = grantResults.length > 0;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    ok = false;
                }
            }
            if (ok) {
                connectToHc05();
                if (surfaceHolder.getSurface().isValid()) {
                    startCamera();
                }
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void connectToHc05() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!btAdapter.isEnabled()) {
                        showToast("Turn on Bluetooth");
                        return;
                    }
                    Set<BluetoothDevice> bonded = btAdapter.getBondedDevices();
                    BluetoothDevice target = null;
                    for (BluetoothDevice d : bonded) {
                        if (DEVICE_NAME.equals(d.getName())) {
                            target = d;
                            break;
                        }
                    }
                    if (target == null) {
                        showToast("HC-05 not paired");
                        return;
                    }
                    btAdapter.cancelDiscovery();
                    btSocket = target.createRfcommSocketToServiceRecord(SPP_UUID);
                    btSocket.connect();
                    btOut = btSocket.getOutputStream();
                    connected = true;
                    showToast("Connected");
                } catch (IOException e) {
                    Log.e(TAG, "connect failed", e);
                    connected = false;
                    closeSocket();
                    showToast("Connection failed");
                } catch (SecurityException se) {
                    Log.e(TAG, "no permission", se);
                    showToast("Bluetooth permission missing");
                }
            }
        }).start();
    }

    public void sendCommand(final String cmd) {
        if (!connected || btOut == null) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    btOut.write(cmd.getBytes());
                    btOut.flush();
                } catch (IOException e) {
                    Log.e(TAG, "send failed", e);
                    connected = false;
                    closeSocket();
                    showToast("Connection lost");
                }
            }
        }).start();
    }

    private void closeSocket() {
        try {
            if (btOut != null) {
                btOut.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (btSocket != null) {
                btSocket.close();
            }
        } catch (IOException ignored) {
        }
        btOut = null;
        btSocket = null;
    }

    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (previewing) {
            return;
        }
        try {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            previewing = true;
        } catch (Exception e) {
            Log.e(TAG, "camera failed", e);
            stopCamera();
        }
    }

    private void stopCamera() {
        if (camera != null) {
            try {
                camera.stopPreview();
            } catch (Exception ignored) {
            }
            camera.release();
            camera = null;
        }
        previewing = false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera == null) {
            return;
        }
        try {
            camera.stopPreview();
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            previewing = true;
        } catch (Exception e) {
            Log.e(TAG, "preview restart failed", e);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (surfaceHolder != null && surfaceHolder.getSurface().isValid()) {
            startCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connected = false;
        closeSocket();
        stopCamera();
    }
}