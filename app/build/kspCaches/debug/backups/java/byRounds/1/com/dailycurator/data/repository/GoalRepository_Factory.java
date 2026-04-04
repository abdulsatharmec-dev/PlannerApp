package com.dailycurator.data.repository;

import com.dailycurator.data.local.dao.GoalDao;
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
public final class GoalRepository_Factory implements Factory<GoalRepository> {
  private final Provider<GoalDao> daoProvider;

  public GoalRepository_Factory(Provider<GoalDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public GoalRepository get() {
    return newInstance(daoProvider.get());
  }

  public static GoalRepository_Factory create(Provider<GoalDao> daoProvider) {
    return new GoalRepository_Factory(daoProvider);
  }

  public static GoalRepository newInstance(GoalDao dao) {
    return new GoalRepository(dao);
  }
}
