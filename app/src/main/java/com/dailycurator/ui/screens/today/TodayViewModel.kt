package com.dailycurator.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailycurator.data.local.AppPreferences
import com.dailycurator.data.local.HomeLayoutSection
import com.dailycurator.data.local.HomePdfViewMode
import com.dailycurator.data.media.MorningClip
import com.dailycurator.data.media.MorningPlaylistPref
import com.dailycurator.data.media.MorningVideoBucket
import com.dailycurator.data.media.YoutubePlaylistVideoIdsFetcher
import com.dailycurator.data.media.YoutubeVideoIdExtractor
import com.dailycurator.data.model.*
import com.dailycurator.data.repository.GoalRepository
import com.dailycurator.data.repository.HabitRepository
import com.dailycurator.data.repository.GmailMailboxSummaryRepository
import com.dailycurator.data.repository.InsightCacheRepository
import com.dailycurator.data.pomodoro.PomodoroLaunchRequest
import com.dailycurator.data.pomodoro.PomodoroNavBridge
import com.dailycurator.data.repository.TaskRepository
import com.dailycurator.reminders.TaskReminderScheduler
import com.dailycurator.data.repository.toAiInsight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

private fun buildMorningClipsFlow(
    youtubeBlockFlow: Flow<String>,
    localUrisFlow: Flow<List<String>>,
    playlistEntriesFlow: Flow<List<MorningPlaylistPref>>,
    fetcher: YoutubePlaylistVideoIdsFetcher,
): Flow<List<MorningClip>> =
    combine(youtubeBlockFlow, localUrisFlow, playlistEntriesFlow) { y, l, p -> Triple(y, l, p) }
        .flatMapLatest { (youtubeBlock, locals, playlists) ->
            flow {
                val singles = YoutubeVideoIdExtractor.parseLines(youtubeBlock).mapIndexed { i, id ->
                    MorningClip("yt_$id", "YouTube ${i + 1}", id, null)
                }
                val localsClips = locals.mapIndexed { i, u ->
                    MorningClip("loc_${u.hashCode()}_$i", "Saved video ${i + 1}", null, u)
                }
                emit(singles + localsClips)
                if (playlists.isEmpty()) return@flow
                val playlistClips = withContext(Dispatchers.IO) {
                    buildList {
                        var n = 0
                        for ((pi, pl) in playlists.withIndex()) {
                            val ids = fetcher.fetchOrderedVideoIds(pl.playlistId)
                            val ex = pl.excludedVideoIds.toSet()
                            for (vid in ids) {
                                if (vid in ex) continue
                                n++
                                add(
                                    MorningClip(
                                        id = "yt_pl_${pl.playlistId}_${vid}_$n",
                                        label = "Playlist ${pi + 1} · clip $n",
                                        youtubeVideoId = vid,
                                        localUri = null,
                                    ),
                                )
                            }
                        }
                    }
                }
                emit(singles + playlistClips + localsClips)
            }
        }

data class TodayUiState(
    val tasks: List<PriorityTask> = emptyList(),
    /** Persisted tag label → chip background ARGB (same as Tasks screen). */
    val taskTagColors: Map<String, Int> = emptyMap(),
    val goals: List<WeeklyGoal> = emptyList(),
    val assistantInsight: AiInsight = AiInsight(
        insightText = "Insights will appear here after the first daily generation.",
        boldPart = "Assistant insight",
    ),
    val weeklyGoalsInsight: AiInsight = AiInsight(
        insightText = "Weekly goal coaching will appear after generation.",
        boldPart = "Weekly focus",
    ),
    val goalsCollapsed: Boolean = false,
    val assistantInsightEnabled: Boolean = true,
    val weeklyGoalsInsightEnabled: Boolean = true,
    val cerebrasConfigured: Boolean = false,
    val assistantInsightLoading: Boolean = false,
    val weeklyGoalsInsightLoading: Boolean = false,
    val dayWindowStart: LocalTime = LocalTime.of(4, 0),
    val dayWindowEnd: LocalTime = LocalTime.of(22, 0),
    val homeGmailSummaryEnabled: Boolean = false,
    /** Condensed markdown/text for the Home card (from cached mailbox summary). */
    val gmailHomeDigestMarkdown: String = "",
    /** Motivation bucket clips (Settings → Motivation). */
    val morningMotivationClips: List<MorningClip> = emptyList(),
    /** Spiritual bucket clips (Settings → Spiritual). */
    val morningSpiritualClips: List<MorningClip> = emptyList(),
    /** Which bucket the home card shows by default (Motivation). */
    val homeMorningVideoTab: MorningVideoBucket = MorningVideoBucket.MOTIVATION,
    /** When true, morning motivation players may start playback automatically (Settings). */
    val morningMotivationAutoplay: Boolean = false,
    /** Persisted order of major blocks on Today (home). */
    val homeLayoutOrder: List<HomeLayoutSection> = HomeLayoutSection.defaultOrder,
    /** Content URI for the optional daily PDF on Home (Settings). */
    val homeDailyPdfUri: String = "",
    /** Last visible PDF page index (0-based), persisted. */
    val homeDailyPdfLastPage: Int = 0,
    /** Continuous, single-page, or reading (text reflow) layout for the home PDF. */
    val homePdfViewMode: HomePdfViewMode = HomePdfViewMode.CONTINUOUS,
    /** PDF viewer uses its own light or dark palette (not the app scaffold theme). */
    val homePdfThemeDark: Boolean = false,
    /** Zoom for rendered pages and reading text size (0.5–3). */
    val homePdfZoomScale: Float = 1f,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val goalRepo: GoalRepository,
    private val habitRepo: HabitRepository,
    private val insightRepo: InsightCacheRepository,
    private val gmailMailboxSummaryRepo: GmailMailboxSummaryRepository,
    private val prefs: AppPreferences,
    private val youtubePlaylistVideoIdsFetcher: YoutubePlaylistVideoIdsFetcher,
    private val pomodoroNavBridge: PomodoroNavBridge,
    private val taskReminderScheduler: TaskReminderScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TodayUiState(
            cerebrasConfigured = prefs.isLlmConfigured(),
            homePdfThemeDark = prefs.isHomePdfThemeDark(),
            homePdfZoomScale = prefs.getHomePdfZoomScale(),
        ),
    )
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _detailGoalId = MutableStateFlow<Long?>(null)
    val openDetailGoalId: StateFlow<Long?> = _detailGoalId.asStateFlow()

    val goalDetailLinkedTasks: StateFlow<List<PriorityTask>> = _detailGoalId.flatMapLatest { id ->
        if (id == null || id <= 0L) flowOf(emptyList())
        else combine(taskRepo.getTasksForGoal(id), prefs.showWontDoTasksFlow) { list, showWont ->
            if (showWont) list else list.filter { !it.isCantComplete }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val today = LocalDate.now()
    private val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)

    init {
        viewModelScope.launch {
            combine(
                taskRepo.getTasksForDate(LocalDate.now()),
                prefs.showWontDoTasksFlow,
            ) { todayTasks, showWontDo ->
                if (showWontDo) todayTasks else todayTasks.filter { !it.isCantComplete }
            }.collect { visible ->
                _uiState.update { it.copy(tasks = visible) }
            }
        }
        viewModelScope.launch {
            prefs.taskTagColorsFlow.collect { colors ->
                _uiState.update { it.copy(taskTagColors = colors) }
            }
        }
        viewModelScope.launch {
            goalRepo.getGoalsForWeek(weekStart).collect { goals ->
                _uiState.update { it.copy(goals = goals) }
            }
        }
        viewModelScope.launch {
            prefs.assistantInsightEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(assistantInsightEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            prefs.weeklyGoalsInsightEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(weeklyGoalsInsightEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            prefs.llmConfiguredFlow.collect { ok ->
                _uiState.update { it.copy(cerebrasConfigured = ok) }
            }
        }
        viewModelScope.launch {
            insightRepo.observeAssistant().collect { entity ->
                _uiState.update {
                    it.copy(assistantInsight = entity?.toAiInsight() ?: it.assistantInsight)
                }
            }
        }
        viewModelScope.launch {
            insightRepo.observeWeeklyGoals().collect { entity ->
                _uiState.update {
                    it.copy(weeklyGoalsInsight = entity?.toAiInsight() ?: it.weeklyGoalsInsight)
                }
            }
        }
        viewModelScope.launch {
            prefs.dayWindowFlow.collect { w ->
                _uiState.update {
                    it.copy(
                        dayWindowStart = dayWindowMinutesToLocalTime(w.startMinute),
                        dayWindowEnd = dayWindowMinutesToLocalTime(w.endMinute),
                    )
                }
            }
        }
        viewModelScope.launch {
            insightRepo.ensureAssistantForTodayIfNeeded()
        }
        viewModelScope.launch {
            insightRepo.ensureWeeklyGoalsForTodayIfNeeded()
        }
        viewModelScope.launch {
            prefs.homeGmailSummaryEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(homeGmailSummaryEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            gmailMailboxSummaryRepo.observe().collect { entity ->
                val digest = when {
                    entity == null -> ""
                    else -> {
                        val rp = entity.recoveryPlan
                        if (!rp.isNullOrBlank()) rp
                        else {
                            val t = entity.insightText.take(600)
                            if (entity.insightText.length > 600) "$t…" else t
                        }
                    }
                }
                _uiState.update { it.copy(gmailHomeDigestMarkdown = digest) }
            }
        }
        viewModelScope.launch {
            prefs.morningMotivationAutoplayFlow.collect { enabled ->
                _uiState.update { it.copy(morningMotivationAutoplay = enabled) }
            }
        }
        viewModelScope.launch {
            buildMorningClipsFlow(
                prefs.morningYoutubeLinesBlockFlow,
                prefs.morningLocalVideoUrisFlow,
                prefs.morningPlaylistEntriesFlow,
                youtubePlaylistVideoIdsFetcher,
            ).collect { clips ->
                _uiState.update { it.copy(morningMotivationClips = clips) }
            }
        }
        viewModelScope.launch {
            buildMorningClipsFlow(
                prefs.morningSpiritualYoutubeLinesBlockFlow,
                prefs.morningSpiritualLocalVideoUrisFlow,
                prefs.morningSpiritualPlaylistEntriesFlow,
                youtubePlaylistVideoIdsFetcher,
            ).collect { clips ->
                _uiState.update { it.copy(morningSpiritualClips = clips) }
            }
        }
        viewModelScope.launch {
            prefs.homeMorningVideoTabFlow.collect { tab ->
                _uiState.update { it.copy(homeMorningVideoTab = tab) }
            }
        }
        viewModelScope.launch {
            prefs.homeLayoutSectionOrderFlow.collect { order ->
                _uiState.update { it.copy(homeLayoutOrder = order) }
            }
        }
        viewModelScope.launch {
            prefs.homeDailyPdfUriFlow.collect { uri ->
                _uiState.update { it.copy(homeDailyPdfUri = uri) }
            }
        }
        viewModelScope.launch {
            prefs.homeDailyPdfLastPageFlow.collect { page ->
                _uiState.update { it.copy(homeDailyPdfLastPage = page) }
            }
        }
        viewModelScope.launch {
            prefs.homePdfViewModeFlow.collect { mode ->
                _uiState.update { it.copy(homePdfViewMode = mode) }
            }
        }
        viewModelScope.launch {
            prefs.homePdfThemeDarkFlow.collect { dark ->
                _uiState.update { it.copy(homePdfThemeDark = dark) }
            }
        }
        viewModelScope.launch {
            prefs.homePdfZoomScaleFlow.collect { z ->
                _uiState.update { it.copy(homePdfZoomScale = z) }
            }
        }
    }

    fun regenerateAssistantInsight() = viewModelScope.launch {
        _uiState.update { it.copy(assistantInsightLoading = true) }
        val result = insightRepo.regenerateAssistant()
        _uiState.update { it.copy(assistantInsightLoading = false) }
        result.onFailure { /* UI can observe stale state; optional snackbar via effect */ }
    }

    fun regenerateWeeklyGoalsInsight() = viewModelScope.launch {
        _uiState.update { it.copy(weeklyGoalsInsightLoading = true) }
        insightRepo.regenerateWeeklyGoals()
        _uiState.update { it.copy(weeklyGoalsInsightLoading = false) }
    }

    fun toggleTaskDone(task: PriorityTask) = viewModelScope.launch {
        taskRepo.toggleDone(task)
        val t = taskRepo.getById(task.id) ?: return@launch
        if (t.isDone) {
            taskReminderScheduler.cancel(t.id)
        } else {
            taskReminderScheduler.schedule(t)
        }
    }
    fun toggleGoalsCollapsed() = _uiState.update { it.copy(goalsCollapsed = !it.goalsCollapsed) }
    fun toggleGoal(goal: WeeklyGoal) = viewModelScope.launch { goalRepo.toggleCompleted(goal) }

    fun openGoalDetail(goalId: Long) {
        _detailGoalId.value = goalId
    }

    fun dismissGoalDetail() {
        _detailGoalId.value = null
    }

    fun updateWeeklyGoal(goal: WeeklyGoal) = viewModelScope.launch {
        goalRepo.update(goal)
    }

    fun deleteWeeklyGoal(goal: WeeklyGoal) = viewModelScope.launch {
        taskRepo.clearGoalLinks(goal.id)
        goalRepo.delete(goal)
        if (_detailGoalId.value == goal.id) _detailGoalId.value = null
    }

    fun setWeeklyGoalProgress(goalId: Long, percent: Int) = viewModelScope.launch {
        val g = goalRepo.getById(goalId) ?: return@launch
        goalRepo.update(g.copy(progressPercent = percent.coerceIn(0, 100)))
    }

    fun startPomodoroForGoal(goal: WeeklyGoal) {
        pomodoroNavBridge.push(
            PomodoroLaunchRequest(
                entityType = PomodoroLaunchRequest.TYPE_GOAL,
                entityId = goal.id,
                title = goal.title,
            ),
        )
    }

    fun addTask(
        title: String,
        startTime: LocalTime,
        endTime: LocalTime,
        urgency: Urgency = Urgency.GREEN
    ) = viewModelScope.launch {
        val nextRank = (_uiState.value.tasks.maxOfOrNull { it.rank } ?: 0) + 1
        val newId = taskRepo.insert(
            PriorityTask(
                rank = nextRank,
                title = title,
                startTime = startTime,
                endTime = endTime,
                urgency = urgency,
                isTopFive = false,
                date = today
            )
        )
        taskRepo.getById(newId)?.let { taskReminderScheduler.schedule(it) }
    }

    fun startPomodoroForTask(task: PriorityTask) {
        pomodoroNavBridge.push(
            PomodoroLaunchRequest(
                entityType = PomodoroLaunchRequest.TYPE_TASK,
                entityId = task.id,
                title = task.title,
            ),
        )
    }

    fun addGoal(title: String) = viewModelScope.launch {
        goalRepo.insert(WeeklyGoal(title = title, weekStart = weekStart))
    }

    fun addGoalFull(
        title: String,
        description: String?,
        deadline: String?,
        timeEstimate: String?,
        category: String,
        iconEmoji: String?,
        progressPercent: Int,
    ) = viewModelScope.launch {
        goalRepo.insert(
            WeeklyGoal(
                title = title,
                description = description,
                deadline = deadline,
                timeEstimate = timeEstimate,
                category = category,
                iconEmoji = iconEmoji?.trim()?.takeIf { it.isNotEmpty() },
                progressPercent = progressPercent.coerceIn(0, 100),
                weekStart = weekStart,
            ),
        )
    }

    private fun dayWindowMinutesToLocalTime(minuteOfDay: Int): LocalTime {
        val c = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        return LocalTime.of(c / 60, c % 60)
    }

    fun saveHomeLayoutOrder(order: List<HomeLayoutSection>) {
        prefs.setHomeLayoutSectionOrder(order)
    }

    fun setHomeMorningVideoTab(tab: MorningVideoBucket) {
        prefs.setHomeMorningVideoTab(tab)
    }

    fun persistHomePdfLastPage(pageIndex: Int) {
        prefs.setHomeDailyPdfLastPage(pageIndex)
    }

    fun setHomePdfViewMode(mode: HomePdfViewMode) {
        prefs.setHomePdfViewMode(mode)
    }

    fun setHomePdfThemeDark(enabled: Boolean) {
        prefs.setHomePdfThemeDark(enabled)
    }

    fun setHomePdfZoomScale(scale: Float) {
        prefs.setHomePdfZoomScale(scale)
    }

    fun multiplyHomePdfZoom(factor: Float) {
        val cur = _uiState.value.homePdfZoomScale
        prefs.setHomePdfZoomScale(cur * factor)
    }
}
