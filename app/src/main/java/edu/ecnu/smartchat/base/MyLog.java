/**
 * @file MyLog.java
 * @brief 实现保存到本地日志文件，日志文件以当天日期为准
 * @author xiaohua.lu
 * @email luxiaohua@mx2.zzss.com
 * @date 2026-03-25
 */

package edu.ecnu.smartchat.base;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.content.Context;
import android.os.Environment;
import android.util.Log;



public class MyLog {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static String mLogFolder;           ///< 日志目录路径
    private static String mLogFile;             ///< 日志文件全路径
    private static FileWriter mWriter = null;


    private static int mCurrYear;
    private static int mCurrMonth;
    private static int mCurrDay;
    private static long mDetectTimestamp = System.currentTimeMillis();


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////

    public static boolean initialize(Context ctx) {
        String scardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String fetdirPath = scardPath + "/fetdir";
        String logPath = scardPath + "/fetdir/log";
        mLogFolder = scardPath + "/fetdir/log/MyLog";

        Log.d("MyLog", "<initialize> begin, mLogFolder=" + mLogFolder);

        // 创建日志目录
        try {
            File fetdirFolder = new File(fetdirPath);
            if (!fetdirFolder.exists()) {
                fetdirFolder.mkdir();
            }

            File logFolder = new File(logPath);
            if (!logFolder.exists()) {
                logFolder.mkdir();
            }

            File myLogFolder = new File(mLogFolder);
            if (!myLogFolder.exists()) {
                myLogFolder.mkdir();
            }

        } catch (Exception exp) {
            Log.e("MyLog", "<initialize> fail to create log folder, exp=" + exp);
        }

        // 日志当前 年月日
        Calendar calendar = Calendar.getInstance();
        mCurrYear = calendar.get(Calendar.YEAR) ;
        mCurrMonth = calendar.get(Calendar.MONTH) + 1;
        mCurrDay = calendar.get(Calendar.DATE);
        mDetectTimestamp = System.currentTimeMillis();

        // 删除旧的日志文件
        deleteOldFiles();


        String fileName = getCurrDateText() + ".log";
        mLogFile = mLogFolder + "/" + fileName;
        Log.d("MyLog", "<initialize> mLogFile=" + mLogFile);
        return createWriter();
    }

    public static void release() {
        destroyWriter();
        mLogFile = null;
    }

    public static void d(Object mObject, String log) {
        if (mObject != null) {
            if (mObject instanceof String) {
                d((String)mObject, log);
            } else {
                d(mObject.getClass(), log);
            }
        }
    }

    public static void d(Class<?> class_value, String log) {
        String tag = class_value.getSimpleName();
        Log.d(tag, log);
    }

    public static void d(String tag, String log) {
        Log.d(tag, log);

        if (mWriter == null) {
            return;
        }
        try {
            String logText = getLogTimestamp() + " [" + tag + "] " + log + "\n";
            mWriter.write(logText);
            mWriter.flush();   // ?? 是否需要每次刷新
        } catch (IOException exp) {
            exp.printStackTrace();
        }


        long intervalSec = (System.currentTimeMillis() - mDetectTimestamp) / 1000;
        if (intervalSec >= 3600) {  // 每过1小时检测一下年月日是否变化
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR) ;
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DATE);

            if ((year != mCurrYear) || (month != mCurrMonth) || (day != mCurrDay)) {  // 年月日发生变化了
                // 关闭旧的日志文件
                destroyWriter();

                // 创建新的日志文件
                String fileName = getCurrDateText() + ".log";
                mLogFile = mLogFolder + "/" + fileName;
                createWriter();
                Log.d("MyLog", "<d> recreate log file, mLogFile=" + mLogFile);

                mCurrYear = year;
                mCurrMonth = month;
                mCurrDay = day;
            }

            mDetectTimestamp = System.currentTimeMillis();
        }
    }

    public static void show(Context mContext, int mMsgID) {
        show(mContext, -1, mContext.getString(mMsgID));
    }

    public static void show(Context mContext, String mMsg) {
        show(mContext, -1, mMsg);
    }

    public static void show(Context mContext, int mIconID, int mMsgID) {
        show(mContext, mIconID, mContext.getString(mMsgID));
    }

    public static void show(Context mContext, final int mIconID, final String mMsg) {
        if (mContext != null) {
         //   MyToast.show(mContext, mIconID, mMsg);
        }
    }


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Internal Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////

    private static boolean createWriter() {
        if (mWriter == null) {
            try {
                mWriter = new FileWriter(mLogFile, true);
                String log = "\n\n\n=========================================================\n";
                mWriter.write(log);
                mWriter.flush();

            } catch (IOException exp) {
                exp.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private static void destroyWriter() {
        try {
            if (mWriter != null) {
                mWriter.flush();
                mWriter.close();
                mWriter = null;
            }
        } catch (IOException exp) {
            exp.printStackTrace();
            mWriter = null;
        }
    }


    /**
     * @brief 删除旧的日志记录，保留最近七天的
     */
    private static int deleteOldFiles() {
        int deletedCount = 0;

        try {
            // 检查日志文件夹是否存在
            File logDir = new File(mLogFolder);
            if (!logDir.exists() || !logDir.isDirectory()) {
                Log.e("MyLog", "<deleteOldFiles> mLogFolder=" + mLogFolder + " not exists");
                return 0;
            }

            // 获取目录下所有文件
            File[] files = logDir.listFiles();
            if (files == null || files.length == 0) {
                Log.d("MyLog", "<deleteOldFiles> mLogFolder=" + mLogFolder + " NO files!");
                return 0;
            }

            // 统计要保留的日志文件
            ArrayList<String> reservedList = new ArrayList<>();
            for (int i = 1; i< 7; i++) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, -1*i);
                int year = calendar.get(Calendar.YEAR) ;
                int month = calendar.get(Calendar.MONTH) + 1;
                int date = calendar.get(Calendar.DATE);
                String fileName = String.format(Locale.getDefault(), "%04d-%02d-%02d.log", year, month, date);
                reservedList.add(fileName);
                Log.d("MyLog", "<deleteOldFiles> reserved, fileName=" + fileName);
            }


            // 遍历所有文件
            for (File file : files) {
                String fileName = file.getName();
                if (isReservedFile(reservedList, fileName)) { // 保留日志文件跳过
                    continue;
                }

                if (file.delete()) {
                    deletedCount++;
                    Log.d("MyLog", "<deleteOldFiles> delete success, filePath=" + file.getAbsolutePath());
                } else {
                    Log.d("MyLog", "<deleteOldFiles> delete failure, filePath=" + file.getAbsolutePath());
                }
            }

            Log.d("MyLog", "<deleteOldFiles> done, deletedCount=" + deletedCount);

        } catch (Exception exp) {
            Log.e("MyLog", "<deleteOldFiles> [EXCEPTION] exp=" + exp);
        }

        return deletedCount;
    }

    /**
     * @brief 判断文件是否应该被保留
     */
    private static boolean isReservedFile(final List<String> reservedList, final String fileName) {
        int reservedCount = reservedList.size();

        for (int i = 0; i < reservedCount; i++) {
            String reservedFile = reservedList.get(i);
            if (reservedFile.contains(fileName)) {
                return true;
            }
        }
        return false;
    }

    private static String getLogTimestamp() {
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
        return time_txt;
    }

    /**
     * @brief 获取当天日期的文本，例如： 2026_03_25
     */
    private static String getCurrDateText() {
        String dateText = "";
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) ;
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);
        dateText = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, date);
        return dateText;
    }
}
