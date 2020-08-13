package com.example.smart_home_terminal;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.spec.ECField;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimerTaskActivity extends AppCompatActivity {

    private EditText inputTaskName;
    private RadioButton radioLight;
    private RadioButton radioFan;
    private RadioButton radioCycle;
    private RadioButton radioPower;
    private RadioButton radioOne;
    private Button tackStartTime;
    private TextView showStartTime;
    private Button tackEndTime;
    private TextView showEndTime;
    private Button taskCommit;

    // MySQL 8.0 以上版本 - JDBC 驱动名及数据库 URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://182.92.150.158:3306/Smart_Home";


    // 数据库的用户名与密码，需要根据自己的设置
    static final String USER = "root";
    static final String PASS = "4682327";
    private static Connection conn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_task);

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setTitle("定时任务");
        findViews();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("连接数据库...");
                    conn = DriverManager.getConnection(DB_URL, USER, PASS);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                //Toast.makeText(TimerTaskActivity.this,"数据库连接",Toast.LENGTH_SHORT).show();
            }
        }).start();

    }

    private void findViews() {
        inputTaskName = (EditText)findViewById( R.id.input_task_name );
        radioLight = (RadioButton)findViewById( R.id.radio_light );
        radioFan = (RadioButton)findViewById( R.id.radio_fan );
        radioPower = (RadioButton) findViewById( R.id.radio_power );
        tackStartTime = (Button)findViewById( R.id.tack_start_time );
        showStartTime = (TextView)findViewById( R.id.show_start_time );
        tackEndTime = (Button)findViewById( R.id.tack_end_time );
        showEndTime = (TextView)findViewById( R.id.show_end_time );
        taskCommit = (Button)findViewById( R.id.task_commit );
    }


    String startTime;
    String endTime;
    public void Task_Click(View view) throws Exception {
        switch (view.getId()){
            case R.id.tack_start_time:{
                //时间选择器
                TimePickerView pvTime = new TimePickerBuilder(TimerTaskActivity.this, new OnTimeSelectListener() {
                    @Override
                    public void onTimeSelect(Date date, View v) {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        startTime = dateFormat.format(date);
                        showStartTime.setText(startTime);
                    }
                }).setType(new boolean[]{true, true, true, true, true, true})// 默认全部显示
                        .setLabel("年","月","日","时","分","秒")//默认设置为年月日时分秒
                        .build();
                pvTime.show();
            }break;

            case R.id.tack_end_time:{
                //时间选择器
                TimePickerView pvTime = new TimePickerBuilder(TimerTaskActivity.this, new OnTimeSelectListener() {
                    @Override
                    public void onTimeSelect(Date date, View v) {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        endTime = dateFormat.format(date);
                        showEndTime.setText(endTime);
                    }
                }).setType(new boolean[]{true, true, true, true, true, true})// 默认全部显示
                        .setLabel("年","月","日","时","分","秒")//默认设置为年月日时分秒
                        .build();
                pvTime.show();
            }break;

            case R.id.task_commit:{

                //存储到数据库
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                        String sql = "INSERT INTO Task (Taskname,Tasktype,startTime,endTime)"
                                + "values(?,?,?,?)";
//        String sql = "INSERT INTO TH_Table_24H (tempValue,humiValue)"
//        +"values(?,?)";
                        //预编译
                        PreparedStatement ptmt = conn.prepareStatement(sql); //预编译SQL，减少sql执行

                        String name = inputTaskName.getText().toString();
                        //0：风扇   1：灯   3：电源
                        int taskType;
                        if(radioLight.isChecked()){
                            taskType = 1;
                        }else if(radioFan.isChecked()){
                            taskType = 0;
                        }else if(radioPower.isChecked()){
                            taskType = 3;
                        }else{
                            Toast.makeText(TimerTaskActivity.this,"请选择任务类型",Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //传参
                        ptmt.setString(1, name);
                        ptmt.setInt(2, taskType);
                        ptmt.setString(3, startTime);
                        ptmt.setString(4, endTime);

                        ptmt.execute();
                        //向后端发送请求
                        setResult(RESULT_OK);
                        finish();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    }
                }).start();

                Thread.sleep(200);



            }break;
        }
    }
/*
    private void add() throws SQLException {
        //sql
        String sql = "INSERT INTO Task (name,type,isCycle,startTime,endTime)"
                +"values(?,?,?,?,?)";
//        String sql = "INSERT INTO TH_Table_24H (tempValue,humiValue)"
//        +"values(?,?)";
        //预编译
        PreparedStatement ptmt = null; //预编译SQL，减少sql执行
        try {
            ptmt = conn.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String name = inputTaskName.getText().toString();
        int task;
        //0：风扇 ------- 1：灯 ------- 2：电源
        if(radioFan.isChecked()){
            task = 0;
        }else if(radioLight.isChecked()){
            task = 1;
        }else if(radioPower.isChecked()){
            task = 2;
        }else{
            Toast.makeText(TimerTaskActivity.this,"请选择任务",Toast.LENGTH_SHORT).show();
            return;
        }

        int type;
        //0：不循环 ------- 1：循环
        if(radioOne.isChecked()){
            type = 0;
        }else if(radioCycle.isChecked()){
            type = 1;
        }else {
            Toast.makeText(TimerTaskActivity.this,"请选择任务类型",Toast.LENGTH_SHORT).show();
            return;
        }


        //传参
        ptmt.setString(1, name);
        ptmt.setInt(2, task);
        ptmt.setInt(3, type);
        ptmt.setString(4, startTime);
        ptmt.setString(5, endTime);

        ptmt.execute();
    }*/
}
