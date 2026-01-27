package com.jieli.btsmart.tool.ai.doubao.translate.auth;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Copyright (year) Beijing Volcano Engine Technology Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class SignSpeechTranslate {

    private static final String TAG = SignSpeechTranslate.class.getSimpleName();
    private static final BitSet URLENCODER = new BitSet(256);

    private static final String CONST_ENCODE = "0123456789ABCDEF";
    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.ENGLISH);

    public static String getXDate(Date date) {
        TimeZone timeZone = TimeZone.getTimeZone("GMT");
        if (DATE_FORMAT.getTimeZone() != timeZone) {
            DATE_FORMAT.setTimeZone(timeZone);
        }
        return DATE_FORMAT.format(date);
    }

    private final String region;
    private final String service;
    private final String host;
    private final String path;
    private final String ak;
    private final String sk;

    static {
        int i;
        for (i = 97; i <= 122; ++i) {
            URLENCODER.set(i);
        }

        for (i = 65; i <= 90; ++i) {
            URLENCODER.set(i);
        }

        for (i = 48; i <= 57; ++i) {
            URLENCODER.set(i);
        }
        URLENCODER.set('-');
        URLENCODER.set('_');
        URLENCODER.set('.');
        URLENCODER.set('~');
    }

    public SignSpeechTranslate(String region, String service, String host, String path, String ak, String sk) {
        this.region = region;
        this.service = service;
        this.host = host;
        this.path = path;
        this.ak = ak;
        this.sk = sk;
    }

    /*public static void main(String[] args) throws Exception {

        String AccessKeyID = "AKLTODZiOGZiOTE";
        String SecretAccessKey = "TURrNE4rNU5qQQ==";


        // 请求地址
        String endpoint = "translate.volces.com";
        String path = "/api/translate/speech/v1/"; // 路径，不包含 Query// 请求接口信息
        String service = "translate";
        String region = "cn-north-1";
        SignSpeechTranslate sign = new SignSpeechTranslate(region, service, endpoint, path, AccessKeyID, SecretAccessKey);

        String url = "wss://" + endpoint + path + "?" + sign.getSignUrl("SpeechTranslate", "2020-06-01", new byte[0]);
        System.out.println(url);

    }*/

    public String getSignUrl(String apiName, String version, byte[] body) throws Exception {
        if (body == null) {
            body = new byte[0];
        }
        String xContentSha256 = hashSHA256(body);
        String xDate = getXDate(Calendar.getInstance().getTime());
        String shortXDate = xDate.substring(0, 8);
        String signQueries = "Action;Version;X-Algorithm;X-Credential;X-Date;X-NotSignBody;X-SignedHeaders;X-SignedQueries";
        String credentialScope = shortXDate + "/" + region + "/" + service + "/request";

        SortedMap<String, String> realQueryList = new TreeMap<>();
        realQueryList.put(AIConfig.KEY_ACTION, apiName);
        realQueryList.put(AIConfig.KEY_VERSION, version);
        realQueryList.put("X-Algorithm", "HMAC-SHA256");
        realQueryList.put("X-Credential", ak + "/" + credentialScope);
        realQueryList.put("X-Date", xDate);
        realQueryList.put("X-NotSignBody", "");
        realQueryList.put("X-SignedHeaders", "");
        realQueryList.put("X-SignedQueries", signQueries);


        StringBuilder querySB = new StringBuilder();
        for (String key : realQueryList.keySet()) {
            querySB.append(signStringEncoder(key)).append("=").append(signStringEncoder(realQueryList.get(key))).append("&");
        }
        querySB.deleteCharAt(querySB.length() - 1);

        String canonicalStringBuilder = "GET" + "\n" + path + "\n" + querySB + "\n\n\n\n" + // signed_headers
                xContentSha256;
//        JL_Log.d(TAG, "getSignUrl", "canonicalStringBuilder: " + canonicalStringBuilder);

        String hashcanonicalString = hashSHA256(canonicalStringBuilder.getBytes());
        String signString = "HMAC-SHA256" + "\n" + xDate + "\n" + credentialScope + "\n" + hashcanonicalString;
//        JL_Log.d(TAG, "getSignUrl", "StringToSign:" + signString);

        byte[] signKey = genSigningSecretKeyV4(sk, shortXDate, region, service);
//        JL_Log.d(TAG, "getSignUrl", "签名密钥:" + bytesToHex(signKey));
        String signature = bytesToHex(hmacSHA256(signKey, signString));
//        JL_Log.d(TAG, "getSignUrl", "signature:" + signature);

        realQueryList.put("X-Signature", signature);

        querySB = new StringBuilder();
        for (String key : realQueryList.keySet()) {
            querySB.append(signStringEncoder(key)).append("=").append(signStringEncoder(realQueryList.get(key))).append("&");
        }
        querySB.deleteCharAt(querySB.length() - 1);

        return querySB.toString();
    }

    public HashMap<String, String> getSignHeader(String method, Map<String, String> queryList, byte[] body,
                                                 Date date, String action, String version) throws Exception {
        if (body == null) {
            body = new byte[0];
        }
//        JL_Log.i(TAG, "getSignHeader", "date : " + date.getTime() + "\n, body : " + CHexConver.byte2HexStr(body));
        String xContentSha256 = hashSHA256(body);
        String xDate = getXDate(date);
        String shortXDate = xDate.substring(0, 8);
        String contentType = "application/json; charset=utf-8";
        String signHeader = "host;x-date;x-content-sha256;content-type";

        SortedMap<String, String> realQueryList = new TreeMap<>(queryList);
        realQueryList.put(AIConfig.KEY_ACTION, action);
        realQueryList.put(AIConfig.KEY_VERSION, version);
        StringBuilder querySB = new StringBuilder();
        for (String key : realQueryList.keySet()) {
            querySB.append(signStringEncoder(key)).append("=").append(signStringEncoder(realQueryList.get(key))).append("&");
        }
        querySB.deleteCharAt(querySB.length() - 1);

        String canonicalStringBuilder = method + "\n" + path + "\n" + querySB + "\n" +
                "host:" + host + "\n" +
                "x-date:" + xDate + "\n" +
                "x-content-sha256:" + xContentSha256 + "\n" +
                "content-type:" + contentType + "\n" +
                "\n" +
                signHeader + "\n" +
                xContentSha256;

//        JL_Log.d(TAG, "getSignHeader", canonicalStringBuilder);

        String hashcanonicalString = hashSHA256(canonicalStringBuilder.getBytes());
        String credentialScope = shortXDate + "/" + region + "/" + service + "/request";
        String signString = "HMAC-SHA256" + "\n" + xDate + "\n" + credentialScope + "\n" + hashcanonicalString;
//        JL_Log.d(TAG, "getSignHeader", "signString : \n" + signString);

        byte[] signKey = genSigningSecretKeyV4(sk, shortXDate, region, service);
//        JL_Log.d(TAG, "getSignHeader", "signKey : \n" + bytesToHex(signKey));
        String signature = bytesToHex(hmacSHA256(signKey, signString));
//        JL_Log.d(TAG, "getSignHeader", "signature : \n" + signature);
        String sign = "HMAC-SHA256" +
                " Credential=" + ak + "/" + credentialScope +
                ", SignedHeaders=" + signHeader +
                ", Signature=" + signature;
        String url = "https://" + host + path + "?" + querySB;
//        JL_Log.d(TAG, "getSignHeader", "url : " + url + ", \nsign : " + sign);
        HashMap<String, String> map = new HashMap<>();
        map.put(AIConfig.KEY_HOST, host);
        map.put(AIConfig.KEY_X_DATE, xDate);
        map.put(AIConfig.KEY_X_CONTENT_SHA256, xContentSha256);
        map.put(AIConfig.KEY_CONTENT_TYPE, contentType);
        map.put(AIConfig.KEY_AUTHORIZATION, sign);
        map.put(AIConfig.KEY_URL, url);
        return map;
    }

    private String signStringEncoder(String source) {
        if (source == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder(source.length());
        ByteBuffer bb = UTF_8.encode(source);
        while (bb.hasRemaining()) {
            int b = bb.get() & 255;
            if (URLENCODER.get(b)) {
                buf.append((char) b);
            } else if (b == 32) {
                buf.append("%20");
            } else {
                buf.append("%");
                char hex1 = CONST_ENCODE.charAt(b >> 4);
                char hex2 = CONST_ENCODE.charAt(b & 15);
                buf.append(hex1);
                buf.append(hex2);
            }
        }

        return buf.toString();
    }

    public static String hashSHA256(byte[] content) throws Exception {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            return bytesToHex(md.digest(content));
        } catch (Exception e) {
            throw new Exception(
                    "Unable to compute hash while signing request: "
                            + e.getMessage(), e);
        }
    }

    public static byte[] hmacSHA256(byte[] key, String content) throws Exception {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(content.getBytes());
        } catch (Exception e) {
            throw new Exception(
                    "Unable to calculate a request signature: "
                            + e.getMessage(), e);
        }
    }

    private byte[] genSigningSecretKeyV4(String secretKey, String date, String region, String service) throws Exception {
        byte[] kDate = hmacSHA256((secretKey).getBytes(), date);
        byte[] kRegion = hmacSHA256(kDate, region);
        byte[] kService = hmacSHA256(kRegion, service);
        return hmacSHA256(kService, "request");
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}