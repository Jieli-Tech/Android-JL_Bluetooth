package com.jieli.btsmart.data.model.chargingcase;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;
import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.btsmart.util.ChargingBinUtil;

import org.jetbrains.annotations.Contract;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 资源文件
 * @since 2023/12/7
 */
public class ResourceFile implements Parcelable {


    /**
     * 不存在状态
     */
    public static final int STATE_NOT_EXIST = 0;
    /**
     * 已存在状态
     */
    public static final int STATE_ALREADY_EXIST = 1;
    /**
     * 正在使用状态
     */
    public static final int STATE_USING = 2;



    @IntDef(value = {
            STATE_NOT_EXIST,
            STATE_ALREADY_EXIST,
            STATE_USING,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResourceState {
    }

    /**
     * 资源ID
     */
    private final int id;
    /**
     * 资源类型
     */
    @ChargingCaseInfo.ResourceType
    private final int type;
    /**
     * 文件名
     */
    @NonNull
    private String name = "";
    /**
     * 文件路径
     */
    @NonNull
    private String path = "";
    /**
     * 文件大小
     */
    private long size;
    /**
     * 设备存储状态
     */
    @ResourceState
    private int devState = STATE_NOT_EXIST;
    /**
     * 设备资源信息
     */
    private ResourceInfo devFile;

    public ResourceFile(int id, @ChargingCaseInfo.ResourceType int type) {
        this.id = id;
        this.type = type;
    }

    protected ResourceFile(@NonNull Parcel in) {
        id = in.readInt();
        type = in.readInt();
        name = Objects.requireNonNull(in.readString());
        path = Objects.requireNonNull(in.readString());
        size = in.readLong();
        devState = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(type);
        dest.writeString(name);
        dest.writeString(path);
        dest.writeLong(size);
        dest.writeInt(devState);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ResourceFile> CREATOR = new Creator<ResourceFile>() {
        @NonNull
        @Contract("_ -> new")
        @Override
        public ResourceFile createFromParcel(Parcel in) {
            return new ResourceFile(in);
        }

        @NonNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public ResourceFile[] newArray(int size) {
            return new ResourceFile[size];
        }
    };

    public int getId() {
        return id;
    }

    @ChargingCaseInfo.ResourceType
    public int getType() {
        return type;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public ResourceFile setName(@NonNull String name) {
        this.name = name;
        return this;
    }

    @NonNull
    public String getPath() {
        return path;
    }

    public ResourceFile setPath(@NonNull String path) {
        this.path = path;
        return this;
    }

    public long getSize() {
        return size;
    }

    public ResourceFile setSize(long size) {
        this.size = size;
        return this;
    }

    @ResourceState
    public int getDevState() {
        return devState;
    }

    public ResourceFile setDevState(@ResourceState int devState) {
        this.devState = devState;
        return this;
    }

    public ResourceInfo getDevFile() {
        return devFile;
    }

    public ResourceFile setDevFile(ResourceInfo devFile) {
        this.devFile = devFile;
        return this;
    }

    public String getFileName() {
        return ChargingBinUtil.getFileNameByPath(path, true);
    }

    public boolean isGif() {
        return ChargingBinUtil.isGif(path);
    }

    /**
     * 是否设备正在使用的资源
     *
     * @return 结果
     */
    public boolean isDeviceUsing() {
        return devState == STATE_USING;
    }

    public boolean isCustomResource(){
        return type == ChargingCaseInfo.TYPE_SCREEN_SAVER && (name.toUpperCase().startsWith(ResourceInfo.CUSTOM_SCREEN_NAME)
                || name.equalsIgnoreCase(ResourceInfo.CUSTOM_SCREEN_NAME))
                || type == ChargingCaseInfo.TYPE_WALLPAPER && (name.toLowerCase().startsWith(ResourceInfo.CUSTOM_WALLPAPER_NAME)
                || name.equalsIgnoreCase(ResourceInfo.CUSTOM_WALLPAPER_NAME));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceFile that = (ResourceFile) o;
        return id == that.id && type == that.type && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, path);
    }

    @Override
    public String toString() {
        return "ResourceFile{" +
                "id=" + id +
                ", type=" + ChargingCaseInfo.printResourceType(type) +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", size=" + size +
                ", devState=" + devState +
                ", devFile=" + devFile +
                '}';
    }
}
