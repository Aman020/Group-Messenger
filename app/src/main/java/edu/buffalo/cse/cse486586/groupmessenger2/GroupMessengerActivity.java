package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    private final static int SERVER_PORT = 10000;
    private static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final String [] ports = new String[] {"11108","11112","11116","11120","11124"};
    private  static  final Uri CONTENT_URI = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
    Socket[] sockets = new Socket[5];
    int sequenceNumber =0;
    HashMap<String,Double> sequenceNumberMap = new HashMap<String, Double>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        

        try
        {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */


        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button sendButton = (Button) findViewById(R.id.button4);
                String message = sendButton.getText().toString();
                sendButton.setText("");
                TextView displayText = (TextView) findViewById(R.id.textView1);
                displayText.append("\n");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message);


            }
        });




    }
    private class ServerTask extends AsyncTask<ServerSocket, String,Void>{
        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {

            try{
                TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                String processID = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);

                ServerSocket serverSocket = serverSockets[0];
                while(true)
                {
                    Socket socket = serverSocket.accept();
                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    String messageFromClient = inputStream.readUTF();
                    Log.i("Client message", " Received message at server -" +messageFromClient);


                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    outputStream.writeUTF(String.valueOf(++sequenceNumber + (Double.valueOf(processID)/10000)) );
                    outputStream.flush();

                }

            }
            catch(Exception ex)
            {
                ex.printStackTrace();

            }



            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }
    }

    private class ClientTask extends AsyncTask<String, Void,Void>{
        List<Double> proposedSequenceNumber = new ArrayList();
        @Override
        protected Void doInBackground(String... strings) {
            int i =0;
            String messageToSend = strings[0];
            try {
                while (i <= ports.length) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(ports[i]));
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    outputStream.writeUTF(messageToSend);
                    outputStream.flush();

                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    String responseFromServer = inputStream.readUTF();
                    proposedSequenceNumber.add(Double.valueOf(responseFromServer));
                    Log.i("Response from serer", "Response received-" + responseFromServer);
                    outputStream.close();
                    inputStream.close();
                    socket.close();

                    i++;
                }

                double maxSequenceNumberReceived = getMaxValue(proposedSequenceNumber);

                Log.i("Max value-" ,String.valueOf(maxSequenceNumberReceived));

            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }



            return null;
        }
    }
    private double getMaxValue(List<Double> list)
    {
        double max = Float.MIN_VALUE;
        double current =0.0;
        for(int i =0; i < list.size();i++)
        {
            if ((current = list.get(i)) > max) max = current;
        }
        return max;

    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
