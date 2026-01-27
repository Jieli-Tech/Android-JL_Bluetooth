package com.jieli.btsmart.data.model.music;

public class SongInfoData {
    private int index;
    private String songName;
    private String songAuthor;
    private String songPath;
    private int songDuration;
    private long songZize;

    public int getIndex() {
        return index;
    }

    public void setIndex(int indexa) {
        this.index = indexa;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getSongAuthor() {
        return songAuthor;
    }

    public void setSongAuthor(String songAuthor) {
        this.songAuthor = songAuthor;
    }

    public String getSongPath() {
        return songPath;
    }

    public void setSongPath(String songPath) {
        this.songPath = songPath;
    }

    public int getSongDuration() {
        return songDuration;
    }

    public void setSongDuration(int songDuration) {
        this.songDuration = songDuration;
    }

    public long getSongZize() {
        return songZize;
    }

    public void setSongZize(long songZize) {
        this.songZize = songZize;
    }
}
