# Architecture comparison: **dayroute** vs **Dayroute_new** (Daily Curator)

This document contrasts the original **DayRoute** app (`com.dayroute`, folder `dayroute/`) with the newer **Daily Curator** codebase (`com.dailycurator`, folder `Dayroute_new/`). Use it alongside the AI-specific reference for the legacy app: `../dayroute/docs/AI_IMPLEMENTATION_REFERENCE.md`.

---

## 1. High-level summary

| Area | **dayroute** (`com.dayroute`) | **Dayroute_new** (`com.dailycurator`) |
|------|-------------------------------|----------------------------------------|
| **Product focus** | Planner + AI chat + file memory + durable “facts” | Rich “Today” UX + tasks / habits / goals + AI chat |
| **LLM integration** | Custom `AIRepository` (OkHttp + Gson → Cerebras) | **Koog** agents (`AIAgent`, `OpenAILLMClient`) → same Cerebras base URL |
| **Central AI abstraction** | Single `AIRepository` for all model calls | No shared repository; `ChatViewModel` builds client + agent |
| **Chat persistence** | Room: messages, facts, suggestions, documents | In-memory `StateFlow` only (lost on process death) |
| **Daily insight** | Real LLM + per-day cache in `SettingsRepository` | **Static / demo** `AiInsight` in `TodayViewModel` (no API call) |
| **Documents & memory** | ML Kit OCR + LLM metadata + Room + Memory UI | **Not present** in Room or navigation |
| **Navigation** | Drawer (Home, Files, Memory, Settings) + bottom (Goals, Habits, Chat) | Single bottom bar: Today, Tasks, Habits, Goals, Chat + Settings (stack) |
| **Domain data (Room)** | Planner entities + chat/memory entities | Tasks, habits, goals only |

---

## 2. Package and module layout

| | **dayroute** | **Dayroute_new** |
|--|--------------|------------------|
| Application id | `com.dayroute` | `com.dailycurator` |
| Main UI entry | `MainScreen` + nested routes | `AppNavHost` + `Scaffold` + `NavHost` |
| Feature packages | `ui/chat`, `ui/memory`, `ui/settings`, `viewmodel/*`, `data/*` | `ui/screens/*`, `ui/components`, `data/repository`, `data/local` |

---

## 3. AI and networking

### 3.1 **dayroute**

- **One class** owns HTTP, prompts, parsing: `data/AIRepository.kt`.
- **Provider:** Cerebras OpenAI-compatible `POST /v1/chat/completions`.
- **Features:** home insight, chat reply, fact extraction JSON, document analysis JSON.
- **Dependencies:** OkHttp, Gson (see `dayroute/app/build.gradle.kts`).

### 3.2 **Dayroute_new**

- **Koog stack** in `app/build.gradle.kts`: `koog-agents`, `agents-core`, `prompt-executor-*`, `prompt-llm`.
- **`ChatViewModel`** (`ui/screens/chat/ChatViewModel.kt`):
  - Reads API key from `AppPreferences.getCerebrasKey()`.
  - Builds `OpenAILLMClient(apiKey, OpenAIClientSettings(baseUrl = "https://api.cerebras.ai"))`.
  - Wraps with `SingleLLMPromptExecutor` and `AIAgent(promptExecutor, llmModel)`.
  - Model id is **hardcoded** (e.g. `qwen-3-235b-a22b-instruct-2507`) — no settings enum like `dayroute`’s `CerebrasModel`.
- **Context for chat:** `buildContext()` pulls **today’s tasks, habits, and weekly goals** from repositories and prepends to the user message (no separate “system” message block in the same shape as `AIRepository.buildChatUserPrompt`, but same idea).
- **No** `AIRepository`-style typed `AIResult` / `AIError`; failures are caught and shown as assistant text.

### 3.3 Insight surfaces

| Surface | **dayroute** | **Dayroute_new** |
|---------|--------------|------------------|
| Home / Today card | `MainViewModel` → `aiRepo.getInsight(tasks)` + cache | `TodayViewModel` default `AiInsight(...)` **fixed strings** |
| Extra insight UI | Single `AssistantInsightCard` string | `AIInsightCard` + **hardcoded** goal insight sentence in `TodayScreen` |
| Settings | Model + API key + insight cache keys | Cerebras key only (`SettingsViewModel` / `AppPreferences`); “AI Preferences” / “Insight Frequency” rows are **non-functional** placeholders |

---

## 4. Data layer and persistence

### 4.1 Room

| **dayroute** | **Dayroute_new** |
|--------------|------------------|
| `DayRouteDatabase`: planner + `ChatMemoryDao` (messages, facts, suggestions, uploaded docs) | `AppDatabase`: `TaskEntity`, `HabitEntity`, `GoalEntity` only |
| Chat and memory survive restarts | Chat does **not** persist |

### 4.2 Preferences / settings

| **dayroute** | **Dayroute_new** |
|--------------|------------------|
| `SettingsRepository`: theme, Cerebras key, selected model, **daily insight text + date** | `AppPreferences`: **Cerebras key** (and other prefs as extended) |

### 4.3 Repositories

| **dayroute** | **Dayroute_new** |
|--------------|------------------|
| `PlannerRepository`, `AIRepository`, `ChatMemoryRepository`, `DocumentProcessingRepository`, `SettingsRepository` | `TaskRepository`, `HabitRepository`, `GoalRepository` (pattern: DAO + mapping) |

---

## 5. Navigation and information architecture

```text
dayroute (conceptual)
├── Drawer: Home, Files, Memory, Settings
└── Bottom: Goals, Habits, Chat

Dayroute_new
├── Bottom: Today | Tasks | Habits | Goals | Chat
└── Top bar → Settings (separate route, back stack)
```

**dayroute** separates **Files** (uploads) and **Memory** (facts / suggestions) from chat. **Dayroute_new** has no Files/Memory routes; chat is the only AI surface besides static insight UI.

---

## 6. On-device ML

| **dayroute** | **Dayroute_new** |
|--------------|------------------|
| ML Kit text recognition + face detection in `DocumentProcessingRepository` | **None** in Gradle (no ML Kit) |

---

## 7. Dependency injection

Both use **Hilt** with an `AppModule` / `@Module` providing:

| **dayroute** | **Dayroute_new** |
|--------------|------------------|
| Database, `ChatMemoryDao`, OkHttp, Gson, etc. | `AppDatabase`, task/habit/goal DAOs, `AppPreferences` |

**Dayroute_new** does not provide a singleton LLM client or `AIRepository` in DI; the chat stack is constructed inside `ChatViewModel`.

---

## 8. Testing

| **dayroute** | **Dayroute_new** |
|--------------|------------------|
| Tests for `AIRepository` prompts/parsing, insight policy, `ChatMemoryRepository` | `KoogTest` exercises Cerebras-oriented `OpenAILLMClient` / `AIAgent` setup |

---

## 9. When to use which patterns

- **Prefer dayroute’s pattern** if you need: one auditable AI gateway, persisted chat, RAG over files, structured JSON extraction, insight caching, and user-selectable models.
- **Prefer Dayroute_new’s pattern** if you want: quick integration via **Koog**, agent-style `run(prompt)`, and minimal glue code — accepting in-memory chat and hardcoded model id unless you extend it.

---

## 10. Practical alignment / migration notes

To make **Dayroute_new** closer to **dayroute** (optional roadmap):

1. Introduce an `AIRepository` (or Koog wrapper service) as the **single** LLM entry point; inject it from Hilt.
2. Persist chat (Room or DataStore); optionally add facts/documents like `ChatMemoryRepository`.
3. Replace static `AiInsight` with `getInsight`-style calls + **per-day cache** in preferences.
4. Surface **model selection** in settings (mirror `CerebrasModel`).
5. Add Files + Memory flows if product requires document RAG.

---

## 11. Document history

| Date | Note |
|------|------|
| 2026-04-05 | Initial comparison (Dayroute_new vs dayroute) |

---

*Paths in this file are relative to the DayRoute workspace parent: `dayroute/` and `Dayroute_new/`.*
