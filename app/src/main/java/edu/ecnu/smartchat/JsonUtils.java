
package edu.ecnu.smartchat;


import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;



public class JsonUtils {
	private static final String TAG = "IOTSDK/JsonUtils";


	public static JSONObject generateJsonObject(final String content) {
		try {
			JSONObject newJsonObj = new JSONObject(content);
			return newJsonObj;

		} catch (JSONException jsonExp) {
			jsonExp.printStackTrace();
			Log.e(TAG, "<generateJsonObject> [EXCEPTION] jsonExp=" + jsonExp);
			return null;
		}
	}


	public static JSONObject parseJsonObject(JSONObject jsonState, String fieldName, JSONObject defVal) {
		try {
			JSONObject value = jsonState.getJSONObject(fieldName);
			return value;

		} catch (JSONException e) {
			Log.e(TAG, "<parseJsonObject> "
					+ ", fieldName=" + fieldName + ", exp=" + e.toString());
			return defVal;
		}
	}

	public static int parseJsonIntValue(JSONObject jsonState, String fieldName, int defVal) {
		try {
			int value = jsonState.getInt(fieldName);
			return value;

		} catch (JSONException e) {
			Log.e(TAG, "<parseJsonIntValue> "
					+ ", fieldName=" + fieldName + ", exp=" + e.toString());
			return defVal;
		}
	}

	public static long parseJsonLongValue(JSONObject jsonState, String fieldName, long defVal) {
		try {
			long value = jsonState.getLong(fieldName);
			return value;

		} catch (JSONException e) {
			Log.e(TAG, "<parseJsonLongValue> "
					+ ", fieldName=" + fieldName + ", exp=" + e.toString());
			return defVal;
		}
	}

	public static boolean parseJsonBoolValue(JSONObject jsonState, String fieldName, boolean defVal) {
		try {
			boolean value = jsonState.getBoolean(fieldName);
			return value;

		} catch (JSONException e) {
			Log.e(TAG, "<parseJsonBoolValue> "
					+ ", fieldName=" + fieldName + ", exp=" + e.toString());
			return defVal;
		}
	}

	public static String parseJsonStringValue(JSONObject jsonState, String fieldName, String defVal) {
		try {
			String value = jsonState.getString(fieldName);
			return value;

		} catch (JSONException e) {
			Log.e(TAG, "<parseJsonIntValue> "
					+ ", fieldName=" + fieldName + ", exp=" + e.toString());
			return defVal;
		}
	}

	public static JSONArray parseJsonArray(JSONObject jsonState, String fieldName) {
		try {
			JSONArray jsonArray = jsonState.getJSONArray(fieldName);
			return jsonArray;

		} catch (JSONException e) {
			Log.e(TAG, "<parseJsonArray> "
					+ ", fieldName=" + fieldName + ", exp=" + e.toString());
			return null;
		}
	}

	/**
	 * @brief 将字节流转换成 long 类型
	 */
	public static long bytesToLong(final byte[] bytes, int offset, int length, boolean bigEndian) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, length);
		if (bigEndian) {
			buffer.order(ByteOrder.BIG_ENDIAN); // 指定大端序
		} else {
			buffer.order(ByteOrder.LITTLE_ENDIAN); // 指定小端序
		}
		return buffer.getLong();
	}



}

