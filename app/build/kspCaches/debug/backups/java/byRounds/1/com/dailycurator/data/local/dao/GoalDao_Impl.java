package com.dailycurator.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.dailycurator.data.local.entity.GoalEntity;
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
public final class GoalDao_Impl implements GoalDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GoalEntity> __insertionAdapterOfGoalEntity;

  private final EntityDeletionOrUpdateAdapter<GoalEntity> __deletionAdapterOfGoalEntity;

  private final EntityDeletionOrUpdateAdapter<GoalEntity> __updateAdapterOfGoalEntity;

  private final SharedSQLiteStatement __preparedStmtOfSetCompleted;

  public GoalDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGoalEntity = new EntityInsertionAdapter<GoalEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `goals` (`id`,`title`,`description`,`deadline`,`timeEstimate`,`category`,`isCompleted`,`weekStart`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GoalEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        if (entity.getDeadline() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDeadline());
        }
        if (entity.getTimeEstimate() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getTimeEstimate());
        }
        statement.bindString(6, entity.getCategory());
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindString(8, entity.getWeekStart());
      }
    };
    this.__deletionAdapterOfGoalEntity = new EntityDeletionOrUpdateAdapter<GoalEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `goals` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GoalEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfGoalEntity = new EntityDeletionOrUpdateAdapter<GoalEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `goals` SET `id` = ?,`title` = ?,`description` = ?,`deadline` = ?,`timeEstimate` = ?,`category` = ?,`isCompleted` = ?,`weekStart` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GoalEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        if (entity.getDeadline() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDeadline());
        }
        if (entity.getTimeEstimate() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getTimeEstimate());
        }
        statement.bindString(6, entity.getCategory());
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindString(8, entity.getWeekStart());
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfSetCompleted = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE goals SET isCompleted = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final GoalEntity goal, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfGoalEntity.insertAndReturnId(goal);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final GoalEntity goal, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfGoalEntity.handle(goal);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final GoalEntity goal, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfGoalEntity.handle(goal);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object setCompleted(final long id, final boolean completed,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetCompleted.acquire();
        int _argIndex = 1;
        final int _tmp = completed ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetCompleted.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GoalEntity>> getGoalsForWeek(final String weekStart) {
    final String _sql = "SELECT * FROM goals WHERE weekStart = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, weekStart);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"goals"}, new Callable<List<GoalEntity>>() {
      @Override
      @NonNull
      public List<GoalEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfDeadline = CursorUtil.getColumnIndexOrThrow(_cursor, "deadline");
          final int _cursorIndexOfTimeEstimate = CursorUtil.getColumnIndexOrThrow(_cursor, "timeEstimate");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfWeekStart = CursorUtil.getColumnIndexOrThrow(_cursor, "weekStart");
          final List<GoalEntity> _result = new ArrayList<GoalEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GoalEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpDeadline;
            if (_cursor.isNull(_cursorIndexOfDeadline)) {
              _tmpDeadline = null;
            } else {
              _tmpDeadline = _cursor.getString(_cursorIndexOfDeadline);
            }
            final String _tmpTimeEstimate;
            if (_cursor.isNull(_cursorIndexOfTimeEstimate)) {
              _tmpTimeEstimate = null;
            } else {
              _tmpTimeEstimate = _cursor.getString(_cursorIndexOfTimeEstimate);
            }
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final String _tmpWeekStart;
            _tmpWeekStart = _cursor.getString(_cursorIndexOfWeekStart);
            _item = new GoalEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpDeadline,_tmpTimeEstimate,_tmpCategory,_tmpIsCompleted,_tmpWeekStart);
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
