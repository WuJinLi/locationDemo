package com.wjl.demo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import java.util.ArrayList;
import java.util.List;

/**
 * author: WuJinLi
 * time  : 2018/5/8
 * desc  :
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button btn_go_webview, btn_get_location, btn_start, btn_gps;
    Context context = this;
    TextView tv_location_info, tv_title;
    Chronometer time;

    public LocationClient locationClient;
    public MyLocationListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initViews();
    }

    private void initData() {
        locationClient = new LocationClient(getApplicationContext());
        listener = new MyLocationListener();
        locationClient.registerLocationListener(listener);
        setContentView(R.layout.activity_main);
        List<String> list = new ArrayList<>();
        //运行时权限，没有注册就重新注册
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            list.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) !=
                PackageManager.PERMISSION_GRANTED) {
            list.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!list.isEmpty()) {//没有权限就添加
            String[] permissions = list.toArray(new String[list.size()]);//如果list不为空，就调用ActivityCompat
            // .requestPermissions添加权限
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        } else {//有相关权限则执行程序
            requestLocation();
        }

    }

    private void initViews() {
        btn_go_webview = findViewById(R.id.btn_go_webview);
        btn_get_location = findViewById(R.id.btn_get_location);
        tv_location_info = findViewById(R.id.tv_location_info);
        tv_title = findViewById(R.id.tv_title);
        time = findViewById(R.id.time);
        btn_start = findViewById(R.id.btn_start);
        btn_gps = findViewById(R.id.btn_gps);

        btn_go_webview.setOnClickListener(this);
        btn_get_location.setOnClickListener(this);
        btn_start.setOnClickListener(this);
        btn_gps.setOnClickListener(this);
        time.setFormat("T：%s");// 更改时间显示格式
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_go_webview:
                startActivity(new Intent(MainActivity.this, WebViewActivity.class));
                break;
            case R.id.btn_get_location:
                tv_title.setText("定位详细信息展示");
                break;
            case R.id.btn_start:
            case R.id.btn_gps:
                tv_location_info.setText("");
                requestLocation();
                break;
            default:
                break;
        }
    }

    private void requestLocation() {
        init();
        locationClient.start();
    }

    private void init() {
        LocationClientOption option = new LocationClientOption();
        if (NetWorkUtils.isNetworkAvailable(this)) {
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        } else {
            option.setOpenGps(true);
            option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
        }
        option.setLocationNotify(true);
        option.setIsNeedAddress(true);
        option.setScanSpan(1000); //设置发起定位请求的间隔时间为5000ms
        locationClient.setLocOption(option);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "用户取消权限，程序运行失败", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();//调用定位
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            {//定位信息获取
                StringBuilder currentPosition = new StringBuilder();
                currentPosition.append("纬度：").append(bdLocation.getLatitude()).append("\n");
                currentPosition.append("经线：").append(bdLocation.getLongitude()).append("\n");
                currentPosition.append("国家：").append(bdLocation.getCountry()).append("\n");
                currentPosition.append("省：").append(bdLocation.getProvince()).append("\n");
                currentPosition.append("市：").append(bdLocation.getCity()).append("\n");
                currentPosition.append("区：").append(bdLocation.getDirection()).append("\n");
                currentPosition.append("街道：").append(bdLocation.getStreet()).append("\n");
                currentPosition.append("定位方式：");
                if (bdLocation.getLocType() == BDLocation.TypeGpsLocation) {
                    currentPosition.append("GPS");
                } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
                    currentPosition.append("网络");
                } else {
                    currentPosition.append("Error:" + bdLocation.getLocType());
                }
                tv_location_info.setText(currentPosition);

                locationClient.stop();
            }
        }
    }


    @Override
    protected void onDestroy() {
        locationClient.unRegisterLocationListener(listener);
        locationClient.stop();
        super.onDestroy();
    }

}
