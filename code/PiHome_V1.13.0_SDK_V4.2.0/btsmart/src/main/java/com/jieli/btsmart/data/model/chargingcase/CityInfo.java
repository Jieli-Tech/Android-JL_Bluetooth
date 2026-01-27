package com.jieli.btsmart.data.model.chargingcase;

/**
 * CityInfo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 城市信息
 * @since 2024/10/28
 */
public class CityInfo {
    /**
     * 状态码
     */
    private String status;
    /**
     * 信息
     */
    private String info;
    /**
     * 结果码
     */
    private String infocode;
    /**
     * 省份
     */
    private String province;
    /**
     * 城市
     */
    private String city;
    /**
     * 城市码
     */
    private String adcode;
    /**
     * 经纬度
     */
    private String rectangle;


    public String getStatus() {
        return status;
    }

    public CityInfo setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getInfo() {
        return info;
    }

    public CityInfo setInfo(String info) {
        this.info = info;
        return this;
    }

    public String getInfocode() {
        return infocode;
    }

    public CityInfo setInfocode(String infocode) {
        this.infocode = infocode;
        return this;
    }

    public String getProvince() {
        return province;
    }

    public CityInfo setProvince(String province) {
        this.province = province;
        return this;
    }

    public String getCity() {
        return city;
    }

    public CityInfo setCity(String city) {
        this.city = city;
        return this;
    }

    public String getAdcode() {
        return adcode;
    }

    public CityInfo setAdcode(String adcode) {
        this.adcode = adcode;
        return this;
    }

    public String getRectangle() {
        return rectangle;
    }

    public CityInfo setRectangle(String rectangle) {
        this.rectangle = rectangle;
        return this;
    }

    @Override
    public String toString() {
        return "CityInfo{" +
                "status='" + status + '\'' +
                ", info='" + info + '\'' +
                ", infocode='" + infocode + '\'' +
                ", province='" + province + '\'' +
                ", city='" + city + '\'' +
                ", adcode='" + adcode + '\'' +
                ", rectangle='" + rectangle + '\'' +
                '}';
    }
}
