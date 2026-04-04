package com.dailycurator.ui.screens.today;

import com.dailycurator.data.repository.GoalRepository;
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
public final class TodayViewModel_Factory implements Factory<TodayViewModel> {
  private final Provider<TaskRepository> taskRepoProvider;

  private final Provider<GoalRepository> goalRepoProvider;

  public TodayViewModel_Factory(Provider<TaskRepository> taskRepoProvider,
      Provider<GoalRepository> goalRepoProvider) {
    this.taskRepoProvider = taskRepoProvider;
    this.goalRepoProvider = goalRepoProvider;
  }

  @Override
  public TodayViewModel get() {
    return newInstance(taskRepoProvider.get(), goalRepoProvider.get());
  }

  public static TodayViewModel_Factory create(Provider<TaskRepository> taskRepoProvider,
      Provider<GoalRepository> goalRepoProvider) {
    return new TodayViewModel_Factory(taskRepoProvider, goalRepoProvider);
  }

  public static TodayViewModel newInstance(TaskRepository taskRepo, GoalRepository goalRepo) {
    return new TodayViewModel(taskRepo, goalRepo);
  }
}
