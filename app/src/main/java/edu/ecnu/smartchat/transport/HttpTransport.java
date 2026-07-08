package edu.ecnu.smartchat.transport;


import android.util.Base64;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import edu.ecnu.smartchat.base.ErrCode;

public class HttpTransport {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////
    /*
     * @brief HTTP请求参数
     */
    public static class RequestParam {
        public String mServerUrl;           ///< URL地址
        public String mAuthKey;             ///< 请求认证的Key
        public String mAuthSecret;          ///< 请求认证的Secret
        public HashMap<String, String> mReqParams;     ///< 附带参数
        public String mReqBody;             ///< 请求内容数据
        public Object mUserData;            ///< 用户附带数据

        @Override
        public String toString() {
            String infoText = "{ mServerUrl=" + mServerUrl
                    + ", mAuthKey=" + mAuthKey
                    + ", mAuthSecret=" + mAuthSecret
                    + ", mReqParams=" + mReqParams
                    + ", mReqBody=" + mReqBody + " }";
            return infoText;
        }
    }

    /*
     * @brief HTTP请求后，服务器回应结果
     */
    public static class RequestResult {
        public int mHttpCode;               ///< HTTP回应代码
        public int mErrorCode;              ///< 操作错误码字段
        public String mResponse;            ///< 响应数据

        @Override
        public String toString() {
            String infoText = "{ mHttpCode=" + mHttpCode
                    + ", mErrorCode=" + mErrorCode + ", mResponse=" + mResponse + " }";
            return infoText;
        }
    }

    /*
     * @brief HTTP请求后，服务器回应结果
     */
    public static class HttpTask {
        public Future<Integer> mFuture;
        public HttpReqCallable mReqCallable;

    }

    /*
     * @brief HTTP请求回调
     */
    public interface IRequestCallback{
        void onRequestDone(final RequestParam requestParam, final RequestResult requestResult);
    }




    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "DBGFACE/HttpTransport";


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static HttpTransport mInstance = null;
    private ExecutorService mExecSrv;


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public static HttpTransport getInstance() {
        if (mInstance == null) {
            synchronized (HttpTransport.class) {
                if (mInstance == null) {
                    mInstance = new HttpTransport();
                }
            }
        }
        return mInstance;
    }

    public int initialize() {
        mExecSrv = Executors.newFixedThreadPool(1);
        Log.d(TAG, "<initialize> done");
        return ErrCode.XOK;
    }

    public void release() {
        if (mExecSrv != null) {
            mExecSrv.shutdownNow();
            mExecSrv = null;
        }
        mInstance = null;
        Log.d(TAG, "<release> done");
    }

    /**
     * @brief 发送HTTP请求到服务器，并且异步返回回应数据
     * @param requestParam: 请求参数
     * @param requestCallback: 回调接口
     * @return 返回请求任务
     */
    public HttpTask requestToServer(final RequestParam requestParam,
                                    final IRequestCallback requestCallback) {
        if (requestParam == null || requestCallback == null) {
            return null;
        }

        HttpTask httpTask = new HttpTask();
        httpTask.mReqCallable = new HttpReqCallable(requestParam, requestCallback);
        httpTask.mFuture = mExecSrv.submit(httpTask.mReqCallable);
        return httpTask;
    }

    /**
     * @brief 取消正在进行的请求任务
     * @param httpTask: 请求任务
     */
    public void requestCancel(HttpTask httpTask) {
        if (httpTask == null) {
            return;
        }

        if (httpTask.mReqCallable != null) {
            httpTask.mReqCallable.stop();
        }

        if (httpTask.mFuture != null) {
            httpTask.mFuture.cancel(true);
        }
    }




    //////////////////////////////////////////////////////////////////////////
    ////////////////////// Internal Methods /////////////////////////////////
    //////////////////////////////////////////////////////////////////////////

    private class HttpReqCallable implements Callable<Integer> {
        private RequestParam mRequestParam;
        private IRequestCallback mRequestCallback;
        private AtomicBoolean mRunning = new AtomicBoolean(true);


        public HttpReqCallable(final RequestParam requestParam, final IRequestCallback callback) {
            mRequestParam = requestParam;
            mRequestCallback = callback;
            mRunning.set(true);
        }

        @Override
        public Integer call() {
            RequestResult requestResult = doRequestToServer(mRequestParam);
            mRequestCallback.onRequestDone(mRequestParam, requestResult);
            return requestResult.mErrorCode;
        }

        /**
         * @brief 强制停止线程处理任务，这个方法会在其他线程中被调用
         */
        public void stop() {
            mRunning.set(false);
        }


        private String generateBasicAuth(final String key, final String secret) {
            String auth = key + ":" + secret;
            byte[] authBytes = auth.getBytes(Charset.forName("UTF-8"));
            String authHeader = "Basic " + Base64.encodeToString(authBytes, Base64.NO_WRAP);
            return authHeader;
        }

        /*
         * @brief 给服务器发送HTTP请求，并且等待接收回应数据
         *        该函数是阻塞等待调用，必须在工作线程中执行
         */
        private RequestResult doRequestToServer(final RequestParam requestParam) {
            RequestResult requestResult = new RequestResult();
            requestResult.mErrorCode = ErrCode.XOK;

            String baseUrl = requestParam.mServerUrl;
            HashMap<String, String> params = requestParam.mReqParams;
            String basicAuth = ""; // generateBasicAuth(requestParam.mAuthKey, requestParam.mAuthSecret);

            // 检测 URL规格
            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                requestResult.mErrorCode = ErrCode.XERR_HTTP_URL;
                Log.d(TAG, "<doRequestToServer> Invalid baseUrl=" + baseUrl);
                return requestResult;
            }

            // 拼接URL和请求参数生成最终URL
            String realURL = baseUrl;
            if ((params != null) && (params.size() > 0)) {
                Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
                Map.Entry<String, String> entry =  it.next();
                realURL += "?" + entry.getKey() + "=" + entry.getValue();
                while (it.hasNext()) {
                    entry =  it.next();
                    realURL += "&" + entry.getKey() + "=" + entry.getValue();
                }
            }

            // 支持json格式消息体
            String realBody = requestParam.mReqBody;

            Log.d(TAG, "<doRequestToServer> requestUrl=" + realURL
                    + ", requestBody="  + realBody
                    + ", basicAuth=" + basicAuth);



            // 进行HTTP链路打开操作
            HttpConnection httpConnect = new HttpConnection();
            if (!mRunning.get()) {
                Log.d(TAG, "<doRequestToServer> opening interruped");
                requestResult.mErrorCode = ErrCode.XERR_HTTP_CONNECT;
                return requestResult;
            }
            int errCode = httpConnect.open(realURL, basicAuth, realBody);
            if (errCode != ErrCode.XOK) {  // 打开失败了
                httpConnect.close();
                requestResult.mErrorCode = ErrCode.XERR_HTTP_CONNECT;
                return requestResult;
            }

            // 数据读取操作
            if (!mRunning.get()) {
                Log.d(TAG, "<doRequestToServer> reading interruped");
                requestResult.mErrorCode = ErrCode.XERR_HTTP_NO_RESPONSE;
                return requestResult;
            }
            requestResult = httpConnect.read();
            if (requestResult.mErrorCode == ErrCode.XERR_HTTP_NO_RESPONSE) {  // 三次重试读取失败了
                httpConnect.close();
                return requestResult;
            }

            // 关闭 HTTP链路
            httpConnect.close();

            return requestResult;
        }
    }


}
