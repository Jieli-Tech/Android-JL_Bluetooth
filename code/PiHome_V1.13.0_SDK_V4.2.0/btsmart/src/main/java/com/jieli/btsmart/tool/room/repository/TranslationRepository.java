package com.jieli.btsmart.tool.room.repository;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.TranslationSession;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;
import com.jieli.btsmart.tool.room.AppDatabase;
import com.jieli.btsmart.tool.room.dao.TranslationDao;
import com.jieli.component.utils.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * TranslationRepository
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译功能存储器
 * @since 2025/8/12
 */
public class TranslationRepository {

    private static final String TAG = TranslationRepository.class.getSimpleName();
    /**
     * 单例对象
     */
    private static volatile TranslationRepository instance;
    /**
     * 翻译记录操作
     */
    @NonNull
    private final TranslationDao mDao;
    /**
     * 任务线程池
     */
    private final ExecutorService service;


    private TranslationRepository(@NonNull AppDatabase database) {
        mDao = database.translationDao();
        service = Executors.newSingleThreadExecutor();
    }

    public static TranslationRepository getInstance() {
        if (null == instance) {
            synchronized (TranslationRepository.class) {
                if (null == instance) {
                    instance = new TranslationRepository(AppDatabase.getInstance());
                }
            }
        }
        return instance;
    }

    @NonNull
    public TranslationDao getDao() {
        return mDao;
    }

    public void addTranslationSession(@NonNull TranslationSession session, Consumer<Integer> callback) {
        executeTask(() -> {
            final Long id = mDao.insertTranslationSession(session);
            int sessionId = id == null ? 0 : id.intValue();
            if (null != callback) {
                callback.accept(sessionId);
            }
        });
    }

    public void removeTranslationSession(int sessionId, Consumer<Boolean> callback) {
        executeTask(() -> {
            if (null != callback) {
                callback.accept(removeSessionRecord(sessionId));
            }
        });
    }

    public void removeTranslationSession(@NonNull List<Integer> sessionIds, Consumer<Map<Integer, Boolean>> callback) {
        if (sessionIds.isEmpty()) {
            if (null != callback) {
                callback.accept(new HashMap<>());
            }
            return;
        }
        executeTask(() -> {
            HashMap<Integer, Boolean> map = new HashMap<>();
            for (Integer id : sessionIds) {
                boolean ret = removeSessionRecord(id);
                map.put(id, ret);
            }
            if (null != callback) {
                callback.accept(map);
            }
        });
    }

    public void updateTranslationSession(@NonNull TranslationSession session, Consumer<Boolean> callback) {
        executeTask(() -> {
            int count = mDao.updateTranslationSession(session);
            if (null != callback) {
                callback.accept(count > 0);
            }
        });
    }

    public void updateSessionEndTime(int sessionId, long endTime, Consumer<Boolean> callback) {
        executeTask(() -> {
            final TranslationSession session = mDao.queryTranslationSessionById(sessionId);
            if (null == session) {
                if (null != callback) {
                    callback.accept(false);
                }
                return;
            }
            session.setEndTime(endTime);
            int count = mDao.updateTranslationSession(session);
            if (null != callback) {
                callback.accept(count > 0);
            }
        });
    }

    public void queryTranslationSession(int sessionId, Consumer<TranslationSession> callback) {
        executeTask(() -> {
            TranslationSession session = mDao.queryTranslationSessionById(sessionId);
            if (null != callback) {
                callback.accept(session);
            }
        });
    }

    public void queryTranslationSessions(int mode, long startTime, long endTime, Consumer<List<TranslationSession>> callback) {
        executeTask(() -> {
            List<TranslationSession> list;
            if (mode == 0xFF) {
                list = mDao.queryTranslationSessionsByTime(startTime, endTime);
            } else {
                list = mDao.queryTranslationSessionsByTime(mode, startTime, endTime);
            }
            if (null == list) {
                list = new ArrayList<>();
            }
            if (null != callback) {
                callback.accept(list);
            }
        });
    }

    public void addTranslationRecord(@NonNull TranslationRecord record, Consumer<Integer> callback) {
        executeTask(() -> {
            Long id = mDao.insertTranslationRecord(record);
            int recordId = id == null ? 0 : id.intValue();
            if (null != callback) {
                callback.accept(recordId);
            }
        });
    }

    public void removeTranslationRecord(@NonNull TranslationRecord record, Consumer<Boolean> callback) {
        executeTask(() -> {
            int count = mDao.deleteTranslationRecord(record);
            if (null != callback) {
                callback.accept(count > 0);
            }
        });
    }

    public void updateTranslationRecord(@NonNull TranslationRecord record, Consumer<Boolean> callback) {
        executeTask(() -> {
            int count = mDao.updateTranslationRecord(record);
            if (null != callback) {
                callback.accept(count > 0);
            }
        });
    }

    public void queryTranslationRecords(int sessionId, int limit, Consumer<List<TranslationRecord>> callback) {
        executeTask(() -> {
            List<TranslationRecord> list;
            if (limit > 0) {
                list = mDao.queryTranslationRecordBySessionId(sessionId, limit);
            } else {
                list = mDao.queryTranslationRecordBySessionId(sessionId);
            }
            if (null == list) {
                list = new ArrayList<>();
            }
            if (null != callback) {
                callback.accept(list);
            }
        });
    }

    public void querySessionRecord(int sessionId, Consumer<TranslationSessionRecord> callback) {
        executeTask(() -> {
            TranslationSessionRecord sessionRecord = mDao.querySessionRecordBySessionId(sessionId);
            if (null != callback) {
                callback.accept(sessionRecord);
            }
        });
    }

    public void querySessionRecordByTime(int mode, long startTime, long endTime, int limit, Consumer<List<TranslationSessionRecord>> callback) {
        executeTask(() -> {
            List<TranslationSessionRecord> list = mDao.querySessionRecordListByTime(mode, startTime, endTime, limit);
            if (null != callback) {
                callback.accept(list);
            }
        });
    }

    private void executeTask(Runnable runnable) {
        if (null == runnable) return;
        if (null == service || service.isShutdown()) {
            JL_Log.i(TAG, "executeTask", "service is null.");
            return;
        }
        service.execute(runnable);
    }

    private boolean removeSessionRecord(int sessionId) {
        List<TranslationRecord> records = mDao.queryTranslationRecordBySessionId(sessionId);
        if (records != null) {
            for (TranslationRecord record : records) {
                String srcFilePath = record.getSrcFilePath();
                if (!TextUtils.isEmpty(srcFilePath)) {
                    FileUtil.deleteFile(new File(srcFilePath));
                }
                String destFilePath = record.getDestFilePath();
                if (!TextUtils.isEmpty(destFilePath)) {
                    FileUtil.deleteFile(new File(destFilePath));
                }
            }
        }
        boolean ret = mDao.deleteSessionAndRecordBySessionId(sessionId);
        JL_Log.d(TAG, "removeSessionRecord", "sessionId : " + sessionId + " ---> " + ret);
        return ret;
    }
}
