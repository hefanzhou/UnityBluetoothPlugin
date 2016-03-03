using UnityEngine;
using System.Collections;
using System.Collections.Generic;

namespace Bluetooth
{
    public class BluetoothDevice
    {
        private string name;

        public string Name
        {
            get { return name; }
        }
        private string address;

        public string Address
        {
            get { return address; }
        }
        public BluetoothDevice(string name, string address)
        {
            this.name = name;
            this.address = address;
        }

        public override string ToString()
        {

            return "name:" + name + " address:" + address;
        }
    }
    public class AndroidBridge : MonoBehaviour
    {
        private List<BluetoothDevice> deviceList = new List<BluetoothDevice>();
        public List<BluetoothDevice> DeviceList
        {
            get
            {
                return deviceList;
            }
        }

        static AndroidBridge instance;
        public static AndroidBridge Instance
        {
            get
            {
                if (instance == null) Initialize();
                return instance;
            }
        }

        private int state = STATE_NONE;

        public int State
        {
          get { return state; }
        }

        private BluetoothDevice myDevice = null;

        public BluetoothDevice MyDevice
        {
            get 
            {
                if (myDevice == null)
                {
                    string deviceStr = GetMyDeviceAddressAndName();
                    string[] nameAndAddress = deviceStr.Split(SEPARATOR);
                    if (nameAndAddress.Length < 2) Debug.LogError("device expec advice string but get:" + deviceStr);
                    myDevice = new BluetoothDevice(nameAndAddress[0], nameAndAddress[1]);
                }
                return myDevice; 
            }
        }

        private static int OPENSUCCESS = 1;
        private static int AERLDYOPEN = 2;
        private static char SEPARATOR = '@';
        private static char SEPARATORADVICE = '#';

        private const int ERROR_CANTOPENBT = 1;

        public const int STATE_NONE = 0;       // we're doing nothing
        public const int STATE_LISTEN = 1;     // now listening for incoming connections
        public const int STATE_CONNECTING = 2; // now initiating an outgoing connection
        public const int STATE_CONNECTED = 3;  // now connected to a remote device

        public delegate void FindDeviceHandler(BluetoothDevice device);
        public event FindDeviceHandler OnFindDeviceEvent;

        public delegate void OnReciveMsgHandler(string msg);
        public event OnReciveMsgHandler OnReciveMsgEvent;
        public event OnReciveMsgHandler OnConnectDeviceEvent;

        public delegate void OnStateChangeHandler(int state);
        public OnStateChangeHandler OnstateChangeEvent;

        public static void Initialize()
        {
            if (instance == null)
            {
                GameObject newGameObject = new GameObject("AndroidMsgBridge");
                newGameObject.AddComponent<AndroidBridge>();
                instance = newGameObject.GetComponent<AndroidBridge>();
                Debug.Log("initialize success");
            }
        }

        void Start()
        {
            if (gameObject.name != "AndroidMsgBridge") Destroy(GetComponent<AndroidBridge>());
            AndroidBridge.Initialize();
        }



        //////////////////////android call unity
        public void OnFindDevice(string deviceStr)
        {
            string[] nameAndAddress = deviceStr.Split(SEPARATOR);
            if (nameAndAddress.Length < 2) Debug.LogError("device expec advice string but get:" + deviceStr);
            BluetoothDevice device = new BluetoothDevice(nameAndAddress[0], nameAndAddress[1]);

            foreach (BluetoothDevice tempDevice in deviceList)
            {
                if (tempDevice.Address == device.Address) return;
            }
            deviceList.Add(device);
            OnFindDeviceEvent(device);
        }

        public void OnReciveMsg(string msg)
        {
            Debug.Log("recive msg:" + msg);
            OnReciveMsgEvent(msg);
        }

        public void OnConnected(string deviceName)
        {
            OnConnectDeviceEvent(deviceName);
        }

        //////////////////////unity call android
        public bool OpenBluetooth()
        {
            AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
            int result = jo.Call<int>("Open");
            if (result == OPENSUCCESS) return true;
            else if (result == AERLDYOPEN) return true;
            else return false;
        }

        public void CloseBluetooth()
        {
            AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
            jo.Call("Close");
        }

        public void Error(string errorCodeStr)
        {
            int errorCode = int.Parse(errorCodeStr);
            switch (errorCode)
            {
                case ERROR_CANTOPENBT:
                    Debug.LogError("can not open bluetooth");
                    break;
            }
        }

        public List<BluetoothDevice> FindDevice()
        {
            AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
            string str = jo.Call<string>("Find");
            string[] deviceArray = str.Split(SEPARATORADVICE);
            foreach (string tempStr in deviceArray)
            {
                OnFindDevice(tempStr);
            }
            return deviceList;
        }

        public bool Connect(BluetoothDevice device)
        {
            AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
            bool result = jo.Call<bool>("Connect", device.Address);
            return result;
        }

        public bool Connect(string address)
        {
            AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
            bool result = jo.Call<bool>("Connect", address);
            return result;
        }
        
        public bool SendMsg(string msg)
        {
            Debug.Log("sendmsg unity");
            AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
            bool result = jo.Call<bool>("SendMsg", msg);
            return result;
        }

        public void EnsureDiscoverable()
        {
            AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
            jo.Call("EnsureDiscoverable");
        }

        public void StartServer()
        {
            AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
            jo.Call("StartServer");
        }
        public void OnStateChange(string stateCode)
        {
            state = int.Parse(stateCode);
            OnstateChangeEvent(state);
        }

        private string GetMyDeviceAddressAndName()
        {
            AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
            string result = jo.Call<string>("GetMyDeviceAddressAndName");
            return result;
        }
    }
}


