package com.example.check.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.check.MySQL.User;
import com.example.check.R;
import com.example.check.MySQL.DBUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class LoginActivity extends Activity {
    private Context context;
    private String username="";
    private String passward="";
    private Button loginButton;
    private static DBUtils DB;
    private static final int TEST_USER_SELECT = 1;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String user;
            switch (msg.what){
                case TEST_USER_SELECT:
                    User test = (User) msg.obj;
                    user = test.getUser();
                    System.out.println("***********");
                    System.out.println("User:"+user);
                    System.out.println("***********");
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        this.context = this;
        loginButton = findViewById(R.id.btn_login);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText userEdit = findViewById(R.id.et_account);
                EditText passEdit = findViewById(R.id.et_password);
                username = userEdit.getText().toString();
                passward = passEdit.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Connection conn = null;
                        User user = new User();
                        conn = (Connection) DBUtils.getConnect();
                        String sql = "select * from user where username = '" + username + "' and passward = '" + passward + "'";
                        Statement st;
                        try {
                            st = (Statement) conn.createStatement();
                            ResultSet rs = st.executeQuery(sql);
                            while (rs.next()){
                                user.setUsername(rs.getString("username"));
                                user.setPassward(rs.getString("passward"));
                            }
                            Message msg = new Message();
                            msg.what =TEST_USER_SELECT;
                            msg.obj = user;
                            handler.sendMessage(msg);
                            if(user.getUsername().equals(username) && user.getPassward().equals(passward)){
                                Intent intent = new Intent(LoginActivity.this, BaiduMapActivity.class);
                                intent.putExtra("username",user.getUsername()).putExtra("passward",user.getPassward());
                                startActivity(intent);
                            }else{
                                Looper.prepare();
                                Toast.makeText(context,"用户或密码错误",Toast.LENGTH_LONG).show();
                                Looper.loop();
                            }
                            st.close();
                            conn.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }



}
