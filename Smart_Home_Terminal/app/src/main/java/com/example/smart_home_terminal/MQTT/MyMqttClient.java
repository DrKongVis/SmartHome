package com.example.smart_home_terminal.MQTT;


import android.os.Handler;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


public class MyMqttClient  {

    public MqttClient mqttClient = null;
    private MemoryPersistence memoryPersistence = null;
    private MqttConnectOptions mqttConnectOptions = null;
    private String ClientName = "Drkong";

    public boolean start(String clientId, Handler DataHandler,Handler BackHandler) {
        //初始化连接设置对象
        mqttConnectOptions = new MqttConnectOptions();
        //设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
        mqttConnectOptions.setCleanSession(true);
        //设置连接超时时间，单位是秒
        mqttConnectOptions.setConnectionTimeout(10);
        //设置持久化方式
        memoryPersistence = new MemoryPersistence();
        if(null != clientId) {
            try {
                //mqttClient = new MqttClient("tcp://182.92.150.158:1883", clientId,memoryPersistence);
                mqttClient = new MqttClient("tcp://192.168.31.146:1883", clientId,memoryPersistence);
            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        //设置连接和回调
        if(null != mqttClient) {
            if(!mqttClient.isConnected()) {
                //创建回调函数对象
                MQTTReceiveCallback MQTTReceiveCallback = new MQTTReceiveCallback(DataHandler,BackHandler,mqttClient);
                //客户端添加回调函数
                mqttClient.setCallback(MQTTReceiveCallback);
                //创建连接
                try {
                    System.out.println("创建连接");
                    mqttClient.connect(mqttConnectOptions);
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }else {
            System.out.println("mqttClient为空");
        }
        subTopic("iot");
        subTopic("back");

        return mqttClient.isConnected();
    }

    //	关闭连接
    public void closeConnect() {
        //关闭存储方式
        if(null != memoryPersistence) {
            try {
                memoryPersistence.close();
            } catch (MqttPersistenceException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else {
            System.out.println("memoryPersistence is null");
        }

        //关闭连接
        if(null != mqttClient) {
            if(mqttClient.isConnected()) {
                try {
                    mqttClient.disconnect();
                    mqttClient.close();
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }else {
                System.out.println("mqttClient is not connect");
            }
        }else {
            System.out.println("mqttClient is null");
        }
    }

    //	发布消息
    public void publishMessage(String pubTopic, String message, int qos) {
        if(null != mqttClient&& mqttClient.isConnected()) {
            System.out.println("发布消息   "+mqttClient.isConnected());
            System.out.println("id:"+mqttClient.getClientId());
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(qos);
            mqttMessage.setPayload(message.getBytes());

            MqttTopic topic = mqttClient.getTopic(pubTopic);

            if(null != topic) {
                try {
                    MqttDeliveryToken publish = topic.publish(mqttMessage);
                    if(!publish.isComplete()) {
                        System.out.println("消息发布成功");
                    }
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }else {
            reConnect();
        }
    }

    //	重新连接
    public void reConnect() {
        if(null != mqttClient) {
            if(!mqttClient.isConnected()) {
                if(null != mqttConnectOptions) {
                    try {
                        mqttClient.connect(mqttConnectOptions);
                    } catch (MqttException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }else {
                    System.out.println("mqttConnectOptions is null");
                }
            }else {
                System.out.println("mqttClient is null or connect");
            }
        }
    }
    //	订阅主题
    public void subTopic(String topic) {
        if(null != mqttClient&& mqttClient.isConnected()) {
            try {
                mqttClient.subscribe(topic, 1);
            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else {
            System.out.println("mqttClient is error");
        }
    }

    //	清空主题
    public void cleanTopic(String topic) {
        if(null != mqttClient&& !mqttClient.isConnected()) {
            try {
                mqttClient.unsubscribe(topic);
            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else {
            System.out.println("mqttClient is error");
        }
    }

}