package com.jieli.btsmart.data.model.chargingcase;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.jieli.btsmart.util.AppUtil;

import org.jetbrains.annotations.Contract;

import java.util.Objects;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 资源文件
 * @since 2023/12/7
 */
public class ResourceFile implements Parcelable {

    public static final int TYPE_SCREEN_SAVER = 1;
    public static final int TYPE_BOOT_ANIM = 2;

    private final int id;
    private final int type;
    @NonNull
    private String name = "";
    @NonNull
    private String path = "";
    private long size;

    public ResourceFile(int id, int type) {
        this.id = id;
        this.type = type;
    }

    protected ResourceFile(@NonNull Parcel in) {
        id = in.readInt();
        type = in.readInt();
        name = in.readString();
        path = in.readString();
        size = in.readLong();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(type);
        dest.writeString(name);
        dest.writeString(path);
        dest.writeLong(size);
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

    public String getFileName() {
        return AppUtil.getFileName(path, true);
    }

    public boolean isGif() {
        return AppUtil.isGif(path);
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
                ", type=" + type +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", size=" + size +
                '}';
    }
}
