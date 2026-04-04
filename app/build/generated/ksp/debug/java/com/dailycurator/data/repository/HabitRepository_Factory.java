package com.dailycurator.data.repository;

import com.dailycurator.data.local.dao.HabitDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class HabitRepository_Factory implements Factory<HabitRepository> {
  private final Provider<HabitDao> daoProvider;

  public HabitRepository_Factory(Provider<HabitDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public HabitRepository get() {
    return newInstance(daoProvider.get());
  }

  public static HabitRepository_Factory create(Provider<HabitDao> daoProvider) {
    return new HabitRepository_Factory(daoProvider);
  }

  public static HabitRepository newInstance(HabitDao dao) {
    return new HabitRepository(dao);
  }
}
