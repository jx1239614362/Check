package com.example.check.MySQL;

import android.util.Log;

import java.sql.*;

public class DBUtils {
    private static String driver="com.mysql.jdbc.Driver";
    private static String user="root";
    private static String password = "root";
    private static String url = "jdbc:mysql://192.168.43.5:3306/check?useSSL=false&characterEncoding=utf-8";
    //private static String table = "user";
    public static Connection getConnect(){
        Connection connection = null;
        try {
            Class.forName(driver);
            connection= DriverManager.getConnection(url, user, password);
            Log.e("数据库连接","success");
        } catch (Exception e) {
            Log.e("数据库连接","failure");
            e.printStackTrace();
        }
        return connection;
    }


}
