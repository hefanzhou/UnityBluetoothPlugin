package com.vr.bluetooth;

import java.util.ArrayList;
import java.util.Set;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;
import com.vr.testndk.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class BlueActivity extends UnityPlayerActivity {

	private ArrayList<BluetoothDevice> deviceList;

	private static final String TAG = "Unity";
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static String EXTRA_DEVICE_ADDRESS = "device_address";

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private static final int REQUEST_VISIBLE = 3;
	// Name of the connected device
	private String mConnectedDeviceName = null;
	// String buffer for outgoing messages
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "OnCreate");
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);

		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (mBluetoothAdapter.isEnabled()) {
			if (mChatService == null)
				setupChat();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Make sure we're not doing discovery anymore
		if (mBluetoothAdapter != null) {
			mBluetoothAdapter.cancelDiscovery();
		}

		// Unregister broadcast listeners
		this.unregisterReceiver(mReceiver);
		mChatService.stop();
	}

	private void setupChat() {
		// Initialize the BluetoothChatService to perform bluetooth connections
		deviceList = new ArrayList<BluetoothDevice>();
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		for (BluetoothDevice bt : pairedDevices)
			deviceList.add(bt);
		mChatService = new BluetoothChatService(this, mHandler);
	}








	public void ConnectTo(BluetoothDevice device) {
		Log.i(TAG, "connectTo:" + device.getName());
		mChatService.connect(device);
	}
	

	


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				OnStateChange(msg.arg1);
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					Log.i(TAG, "CONNECTED:" + mConnectedDeviceName);
					break;
				case BluetoothChatService.STATE_CONNECTING:
					Log.i(TAG, "CONNECTing");
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				String readMessage = new String(readBuf, 0, msg.arg1);
				OnReciveMsg(readMessage);
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				OnConnected(mConnectedDeviceName);
				Toast.makeText(getApplicationContext(),"Connected to " + mConnectedDeviceName,Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	private void DoDiscovery() {
		if (D)
			Log.d(TAG, "doDiscovery()");

		// If we're already discovering, stop it
		if (mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		}

		// Request discover from BluetoothAdapter
		mBluetoothAdapter.startDiscovery();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {

		case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) {
				setupChat();

			} else {
				Log.d(TAG, "BT not enabled");
				PushErrorToUnity(ERROR_CANTOPENBT);
			}
		case REQUEST_VISIBLE :
			
		case REQUEST_CONNECT_DEVICE :
			
		}
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's already paired, skip it, because it's been listed
				// already
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					AddDevice(device);
					Log.i(TAG, "find device:" + device.getName() + "\n"
							+ device.getAddress());
				}
				// When discovery is finished, change the Activity title
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				if (deviceList.isEmpty()) {
					Log.i(TAG, "find finish but not device");
				}
			}
		}
	};
	
	
	
	///Interface to unity
	private static final int OPENSUCCESS = 1;
	private static final int AERLDYOPEN = 2;
	private static final char SEPARATOR = '@';
	private static final char SEPARATORADVICE = '#';
	
	private static final int ERROR_CANTOPENBT = 1;
	
	public int Open() {
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			return OPENSUCCESS;
		} else {
			return AERLDYOPEN;
		}
	}
	
	public int IsOpen() {
		if(mBluetoothAdapter.isEnabled())
			return 1;
		else 
			return 0;
	}

	public void Close() {
		mBluetoothAdapter.disable();
		Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG).show();
	}
	
	//let device can being discovered
	public void EnsureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}
	
	//return advice by format name@adress#name2@adress2#...
	public String Find() {
		if (IsOpen() == 0) Open();
		DoDiscovery();
		StringBuilder deviceSB = new StringBuilder();
		
		for (BluetoothDevice bt : deviceList)
			deviceSB.append(bt.getName()).append(SEPARATOR).append(bt.getAddress()).append(SEPARATORADVICE);
		
		return deviceSB.toString();
	}
	
	private void AddDevice(BluetoothDevice btDevice)
	{
		if (!deviceList.contains(btDevice))
		{
			deviceList.add(btDevice);
			UnityPlayer.UnitySendMessage("AndroidMsgBridge","OnFindDevice", 
					btDevice.getName() + SEPARATOR + btDevice.getAddress());
		}
	}
	
	private BluetoothDevice GetDeviceByAddress(String address)
	{
		for(BluetoothDevice bt : deviceList)
		{
			if (bt.getAddress().equals(address))
				return bt;
		}
		return null;
	}
	
	public boolean Connect(String address) {
		BluetoothDevice btDevice = GetDeviceByAddress(address);
		if(btDevice == null) return false;
		ConnectTo(btDevice);
		return true;
	}
	
	private void OnReciveMsg(String msg) {
		UnityPlayer.UnitySendMessage("AndroidMsgBridge","OnReciveMsg",msg);
	}
	
	public boolean SendMsg(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, "not connect", Toast.LENGTH_SHORT).show();
			return false;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);
			Log.i(TAG, "send:" + message);
			return true;
		}
		
		return false;
	}
	
	private void OnConnected(String deviceName) {
		UnityPlayer.UnitySendMessage("AndroidMsgBridge","OnConnected",deviceName);
	}
	
	public void StartServer() {
		mChatService.start();
	}
	
	public void PushErrorToUnity(int errorCode) {
		UnityPlayer.UnitySendMessage("AndroidMsgBridge","Error",String.valueOf(errorCode));
	}
	
	private void OnStateChange(int state) {
		UnityPlayer.UnitySendMessage("AndroidMsgBridge","OnStateChange",String.valueOf(state));
	}
	
	public String GetMyDeviceAddressAndName() {
	 	return mBluetoothAdapter.getName() + SEPARATOR + mBluetoothAdapter.getAddress();
	}
}
