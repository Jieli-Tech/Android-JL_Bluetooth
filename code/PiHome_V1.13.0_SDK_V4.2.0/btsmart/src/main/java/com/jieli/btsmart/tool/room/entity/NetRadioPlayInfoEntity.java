package com.jieli.btsmart.tool.room.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jieli.audio.media_player.Music;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/11 15:29
 * @desc :当前播放的电台列表信息
 */
@Entity
public class NetRadioPlayInfoEntity {
    @NonNull
    public String radioInfoId;

    @NonNull
    public String getRadioInfoId() {
        return radioInfoId;
    }

    public void setRadioInfoId(@NonNull String radioInfoId) {
        this.radioInfoId = radioInfoId;
    }

    private long id;
    @PrimaryKey
    @NonNull
    private String title;
    private String album;
    private int duration;
    private long size;
    private String artist;
    private String url;
    private String coverUrl;
    private boolean selected;
    private boolean collect;
    private int local; //0:本地 ,1:网络 ,5:图灵H5类型,2.短音频 3:m3u8
    private int download;   //    1：未下载，2：已下载,3：正在下载
    private int listType;//歌单类型 1：本地 2：国家 3：省市 4：收藏
    private String auth;
    private int position = 0;

    public void setDownload(int download) {
        this.download = download;
    }

    public int getDownload() {
        return download;
    }

    public void setLocal(int local) {
        this.local = local;
    }

    public int getLocal() {
        return local;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isCollect() {
        return collect;
    }

    public void setCollect(boolean collect) {
        this.collect = collect;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String getAuth() {
        return auth;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public int getListType() {
        return listType;
    }

    public void setListType(int listType) {
        this.listType = listType;
    }

    @Override
    public String toString() {
        return "Music{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", album='" + album + '\'' +
                ", duration=" + duration +
                ", size=" + size +
                ", artist='" + artist + '\'' +
                ", url='" + url + '\'' +
                ", coverUrl='" + coverUrl + '\'' +
                ", selected=" + selected +
                ", collect=" + collect +
                ", download=" + download +
                ", local=" + local +
                ", auth=" + auth +
                ", position=" + position +
                '}';
    }
}
