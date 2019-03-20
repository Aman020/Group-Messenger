package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    private class Message {
        float mySequenceNo;
        String messageText;
        boolean toBeDelivered;
        String mesageSendingPort;
        int myPort;

        Message(float mySequenceNo, String messageText, boolean toBeDelivered, String mesageSendingPort, int myPort)

        {
            this.mesageSendingPort = mesageSendingPort;
            this.mySequenceNo = mySequenceNo;
            this.messageText = messageText;
            this.toBeDelivered = toBeDelivered;
            this.myPort = myPort;
        }
    }

    private final static int SERVER_PORT = 10000;
    private static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final String [] ports = new String[] {"11108","11112","11116","11120","11124"};
    private  static  final Uri CONTENT_URI = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
    Socket[] sockets = new Socket[5];
    static int actualSequenceNo=0;
    static float myLargestAgreedSeqNo =0.0F;
    static float currentMaxProposedNo=0.0F;
    static int setCount =0;
    static String failedport="";
    static int counter =0;
    boolean isAlreadyRemoved =false;
    List<Message> holdBackQueue = new ArrayList<Message>();

    private class MessageCompare implements Comparator<Message>
    {
        @Override
        public int compare(Message lhs, Message rhs) {
            if(lhs.mySequenceNo < rhs.mySequenceNo) return -1;
            else if (lhs.mySequenceNo >rhs.mySequenceNo) return 1;
            else
            {
                if( lhs.myPort > rhs.myPort) return 1;
                else return -1;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        final String processId = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        Log.e("*********","****MY PORT ID*****----"+ Integer.parseInt(processId)*2);

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

                ServerSocket serverSocket = serverSockets[0];
                while (true) {
                    Socket socket = serverSocket.accept();
                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    String messageFromClient = inputStream.readUTF();
                    String[] messageFromClientTokens = messageFromClient.split(":");

                    if(!failedport.equals(""))
                    {

                        Log.e("Failed port not null", failedport);
                        removeAllFailedMesages(failedport);
                    }

                    if (messageFromClientTokens.length == 3) ManipulateClientMessage(messageFromClientTokens, socket, processID);
                    else if (messageFromClientTokens.length == 4){
                        myLargestAgreedSeqNo = Math.max(myLargestAgreedSeqNo,Float.valueOf(messageFromClientTokens[2]));
                        ContentValues content = new ContentValues();


                        for (int i = 0; i < holdBackQueue.size(); i++) {
                            if (holdBackQueue.get(i).messageText.equals(messageFromClientTokens[1])) {
                                Message toUpdateMessage = holdBackQueue.remove(i);
                                toUpdateMessage.mySequenceNo = Float.valueOf(messageFromClientTokens[2]);
                                toUpdateMessage.toBeDelivered= true;
                                toUpdateMessage.mesageSendingPort = messageFromClientTokens[0];
                                holdBackQueue.add(toUpdateMessage);
                                Collections.sort(holdBackQueue, new MessageCompare());
                                currentMaxProposedNo = currentMaxProposedNo >  Float.valueOf(messageFromClientTokens[2])?currentMaxProposedNo:Float.valueOf(messageFromClientTokens[2]);
                                setCount++;
                                break;
                            }

                        }

                        while(!holdBackQueue.isEmpty()&& holdBackQueue.get(0).toBeDelivered == true)
                        {
                            Log.i("Queue",holdBackQueue.get(0).mySequenceNo + "-" + holdBackQueue.get(0).messageText);
                            Message message = holdBackQueue.remove(0);
                            content.put("key", String.valueOf(actualSequenceNo++));
                            content.put("value", message.messageText);
                            getContentResolver().insert(CONTENT_URI, content);
                            publishProgress(actualSequenceNo + " -" + message.messageText);
                        }



                    }
                    else
                    {
                        throw new IllegalStateException("unexoected");

                    }
                }


            }
            catch(Exception ex){
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
            float myProposedSeqNo = 1 +currentMaxProposedNo;
            myProposedSeqNo =  myProposedSeqNo + Float.valueOf(processID)*2/10000;
            holdBackQueue.add(new Message(myProposedSeqNo,messageFromClientTokens[1],false,messageFromClientTokens[0] , Integer.parseInt(processID)*2));
            outputStream.writeUTF(  Integer.valueOf(processID) * 2 + ":" + messageFromClientTokens[1] + ":" + myProposedSeqNo);
            outputStream.flush();
            currentMaxProposedNo = myProposedSeqNo;
        }catch(Exception ex)
        {
            ex.printStackTrace();
        }

    }

    private class ClientTask extends AsyncTask<String, Void,Void> {
        @Override
        protected Void doInBackground(String... strings) {

            String messageText = strings[0];
            String messageSendingPort = strings[1];


            try {

                float max = sendingMessageFromClientFirstTime (messageSendingPort,messageText,++counter);
                Log.i("CurrentMaxnumber", String.valueOf(currentMaxProposedNo));
                Thread.sleep(500);
                sendingMessageFromClientToDeliver(messageSendingPort,messageText, max);
                Thread.sleep(500);

            }

            catch(Exception ex)
            {
                ex.printStackTrace();
            }

            return null;
        }
    }

    private String getSendingMessage( String sendingPort,  String messageText,float myLargestProposedSeqNo, boolean isFirstTime,String delimeter) {
        if(isFirstTime)
            return sendingPort + delimeter + messageText + delimeter + String.valueOf(myLargestProposedSeqNo);
        else
            return sendingPort + delimeter + messageText + delimeter + String.valueOf(myLargestProposedSeqNo) + delimeter + "deliver";
        //TODO Think something better to differenciate betwween the initital and to be delivered message

    }
    private  float  CalculateCurrentMaxProposedNo(float currentMaxProposedNo,String receivedMessageServer, String delimeter){

        if (receivedMessageServer != "" && receivedMessageServer != null) {
            String []receivedMessageServerTokens = receivedMessageServer.split(delimeter);
            return Math.max(currentMaxProposedNo, Float.valueOf(receivedMessageServerTokens[2]));



        }

        return -1;


    }
    private void removeAllFailedMesages(String failedPort)
    {
        Log.e("removing failed message","removing falied messages");
        Log.e("Size of queue", String.valueOf(holdBackQueue.size()));
        List<Message> d = new ArrayList<Message>();
        for(int i=0;i< holdBackQueue.size();i++)
        {
            if(holdBackQueue.get(i).mesageSendingPort.equals(failedPort))
            {
                Log.e("Added--", holdBackQueue.get(i).mesageSendingPort + "-" + holdBackQueue.get(i).messageText + "-" +holdBackQueue.get(i).toBeDelivered);
                d.add(holdBackQueue.get(i));
            }
        }
        for (Message m: d) {
            Log.e("Removing--", m.mesageSendingPort + "-" + m.messageText + "-" + m.toBeDelivered);
            holdBackQueue.remove(m);
        }

    }
    private float sendingMessageFromClientFirstTime(String messageSendingPort, String messageText, float currentMaxProposedNo) {


        int i = 0;
        while (i < ports.length) {
            try {
                if( ports[i].equals(failedport))
                {i++;}
                else {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(ports[i]));
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    outputStream.writeUTF(getSendingMessage(messageSendingPort, messageText, currentMaxProposedNo, true, ":"));
                    outputStream.flush();
                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    String receivedMessageServer = inputStream.readUTF();
                    currentMaxProposedNo = CalculateCurrentMaxProposedNo(currentMaxProposedNo, receivedMessageServer, ":");
                    i++;
                }
            }
            catch (Exception ex) {
                Log.i("Inside","Exception");
                Log.i("Exception-----","**********" + ports[i]);
                failedport = ports[i];
                ex.printStackTrace();
            }
        }
        return  currentMaxProposedNo;

    }

    private void sendingMessageFromClientToDeliver(String messageSendingPort, String messageText, float currentMaxProposedNo) throws Exception {

        int i = 0;
        while (i < ports.length) {
            if (ports[i].equals(failedport)){ i++;}
            else {
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(ports[i]));
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    outputStream.writeUTF(getSendingMessage(messageSendingPort, messageText, currentMaxProposedNo, false, ":"));
                    outputStream.flush();
                    i++;
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    Log.i("Inside", "Exception");
                    Log.i("Exception-----", "**********" + ports[i]);
                    failedport = ports[i];
                }
            }
        }



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

}
