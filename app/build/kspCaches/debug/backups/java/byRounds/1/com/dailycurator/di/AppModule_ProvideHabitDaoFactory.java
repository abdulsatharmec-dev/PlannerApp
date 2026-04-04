package com.dailycurator.di;

import com.dailycurator.data.local.AppDatabase;
import com.dailycurator.data.local.dao.HabitDao;
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
public final class AppModule_ProvideHabitDaoFactory implements Factory<HabitDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideHabitDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public HabitDao get() {
    return provideHabitDao(dbProvider.get());
  }

  public static AppModule_ProvideHabitDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideHabitDaoFactory(dbProvider);
  }

  public static HabitDao provideHabitDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideHabitDao(db));
  }
}
