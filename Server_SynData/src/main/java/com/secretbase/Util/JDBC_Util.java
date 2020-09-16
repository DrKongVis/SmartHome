package com.secretbase.Util;

import com.secretbase.Job.Now_TH_Data;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JDBC_Util {
    // MySQL 8.0 以上版本 - JDBC 驱动名及数据库 URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://192.168.31.146:3306/Smart_Home";


    // 数据库的用户名与密码，需要根据自己的设置
    static final String USER = "root";
    static final String PASS = "4682327";
    private static Connection conn = null;

    private static float tempValue;
    private static float humiValue;

    public static void Init_Jdbc(){
        try {
            if(conn == null || conn.isClosed()){
                conn = DriverManager.getConnection(DB_URL, USER, PASS);
            }
            Log_Util.info("connected："+conn.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void upData_Now(float temp,float humi) {
        String sql = "UPDATE TH_Table_Now set tempValue=?,humiValue=? where id = 1";
        //预编译
        PreparedStatement ptmt = null; //预编译SQL，减少sql执行
        try {
            ptmt = conn.prepareStatement(sql);
            //传参
            ptmt.setFloat(1, temp);
            ptmt.setFloat(2, humi);

            ptmt.execute();
            Log_Util.info("数据库更新完成");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * 将当前温湿度同步到数据库
     */
    public static void sync(){
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id,tempValue,humiValue FROM TH_Table_24H");

            List<Now_TH_Data> list = new ArrayList<Now_TH_Data>();
            while (rs.next()) {
                //舍弃多余位
                if(rs.getInt("id") == 24){
                    break;
                }
                Now_TH_Data data = new Now_TH_Data();
                data.setId(rs.getInt("id")+1);                  //时间增加一小时
                data.setTempValue(rs.getFloat("tempValue"));
                data.setHumiValue(rs.getFloat("humiValue"));
                System.out.println("已获取"+data);
                list.add(data);
            }


            //将当前实时温湿度插入24H_Table 的id=1位置
            selectTH_Now();                       //获取当前温湿度
            System.out.println(tempValue);
            System.out.println(humiValue);

            upDate(tempValue,humiValue,1);

            //遍历列表---------------------------------------------------------------进行插入
            for(Now_TH_Data data : list){
                upDate(data.getTempValue(),data.getHumiValue(),data.getId());
            }
            //遍历列表---------------------------------------------------------------进行插入

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void upDate(float temp,float humi,int id){
        String sql = "UPDATE TH_Table_24H set tempValue=?,humiValue=? where id = ?";
            //预编译
            PreparedStatement ptmt = null; //预编译SQL，减少sql执行
            try {
                ptmt = conn.prepareStatement(sql);
                //传参
                ptmt.setFloat(1, temp);
                ptmt.setFloat(2, humi);
                ptmt.setInt(3, id);
                ptmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }

    private static void selectTH_Now() {
        System.out.println("连接数据库...");
        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT tempValue,humiValue FROM TH_Table_Now");

            while (rs.next()) {
                tempValue = rs.getFloat("tempValue");
                humiValue = rs.getFloat("humiValue");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
