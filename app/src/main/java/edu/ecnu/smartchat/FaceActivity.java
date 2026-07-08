
package edu.ecnu.smartchat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
//import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;
import com.iflytek.aikit.core.AiResponse;

import edu.ecnu.smartchat.base.MyLog;


public class FaceActivity extends AppCompatActivity {



    /////////////////////////////////////////////////////////////////////////////
    //////////////////////////// Constant Definition ////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    private final static String TAG = "[DBGFACE]/FaceActivity";

    private static final int PERMISSIONS_REQUEST_READ_SD = 1001;

    //
    // The message Id
    //
    private static final int MSGID_CHATENG_ANSWERED = 0x4001;
    private static final int MSGID_TIMER = 0x4002;
    private static final int REQUEST_OVERLAY_PERMISSION = 3;


    private static final int CAMERA_FACE_WIDTH = 480;
    private static final int CAMERA_FACE_HEIGHT = 640;



    /////////////////////////////////////////////////////////////////////////////
    //////////////////////////// Variable Definition ////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    private Button mBtnCreateDestroy = null;
    private Button mBtnRunStop = null;

    private Handler mMsgHandler = null;


    private Activity mActivity = this;

    private TeamRecognizer teamRecognizer = null;      // 球队识别器

    /////////////////////////////////////////////////////////////////////////////
    /////////////////////// Override Methods of Activity ////////////////////////
    /////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_face);

        Log.d(TAG, "<onCreate> ENTER");
   //     MyLog.initialize(this);
        mActivity = this;


        mBtnCreateDestroy = (Button)findViewById(R.id.btn_create_destroy);
        mBtnCreateDestroy.setOnClickListener(view -> onBtnCreateDestroy(view) );

        mBtnRunStop = (Button)findViewById(R.id.btn_run_stop);
        mBtnRunStop.setOnClickListener(view -> onBtnRunStop(view) );

        // 检查并申请所需权限：文件读取（READ_EXTERNAL_STORAGE）、麦克风（RECORD_AUDIO）
        // 注：INTERNET 为普通权限，无需运行时申请
        // 注：Android 11+ 中 WRITE_EXTERNAL_STORAGE 运行时权限已无效（Scoped Storage 强制），
        //     如需写入共享存储，请使用 MANAGE_EXTERNAL_STORAGE 或 getExternalFilesDir()
        String[] neededPermissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        };

        ArrayList<String> requestList = new ArrayList<>();
        for (String perm : neededPermissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), perm)
                    != PackageManager.PERMISSION_GRANTED) {
                requestList.add(perm);
            }
        }

        if (!requestList.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    requestList.toArray(new String[0]), PERMISSIONS_REQUEST_READ_SD);
        }


        // 创建主线程消息处理
        mMsgHandler = new Handler(this.getMainLooper()) {
            @SuppressLint("HandlerLeak")
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSGID_CHATENG_ANSWERED:
                     //   onMsgChatingAnswered(msg);
                        break;

                    case MSGID_TIMER:
                            onMsgTimer(msg);
                        break;
                }
            }
        };

/*
        XXPermissions.with(mActivity)
                .permission("android.permission.WRITE_EXTERNAL_STORAGE"
                        , "android.permission.READ_EXTERNAL_STORAGE"
                        , "android.permission.MANAGE_EXTERNAL_STORAGE"
                        , "android.permission.RECORD_AUDIO")
                .request(new com.hjq.permissions.OnPermission() {
                    @Override
                    public void hasPermission(List<String> permissions, boolean allGranted) {
                        MyLog.d(TAG, "<initGame.onGranted> allGranted=" + allGranted);
                        if (allGranted) {
                            teamRecognizer = new TeamRecognizer();
                            teamRecognizer.initialize(mActivity.getApplicationContext(), new TeamRecognizer.IInitCallback() {
                                @Override
                                public void onRecognizerInitDone(int errCode) {
                                    MyLog.d(TAG, "<initGame.onRecognizerInitDone> errCode=" + errCode);
                                }
                            });
                        }
                    }

                    @Override
                    public void noPermission(List<String> permissions, boolean doNotAskAgain) {
                        MyLog.d(TAG, "<initGame.onDenied> doNotAskAgain=" + doNotAskAgain);
                        popupMessage("获取录音权限失败，请手动赋予录音权限");
                        if (doNotAskAgain) {
                            XXPermissions.startPermissionActivity(mActivity, permissions);
                        }
                    }
                });

*/

        Log.d(TAG, "<onCreate> EXIT");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_READ_SD) {
            boolean readStorageGranted = false;
            boolean recordAudioGranted = false;

            for (int i = 0; i < permissions.length; i++) {
                String perm = permissions[i];
                int result = grantResults[i];
                MyLog.d(TAG, "<onRequestPermissionsResult> " + perm + " result=" + result);

                if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(perm)) {
                    readStorageGranted = (result == PackageManager.PERMISSION_GRANTED);
                } else if (Manifest.permission.RECORD_AUDIO.equals(perm)) {
                    recordAudioGranted = (result == PackageManager.PERMISSION_GRANTED);
                }
            }

            MyLog.d(TAG, "<onRequestPermissionsResult> readStorageGranted=" + readStorageGranted
                    + ", recordAudioGranted=" + recordAudioGranted);

            if (!recordAudioGranted) {
                popupMessage("缺少录音权限，语音功能将无法使用");
            }
            if (!readStorageGranted) {
                popupMessage("缺少文件读取权限，无法读取外部文件");
            }

            teamRecognizer = new TeamRecognizer();
            teamRecognizer.initialize(mActivity.getApplicationContext(), new TeamRecognizer.IInitCallback() {
                @Override
                public void onRecognizerInitDone(int errCode) {
                    MyLog.d(TAG, "<initGame.onRecognizerInitDone> errCode=" + errCode);
                }
            });
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_CHATENG_ANSWERED);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            System.exit(0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }



    /////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// UI Events Methods ////////////////////////////
    /////////////////////////////////////////////////////////////////////////////

    void onMsgTimer(Message msg) {
        String time_txt = "";
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) ;
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int ms = calendar.get(Calendar.MILLISECOND);
        time_txt = String.format(Locale.getDefault(), "%d-%02d-%02d %02d:%02d:%02d.%d",
                year, month, date, hour,minute, second, ms);

        MyLog.d(TAG, "<onMsgTimer> time=" + time_txt);

        if (mMsgHandler != null) {
            mMsgHandler.sendEmptyMessageDelayed(MSGID_TIMER, 10000);
        }
    }


    private boolean bRecognizing = false;

    /**
     * @brief 创建或销毁
     */
    void onBtnCreateDestroy(View view) {
        if (!bRecognizing) {

            Log.d(TAG, "<onBtnCreateDestroy> recognizing start......");

            String wakeupWords = "法国队,西班牙队";
            teamRecognizer.recognizeStart(wakeupWords, new TeamRecognizer.IRecognizeCallback() {
                @Override
                public void onRecognizeResult(List<AiResponse> outputData) {
                    MyLog.d(TAG, "<onRecognizeResult> " + outputData);

                    runOnUiThread(() -> {
                        popupMessage("<onRecognizeResult> outputData=" + outputData);
                    });
                }
            });

            popupMessage("Start recognizing...");
            bRecognizing = true;

        } else {

            teamRecognizer.recognizeStop();
            bRecognizing = false;


            Log.d(TAG, "<onBtnCreateDestroy> recognize stopped!");
            popupMessage("Recognize stopped!");
        }


    }



    /**
     * @brief 运行或者停止
     */
    void onBtnRunStop(View view) {

    }

    ////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// 解析特征值 ////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    Thread mParseThread = null;




    ////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// 模型文件管理 ////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////



    public void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }





    ////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// Utility Methods /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @brief 将Bitmap转换为NV21格式的字节数组
     */
    private byte[] convertBitmapToNV21(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);

        // NV21数组大小 = Y分量 + UV交错分量
        byte[] nv21 = new byte[width * height * 3 / 2];
        int yIndex = 0;
        int uvIndex = width * height;

        // 转换为YUV
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = argb[y * width + x];

                // 提取ARGB分量
                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;

                // 转换公式 (标准RGB转YUV)
                int Y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                int U = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                int V = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;

                // 限制取值范围
                Y = Math.max(0, Math.min(Y, 255));
                U = Math.max(0, Math.min(U, 255));
                V = Math.max(0, Math.min(V, 255));

                // 存储Y分量
                nv21[yIndex++] = (byte) Y;

                // 对于UV分量，NV21格式要求UV交错存储（VU交错）
                // 这里先计算，稍后处理UV平面
                if (y % 2 == 0 && x % 2 == 0) {
                    // 计算UV平面的位置（每2x2块采样一个UV）
                    int uvOffset = uvIndex + (y / 2) * width + (x / 2) * 2;
                    // 存储V分量
                    nv21[uvOffset] = (byte) V;
                    // 存储U分量
                    nv21[uvOffset + 1] = (byte) U;
                }
            }
        }

        return nv21;
    }

    /**
     * 将ARGB_8888格式的Bitmap转换为BGR格式的24位像素数据
     *
     * @param inputBmp ARGB_8888格式的Bitmap
     * @return BGR格式的字节数组，长度 = 宽度 * 高度 * 3
     */
    public static byte[] convertBitmapToBgr(Bitmap inputBmp) {
        if (inputBmp == null) {
            return null;
        }

        int width = inputBmp.getWidth();
        int height = inputBmp.getHeight();

        // 获取Bitmap像素数据
        int[] pixels = new int[width * height];
        inputBmp.getPixels(pixels, 0, width, 0, 0, width, height);

        // 创建BGR数据数组
        byte[] bgrData = new byte[width * height * 3];

        // 遍历像素并转换为BGR格式
        int pixelIndex = 0;
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            // 按照BGR顺序存储
            bgrData[pixelIndex++] = (byte) (pixel & 0xFF);          // Blue
            bgrData[pixelIndex++] = (byte) ((pixel >> 8) & 0xFF);   // Green
            bgrData[pixelIndex++] = (byte) ((pixel >> 16) & 0xFF);  // Red
        }

        return bgrData;
    }

    /**
     * @brief 将float数组以二进制形式保存到文件
     * @param feats float数组
     * @param filePath 文件路径
     * @return 是否保存成功
     */
     public boolean saveFloatArrayToFile(float[] feats, String filePath) {
        FileOutputStream fos = null;
        DataOutputStream dos = null;

        try {
            File file = new File(filePath);

            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 创建文件输出流
            fos = new FileOutputStream(file);
            dos = new DataOutputStream(fos);

            // 1. 首先写入数组长度
            dos.writeInt(feats.length);

            // 2. 写入float数据
            for (float value : feats) {
                dos.writeFloat(value);
            }

            // 强制刷入磁盘
            dos.flush();

            return true;

        } catch (IOException exp) {
            exp.printStackTrace();
            return false;

        } finally {
            // 关闭流
            try {
                if (dos != null) dos.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 从二进制文件读取float数组
     * @param filePath 文件路径
     * @return float数组，读取失败返回null
     */
    public float[] loadFloatArrayFromFile(String filePath) {
        FileInputStream fis = null;
        DataInputStream dis = null;

        try {
            File file = new File(filePath);

            // 检查文件是否存在
            if (!file.exists()) {
                Log.d(TAG, "<loadFloatArrayFromFile> file not exist, filePath=" + filePath);
                return null;
            }

            // 获取文件大小
            long fileSize = file.length();

            // 创建文件输入流
            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);

            // 1. 读取数组长度
            int length = dis.readInt();

            // 2. 验证文件大小是否匹配
            long expectedSize = 4 + length * 4; // 4字节存储长度 + 每个float 4字节
            if (fileSize != expectedSize) {
                Log.d(TAG, "<loadFloatArrayFromFile> 文件大小不匹配: 期望=" + expectedSize + ", 实际=" + fileSize);
                return null;
            }

            // 3. 读取float数据
            float[] feats = new float[length];
            for (int i = 0; i < length; i++) {
                feats[i] = dis.readFloat();
            }

            return feats;

        } catch (IOException exp) {
            exp.printStackTrace();
            return null;
        } finally {
            // 关闭流
            try {
                if (dis != null) dis.close();
                if (fis != null) fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * @brief 将byte[]数组以二进制形式保存到文件
     * @param inputData byte数组
     * @param filePath 文件路径
     * @return 是否保存成功
     */
    public boolean saveByteArrayToFile(byte[] inputData, String filePath) {
        FileOutputStream fos = null;
        DataOutputStream dos = null;

        try {
            File file = new File(filePath);

            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 创建文件输出流
            fos = new FileOutputStream(file);
            dos = new DataOutputStream(fos);

            // 直接写入数据流
            dos.write(inputData);

            // 强制刷入磁盘
            dos.flush();

            return true;

        } catch (IOException exp) {
            exp.printStackTrace();
            return false;

        } finally {
            // 关闭流
            try {
                if (dos != null) dos.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public byte[] loadByteArrayFromFile(String filePath) {
        FileInputStream fis = null;
        DataInputStream dis = null;

        try {
            File file = new File(filePath);

            // 检查文件是否存在
            if (!file.exists()) {
                Log.d(TAG, "<loadByteArrayFromFile> file not exist, filePath=" + filePath);
                return null;
            }

            // 获取文件大小
            long fileSize = file.length();

            // 创建文件输入流
            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);



            // 3. 读取 bytes数据
            byte[] data = new byte[(int)fileSize];
            dis.read(data);
            return data;

        } catch (IOException exp) {
            exp.printStackTrace();
            return null;
        } finally {
            // 关闭流
            try {
                if (dis != null) dis.close();
                if (fis != null) fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 使用try-with-resources（需要API 19+）
     */
    public static boolean saveTextToFile(String text, String filePath) {
        if (text == null) {
            return false;
        }

        File file = new File(filePath);
        File parentDir = file.getParentFile();

        // 创建父目录
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                return false;
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(text);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Methods of UI //////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    protected void popupMessage(String message)  {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }




    /////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Methods of Player ////////////////////////////
    /////////////////////////////////////////////////////////////////////////////

    /**
     * 将BGR格式的图像数据保存为JPEG文件
     *
     * @param bgrBuffer BGR格式的图像数据
     * @param width 图像宽度
     * @param height 图像高度
     * @param filePath 保存的JPEG文件路径
     * @return 保存是否成功
     */
    public static boolean saveBgrToFile(byte[] bgrBuffer, int width, int height, String filePath) {
        if (bgrBuffer == null || width <= 0 || height <= 0 || filePath == null || filePath.isEmpty()) {
            return false;
        }

        // 检查BGR数据大小是否正确
        int expectedSize = width * height * 3;
        if (bgrBuffer.length < expectedSize) {
            return false;
        }

        FileOutputStream fos = null;

        try {
            // 1. 将BGR数据转换为Bitmap
            Bitmap bitmap = bgrToBitmap(bgrBuffer, width, height);
            if (bitmap == null) {
                return false;
            }

            // 2. 确保父目录存在
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    return false;
                }
            }

            // 3. 保存为JPEG文件
            fos = new FileOutputStream(file);
            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();

            // 4. 回收Bitmap内存
            bitmap.recycle();

            return success;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean saveBmpToFile(final Bitmap bitmap, String filePath) {
        if (bitmap == null || filePath == null || filePath.isEmpty()) {
            return false;
        }

        FileOutputStream fos = null;

        try {
            // 2. 确保父目录存在
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    return false;
                }
            }

            // 3. 保存为JPEG文件
            fos = new FileOutputStream(file);
            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();

            // 4. 回收Bitmap内存
            bitmap.recycle();

            return success;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 将BGR数据转换为Bitmap
     */
    public static Bitmap bgrToBitmap(byte[] bgrBuffer, int width, int height) {
        if (bgrBuffer == null || bgrBuffer.length < width * height * 3) {
            return null;
        }

        int[] argbPixels = new int[width * height];
        int pixelIndex = 0;

        // 将BGR转换为ARGB
        for (int i = 0; i < argbPixels.length; i++) {
            int b = bgrBuffer[pixelIndex++] & 0xFF;  // Blue
            int g = bgrBuffer[pixelIndex++] & 0xFF;  // Green
            int r = bgrBuffer[pixelIndex++] & 0xFF;  // Red

            // 创建ARGB像素值 (Alpha=255, Red, Green, Blue)
            argbPixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        // 创建Bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(argbPixels, 0, width, 0, 0, width, height);

        return bitmap;
    }




}
