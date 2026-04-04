package com.dailycurator.di;

import com.dailycurator.data.local.AppDatabase;
import com.dailycurator.data.local.dao.GoalDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class AppModule_ProvideGoalDaoFactory implements Factory<GoalDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideGoalDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public GoalDao get() {
    return provideGoalDao(dbProvider.get());
  }

  public static AppModule_ProvideGoalDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideGoalDaoFactory(dbProvider);
  }

  public static GoalDao provideGoalDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGoalDao(db));
  }
}
