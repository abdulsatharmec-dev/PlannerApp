# Daily Curator — Android App Specification

> **Purpose**: A comprehensive daily productivity app combining AI-assisted task scheduling, habit tracking, weekly goals, and dual-view timeline/clock scheduling. This spec is written for a coding assistant to implement the app exactly as seen in the provided UI mockups.

---

## 1. Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| Navigation | Jetpack Navigation Compose |
| Local DB | Room (SQLite) |
| Dependency Injection | Hilt |
| State Management | StateFlow + ViewModel |
| Date/Time | java.time (API 26+) |
| Min SDK | 26 |
| Target SDK | 35 |

---

## 2. Design System

### 2.1 Color Palette

```kotlin
// Light theme (default)
val Primary = Color(0xFF1A1F5E)          // Deep navy blue (headers, active icons)
val PrimaryButton = Color(0xFF2D35C9)    // Bright indigo (CTA buttons)
val AccentGreen = Color(0xFF1DB954)      // Bright green (check icons, completed goals)
val AccentRed = Color(0xFFE53935)        // Red (overdue, resistance metrics)
val AccentTeal = Color(0xFF00BCD4)       // Teal (physical habit tag)
val AccentBrown = Color(0xFF8D6E63)      // Muted brown (mental habit tag)
val AccentDeepGreen = Color(0xFF2E7D32)  // Deep green (spiritual habit tag)
val Background = Color(0xFFF4F5FB)       // Light lavender-white
val Surface = Color(0xFFFFFFFF)          // White cards
val SurfaceVariant = Color(0xFFF0F1FA)   // Light blue-grey (priority item bg)
val TextPrimary = Color(0xFF0D0F1C)      // Near black
val TextSecondary = Color(0xFF6B7280)    // Medium grey
val TextTertiary = Color(0xFFADB5BD)     // Light grey (disabled/pending)
val Divider = Color(0xFFE5E7EB)
val InsightBg = Color(0xFFF0F4FF)        // AI insight card background
val NowIndicator = Color(0xFFE53935)     // Red dot for current time
val TimelineBlue = Color(0xFF1565C0)     // Dark blue for clock arc/hand
val ProgressTrackBg = Color(0xFFE0E0E0)  // Grey track for progress bars
```

### 2.2 Typography

```kotlin
// Font family: Use "Plus Jakarta Sans" (Google Fonts)
val AppTypography = Typography(
    displayLarge  = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleLarge    = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    titleMedium   = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    bodyMedium    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    bodySmall     = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal),
    labelSmall    = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium,
                              letterSpacing = 0.5.sp)
)
```

### 2.3 Shape & Elevation

```kotlin
val CardShape   = RoundedCornerShape(16.dp)
val ChipShape   = RoundedCornerShape(6.dp)
val ButtonShape = RoundedCornerShape(14.dp)
val CardElevation = 0.dp   // Flat cards, no elevation shadow
```

### 2.4 Spacing

- Screen horizontal padding: `20.dp`
- Card internal padding: `16.dp`
- Between sections: `24.dp`
- Between list items: `10.dp`

---

## 3. Navigation Structure

### Bottom Navigation Bar (4 tabs)

| Tab | Icon | Label | Screen |
|---|---|---|---|
| 1 | `schedule` (list) | Timeline | TodayScreen |
| 2 | `track_changes` | Goals | GoalsScreen |
| 3 | `schedule` (clock) | Clock | ClockScreen (nested in TodayScreen) |
| 4 | `settings` | Settings | SettingsScreen |

> **Note**: The "Today" screen has **two sub-views** toggled by chips: `Timeline` and `Clock`. These are NOT separate bottom nav tabs — they are a segmented toggle within the Today/Schedule section.

---

## 4. Screens — Full Specification

---

### 4.1 Today Screen (Main / Home)

**Route**: `today`  
**Scroll**: Single vertical `LazyColumn`

#### 4.1.1 Top App Bar

```
[🟦 App Icon (24dp rounded square)]  "Daily Curator"    [✦ AI Sparkle Icon]
```

- App icon: Dark navy square with white calendar/bookmark icon
- Title: `headlineMedium`, `TextPrimary`
- Right icon: 4-point sparkle/star, `Primary` color, tappable (opens AI assistant panel)

---

#### 4.1.2 AI Assistant Insight Card

```
╔══════════════════════════════════════════╗
║  ✦ ASSISTANT INSIGHT          [Gemini🔵] ║
║                                          ║
║  You've crushed 85% of your deep        ║
║  work this week.                         ║
║                                          ║
║  Today's focus on Strategy Audit will   ║
║  put you ahead of schedule for the Q3   ║
║  release. Keep this momentum.            ║
╚══════════════════════════════════════════╝
```

- Background: `InsightBg` (#F0F4FF)
- Border radius: `16.dp`
- Top-left label: `✦ ASSISTANT INSIGHT` — `labelSmall`, `Primary`, uppercase
- Top-right: Gemini "G" logo badge (circle, blue gradient, 32dp)
- Bold headline: first sentence in `titleLarge`, `Primary`
- Body: `bodyMedium`, `TextSecondary`
- No border, subtle background only

---

#### 4.1.3 Top 5 Priorities Section

**Header**: `TOP 5 PRIORITIES` — `labelSmall`, `TextSecondary`, uppercase, `letterSpacing = 1.sp`

**Priority Item Layout** (vertical list, 5 items):

```
╔════════════════════════════════════════════╗
║ [01]  Finalize Q4 Budget Sheets           ○║
║       09:00 - 10:30 AM • Due by 4:00 PM   ║
╚════════════════════════════════════════════╝
```

Each item:
- Left: Number badge `01`–`05` — `labelSmall`, `TextTertiary`
- Left accent bar: 3dp wide colored vertical line
  - Item 01–03: `AccentGreen`
  - Item 04: `AccentRed` (overdue/urgent)
  - Item 05: `TextTertiary` (neutral)
- Title: `titleMedium`, `TextPrimary`
- Subtitle: time range + bullet + status — `bodySmall`, `TextSecondary`
- Right: Circle status button (empty circle = not done, filled = done)
  - Size: 22dp, border: 1.5dp `TextTertiary`
  - When done: filled with `AccentGreen` + white checkmark
- Background: `SurfaceVariant` rounded `12.dp`
- Padding: `14.dp` horizontal, `12.dp` vertical

**Data model per priority item**:
```kotlin
data class PriorityTask(
    val id: Long,
    val rank: Int,              // 1-5
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val dueInfo: String?,       // "Due by 4:00 PM"
    val statusNote: String?,    // "Legal approval pending"
    val urgency: Urgency,       // GREEN, RED, NEUTRAL
    val isDone: Boolean
)
```

**"+ New Task" Button**:
- Full width, `PrimaryButton` background, white text, `titleMedium`
- Height: `52.dp`, shape: `ButtonShape`
- Leading `+` icon

---

#### 4.1.4 Weekly Goals Section

**Header row**:
```
[✦ icon]  Weekly Goals         [^ collapse chevron]
          PROGRESS: 3/5 GOALS
```

- Title: `titleLarge`, `Primary`
- Progress subtitle: `labelSmall`, `AccentGreen`, uppercase
- Right: Chevron up/down — toggles collapse of goal list

**AI Insight Sub-card** (inside Weekly Goals, always visible):

```
╔════════════════════════════════════════════╗
║ [🟢] AI Insight: Slight delay on          ║
║      Strategy Audit due to unplanned      ║
║      syncs this morning.                   ║
║                                            ║
║      Recovery plan: Focus on the 90m      ║
║      Deep Work block tomorrow morning     ║
║      to catch up on documentation tasks.  ║
╚════════════════════════════════════════════╝
```

- Background: `Surface`
- Left border: 3dp `AccentGreen`
- `AI Insight:` label bold `AccentGreen`, rest normal `TextPrimary`
- Recovery plan text: italic, `TextSecondary`

**Goal List Items**:

```
✅  Finalize Q3 Pipeline Audit          ← completed
✅  Client Showcase Asset Selection     ← completed  
✅  Weekly Team Performance Sync        ← completed
○   Budget Forecast v2.0               ← pending
○   Internal Architecture Refactor     ← pending
```

- Completed: `AccentGreen` filled checkbox icon + `TextPrimary` text (normal weight)
- Pending: Empty circle icon + `TextTertiary` text
- Row height: `44.dp`, no card background, just list item

**Data model**:
```kotlin
data class WeeklyGoal(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
    val weekStart: LocalDate
)
```

---

#### 4.1.5 Today's Schedule Section

**Header**:
```
TODAY                           Aug 24
Schedule
[Timeline chip] [Clock chip]
```

- `TODAY` label: `labelSmall`, `AccentGreen`, uppercase
- `Schedule` heading: `headlineMedium`, `TextPrimary`
- Date: `bodyMedium`, `TextSecondary`, right-aligned
- Sub-tabs: `Timeline` and `Clock` — pill-shaped chips
  - Active: `Primary` background, white text
  - Inactive: `SurfaceVariant` background, `TextSecondary`

---

#### 4.1.5a Timeline Sub-view (default)

A **vertical time-based schedule list**:

```
10:15 AM  ● NOW
─────────────────────────────────
09:00     ╔══════════════════════╗
          ║ Deep Work: Strategy  🔒║
          ║ Audit                ║
          ║ Main Conference Hall ║
          ║ [FOCUS] [HIGH]       ║
          ╚══════════════════════╝
─────────────────────────────────
11:30     Client Briefing: Zenith Labs
          Virtual Meet • 45m
```

**NOW indicator**:
- Red dot (8dp) + `NOW` red pill label + horizontal red dashed line spanning full width
- Appears at the current time position

**Event card (block event)**:
- Background: `SurfaceVariant` (slightly blue-grey)
- Rounded `12.dp`
- Title: `titleMedium`, `TextPrimary` + lock icon (🔒) if protected/focus block
- Subtitle: location/detail, `bodySmall`, `TextSecondary`
- Tags: small chips `[FOCUS]` `[HIGH]` — `ChipShape`, `SurfaceVariant`/`Primary` bg
- Left accent: 3dp colored bar (blue for focus blocks)

**Time labels**: `bodySmall`, `TextTertiary`, left column (fixed 52dp width)

**Data model**:
```kotlin
data class ScheduleEvent(
    val id: Long,
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val location: String?,
    val tags: List<String>,
    val priority: EventPriority,   // HIGH, MEDIUM, LOW
    val isProtected: Boolean,
    val date: LocalDate
)
```

---

#### 4.1.5b Clock Sub-view

A **radial/circular clock visualization** replacing the timeline list:

```
         Strategic Portfolio
               Review
         ╭──────────────╮
        /                 \     ← arc segments for each event
       |    10:15           |
       |    NOW             |
        \                 /
         ╰──────────────╯
   Curatorial          Gallery-Lighting
   Documentation            Sync

WINDOW START: 09:00          WINDOW END: 10:00
```

**Clock face**:
- Circular dial, ~300dp diameter, centered
- Arc segments drawn for each scheduled event in the visible time window
  - Each event = colored arc segment proportional to duration
  - Colors: Blue (`Primary`) for main event, grey for others
- Current time hand: red line from center to current-time position on arc
- Center text: current time large (`28.sp bold`) + `NOW` red label below
- Event labels: outside the circle at their arc position
- Bottom: `WINDOW START` and `WINDOW END` labels with times

**Implementation**: Custom `Canvas` composable drawing arcs and text.

---

### 4.2 Habits Screen

**Route**: `habits`  
**App Bar**: `[calendar icon] Curator` + `[grid icon]` + `[avatar circle]`

#### 4.2.1 Header

```
PERFORMANCE TRACK
Habits                    [+ New Habit button]
```

- `PERFORMANCE TRACK`: `labelSmall`, `TextSecondary`, uppercase
- `Habits`: `displayLarge`, `TextPrimary`
- `+ New Habit`: outlined or filled button, `PrimaryButton`, right-aligned

---

#### 4.2.2 AI Habit Extractor Card

```
╔═════════════════════════════════════════╗
║ [✦ teal icon]  AI Habit Extractor    › ║
║                Analyze your journal    ║
║                for new habits          ║
╚═════════════════════════════════════════╝
```

- Background: `Surface`, rounded `16.dp`, subtle border or shadow
- Left icon: teal sparkle on light teal bg square (`48x48dp`, rounded `12.dp`)
- Chevron right: navigates to journal analysis screen
- `bodyMedium` text

---

#### 4.2.3 "Building Momentum" Section

Section header: `BUILDING MOMENTUM` — `labelSmall`, `TextSecondary`, uppercase, `letterSpacing = 1.5.sp`

**Habit Card layout**:

```
╔══════════════════════════════════════════════╗
║ [💧icon]  Hydration Protocol    Streak       ║
║           PHYSICAL              12 Days      ║
║                                              ║
║  2.2L / 3.0L                         73%    ║
║  ████████████████████░░░░░░░░░░░░░░          ║
║  0.8L remaining to goal                      ║
╚══════════════════════════════════════════════╝
```

**Card anatomy**:
- Background: `Surface`, `CardShape` (16dp), no elevation
- Icon: 48dp rounded square with colored background + emoji/icon
- Title: `titleLarge`, `Primary`
- Category tag: `labelSmall`, uppercase, colored text (PHYSICAL=`AccentTeal`, MENTAL=`AccentBrown`, SPIRITUAL=`AccentDeepGreen`)
- Right column: `Streak` label (`bodySmall`, `TextSecondary`) + streak value (`titleLarge bold`, `Primary`)
- Progress bar: full width, height `6.dp`, rounded, track=`ProgressTrackBg`
  - Fill color per category: PHYSICAL=`Primary`, MENTAL=`AccentRed`, SPIRITUAL=`AccentDeepGreen`
- Below bar: progress fraction left (`bodySmall`, `TextSecondary`), percentage right (`bodySmall`, `TextSecondary`)
- Bottom note: remaining/completed text (`bodySmall`, `TextTertiary`, italic)
- When goal met: percentage replaced with `Goal Met` in `AccentDeepGreen`

**Habit categories in Building Momentum**:
1. **Hydration Protocol** — PHYSICAL — 2.2L / 3.0L — 73% — Streak 12 Days
2. **Deep Focus Session** — MENTAL — 3/5 sessions — 60% — Streak 5 Days
3. **Sacred Reading** — SPIRITUAL — 20/20 mins — Goal Met — Streak 28 Days

---

#### 4.2.4 "Eliminating Friction" Section

Section header: `ELIMINATING FRICTION` — `labelSmall`, `AccentRed`, uppercase

**Anti-habit card layout** (same structure, but shows "Resistance"):

```
╔══════════════════════════════════════════════╗
║ [📱icon]  Digital Distraction   Resistance   ║
║           MENTAL                3 Days        ║
║                                              ║
║  15 / 30 min limit                    50%    ║
║  ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░       ║
║  15 mins remaining allowance                 ║
╚══════════════════════════════════════════════╝
```

- `Resistance` label instead of `Streak`
- Resistance value in `AccentRed`
- Progress bar fill: `AccentRed`

---

**Data model**:
```kotlin
data class Habit(
    val id: Long,
    val name: String,
    val category: HabitCategory,   // PHYSICAL, MENTAL, SPIRITUAL
    val habitType: HabitType,      // BUILDING, ELIMINATING
    val iconEmoji: String,
    val currentValue: Float,
    val targetValue: Float,
    val unit: String,              // "L", "sessions", "mins", "min limit"
    val streakDays: Int,
    val isGoalMet: Boolean,
    val date: LocalDate
)

enum class HabitCategory { PHYSICAL, MENTAL, SPIRITUAL }
enum class HabitType { BUILDING, ELIMINATING }
```

---

### 4.3 Goals Screen

**Route**: `goals`

#### Layout

```
WEEKLY GOALS
This Week                    [+ New Goal]

╔════════════════════════════════════════╗
║ Progress Overview                      ║
║ 3 / 5 Goals Completed    ●●●○○ 60%    ║
╚════════════════════════════════════════╝

COMPLETED (3)
✅ Finalize Q3 Pipeline Audit
✅ Client Showcase Asset Selection
✅ Weekly Team Performance Sync

IN PROGRESS (2)
○  Budget Forecast v2.0
○  Internal Architecture Refactor
```

- Progress bar at top showing week completion
- Grouped list: Completed vs In Progress
- Each goal tappable → detail/edit sheet

---

### 4.4 Settings Screen

**Route**: `settings`

Standard settings list:
- Profile (name, avatar)
- Notifications
- AI Assistant (enable/disable, preferences)
- Theme (Light / Dark / System)
- Data & Sync
- About

---

## 5. Add Task / Add Habit Bottom Sheets

### 5.1 Add Task Sheet

Triggered by "+ New Task" button.

```
╔══════════════════════════════════╗
║ New Task                      ✕ ║
║──────────────────────────────────║
║ Task title           [text field]║
║ Date                 [date pick] ║
║ Start time           [time pick] ║
║ End time             [time pick] ║
║ Due by               [time pick] ║
║ Priority rank        [1–5 picker]║
║ Tags                 [chip input]║
║ Status note          [text field]║
║                                  ║
║        [Add Task Button]         ║
╚══════════════════════════════════╝
```

### 5.2 Add Habit Sheet

```
╔══════════════════════════════════╗
║ New Habit                     ✕ ║
║──────────────────────────────────║
║ Habit name           [text field]║
║ Category        [PHYSICAL/MENTAL/SPIRITUAL chips]
║ Type            [BUILDING / ELIMINATING toggle]
║ Target value         [number]    ║
║ Unit                 [text field]║
║ Icon                 [emoji pick]║
║                                  ║
║        [Add Habit Button]        ║
╚══════════════════════════════════╝
```

---

## 6. Database Schema (Room)

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rank: Int,
    val title: String,
    val startTime: String,    // ISO "HH:mm"
    val endTime: String,
    val dueInfo: String?,
    val statusNote: String?,
    val urgency: String,      // "GREEN", "RED", "NEUTRAL"
    val isDone: Boolean,
    val date: String,         // ISO "yyyy-MM-dd"
    val tags: String,         // JSON array
    val location: String?,
    val isProtected: Boolean
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val habitType: String,
    val iconEmoji: String,
    val currentValue: Float,
    val targetValue: Float,
    val unit: String,
    val streakDays: Int,
    val date: String
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isCompleted: Boolean,
    val weekStart: String
)

@Entity(tableName = "ai_insights")
data class AIInsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val insightText: String,
    val recoveryPlan: String?,
    val date: String
)
```

---

## 7. Key Composables Reference

```
DailyCuratorApp
├── AppNavHost
│   ├── TodayScreen
│   │   ├── TopAppBar
│   │   ├── AIInsightCard
│   │   ├── PrioritiesSection
│   │   │   ├── PriorityItem (x5)
│   │   │   └── NewTaskButton
│   │   ├── WeeklyGoalsSection
│   │   │   ├── AIGoalInsightCard
│   │   │   └── GoalListItem (x5)
│   │   └── TodayScheduleSection
│   │       ├── ScheduleSubTabRow (Timeline | Clock)
│   │       ├── TimelineView
│   │       │   ├── NowIndicator
│   │       │   └── TimelineEventCard
│   │       └── ClockView (Canvas)
│   ├── HabitsScreen
│   │   ├── AIHabitExtractorCard
│   │   ├── HabitSectionHeader ("Building Momentum")
│   │   ├── HabitCard (x3)
│   │   ├── HabitSectionHeader ("Eliminating Friction")
│   │   └── HabitCard (anti-habit)
│   ├── GoalsScreen
│   └── SettingsScreen
├── BottomNavigationBar
├── AddTaskBottomSheet
└── AddHabitBottomSheet
```

---

## 8. Animations & Interactions

| Trigger | Animation |
|---|---|
| App launch | Staggered fade-in of cards (50ms delay each) |
| Priority item done | Checkbox fills with scale + green sweep animation |
| Progress bar | Animate from 0 to value on screen enter (500ms, EaseOut) |
| Clock hand | Smooth sweep to current time on Clock view open |
| Goal collapse | Animated height change (300ms) |
| Bottom sheet | Slide up from bottom (standard Material motion) |
| Tab switch | CrossFade between Timeline and Clock views |
| NOW indicator | Pulse animation on red dot (repeat infinite) |

---

## 9. Sample / Seed Data

Pre-populate DB on first install:

**Tasks (Aug 24)**:
1. Finalize Q4 Budget Sheets — 09:00–10:30 — Due 4:00 PM — GREEN
2. Contract Review: Acme Corp — 11:00–12:00 — Legal approval pending — GREEN
3. Hiring: Senior UI Designer — 01:30–02:30 — Review 5 portfolios — GREEN
4. Presentation Prep: Board Meeting — 03:00–04:30 — Slide 12–24 remaining — RED
5. Bi-Weekly Sync: Marketing — 04:30–05:00 — External Agency included — NEUTRAL

**Goals (current week)**:
- Finalize Q3 Pipeline Audit ✅
- Client Showcase Asset Selection ✅
- Weekly Team Performance Sync ✅
- Budget Forecast v2.0 ○
- Internal Architecture Refactor ○

**Habits (today)**:
- Hydration Protocol — PHYSICAL — 2.2/3.0L — streak 12
- Deep Focus Session — MENTAL — 3/5 — streak 5
- Sacred Reading — SPIRITUAL — 20/20 — streak 28
- Digital Distraction — MENTAL ELIMINATING — 15/30 min — 3 days resistance

**AI Insight**: "You've crushed 85% of your deep work this week. Today's focus on Strategy Audit will put you ahead of schedule for the Q3 release. Keep this momentum."

---

## 10. File / Package Structure

```
com.dailycurator/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/ (TaskDao, HabitDao, GoalDao)
│   │   └── entity/ (TaskEntity, HabitEntity, GoalEntity)
│   ├── repository/ (TaskRepository, HabitRepository, GoalRepository)
│   └── model/ (PriorityTask, Habit, WeeklyGoal, ScheduleEvent)
├── di/
│   └── AppModule.kt
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Theme.kt
│   ├── components/
│   │   ├── AIInsightCard.kt
│   │   ├── PriorityItem.kt
│   │   ├── HabitCard.kt
│   │   ├── GoalListItem.kt
│   │   ├── TimelineEventCard.kt
│   │   ├── ClockView.kt          ← Canvas-based
│   │   ├── NowIndicator.kt
│   │   └── ProgressBar.kt
│   ├── screens/
│   │   ├── today/
│   │   │   ├── TodayScreen.kt
│   │   │   └── TodayViewModel.kt
│   │   ├── habits/
│   │   │   ├── HabitsScreen.kt
│   │   │   └── HabitsViewModel.kt
│   │   ├── goals/
│   │   │   ├── GoalsScreen.kt
│   │   │   └── GoalsViewModel.kt
│   │   └── settings/
│   │       └── SettingsScreen.kt
│   └── navigation/
│       └── AppNavHost.kt
└── MainActivity.kt
```

---

## 11. Implementation Notes for Coding Assistant

1. **Clock View**: Use `Canvas` in Compose. Draw a circular arc for the full time window (e.g. 09:00–18:00), then overlay colored arc segments per event. Use `drawArc()`. The clock hand is a line from center to the current-time angle. Event labels use `drawContext.canvas.nativeCanvas.drawText()` positioned outside the circle radius.

2. **NOW indicator in Timeline**: Calculate the pixel offset of the current time within the LazyColumn using time-to-pixel mapping. Overlay with a `Box` using absolute positioning or use a custom `Layout`.

3. **Priority accent bar**: Each `PriorityItem` row has a `Box` on the left with `width = 3.dp, fillMaxHeight()` in the card's `urgency` color.

4. **Progress bars**: Use `LinearProgressIndicator` with custom track/indicator colors per habit category, wrapped in a `Box` for the rounded-ends effect.

5. **Streak vs Resistance**: The `HabitCard` composable accepts a `labelText: String` ("Streak" or "Resistance") and `valueColor: Color` parameter so it works for both building and eliminating habits.

6. **Seed data**: Insert seed data in a `RoomDatabase.Callback.onCreate()` using a coroutine scope.

7. **Weekly Goals progress**: Compute `completed / total` at ViewModel level, expose as `StateFlow<Pair<Int,Int>>`.

8. **Bottom Nav**: Use `NavigationBar` + `NavigationBarItem`. Active icon filled, inactive outlined.
