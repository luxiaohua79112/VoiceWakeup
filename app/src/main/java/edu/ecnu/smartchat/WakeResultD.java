package edu.ecnu.smartchat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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
public class WakeResultD {

    public String sid = "";
    public int istart = 0;
    public int iresid = 0;
    public int iresIndex = 0;
    public int iduration = 0;
    public int nfillerscore = 0;
    public int nkeywordscore = 0;
    public int ncm_keyword = 0;
    public int ncm_filler = 0;
    public int ncm = 0;
    public int ncmThresh = 0;
    public double decConfidence = 0.0;
    public String keyword = "";
    public int nDelayFrame = 0;
    public int wakeUpType = 0;

    /**
     * @brief 解析唤醒结果 JSON 字符串
     *
     * @param json SDK 返回的 JSON 字符串
     * @return rlt 数组对应的结构体列表，解析失败返回空列表
     */
    public static ArrayList<WakeResultD> parseList(String json) {
        ArrayList<WakeResultD> resultList = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return resultList;
        }
        try {
            JSONObject root = new JSONObject(json);
            JSONArray rltArray = root.optJSONArray("rlt");
            if (rltArray == null) {
                return resultList;
            }
            for (int i = 0; i < rltArray.length(); i++) {
                JSONObject item = rltArray.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                WakeResultD result = new WakeResultD();
                result.sid = item.optString("sid", "");
                result.istart = item.optInt("istart", 0);
                result.iresid = item.optInt("iresid", 0);
                result.iresIndex = item.optInt("iresIndex", 0);
                result.iduration = item.optInt("iduration", 0);
                result.nfillerscore = item.optInt("nfillerscore", 0);
                result.nkeywordscore = item.optInt("nkeywordscore", 0);
                result.ncm_keyword = item.optInt("ncm_keyword", 0);
                result.ncm_filler = item.optInt("ncm_filler", 0);
                result.ncm = item.optInt("ncm", 0);
                result.ncmThresh = item.optInt("ncmThresh", 0);
                result.decConfidence = item.optDouble("decConfidence", 0.0);
                result.keyword = item.optString("keyword", "");
                result.nDelayFrame = item.optInt("nDelayFrame", 0);
                result.wakeUpType = item.optInt("wakeUpType", 0);
                resultList.add(result);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return resultList;
    }
}
