package com.dailycurator.ui.screens.tasks;

import com.dailycurator.data.repository.TaskRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class TasksViewModel_Factory implements Factory<TasksViewModel> {
  private final Provider<TaskRepository> repoProvider;

  public TasksViewModel_Factory(Provider<TaskRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public TasksViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static TasksViewModel_Factory create(Provider<TaskRepository> repoProvider) {
    return new TasksViewModel_Factory(repoProvider);
  }

  public static TasksViewModel newInstance(TaskRepository repo) {
    return new TasksViewModel(repo);
  }
}
