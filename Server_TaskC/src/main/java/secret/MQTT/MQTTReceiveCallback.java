package secret.MQTT;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import secret.MyUtil.Log_Util;


/**
 * 发布消息的回调类
 *
 * 必须实现MqttCallback的接口并实现对应的相关接口方法CallBack 类将实现 MqttCallBack。
 * 每个客户机标识都需要一个回调实例。在此示例中，构造函数传递客户机标识以另存为实例数据。
 * 在回调中，将它用来标识已经启动了该回调的哪个实例。
 * 必须在回调类中实现三个方法：
 *
 *  public void messageArrived(MqttTopic topic, MqttMessage message)接收已经预订的发布。
 *
 *  public void connectionLost(Throwable cause)在断开连接时调用。
 *
 *  public void deliveryComplete(MqttDeliveryToken token))
 *  接收到已经发布的 QoS 1 或 QoS 2 消息的传递令牌时调用。
 *  由 MqttClient.connect 激活此回调。
 *
 */
public class MQTTReceiveCallback implements MqttCallbackExtended {

    private MqttClient client;

    public MQTTReceiveCallback(MqttClient client) {
        this.client = client;
    }

    @Override
    public void connectComplete(boolean b, String s) {
        System.out.println("丢失重连成功");
    }

    public void connectionLost(Throwable cause) {
        // 连接丢失后，一般在这里面进行重连
        System.out.println("连接断开，可以做重连");

        Log_Util.error("connect lost");
        int i = 3;
        while (true) {
            if (i < 0) {
                //这里是我自己为了失败3次后告警使用
                Log_Util.warning("重连异常");
                return;
            }
            if (!client.isConnected()) {
                System.out.println("***** client to connect *****");
                try {
                    //这个是60秒后重连
                    Thread.sleep(60000);
                    client.reconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                i--;
            }
            if (client.isConnected()) {
                System.out.println("***** connect success *****");
                return;
            }
        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("deliveryComplete---------" + token.isComplete());
    }

    public void messageArrived(String topic, MqttMessage message) throws Exception {
//        // subscribe后得到的消息会执行到这里面
//        System.out.println("接收消息主题 : " + topic);
//        System.out.println("接收消息Qos : " + message.getQos());
//        String info = new String(message.getPayload());
//        System.out.println("接收消息内容 : " + info);
//
//        JSONObject jsonObject = JSONObject.parseObject(info);
//        float temp = jsonObject.getFloat("temp");
//        float humi = jsonObject.getFloat("humi");
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                JDBC_Util.upData_Now(temp,humi);
//            }
//        }).start();
    }
}