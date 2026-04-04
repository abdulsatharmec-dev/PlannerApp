package com.dailycurator.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.dailycurator.data.local.entity.HabitEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HabitDao_Impl implements HabitDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HabitEntity> __insertionAdapterOfHabitEntity;

  private final EntityDeletionOrUpdateAdapter<HabitEntity> __deletionAdapterOfHabitEntity;

  private final EntityDeletionOrUpdateAdapter<HabitEntity> __updateAdapterOfHabitEntity;

  public HabitDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHabitEntity = new EntityInsertionAdapter<HabitEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `habits` (`id`,`name`,`category`,`habitType`,`iconEmoji`,`currentValue`,`targetValue`,`unit`,`streakDays`,`date`,`isDone`,`doneNote`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HabitEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getCategory());
        statement.bindString(4, entity.getHabitType());
        statement.bindString(5, entity.getIconEmoji());
        statement.bindDouble(6, entity.getCurrentValue());
        statement.bindDouble(7, entity.getTargetValue());
        statement.bindString(8, entity.getUnit());
        statement.bindLong(9, entity.getStreakDays());
        statement.bindString(10, entity.getDate());
        final int _tmp = entity.isDone() ? 1 : 0;
        statement.bindLong(11, _tmp);
        if (entity.getDoneNote() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getDoneNote());
        }
      }
    };
    this.__deletionAdapterOfHabitEntity = new EntityDeletionOrUpdateAdapter<HabitEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `habits` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HabitEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfHabitEntity = new EntityDeletionOrUpdateAdapter<HabitEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `habits` SET `id` = ?,`name` = ?,`category` = ?,`habitType` = ?,`iconEmoji` = ?,`currentValue` = ?,`targetValue` = ?,`unit` = ?,`streakDays` = ?,`date` = ?,`isDone` = ?,`doneNote` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HabitEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getCategory());
        statement.bindString(4, entity.getHabitType());
        statement.bindString(5, entity.getIconEmoji());
        statement.bindDouble(6, entity.getCurrentValue());
        statement.bindDouble(7, entity.getTargetValue());
        statement.bindString(8, entity.getUnit());
        statement.bindLong(9, entity.getStreakDays());
        statement.bindString(10, entity.getDate());
        final int _tmp = entity.isDone() ? 1 : 0;
        statement.bindLong(11, _tmp);
        if (entity.getDoneNote() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getDoneNote());
        }
        statement.bindLong(13, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final HabitEntity habit, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfHabitEntity.insertAndReturnId(habit);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final HabitEntity habit, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfHabitEntity.handle(habit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final HabitEntity habit, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfHabitEntity.handle(habit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HabitEntity>> getHabitsForDate(final String date) {
    final String _sql = "SELECT * FROM habits WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habits"}, new Callable<List<HabitEntity>>() {
      @Override
      @NonNull
      public List<HabitEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfHabitType = CursorUtil.getColumnIndexOrThrow(_cursor, "habitType");
          final int _cursorIndexOfIconEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "iconEmoji");
          final int _cursorIndexOfCurrentValue = CursorUtil.getColumnIndexOrThrow(_cursor, "currentValue");
          final int _cursorIndexOfTargetValue = CursorUtil.getColumnIndexOrThrow(_cursor, "targetValue");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfStreakDays = CursorUtil.getColumnIndexOrThrow(_cursor, "streakDays");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfIsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDone");
          final int _cursorIndexOfDoneNote = CursorUtil.getColumnIndexOrThrow(_cursor, "doneNote");
          final List<HabitEntity> _result = new ArrayList<HabitEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpHabitType;
            _tmpHabitType = _cursor.getString(_cursorIndexOfHabitType);
            final String _tmpIconEmoji;
            _tmpIconEmoji = _cursor.getString(_cursorIndexOfIconEmoji);
            final float _tmpCurrentValue;
            _tmpCurrentValue = _cursor.getFloat(_cursorIndexOfCurrentValue);
            final float _tmpTargetValue;
            _tmpTargetValue = _cursor.getFloat(_cursorIndexOfTargetValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final int _tmpStreakDays;
            _tmpStreakDays = _cursor.getInt(_cursorIndexOfStreakDays);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final boolean _tmpIsDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp != 0;
            final String _tmpDoneNote;
            if (_cursor.isNull(_cursorIndexOfDoneNote)) {
              _tmpDoneNote = null;
            } else {
              _tmpDoneNote = _cursor.getString(_cursorIndexOfDoneNote);
            }
            _item = new HabitEntity(_tmpId,_tmpName,_tmpCategory,_tmpHabitType,_tmpIconEmoji,_tmpCurrentValue,_tmpTargetValue,_tmpUnit,_tmpStreakDays,_tmpDate,_tmpIsDone,_tmpDoneNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
