package com.dailycurator.ui.screens.habits;

import com.dailycurator.data.repository.HabitRepository;
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
public final class HabitsViewModel_Factory implements Factory<HabitsViewModel> {
  private final Provider<HabitRepository> repoProvider;

  public HabitsViewModel_Factory(Provider<HabitRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public HabitsViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static HabitsViewModel_Factory create(Provider<HabitRepository> repoProvider) {
    return new HabitsViewModel_Factory(repoProvider);
  }

  public static HabitsViewModel newInstance(HabitRepository repo) {
    return new HabitsViewModel(repo);
  }
}
