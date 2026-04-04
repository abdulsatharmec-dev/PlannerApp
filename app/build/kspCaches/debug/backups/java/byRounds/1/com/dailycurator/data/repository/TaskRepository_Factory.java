package com.dailycurator.data.repository;

import com.dailycurator.data.local.dao.TaskDao;
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
public final class TaskRepository_Factory implements Factory<TaskRepository> {
  private final Provider<TaskDao> daoProvider;

  public TaskRepository_Factory(Provider<TaskDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public TaskRepository get() {
    return newInstance(daoProvider.get());
  }

  public static TaskRepository_Factory create(Provider<TaskDao> daoProvider) {
    return new TaskRepository_Factory(daoProvider);
  }

  public static TaskRepository newInstance(TaskDao dao) {
    return new TaskRepository(dao);
  }
}
