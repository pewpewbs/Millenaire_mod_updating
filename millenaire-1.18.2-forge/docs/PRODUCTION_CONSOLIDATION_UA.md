# Production consolidation and acceptance

## Baseline

The supplied production_audit(7).json records 7 cultures, 285 NPC profiles,
499 goals, 0 ERROR and 239 REVIEW. The 239 are 183 starting-supply reviews,
28 absent culture-local producers with external prices, 26 delivery-list
reviews, and 2 leisure flower-gathering reviews. There are 140 village/lone
building definitions. All have an empty unresolved_recipe_inputs list after
the auditor's external-price-assisted reachability calculation. This is not
a runtime simulation, and does not establish live trade access or prosperity.

Do not turn every culture into a self-sufficient economy solely to reduce
REVIEW counts. Do not remove delivery routes or prices solely to silence the
auditor. No additional cultural recipe, price or building-plan edits in this
package; earlier configuration repairs remain intact.

## Runtime changes

- Mining and fishing must reach the actual loaded target and cannot generate
  resources through stuckAction. Registered source and work-site identity
  are rechecked; unsupported legacy sources are not selected.
- Mining, fishing, crafting and cooking require a continuous work interval.
- Crafting rechecks stock, recipe inputs and the selected work position.
  Cooking rechecks furnace/firepit registration and may collect completed
  output even when the stock gate no longer permits new input.
- Gathering rechecks stock and workplace, handles a full chest transactionally,
  and stops when the next target is absent. Brick collection pays only after
  the source block is successfully removed.
- Null homes/town halls are not considered valid generic workplaces.

## Existing saves

Stock plan source-marker corrections apply to new builds. On active server
buildings, the narrow repair also checks the eleven changed coordinates
across five known plan/variation combinations: two Indian quarry variants,
Mayan mine, Japanese quarry and Japanese lone mine. The exact current base
marker must match; no later non-empty plan override is permitted. Existing
registered stone may be replaced with the corrected sand/gravel only when
the live block is still stone, its support is sturdy and the space above is
clear. If the correct live block is already present, its registration can
be repaired without placing a block. Other blocks, missing or unregistered
old stone, custom buildings and unloaded chunks are preserved. Unloaded
chunks are retried. This is idempotent when the new source is registered
and present. Changed registrations request a normal town-hall save.

Back up an existing world before first launch. Check MILL-PRODUCTION-SOURCE-REPAIRED
and MILL-PRODUCTION-SOURCE-REVIEW in the log. The latter is not permission to
overwrite user-built blocks. Old Japanese gravel plans created with the
incorrect palette color may lack prior registration: do not recreate blocks
automatically when the expected old registration is absent.

## One acceptance command

From millenaire-1.18.2-forge:

    python tools/validate_production.py --build

This runs the Python regression suite, the standalone Java migration
decision smoke test, the full static PNG audit and the Gradle build in order.
It fails on the first failed step. No manual repeated audits per change.
The Java smoke test checks 384 possible state combinations, not a Forge
world. Live acceptance still needs a backed-up existing save plus a new world:
observe mining, fishing, workshop deliveries, furnace processing, and
construction/pathing across cultures. A zero-ERROR audit cannot establish
live NPC access or game performance.
