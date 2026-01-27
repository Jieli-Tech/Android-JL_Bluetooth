package com.jieli.btsmart.data.model.translation;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TranslationSessionRecord
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译会议记录
 * @since 2025/6/16
 */
public class TranslationSessionRecord implements Parcelable {
    /**
     * 原文类型
     */
    public static final int TYPE_SRC_TEXT = 0;
    /**
     * 译文类型
     */
    public static final int TYPE_DEST_TEXT = 1;

    /**
     * 翻译会议主题
     */
    @NonNull
    private final TranslationSession session;
    /**
     * 翻译记录列表
     */
    @NonNull
    private final List<TranslationRecord> records;

    public TranslationSessionRecord(@NonNull TranslationSession session) {
        this(session, new ArrayList<>());
    }

    public TranslationSessionRecord(@NonNull TranslationSession session, @NonNull List<TranslationRecord> recordList) {
        this.session = session;
        records = recordList;
    }

    protected TranslationSessionRecord(Parcel in) {
        session = Objects.requireNonNull(in.readParcelable(TranslationSession.class.getClassLoader()));
        records = Objects.requireNonNull(in.createTypedArrayList(TranslationRecord.CREATOR));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(session, flags);
        dest.writeTypedList(records);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TranslationSessionRecord> CREATOR = new Creator<TranslationSessionRecord>() {
        @Override
        public TranslationSessionRecord createFromParcel(Parcel in) {
            return new TranslationSessionRecord(in);
        }

        @Override
        public TranslationSessionRecord[] newArray(int size) {
            return new TranslationSessionRecord[size];
        }
    };

    @NonNull
    public TranslationSession getSession() {
        return session;
    }

    @NonNull
    public List<TranslationRecord> getRecords() {
        return records;
    }

    public int getRecordSize(){
        return records.size();
    }

    public int getDuration(int type) {
        if (records.isEmpty()) return 0;
        int duration = 0;
        for (TranslationRecord record : records) {
            if (type == TYPE_DEST_TEXT) {
                duration += record.getDestFileDuration();
            } else {
                duration += record.getSrcFileDuration();
            }
        }
        return duration;
    }

    public String getText(int type) {
        if (records.isEmpty()) return "";
        StringBuilder text = new StringBuilder();
        for (TranslationRecord record : records) {
            if (type == TYPE_DEST_TEXT) {
                text.append(record.getDestText());
            } else {
                text.append(record.getSrcText());
            }
        }
        return text.toString();
    }

    public TranslationRecord findRecordByDuration(int type, int duration) {
        if (records.isEmpty()) return null;
        int progress = 0;
        int index = -1;
        for (int i = 0; i < records.size(); i++) {
            TranslationRecord record = records.get(i);
            if (type == TYPE_DEST_TEXT) {
                progress += record.getDestFileDuration();
            } else {
                progress += record.getSrcFileDuration();
            }
            if (progress >= duration) {
                index = i;
                break;
            }
        }
        return index == -1 ? null : records.get(index);
    }

    public int getStartTimeByRecord(int type, TranslationRecord record) {
        if (null == record || records.isEmpty()) return 0;
        int startTime = 0;
        for (TranslationRecord item : records) {
            if (item.equals(record)) break;
            if (type == TYPE_DEST_TEXT) {
                startTime += item.getDestFileDuration();
            } else {
                startTime += item.getSrcFileDuration();
            }
        }
        return startTime;
    }

    public TranslationRecord getItem(int index) {
        if (records.isEmpty()) return null;
        if (index < 0 || index >= records.size()) return null;
        return records.get(index);
    }

    public int findPosition(TranslationRecord record) {
        if (null == record || records.isEmpty()) return -1;
        for (int i = 0; i < records.size(); i++) {
            final TranslationRecord item = records.get(i);
            if (item.equals(record)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return "SessionRecord{" +
                "session=" + session +
                ",\n records=" + records +
                '}';
    }
}
