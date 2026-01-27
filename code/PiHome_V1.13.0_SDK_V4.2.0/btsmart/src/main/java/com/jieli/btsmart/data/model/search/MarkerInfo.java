package com.jieli.btsmart.data.model.search;

import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.Marker;

/**
 * 锚点信息
 *
 * @author zqjasonZhong
 * @since 2020/7/16
 */
public class MarkerInfo {
    private Marker mMarker;
    private Circle mCircle;
    private int type = MARKER_TYPE_DEVICE;
    private String addressName;

    public static final int MARKER_TYPE_DEVICE = 0;
    public static final int MARKER_TYPE_PHONE = 1;

    public Marker getMarker() {
        return mMarker;
    }

    public MarkerInfo setMarker(Marker marker) {
        mMarker = marker;
        return this;
    }

    public Circle getCircle() {
        return mCircle;
    }

    public MarkerInfo setCircle(Circle circle) {
        mCircle = circle;
        return this;
    }

    public int getType() {
        return type;
    }

    public MarkerInfo setType(int type) {
        this.type = type;
        return this;
    }

    public String getAddressName() {
        return addressName;
    }

    public MarkerInfo setAddressName(String addressName) {
        this.addressName = addressName;
        return this;
    }

    @Override
    public String toString() {
        return "MarkerInfo{" +
                "mMarker=" + mMarker +
                ", addressName='" + addressName + '\'' +
                '}';
    }
}
