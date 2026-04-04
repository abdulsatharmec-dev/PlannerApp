package com.dailycurator.ui.screens.goals;

import com.dailycurator.data.repository.GoalRepository;
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
public final class GoalsViewModel_Factory implements Factory<GoalsViewModel> {
  private final Provider<GoalRepository> repoProvider;

  public GoalsViewModel_Factory(Provider<GoalRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public GoalsViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static GoalsViewModel_Factory create(Provider<GoalRepository> repoProvider) {
    return new GoalsViewModel_Factory(repoProvider);
  }

  public static GoalsViewModel newInstance(GoalRepository repo) {
    return new GoalsViewModel(repo);
  }
}
