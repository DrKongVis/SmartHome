package secret.MyUtil;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JDBC_Util {
    // MySQL 8.0 以上版本 - JDBC 驱动名及数据库 URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://192.168.31.146:3306/Smart_Home";


    // 数据库的用户名与密码，需要根据自己的设置
    static final String USER = "root";
    static final String PASS = "4682327";
    public static Connection conn = null;

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
    public static void del_task(String name){
        String sql = "delete from Task where Taskname = ?";
        PreparedStatement ptmt = null;
        try {
            ptmt = conn.prepareStatement(sql);
            ptmt.setString(1, name);
            ptmt.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
