using UnityEngine;
using System.Collections;
using Bluetooth;
using System.Text;

public class TestBluetooth : MonoBehaviour {
    private StringBuilder msgSB = new StringBuilder("msg:/r/n");
    private string sendStr = "";
	// Use this for initialization
	void Start () {
        AndroidBridge.Instance.OnReciveMsgEvent += OnReciveMsg;
	}

    void OnReciveMsg(string msg)
    {
        msgSB.Append("remote：").Append(msg).Append("/r/n");
    }
	
	// Update is called once per frame
	void Update () {
	
	}

    void OnGUI()
    {
        if (AndroidBridge.Instance.State == AndroidBridge.STATE_LISTEN)
        {
            GUILayout.Label("Server is open");
        }
        if (AndroidBridge.Instance.State == AndroidBridge.STATE_CONNECTED)
        {
            sendStr = GUILayout.TextArea(sendStr);
            if (GUILayout.Button("Send", GUILayout.Width(200), GUILayout.Height(60)))
            {
                AndroidBridge.Instance.SendMsg(sendStr);
                msgSB.Append("me:").Append(sendStr).Append("/r/n");
                sendStr = "";
            }

            GUILayout.Label(msgSB.ToString());

            return;
        }
        if (GUILayout.Button("Open",GUILayout.Width(200), GUILayout.Height(60)))
        {
            AndroidBridge.Instance.OpenBluetooth();
        }

        if (GUILayout.Button("Close", GUILayout.Width(200), GUILayout.Height(60)))
        {
            AndroidBridge.Instance.CloseBluetooth();
        }

        if (GUILayout.Button("Find", GUILayout.Width(200), GUILayout.Height(60)))
        {
            AndroidBridge.Instance.FindDevice();
        }

        if (GUILayout.Button("StartServer", GUILayout.Width(200), GUILayout.Height(60)))
        {
            AndroidBridge.Instance.StartServer();
        }

        if (GUILayout.Button("Discorverable", GUILayout.Width(200), GUILayout.Height(60)))
        {
            AndroidBridge.Instance.EnsureDiscoverable();
        }


        GUILayout.Label("Device List" + AndroidBridge.Instance.MyDevice);
        foreach (BluetoothDevice bt in AndroidBridge.Instance.DeviceList)
        {
            if (GUILayout.Button(bt.Name, GUILayout.Width(200), GUILayout.Height(60)))
            {
                AndroidBridge.Instance.Connect(bt);
            }
        }
    }
}
