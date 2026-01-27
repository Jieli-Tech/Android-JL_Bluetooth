package com.jieli.btsmart.tool.room.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.TranslationSession;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * TranslationDao
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译记录数据库操作
 * @since 2025/6/9
 */
@Dao
public interface TranslationDao {

    /**
     * 插入翻译会议信息
     *
     * @param session TranslationSession 翻译会议信息
     * @return int 插入记录ID
     */
    @Insert(entity = TranslationSession.class, onConflict = OnConflictStrategy.REPLACE)
    Long insertTranslationSession(TranslationSession session);

    /**
     * 删除翻译会议信息
     *
     * @param session TranslationSession 翻译会议信息
     * @return int 记录行数
     */
    @Delete(entity = TranslationSession.class)
    int deleteTranslationSession(TranslationSession session);

    /**
     * 根据会议ID删除翻译会议信息
     *
     * @param id int 翻译会议ID
     * @return int 记录行数
     */
    @Query("DELETE FROM TranslationSession WHERE id = :id")
    int deleteTranslationSessionById(int id);

    /**
     * 更新翻译会议信息
     *
     * @param session TranslationSession 翻译会议信息
     * @return int 记录行数
     */
    @Update(entity = TranslationSession.class)
    int updateTranslationSession(TranslationSession session);

    /**
     * 根据会议ID查询翻译会议信息
     *
     * @param id int 会议ID
     * @return TranslationSession 翻译会议信息
     */
    @Query("SELECT * FROM TranslationSession WHERE id = :id LIMIT 1")
    TranslationSession queryTranslationSessionById(int id);

    /**
     * 查询指定时间段内所有翻译模式的会议信息
     *
     * @param startTime long 开始时间戳
     * @param endTime   long 结束时间戳
     * @return List\<TranslationSession\> 翻译会议信息列表
     */
    @Transaction
    @Query("SELECT * FROM TranslationSession WHERE startTime >= :startTime AND endTime <= :endTime ORDER BY startTime DESC")
    List<TranslationSession> queryTranslationSessionsByTime(long startTime, long endTime);

    /**
     * 查询指定时间段内指定翻译模式的会议信息
     *
     * @param mode      int   翻译模式
     * @param startTime long  开始时间戳
     * @param endTime   long  结束时间戳
     * @return List\<TranslationSession\> 翻译会议信息列表
     */
    @Transaction
    @Query("SELECT * FROM TranslationSession WHERE translationMode = :mode and startTime >= :startTime AND endTime <= :endTime ORDER BY startTime DESC")
    List<TranslationSession> queryTranslationSessionsByTime(int mode, long startTime, long endTime);

    /**
     * 插入翻译记录
     *
     * @param record TranslationRecord 翻译记录
     * @return int 插入记录ID
     */
    @Insert(entity = TranslationRecord.class, onConflict = OnConflictStrategy.REPLACE)
    Long insertTranslationRecord(TranslationRecord record);

    /**
     * 删除翻译记录
     *
     * @param record TranslationRecord 翻译记录
     * @return int 记录行数
     */
    @Delete(entity = TranslationRecord.class)
    int deleteTranslationRecord(TranslationRecord record);

    /**
     * 删除指定会议ID的所有翻译记录
     *
     * @param sessionId int 会议ID
     * @return int 记录行数
     */
    @Query("DELETE FROM TranslationRecord WHERE sessionId = :sessionId")
    int deleteRecordListBySessionId(int sessionId);

    /**
     * 更新翻译记录
     *
     * @param record TranslationRecord 翻译记录
     * @return int 记录行数
     */
    @Update(entity = TranslationRecord.class)
    int updateTranslationRecord(TranslationRecord record);

    /**
     * 根据翻译记录ID查询翻译记录
     *
     * @param recordId int 翻译记录ID
     * @return TranslationRecord 翻译记录
     */
    @Query("SELECT * FROM translationrecord WHERE id = :recordId LIMIT 1")
    TranslationRecord queryTranslationRecordById(int recordId);

    /**
     * 根据会议ID查询翻译记录
     *
     * @param sessionId int 会议ID
     * @return List\<TranslationRecord\> 翻译记录列表
     */
    @Transaction
    @Query("SELECT * FROM translationrecord WHERE sessionId = :sessionId ORDER BY updateTime ASC")
    List<TranslationRecord> queryTranslationRecordBySessionId(int sessionId);

    /**
     * 根据会议ID查询翻译记录(取N项)
     *
     * @param sessionId int 会议ID
     * @param limit     int 数量限制
     * @return List\<TranslationRecord\> 翻译记录列表
     */
    @Transaction
    @Query("SELECT * FROM translationrecord WHERE sessionId = :sessionId ORDER BY updateTime ASC LIMIT :limit")
    List<TranslationRecord> queryTranslationRecordBySessionId(int sessionId, int limit);

    /**
     * 根据会议ID删除翻译记录和会议信息
     *
     * @param sessionId int 会议ID
     * @return boolean 结果
     */
    @Transaction
    default boolean deleteSessionAndRecordBySessionId(int sessionId) {
        deleteRecordListBySessionId(sessionId); //删除所有翻译记录
        int count = deleteTranslationSessionById(sessionId); //删除会议信息
        return count > 0;
    }

    /**
     * 根据会议ID查询完整的翻译会议记录
     *
     * @param sessionId int 会议ID
     * @return TranslationSessionRecord 翻译会议记录
     */
    @Transaction
    default TranslationSessionRecord querySessionRecordBySessionId(int sessionId) {
        TranslationSession session = queryTranslationSessionById(sessionId);
        if (null == session) return null;
        List<TranslationRecord> records = queryTranslationRecordBySessionId(sessionId);
        if (null == records) {
            records = new ArrayList<>();
        }
        return new TranslationSessionRecord(session, records);
    }

    /**
     * 查询指定翻译模式在特定时间段的会议记录
     *
     * @param mode             int 翻译模式<p> -1 表示所有翻译模式</p>
     * @param startTime        long 开始时间戳
     * @param endTime          long 结束时间戳
     * @param limitRecordCount int 翻译记录个数限制<p>大于0为限制个数，否则为不限制</p>
     * @return List\<TranslationSessionRecord\> 会议记录列表
     */
    @Transaction
    default List<TranslationSessionRecord> querySessionRecordListByTime(int mode, long startTime, long endTime, int limitRecordCount) {
        if (startTime > endTime) return new ArrayList<>();
        List<TranslationSession> sessions;
        if (mode == -1) {
            sessions = queryTranslationSessionsByTime(startTime, endTime);
        } else {
            sessions = queryTranslationSessionsByTime(mode, startTime, endTime);
        }
        if (null == sessions || sessions.isEmpty()) return new ArrayList<>(); //缺少会议记录
        List<TranslationSessionRecord> list = new ArrayList<>();
        for (TranslationSession session : sessions) {
            List<TranslationRecord> records;
            if (limitRecordCount > 0) {
                records = queryTranslationRecordBySessionId(session.getId(), limitRecordCount);
            } else {
                records = queryTranslationRecordBySessionId(session.getId());
            }
            if (null == records || records.isEmpty()) continue; //缺少有效记录
            list.add(new TranslationSessionRecord(session, records));
        }
        return list;
    }
}
