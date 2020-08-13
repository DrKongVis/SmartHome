package com.example.smart_home_terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.example.smart_home_terminal.AAChartCoreLib.AAChartConfiger.AAChartModel;
import com.example.smart_home_terminal.AAChartCoreLib.AAChartConfiger.AAChartView;
import com.example.smart_home_terminal.AAChartCoreLib.AAChartConfiger.AAOptionsConstructor;
import com.example.smart_home_terminal.AAChartCoreLib.AAChartConfiger.AASeriesElement;
import com.example.smart_home_terminal.AAChartCoreLib.AAChartEnum.AAChartSymbolStyleType;
import com.example.smart_home_terminal.AAChartCoreLib.AAChartEnum.AAChartType;
import com.example.smart_home_terminal.AAChartCoreLib.AAOptionsModel.AAOptions;
import com.example.smart_home_terminal.AAChartCoreLib.AAOptionsModel.AAStyle;
import com.example.smart_home_terminal.AAChartCoreLib.AAOptionsModel.AATooltip;
import com.example.smart_home_terminal.MQTT.MyMqttClient;
import com.example.smart_home_terminal.TaskList.TaskAdapter;
import com.example.smart_home_terminal.TaskList.TaskClass;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    private static final int BNT_TEMP_UP = 1;           //更新温度按钮UI
    private static final int BNT_RH_UP = 2;             //更新湿度按钮UI

    private static final int IMG_LIGHT_UP = 3;          //更新灯按钮UI
    private static final int IMG_FAN_UP = 4;            //更新风扇按钮UI
    private static final int IMG_DOOR_UP = 5;           //更新门按钮UI

    private static final int UP_DATA_UI_ALL = 6;        //更新所有UI
    private static final int REFRESH_LIST = 7;          //刷新ListView

    private static final String SB_SYS_DEBUG = "系统日志";

    double T_data = 0;
    double H_data = 0;

    private boolean isTemp = true;

    private AAChartView aaChartView_TH;

    private MyMqttClient client;

    private TextView showTemperature;
    private TextView showHumidity;
    private ImageView showWeather;
    private ImageView power;
    private Button choiceTemperature;
    private Button choiceHumidity;
    private AAChartView AAChartViewTH;
    private ImageView bntLight;
    private ImageView bntFan;
    private ImageView bntDoor;
    private ImageView settingTimer;
    private ListView taskList;
    private TextView terminalInfo;
    private LinearLayout layout_light;
    private LinearLayout layout_fan;

    private StringBuilder sb = new StringBuilder();

    // MySQL 8.0 以上版本 - JDBC 驱动名及数据库 URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    //static final String DB_URL = "jdbc:mysql://182.92.150.158:3306/Smart_Home";
    static final String DB_URL = "jdbc:mysql://192.168.1.105:3306/Smart_Home";

    // 数据库的用户名与密码，需要根据自己的设置
    static final String USER = "root";
    static final String PASS = "4682327";
    private static Connection conn;

    private Object[] tempValue = new Object[24];
    private Object[] humiValue = new Object[24];

    private ListView listView;
    private List<TaskClass> tasks = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //隐藏标题栏
        getSupportActionBar().hide();

        //初始化连接
        ConnectInit();

        //初始化控件
        findViews();

        //初始化图表
        CreateChart(1);
        upDateChart(true,2000);

        //初始化任务ListView
        listView = findViewById(R.id.task_list);
        SearchTask();

        //设置长按点击事件
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                TaskClass task = tasks.get(position);
                tasks.remove(position);
                //在数据库内删除
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //sql
                            String sql = "DELETE from Task where Taskname = ?";

                            //预编译
                            PreparedStatement ptmt = conn.prepareStatement(sql); //预编译SQL，减少sql执行

                            //传参
                            ptmt.setString(1, task.getName());

                            ptmt.execute();
                            PUSH_Message_To_UIHandler(REFRESH_LIST);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                //Web端开始执行任务时再在数据库中检查，如果被删除则中断任务
                return false;
            }
        });
    }

    private void SearchTask() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT Taskname,startTime,endTime from Task");
                    tasks.clear();
                    System.out.println("tasksCleared"+tasks.size()+"form:"+Thread.currentThread().getName());
                    while (rs.next()){
                        String name = rs.getString("Taskname");
                        String startTime = rs.getString("startTime");
                        String endTime = rs.getString("endTime");
                        TaskClass task = new TaskClass(name,startTime,endTime);
                        tasks.add(task);
                        System.out.println("tasksSize:"+tasks.size()+"\n"+task);
                    }
                    PUSH_Message_To_UIHandler(REFRESH_LIST);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void PUSH_Message_To_UIHandler(int what){
        Message message = new Message();
        message.what = what;
        handler_UI_UpData.sendMessage(message);
    }

    private void ConnectInit(){
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("连接数据库...");
                    conn = DriverManager.getConnection(DB_URL, USER, PASS);
                    System.out.println("连接成功！！！");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        //连接MQTT服务器
        client = new MyMqttClient();
        Boolean isConnect = client.start("Smart_Home_Client"+new Random().nextInt()+"",Data_handler,BackHandler);
        Toast.makeText(MainActivity.this,"连接状态："+isConnect,Toast.LENGTH_SHORT).show();
    }
    private void UPDateUI(){
        PUSH_Message_To_UIHandler(UP_DATA_UI_ALL);
        SearchTask();
    }

    /**
     *
     * @param isT       判断是温度还是湿度
     */
    private void upDateChart(final boolean isT,int delay) {
        System.out.println("进入：upDateChart");
        final Handler mHandler = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                AASeriesElement[] aaSeriesElementsArr = configureChartSeriesArray_TH(isT);
                aaChartView_TH.aa_onlyRefreshTheChartDataWithChartOptionsSeriesArray(aaSeriesElementsArr);
                //每隔15分钟循环执行run方法
                //mHandler.postDelayed(this, 900000);
                //mHandler.postDelayed(this, 1000);
            }
        };

        mHandler.postDelayed(r, delay);//延时2000毫秒
        System.out.println("--------------");
    }

    private AASeriesElement[] configureChartSeriesArray_TH(boolean isTemp) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT id,tempValue,humiValue FROM TH_Table_24H");
                        int i=23;
                        while (rs.next()) {
                            tempValue[i] = rs.getFloat("tempValue");
                            humiValue[i] = rs.getFloat("humiValue");
                            i--;
                        }
                        System.out.println("已存入温度："+ Arrays.toString(tempValue));
                        System.out.println("已存入湿度："+ Arrays.toString(humiValue));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            Thread.sleep(1000);
            System.out.println("存入数组");
        } catch (Exception e) {
            e.printStackTrace();
        }


        //存入数据更新数组
        AASeriesElement[] aaSeriesElementsArr;
        if(isTemp){
            aaSeriesElementsArr = new AASeriesElement[]{
                    new AASeriesElement()
                            .name("温度")
                            .data(tempValue),
            };
        }else{
            aaSeriesElementsArr = new AASeriesElement[]{
                    new AASeriesElement()
                            .name("湿度")
                            .data(humiValue),
            };
        }
        return aaSeriesElementsArr;
    }

    public void Main_Click(View view) {
        switch (view.getId()){
            //切换温湿度图表
            case R.id.choice_temperature:{
                if(isTemp == true){
                    return;
                }
                Message message = new Message();
                message.what = BNT_TEMP_UP;
                handler_UI_UpData.sendMessage(message);
            }break;
            case R.id.choice_humidity:{
                if(isTemp == false){
                    return;
                }
                Message message = new Message();
                message.what = BNT_RH_UP;
                handler_UI_UpData.sendMessage(message);
            }break;
            case R.id.l_bnt_light:{
                Message message = new Message();
                message.what = IMG_LIGHT_UP;
                handler_UI_UpData.sendMessage(message);
            }break;
            case R.id.l_bnt_fan:{
                Message message = new Message();
                message.what = IMG_FAN_UP;
                handler_UI_UpData.sendMessage(message);
            }break;
            case R.id.l_bnt_door:{
                Message message = new Message();
                message.what = IMG_DOOR_UP;
                handler_UI_UpData.sendMessage(message);
            }break;
            case R.id.l_setting_timer:{
                Intent intent = new Intent(MainActivity.this,TimerTaskActivity.class);
                startActivityForResult(intent,1);
            }break;
            case R.id.l_refresh:{
                UPDateUI();
            }break;
            case R.id.l_show_weather:{

            }
        }
    }

    /*public static String doGet(String httpurl) {
        HttpURLConnection connection = null;
        InputStream is = null;
        BufferedReader br = null;
        String result = null;// 返回结果字符串
        try {
            // 创建远程url连接对象
            URL url = new URL(httpurl);
            // 通过远程url连接对象打开一个连接，强转成httpURLConnection类
            connection = (HttpURLConnection) url.openConnection();
            // 设置连接方式：get
            connection.setRequestMethod("GET");
            // 设置连接主机服务器的超时时间：15000毫秒
            connection.setConnectTimeout(15000);
            // 设置读取远程返回的数据时间：60000毫秒
            connection.setReadTimeout(60000);
            // 发送请求
            connection.connect();
            // 通过connection连接，获取输入流
            if (connection.getResponseCode() == 200) {
                is = connection.getInputStream();
                // 封装输入流is，并指定字符集
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                // 存放数据
                StringBuffer sbf = new StringBuffer();
                String temp = null;
                while ((temp = br.readLine()) != null) {
                    sbf.append(temp);
                    sbf.append("\r\n");
                }
                result = sbf.toString();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            connection.disconnect();// 关闭远程连接
        }

        return result;
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Toast.makeText(MainActivity.this,"回调",Toast.LENGTH_SHORT).show();
        switch (requestCode) {
            case 1: {
                if (resultCode == RESULT_OK) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            /*
                            //Toast.makeText(MainActivity.this,"OK",Toast.LENGTH_SHORT).show();
                            // 2.创建数据报，包含发送的数据信息
                            String UDPData = "StartTask";
                            byte[] bytes = UDPData.getBytes();
                            try {
                                DatagramPacket packet = new DatagramPacket(bytes,
                                        bytes.length,
                                        InetAddress.getByName("182.92.150.158"), 12345);
                                // 3.创建DatagramSocket对象
                                DatagramSocket socket = null;
                                socket = new DatagramSocket();
                                // 4.向服务器端发送数据报
                                socket.send(packet);
                                System.out.println("UDP发送完成");
                                //Toast.makeText(MainActivity.this,"发送完成"+packet.getAddress(),Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }*/

                            try {
                                //String serverName = "192.168.31.145";
                                String serverName = "182.92.150.158";
                                int port = 1234;
                                System.out.println("连接到主机：" + serverName + " ，端口号：" + port);
                                Socket client = new Socket(serverName, port);
                                System.out.println("远程主机地址：" + client.getRemoteSocketAddress());
                                OutputStream outToServer = client.getOutputStream();
                                DataOutputStream out = new DataOutputStream(outToServer);

                                out.writeUTF("StartTask");
                                InputStream inFromServer = client.getInputStream();
                                DataInputStream in = new DataInputStream(inFromServer);
                                System.out.println("-------服务器响应： " + in.readUTF());
                                client.close();
                            }catch (Exception e){
                                e.printStackTrace();
                            }

                        }
                    }).start();
                }
                UPDateUI();
            }
            break;
        }
    }

    /**
     * 获取数据
     */
    public Handler Data_handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what == 10){
                String str = (String) msg.obj;
                System.out.println(str);
                addStringBuilder("来自远端mqtt",str+"\n");
                Log.e("mydebug", "handleMessage: "+str);
                JSONObject jsonObject = JSONObject.parseObject(str);
                T_data = jsonObject.getFloat("temp").floatValue();
                H_data = jsonObject.getFloat("humi").floatValue();

                showTemperature.setText(T_data+"");
                showHumidity.setText(H_data+"");
            }
        }
    };

    /**
     * 加入字符串构造器
     */
    private void addStringBuilder(String header,String info){
        if(sb.length() > 1024){
            sb.delete(0,512);
        }
        sb.append(header+":"+info);
        terminalInfo.setText(sb.toString());
    }


    private boolean fin_is_auto = true;
    private boolean light_is_auto = true;
    private void findViews() {
        showTemperature = (TextView)findViewById( R.id.show_temperature );
        showHumidity = (TextView)findViewById( R.id.show_humidity );
        showWeather = (ImageView)findViewById( R.id.show_weather );
        power = (ImageView)findViewById( R.id.power );
        choiceTemperature = (Button)findViewById( R.id.choice_temperature );
        choiceHumidity = (Button)findViewById( R.id.choice_humidity );
        AAChartViewTH = (AAChartView)findViewById( R.id.AAChartView_TH );
        bntLight = (ImageView)findViewById( R.id.bnt_light );
        bntFan = (ImageView)findViewById( R.id.bnt_fan );
        bntDoor = (ImageView)findViewById( R.id.bnt_door );
        settingTimer = (ImageView)findViewById( R.id.setting_timer );
        taskList = (ListView)findViewById( R.id.task_list );
        terminalInfo = (TextView)findViewById( R.id.terminal_info );
        layout_light = (LinearLayout)findViewById(R.id.l_bnt_light);
        layout_fan = (LinearLayout)findViewById(R.id.l_bnt_fan);

        layout_light.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                light_is_auto = !light_is_auto;
                if(light_is_auto){
                    client.publishMessage("cmd","fin_auto",1);
                }else{
                    client.publishMessage("cmd","fin_auto_no",1);
                    client.publishMessage("cmd","Light_Off",1);
                }
                return true;
            }
        });
        layout_fan.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(fin_is_auto) {
                    client.publishMessage("cmd","light_auto",1);
                }else{
                    client.publishMessage("cmd","light_auto_no",1);
                    client.publishMessage("cmd","Light_Off",1);
                }
                return true;
            }
        });
    }

    boolean light_isWork = false;
    boolean fan_isWork = false;
    boolean door_isWork = false;
    Handler handler_UI_UpData = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case BNT_TEMP_UP:{
                    choiceHumidity.setBackgroundResource(R.drawable.background_cycle3);
                    choiceTemperature.setBackgroundResource(R.drawable.background_cycle3_green);
                    isTemp = true;

                    upDateChart(true,1000);
                    CreateChart(1);
                }break;
                case BNT_RH_UP:{
                    choiceTemperature.setBackgroundResource(R.drawable.background_cycle3);
                    choiceHumidity.setBackgroundResource(R.drawable.background_cycle3_green);
                    isTemp = false;
                    upDateChart(false,1000);
                    CreateChart(2);
                }break;
                case IMG_LIGHT_UP:{
                    if(!light_is_auto){
                        if(light_isWork){
                            client.publishMessage("cmd","Light_On",1);
                        }else{
                            client.publishMessage("cmd","Light_Off",1);
                        }
                    }else{
                        Toast.makeText(MainActivity.this,"请先长按脱离自动模式",Toast.LENGTH_SHORT).show();
                    }
                }break;
                case IMG_FAN_UP:{
                    if(!fin_is_auto){
                        if(fan_isWork){
                            client.publishMessage("cmd","Fan_On",1);
                        }else{
                            client.publishMessage("cmd","Fan_Off",1);
                        }
                    }else{
                        Toast.makeText(MainActivity.this,"请先长按脱离自动模式",Toast.LENGTH_SHORT).show();
                    }
                }break;
                case IMG_DOOR_UP:{
                    if(door_isWork){
                        client.publishMessage("cmd","Door_On",1);
                    }else{
                        client.publishMessage("cmd","Door_Off",1);
                    }
                }break;
                case UP_DATA_UI_ALL:{
                    //更新温湿度图表
                    if(isTemp){
                        upDateChart(true,1000);
                        CreateChart(1);
                    }else{
                        upDateChart(false,1000);
                        CreateChart(2);
                    }
                    //更新任务
                }break;
                case REFRESH_LIST:{
                    TaskAdapter adapter = new TaskAdapter(tasks,MainActivity.this);
                    listView.setAdapter(adapter);
                }
            }
        }
    };

    private Handler BackHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 10:{
                    String info = (String) msg.obj;
                    System.out.println("back:"+info);
                    JSONObject jsonObject = JSONObject.parseObject(info);
                    int fan = jsonObject.getInteger("fan").intValue();
                    int light = jsonObject.getInteger("light").intValue();
                    int curtain = jsonObject.getInteger("curtain").intValue();
                    int Mode_Fan = jsonObject.getInteger("Mode_Fan").intValue();
                    int Mode_Light = jsonObject.getInteger("Mode_Light").intValue();
                    if(fan == 1){
                        //关
                        bntFan.setImageResource(R.drawable.ct_fan0);
                        fan_isWork = false;
                    }else{
                        //开
                        bntFan.setImageResource(R.drawable.ct_fan1);
                        fan_isWork = true;
                    }

                    if(light == 0){
                        //关
                        bntLight.setImageResource(R.drawable.ct_light0);
                        light_isWork = false;
                    }else{
                        //开
                        bntLight.setImageResource(R.drawable.ct_light1);
                        light_isWork = true;
                    }

                    if(curtain == 0){
                        //关
                        door_isWork = false;
                        bntDoor.setImageResource(R.drawable.ct_door2);
                    }else{
                        //开
                        door_isWork = true;
                        bntDoor.setImageResource(R.drawable.ct_door1);
                    }

                    if(Mode_Fan == 0){
                        //自动
                        bntFan.setImageResource(R.drawable.auto_fan);
                        fin_is_auto = true;
                    }else{
                        //手动
                        bntFan.setImageResource(R.drawable.ct_fan0);
                        fan_isWork = false;
                        fin_is_auto = false;
                    }

                    if(Mode_Light == 0){
                        //自动
                        bntLight.setImageResource(R.drawable.auto_light);
                        light_is_auto = true;
                    }else{
                        //手动
                        bntLight.setImageResource(R.drawable.ct_light0);
                        light_isWork = false;
                        light_is_auto = false;
                    }
                }
            }
        }
    };


    /**
     *
     * @param flag   1->温度曲线图       2->湿度曲线图
     */
    public void CreateChart(int flag){
        //使用Model来构建图表
        AAOptions aaOptions_TH;
        if(flag == 1){
            aaOptions_TH = Get_TEMP_AAOptions();
        }else{
            aaOptions_TH = Get_RH_AAOptions();
        }

        //System.out.println(aaOptions_TH);
        //获得布局
        aaChartView_TH = findViewById(R.id.AAChartView_TH);
        //加载图表
        aaChartView_TH.aa_drawChartWithChartOptions(aaOptions_TH);
    }

    /**
     * 构建AAChartModel
     * @return
     */
    AAOptions Get_TEMP_AAOptions() {
        AAChartModel aaChartModel = new AAChartModel()
                .chartType(AAChartType.Area)//图形类型
                .title("")//图表主标题
                .markerSymbolStyle(AAChartSymbolStyleType.BorderBlank)//折线连接点样式为外边缘空白
                .dataLabelsEnabled(false)
                .categories(new String[]{
                        "24","23","22","21","20","19","18","17","16","15","14",
                        "13","12","11","10","09","08","07","06","05","04","03",
                        "02","01"
                        })
                .series(new AASeriesElement[]{
                        new AASeriesElement()
                                .name("最近24小时温度")
                                .lineWidth(3f)
                                .color("#00CD66"/*绿色*/)
                                .fillOpacity(0.5f)
                                .data(new Object[]{
                                1.51, 6.7, 0.94, 1.44, 1.6, 1.63, 1.56, 1.91, 2.45, 3.87, 3.24, 4.90, 4.61, 4.10,
                                4.17, 3.85, 4.17, 3.46, 3.46, 3.55, 3.50, 4.13, 2.58, 2.28})
                });

        AATooltip aaTooltip = new AATooltip()
                .useHTML(true)
                .formatter(" function () {\n" +
                        "        return ' 🌕 🌖 🌗 🌘 🌑 🌒 🌓 🌔 <br/> '\n" +
                        "        +  this.x\n" +
                        "        + ' </b> 小时前的温度为 <b> '\n" +
                        "        +  this.y\n" +
                        "        + ' </b> ℃ ';\n" +
                        "        }")
                .valueDecimals(2)//设置取值精确到小数点后几位//设置取值精确到小数点后几位
                .backgroundColor("#000000")
                .borderColor("#000000")
                .style(new AAStyle()
                        .color("#FFD700")
                        .fontSize(12.f)
                );
        AAOptions aaOptions = AAOptionsConstructor.configureChartOptions(aaChartModel);
        aaOptions.tooltip(aaTooltip);
        return aaOptions;
    }
    AAOptions Get_RH_AAOptions() {
        AAChartModel aaChartModel = new AAChartModel()
                .chartType(AAChartType.Area)//图形类型
                .title("")//图表主标题
                .markerSymbolStyle(AAChartSymbolStyleType.BorderBlank)//折线连接点样式为外边缘空白
                .dataLabelsEnabled(false)
                .categories(new String[]{
                        "24","23","22","21","20","19","18","17","16","15","14",
                        "13","12","11","10","9","8","7","6","5","4","3",
                        "2","1"
                })
                .series(new AASeriesElement[]{
                        new AASeriesElement()
                                .name("最近24小时湿度")
                                .lineWidth(3f)
                                .color("#4876FF"/*绿色*/)
                                .fillOpacity(0.5f)
                                .data(new Object[]{
                                1.51, 6.7, 0.94, 1.44, 1.6, 1.63, 1.56, 1.91, 2.45, 3.87, 3.24, 4.90, 4.61, 4.10,
                                4.17, 3.85, 4.17, 3.46, 3.46, 3.55, 3.50, 4.13, 2.58, 2.28})
                });

        AATooltip aaTooltip = new AATooltip()
                .useHTML(true)
                .formatter(" function () {\n" +
                        "        return ' 🌕 🌖 🌗 🌘 🌑 🌒 🌓 🌔 <br/> '\n" +
                        "        +  this.x\n" +
                        "        + ' </b> 小时前的湿度为 <b> '\n" +
                        "        +  this.y\n" +
                        "        + ' </b> ℃ ';\n" +
                        "        }")
                .valueDecimals(2)//设置取值精确到小数点后几位//设置取值精确到小数点后几位
                .backgroundColor("#000000")
                .borderColor("#000000")
                .style(new AAStyle()
                        .color("#FFD700")
                        .fontSize(12.f)
                );
        AAOptions aaOptions = AAOptionsConstructor.configureChartOptions(aaChartModel);
        aaOptions.tooltip(aaTooltip);
        return aaOptions;
    }
}
