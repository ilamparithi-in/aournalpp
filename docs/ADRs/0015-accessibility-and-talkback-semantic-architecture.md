# ADR 0015: Accessibility (a11y), TalkBack Semantics, and WCAG 2.2 Standards

## Status
Accepted

## Context
1. **Assistive Technology Support & Digital Inclusivity**: Aournal++ is designed for handwriting, note-taking, and PDF annotation on Android devices across diverse user populations. Users relying on assistive technologies (Google TalkBack screen reader, Switch Access, voice control, keyboard navigation, and stylus interactions) require full, predictable access to all UI controls and document states.
2. **Compound Component Noise & Semantic Fragmentation**: In modern Jetpack Compose layouts (such as rich note cards containing thumbnails, format pills, pin badges, modification timestamps, and 3-dot overflow buttons), unmerged nodes cause TalkBack to read disjointed, out-of-context fragments as separate focus targets.
3. **Touch Target Size & Motor Accessibility**: Small icon buttons, color swatches, filter chips, and floating drag handles can be difficult to activate for users with motor impairments or when using styluses on high-DPI displays. WCAG 2.2 (Success Criterion 2.5.8 & 2.5.5) mandates a minimum interactive target size of $48\times 48\text{ dp}$.
4. **Contributor & Agent Architecture Alignment**: To ensure future screens, dialogs, and tools conform to accessibility and TalkBack standards without regression, a formal architectural pattern must be established and strictly enforced across the codebase.

---

## Decision

### 1. Centralized Accessibility Architecture (`AccessibilityUtils.kt`)
All reusable accessibility modifiers, constants, and semantic description builders must reside under `dev.ilamparithi.aournalpp.ui.util.AccessibilityUtils`.

- **Minimum Touch Target Modifier (`Modifier.minTouchTarget()`)**:
  Enforces a minimum bounding box of $48\times 48\text{ dp}$ via `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` without distorting visual layout padding or icon asset dimensions.
- **Heading Semantics Modifier (`Modifier.a11yHeading()`)**:
  Applies `semantics { heading() }` to enable TalkBack rotor heading navigation across all screen headers, sub-headers, and dialog titles.

---

### 2. Compound Card Merging & Custom Accessibility Actions
Rich composite items (such as `StandardNoteCard`, `ExpressiveNoteCard`, folder browser cards, and hero summary banners) must not present fragmented semantic children.

- **Merge Descendants Rule**: Apply `Modifier.semantics(mergeDescendants = true)` and set `role = Role.Button` on the outer card container.
- **Synthesized Content Descriptions**: The main card description must be computed using centralized synthesizers (e.g. `AccessibilityUtils.buildNoteCardA11yDescription(...)`) communicating:
  1. Primary state (e.g., Selected, Pinned)
  2. Document / folder title
  3. File format (`.xopp`, `.pdf`, `.xoj`)
  4. Parent folder location
  5. Formatted last-modified timestamp
- **Custom Accessibility Actions (`customActions`)**: Secondary and overflow operations (such as *Pin/Unpin*, *Export PDF*, *Share*, *Rename*, *Duplicate*, *Delete*, *Restore*, *Customize Appearance*) must be registered directly in `customActions` via `CustomAccessibilityAction(label) { ... }`. This allows TalkBack users to execute actions directly from the local context menu without traversing deep dropdown menus.

---

### 3. Roles and Dynamic State Descriptions
All interactive elements must explicitly declare their semantic role and dynamic state description:

| Control Type | Semantic Role | `stateDescription` Values | String Naming Pattern |
| :--- | :--- | :--- | :--- |
| **Radio buttons & Swatches** | `Role.RadioButton` | `state_selected` / `state_not_selected` | `cd_visual_color_picker`, `cd_choose_custom_emoji` |
| **Switches & Feature Toggles** | `Role.Switch` | `state_enabled` / `state_disabled` | `cd_finger_as_stylus_enabled`, `cd_toggle_torch` |
| **Multi-select Checkboxes** | `Role.Checkbox` | `state_selected` / `state_not_selected` | `cd_note_selected`, `cd_conflict_version` |
| **Tabs & View Selectors** | `Role.Tab` | `state_selected` / `state_not_selected` | `hub_view_carousel`, `hub_view_compact_grid` |
| **Expandable Panels / Keyboards**| `Role.Button` | `state_expanded` / `state_collapsed` | `cd_toggle_keyboard_show`, `cd_toggle_keyboard_hide` |

---

### 4. Accessibility String Taxonomy in `res/values/strings.xml`
In compliance with [ADR 0014](file:///home/ilam_common/DevHome/GitHub/xopp-android/docs/ADRs/0014-localization-and-string-resource-architecture.md), all accessibility strings must be stored in `strings.xml` with zero hardcoded literals and clear translator notes:

- `cd_*`: Content descriptions for icons, image buttons, handles, and swatches (`cd_exit_note`, `cd_drag_toolbar_handle`).
- `action_*`: Custom accessibility action labels (`action_open_note`, `action_pin_folder`, `action_duplicate_note`).
- `state_*`: Dynamic accessibility state descriptions (`state_selected`, `state_enabled`, `state_expanded`).
- `a11y_*`: Parameterized announcement templates (`a11y_note_card_template`, `a11y_conflict_version_template`).
- `<plurals>`: Pluralized item counts (`hub_section_folders_count`, `hub_section_notes_count`).

---

### 5. Minimum Touch Target Enforcement ($\ge 48\times 48\text{ dp}$)
Every interactive control (clickable icons, 3-dot overflow buttons, drag handles, filter chips, swatch circles, toggle buttons) must meet or exceed $48\times 48\text{ dp}$. If the visual icon is smaller (e.g., $16\text{ dp}$ or $24\text{ dp}$), apply `Modifier.minTouchTarget()` to expand the interactive touch hitbox.

---

### 6. Automated Testing Standards
All accessibility description builders and utility functions must be covered by automated unit tests in `app/src/test/java/dev/ilamparithi/aournalpp/ui/util/AccessibilityUtilsTest.kt`. Tests must verify:
- Accurate string synthesis across file types, pinned states, and multi-selection modes.
- Folder card announcements including custom folder roles and localized counts.
- Cloud sync conflict version announcements.
- Preservation of minimum touch target constants ($48\text{ dp}$).

---

## Consequences

- **Full WCAG 2.2 Level AA Compliance**: Guarantees accessible target sizes and structured heading hierarchies across all app screens.
- **Cohesive Screen Reader Experience**: Eliminates repetitive semantic clutter, allowing TalkBack users to browse note lists quickly and access secondary actions effortlessly.
- **Strict Guidelines for Future Features**: Any new Composable or Activity introduced in future sprints must implement `Modifier.minTouchTarget()`, `Modifier.a11yHeading()`, semantic roles, and localized `cd_*` string resources.
- **Maintainable & Testable**: Centralized synthesis logic ensures a single source of truth that is automatically verified via unit tests during CI builds.
