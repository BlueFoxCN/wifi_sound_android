package com.bluefox;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.sinpo.xnfc.R;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import android.widget.TimePicker;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


/**
 * Created by jesse on 8/10/14.
 */
public class RecordTime extends Activity implements OnClickListener {

    Button returnButton = null;
    Button setRecordTimeButton = null;
    public String ipAddr = "";
    TextView recordTime = null;
    TimePicker startTimePicker = null;
    TimePicker endTimePicker = null;
    private final int clientPort = 8001;

    public void onClick(View v) {
        if (v == returnButton) {
            Intent intent = new Intent(this, WifiSound.class);
            startActivity(intent);
        } else if (v == setRecordTimeButton) {
            new SetRecordTimeTask().execute();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.record_time);

        Intent intent = getIntent();
        ipAddr = intent.getStringExtra("ip");

        returnButton = (Button) findViewById(R.id.returnButton);
        returnButton.setText("∑µªÿ");
        returnButton.setOnClickListener(this);

        setRecordTimeButton = (Button) findViewById(R.id.setRecordTimeButton);
        setRecordTimeButton.setText("…Ë÷√");
        setRecordTimeButton.setOnClickListener(this);

        recordTime = (TextView) findViewById(R.id.recordTime);

        startTimePicker = (TimePicker) findViewById(R.id.startTimePicker);
        endTimePicker = (TimePicker) findViewById(R.id.endTimePicker);

        // send request to get the record time
        new GetRecordTimeTask().execute();
    }

    class GetRecordTimeTask extends AsyncTask<Void, Void, String> {
        protected String doInBackground(Void... voids) {
            try {
                Socket s = new Socket(ipAddr, clientPort);
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                //send output msg
                out.write("query");
                out.flush();
                Log.i("TcpClient", "sent: query");
                //accept server response
                String inMsg = in.readLine() + System.getProperty("line.separator");
                // String inMsg = in.readLine();
                Log.i("TcpClient", "received: " + inMsg);
                //close connection
                s.close();
                return inMsg;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

        protected void onPostExecute(String value) {
            recordTime.setText(value);
            int startTime = Integer.parseInt(value.split(":|;")[0]);
            int endTime = Integer.parseInt(value.split(":|;")[1]);
            int startHour = startTime / 3600;
            int startMinute = (startTime % 3600) / 60;
            int endHour = endTime / 3600;
            int endMinute = (endTime % 3600) / 60;
            startTimePicker.setCurrentHour(startHour);
            startTimePicker.setCurrentMinute(startMinute);
            endTimePicker.setCurrentHour(endHour);
            endTimePicker.setCurrentMinute(endMinute);
        }
    }

    class SetRecordTimeTask extends AsyncTask<Void, Void, String> {
        protected String doInBackground(Void... voids) {
            try {
                Socket s = new Socket(ipAddr, clientPort);
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                //send output msg
                Integer startTime = startTimePicker.getCurrentHour() * 3600 + startTimePicker.getCurrentMinute() * 60;
                Integer endTime = endTimePicker.getCurrentHour() * 3600 + endTimePicker.getCurrentMinute() * 60;
                String recordTimeStr = startTime.toString() + ":" + endTime.toString() + ";";
                // out.write(recordTime.getText().toString());
                out.write(recordTimeStr);
                out.flush();
                //accept server response
                // String inMsg = in.readLine();
                // Log.i("TcpClient", "received: " + inMsg);
                //close connection
                s.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

        protected void onPostExecute(String value) {
        }
    }
}