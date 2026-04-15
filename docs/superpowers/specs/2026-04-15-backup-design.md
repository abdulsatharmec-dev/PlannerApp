# Backup and restore design — DayRoute / Daily Curator

**Date:** 2026-04-15  
**Status:** Approved (conversation)  
**Audience:** Implementer  

## Problem

The app stores data locally (Room SQLite, SharedPreferences, and on-disk files such as journal voice attachments). Uninstall or clearing app storage removes that data with no recovery unless a copy exists elsewhere.

## Goals

1. **Automatic backup** when the device is on **unmetered network (Wi‑Fi)**, to reduce risk of silent data loss.
2. **Manual export** of the same backup artifact so the user can store an emergency copy anywhere (Drive via share, USB, email, another cloud).

Non-goals for this spec: multi-user sharing, public hosting, or a custom paid backend. The app remains **personal / single-user**.

## In scope for each backup snapshot

| Component | Included |
|-----------|----------|
| Room database | Yes — `daily_curator.db` (or equivalent export) |
| SharedPreferences | Yes — export as JSON or verifiable copy of prefs payload |
| On-disk attachments | Yes — e.g. journal voice files not embedded in DB |

Implementation may use one **ZIP** (or encrypted ZIP) per snapshot containing the above with a small **manifest** (schema version, app version, UTC timestamp, optional device id hash).

## Cloud target (automatic backup)

- **Primary:** **Google Drive** under the signed-in Google account (OAuth, no self-hosted server).
- **Folder:** A **user-visible** Drive folder (e.g. `DayRoute backups` or product name) so backups appear in the Drive app, not only hidden app-specific storage.
- **Retention:** Keep the last **N** backup files on Drive (e.g. 10–30); delete older remote objects to respect free-tier quota.

If a future iteration targets another provider (e.g. Nextcloud via WebDAV), the **packaging** step (build ZIP) stays the same; only the **uploader** changes.

## Automatic backup behavior

- **Scheduler:** `WorkManager` **periodic** task (e.g. daily), with constraints:
  - `NetworkType.UNMETERED` (Wi‑Fi)
  - Optional: `requiresBatteryNotLow`
- **Incremental urgency:** Optionally schedule an **one-off** run after meaningful local writes (debounced), still gated on unmetered network.
- **Naming:** e.g. `dayroute-backup-YYYYMMDD-HHmmss.zip` (or encrypted extension).
- **Multi-device:** **Last successful upload wins** using manifest timestamps; minimal UI acceptable for a single-user app. Document that restoring from an older file overwrites newer local data if chosen explicitly.

## Manual export and restore

- **Export:** “Export backup” produces the **same archive format** as automatic backup, then uses **SAF** (save to user-chosen location) and/or **share sheet** for flexible placement.
- **Restore:** “Restore from backup” lets the user pick a ZIP via SAF, shows a **strong warning** that restore **replaces** current local DB, preferences, and attachment files. Optional UX: offer “export current data first” before destructive restore.

## Authentication and errors

- **Google Sign-In** (or equivalent) with Drive scope appropriate for app-created files (e.g. `drive.file` / folder-scoped access per current Google API guidance).
- If the user is **not signed in**, surface a clear prompt; do not fail silently on scheduled runs.
- On upload failure, **retry** on next eligible run; optionally surface last error in settings. Preserve **last known good** local state always.

## Security

- Drive access is bounded by the user’s Google account.
- **Optional:** Passphrase-based encryption for ZIP for manual copies that may sit on shared storage. Automatic Drive copies may rely on account security alone unless the product owner requires at-rest encryption in the archive.

## Testing

- **Unit:** Build ZIP from fixture DB + prefs + sample files; validate manifest and contents.
- **Instrumented:** Restore flow replaces DB and prefs; app launches with restored data.
- **Manual checklist:** Sign-in, Wi‑Fi upload, airplane mode / offline behavior, retention pruning, full round-trip export → clear data → restore.

## Open implementation choices (not blocking spec)

Exact WorkManager interval, value of **N** for retention, and whether debounced post-change jobs are in **v1** or a follow-up can be decided in the implementation plan.

## Approval

Design approved by product owner in conversation on 2026-04-15 prior to this document.
