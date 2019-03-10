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
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

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
    static int myLargestProposedSeqNo=0;
    static int myLargestAgreedSeqNo =0;
    static int currentMaxProposedNo=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        final String processId = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);

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
                EditText editTextMessage = (EditText) findViewById(R.id.editText1);
                String message = editTextMessage.getText().toString();
                editTextMessage.setText("");
                TextView displayText = (TextView) findViewById(R.id.textView1);
                displayText.append("\n");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message, String.valueOf(Integer.valueOf(processId) * 2));


            }
        });




    }
    private class ServerTask extends AsyncTask<ServerSocket, String,Void>{
        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {

            try {
                TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                String processID = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
                Log.i("SERVER- " +Integer.valueOf(processID)*2, " My PROCESS ID-" + processID);
                Log.i("SERVER-"+Integer.valueOf(processID)*2, " My PORT NO-" + Integer.valueOf(processID)*2);

                ServerSocket serverSocket = serverSockets[0];
                while(true)
                {
                    Socket socket = serverSocket.accept();
                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    String messageFromClient = inputStream.readUTF();
                    Log.i("SERVER-"+Integer.valueOf(processID)*2, "MESSAGE RECEIVED FROM CLIENT" + messageFromClient);

                    String [] messageFromClientTokens = messageFromClient.split(":");
                    Log.i("Message length--",String.valueOf(messageFromClientTokens.length));
                    if( messageFromClientTokens.length == 3) ManipulateClientMessage(messageFromClientTokens,socket,processID);
                    else {

                        Log.i("SERVER-" + Integer.valueOf(processID) * 2, "MESSAGE TO BE DELIVERED--"+ messageFromClientTokens[0] + " -" + messageFromClientTokens[1]);
                        Log.i("SERVER-" + Integer.valueOf(processID) * 2, "MESSAGE SENT");
                        publishProgress(  messageFromClientTokens[2] + " -" + messageFromClientTokens[1]);
                        myLargestAgreedSeqNo = Integer.valueOf(messageFromClientTokens[2]);
                        Log.i("SERVER-" + Integer.valueOf(processID) * 2, "My largest agreed sequence no -" + myLargestAgreedSeqNo);

                    }

                }


            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }


        protected void onProgressUpdate(String... values) {
            String strReceived = values[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            remoteTextView.append("\n");
            return;

        }
    }


    private void  ManipulateClientMessage(String [] messageFromClientTokens, Socket socket, String processID) {
        try
        {
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            int myProposedSeqNo = 1 + Math.max(myLargestProposedSeqNo, myLargestAgreedSeqNo);
            Log.i("SERVER-"+Integer.valueOf(processID)*2, " My proposed sequence no-" + myProposedSeqNo);
            Log.i("SERVER-"+Integer.valueOf(processID)*2, "MESSAGE SENDING FROM SERVER" + Integer.valueOf(processID) * 2 + ":" + messageFromClientTokens[1] + ":" + myProposedSeqNo);
            outputStream.writeUTF(  Integer.valueOf(processID) * 2 + ":" + messageFromClientTokens[1] + ":" + myProposedSeqNo);
            outputStream.flush();
            Log.i("SERVER-"+Integer.valueOf(processID)*2,"MESSAGE SENT");
            currentMaxProposedNo = myProposedSeqNo;
        }catch(Exception ex)
        {
            ex.printStackTrace();
        }

    }

    private void DeliverClientMessage(String [] messageFromClientTokens, String processID){

    }

    private class ClientTask extends AsyncTask<String, Void,Void> {
        @Override
        protected Void doInBackground(String... strings) {

            String messageText = strings[0];
            String messageSendingPort = strings[1];


            try {
                currentMaxProposedNo = sendingMessageFromClientFirstTime (messageSendingPort,messageText,currentMaxProposedNo);
                sendingMessageFromClientToDeliver(messageSendingPort,messageText,currentMaxProposedNo);

                }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }

            return null;
        }
    }

    private String getSendingMessage( String sendingPort,  String messageText,int myLargestProposedSeqNo, boolean isFirstTime,String delimeter) {
        if(isFirstTime)
            return sendingPort + delimeter + messageText + delimeter + String.valueOf(myLargestProposedSeqNo);
        else
            return sendingPort + delimeter + messageText + delimeter + String.valueOf(myLargestProposedSeqNo) + delimeter + "deliver";
            //TODO Think something better to differenciate betwween the initital and to be delivered message

    }
    private int CalculateCurrentMaxProposedNo(int currentMaxProposedNo,String receivedMessageServer, String delimeter){

            if (receivedMessageServer != "" && receivedMessageServer != null) {
                String []receivedMessageServerTokens = receivedMessageServer.split(delimeter);
                return Math.max(currentMaxProposedNo, Integer.valueOf(receivedMessageServerTokens[2]));

            }

        return -1;


    }
    private int sendingMessageFromClientFirstTime(String messageSendingPort, String messageText, int currentMaxProposedNo) throws Exception {

            try {
                int i = 0;
                while (i < ports.length) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(ports[i]));
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    Log.i("Client - " + messageSendingPort, "SENDING MESSAGE TO GET PROPOSALS-" + getSendingMessage(messageSendingPort, messageText, myLargestProposedSeqNo,true, ":"));
                    outputStream.writeUTF(getSendingMessage(messageSendingPort, messageText, myLargestProposedSeqNo, true,":"));
                    outputStream.flush();
                    Log.i("Client - " + messageSendingPort, "MESSAGE SENT TO GET PROPOSALS");

                    Log.i("Client-" + messageSendingPort, " RECEIVING THE PROPOSED NO FROM OTHER AVDS");

                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    String receivedMessageServer = inputStream.readUTF();

                    currentMaxProposedNo = CalculateCurrentMaxProposedNo(currentMaxProposedNo, receivedMessageServer, ":");

                    i++;

                }
                return  currentMaxProposedNo;
            } catch (Exception ex) {
                throw ex;
            }



        }
    private void sendingMessageFromClientToDeliver(String messageSendingPort, String messageText, int currentMaxProposedNo) throws Exception {
        try {
            int i = 0;
            while (i < ports.length) {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(ports[i]));
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                Log.i("Client - " + messageSendingPort, "SENDING MESSAGE TO DELIVER-" + getSendingMessage(messageSendingPort, messageText, myLargestProposedSeqNo, false,":"));
                outputStream.writeUTF(getSendingMessage(messageSendingPort, messageText, currentMaxProposedNo, false, ":"));
                outputStream.flush();
                Log.i("Client - " + messageSendingPort, "MESSAGE SENT TO DELIVER");
                i++;

            }
        } catch (Exception ex) {
            throw ex;
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

}
