# Design: Gmail, mailbox summary, and agent memory (2026-04-06)

## Context

- **Chat today**: `ChatViewModel` uses `CerebrasRestClient` with OpenAI-style tool calling and `ChatToolExecutor`; it does **not** run a Koog `AIAgent` graph in production (Koog is present for tests/check modules).
- **Insights today**: `InsightCacheRepository` + `CachedInsightEntity` keyed by `InsightType` for assistant / weekly goals.

## Architecture choices

### Gmail + “Koog agent”

- **Requirement alignment**: Koog’s [tools overview](https://docs.koog.ai/tools-overview/) describes registering tools on an agent. This app’s agent surface is the existing **Cerebras function-calling loop**, so Gmail is exposed as **additional tools** (`gmail_list_messages`, `gmail_get_message`, `gmail_send_email`) with the same registry/executor pattern as tasks/goals/habits.
- **Future**: Migrating chat to `AIAgent` + `ToolRegistry` would map these same executors into Koog tool classes without changing Gmail REST code.

### Authentication & multi-mailbox

- **OAuth**: Google Sign-In (`play-services-auth`) requests `gmail.readonly` + `gmail.send`. Access tokens are obtained on a background thread via `GoogleAuthUtil.getToken` (deprecated but still the standard client-only path; recoverable auth intents are surfaced with `FLAG_ACTIVITY_NEW_TASK` when needed).
- **Storage**: Linked account emails + per-account “show in mailbox summary” flags live in `SharedPreferences` as JSON (`AppPreferences`).
- **Setup**: Developers must create an OAuth **Web client ID** in Google Cloud Console and set `default_web_client_id` in `strings.xml` (or use the placeholder flow without `requestIdToken` where the platform allows).

### Mailbox summary (Assistant Insight–like)

- **Not** part of the chat tool loop. New `GmailMailboxSummaryRepository` fetches message metadata/snippets per visible account (search includes **inbox OR spam**), builds a structured text bundle, and calls Cerebras for **Markdown** output (not JSON like other insights).
- **Cache**: Reuse `cached_insights` with `InsightType.GMAIL_MAILBOX_SUMMARY` and a `dayKey` encoding `{date}_{rangeDays}` for invalidation.
- **Home**: Optional condensed card reads the same cache; toggle in Settings.

### Long-term memory (Koog LTM–like)

- Koog [long-term memory](https://docs.koog.ai/features/long-term-memory/) uses pluggable storage + retrieval/ingestion. On-device we implement the **same responsibilities** without the experimental API:
  - **Storage**: Room table `agent_memory` (multiple rows: manual vs auto).
  - **Retrieval**: When enabled, `ChatViewModel` prepends a `--- LONG-TERM MEMORY ---` block from stored rows (default-on for chat).
  - **Ingestion**: After a successful assistant turn, optional LLM call with a **configurable extraction prompt** appends concise auto rows; Memory screen offers **manual CRUD** and **“Update from planner data”** (tasks/habits/goals snapshot).

## Navigation

- Drawer: **Gmail Mailbox Summary**, **Memory Management** (alongside Journal, Pomodoro).
- Settings: Gmail linking, agent read/send toggles, summary prompt, home summary toggle, memory master toggle, extraction prompt, per-account summary visibility.

## Testing / verification

- `./gradlew :app:assembleDebug` after wiring Hilt and migrations.
- Manual: link Gmail test account, run summary, open chat with tools, edit memory.
