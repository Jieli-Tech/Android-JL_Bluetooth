package com.jieli.btsmart.data.model.settings;

import androidx.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.jieli.bluetooth.constant.AttrAndFunCode;

/**
 * 闪灯设置数据
 *
 * @author zqjasonZhong
 * @since 2020/5/18
 */
@SuppressWarnings("UnusedReturnValue")
public class LedBean implements Parcelable, MultiItemEntity {
    private int sceneId;
    private String scene; //场景
    private int effectId;
    private String effect; //效果
    private int attrType = AttrAndFunCode.ADV_TYPE_LED_SETTINGS;
    private int itemType;

    public final static int ITEM_TYPE_ONE = 0;
    public final static int ITEM_TYPE_TWO = 1;
    public final static int ITEM_TYPE_THREE = 2;

    public LedBean() {

    }

    public LedBean(int sceneId, String scene, int effectId, String effect) {
        this(sceneId, scene, effectId, effect, AttrAndFunCode.ADV_TYPE_LED_SETTINGS);
    }

    public LedBean(int sceneId, String scene, int effectId, String effect, int attrType) {
        setSceneId(sceneId);
        setScene(scene);
        setEffectId(effectId);
        setEffect(effect);
        setAttrType(attrType);
    }

    protected LedBean(Parcel in) {
        sceneId = in.readInt();
        scene = in.readString();
        effectId = in.readInt();
        effect = in.readString();
        attrType = in.readInt();
    }

    public static final Creator<LedBean> CREATOR = new Creator<LedBean>() {
        @Override
        public LedBean createFromParcel(Parcel in) {
            return new LedBean(in);
        }

        @Override
        public LedBean[] newArray(int size) {
            return new LedBean[size];
        }
    };

    public int getSceneId() {
        return sceneId;
    }

    public LedBean setSceneId(int sceneId) {
        this.sceneId = sceneId;
        return this;
    }

    public String getScene() {
        return scene;
    }

    public LedBean setScene(String scene) {
        this.scene = scene;
        return this;
    }

    public int getEffectId() {
        return effectId;
    }

    public LedBean setEffectId(int effectId) {
        this.effectId = effectId;
        return this;
    }

    public String getEffect() {
        return effect;
    }

    public LedBean setEffect(String effect) {
        this.effect = effect;
        return this;
    }

    public int getAttrType() {
        return attrType;
    }

    public LedBean setAttrType(int attrType) {
        this.attrType = attrType;
        return this;
    }

    public LedBean setItemType(int itemType) {
        this.itemType = itemType;
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return "LedBean{" +
                "sceneId=" + sceneId +
                ", scene='" + scene + '\'' +
                ", effectId=" + effectId +
                ", effect='" + effect + '\'' +
                ", attrType=" + attrType +
                ", itemType=" + itemType +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(sceneId);
        dest.writeString(scene);
        dest.writeInt(effectId);
        dest.writeString(effect);
        dest.writeInt(attrType);
        dest.writeInt(itemType);
    }

    @Override
    public int getItemType() {
        return itemType;
    }
}
