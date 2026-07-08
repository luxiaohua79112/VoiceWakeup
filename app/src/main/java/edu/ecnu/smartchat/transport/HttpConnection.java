package edu.ecnu.smartchat.transport;


import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import edu.ecnu.smartchat.base.ErrCode;


public class HttpConnection {



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "DBGFACE/HttpConnect";
    private static final int CONNECT_TIMEOUT = 5000;  ///< 连接超时
    private static final int READ_TIMEOUT = 5000;  ///< 数据读取超时



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private HttpURLConnection mHttpConnect = null;
    private BufferedReader mBufferReader = null;


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 打开 HTTP 连接
     * @param realURL : 实际要连接的URL地址
     * @param realBody : 实际请求数据包
     * @return 返回错误码
     */
    public int open(final String realURL, final String basicAuth, final String realBody) {
        // 创建 URL对象
        java.net.URL url;
        try {
            url = new URL(realURL);
        } catch (MalformedURLException urlExp) {
            urlExp.printStackTrace();
            Log.d(TAG, "<open> realUrl=" + realURL + ", basicAuth=" + basicAuth
                    + ", realBody=" + realBody + ", [URL_EXP] urlExp=" + urlExp);
            return ErrCode.XERR_INVALID_PARAM;
        }

        Log.d(TAG, "<open> realUrl=" + realURL + ", basicAuth=" + basicAuth
                + ", realBody=" + realBody);

        // 连接HTTP服务器，写入数据
        try {
            mHttpConnect = (HttpURLConnection) url.openConnection();
            if (!TextUtils.isEmpty(basicAuth)) {  // 设置 basic auth
                mHttpConnect.setRequestProperty("Authorization", basicAuth);
            }
            mHttpConnect.setConnectTimeout(CONNECT_TIMEOUT);
            mHttpConnect.setReadTimeout(READ_TIMEOUT);
            mHttpConnect.setDoInput(true);
            mHttpConnect.setDoOutput(true);
            mHttpConnect.setRequestMethod("POST");
            mHttpConnect.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            Log.d(TAG, "<open> connecting...");
            mHttpConnect.connect();

            // 写入请求数据
            if (!TextUtils.isEmpty(realBody)) {
                DataOutputStream os = new DataOutputStream(mHttpConnect.getOutputStream());
                os.write(realBody.getBytes());  // 必须是原始数据流，否则中文乱码
                os.flush();
                os.close();
            }

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
            Log.d(TAG, "<open> [IO_EXP], ioExp=" + ioExp + ", realUrl=" + realURL
                    + ", basicAuth=" + basicAuth  + ", realBody=" + realBody );
            return ErrCode.XERR_HTTP_CONNECT;
        }

        Log.d(TAG, "<open> done, realUrl=" + realURL + ", basicAuth=" + basicAuth
                + ", realBody=" + realBody );
        return ErrCode.XOK;
    }

    /**
     * @brief 关闭HTTP 连接
     * @return 无
     */
    public void close() {
        if (mBufferReader != null) {
            try {
                mBufferReader.close();
            } catch (IOException ioExp) {
                ioExp.printStackTrace();
            }
            mBufferReader = null;
        }

        if (mHttpConnect != null) {
            mHttpConnect.disconnect();
            mHttpConnect = null;
            Log.d(TAG, "<close> done");
        }
    }


    /**
     * @brief 读取HTTP响应数据
     * @return 返回解析后的响应数据
     */
    public HttpTransport.RequestResult read() {
        HttpTransport.RequestResult requestResult = new HttpTransport.RequestResult();
        if (mHttpConnect == null) {
            Log.d(TAG, "<read> bad status");
            requestResult.mErrorCode = ErrCode.XERR_BAD_STATE;
            return requestResult;
        }

        Log.d(TAG, "<read> begin");
        try {
            requestResult.mErrorCode = ErrCode.XOK;
            requestResult.mHttpCode = mHttpConnect.getResponseCode();
            if (requestResult.mHttpCode != HttpURLConnection.HTTP_OK) {
                requestResult.mErrorCode = ErrCode.XERR_HTTP_RESP_CODE + requestResult.mHttpCode;
                Log.d(TAG, "<read> Error mHttpCode="
                        + requestResult.mHttpCode + ", errMessage=" + mHttpConnect.getResponseMessage());
                return requestResult;
            }

            // 读取回应数据包
            InputStream inputStream = mHttpConnect.getInputStream();
            mBufferReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = mBufferReader.readLine()) != null) {
                response.append(line);
            }

            // 保存回应数据包
            requestResult.mResponse = response.toString();
            Log.d(TAG, "<read> done, response=" + response);


        } catch (IOException ioExp) {
            Log.d(TAG, "<read> [IO_EXP], ioExp=" + ioExp);
            requestResult.mErrorCode = ErrCode.XERR_HTTP_NO_RESPONSE;
        }

        return requestResult;
    }

}
