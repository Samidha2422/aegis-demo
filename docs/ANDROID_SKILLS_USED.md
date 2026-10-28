# Android Skills Used & Resolution Log

This document records all Android skills enumerated, analyzed, and applied for the Aegis Demo project in accordance with Operating Rule R1.

---

## Skills Inventory & Application

| Skill Name | Location / Origin | Applied Where | Conflict Resolved / Design Decision |
|---|---|---|---|
| **adaptive** | `~/.agents/skills/adaptive/SKILL.md` | `ShortNavigationBar` for phone layouts, adaptive transitions, flexible window handling. | Used `NavigationSuiteScaffold` pattern concepts, ensuring insets are applied individually per screen rather than at the root to avoid clipping edge-to-edge content. |
| **edge-to-edge** | `~/.agents/skills/edge-to-edge/SKILL.md` | Root Activity (`enableEdgeToEdge`), Top Bars, Chat Composer, Keyboard handling. | Resolved IME inset double padding: configured `adjustResize` in Manifest and consumed `innerPadding` from `Scaffold` with `fitInside(WindowInsetsRulers.Ime.current)` / `imePadding` on composer. |
| **navigation-3** | `~/.agents/skills/navigation-3/SKILL.md` | Navigation architecture, multi-backstack state preservation, ModalBottomSheet destinations. | Decoupled screen routes using type-safe sealed class routes; state is preserved per top-level destination across configuration changes. |
| **jetpack-compose-performance** | `~/.agents/skills/jetpack-compose-performance/SKILL.md` | `LazyColumn` message lists, `SafeMessageBubble`, feed comments. | Enforced primitive keys and `contentType` definitions for all items in `LazyColumn` to avoid recomposition churn and slot table de-allocation during fast scenario scrolling. Avoided redundant `@Immutable` on collection classes under Strong Skipping mode. |
| **testing-setup** | `~/.agents/skills/testing-setup/SKILL.md` | Unit test infrastructure, Fake repositories, ViewModel test harnesses. | Configured JUnit 4/5 + Coroutines Test (`runTest`, `TestDispatcher`) for ViewModels and `ActionPolicy` unit tests. |

---

## Conflicts & Resolutions

1. **Adaptive Navigation vs Edge-to-Edge:**
   - *Conflict:* Putting `safeDrawingPadding` on the parent `NavigationSuiteScaffold` causes severe clipping of background surfaces into the system bar area.
   - *Resolution:* Preserved edge-to-edge draw-through by allowing scaffolds to draw behind system bars and applying insets to inner list `contentPadding` and composer containers.
2. **Material 3 Expressive Version Alignment:**
   - *Conflict:* Material 3 `1.4.0` hid several Expressive APIs.
   - *Resolution:* Targeted `androidx.compose.material3:material3:1.5.0-alpha06` / `1.5.0-beta01` with `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` enabled module-wide.
