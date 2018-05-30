package com.wjl.demo.permission;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.wjl.demo.R;
import com.wjl.demo.permission.callback.PermissionOriginResultCallBack;
import com.wjl.demo.permission.callback.PermissionResultCallBack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Android6.0权限申请工具类
 * Created by yanxing on 12/9/15.
 */
public class PermissionUtil {
    public final static String TAG = "PermissionUtil";
    public final static String ACCEPT = "accept";
    public final static String DENIED = "denied";
    public final static String RATIONAL = "rational";
    public final static int PERMISSION_GRANTED = 1;
    public final static int PERMISSION_RATIONAL = 2;
    public final static int PERMISSION_DENIED = 3;
    public final static int PERMISSION_REQUEST_CODE = 42;

    private PermissionResultCallBack mPermissionResultCallBack;
    private PermissionOriginResultCallBack mPermissionOriginResultCallBack;
    private volatile static PermissionUtil instance;
    private int mRequestCode = -1;
    private static Activity mActivity;
    private Fragment mFragment;
    private List<PermissionInfo> mPermissionListNeedReq;
    /**
     * 被拒绝的权限列表
     */
    private List<PermissionInfo> mPermissionListDenied;
    /**
     * 被接受的权限列表
     */
    private List<PermissionInfo> mPermissionListAccepted;
    private String[] mPermissions;

    public boolean isUsedInLocation;

    private int type = 0;//权限类型 1.摄像机 2.麦克风 3.通讯录 4.蓝牙

    public void setType(int type) {
        this.type = type;
    }

    private PermissionUtil() {

    }

    public static PermissionUtil getInstance() {
        if (instance == null) {
            synchronized (PermissionUtil.class) {
                if (instance == null) {
                    instance = new PermissionUtil();
                }
            }
        }
        return instance;
    }

    /**
     * 检查单个权限是否被允许,(当应用第一次安装的时候,不会有rational的值,此时返回均是denied)
     *
     * @param permission The name of the permission being checked.
     * @return PermissionUtil.PERMISSION_GRANTED / PERMISSION_DENIED / PERMISSION_RATIONAL or {@code null}
     * if context is not instanceof Activity.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public int checkSinglePermission(String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return PERMISSION_GRANTED;
        }

        if (mActivity == null) {
            mActivity = getTopActivity();
        }

        if (mActivity != null) {
            if (mActivity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                return PERMISSION_GRANTED;
            } else {
                if (mActivity.shouldShowRequestPermissionRationale(permission)) {
                    return PERMISSION_RATIONAL;
                } else {
                    return PERMISSION_DENIED;
                }
            }
        } else {
            Log.e(TAG, "TopActivity not find");
            return -1;
        }
    }

    /**
     * 检查多个权限的状态,不会进行权限的申请.(当应用第一次安装的时候,不会有rational的值,此时返回均是denied)
     *
     * @param permissions The name of the permission being checked.
     * @return Map<String ,   List < PermissionInfo>> or {@code null}
     * if context is not instanceof Activity or topActivity can not be find
     */
    public Map<String, List<PermissionInfo>> checkMultiPermissions(String... permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return null;
        }

        if (mActivity == null) {
            mActivity = getTopActivity();
        }

        if (mActivity != null) {
            this.mPermissionListNeedReq = new ArrayList<PermissionInfo>();
            this.mPermissionListDenied = new ArrayList<PermissionInfo>();
            this.mPermissionListAccepted = new ArrayList<PermissionInfo>();

            for (String permission : permissions) {
                int result = checkSinglePermission(permission);
                switch (result) {
                    case PERMISSION_GRANTED:
                        mPermissionListAccepted.add(new PermissionInfo(permission));
                        break;
                    case PERMISSION_RATIONAL:
                        mPermissionListNeedReq.add(new PermissionInfo(permission));
                        break;
                    case PERMISSION_DENIED:
                        mPermissionListDenied.add(new PermissionInfo(permission));
                        break;
                    default:
                        break;
                }
            }

            HashMap<String, List<PermissionInfo>> map = new HashMap<String, List<PermissionInfo>>();
            if (!mPermissionListAccepted.isEmpty()) {
                map.put(ACCEPT, mPermissionListAccepted);
            }
            if (!mPermissionListNeedReq.isEmpty()) {
                map.put(RATIONAL, mPermissionListNeedReq);
            }
            if (!mPermissionListDenied.isEmpty()) {
                map.put(DENIED, mPermissionListDenied);
            }
            return map;
        } else {
            return new HashMap<String, List<PermissionInfo>>();
        }

    }

    /**
     * 用于fragment中请求权限
     *
     * @param fragment
     * @param permissions The name of the permission being checked.
     * @param callBack
     * @throws RuntimeException if not in the MainThread
     */
    public void request(@NonNull Fragment fragment, @NonNull String[] permissions, PermissionResultCallBack callBack) {
        this.mFragment = fragment;
        this.request(fragment.getActivity(), permissions, callBack);
    }

    /**
     * 用于fragment中请求权限
     *
     * @param fragment
     * @param permissions The name of the permission being checked.
     * @param callBack
     * @throws RuntimeException if not in the MainThread
     */
    public void request(@NonNull Fragment fragment, @NonNull String[] permissions, PermissionOriginResultCallBack
            callBack) {
        this.mFragment = fragment;
        this.request(fragment.getActivity(), permissions, callBack);
    }

    /**
     * 用于activity中请求权限
     *
     * @param activity
     * @param permissions The name of the permission being checked.
     * @param callBack
     * @throws RuntimeException if not in the MainThread
     */
    public void request(@NonNull Activity activity, @NonNull String[] permissions, PermissionResultCallBack callBack) {

        if (!this.checkSituation(permissions, callBack)) {
            return;
        }

        this.request(activity, permissions);
    }

    /**
     * 用于activity中请求权限
     *
     * @param activity
     * @param permissions The name of the permission being checked.
     * @param callBack
     * @throws RuntimeException if not in the MainThread
     */
    public void request(@NonNull Activity activity, @NonNull String[] permissions, PermissionOriginResultCallBack
            callBack) {

        if (!this.checkSituation(permissions, callBack)) {
            return;
        }

        this.request(activity, permissions);
    }

    /**
     * 申请权限方法
     *
     * @param permissions The name of the permission being checked.
     * @param callBack
     * @throws RuntimeException if not in the MainThread
     */
    public void request(@NonNull String[] permissions, PermissionResultCallBack callBack) {

        if (!this.checkSituation(permissions, callBack)) {
            return;
        }

        this.request(mActivity, permissions, callBack);
    }

    /**
     * 申请权限方法
     *
     * @param permissions The name of the permission being checked.
     * @param callBack
     * @throws RuntimeException if not in the MainThread
     */
    public void request(@NonNull String[] permissions, PermissionOriginResultCallBack callBack) {

        if (!this.checkSituation(permissions, callBack)) {
            return;
        }

        this.request(mActivity, permissions, callBack);
    }

    /**
     * 请求权限核心方法
     *
     * @param activity
     * @param permissions
     */
    private void request(Activity activity, String[] permissions) {
        this.mActivity = activity;

        if (mActivity == null) {
            mActivity = getTopActivity();
            if (mActivity == null) {
                Log.e(TAG, "TopActivity not find");
                return;
            }
        }

        this.mPermissions = permissions;

        if (needToRequest()) {
            requestPermissions();
        } else {
            onResult(mPermissionListAccepted, mPermissionListNeedReq, mPermissionListDenied);
            if (mPermissionListDenied.isEmpty() && mPermissionListNeedReq.isEmpty()) {
                onGranted();
                onGranted(mPermissionListAccepted);
            }
        }
    }

    /**
     * 检查环境是否满足申请权限的要求
     *
     * @param permissions
     * @param callBack
     * @return
     * @throws RuntimeException if not in the MainThread
     */
    private boolean checkSituation(String[] permissions, PermissionResultCallBack callBack) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("request permission only can run in MainThread!");
        }

        if (permissions.length == 0) {
            return false;
        }
        this.mPermissionResultCallBack = null;
        this.mPermissionOriginResultCallBack = null;

        this.mPermissionResultCallBack = callBack;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onGranted();
            mPermissionResultCallBack.onPermissionGranted(permissions);
            onResult(toPermissionList(permissions), new ArrayList<PermissionInfo>(), new ArrayList<PermissionInfo>());
            return false;
        }
        return true;
    }

    /**
     * 检查环境是否满足申请权限的要求
     *
     * @param permissions
     * @param callBack
     * @return
     * @throws RuntimeException if not in the MainThread
     */
    private boolean checkSituation(String[] permissions, PermissionOriginResultCallBack callBack) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("request permission only can run in MainThread!");
        }

        if (permissions.length == 0) {
            return false;
        }
        this.mPermissionResultCallBack = null;
        this.mPermissionOriginResultCallBack = null;

        this.mPermissionOriginResultCallBack = callBack;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onGranted();
            onResult(toPermissionList(permissions), new ArrayList<PermissionInfo>(), new ArrayList<PermissionInfo>());
            return false;
        }
        return true;
    }

    /**
     * 通过开启一个新的activity作为申请权限的媒介
     */
    private void requestPermissions() {

        Intent intent = new Intent(mActivity, HelpActivity.class);
        intent.putExtra("permissions", mPermissions);
        if (mRequestCode < 0) {
            mRequestCode = PERMISSION_REQUEST_CODE;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mActivity.startActivity(intent);
    }

    /**
     * 检查是否需要申请权限
     *
     * @return
     */
    @TargetApi(Build.VERSION_CODES.M)
    private boolean needToRequest() {
        checkMultiPermissions(mPermissions);

        if (mPermissionListNeedReq.size() > 0 || mPermissionListDenied.size() > 0) {
            mPermissions = new String[mPermissionListNeedReq.size() + mPermissionListDenied.size()];
            for (int i = 0; i < mPermissionListNeedReq.size(); i++) {
                mPermissions[i] = mPermissionListNeedReq.get(i).getName();
            }
            for (int i = mPermissionListNeedReq.size(); i < mPermissions.length; i++) {
                mPermissions[i] = mPermissionListDenied.get(i - mPermissionListNeedReq.size()).getName();
            }
            return true;
        }

        return false;
    }

    /**
     * 申请权限结果返回
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @TargetApi(Build.VERSION_CODES.M)
    protected void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == mRequestCode) {

            if (mActivity != null) {
                mActivity.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }

            if (mFragment != null) {
                mFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }

            boolean isAllGranted = true;
            List<PermissionInfo> needRationalPermissionList = new ArrayList<PermissionInfo>();
            List<PermissionInfo> deniedPermissionList = new ArrayList<PermissionInfo>();

            for (int i = 0; i < permissions.length; i++) {
                PermissionInfo info = new PermissionInfo(permissions[i]);
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    if (mActivity.shouldShowRequestPermissionRationale(permissions[i])) {
                        needRationalPermissionList.add(info);
                    } else {
                        deniedPermissionList.add(info);
                    }
                    isAllGranted = false;
                } else {
                    mPermissionListAccepted.add(info);
                }
            }

            onResult(mPermissionListAccepted, needRationalPermissionList, deniedPermissionList);

            if (deniedPermissionList.size() != 0) {
                onDenied(deniedPermissionList);
                isAllGranted = false;
            }

            if (needRationalPermissionList.size() != 0) {
                showRational(needRationalPermissionList);
                isAllGranted = false;
            }

            if (mPermissionListAccepted.size() != 0 && mPermissionResultCallBack != null) {
                onGranted(mPermissionListAccepted);
            }

            if (isAllGranted) {
                onGranted();
            }

        }
    }

    /**
     * 返回所有结果的列表list,包括通过的,拒绝的,允许提醒的三个内容,各个list有可能为空
     *
     * @param acceptPermissionList
     * @param needRationalPermissionList
     * @param deniedPermissionList
     * @return
     */
    private void onResult(List<PermissionInfo> acceptPermissionList,
                          List<PermissionInfo> needRationalPermissionList,
                          List<PermissionInfo> deniedPermissionList) {
        if (mPermissionOriginResultCallBack == null) {
            return;
        }

        mPermissionOriginResultCallBack.onResult(acceptPermissionList, needRationalPermissionList,
                deniedPermissionList);
    }

    /**
     * 权限被用户许可之后回调的方法
     */
    private void onGranted() {
        if (mPermissionResultCallBack != null) {
            mPermissionResultCallBack.onPermissionGranted();
        }
    }

    private void onGranted(List<PermissionInfo> list) {
        if (mPermissionResultCallBack == null) {
            return;
        }
        if (list == null || list.size() == 0) return;

        String[] permissions = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            permissions[i] = list.get(i).getName();
        }

        mPermissionResultCallBack.onPermissionGranted(permissions);
    }

    /**
     * 权限申请被用户否定之后的回调方法,这个主要是当用户点击否定的同时点击了不在弹出,
     * 那么当再次申请权限,此方法会被调用
     *
     * @param list
     */
    private void onDenied(List<PermissionInfo> list) {
        if (mPermissionResultCallBack == null) {
            return;
        }
        if (list == null || list.size() == 0) return;

        String[] permissions = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            permissions[i] = list.get(i).getName();
        }

//        mPermissionResultCallBack.onPermissionDenied(permissions);
        // TODO: 17/4/10 整体处理权限提示信息
        if (!TextUtils.isEmpty(getOnPermissionDeniedStr(permissions)) && !isUsedInLocation) {
//            Toast.makeText(mActivity.getApplicationContext(), getOnPermissionDeniedStr(permissions), Toast
// .LENGTH_SHORT).show();
            showDialog(getOnPermissionDeniedStr(permissions));
        }
        if (isUsedInLocation) {
            mPermissionResultCallBack.onPermissionDenied(permissions);
        }
    }

    /**
     * 权限申请被用户否定后的回调方法,这个主要场景是当用户点击了否定,但未点击不在弹出,
     * 那么当再次申请权限的时候,此方法会被调用
     *
     * @param list
     */
    private void showRational(List<PermissionInfo> list) {
        if (mPermissionResultCallBack == null) {
            return;
        }
        if (list == null || list.size() == 0) return;

        String[] permissions = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            permissions[i] = list.get(i).getName();
        }
        mPermissionResultCallBack.onRationalShow(permissions);
    }

    /**
     * 通过反射的方法获取最上层的Activity
     *
     * @return
     */
    private Activity getTopActivity() {
        Activity topActivity = null;
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Method getATMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            Object activityThread = getATMethod.invoke(null);
            activitiesField.setAccessible(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ArrayMap activites = (ArrayMap) activitiesField.get(activityThread);
                if (activites.size() > 0) {
                    Object activityClientRecord = activites.valueAt(0);

                    Class activityClientRecordClass = Class.forName("android.app.ActivityThread$ActivityClientRecord");
                    Field activityField = activityClientRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    topActivity = (Activity) activityField.get(activityClientRecord);
                }
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return topActivity;
    }

    /**
     * 转换为string的list
     *
     * @param list
     * @return
     */
    private List<String> toStringList(List<PermissionInfo> list) {
        List<String> result = new ArrayList<String>();
        for (PermissionInfo info : list) {
            result.add(info.getName());
        }
        return result;
    }


    /**
     * 将字符串数组转换为PermissionInfoList
     *
     * @param permissions
     * @return
     */
    private List<PermissionInfo> toPermissionList(String... permissions) {
        List<PermissionInfo> result = new ArrayList<PermissionInfo>();
        for (String permission : permissions) {
            result.add(new PermissionInfo(permission));
        }
        return result;
    }

    private String getOnPermissionDeniedStr(String[] permissions) {
        StringBuffer stringBuffer = new StringBuffer();
        if (permissions != null && permissions.length > 0) {
            for (String info : permissions) {
                String tempStr = getInfoStrByType(info);
                if (!TextUtils.isEmpty(tempStr)) {
                    stringBuffer.append(tempStr);
                    stringBuffer.append("\n");
                }
            }
        }
        return stringBuffer.toString();
    }

    private String getInfoStrByType(String permissionType) {
        String result = "";
        switch (permissionType) {
            case "android.permission.ACCESS_FINE_LOCATION":
                //获取精确位置  通过GPS芯片接收卫星的定位信息，定位精度达10米以内
//                result = "获取GPS定位权限";
                result = "需要在系统“权限”中打开“GPS”开关，才能使用GPS进行定位";
                type = 0;//默认权限类型
                break;
            case "android.permission.ACCESS_COARSE_LOCATION":
                //获取粗略位置 通过WiFi或移动基站的方式获取用户错略的经纬度信息，定位精度大概误差在30~1500米
//                result = "获取Wi-Fi或者移动基站定位权限";
                result = "需要在系统“权限”中打开“数据”开关，才能使用数据获取定位信息";
                type = 0;//默认权限类型
                break;
            case "android.permission.CHANGE_WIFI_STATE":
                //改变WiFi状态
                result = "获取改变Wi-Fi状态权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.INTERNET":
                //访问网络 访问网络连接，可能产生GPRS流量
                result = "获取访问网络权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.WRITE_EXTERNAL_STORAGE":
                //写入外部存储  允许程序写入外部存储，如SD卡上写文件
//                result = "获取写入外部存储权限";
                result = "需要在系统“权限”中打开“获取内部存储”开关，获取写入内部存储空间权限才能使用电e宝。";
                type = 0;//默认权限类型
                break;
            case "android.permission.MOUNT_UNMOUNT_FILESYSTEMS":
                //挂载文件系统 挂载、反挂载外部文件系统
                result = "获取挂载文件系统权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.READ_SMS":
                //读取短信内容
                result = "获取读取短信权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.READ_CONTACTS":
                //读取联系人 允许应用访问联系人通讯录信息
//                result = "获取读取联系人权限";
//                result = "需要在系统“权限”中打开“读取联系人开关，才能获取通讯录里的联系人";
                result = "当您给好友进行转账/赠送电费小红包，电e宝需读取通讯录好友信息，电e宝不会上传隐私信息，请允许获取通讯录权限。";
                type = 3;//通讯录权限
                break;
            case "android.permission.ACCESS_NETWORK_STATE":
                //获取网络状态 获取网络信息状态，如当前的网络连接是否有效
                result = "获取网络状态权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.ACCESS_WIFI_STATE":
                //获取WiFi状态 获取当前WiFi接入的状态以及WLAN热点的信息
                result = "获取Wi-Fi状态权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.READ_EXTERNAL_STORAGE":
                //程序可以读取设备外部存储空间（内置SDcard和外置SDCard）的文件，如果您的App已经添加了“WRITE_EXTERNAL_STORAGE ”权限
                // ，则就没必要添加读的权限了，写权限已经包含了读权限了。
                //result = "获取读取外部存储空间权限";
                result = "需要在系统“权限”中打开“获取外部存储”开关，获取读取外部存储空间权限才能使用电e宝。";
                type = 0;//默认权限类型
                break;
            case "android.permission.DISABLE_KEYGUARD":
                //禁用键盘锁 允许程序禁用键盘锁
                result = "获取禁用键盘锁权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.VIBRATE":
                //使用振动 允许振动
                result = "获取震动权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.CAMERA":
                //拍照权限  允许访问摄像头进行拍照
//                result = "获取相机拍照权限";
//                result = "需要在系统“权限”中打开“相机”开关，才能使用相机拍照或者扫描";
                result = "扫码(刷脸、照相)功能需要拍照支持，请允许电e宝开启拍照功能。";
                type = 1;//摄像头权限
                break;
            case "android.permission.RECEIVE_USER_PRESENT":
                //允许接收当用户出现，即屏幕点亮的广播
                result = "获取屏幕点亮广播权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.WAKE_LOCK":
                //唤醒锁定  允许程序在手机屏幕关闭后后台进程仍然运行
                result = "获取唤醒锁定权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.RECORD_AUDIO":
                //录音  录制声音通过手机或耳机的麦克
//                result = "获取录音权限";
                result = "上传语音功能需开启麦克风，请允许电e宝获取麦克风权限。";
                type = 2;//麦克风权限
                break;
            case "android.permission.READ_PHONE_STATE":
                //读取电话状态  访问电话状态
//                result = "获取读取电话状态权限";
                result = "需要在系统“权限”中打开“电话”开关，才能直接拨打电话";
                type = 0;//拨打电话权限
                break;
            case "android.permission.DOWNLOAD_WITHOUT_NOTIFICATION":
                //禁止发出通知，既后台下载
                result = "获取禁止发出通知进行后台下载权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.CALL_PHONE":
                //拨打电话 允许程序从非系统拨号器里输入电话号码
//                result = "获取拨打电话权限";
                result = "需要在系统“权限”中打开“电话”开关，才能直接拨打电话";
                type = 0;//拨打电话权限
                break;
            case "android.permission.WRITE_SETTINGS":
                //读写系统设置 允许读写系统设置项
                result = "获取读写系统设置权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.GET_TASKS":
                //获取任务信息  允许程序获取当前或最近运行的应用
                result = "获取访问任务信息权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.BATTERY_STATS":
                //电量统计 获取电池电量统计信息
                result = "获取电池电量状态权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.BLUETOOTH":
                //使用蓝牙  允许程序连接配对过的蓝牙设备
//                result = "获取使用蓝牙权限";
                result = "连接蓝牙打印机功能需开启蓝牙功能，请允许电e宝开启蓝牙功能。";
                type = 4;//蓝牙权限
                break;
            case "android.permission.BLUETOOTH_ADMIN":
                //蓝牙管理  允许程序进行发现和配对新的蓝牙设备
                result = "获取蓝牙管理权限";
                type = 0;//蓝牙权限
                break;
            case "android.permission.CHANGE_NETWORK_STATE":
                //改变网络状态  改变网络状态如是否能联网
                result = "获取改变网络状态权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.NFC":
                //允许NFC通讯 允许程序执行NFC近距离通讯操作，用于移动支持
                result = "获取NFC通讯权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.MODIFY_AUDIO_SETTINGS":
                //修改声音设置 修改声音设置信息
                result = "获取修改声音设置权限";
                type = 0;//麦克风权限
                break;
            case "org.simalliance.openmobileapi.SMARTCARD":
                result = "获取openmobileapi权限";
                type = 0;//默认权限类型
                break;
            case "android.permission.INSTALL_PACKAGES":
                //安装应用程序  允许程序安装应用
                //result = "获取安装应用权限";
                result = "需要在系统“权限”中打开“安装应用权限”开关，才能安装应用";
                type = 0;//默认权限类型
                break;
            case "android.permission.SYSTEM_ALERT_WINDOW":
                //显示系统窗口
                result = "获取显示系统窗口权限";
                type = 0;//默认权限类型
                break;
        }
        return result;
    }

    private void showDialog(String contents) {
        showDialogPermission(mActivity, "开启权限", contents);
    }

    /**
     * 获取权限dialog提示
     *
     * @param context
     * @param titleName 标题
     * @param content   内容描述
     */
    public void showDialogPermission(Context context, String titleName, String content) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawableResource(
                android.R.color.transparent);
        dialog.setContentView(R.layout.dialog_permission);
//        dialog.setCancelable(false);
        dialog.show();
        TextView tv_title = dialog.getWindow().findViewById(R.id.permission_tv_title);
        TextView tv_content = dialog.getWindow().findViewById(R.id.permission_tv_content);
        ImageView img_icon = dialog.getWindow().findViewById(R.id.permission_img_icon);
        //权限类型
        if (type == 1) {//摄像机
            titleName = "开启摄像头";
            img_icon.setBackgroundResource(R.mipmap.img_permission_camera);
        } else if (type == 2) {//麦克风
            titleName = "开启麦克风";
            img_icon.setBackgroundResource(R.mipmap.img_permission_mic);
        } else if (type == 3) {//通讯录
            titleName = "为了您正常使用电e宝，需要获取您的通讯录权限";
            img_icon.setBackgroundResource(R.mipmap.img_permission_address);
        } else if (type == 4) {//蓝牙
            titleName = "开启蓝牙";
            img_icon.setBackgroundResource(R.mipmap.img_permission_bluetooth);
        } else {//默认图片
            titleName = "开启app";
            img_icon.setBackgroundResource(R.mipmap.img_permission_common);
        }
        tv_title.setText(titleName);
        tv_content.setText(content);
        Button btn_next = (Button) dialog.getWindow().findViewById(R.id.btn_next);
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 开启权限
                PermissionUtil.getInstance().getAppDetailSettingIntent();
                dialog.dismiss();
            }
        });
    }

    /**
     * 跳转到权限设置界面
     */
    public void getAppDetailSettingIntent() {

        if (mActivity != null) {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 9) {
                intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.fromParts("package", mActivity.getPackageName(), null));
            } else if (Build.VERSION.SDK_INT <= 8) {
                intent.setAction(Intent.ACTION_VIEW);
                intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                intent.putExtra("com.android.settings.ApplicationPkgName", mActivity.getPackageName());
            }
            mActivity.startActivity(intent);
        }
    }

}
