package com.jieli.btsmart.data.model.auracast;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

/**
 * AuracastQRCode
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/19
 * note: Auracast广播二维码
 */
public class AuracastQRCode implements Parcelable {

    private static final String HEAD_FLAG = "BLUETOOTH:";
    private static final String END_FLAG = ";";
    private static final String SEPARATOR = ":";
    private static final String UUID = "UUID";
    private static final String BROADCAST_NAME = "BN";
    private static final String BROADCAST_CODE = "BC";

    public static AuracastQRCode parseContent(String content) {
        if (null == content || content.isEmpty()) return null;
        String result = content.replaceAll("^BLUETOOTH:(.*?);$", "$1");
        if (result.isEmpty()) return null;
        if (!result.contains(UUID) || !result.contains(BROADCAST_NAME)) return null;
        String[] array = result.split(END_FLAG);
        String uuid = "";
        String name = "";
        String code = "";
        for (String sub : array) {
            String[] subArray = sub.split(SEPARATOR);
            if (subArray.length != 2) continue;
            String key = subArray[0];
            String value = subArray[1];
            switch (key) {
                case UUID: {
                    uuid = value;
                    break;
                }
                case BROADCAST_NAME: {
                    name = value;
                    break;
                }
                case BROADCAST_CODE: {
                    code = value;
                    break;
                }
            }
        }
        if (uuid.isEmpty() || name.isEmpty()) {
            return null;
        }
        return new AuracastQRCode(name, code);
    }

    /**
     * 广播名称
     */
    private String name;
    /**
     * 广播密钥
     */
    private String code;

    public AuracastQRCode() {
        this("", "");
    }

    public AuracastQRCode(String name, String code) {
        this.name = name;
        this.code = code;
    }

    protected AuracastQRCode(Parcel in) {
        name = in.readString();
        code = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(code);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AuracastQRCode> CREATOR = new Creator<AuracastQRCode>() {
        @Override
        public AuracastQRCode createFromParcel(Parcel in) {
            return new AuracastQRCode(in);
        }

        @Override
        public AuracastQRCode[] newArray(int size) {
            return new AuracastQRCode[size];
        }
    };

    public String getName() {
        return name;
    }

    public AuracastQRCode setName(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public AuracastQRCode setCode(String code) {
        this.code = code;
        return this;
    }

    public String generateQRContent(){
        return generateQRContent("184F");
    }

    public String generateQRContent(String uuid) {
        StringBuilder builder = new StringBuilder();
        builder.append(HEAD_FLAG);
        builder.append(UUID).append(SEPARATOR).append(uuid).append(END_FLAG);
        String nameBase = Base64.encodeToString(name.getBytes(), Base64.DEFAULT);
        builder.append(BROADCAST_NAME).append(SEPARATOR).append(nameBase).append(END_FLAG);
        if(null != code && !code.isEmpty()){
            String codeBase = Base64.encodeToString(code.getBytes(), Base64.DEFAULT);
            builder.append(BROADCAST_CODE).append(SEPARATOR).append(codeBase).append(END_FLAG);
        }
        builder.append(END_FLAG);
        return builder.toString();
    }

    @Override
    public String toString() {
        return "AuracastQRCode{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}
