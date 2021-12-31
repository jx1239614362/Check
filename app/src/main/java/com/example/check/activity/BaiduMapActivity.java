package com.example.check.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;
import com.example.check.CleanLeakUtils;
import com.example.check.MyOrientationListener;
import com.example.check.MySQL.DBUtils;
import com.example.check.MySQL.User;
import com.example.check.R;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BaiduMapActivity extends Activity {
    private static final int BAIDU_READ_PHONE_STATE = 100;

    private MapView mMapView = null;
    private BaiduMap mBaiduMap;
    private LocationClient mlocationClient;
    private MylocationListener mlistener;
    private Context context;
    private double mLatitude;
    private double mLongitude;
    private float mCurrentX;
    private Button mGetMylocationBN;
    private Button mGetCheck;
    private BDLocation location = null;
    PopupMenu popup = null;
    String username = "";
    String passward="";
    private static final int TEST_USER_SELECT = 1;
    private MyOrientationListener myOrientationListener;
    Intent intent;
    //定位图层显示方式
    private MyLocationConfiguration.LocationMode locationMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.baidumap_activity);
        this.context = this;
        intent=getIntent();
        username = intent.getStringExtra("username");
        passward = intent.getStringExtra("passward");
        mGetCheck =(Button) findViewById(R.id.id_bn_getCheck);
        initView();
        //判断是否为Android 6.0 以上的系统版本，如果是，需要动态添加权限
        if (Build.VERSION.SDK_INT >= 23) {
            showLocMap();
        } else {
            initLocation();//initLocation为定位方法
        }
    }
    private void initView() {
        mMapView = (MapView) findViewById(R.id.id_bmapView);
        mBaiduMap = mMapView.getMap();
        //根据给定增量缩放地图级别
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(18.0f);
        mBaiduMap.setMapStatus(msu);
        MapStatus mMapStatus;//地图当前状态
        MapStatusUpdate mMapStatusUpdate;//地图将要变化成的状态
        mMapStatus = new MapStatus.Builder().overlook(-45).build();
        mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        mBaiduMap.setMapStatus(mMapStatusUpdate);
        mGetMylocationBN = (Button) findViewById(R.id.id_bn_getMyLocation);
        mGetMylocationBN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMyLocation();
            }
        });

    }
    private void initLocation() {
        //Toast.makeText(context, "this is initLocation function",Toast.LENGTH_SHORT).show();
        locationMode = MyLocationConfiguration.LocationMode.NORMAL;
        //定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
        mlocationClient = new LocationClient(this);
        mlistener = new MylocationListener();
        //注册监听器
        mlocationClient.registerLocationListener(mlistener);

        //配置定位SDK各配置参数，比如定位模式、定位时间间隔、坐标系类型等
        LocationClientOption mOption = new LocationClientOption();
        //设置坐标类型
        mOption.setCoorType("bd09ll");
        //设置是否需要地址信息，默认为无地址
        mOption.setIsNeedAddress(true);

        //在网络定位时，是否需要设备方向 true:需要 ; false:不需要
        mOption.setNeedDeviceDirect(true);
        //设置是否打开gps进行定位
        mOption.setOpenGps(true);
        //设置扫描间隔，单位是毫秒，当<1000(1s)时，定时定位无效
        int span = 1000;
        mOption.setScanSpan(span);
        //设置 LocationClientOption
        mlocationClient.setLocOption(mOption);

        myOrientationListener = new MyOrientationListener(context);
        //通过接口回调来实现实时方向的改变
        myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });

    }
    @Override
    protected void onStart() {
        super.onStart();
        //开启定位
        mBaiduMap.setMyLocationEnabled(true);

        mlocationClient.start();

        myOrientationListener.start();
        mGetCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Check();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        //停止定位
        mBaiduMap.setMyLocationEnabled(false);
        mlocationClient.stop();
        myOrientationListener.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        CleanLeakUtils.fixInputMethodManagerLeak(BaiduMapActivity.this);
        super.onDestroy();
        mMapView.onDestroy();
    }

    public void getMyLocation() {
        LatLng latLng = new LatLng(mLatitude, mLongitude);
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.setMapStatus(msu);
    }
    public void Check(){
        final String mycity = updateWithNewLocation(location);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        final String nowdate = simpleDateFormat.format(date);
        Toast.makeText(context,mycity+":"+nowdate,Toast.LENGTH_SHORT).show();
        Toast.makeText(context,"打卡成功",Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Connection conn = null;
                int u = 0;
                conn = (Connection) DBUtils.getConnect();
                String sql = "update user set address='"+mycity+"',date='"+nowdate+"' where username='"+username+"' and passward ='"+passward+"'";
                PreparedStatement pst;
                try {
                    pst = (PreparedStatement) conn.prepareStatement(sql);
                    u = pst.executeUpdate();

                    pst.close();
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
    //根据经纬度判断
    public void onPopupMenuClick(View v) {
        // 创建PopupMenu对象
        popup = new PopupMenu(this, v);
        // 将R.menu.menu_main菜单资源加载到popup菜单中
        getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());
        // 为popup菜单的菜单项单击事件绑定事件监听器
        popup.setOnMenuItemClickListener(
                new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.id_map_common:
                                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                                break;
                            case R.id.id_map_site:
                                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                                break;
                            case R.id.id_map_traffic:
                                if (mBaiduMap.isTrafficEnabled()) {
                                    mBaiduMap.setTrafficEnabled(false);
                                    item.setTitle("实时交通(off)");
                                } else {
                                    mBaiduMap.setTrafficEnabled(true);
                                    item.setTitle("实时交通(on)");
                                }
                                break;
                            case R.id.id_map_mlocation:

                                getMyLocation();
                                break;
                            case R.id.id_map_model_common:
                                //普通模式
                                locationMode = MyLocationConfiguration.LocationMode.NORMAL;
                                break;
                            case R.id.id_map_model_following:
                                //跟随模式
                                locationMode = MyLocationConfiguration.LocationMode.FOLLOWING;
                                break;
                            case R.id.id_map_model_compass:
                                //罗盘模式
                                locationMode = MyLocationConfiguration.LocationMode.COMPASS;
                                break;
                        }
                        return true;
                    }
                });
        popup.show();
    }
    public void showLocMap() {
        //Toast.makeText(context, "this is showLocMap function",Toast.LENGTH_SHORT).show();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "没有权限,请手动开启定位权限", Toast.LENGTH_SHORT).show();
            // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义）
            ActivityCompat.requestPermissions(BaiduMapActivity.this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE
            }, BAIDU_READ_PHONE_STATE);
        } else {
            initLocation();
        }
    }

    //Android 6.0 以上的版本申请权限的回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            // requestCode即所声明的权限获取码，在checkSelfPermission时传入
            case BAIDU_READ_PHONE_STATE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获取到权限，作相应处理（调用定位SDK应当确保相关权限均被授权，否则可能引起定位失败）
                    initLocation();
                } else {
                    // 没有获取到权限，做特殊处理
                    Toast.makeText(getApplicationContext(), "获取位置权限失败，请手动开启", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    public class MylocationListener extends BDAbstractLocationListener {

        //定位请求回调接口
        //定位请求回调函数,这里面会得到定位信息
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            //BDLocation 回调的百度坐标类，内部封装了如经纬度、半径等属性信息
            if (bdLocation == null) {
                return;
            }
            if (location != null) {
                if (location.getLatitude() == bdLocation.getLatitude() && location.getLongitude() == bdLocation.getLongitude()) {
                    return;
                }
            }
            location = bdLocation;
            String city = location.getCity();
            //Toast.makeText(context, "location为："+ location,Toast.LENGTH_LONG).show();
            mLatitude = bdLocation.getLatitude();
            mLongitude = bdLocation.getLongitude();
            MyLocationData data = new MyLocationData.Builder()
                    .direction(mCurrentX)//设定图标方向
                    .accuracy(bdLocation.getRadius())//getRadius 获取定位精度,默认值0.0f
                    .latitude(mLatitude)//百度纬度坐标
                    .longitude(mLongitude)//百度经度坐标
                    .build();
            //设置定位数据, 只有先允许定位图层后设置数据才会生效，参见 setMyLocationEnabled(boolean)
            mBaiduMap.setMyLocationData(data);
            //地理坐标基本数据结构
            LatLng latLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
            //描述地图状态将要发生的变化,通过当前经纬度来使地图显示到该位置
            MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
            //改变地图状态
            mBaiduMap.setMapStatus(msu);
            //Toast.makeText(context, "您当前的位置为：" + bdLocation.getCity(),Toast.LENGTH_LONG).show();
        }

    }
    private String updateWithNewLocation(BDLocation location) {
        String coordinate = "";
        //Toast.makeText(context, "location information："+ location,Toast.LENGTH_LONG).show();
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                List addresses = geocoder.getFromLocation(latitude,
                        longitude, 1);
                StringBuilder sb = new StringBuilder();
                if (addresses.size() > 0) {
                    //Toast.makeText(context,""+addresses.size(),Toast.LENGTH_LONG).show();
                    Address address =(Address) addresses.get(0);
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        sb.append(address.getAddressLine(i)).append("\n");
                    }
                    sb.append(address.getLocality());
                    coordinate = sb.toString();
                }
            } catch (IOException e) {
                coordinate = "error";
                e.printStackTrace();
            }
        } else {
            coordinate = "no loction";
        }
        return coordinate;
    }
}
