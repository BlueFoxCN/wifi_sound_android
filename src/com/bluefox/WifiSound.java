package com.bluefox;

import com.sinpo.xnfc.R;
import com.whitebyte.wifihotspotutils.ClientScanResult;
import com.whitebyte.wifihotspotutils.FinishScanListener;
import com.whitebyte.wifihotspotutils.WifiApManager;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class WifiSound extends Activity implements OnClickListener {

	public static final int fileRecvPort = 9003;

    private int mInterval = 5000;
    private Handler m1;

    EditText text = null;
    Button toggleWifiButton = null;
	Button exitButton = null;
    Button setRecordTimeButton = null;
    Button fileButton = null;

    WifiApManager wifiApManager;
    TextView wifiStatus;
    boolean curWifiStatus;
    boolean fileRecvStatus;

    String ipAddr = "";

	public byte[] dataRecord;

    public native int open(int compression);
    public native int getFrameSize();
    public native int decode(byte encoded[], short lin[], int size);
    public native int encode(short lin[], int offset, byte encoded[], int size);
    public native void close();

    public void onClick(View v) {
		if (v == exitButton) {
			System.exit(0);
		} else if (v == toggleWifiButton) {
            boolean realWifiStatus = wifiApManager.isWifiApEnabled();
            if (curWifiStatus && realWifiStatus) {
                // close the ap
                wifiApManager.setWifiApEnabled(null, false);
                curWifiStatus = false;
                toggleWifiButton.setText("打开热点");
            } else if (!curWifiStatus && !realWifiStatus) {
                // open the ap
                wifiApManager.setWifiApEnabled(null, true);
                curWifiStatus = true;
                toggleWifiButton.setText("关闭热点");
            } else if (curWifiStatus) {
                // already closed
                curWifiStatus = false;
                toggleWifiButton.setText("打开热点");
            } else {
                // already opened
                curWifiStatus = true;
                toggleWifiButton.setText("关闭热点");
            }
		} else if (v == setRecordTimeButton) {
            Intent intent = new Intent(this, RecordTime.class);
            intent.putExtra("ip", ipAddr);
            startActivity(intent);
        } else if (v == fileButton) {
            if (fileRecvStatus) {
                // stop receiving file
            } else {
                // start receiving file
            }
            fileRecvStatus = !fileRecvStatus;
        }
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);

        fileRecvStatus = false;

        wifiStatus = (TextView) findViewById(R.id.wifiStatus);
        toggleWifiButton = (Button) findViewById(R.id.toggleWifiButton);
        exitButton = (Button) findViewById(R.id.exitButton);
        setRecordTimeButton = (Button) findViewById(R.id.recordTimeButton);
        fileButton = (Button) findViewById(R.id.fileButton);
        wifiApManager = new WifiApManager(this);

        toggleWifiButton = (Button)findViewById(R.id.toggleWifiButton);
        curWifiStatus = wifiApManager.isWifiApEnabled();
        if (curWifiStatus) {
            toggleWifiButton.setText("关闭热点");
        } else {
            toggleWifiButton.setText("打开热点");
        }
        exitButton.setText("退出");
        setRecordTimeButton.setText("设置录音时间");
        fileButton.setText("接收文件");

		exitButton.setOnClickListener(this);
        toggleWifiButton.setOnClickListener(this);
        setRecordTimeButton.setOnClickListener(this);
        fileButton.setOnClickListener(this);

        System.loadLibrary("speex");

        open(8);

        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString();
        File myDir = new File(root + "/my_sounds");
        myDir.mkdirs();
        File file = new File (myDir, "f_1408185089");
        File newFile = new File(myDir, "wav.wav");
        FileInputStream fIn;
        FileOutputStream fOut;
        byte[] data = new byte[1000];
        short[] wavData = new short[10000];
        int readNum = 0;
        int count = 0;
        try {
            fIn = new FileInputStream(file);
            fOut = new FileOutputStream(newFile);

            int len = (int)file.length();

            WaveHeader wavHeader = new WaveHeader((short)1, (short)1, 48000, (short)16, len / 38 * 320);
            wavHeader.write(fOut);
            int totNum = 0;
            readNum = fIn.read(data, 0, 38);
            byte[] tempByteAry = new byte[320];
            while (readNum > 0) {
                totNum += readNum;
                count = decode(data, wavData, readNum);

                for (int i = 0; i < count; i++) {
                    tempByteAry[2 * i] = (byte)(wavData[i] & 0xFF);
                    tempByteAry[2 * i + 1] = (byte)(wavData[i] >> 8);
                }

                fOut.write(tempByteAry);
                readNum = fIn.read(data, 0, 38);
            }
            fIn.close();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


        m1 = new Handler();
        mScan.run();
        mRecvFile.run();

	}

    Runnable mScan = new Runnable() {
        @Override
        public void run() {
            scan();
            m1.postDelayed(mScan, mInterval);
        }
    };

    private void scan() {
        wifiApManager.getClientList(false, new FinishScanListener() {

            @Override
            public void onFinishScan(final ArrayList<ClientScanResult> clients) {

                wifiStatus.setText("WifiApState: " + wifiApManager.getWifiApState() + "\n\n");
                wifiStatus.append("Clients: \n");
                for (ClientScanResult clientScanResult : clients) {
                    wifiStatus.append("####################\n");
                    wifiStatus.append("IpAddr: " + clientScanResult.getIpAddr() + "\n");
                    wifiStatus.append("Device: " + clientScanResult.getDevice() + "\n");
                    wifiStatus.append("HWAddr: " + clientScanResult.getHWAddr() + "\n");
                    wifiStatus.append("isReachable: " + clientScanResult.isReachable() + "\n");
                    ipAddr = clientScanResult.getIpAddr();
                }
            }
        });
    }

    Runnable mRecvFile = new Runnable() {
        @Override
        public void run() {
            new RecvFileTask().execute();
        }
    };

    class RecvFileTask extends AsyncTask<Void, Void, String> {
        protected String doInBackground(Void... voids) {
            ServerSocket ss = null;
            try {
                if (available(fileRecvPort) == false) {
                    return "";
                }
                ss = new ServerSocket(fileRecvPort);
                String fileName;
                File file;
                FileOutputStream fOut = null;
                int fileSize = 0;

                Socket s = ss.accept();
                InputStream inStream = s.getInputStream();
                while (true) {
                    byte[] data = new byte[1000];
                    int count = inStream.read(data);
                    String str = new String(data, 0, count);

                    if (str.startsWith("start")) {
                        // begin to send a new file
                        fileName = str.split(":")[1];
                        fileSize = Integer.parseInt(str.split(":")[2]);
                        file = getWaveStorageFile(fileName);

                        fOut = new FileOutputStream(file);
                        // send back a confirmation
                        OutputStream outStream = s.getOutputStream();
                        outStream.write("confirm".getBytes());
                    } else if (fOut != null) {
                        // data of the file
                        fileSize = fileSize - count;
                        fOut.write(data, 0, count);
                        if (fileSize <= 0) {
                            fOut.close();
                            break;
                        }
                    }
                }
                s.close();
            } catch (InterruptedIOException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (ss != null) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return "";
        }

        protected void onPostExecute(String value) {
            SystemClock.sleep(1000);
            mRecvFile.run();
        }
    }

    @Override
	public void onResume() {
		super.onResume();
	}

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public File getWaveStorageFile(String wavName) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString();
        File myDir = new File(root + "/my_sounds");
        myDir.mkdirs();
        File file = new File (myDir, wavName);
        return file;
    }

    public static boolean available(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                /* should not be thrown */
                }
            }
        }

        return false;
    }
}