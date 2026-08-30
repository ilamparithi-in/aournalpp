# ADR 0014: Localization Architecture and String Resource Standards

## Status
Accepted

## Context
1. **Internationalization & FOSS Translation Readiness**: Aournal++ is an open-source Android application serving a global community. To enable community translation via platforms like Hosted Weblate or Crowdin, all user-facing strings must be decoupled from UI logic and stored in standard Android resource files (`res/values/strings.xml`).
2. **Translation Quality & Syntax Diversity**: Different human languages employ drastically different grammatical structures, word orders, and pluralization categories (e.g., Arabic has 6 plural forms, Slavic languages have 3, English has 2). Hardcoded string concatenation and unindexed formatters lead to broken translations and grammatical errors.
3. **Agent & Contributor Discipline**: Multiple AI agents and human contributors collaborate on this codebase. Without a formal, strictly documented convention, hardcoded strings easily slip back into new Jetpack Compose layouts and dialogs.

## Decision

### 1. Zero Hardcoded Strings Policy
- **Absolute Rule**: No user-facing text may be hardcoded as a raw Kotlin string literal in Composables, Activities, Fragments, or Dialogs.
- All UI text must reference Android resources via:
  - Compose: `stringResource(R.string.string_identifier)` or `pluralStringResource(R.plurals.plural_id, count, ...)`
  - Context / Activity: `context.getString(R.string.string_identifier, ...)` or `context.resources.getQuantityString(R.plurals.plural_id, count, ...)`
- **Untranslatable strings**: Technical constants, URL schemes (`sftp://`, `nc://`, `http://`), protocol keys, file extensions (`.xopp`, `.pdf`), and brand names must be marked with `translatable="false"` or kept in dedicated technical constants.

### 2. String Resource Naming Taxonomy
String resource names in `res/values/strings.xml` must adhere to consistent, prefix-based namespacing:

| Prefix | Usage | Example |
| :--- | :--- | :--- |
| `action_` | Interactive buttons, FABs, menu items, clickable verbs | `action_save`, `action_create_folder` |
| `title_` | Screen titles, card headers, dialog titles | `title_document_hub`, `title_delete_note` |
| `label_` | Form fields, metadata labels, status tags | `label_server_address`, `label_username` |
| `desc_` | Explanatory paragraphs, subtitle explanations | `desc_storage_permission` |
| `msg_` | Dynamic status messages, toasts, banners | `msg_sync_successful`, `msg_exporting_pdf` |
| `pref_` | Settings categories, toggle headers, slider titles | `pref_category_rendering`, `pref_dark_mode` |
| `dialog_` | Specific dialog body prompts, question confirmations | `dialog_delete_confirm_body` |
| `error_` | Error messages, validation alerts, failure notices | `error_invalid_port`, `error_auth_failed` |
| `tab_` | Primary top-level navigation tabs | `tab_home`, `tab_files`, `tab_cloud` |
| `plurals` | Any count-dependent quantity string | `<plurals name="items_count">` |

### 3. Formatting & Positional Arguments
- **Mandatory Positional Indexing**: Never use bare `%s` or `%d`. Always use positional indices (`%1$s`, `%2$d`) to allow translators to change the ordering of variables according to their language grammar.
  ```xml
  <!-- Correct -->
  <string name="msg_page_count">Page %1$d of %2$d</string>

  <!-- Incorrect -->
  <string name="msg_page_count">Page %d of %d</string>
  ```
- **Prohibition of Concatenation**: Never concatenate text using `+` or string templates like `"${getString(R.string.foo)} $bar"`. Place all static and dynamic text in a single parameterized string resource.

### 4. Quantity Strings (`<plurals>`)
- Use `<plurals>` for any string that varies based on a numerical count:
  ```xml
  <plurals name="notes_selected">
      <item quantity="one">%1$d note selected</item>
      <item quantity="other">%1$d notes selected</item>
  </plurals>
  ```

### 5. Context Comments for Translators
- When adding new string resources to `strings.xml`, provide an explanatory XML comment immediately above the entry describing:
  - Where the string is shown in the UI.
  - What each format argument (`%1$s`, etc.) represents.
  ```xml
  <!-- Button to test connection in Cloud Settings dialog. %1$s is the protocol name (e.g. Nextcloud) -->
  <string name="action_test_cloud_connection">Test %1$s Connection</string>
  ```

### 6. Automated Synchronization (Hosted Weblate / FOSS Workflow)
- `res/values/strings.xml` serves as the single source of truth for the base (English) locale.
- Community translators contribute localized `res/values-<lang>/strings.xml` files through continuous Git integration.

## Consequences
- Clean, maintainable UI codebase fully prepared for global community localization.
- Consistent user experience with proper grammatical pluralization and word order across languages.
- Direct compatibility with automated FOSS translation platforms (Weblate).
- Clear, enforceable standards for AI coding agents and human contributors.
