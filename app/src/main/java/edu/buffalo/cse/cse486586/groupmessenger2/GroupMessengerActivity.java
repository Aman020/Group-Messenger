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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
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

    private class Message
        {
        double mySequenceNo;
        String messageText;
        boolean toBeDelivered;
        Message(double mySequenceNo, String messageText, boolean toBeDelivered)
    {
            this.mySequenceNo = mySequenceNo;
            this.messageText = messageText;
            this.toBeDelivered = toBeDelivered;
        }
    }


    private final static int SERVER_PORT = 10000;
    private static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final String [] ports = new String[] {"11108","11112","11116","11120","11124"};
    private  static  final Uri CONTENT_URI = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
    Socket[] sockets = new Socket[5];
    static int actualSequenceNo=0;
    static double myLargestAgreedSeqNo =0.0000;
    static double currentMaxProposedNo=0.00000;
    SortedMap<Double, String > messageToDeliverList = new TreeMap<Double, String>();
    Queue<Message> deliveryQueue = new LinkedList<Message>();
    static int setCount =0;
    PriorityBlockingQueue<Message> holdBackQueue = new PriorityBlockingQueue<Message>(1000, new MessageCompare());

     private class MessageCompare implements Comparator<Message>
     {
         @Override
         public int compare(Message lhs, Message rhs) {
             if(lhs.mySequenceNo < rhs.mySequenceNo) return 1;
             else return -1;
         }
     }

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
            ReentrantLock lock = new ReentrantLock();

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

                    if( messageFromClientTokens.length == 3) {

                        ManipulateClientMessage(messageFromClientTokens, socket, processID);
                    }
                    else {

                        Log.i("SERVER-" + Integer.valueOf(processID) * 2, "MESSAGE TO BE DELIVERED--"+ messageFromClientTokens[0] + " -" + messageFromClientTokens[1]);
                        Log.i("SERVER-" + Integer.valueOf(processID) * 2, "MESSAGE SENT");

                            Iterator value = holdBackQueue.iterator();
                            while (value.hasNext()) {

                                Message msg = (Message) value.next();
                                if (msg.messageText.equals(messageFromClientTokens[1])) {
                                    holdBackQueue.remove(msg);
                                    msg.mySequenceNo = Double.valueOf(messageFromClientTokens[0]);
                                    msg.toBeDelivered = true;
                                    holdBackQueue.add(msg);
                                }

                            }

                        //messageToDeliverList.put(Double.valueOf(messageFromClientTokens[2]),messageFromClientTokens[1]);
                        //setCount++;

                       // Set set = messageToDeliverList.entrySet();
                        //Iterator iterator = set.iterator();

                        if (holdBackQueue.peek() != null && holdBackQueue.peek().toBeDelivered == true) {
                            deliveryQueue.add(holdBackQueue.poll());
                        }
                            myLargestAgreedSeqNo = Double.valueOf(messageFromClientTokens[2]);
//
//                            while (iterator.hasNext()) {
//
                                ContentValues content = new ContentValues();
//                                Map.Entry m = (Map.Entry) iterator.next();
//                                double key = (Double) m.getKey();
//                                String message = (String) m.getValue();
//
//                                Log.i("Delivered Message List" +  Integer.valueOf(processID) * 2, "key-" + key + " " +  "Message-"+message );
                               lock.lock();
                                while(! deliveryQueue.isEmpty() && deliveryQueue.peek().toBeDelivered == true) {

                                    Message message = deliveryQueue.poll();
                                       Log.i("Final----", String.valueOf(actualSequenceNo) + "-" + message.messageText);
                                        content.put("key", String.valueOf(actualSequenceNo++));
                                        content.put("value", message.messageText);
                                        getContentResolver().insert(CONTENT_URI, content);
                                        publishProgress(actualSequenceNo + " -" + message.messageText);
                                        lock.unlock();
                                        Log.i("SERVER-" + Integer.valueOf(processID) * 2, "My largest agreed sequence no -" + myLargestAgreedSeqNo);
                                        //messageToDeliverList.remove(key);


                                }
                                }


                        }

                        //for (Map.Entry<Integer, String> entry : map.entrySet()) {
                       //     System.out.println(entry.getKey() + " => " + entry.getValue());
                       // }


                   // }




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
            double myProposedSeqNo = 1 + Math.max(currentMaxProposedNo, myLargestAgreedSeqNo);
            myProposedSeqNo =  myProposedSeqNo + Double.valueOf(processID)*2/10000;

            holdBackQueue.add(new Message(myProposedSeqNo,messageFromClientTokens[1],false));
            Log.i("SERVER-"+Integer.valueOf(processID)*2,"Holdback Queue Data at head 1st time- " +  holdBackQueue.peek().messageText);
            Log.i("SERVER-"+Integer.valueOf(processID)*2, " My proposed sequence no-" +myProposedSeqNo);
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

    private String getSendingMessage( String sendingPort,  String messageText,double myLargestProposedSeqNo, boolean isFirstTime,String delimeter) {
        if(isFirstTime)
            return sendingPort + delimeter + messageText + delimeter + String.valueOf(myLargestProposedSeqNo);
        else
            return sendingPort + delimeter + messageText + delimeter + String.valueOf(myLargestProposedSeqNo) + delimeter + "deliver";
            //TODO Think something better to differenciate betwween the initital and to be delivered message

    }
    private  double  CalculateCurrentMaxProposedNo(double currentMaxProposedNo,String receivedMessageServer, String delimeter){

            if (receivedMessageServer != "" && receivedMessageServer != null) {
                String []receivedMessageServerTokens = receivedMessageServer.split(delimeter);
                return Math.max(currentMaxProposedNo, Double.valueOf(receivedMessageServerTokens[2]));



            }

        return -1;


    }
    private double sendingMessageFromClientFirstTime(String messageSendingPort, String messageText, double currentMaxProposedNo) throws Exception {

            try {
                int i = 0;
                while (i < ports.length) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(ports[i]));
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    Log.i("Client - " + messageSendingPort, "SENDING MESSAGE TO GET PROPOSALS-" + getSendingMessage(messageSendingPort, messageText, currentMaxProposedNo,true, ":"));
                    outputStream.writeUTF(getSendingMessage(messageSendingPort, messageText, currentMaxProposedNo, true,":"));
                    outputStream.flush();
                    Log.i("Client - " + messageSendingPort, "MESSAGE SENT TO GET PROPOSALS");

                    Log.i("Client-" + messageSendingPort, " RECEIVING THE PROPOSED NO FROM OTHER AVDS");

                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    String receivedMessageServer = inputStream.readUTF();

                    currentMaxProposedNo = CalculateCurrentMaxProposedNo(currentMaxProposedNo, receivedMessageServer, ":");

                    i++;

                }
               // Thread.sleep(500);
                return  currentMaxProposedNo;
            } catch (Exception ex) {
                throw ex;
            }



        }
    private void sendingMessageFromClientToDeliver(String messageSendingPort, String messageText, double currentMaxProposedNo) throws Exception {
        try {
            int i = 0;
            while (i < ports.length) {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(ports[i]));
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                Log.i("Client - " + messageSendingPort, "SENDING MESSAGE TO DELIVER-" + getSendingMessage(messageSendingPort, messageText, currentMaxProposedNo, false,":"));
                outputStream.writeUTF(getSendingMessage(messageSendingPort, messageText, currentMaxProposedNo, false, ":"));
                outputStream.flush();
                Log.i("Client - " + messageSendingPort, "MESSAGE SENT TO DELIVER");
                i++;

            }
           // Thread.sleep(500);
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
