/**
 * @file ErrCode.java
 * @brief This file define the common error code
 * @author xiaohua.lu
 * @email 2489186909@qq.com
 * @version 1.0.0.1
 * @date 2021-12-04
 * @license Copyright (C) 2021 LuXiaoHua. All rights reserved.
 */
package edu.ecnu.smartchat.base;


/*
 * @brief 全局错误码定义
 */
public class ErrCode
{
    // 0: 表示正确
    public static final int XERR_NONE = 0;
    public static final int XOK = 0;

    // 通用错误码
    public static final int XERR_BASE = -1;
    public static final int XERR_UNKNOWN = -1;                  ///< 未知错误
    public static final int XERR_INVALID_PARAM = -2;            ///< 参数错误
    public static final int XERR_UNSUPPORTED = -3;              ///< 当前操作不支持
    public static final int XERR_NO_MEMORY = -4;                ///< 内存不足
    public static final int XERR_BAD_STATE = -5;                ///< 当前操作状态不正确
    public static final int XERR_BUFFER_OVERFLOW = -6;          ///< 缓冲区中数据不足
    public static final int XERR_BUFFER_UNDERFLOW = -7;         ///< 缓冲区中数据过多放不下
    public static final int XERR_COMPONENT_NOT_EXIST = -8;      ///< 相应的组件模块不存在
    public static final int XERR_TIMEOUT = -9;                  ///< 操作超时
    public static final int XERR_HW_NOT_FOUND = -10;            ///< 未找硬件设备
    public static final int XERR_USER_CANCELED = -11;           ///< 用户取消操作

    // 文件操作错误码
    public static final int XERR_FILE_BASE = -10000;
    public static final int XERR_FILE_NOT_EXIST = -10001;
    public static final int XERR_FILE_ALREADY_EXIST = -10002;
    public static final int XERR_FILE_OPEN = -10003;
    public static final int XERR_FILE_EOF = -10004;
    public static final int XERR_FILE_FULL = -10005;
    public static final int XERR_FILE_SEEK = -10006;
    public static final int XERR_FILE_READ = -10007;
    public static final int XERR_FILE_WRITE = -10008;

    // 编解码器错误码
    public static final int XERR_CODEC_BASE = -20000;
    public static final int XERR_CODEC_OPEN = -20001;
    public static final int XERR_CODEC_CLOSE = -20002;
    public static final int XERR_CODEC_INDATA = -20003;
    public static final int XERR_CODEC_OUTDATA = -20004;
    public static final int XERR_CODEC_NOBUFFER = -20005;
    public static final int XERR_CODEC_DECODING = -20006;
    public static final int XERR_CODEC_ENCODING = -20007;
    public static final int XERR_CODEC_MOREINDATA = -20008;     ///< 需要更多的数据进行编解码
    public static final int XERR_CODEC_DEC_EOS = -20009;        ///< 编解码缓冲区也已经完成
    public static final int XERR_CODEC_OUTFMT_READY = -20010;   ///< 输出格式就绪

    //
    // 对服务器的 HTTP操作错误码
    //
    public static final int XERR_HTTP_BASE = -30000;
    public static final int XERR_HTTP_URL = -30001;             ///< URL地址错误
    public static final int XERR_HTTP_PARAM = -30002;           ///< 参数错误
    public static final int XERR_HTTP_BODY = -30003;            ///< body错误
    public static final int XERR_HTTP_METHOD = -30004;          ///< Method方法错误
    public static final int XERR_HTTP_CONNECT = -30005;         ///< 连接错误
    public static final int XERR_HTTP_NO_RESPONSE = -30005;     ///< 服务器无响应
    public static final int XERR_HTTP_RESP_DATA = -30006;       ///< 回应数据包中，"data"自动错误
    public static final int XERR_HTTP_RESP_CODE = -30007;       ///< HTTP回应错误
    public static final int XERR_HTTP_JSON_PARSE = -30008;      ///< 回应数据包中，JSON解析错误
    public static final int XERR_HTTP_JSON_WRITE = -30009;      ///< 请求数据包中，JSON写入错误


}


