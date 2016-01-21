package com.kku.com.termial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    static final ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();;

    private SimpleAdapter adapter;
    private ListView listview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);



        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null){
            Toast.makeText(this,"Device does not support Bluetooth",Toast.LENGTH_LONG).show();
        }
        if (!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,1);
        }

    createSimpleAdapter();
    listview = (ListView) findViewById(R.id.device_list);
    setProgressBarIndeterminateVisibility(true);
    if(mBluetoothAdapter.isDiscovering()){
        mBluetoothAdapter.cancelDiscovery();
    }
        mBluetoothAdapter.startDiscovery();
    ensureDiscoverable();

    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

    setProgressBarIndeterminateVisibility(true);
    //find paired device
    if (pairedDevices.size()>0){
        for (BluetoothDevice device :pairedDevices){
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("device_name",device.getName());
            map.put("device_address",device.getAddress());
            list.add(map);
        }
    }
    //find Bluetooth device
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    registerReceiver(mReceiver,filter);

    filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    this.registerReceiver(mReceiver, filter);

    //Start Activity when click on device item
    listview.setOnItemClickListener(new OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?>arg0,View arg1,int position,long arg3){
					Intent intent = new Intent(MainActivity.this,chat.class);
					 if(position >=0){
					 intent.putExtra("device_name",list.get(position).get("device_name"));
					 intent.putExtra("device_adress",list.get(position).get("device_adress"));
					 startActivity(intent);
				}
        }
    });
    listview.setAdapter(adapter);
}
   private void ensureDiscoverable(){
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivity(discoverableIntent);
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(mBluetoothAdapter != null){
            mBluetoothAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
    }
    public void createSimpleAdapter(){
        adapter = new SimpleAdapter(this,list,R.layout.content_main,new String[]{
                "device_name","device_address"},new int[]{
                R.id.device_name, R.id.device_address});
    }
    @Override
    protected void onActivityResult(int requestcode, int resultcode, Intent intent){
        super.onActivityResult(requestcode, resultcode, intent);
        if(requestcode==1){
            if(resultcode==RESULT_OK){
                Toast.makeText(this,"Enabled Bluetooth succeed.",Toast.LENGTH_LONG).show();

            }else if(resultcode==RESULT_OK){
                Toast.makeText(this,"Can't enabled Bluetooth.",Toast.LENGTH_LONG).show();
            }
        }
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){
        public void onReceive(Context context,Intent intent){
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState()!= BluetoothDevice.BOND_BONDED){
                    HashMap<String, String> map= new HashMap<String, String>();
                    map.put("device_name",device.getName());
                    map.put("device_Address",device.getAddress());
                    list.add(map);
                }

                createSimpleAdapter();
                listview.setAdapter(adapter);
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

                setProgressBarIndeterminateVisibility(false);

                if (adapter.getCount()==0){
                    Toast.makeText(context, "No device found.",Toast.LENGTH_SHORT).show();

                }
            }

        }
    };
}
