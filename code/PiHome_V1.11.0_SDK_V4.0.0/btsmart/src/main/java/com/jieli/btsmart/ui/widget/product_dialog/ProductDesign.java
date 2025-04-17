package com.jieli.btsmart.ui.widget.product_dialog;

import androidx.annotation.NonNull;

/**
 * 产品设计信息
 *
 * @author zqjasonZhong
 * @date 2019/7/23
 */
public class ProductDesign {
    private int action;
    private String scene;
    private float quantity;
    private String imageUrl;
    private String fileUrl;
    private boolean isCharging;
    private boolean isGif;
    private int failedRes;
    private int failedFileRes;

    public final static int ACTION_HIDE_QUANTITY = 0;
    public final static int ACTION_SHOW_QUANTITY = 1;

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public float getQuantity() {
        return quantity;
    }

    public void setQuantity(float quantity) {
        this.quantity = quantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isCharging() {
        return isCharging;
    }

    public void setCharging(boolean charging) {
        isCharging = charging;
    }

    public boolean isGif() {
        return isGif;
    }

    public void setGif(boolean gif) {
        isGif = gif;
    }


    public int getFailedRes() {
        return failedRes;
    }

    public void setFailedRes(int failedRes) {
        this.failedRes = failedRes;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public int getFailedFileRes() {
        return failedFileRes;
    }

    public void setFailedFileRes(int failedFileRes) {
        this.failedFileRes = failedFileRes;
    }

    @NonNull
    @Override
    public String toString() {
        return "ProductDesign{" +
                "action=" + action +
                ", scene='" + scene + '\'' +
                ", quantity=" + quantity +
                ", imageUrl='" + imageUrl + '\'' +
                ", fileUrl='" + fileUrl + '\'' +
                ", isCharging=" + isCharging +
                ", isGif=" + isGif +
                ", failedRes=" + failedRes +
                ", failedFileRes=" + failedFileRes +
                '}';
    }
}
