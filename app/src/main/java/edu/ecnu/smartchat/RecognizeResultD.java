package edu.ecnu.smartchat;

import com.iflytek.aikit.core.AiResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import edu.ecnu.smartchat.base.MyLog;


/**
 * @brief 讯飞 AIKit 语音唤醒结果结构体
 * <p>
 * 对应 SDK 返回 JSON 中 rlt 数组的单个元素。
 * JSON 示例：
 * {
 *   "rlt": [{
 *     "sid": "undefine",
 *     "istart": 1233,
 *     "iresid": 0,
 *     "iresIndex": 0,
 *     "iduration": 73,
 *     "nfillerscore": 56478,
 *     "nkeywordscore": 119369,
 *     "ncm_keyword": 1612,
 *     "ncm_filler": 0,
 *     "ncm": 1612,
 *     "ncmThresh": 800,
 *     "decConfidence": 0.0,
 *     "keyword": "法国队",
 *     "nDelayFrame": 0,
 *     "wakeUpType": 0
 *   }]
 * }
 */
public class RecognizeResultD {
    private static final String TAG = "[AiBasic]/RecognizeResultD";


    public String key = "";
    public ArrayList<WakeResultD> rlt = new ArrayList<>();

    /**
     * @brief 所有结果列表
     */
    public static ArrayList<RecognizeResultD> parseList(List<AiResponse> outputData) {
        ArrayList<RecognizeResultD> recognizedList = new ArrayList<>();
        for (int i = 0; i < outputData.size(); i++) {
            String key = outputData.get(i).getKey();   //引擎结果的key
            byte[] bytes = outputData.get(i).getValue(); //识别结果
            if (bytes == null) {
                continue;
            }
            String value = new String(bytes);
            String infoText = "key=" + key + ", value=" + value
                    + ", status=" + outputData.get(i).getStatus();
            MyLog.d(TAG, "<onRecognizeResult> [" + i + "] " + infoText);

            if ((key.equals("func_wake_up") || key.equals("func_pre_wakeup"))) {
                RecognizeResultD result = new RecognizeResultD();
                result.key = key;
                result.rlt = WakeResultD.parseList(value);
                recognizedList.add(result);
            }
        }
        return recognizedList;
    }


    /**
     * @brief 找到唤醒词最大的那个结果
     */
    public static RecognizeResultD findMaxNcm(ArrayList<RecognizeResultD> recognizedList) {
        if (recognizedList == null || recognizedList.size() <= 0) {
            return null;
        }


        RecognizeResultD maxResult = null;
        int maxNcm = -1;
        int maxIndex = -1;
        for (int i = 0; i < recognizedList.size(); i++) {
            RecognizeResultD recognizeResultD = recognizedList.get(0);
            if ((recognizeResultD.rlt == null) || (recognizeResultD.rlt.size() <=0)) {
                return null;
            }
            WakeResultD wakeResultD = recognizeResultD.rlt.get(0);
            if (wakeResultD.ncm > maxNcm) {
                maxIndex = i;
                maxNcm = wakeResultD.ncm;
            }
        }

        if (maxIndex >= 0 && maxIndex < recognizedList.size()) {
            maxResult = recognizedList.get(maxIndex);
        }

        return maxResult;
    }



}
