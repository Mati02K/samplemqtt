package com.mathesh.qualcommmqttconnector;

import androidx.appcompat.app.AppCompatActivity;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private MaterialButton publishBtn;
    private static final String TAG = "MainActivity";
    private String CLIENT_ID = MqttClient.generateClientId();;
    private String TOPIC = "qualcomm/test";
    private MqttAndroidClient client;
    ArrayAdapter adapter;
    ListView listView;
    EditText publishInput;

    private String BROKER_ENDPOINT = "tcp://broker.hivemq.com:1883";

//    String[] subscribedMsgs = {"Android","IPhone","WindowsMobile","Blackberry",
//            "WebOS","Ubuntu","Windows7","Max OS X"};

    ArrayList<String> subscribedMsgs = new ArrayList<String>();


    private void updateSubscriberMsgList()
    {
        adapter = new ArrayAdapter<String>(this, R.layout.activity_listview, subscribedMsgs);

        listView = (ListView) findViewById(R.id.sub_list);
        listView.setAdapter(adapter);
    }

    private void connectMQTT()
    {
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "Connection to MQTT Broker Successful");
                    Toast.makeText(getApplicationContext(), "Connection Success", Toast.LENGTH_LONG);

                    // Once connection is successful start subscribing
                    subscribe();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(TAG, "Connection to MQTT Broker Failed");
                }
            });
        }
        catch (MqttException e)
        {
            Log.e(TAG, "Exception Occurred while connection");
            e.printStackTrace();
        }
    }

    private void subscribe()
    {
        try
        {
            client.subscribe(TOPIC, 0);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Toast.makeText(getApplicationContext(), "Connection Lost", Toast.LENGTH_LONG);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Log.d(TAG, "Topic:- " + topic);
                    Log.d(TAG, "Msg Received :- " + new String(message.getPayload()));
                    subscribedMsgs.add(new String(message.getPayload()));
                    updateSubscriberMsgList();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
        }
        catch(MqttException e)
        {
            Log.d(TAG, e.getMessage());
        }
    }

    private void publish(String payload)
    {
        try
        {
            byte[] encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(TOPIC, message);
        }
        catch (UnsupportedEncodingException | MqttException e)
        {
            e.printStackTrace();
        }
    }

    private String getWifiMacAddress()
    {
        Log.d(TAG, "called");
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN))
        {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try
        {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        }
        catch (UnknownHostException ex)
        {
            Log.e(TAG, "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }

    private void updateBrokerURL()
    {
        String ipaddress = getWifiMacAddress();
        Log.d(TAG, "Ip Address is" + ipaddress);
        if( ipaddress != null && !ipaddress.isEmpty())
        {
            BROKER_ENDPOINT = "tcp://"+ ipaddress +":1883";
            Log.d(TAG, "BROKER URL" + BROKER_ENDPOINT);
            Toast.makeText(getApplicationContext(), BROKER_ENDPOINT, Toast.LENGTH_LONG);
        }
    }

    private void init()
    {
        publishBtn = findViewById(R.id.change);
        publishInput = findViewById(R.id.SSID);
        client =
                new MqttAndroidClient(this.getApplicationContext(), BROKER_ENDPOINT,
                        CLIENT_ID);

        updateSubscriberMsgList();
        // Connect MQTT
        connectMQTT();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize all views and MQTT connections
        updateBrokerURL();
        init();
        publishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                String payload = String.valueOf(publishInput.getText());
                publish(payload);
            }
        });
    }
}