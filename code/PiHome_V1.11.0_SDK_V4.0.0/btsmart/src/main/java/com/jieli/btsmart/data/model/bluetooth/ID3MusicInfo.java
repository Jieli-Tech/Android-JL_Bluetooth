package com.jieli.btsmart.data.model.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ID3MusicInfo implements Parcelable {
    //    歌曲名
    private String title;
    //    作曲家
    private String artist;
    //    专辑
    private String album;
    //    序号
    private int number = -1;
    //    歌单总长度
    private int total;
    //    总时长
    private int totalTime;
    //    类型
    private String genre;
    //    当前时间
    private int currentTime = -1;
    //    播放状态
    private boolean playStatus;

    public ID3MusicInfo(){

    }

    protected ID3MusicInfo(Parcel in) {
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        number = in.readInt();
        total = in.readInt();
        totalTime = in.readInt();
        genre = in.readString();
        currentTime = in.readInt();
        playStatus = in.readByte() != 0;
    }

    public static ID3MusicInfo cloneMySelf(ID3MusicInfo info){
        ID3MusicInfo clone = new ID3MusicInfo();
        clone.setTotal(info.getTotal());
        clone.setCurrentTime(info.getCurrentTime());
        clone.setPlayStatus(info.isPlayStatus());
        clone.setAlbum(info.getAlbum());
        clone.setArtist(info.getArtist());
        clone.setTitle(info.getTitle());
        clone.setGenre(info.getGenre());
        clone.setNumber(info.getNumber());
        clone.setTotalTime(info.getTotalTime());
        return clone;
    }

    public static final Creator<ID3MusicInfo> CREATOR = new Creator<ID3MusicInfo>() {
        @Override
        public ID3MusicInfo createFromParcel(Parcel in) {
            return new ID3MusicInfo(in);
        }

        @Override
        public ID3MusicInfo[] newArray(int size) {
            return new ID3MusicInfo[size];
        }
    };

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(int totalTime) {
        this.totalTime = totalTime;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(int currentTime) {
        this.currentTime = currentTime;

    }

    public boolean isPlayStatus() {
        return playStatus;
    }

    public void setPlayStatus(boolean playStatus) {
        this.playStatus = playStatus;
    }

    @NonNull
    @Override
    public ID3MusicInfo clone() throws CloneNotSupportedException {
        return (ID3MusicInfo) super.clone();
    }

    @NonNull
    @Override
    public String toString() {
        return "ID3MusicInfo{" +
                "title=" + title +
                ", artist='" + artist + '\'' +
                ", album=" + album +
                ", number=" + number +
                ", total=" + total +
                ", totalTime=" + totalTime +
                ", genre='" + genre + '\'' +
                ", currentTime='" + currentTime + '\'' +
                ", playStatus='" + playStatus + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeInt(number);
        dest.writeInt(total);
        dest.writeInt(totalTime);
        dest.writeString(genre);
        dest.writeInt(currentTime);
        dest.writeByte((byte) (playStatus ? 1 : 0));
    }
}
