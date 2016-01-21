package com.kku.com.termial;

    import java.io.IOException;
    import java.io.InputStream;
    import java.io.OutputStream;
    import java.util.UUID;

    import android.app.Activity;
    import android.bluetooth.BluetoothAdapter;
    import android.bluetooth.BluetoothDevice;
    import android.bluetooth.BluetoothServerSocket;
    import android.bluetooth.BluetoothSocket;

    import android.os.Bundle;
    import android.os.Handler;
    import android.os.Message;
    import android.view.View;
    import android.view.View.OnClickListener;
    import android.view.Window;
    import android.widget.ArrayAdapter;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.ListView;

    import android.support.v7.app.AppCompatActivity;

public class chat extends AppCompatActivity {
    private BluetoothDevice device;
    private static final String NAME = "BluetoothChat";

    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c966");
    private static final int MESSAGE_READ = 1;

    private ConnectThread mConnectThread = null;
    private AcceptThread mAcceptThread=null;
    private ConnectedThread mConnectedThread = null;
    private BluetoothAdapter mBluetoothAdapter=null;
    private BluetoothSocket socket=null;

    private ArrayAdapter<String> mConversationArrayAdapter;
    private ListView mCoverstionView;
    private EditText mInputText;
    private Button mSendButton;
    private String mDeviceName;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            mDeviceName = (String) bundle.get("device_name");
        }
        this.setTitle("talking with" + mDeviceName);
        String address = (String) bundle.get("device_address");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        device = mBluetoothAdapter.getRemoteDevice(address);

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        if (mConnectThread == null) {
            mConnectThread = new ConnectThread(device);
            mConnectThread.start();
        }

        mConversationArrayAdapter = new ArrayAdapter<String>(this,R.layout.message);
        mCoverstionView = (ListView) findViewById(R.id.converstionarea);
        mCoverstionView.setAdapter(mConversationArrayAdapter);

        mInputText = (EditText) findViewById(R.id.inputtext);
        mSendButton = (Button) findViewById(R.id.send);
        mSendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mInputText.getText().toString();
                //send message to other device
                mConnectedThread.write(message.getBytes());
                mConversationArrayAdapter.add("me: " + message);
                mInputText.setText("");
                mInputText.setFocusable(true);
            }
        });

    }
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                if (socket != null) {
                    mConnectedThread = new ConnectedThread(socket);
                    mConnectedThread.start();
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        /**
         * Will cancel the listening socket,and cause the thread to finish
         */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            //Get a bluetoothSocket to connect with the given vluetoothDevice
            try {
                // MY_UUID is the app's UUID string,also used by the server
                //code
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            //cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();
            try {
                //Connect the devvice through the socket. this will back
                //untill it succeed or thorows an excxption
            mmSocket.connect();
            } catch (IOException closeException) {
                //Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException close) {
                }
                return;
            }
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }

        /**
         * Will cancel an in-Progress connection, and close the Socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutSrteam;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutSrteam = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
               mmOutSrteam.write(bytes);
            } catch (IOException e) {
            }
        }
        public void cancel(){
            try{
                mmSocket.close();
            }catch (IOException e){
            }
        }
    }
    private final Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
             if(msg.what==MESSAGE_READ){
             byte[] readbuf = (byte[])msg.obj;
            String readMessage = new String(readbuf, 0,msg.arg1);
            mConversationArrayAdapter.add(mDeviceName + ": " + readMessage);
        }
        }
        };
        }