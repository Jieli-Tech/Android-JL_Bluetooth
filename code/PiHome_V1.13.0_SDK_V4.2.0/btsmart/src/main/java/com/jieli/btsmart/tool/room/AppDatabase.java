package com.jieli.btsmart.tool.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.TranslationSession;
import com.jieli.btsmart.tool.room.dao.FMCollectInfoDao;
import com.jieli.btsmart.tool.room.dao.FittingRecordDao;
import com.jieli.btsmart.tool.room.dao.NetRadioInfoDao;
import com.jieli.btsmart.tool.room.dao.TranslationDao;
import com.jieli.btsmart.tool.room.dao.UserDao;
import com.jieli.btsmart.tool.room.entity.FMCollectInfoEntity;
import com.jieli.btsmart.tool.room.entity.HearingAidFittingRecordEntity;
import com.jieli.btsmart.tool.room.entity.NetRadioCollectAndUserEntity;
import com.jieli.btsmart.tool.room.entity.NetRadioInfoEntity;
import com.jieli.btsmart.tool.room.entity.NetRadioPlayInfoEntity;
import com.jieli.btsmart.tool.room.entity.UserEntity;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/1 15:42
 * @desc :
 */
@Database(entities = {FMCollectInfoEntity.class, NetRadioInfoEntity.class, UserEntity.class,
        NetRadioCollectAndUserEntity.class, NetRadioPlayInfoEntity.class, HearingAidFittingRecordEntity.class,
        TranslationSession.class, TranslationRecord.class}, version = 5)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase sInstance;
    private static ExecutorService sWorkThread;

    public abstract FMCollectInfoDao fmCollectInfoDao();

    public abstract UserDao userDao();

    public abstract NetRadioInfoDao netRadioInfoDao();

    public abstract FittingRecordDao fittingRecordDao();

    public abstract TranslationDao translationDao();

    private final MutableLiveData<Boolean> mIsDatabaseCreated = new MutableLiveData<>();

    public static final String DATABASE_NAME = "jieli-home.db";

    public static AppDatabase getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (AppDatabase.class) {
                if (sInstance == null) {
                    sWorkThread = Executors.newSingleThreadExecutor();
                    sInstance = buildDatabase(context, sWorkThread);
                    sInstance.updateDatabaseCreated(context);
                }
            }
        }
        return sInstance;
    }

    public static AppDatabase getInstance() {
        return sInstance;
    }

    private static AppDatabase buildDatabase(Context context, Executor executor) {
        return Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME)
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build();
    }

    private void updateDatabaseCreated(final Context context) {
        if (context.getDatabasePath(DATABASE_NAME).exists()) {
            setDatabaseCreated();
        }
    }

    private void setDatabaseCreated() {
        mIsDatabaseCreated.postValue(true);
    }

    public LiveData<Boolean> getDatabaseCreated() {
        return mIsDatabaseCreated;
    }

    public ExecutorService getWeakExecutor() {
        return sWorkThread;
    }

    /**
     * 以后要标记数据库版本对应的apk版本方便测试
     * 数据库版本 2->3 NetRadioPlayInfoEntity表格新增了listType列,
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE NetRadioPlayInfoEntity ADD COLUMN listType integer NOT NULL DEFAULT -1");
        }
    };
    /**
     * 数据库版本 3->4 增加了HearingAidFittingRecordEntity 表
     * 增加NetRadioCollectAndUserEntity表的radioInfoId 索引
     */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `tb_fitting_record` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordKey` TEXT, `recordName` TEXT, `time` INTEGER NOT NULL, `version` INTEGER NOT NULL, `channelsNum` INTEGER NOT NULL, `gainsType` INTEGER NOT NULL, `channelsFreqs` TEXT, `leftChannelsValues` TEXT, `rightChannelsValues` TEXT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_NetRadioCollectAndUserEntity_radioInfoId` ON `NetRadioCollectAndUserEntity` (`radioInfoId`)");
        }
    };

    /**
     * 数据库版本 4->5
     * 增加了TranslationSession表 和 TranslationRecord表
     */
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `TranslationSession` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mac` TEXT NOT NULL, `translationMode` INTEGER NOT NULL, `title` TEXT, `startTime` LONG NOT NULL, `endTime` LONG NOT NULL)");
            database.execSQL("CREATE TABLE IF NOT EXISTS `TranslationRecord` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mac` TEXT NOT NULL, `sessionId` INTEGER NOT NULL, `role` INTEGER NOT NULL, `nikeName` TEXT, `srcText` TEXT NOT NULL, `srcLanguage` TEXT, `srcFilePath` TEXT, `srcFileDuration` INTEGER, `destText` TEXT NOT NULL, `destLanguage` TEXT, `destFilePath` TEXT, `destFileDuration` INTEGER, `updateTime` LONG NOT NULL)");
        }
    };
}
