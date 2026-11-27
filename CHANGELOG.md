# Журнал змін Millénaire+ (CHANGELOG)

Цей файл фіксує **додані можливості, зміни поведінки, виправлення й їхню прив'язку до версії мода**. Номери версій беруться з `millenaire-1.18.2-forge/gradle.properties` (`mod_version`), а не вигадуються за кількістю комітів.

> **Стан на 2026-11-23:** код мода — `8.1.2-port.0`; Minecraft **1.18.2**, Forge **40.3.0**, Java **17**. Це версія порту в розробці, а **не підтверджений стабільний реліз**. Наведені нижче зміни перебувають у гілці `fix/villager-door-access-and-village-territory`, **не в `main`**.

> **Правило читання журналу:** короткі SHA однозначно шукаються в [історії робочої гілки](https://github.com/pewpewbs/Millenaire_mod_updating/commits/fix/villager-door-access-and-village-territory/). Повна версія до редакційного скорочення — [незмінний знімок dbcd606](https://github.com/pewpewbs/Millenaire_mod_updating/blob/dbcd606984193eeaf720ea9952bcd93d4932a415/CHANGELOG.md). Скорочено повторювані URL комітів, однакові рядки та повторні нагадування про main; всі ідентифікатори, гіпотези, статуси, виміри й технічні результати збережено. Ігрові спостереження мають пріоритет над статичною перевіркою.

## [Unreleased] — `8.1.2-port.0` (гілка `fix/villager-door-access-and-village-territory`)

Період змін: **22–23 листопада 2026 року** (за датами комітів UTC). Базовий коміт `main`: `a1cde6e6`. На момент створення цього журналу гілка мала **62 додаткові коміти**.

### 27.11.2026 — Швидша ротація іконок їжі

- Іконки їжі на мапі змінюються кожні 1,5 с замість 3 с; матеріали доріг залишаються на 3 с. Рецепти, групування, стабільність слотів і період оновлення підказки не змінено. Додано статичну перевірку; збірка й ігровий тест не запускалися.

### 27.11.2026 — Компактний статус будівництва на мапі

- Спільна підказка мапи: початкове будівництво показує лише «Будується»; поліпшення — «Поліпшення: 6 → 7» з цільового рівня ConstructionIP (стандартний проєкт завжди підвищує рівень на один). Збережено золотий колір та окремий статус незбудованої будівлі. Звичайний рядок рівня не дублюється під час будівництва. Додано статичну регресійну перевірку; Forge та ігровий тест тут не запускалися.

### 27.11.2026 — Як відрізнити прохід через двері від відкривання

- За `LogPathing >= 2` запис `DOOR-CROSS` показує результат спроби пройти конкретні двері або хвіртку на маршруті. Запис містить жителя, завдання, напрямок, координати, початковий стан стулки, тривалість та кількість тактів без руху. Початок відліку прив'язаний до підходу до отвору, завершення — до проходу тіла за дальній край. Для кожного жителя одночасно відстежується один отвір.
- Скасоване завдання, перехід через сусідню стулку, переривання спостереження, телепортація і 100 тактів очікування мають окремі результати. Позначки ручного керування й живлення відповідають стану, що спостерігався при зверненнях до цих дверей.
- У попередньому ігровому журналі було 217 відкривань і 204 команди закриття. За цими числами неможливо визначити кількість фізичних проходів. Після оновлення її можна виміряти за результатом `CROSSED`. Вимірювання на живому поселенні ще потрібне.

### 27.11.2026 — Спільні маршрути, обхід сторонніх будівель та спуск

- Мешканці можуть повторно використати середину шляху, починаючи чи закінчуючи поруч на тому самому поверсі. Два зміщені індекси за ділянками по 8 блоків посилаються на наявний кеш світу. Короткі підходи з’єднуються одиничними кроками. Усі переходи перевіряються в напрямку руху. Для додаткової перевірки таких маршрутів мешканці одного світу ділять бюджет в 1 мс на ігровий такт. Лічильник `cache_corridor` показує успішні проходження перевірки.
- Для звичайних завдань пошук надає перевагу обходу внутрішньої частини компактних житлових і робочих будівель та дверей сторонніх споруд. Ділянки початку й кінця звільнені від цієї надбавки. Якщо звичайний JPS знайшов прохід через сторонню будівлю, той самий знімок місцевості використовується для порівняння з обходом. Єдиний прохід залишається доступним.
- При спуску на один блок житель, який уже падає над запланованою клітинкою приземлення, може відпустити пройдену верхню точку. Після перетину центра нижньої клітинки він може завершити падіння без розвороту.
- Тести охоплюють 25 пар початку й кінця однієї вулиці, строки життя й видалення маршрутів, різні поверхи, обхід, єдиний прохід, двір та падіння в сусідню клітинку.

### 27.11.2026 — Робоча позиція шахтаря в зареєстрованому кар’єрі

- Звичайні шахтарі й завдання культур обирають джерело, з якого можна видобути ресурс. Маршрут веде на перевірену позицію. Після тривалої невдачі житель тимчасово відкладає це джерело. Здобич видається після штатного інтервалу з повторною перевіркою самого блока, доступу й чинних обмежень запасу.
- Відтворено випадок Kaland на фрагменті 11×11×11 блоків. Попередній добір давав десять геометричних позицій, першою була клітинка шахтаря. Новий добір пропустив закритий камінь у всіх 154 повтореннях. Відкриття проходу дозволило видобуток. В ізольованому тесті перевірено також інше джерело, старе призначення, ліміт складу, повторні спроби та точну кількість здобичі.
- Для відтворення тестові класи завантажують таблицю старих блоків із ресурсів збірки та ініціалізують потрібні об’єкти Forge. Початковий стенд не мав цих даних. Окремий контроль показав, що край підлоги може перекрити доступ до близького джерела. Зі штатної робочої позиції той самий тест пройшов.

### 27.11.2026 — Shorter chat and localized upgrade status

- Chat shows the speaker name only; recipient metadata, dialogue suppression, range and speech colours are unchanged. The resource panel now supplies the upgrade level to the translation placeholder, consistent with the three other upgrade status calls in PanelContentGenerator. Ukrainian, English and Russian wording updated. Added two source regression guards. No gameplay changes; Forge build and runtime testing pending.

### 27.11.2026 — Production provenance and presentation polish

- Straw-bed-only crafting omitted from map purpose, resident activity and travel-book production/consumption listings; actual crafting, housing, trade and resource accounting unchanged. Wet-to-dry brick tooltip reflects actual intermediate block. Free-output goals show that no inputs are consumed and show real output quantities; no fake recipes. Fort's intentional specialization (no sugar plantation or spice garden) documented; no automatic resource injection. Rice plan PNG inspection remains pending. Static guards updated; compile/playtest not run here.

### 27.11.2026 — Social pairs and vanilla-first acacia

- Socialising residents display an arrived status; dialogue selects a shuffled pool of nearby available residents who have reached a meeting point. Assign partners only after a compatible authored two-person dialogue is found; reciprocal target validity and cleanup prevent abandoned dialogue. Existing v1/v2 content format unchanged (no three-person dialogue support). Work retains priority. Ukrainian, English, Russian status labels added.
- Registered acacia saplings use the vanilla grower in roomy sites. Compact fallback requires obstruction of minimum vanilla clearance, actual failed vanilla placement, unchanged sapling and Forge permission; a veto or replacement never triggers compact fallback. Ordinary growth and bone meal untouched. Uses actual 1.18.2 TreeFeatures.ACACIA configuration. No culture-specific exceptions. Recheck both Indian and Byzantine groves in game. Static regression checks updated; Forge and game not run in this environment.
- Stable road/food icon ordering and category/recipe tooltip, omit straw-bed activity icon, colour level details. No construction economy changes.

### 27.11.2026 — Map: compact production and upgrade presentation

- Localized separate level line; real next-level material stacks and counts in hover, removed generic construction wording. Construction hover takes precedence over planned locations and displays amber status. Brick gathering uses pickaxe; mixed extraction uses one pickaxe, soft-only remains shovel. Straw-bed production hidden from map output only. Food and road outputs rotate every three seconds; chosen item retains its own recipe. No recipe or stock changes. Source checks added; Forge/game not run here.

### 27.11.2026 — Плавніший рух і проходження водних точок маршруту

- На рівній відкритій ділянці житель може націлитися на дальшу точку свого маршруту. Перевірка охоплює підлогу та весь простір для тіла. Біля дверей, сходів, огорож, посівів і країв платформи залишаються початкові точки. Зіткнення під час скороченого проходу повертає жителя до пропущених точок.
- Для жителя у воді перевірка досягнення точки враховує горизонтальну відстань і глибину. На вертикальному відрізку перевіряється висота. Біля запланованого берега з вільним місцем для тіла житель може спробувати вистрибнути. Додаткового періодичного телепорту немає.
- Збірка Forge пройшла. Геометричний тест виконав 51 перевірку, зокрема на відкритій прямій, повороті, суцільних і половинних блоках, біля перешкод та у воді. Приймання у грі передбачає порівняння ходи на тому самому маршруті й повну доставку через водойму.
- Попередній пакет навігації опубліковано в робочій гілці комітом `63a15b8`. [Аудит і план від 27 листопада](millenaire-1.18.2-forge/docs/Millenaire_Audit_2026-11-27_UA.md) фіксують стан на цьому коміті.

### 26.11.2026 — NAV-PHASE2: directed routes, ladder control and door geometry

- Based on handoff §§26–27 and local base 9c0ece1. JPS reconstructs intermediate heights in search direction; rejects endpoint mismatches. Reject +2 upward edges. AS route follower retains validated corners and ladder rungs instead of vanilla shortcut/airborne skipping, uses physical support height.
- Ladder body stays clear of the panel in all four facings; exclusive ladder/ground movement ownership prevents stale ground JUMPING during descent. Isolated rung completion and first-rung ground approach retained.
- Read-only saved hotspot (22,66,571) reproduces east-facing OPEN door panel across north/south stair entrance. Planner now checks actual door panel shape in the directed passage state; server manager selects the same state, respects player override and powered doors, checks occupants before closing a sideways panel. No automatic building/door rotation.
- Full Forge build PASS; 175 Python checks and production validation PASS; 11 navigation Java smokes plus ladder/transition smokes PASS; 32 executable Forge geometry/follower/JPS assertions PASS. No new in-game traversal or controlled performance acceptance yet. Phase 2 remains open. Details and reproduction: millenaire-1.18.2-forge/docs/NAVIGATION_PHASE2_2026-11-26_UA.md. Local changes, not committed/pushed/merged.

### 26.11.2026 — Validator: stale crop planting label

- Local updater at 58863fb: 174/175 Python tests passed, including sleep pose, sleep reservations and acacia checks. One stale assertion expected the previous `Plants crops` / `Садить культури` label after the intentional noun-based map UI change. Updated the assertion to `Crop planting` / `Посадка культур`; retained the shared crop and animal capability guards. Checked remaining assertions in the failing method against current source. No production Java changes. Installer correctly aborted before Java smoke tests, audit, Forge build and installation; rerun required.

### 26.11.2026 — Sleep pose lifecycle follow-up

- Source review of 8c9b25f found checkGoals resets actionStart after every performAction, then reassigns shouldLieDown from GoalSleep.shouldVillagerLieDown() (now false) at the next interval. Preserve the occupancy-validated pose across sleep work intervals; retain stopMoving on active sleep ticks. Invalid occupancy, leaving action range, clearing/completing the goal still clear the pose. Other goals unchanged. Added source regression guard. Full Forge build and actual night-cycle verification pending.

### 26.11.2026 — Sleep occupancy and model lighting

- GoalSleep: reserve physical bed halves and floor footprint, consider other NPC sleep goals and nearby sleepers plus vanilla bed occupied state; skip unavailable chunks and duplicate candidate coordinates; remove unconditional occupied sleeping-marker fallback. Live validation before lying down. No change to daytime navigation or villager beds' construction. If no free safe spot, NPC does not lie down; night-action availability in overcrowded houses needs in-game verification.
- Sleeping black model screenshot: likely lighting sampled at standing eye height inside roof. Render only sleeping NPC using actual light sampled in nearby air at feet/above; do not force full brightness or change block light. Exact cause unconfirmed until client test; renderer and sleep changes uncompiled here. Added source guards.

### 26.11.2026 — Acacia planting and Byzantine wool

- User confirms spruce planted, acacia not planted even after manually grown tree was chopped. Live ResManager previously required complete 5x5x6 air volume for choosing acacia planting. Separate planting viability (registered marker, loaded chunk, replaceable target, valid soil and border) from tree growth. Lower straight trunk clearance now only center through y=2; upper checks natural nonpersistent acacia foliage, not player blocks; compact feature size lower radius zero through y=2. Acacia planting logs on actual successful placement. No new chunk loading, no forced block removal. Exact cause on the user's existing grove not runtime-proven.
- Verified Byzantine shepherd collectGood/bringBackHomeGood wool_white, trade list wool_white, tailor recipes makeclotheswool and makeclothes_byz_wool plus yellow/purple carpets use white wool and dyes. Byzantine NPC shearing returns white wool; sheep killed by Byzantine NPC have only wool drops normalized to white, keeping amounts, other drops, animal colours, player actions and other cultures unchanged. Tooltip reflects policy. Tests updated to follow live manager, not legacy class. Full build/runtime not yet verified.

### 26.11.2026 — MAP readability and deferred gameplay requirements

- Map: adults first then children; portrait hover shows current action in yellow, profession in aqua and actual configured non-leisure travel-book goal icons. Native title muted, purpose and output headings tinted, unbuilt status red. Level suffix now "lvl N", no parentheses or max denominator; maximum stays gold. Pinned tooltip level/name hover shows next plan level, declared resident capacity/irrigation/shop/tag differences and material costs when available; no fabricated architectural description. Ukrainian Byzantine corner border post label simplified. Crop purpose labels use nouns; extraction and logging site wording.
- BACKLOG / next navigation implementation (do not implement now): pathfinding must avoid walking on planted crop beds, use existing field paths and reachable edge work positions; evaluate planting/harvest access without breaking goals. Fix gate close lifecycle after NPC passes, including interrupted goals, player priority, herd escapes, unloaded chunks and gates shared by multiple actors. Reproduce screenshot where sheep escaped the Byzantine enclosure. No change to current route planner or gate logic in this UI patch.
- BACKLOG / future economy and reputation: assess penalty for stealing settlement-owned crops, distinguish village plots from player-owned plots and accidental harvest, prevent repeated/false penalties. Not implemented.
- Build and live verification pending.

### 26.11.2026 — HOTFIX: acacia live resource manager API

- User's first one-click run (a6dae8a): 173/173 Python tests, Java smoke tests and production audit passed; compileJava failed at GoalGenericPlantSapling.java:114 because Building.getResManager() returns buildingmanagers.ResManager, not legacy BuildingResManager. Installer correctly cancelled before touching installed JARs.
- Added canPlantAcaciaAt and the selection guard to the actual live ResManager; public shared GroveAcaciaGrowth.hasCompactRoom enables safe cross-package calls. Registered grove markers use the compact clearance, non-grove sites retain simple planting viability. No change to updater, growth shape or navigation. Corrected regression test to follow Building's actual return type and check live selection/action wiring. Previous five-block veto was in the legacy class, not established as the cause of the in-game issue. New full compile and in-game planting remain to be verified.

### 26.11.2026 — ONE-CLICK UPDATE / VALIDATE / INSTALL

- Root Update-Millenaire.bat checks the working branch, runs git pull --ff-only, and launches the updated PowerShell installer only after success. No reset, merge commit or automatic discard of local edits.
- Update-Millenaire.ps1 resolves artifact name from gradle.properties, uses local JDK 17 for production validation (Python tests, Java smoke tests, audit), then calls build-local.ps1 once for Forge build. Any validation/build failure aborts before touching installed mods. Preserves existing Codefication1182 ATLauncher instance, guards against its running Java process, checks Forge archive metadata and SHA256, backs up old JARs and attempts rollback on install failure. No game code changed. Windows end-to-end execution not performed here.

### 26.11.2026 — MAP-LABELS / TRAVELBOOK-ICONS / ACACIA-PLANTING

- Map: Ukrainian дитина/дитини/дітей with teen exceptions, English/Russian corresponding inflection; level suffix (lvl current/max), at max (lvl current) with existing gold colour. No change to unbuilt or non-upgradable buildings.
- Building detail travel book: associated village types now use six-column icon-only reference buttons; original hover and click routing retained, no text duplication. Other lists unchanged.
- Acacia: prior planting filter rejected another marker within five blocks even though compact growth permits a straight trunk and compact crown. Registered grove markers now use the same loaded-area, soil, border and actual clearance check as compact growth, not the old neighbour-distance veto. Non-grove sites retain old rule. Recheck acacia at action time and only consume a carried sapling after successful placement, retaining original free-planting policy. No changes to player-placed trees, activation or generation. Added source regression tests. Forge build and in-game planting not yet verified.

### 25.11.2026 — HOTFIX: cold village activation deadlock

- Runtime report after 31a7823: all villagers frozen. latest(20261125-220239).log: normal join and save, zero NAV-/DOOR-/MILL-SLOW/VILLAGE-PREP entries between 23:00:30 and 23:02:24. Source confirms saved buildings start with empty VillageMapInfo; loadChunks required nonzero map width, isVillageChunksLoaded returned false on empty map, and only active updateTownHall initialized the map. This is a bootstrap deadlock, not a pathing failure.
- Derive chunk-aligned bounds from saved building locations, construction locations and village radius without scanning or allocating map. Use the same geometry for terrain-map sizing. Request chunks before map initialization; require FULL map plus one-chunk border; initialize terrain before publishing active state. No hard wait for all Forge ticket bookkeeping when actual area is available.
- Remove staged global MSPT admission gate and temporary-ticket wait for existing village activation. Restore Forge ticking tickets with up to four claims per village/tick, at most one cold synchronous claim; this favors correctness/progress over aggressive throttling. Keep generation preparation queue and scoped terrain cache unchanged. Add MILL-ACTIVATION waiting/active and static regression guards. No save migration or AI goal changes; build and actual existing-world recovery still need verification. Cold forceChunk can still cause a short stall.
- Startup measurements from same log: cultures 11384ms, building-points 1191ms, languages 667ms; deferred task 13.72s. Startup optimization separate from freeze.

### 25.11.2026 — PERF: activation tickets and terrain classification

- User validated 07bff86: 168/168 Python tests and Forge BUILD SUCCESSFUL. No gameplay log for that build yet. Reopened latest(20261125-212112).log: 1482 repeated slow building updates, exact internal cause not proven. Confirmed separate ticket stalls 511/457/155/104 ms with attempted=4, terrain scans 200–706ms, final village build 4311ms. Reverse ENDPOINT_MISMATCH chose FORWARD, preserve safety checks. optionsManagerViewed is logged by JourneyMap, not Millenaire. Deferred Millenaire startup 10.22s remains to profile.
- Prepare one FULL chunk per active loader via temporary vanilla ticket; promote to original Forge ticking ticket only when getChunkNow succeeds. Shared per-world admission budget, slows >=40 MSPT, pauses >=50; timeout and release on inactivity. Cache required rectangle while stable. Fix activation axis mismatch; avoid cold chest link reads and duplicate lookup. Add diagnostic timings for save, chest links, inn, visitors, spawn, unlock, exploration, terrain refresh and construction queue.
- Cache terrain classifications per synchronous chunk scan only, preserve state-dependent log tags and terrain criteria. No changes to construction transaction, navigation or other mods. Source guards updated; build and in-game tests not yet run; original 1482 stalls not declared fixed. Forge promotion/removal can still have synchronous costs.

- Added subsystem timings MILL-STARTUP and final generation phase timings for centre, initial buildings, village initialization, rush construction. Chest guard checks actual FULL via getChunkNow. Startup and final synchronous build are measured, not claimed optimized in full; no unsafe background registry/world mutations.

### 25.11.2026 — MAP-SEMANTICS / GROVE-ACACIA / LIVE-GENERATION

- Звіт користувача: 17 скриншотів Hagios Nikolaos і latest(20261125-212112).log (21:24:59–22:20:44, 33 721 рядків). VILLAGE-PREP: 13 кандидатів, 4 завершені підготовки, 3 layout-rejected, 1 успішна генерація (byzantines/religiousvillage, -3615/71/2623; build 4311 ms), 65 pause/65 resume. View distance 8, simulation distance 10. Три окремі споруди: lonedislogcabin (Inuit), loneinn і abandonedmill (Norman). Це підтверджує роботу черги, але не гарантує частоту спавну на 2–4 чанках і не усуває синхронний фінальний build.
- VILLAGE-GEN-CHAT: нові звичайні lone buildings повідомляються в чаті за наявним радіусом 2000 блоків; раніше повідомлення обмежувались ключовими/спеціальними спорудами. Немає повторних повідомлень для вже зареєстрованих споруд.
- MAP-RESIDENT-KEY: порівняння housePos запису з loc.chestPos (ключ реєстру Building), а не геометричним loc.pos. Знімок записів центру збережено, без завантаження сутностей/чанків. Немає лічильника мешканців для ще не збудованих споруд. Рівні додано до перекладеної назви (золотий колір на максимумі).
- MAP-SEMANTICS: житло/прихисток — ліжко; торгівля — гаманець; вилучено дубльовану назву типу крамниці та позначки ймовірнісної здобичі. Мінеральна сировина — добування, врожай — збирання, тварини/риба — отримання, фруктові дерева — вирощування. Лопата для піску/гравію/глини, кирка для гірських порід. Окремі поля родини землеробів (Agrós tag=farm, Choráfi tag=wheat); не приписуємо врожай житловому будинку. Одна іконка вовни з поясненням кольору; фактична стрижка не змінена. Шахта Latomeío містить miner_byzantine та wife_byzantine; остання має makedirtwallbyz і makestrawbedbyz, тому крафти домогосподарства справжні. Патруль і молитва відображаються за конфігурацією GoalGenericVisit (patrol/gopray), а не за назвою споруди. Спільний інтерфейс EN/UK/RU; українські goal.* спрощено до «Добуває…», tendfurnace — «Переробляє ресурси в печі».
- GROVE-ACACIA: експериментальний компактний варіант прямостовбурної акації на acaciaspawn у grove, тільки під час планового росту. Перевірка завантаження чанків, ґрунту, повітряного об'єму, меж світу; Forge growth event із повагою до DENY, без виконання неперевіреної заміни feature; відновлення саджанця при невдалій генерації, якщо його позиція порожня. Для непридатного компактного простору залишено звичайний grower. Не змінює правила інших дерев/кісткового борошна. Фактична причина невдачі росту у конкретному гаю ще не доведена.
- BUILDING-SLOW: у сесії 538/504/440 повільних оновлень будівель -1465/81/1172, -3609/71/3060, -1574/67/2090 (1482 разом). Причина не встановлена. Активація перевіряє повні доступні чанки через getChunkNow, включно з краєм незрівняної області; порожня карта не активує споруду. MILL-SLOW додано для activation, grove, pens, signs, initial-packets, refresh-goods. Потрібні нові логи для локалізації затримок.
- Змінено source regression guards (map, chat, activation, grove). Повна Forge-збірка, новий прогін тестів та ігрова перевірка цього пакета не виконані. main не змінено.

### 25.11.2026 — VALIDATOR: stale progression assertion

- Локальний запуск користувача на 0aba3e4: 163/164 Python tests passed, усі 5 village-generation wiring tests passed. Один застарілий assertion у test_operational_priority_is_soft шукав прямий return тернарного виразу, хоча після додавання діагностики значення зберігається в chosen і повертається без зміни логіки.
- Оновлено лише тест: перевірка тернарного вибору й return chosen у межах select, з нечутливістю до пробілів; production Java не змінено. Валідатор зупинився перед Java smoke/Forge build, тому їхній результат у цьому запуску невідомий. Потрібен повторний повний запуск.

### 25.11.2026 — VILLAGE-PREP: adaptive natural village area preparation (experimental)

- User approved generation independent of low client render distance. Preserve original fast path for ready areas and existing biome, distance, spawn protection, territory, terrain and layout checks. Only incomplete-area rejection of ordinary natural generation submits a candidate. Explicit player spawning, custom settlements, lone buildings, hamlets and other dimensions retain original paths. Candidate admission checks player proximity (200 blocks horizontally), distance and generation settings; eligibility/type selected only with FULL centre available, checked again before construction. Does not change authored village types, trade specialization or deferred ideas.
- VillagePreparationQueue uses overworld SavedData with up to 8 pending candidates, one active. Seed, completion ratio and selected culture/type saved; active progress/tickets are runtime-only and reconstructed after restart. No new permanent forced chunks. Dedup nearby candidates; suppress natural recursive callbacks in active region plus halo and briefly after release. One temporary radius-zero FULL ticket at a time, wait via getChunkNow; hold previously prepared chunks until existing generator finishes; no synchronous getChunk/join in queue itself. Default 16x16, max 22x22 (484 direct tickets), larger layouts retain old path. Vanilla dependencies may load additional surrounding chunks. Release on completion, rejection, timeout, no players, overload lasting 1200 ticks, disabled generation and unload. Revalidate distance while preparing.
- Adaptive interval: normally 2 ticks, at >=40 MSPT or >=15ms of current Mill world work 10 ticks, at >=55 MSPT or >=30ms world work pause. Resume after load drops. VILLAGE-PREP queued/preparing/paused/resumed/build/finished diagnostics. Queue is server-thread-only; no unsafe asynchronous world mutation. Existing final layout/build/hamlet phase remains synchronous and may still produce a spike. Exact spawn density not guaranteed because candidate selection/order changes on incomplete areas and queue cap/suppression may skip candidates. Needs controlled 2–4 vs 12–16 chunk in-game tests, ticket leak/memory monitoring and restart test. Do not claim full independence until verified.
- Added pure Java policy smoke test and 2 source wiring guards; integrated into validator. Local javac --release 17 and Java smoke execution passed 2351 assertions (area, uniqueness, negative coordinates, backpressure and maximum bounds). Exact Git blob hashes verified: policy ed3215c, smoke 23e0363. Full Forge compilation, Python suite and actual game behavior not yet verified.

### 25.11.2026 — LIVE-SESSION-183520 / MAP-PURPOSE-ICONS / RESOURCE-STATUS / GENERATION-TRACE

- latest(20261125-183520).log, 15798 рядків, 18:34–19:34. 25 повільних village-generation, max 577 мс; 1 Can't keep up 2103 мс на вході; 13 повільних area-check (7 found=false, 6 true); 3 build-and-register (-856/64/1128, -2360/128/1656, -2712/100/2024), два повідомлення про lone building Apomonomeno Boske Probaton. Немає підтвердження появи звичайного нового села. View distance 4, о 19:24:45 змінено на 8. За default radius=80 вимагається область 16×16 чанків (chunkRadius=7 та додатковий кінцевий чанк); вузький коридор при польоті може не забезпечити її. Default мінвідстані село-село 500, село-lone 250, spawn protection 250+radius, biome validity default 60%, usable area >70%. Фактичні конфіги світу не отримані. Не змінено критерії, ймовірність, чергу чи правила спавну. Додано VILLAGE-GEN-REGISTER з kind/type/culture/pos і VILLAGE-GEN-REJECT при LogWorldGeneration>=2 (area-incomplete, distance, no-eligible-type, layout, villages-disabled). Потрібні контрольований політ зі збільшеною view distance та лог причин. Окремо 23 повільні оновлення будівлі -1465/81/1172 — наступний аудит.
- 9 NAV-TRAP-RESCUE, 16 DOOR-BLOCKED, 865 stalled=true; 3 rescue Saburo Kato goal=construction о 19:26:35–47, повторний abandoned=true. Попередній цикл Ami Tanaka не повторився. 1146 ASTAR-SEARCH goal=chat. Не порівнювати сирі лічильники різних за тривалістю/навантаженням сеансів як контрольований benchmark.
- **BACKLOG NAV-PARTIAL-COLLISION (для окремого поглибленого аудиту, не реалізовано):** користувач спостерігає застрягання будівельника й можливо інших NPC біля скринь та драбин: боковий рух у драбину, частковий об'єм скрині, невідповідність планованого маршруту фізичній AABB. Перевірити swept AABB / VoxelShape для бокового руху, опори, часткових блоків, різних орієнтацій, драбин та всіх аналогічних блоків у A*/JPS/vanilla fallback і фактичному контролері руху. Не вважати це доведеною причиною без координат/станів блоків; не реалізовувати зараз.
- **BACKLOG TRADE-RESOURCE-SUPPLY — доповнення, не реалізовано:** в панелі необхідних ресурсів показувати замовлений обсяг, дефіцит, жовтий статус «Очікується поставка» замість червоного для підтвердженого вантажу та ETA в ігрових днях (не вигадувати ETA без моделі доставки; скасоване замовлення знову дефіцит). Ціна між поселеннями залежно від їхніх відносин/репутації; додатковий торговий бонус спеціалізованих торговельних поселень. Зберегти спеціалізацію, реальні запаси, уникати створення ресурсів з нічого. Лише за окремим погодженням.
- Japanese recipes: genericcrafting/japanese/makepaperrice.txt 4 rice -> 1 paper; genericcrafting/banners/makebannerrice.txt 10 rice -> 1 banner_white; japanese_crafter має обидві цілі. Це авторські рецепти, не змінено. japaneselookout_A.txt має initial.tag=patrol, japanese_samurai має goal=patrol; вежа призначена для патрулювання, точну поведінку на висоті ще не перевірено.
- UI: uk goal.gosocialise «Йде спілкуватися»; верхній відступ 14 усунуто, резерв ширини збоку для X у перших двох рядках; призначення житла (ліжко), заїжджого двору (золотий деньє), patrol (щит) у спільній карті для всіх культур, три мови. Панель населення за відсутності клієнтської сутності відрізняє «поза спостереженням» від server missing; відсутній не означає загиблий. У ресурсних панелях старий ui.lackingresources приховано лише коли всі фактично показані доступні ресурси достатні; інші статуси не змінено. Це корекція UI, не зміна черги будівництва. Лог 19:05:39 forge affordable=false, 19:09:36 centre-style selected sakebrewery/1, missing gravel, точний момент скрину не встановлено.
- Додано 3 source regression guards. Forge-збірку, Python suite та Minecraft playtest ще не виконано. Main не змінено.

### 25.11.2026 — JAPANESE-TRADE-SPECIALIZATION / BACKLOG: TRADE-RESOURCE-SUPPLY

- За рішенням користувача скасовано JAPANESE-WHEAT-BOOTSTRAP для торговельного японського поселення: trading.txt відновлено побайтово до версії перед додаванням ферми (f2ac0c8), без start=japanesefarm, з never=japanesefarm. Спеціалізація на саке та залежність від імпорту збережені. Інші типи поселень і виробничі механіки не змінено. Оновлено конфігураційні регресійні тести. Існуючі ферми в збережених світах не видаляємо.
- **Відкладена ідея — TRADE-RESOURCE-SUPPLY, не реалізовувати зараз.** Після стабілізації порту дослідити та покращити міжселищну торгівлю для всіх культур і спеціалізованих поселень: виявлення дефіциту будівельних та виробничих ресурсів (наприклад wheat/thatch), пошук фактичних постачальників і надлишків, пріоритет необхідних вантажів, надійність доставки, залежність від відстані, відносин і доступності поселень. Перед вибором архітектури перевірити реальну роботу торговців і причини недоставки. Не створювати ресурси з нічого, не робити всі поселення самодостатніми, не примушувати генерацію чанків для торгівлі. Розглянути безпечні механізми для ізольованих поселень без постачальника. Реалізація лише за окремим погодженням користувача.

### 25.11.2026 — MAP-RESIDENT-SNAPSHOT / RESIDENT-LABEL

- Isesake: мешканці показувалися лише після наближення до будинку. BuildingMapTooltip.staff вимагав building!=null і прив'язував записи до об'єкта будівлі. Тепер окремий повний snapshot записів із успішно прочитаного пакета центру, housePos.sameBlock(loc.pos) незалежно від завантаженого будинку або NPC; при явному запиті карти оновлюємо пакет центру. Без примусового завантаження чанків, без зміни формату пакета. Невідома кількість не показується як 0. Поточна дія доступна лише для синхронізованої сутності.
- Рядок фактично рахує мешканців, а не робочі місця: «Мешканці / Residents / Жители» для будівель із житловими слотами, включно з виробничими будинками; дорослі відносно планових слотів, діти окремо (+N) і портрети. Померлі та нападники не враховані. Не змінено населення, професії чи AI. Додано source regression guard, повна збірка й ігрова перевірка ще потрібні.
- Уточнення економіки: japanese/traded_goods.txt містить wheat і thatch; торговельні поселення оригінально залежать від імпорту. Building.attemptMerchantMove вимагає іншого типу поселення, неворожих відносин, заїжджого двору та відстані <2000. Наявність механізму не гарантує фактичного постачання. Стартова ферма з попереднього коміту збережена.

### 25.11.2026 — JAPANESE-WHEAT-BOOTSTRAP

- Isesake: торгівельний тип з центром sakebrewery не мав доступу до пшеничної ферми. Перевірено конфігурації nogyo, gunji, seiji, trading, japanese_controlled, japanese_customcontrolled: тільки trading серед стандартних типів містив never=japanesefarm; інші вже мають start=japanesefarm і start=japanesegrove. Це обмеження контенту, а не помилка ранжування VillageProgression.
- trading.txt: замінено заборону japanesefarm на одну стартову ферму, нарівні з гаєм. WorldGenVillage.generateVillage читає startBuildings для звичайної природної генерації та створення гравцем. Не змінено решту типів, інші культури, хутори, рецепт, торгівлю, AI. Власноруч спланований japanese_customcontrolled збережено без автоматичної забудови.
- japanese_farmer (Hyakushou) і japanese_farmwife (Tsuma) мають harvestwheathome/plantwheathome. japanese_farmwife та japanese_peasantwife мають makethatch і requiredGood=wheat,16. Потреба в запасі не є розміром рецепта. Пшениця — сировина для покрівлі, не блок сіна.
- Зміна для нової генерації; наявні поселення не отримують готову ферму від цього патча. Додаткова обов'язкова ділянка 20x14 може підвищити вимоги до придатності стартового планування; не послаблювали геометрію. Потрібно перевірити нове торговельне поселення та реальне вирощування пшениці. Додано два конфігураційні regression tests; Forge-збірку і повний тестовий набір не запускали.

### 25.11.2026 — NAV-TRAP-LOOP / CHAT-PATH-RATE / TICKET-BATCH / PROGRESSION-TRACE / ENTITY-ICONS / LEGACY-CONFIG

- Сесія latest(20261125-151108).log без дослідження нових чанків: 201 NAV-TRAP-RESCUE (200 Ami Tanaka), 0 DOOR-BLOCKED, 6 MILL-SLOW на старті світу (дві будівлі 927/1813 мс, вкладені підсумки); один Can't keep up 2972 мс. 1999 записів про chat path searches; 718128 скопійованих секцій за діагностикою попереднього аналізу. Ці дані не підтверджують швидкість генерації нових територій.
- PhysicalTrapRecovery не трактує позу сплячого NPC як привід для аварійного перенесення; перевірка після пробудження збережена. MillVillager при повторному звільненні поблизу за 200 тіків скидає невдалу ціль/маршрут і тимчасово уникає цієї позиції (200 тіків, радіус 1.5). Не гарантує усунення неправильної геометрії/спального місця; потрібна перевірка в грі.
- Chat: окреме обмеження нових пошуків до одного за 40 тіків, збереження активного маршруту при зсуві партнера менше 3 блоків, припинення пошуку в межах дистанції розмови. Не змінено 10-тіковий ліміт переслідування рухомих робочих цілей, A*/JPS primary, геометричні перевірки. Менша кількість пошуків має зменшити snapshot copies; сам кеш секцій між тіками НЕ розширено, бо відсутня універсальна інвалідація всіх мутацій світу. Фактичний приріст і потреба окремої оптимізації snapshot ще невідомі.
- BuildingChunkLoader: максимум 4 спроби нових Forge tickets на поселення за тік, без зміни потрібного прямокутника; активація поблизу гравця після готовності чанків і tickets. Повільні етапи village-tickets, building-townhall, building-source-reconcile логуються від 100 мс. Це обмеження пакетної роботи, не асинхронний доступ до світу; один ticket все ще може бути повільним, кілька сіл можуть діяти в одному тіку.
- VillageProgression: діагностика рішення (rank, centre-style, construction-supplier, operating-supplier, no-eligible-project), кількості кандидатів і нестачі матеріалів, обмеження 200 тіків на поселення; фактичний picked з рівнем/доступністю/резервом. Правила економіки й ранжування не змінено.
- build.gradle: generateMillenaireEntityIcons створює відсутні entity_icon з реальної текстури 64x32 або її пропорційного HD-варіанта (обличчя+зовнішній шар) для всіх підпапок культур; існуючі іконки не замінюються. Збірка має підтвердити створені файли; сумісність JourneyMap потребує ігрової перевірки.
- П'ять відомих застарілих конфігураційних ключів більше не дають помилку unknown config; при переписуванні користувацького файлу старі значення залишаються як коментарі. Інші невідомі ключі, чинні налаштування і прив'язки клавіш Forge не змінюються.
- Dynamic Surroundings 5.0.0.4: сторонній Forge Version Check отримав некоректний JSON versions.json (Unterminated object); мод продовжив запуск, OpenAL ініціалізовано. Не змінено сторонній мод.
- Додано 6 source regression guards. Повну Forge-збірку та ігровий тест не проводили. Не змінено main. Залишаються окрема перевірка генерації нових територій, реальних причин повільного першого оновлення та можливе подальше скорочення обсягу snapshot без послаблення безпеки.

### 25.11.2026 — MAP-ANIMAL-DROPS / MAP-CROP-SOURCE / INN-PURPOSE / UNIT-COUNT

- Ігрові спостереження Isesake: рецепт із числами поверх іконок після vanilla overlay працює (скрин залізної мотики); користувач просить не показувати 1. Також приховано 0/0 для планів без покращень; загальні дії «Забиває тварин», «Садить культури», «Садить саджанці», без повторного напису про зрошення у товарі.
- GoalGenericSlaughterAnimal призначає ціль атаки, а звичайні дропи збираються через MillVillager.getGoodsToCollect/pickUpGoods. japanese_chickenfarmer має collectGood і bringBackHomeGood для chickenmeat/feather/egg; slaughterchicken не задає bonusItem. ProductionCapability.animalGoods описує відомі vanilla види здобичі, які професія збирає й повертає додому, включно з яйцем як окремим продуктом курки; бонуси враховують required tag. Не симулюємо loot tables, не гарантуємо кількість, не змінюємо забій/підбирання/доставку.
- Japanese wheat farm japanesefarm_A.txt (priority=1000, japanese_farmer/japanese_farmwife) має професії з harvestwheathome/plantwheathome. japanese_farmwife також прописана у chickenfarm_A.txt; звичайна наявність домашньої цілі не доводить наявність грядок. ProductionCapability.hasCrop перевіряє реальні зареєстровані ґрунти на сервері для існуючих будівель, маркери плану/попередніх рівнів для майбутніх та клієнта; 10 типів відповідно до BuildingPlan.referenceBuildingPoints. Кеш за планом, без читання чанків. Спільна перевірка у BuildingMapTooltip і VillageProgression.recipes; усуває хибних постачальників, але не гарантує пріоритет, якщо ферма відсутня у типі поселення, недоступна або не вистачає ресурсів. Не змінено правила вибору/вартість/порядок будівництва. Конкретний стан проєктів і запасів Isesake ще не перевірено.
- Shijin = japaneseinn_A.txt, initial.tag=inn, initial.male=japanese_merchant (visitor, localmerchant); Building.updateInn/attemptMerchantMove/transferMerchantStock забезпечують міжселищну торгівлю. Підказка тепер відрізняє прихисток мандрівного торговця від звичайного житла. Не змінено механіку торгівлі.
- Додано source regression guard. Збірку Forge та повний набір тестів не запускав; реальна поява ресурсів, пріоритет пшеничної ферми та рендер одиничної кількості потребують ігрової перевірки. Гілка main не змінювалась.

### 25.11.2026 — MAP-COMPACT-PURPOSE / MAP-SPECIES / MAP-PORTRAITS / MAP-COUNT-OVERLAY-2

- Повторна перевірка у грі (Isesake, 7 скринів): попередній see-through прохід не виправив перекриття кількості моделями; пшениця об'єднувала розведення й урожай; у гаї показувався лише дуб, хоч ростуть також ялини; дублювалися описи видобутку, продажу та безкоштовного крафту; зайві порожні рядки робочих місць/призначення; X зміщений.
- Спільний UI для всіх культур: «Призначення», «Працівники»; невідоме призначення та відсутність фіксованих робочих місць не показуються. Загальні категорії крафту, видобутку, збирання, печі; розведення — пшениця, урожай — залізна мотика. Іконки товарів без повторного «Продається»; безкоштовний крафт — тільки назва, без розміру партії; рецепти з інгредієнтами й справді додаткова інформація (імовірнісні дропи/зрошення) збережені.
- Лісозаготівля: 6 видів деревини за маркерами посадки та відповідними колодами/саджанцями плану, включно з попередніми рівнями й доступними woodspawnTypes; кеш планів; без сканування світу або завантаження чанків. Це потенційні види за планом, не інвентаризація кожного дерева, доданого гравцем. Вовна — усі підтримувані фактичним GoalShearSheep кольори, не лише біла; інуїтський бонус кісткового блока позначено імовірнісним.
- Працівники у вигляді 16x16 портретів з фактичної синхронізованої текстури NPC/запису; 64x32 UV відповідно до трьох моделей Millenaire, шар капелюха; за відсутності текстури — стандартна голова. Hover закріпленої панелі: ім'я, професія, поточна дія або чесно невідомий стан. Немає нових мережевих запитів, повні імена не розтягують панель. Кеш доступності текстур на GUI. Хрестик центрується за шириною гліфа і висотою шрифту.
- Кількості рецептів: vanilla ItemRenderer.renderGuiItemDecorations з явним числом, у тому ж model-view перетворенні, після всіх моделей. Попереднє рішення з окремим framePose не використовуємо. Три нові статичні regression guards; збірка, повний тестовий набір та ігровий рендер ще не перевірені. AI, економіку, реальні рецепти й будівництво не змінено. Залишок аудиту legacy-виходів та точна перевірка портретів/кількостей у грі потрібні.

### 25.11.2026 — CHUNK-GEN-LAG (перша оптимізація) / DOOR-PAIR-RUNTIME-REMOVAL

- Лог попередньої ігрової сесії latest(20261125-142711).log: 657 повідомлень MILL-SLOW stage=village-generation; максимальне зафіксоване відставання сервера 79786 мс о 14:01:31. Сукупний лог не доводить, що всі затримки викликані одним підетапом. У генераторі 17×17 альтернативних позицій мають перекривні області; WorldUtilities.isChunkGenerated для незавантаженого чанка синхронно читає NBT з chunkMap. Однакові перевірки повторювалися.
- VillageGenerationProbe кешує позитивні й негативні результати лише в межах однієї спроби пошуку ділянки. Збережено порядок перебору, існуючі межі прямокутників (включно з додатковим кінцевим чанком), правила вибору території та перевірку повної генерації. Не додано примусового завантаження чанків чи постійного кешу негативних результатів. Зупиняємо пошук решти обов'язкових стартових споруд після першої невдачі, оскільки весь план уже непридатний. Це може змінити послідовність випадкових чисел після відхиленої спроби; існуючі споруди не перебудовуються.
- Додано окремі MILL-SLOW для village-area-check (probes, cacheHits, found), village-biome, village-chunk-preparation, village-terrain-map, village-centre-site, village-start-layout, village-reserve-sites, village-build-and-register, village-hamlets (поріг 100 мс). Підетапи можуть вкладатися в загальний час, не підсумовувати їх без урахування вкладеності. Синхронні завантаження при фактичному будівництві/генерації хуторів залишилися; переносити мутації світу на фоновий потік не можна. Потрібен новий профіль ігрової сесії перед наступною оптимізацією.
- За рішенням користувача видалено власне парне відкривання для гравця та NPC у VillageDoorAccess (pairedDoor/openPartner). Нормалізацію FACING/HINGE у BuildingPlan, одиночне відкривання, lease, ручний override, безпечне зачинення та хвіртки збережено. Double Doors залишається необов'язковим зовнішнім модом; реальну сумісність ще не перевірено.
- Додано VillageGenerationProbeSmoke та 2 статичні regression guards; оновлено guard парних дверей і validate_production.py. Чистий Java 17 тест виконано локально: 289 порівнянь областей + 6 додаткових перевірок, 479 унікальних перевірок і 8665 повторно використаних результатів на синтетичній карті. SHA файлів ідентичні GitHub (43d17e1, bc6e22d). Це не вимірювання FPS/TPS або дискових затримок Minecraft. Повна Forge-збірка, Python suite та ігрове тестування ще потрібні.
- Інші відкриті спостереження з логу: повторні NAV-TRAP-RESCUE, застрягання Reina Maeda, невдалі маршрути plantRice/harvestRice та DOOR-BLOCKED залишено окремими задачами; не змінено AI, тваринництво або чергу будівництва цим пакетом.

### 25.11.2026 — MAP-ACTION-DEDUP / MAP-BREED-TAGS / MAP-RECIPE-COUNT

- Повторні ігрові скрини Isesake: список крафтів дублював продукцію та рецепти; цифри кількості перекривалися 3D-іконками; дія розведення показувалася лише за професією, незалежно від тегів будинку.
- Спільна карта всіх культур: верстак показує загальну дію «Виготовляє ресурси», піч — «Працює з піччю», рецепти лише у підказках продукції. Розведення потребує одного з тегів sheeps/cattle/pigs/chicken, іконка — пшениця. Для крафтів без input виводиться відсутність витрат сировини й кількість за цикл, без повторення назви дії. Японська селянка має makepathdirt/makepathgravel/makepathgravelslabs; конфігурації дають по 8 блоків без input. Конкретні теги будинку на скрині не підтверджені.
- Цифри рецептів малюються окремим see-through текстовим проходом після моделей. Змінено лише UI, не виробництво чи AI. Forge-збірка та візуальний тест ще потрібні.

### 25.11.2026 — нові ігрові спостереження: CHUNK-GEN-LAG / UI-BUTTON-FIT / VILLAGE-TYPE-LABEL

- CHUNK-GEN-LAG: користувач повідомляє про сильні лаги при прогрузці нових чанків. Причину не підтверджено без нового latest.log. WorldGenVillage.generateVillage синхронно викликає world.getChunk(x,z) для відсутніх сусідніх чанків, winfo.update, пошук ділянок, побудову стартових споруд, розчищення дерев та генерацію hamlets. Зіставити час зависань з MILL-SLOW stage=village-generation (>=100 ms), іншими MILL-SLOW і Can't keep up. Попередні оптимізації preload/збереження не довели усунення першопричини; не змінювати генерацію навмання.
- UI-BUTTON-FIT: скрин керування проєктами Touor/Muraile: підписи «Заборонити поліпшення» та «Вилучити будівлю» виходять за межі кнопок і накладаються. GuiControlledProjects формує дві кнопки; GuiText.buttonPagination ділить фіксовану ширину на 1/2/3, не враховуючи ширину локалізованого тексту. Потрібно системне підлаштування підписів (з урахуванням іконок, масштабу GUI, доступності повного підпису), перевірити всі меню й мови, не лише скорочувати два переклади.
- VILLAGE-TYPE-LABEL: скрин GuiNewVillage: Bukkyou Mura, Gunjiteki Mura, Nouji Mura, Nautchiaqabvik мають лише назву культури, у норманів типи відображаються. GuiNewVillage додає getNameTranslated лише якщо не null; VillageType.getNameTranslated залежить від Culture.canReadBuildingNames (languageLearning вимкнено або knowledge >=100, за відсутності профілю true). Ключі перекладу присутні; конкретний стан knowledge у світі ще не підтверджено. Пропозиція: у меню створення поселень показувати локалізований тип незалежно від знання мови, не змінюючи решту механіки. Окремо перевірити всі village.* на відповідність реальним типам: japanese seiji (Bukkyou Mura) зараз «імперське село», має бути релігійне. Не зачіпати сторонні переклади.
- Лише аудит і запис. Код/конфігурації не змінено; розширене тваринництво залишається відкладеним.

### 25.11.2026 — BACKLOG: розвиток тваринництва (відкладено за рішенням користувача)

**Не реалізовувати зараз.** Спочатку стабілізувати поточний порт і перевірити вже внесені виправлення в грі; повноцінне керування стадами може бути непропорційно великим за обсягом і витратами на сервер. Повернутися до цих ідей лише за окремим погодженням.

| Ідея | Поточна поведінка / підстава | Можливий розвиток |
|---|---|---|
| HERD-OWNERSHIP / ESCAPE | Тварини обліковуються за відстанню, не за належністю фермі. Втікачі поза зоною обліку можуть спричинити нічне поповнення стада; сусідні тварини гравця можуть потрапити в облік. | Прив'язка до господарства, безпечне повернення втікачів, захист чужих тварин, запобігання необмеженому розмноженню через відкритий загін. |
| PEN-BREEDING-RESERVE | Забій рахує доросле поголів'я всієї ферми, не окремих ізольованих загонів. | Облік зв'язності загонів, збереження щонайменше племінної пари в кожному придатному загоні. |
| SLAUGHTER-BONUS-LIFECYCLE | GoalGenericSlaughterAnimal нараховує bonusItem при призначенні цілі атаки, до підтвердження смерті. slaughtersheepbyz2 задає mutton і bone 100%, leather і feta 50% (номінальні шанси конфігурації). | Нарахування бонусів лише один раз після підтвердженого забою, без дублювання або втрати звичайної здобичі. |
| FETA-PRODUCTION | Фета є бонусом забою, а не продуктом переробки молока. | Доїння овець і виробництво фети з молока; окремо визначити баланс, рецепти та UI. |
| LONE-SHEPHERD-FUEL | loneshepherd_byzantine стартує з 16 колод акації, купує деревину, але не має завдання рубання дерев. Після вичерпання палива приготування баранини може зупинитися. | Обмежена самостійна заготівля для власної печі або збереження торговельної залежності; не перетворювати вівчаря на повноцінного лісоруба. |

Перед майбутньою реалізацією оцінити вартість навігації/обліку, збереження стану в старих світах, сумісність з усіма культурами та сторонніми модами. Розбити на незалежні мінімальні зміни; не вважати ці ідеї погодженими до виконання. Цей запис — лише документація, код і конфігурації не змінено.

### 25.11.2026 — CROP-SEED-SURPLUS / NPC-ANIMAL-XP

- Уточнення ігрового тесту: успішну стрижку спостерігали у візантійців, не норманів; інші культури не підтверджені.
- Для трьох wheat harvest goals (home, tagged field, paddy) додано opt-in surplusseed=seeds у спільному GoalGenericHarvestCrop. Ліміт діє лише на вторинний дроп, не на основний урожай/зрошення/активацію завдання. Резерв: дві посівні на зареєстровану площу (мінімум 16 на поле), локальний буфер, дві одиниці корму на маркер курки, потреби профілів мешканців, більший з trade target/reserved. Ураховуються запаси відомих будівель та живих NPC; обсяг дропу обмежений дефіцитом (в т.ч. множник). Наявні запаси не видаляються. Для віддаленого незавантаженого складу fallback на локальний резерв, для невідомого локального — не обмежувати; не примушувати чанки завантажуватися. Не гарантує доставку між фермами. Рис, кукурудза, бавовна, морква, картопля, куркума, виноград та інші основні продукти не обмежені цим механізмом. Майбутні окремі seed-дропи можуть увімкнути ту ж опцію.
- NPC-ANIMAL-XP: користувач повідомив про досвід від тварин, убитих жителями. LivingDeathEvent записує ознаку фактичного смертельного удару MillVillager для Animal/WaterAnimal; LivingExperienceDropEvent обнуляє XP лише для таких смертей. Звичайні дропи, вбивства гравцем, загальні правила світу та вже створені сфери досвіду не змінені. Сторонні моди, що породжують XP поза Forge event, не перевірені.
- Поріг забою: дорослі особини в радіусі 15 горизонтально / 10 вертикально навколо будівлі > кількість маркерів відповідного типу (aggressiveSlaughter — половина, округлення вниз). Не по окремих загонах; при 4 маркерах нормальний забій з 5 дорослих. Можлива втрата пари в одному ізольованому загоні; поріг не змінювали, потрібен окремий аудит зв'язності та реального поголів'я.
- 12/12 чистих Java-перевірок політики запасу пройшли локально з javac --release 17; ідентичність файлів у GitHub підтверджена blob SHA e1215fb і 77911a7. Додано 4 статичні regression guards; включено в validate_production.py. Повна збірка, запуск нових Python-тестів та ігрова перевірка ще потрібні.

### 25.11.2026 — ігрове спостереження: вівчар і надлишок насіння

- ANIMAL-WORK: користувач повідомив, що стрижка овець тепер виглядає нормально в одному випадково згенерованому **візантійському** поселенні (культура уточнена користувачем; первинно помилково названо нормандською). Часткове позитивне ігрове підтвердження; інші культури, загони і довготривалі цикли ще не перевірені.
- CROP-SEED-SURPLUS (питання користувача, підтверджено механіку): harvestwheat і harvestwheathome дають wheat 100%, seeds 100% та seeds 75%; plantwheat і plantwheathome витрачають одну насінину. У GoalGenericHarvestCrop умова randomInt(100) > chance пропускає випадки понад chance; фактичний шанс другого дропу за рівномірного 0..99 становить 76%, якщо тюнінг кількості не змінено. harvestwheathome має villagelimit=wheat,512, harvestwheat не має цього ліміту; ліміт насіння не заданий. Зростання запасу можливе, але фактичне переповнення складу не підтверджено. Майбутній аудит: баланс насіння для всіх культур, резерв на пересівання, корм/торгівлю, облік доступних запасів та інвентарів; обмежувати додатковий дроп, не блокувати збирання основного врожаю. Код не змінено.

### 25.11.2026 — повторний тест карти після c956cd6: MAP-PIN-LOCK / MAP-LAYOUT / MAP-ACTIONS / MAP-RECIPES / L10N-GOAL-VERBS

- Ігрові скрини: закріплення переносило панель в інше місце; можна було відкрити іншу будівлю без закриття; іконки перекривали вторинну підказку; «Що робить» було довгим текстом, рецепт стейка — текстом без іконок; над будівельницею «Будівництво», а не «Будує». Попередня реалізація 0f7e2e4 недостатня.
- Панель використовує позицію останнього hover, однаковий layout до/після кліку (обмеження межами екрана), X/Esc, блокує кліки карти до закриття; резерв заголовка, окремий вторинний tooltip на вищому Z за предметами, візуальні рецепти з реальними кількостями та стрілкою. Групи дій за класом Goal, піч об'єднує приготування й обслуговування; меч, сіно, ножиці, інструменти тощо. Спільно для всіх культур; невідомі legacy-рецепти не вигадуються.
- У shared languages/uk/strings.txt змінено 336 значень goal.*; ключі збережено, звичайні дії короткі («Будує», «Стриже овець»), ремісничі фрази також скорочено до дієслівних форм із узгодженням відмінків. Оновлено попередній regression guard рецептів та додано три статичні перевірки карти і локалізації. Змінено лише goal.*; діалоги й інші переклади не зачіпались. Використання спільних ключів поширює зміни на карту, NPC та діагностику. Повна збірка і візуальна перевірка ще потрібні.

### 25.11.2026 — локальна збірка d29d4a0 та виправлення компіляції

- Звіт користувача: 136/136 Python-тестів, 384/384 перевірок міграції, 28/28 чистих Java-перевірок дверей/загонів. Аудит: 7 культур, 285 профілів, 499 цілей, 239 REVIEW, 0 ERROR.
- Forge :compileJava FAILED (22 с): BuildingMapTooltip.java:221, повторне оголошення змінної source у describe. Перейменовано локальну змінну циклу на sourceState разом з усіма її використаннями; логіку не змінено.
- Повторна Forge-збірка після цього виправлення та ігрові перевірки ще не підтверджені.

### 25.11.2026 — перевірки поточного пакета (перед Forge-збіркою)

- Окремий Java 17 smoke-test чистих функцій DoorPairRules/PenAllocationRules: 28 перевірок геометрії 4 FACING × 2 HINGE та пріоритетів розподілу тварин. Пройдено локально для ідентичних файлів (blob 3f844cc, 6530079, fececc3). Це не матриця проходження дверей у Minecraft. Додано запуск у validate_production.py та п'ять статичних regression guards.
- Панелі ресурсів також пояснюють дефіцит, коли фізичний запас більший за доступний: склад / недоступно / доступно / потрібно. Різниця не обов'язково тільки резерв. Активне будівництво не змінено.
- Результати повної Forge-збірки, усіх Python-тестів та ігрової перевірки ще не отримано. Статус повторних проблем не закрито.

### 25.11.2026 — MAP-DETAILS / UI-WHAT-DOES / L10N-ABBOT-ROLE (реалізація, не перевірено в грі)

- Карта: клік закріплює панель, Esc знімає закріплення, інша будівля перемикає вибір. Панель повторно знаходить локацію в синхронізованому списку, оновлення 500 мс. Резидентні виробничі ролі зіставлено з записами жителів поточного будинку, виведено зайняті/передбачені місця, окремо позначено відсутність працівників. Для спільних робочих місць без заданого штату не вигадувати кількість. У закріпленій панелі hover працівника показує доступну синхронізовану ціль, якщо живий NPC доступний; інакше статус невідомий.
- Іконки закріпленої панелі показують назву предмета і джерела з фактичних Goal: crafting — сировина/вихід, cooking — вхід, harvest — культура, slaughter — тварина/імовірнісні дропи, legacy — відома дія. Планове виробництво та торгівля не видаються за фактичне. Що робить — перелік доступних дій; потенційні результати окремо. Не всі legacy-рецепти розшифровані до точних інгредієнтів; серверний запит причин блокування не доданий.
- Українські написи над NPC використовують локалізовану професію, якщо вона доступна; квестові короткі мітки без дубля native role. «Повернутися до абата» підтримано явно, для інших ролей після «до» нейтральна конструкція з іменем; повна система відмінювання та квестові довгі тексти не змінені. Паралельну languages/uk не чіпали.
- Потрібні Forge-збірка та ігрова перевірка закріпленої панелі, відображення/масштабу, синхронізації NPC, рецептів, українських квестових міток.

### 25.11.2026 — ANIMAL-PEN-SPAWN / DOOR-PAIR (реалізація, не перевірено в грі)

- updatePens вибирав випадковий маркер незалежно для кожної тварини. Додано AnimalPenAccess: групування маркерів за прямою низькою колізійною видимістю, облік існуючих тварин, завершення пари перед заповненням інших загонів; перевірка завантаженого чанка та фізичного простору. Не переміщує наявних тварин, не збільшує загальну норму, вовків/кальмарів залишено на старій схемі. Це консервативна апроксимація, не повний flood-fill загонів; складні непрямі проходи можуть бути розділені. Уже ізольовані тварини при повному стаді автоматично не перерозподіляються.
- GoalBreedAnimals перевіряє наявність дорослого партнера того самого типу з низькою колізійною видимістю; не витрачає корм на очевидно ізольовану ціль, при виконанні потрібна видимість NPC. Не заміняє Vanilla-розведення.
- DOOR-PAIR: у спільному планувальнику нормалізовано однозначні суміжні однотипні двері на одній висоті з поперечною віссю проходу, однаковий FACING і зовнішні HINGE, включно з верхньою половиною. Неоднозначні ряди дверей та перпендикулярні незалежні двері не об'єднуються. VillageDoorAccess відкриває виявлену пару для NPC та синхронізує ручний клік (не redstone, не sneak); чинні lease/override збережено. Закривання кожної стулки окремо за фізичною безпекою. Не переписує вже поставлені неправильні двері у світі.
- DOOR-ORIENTATION (363 EAST у старому лозі) не закрито: повна матриця реального проходження ще не виконана. Збірка/ігрова перевірка нового коду потрібні.

### 25.11.2026 — PROGRESSION-OPERATING-INPUTS / RESOURCE-PANEL (реалізація, не перевірено в грі)

- Попередня пріоритетність враховувала лише нестачу матеріалів будівництва. Додано м'яку перевагу доступному локальному виробнику дефіцитного корму/вхідних матеріалів ремесла та печей перед залежним проєктом; без примусового будівництва за відсутності доступного виробника, з урахуванням наявних запасів, альтернативних зернових для тварин і наявних планових виробників. Не повний рекурсивний граф експлуатаційних залежностей; не гарантує наявність працівників у наявних виробників. Уникає заміни на постачальника з власними дефіцитними експлуатаційними входами.
- RESOURCE-PANEL: скриншот абатства: ґрунт 8/8 без рамки, всі ресурси зелені попри «Бракує ресурсів». Ґрунт може бути законним матеріалом плану (не видаляти без перевірки конкретного рівня). PanelContentGenerator відображав фізичний склад, canAffordProgression — доступний залишок після резервів. Для ще не розпочатого проєкту обидва ресурсні екрани тепер використовують той самий розрахунок доступності. Для активного будівництва збережено показ складу та інвентаря будівельника. Рамка додана до звичайних іконок GuiText; клікабельні торгові посилання збережено. Деталізація окремо «склад/резерв/доступно» ще не додана. Причину саме цього випадку підтвердити після гри.
- Повна збірка та ігрові перевірки потрібні. Інші відкриті проблеми залишаються в журналі.

### 25.11.2026 — поточні спостереження: абатство, інтерактивна карта, локалізація

- **ECON-SPECIALIZATION (аудит конфігурацій, не баг сам по собі):** `norman/villages/ecclesiastique.txt` має центр abbey, початкові lumbermanhut, farm, cattlefarm, grove, inn, carpenterhouse; CORE church, market, bakery, glassblower, presbytery, quarry; SECONDARY archives, quarry, lumbermanhut, grove, weaverhouse, watchtower; never armoury, tavern, alchemyworkshop, fountain. `grosbourg/hameau_abbatiale.txt` має центр abbey, radius=50, start cattlefarm/lumbermanhut/guesthouse/grove, CORE 2×sheepchickenfarm і cattlefarm, без власної пшеничної ферми в заданій черзі. Не ототожнювати хутір і повне релігійне поселення лише за центром/суфіксом назви. У `PROGRESSION-OPERATING-INPUTS` врахувати спеціалізацію, торгівлю, зовнішні поставки, фактичну наявність виробників; не примушувати всі поселення до автаркії.
- **MAP-DETAILS (розширення MAP-WORKERS / UI-WHAT-DOES, заплановано):** hover на будівлі — короткий огляд; клік — закріплена інтерактивна панель, повторний клік/інша будівля/Esc — перемикання/закриття без перекриття навігації. Показувати зайняті/передбачені робочі місця на поточному рівні. Hover працівника — ім'я, професія, фактична поточна ціль/статус (робота, сон, відпочинок, доставка, недоступний NPC); діагностична причина блокування лише якщо вона відома. Hover іконки продукту — реальний рецепт або спосіб добування (вхідні ресурси, кількість/вихід за наявності в конфігурації, робоча ціль, джерело, необхідна будівля/рівень, умови); якщо кількість залежить від шансів чи таблиць дропу, не вигадувати гарантоване число. Розмежувати можливе виробництво, фактичну зайнятість, продаж та імпорт; дані з наявних визначень Goal/планів/серверного стану, не з жорстко заданого списку для кожної культури. Мінімізувати мережевий трафік: обмежене оновлення вибраної панелі, не запускати AI заново заради UI.
- **L10N-ABBOT-ROLE (спостереження, джерело перевірено):** скриншот Lothar de Falaise, напис над NPC «Abbaï» та квестова мітка «Повернутися до Abbaï (Абат)». У `cultures/norman/villagers/leadersandwives/abbot.txt` `native_name=Abbaï`, фінальний символ U+00EF (латинська ï), у `languages/uk/norman_strings.txt` `villager.abbot=Абат`. Це не суміш кирилиці з латиницею у вихідному слові; `RenderMillVillager` свідомо виводить native occupation, а квестова мітка використовує `QuestInstance.getLabel()/handleString()` і `getGameOccupation()` для role-placeholder. В українському інтерфейсі потрібна природна локалізована назва й відмінювання («Повернутися до абата»), без втрати автентичної назви у відповідних довідкових місцях; точну зміну для всіх мов/ролей ще визначити. Не конфліктувати з паралельною роботою над `languages/uk`.
- Гравець продовжує ігрове тестування патча 55afe2b. Код і переклади цим записом не змінено.

### 25.11.2026 — повторний ігровий тест патча 55afe2b: нові спостереження та невирішені проблеми

**Статус: відкрито; гравець продовжує тестувати цей патч. Цей запис не є виправленням.**

| Ідентифікатор | Спостереження / попередня зміна | Наступна перевірка |
|---|---|---|
| PROGRESSION-OPERATING-INPUTS | У Barneville-l'Église вівчарня побудована раніше за звичайну ферму, хоча ферма має забезпечувати кормом залежні виробництва. Попередній механізм VillageProgression не гарантує порядку за експлуатаційними ресурсами: prerequisite викликається за нестачі будівельних матеріалів, а не за нестачі корму/сировини майбутнього виробництва. | Аналіз усіх культур, граф рецептур і корму, доступність фактичних виробників, запасів та імпорту; врахувати відкладені проєкти, не створювати циклічних блокувань. Конкретну причину вибору в цьому сеансі підтвердити логом. |
| ANIMAL-PEN-SPAWN | Скрін 1: корови розділені непрохідною огорожею, в одному загоні лише одна, в іншому кілька; розведення в ізольованому загоні з однією твариною неможливе. Патч 57a6029 переслідування/забою не гарантував правильного розподілу тварин між загонами; повторні скарги на роботу тваринників залишаються відкритими. | Аудит маркерів появи, реальної зв'язності загонів, вибору пар, корму і доступу NPC для всіх культур, рівнів і варіантів. Не ламати огорожі, не створювати необмежене поголів'я. |
| MAP-WORKERS | Скрін 2: карта показує потенційні виробництва вівчарні, але не показує, чи є працівники. | Показувати фактичну зайнятість (наприклад «Працівники: 0/1» та «Немає працівників»); відокремити потенційне виробництво від поточної активності, не плутати відсутність працівника зі сном, дозвіллям або тимчасовою відсутністю NPC. |
| UI-WHAT-DOES | Запит гравця: дії/завдання жителів логічніше описувати під питанням «Що робить». | Знайти відповідний екран та ключі; зберегти всі дії, узгодити українську термінологію. |
| DOOR-PAIR | Скрін 3: бажаний симетричний двостулковий прохід із петлями по зовнішніх краях; гравець повторно повідомляє про неправильну орієнтацію окремих стулок у збудованих спорудах. Попередні DoorHingeSelection/DoorPlacement та 57a6029 не гарантували потрібного результату в усіх спорудах. Це окремо від відкритої DOOR-ORIENTATION. | Нормалізувати справжні пари суміжних дверей на однаковій висоті, однаковий FACING, протилежні HINGE, узгоджені верхня/нижня половини; перевірити всі повороти плану, порядок встановлення, покращення, обидва боки проходу. Парне відкривання для NPC та гравця з урахуванням ручного керування, без об'єднання незалежних дверей. Вже встановлені двері не переписувати без перевірки плану та змін гравця. |

Правило журналу надалі: для повторного повідомлення про проблему записувати ідентифікатор, дату/сеанс, попередній патч, фактичний результат і статус (відкрито / частково вирішено / підтверджено в грі). Компіляція та статичні тести не є підтвердженням ігрового виправлення. Запит користувача про довідник типів поселень усіх культур також зафіксовано.

Локальний звіт збірки 25.11.2026: 131 Python-тест, 384 перевірки міграції, 7 культур, 0 ERROR, 239 REVIEW, BUILD SUCCESSFUL (38 с). Окремі нові Java-перевірки пошуку ділянок у цьому виводі не запускались. Ігрове тестування триває.

### 25.11.2026 — VILLAGE-SITE: ділянки, черга та попередні резерви

- Виправлено пропуски кутів/крайнього радіуса, один автоматичний поворот і перезапис досяжності останньою клітинкою. Перебір перевіряє всі центри кілець та дозволені повороти, не змінюючи задану орієнтацію; кожна колонка перевіряється один раз.
- VILLAGE-SITE-NONE: підсумковий лог першої причини відмови кожної спроби, кількості центрів/поворотів і часу.
- VILLAGE-SITE-DEFER: окремий cooldown 1600 ігрових тіків за ключем проєкту. Невдалий основний проєкт звільняє чергу та резерв матеріалів. Таймер не зберігається після перезапуску.
- Нові автономні поселення: початкові споруди від найбільших до найменших (за наявності залежностей близькості зберігається авторський порядок); попередні резерви до трьох придатних CORE-проєктів (до п'яти пошуків), спочатку житлові будівлі працівників, потім за площею. Виключено платні/умовні плани та неоднозначні дублікати початкових будівель. Наявний формат location.level=-1, без фізичного будівництва резервів.
- Перед будівництвом CORE-резерву перевірка актуального рельєфу у копії карти, без генерації чанків. Якщо чанк недоступний — відкласти, якщо ділянка стала непридатною або змінилася висота — звільнити резерв та повторити пошук пізніше. Перевірка не гарантує актуальної регіональної зв'язності.
- Радіус, фізичні наявні споруди та межі інших поселень не змінено. Повний планувальник з backtracking, гарантія розміщення всіх CORE і динамічне розширення не реалізовані. Додано BuildingSiteSearchCheck і ProjectSiteCooldownCheck; Forge-збірка, збереження/відновлення резервів та реальна поведінка потребують перевірки.

### 25.11.2026 — українська локалізація Minecraft (uk_ua.json)

- Відредаговано `millenaire-1.18.2-forge/src/main/resources/assets/millenaire/lang/uk_ua.json` на основі `en_us.json`: предмети, блоки, будівельні маркери, обладунки, гербові візерунки, досягнення, повідомлення та інтерфейс діагностики.
- Уніфіковано терміни «поселення», «жителі», «сувій», «пісковик», «саманна цегла», «кайло», «стяг», назви культур і кольори; виправлено машинні спотворення, повтори та граматичні помилки.
- Змінено 676 із 1477 значень; перевірено рівність набору ключів із англійською локалізацією, синтаксис JSON і збереження форматних параметрів та аргументів команд. Ідентифікатори команд і власна назва Millénaire залишені без перекладу.
- `todeploy/millenaire/languages/uk/` не змінено: це окрема паралельна робота. Застарілий `assets/millenaire/lang/uk_ua.lang` не редагувався, оскільки Gradle виключає *.lang з ресурсів мода 1.18.2. Відображення в Minecraft ще не перевірено.

### DOOR-ORIENTATION — гіпотеза про залежність проходження від орієнтації дверей (25.11.2026)

**Статус: ВІДКРИТО — гіпотеза, не підтверджене виправлення.**

Гравець зауважив, що частина дверей стабільно відкривається, а частина — ні; припустив залежність від напрямку встановлення дверей у закритому стані (`FACING`). Конкретні проблемні сторони світу гравець не встановлював: схід/захід проти півночі/півдня були лише умовним прикладом.

Повторний підрахунок записів у попередньому ігровому лозі `latest(20261124-232930).log` (6193 рядки):

| FACING | DOOR-BLOCKED | DOOR-WAYPOINT | DOOR-WAYPOINT з collision=true |
|---|---:|---:|---:|
| EAST | 363 | 371 | 300 |
| WEST | 0 | 17 | 5 |
| NORTH | 0 | 0 | 0 |
| SOUTH | 0 | 1 | 1 |

Усі 363 записи DOOR-BLOCKED мають FACING=EAST. Це підстава для цільового аудиту орієнтації, а не доказ причинності: записи повторюються для тих самих дверей/NPC, кількість спостережень за напрямками суттєво відрізняється, а відсутність записів NORTH не підтверджує справність північних дверей. Лог передує патчу `57a6029`; він не підтверджує його ігровий результат.

**Наступні дії (ще не реалізовані):**

- Аудит симетричності пошуку маршруту A*/JPS, Vanilla fallback, відкривання, утримання/закривання дверей і фізичного проходження при поворотах на 90°, 180° та 270°. Перевірити перестановку X/Z, знаки напрямків, FACING, HINGE, OPEN, HALF, обидві сторони підходу й реальні колізії відкритої стулки.
- Створити автоматизовану матрицю 32 базових сценаріїв: 4 FACING × 2 HINGE × 2 напрямки проходження × 2 початкові стани OPEN. Окремо додати подвійні двері, сходинки та прилеглі блоки. Перевіряти відкривання, фізичний прохід у двох напрямках і закривання після звільнення проходу; телепортація не є успішним проходженням.
- Перевага загальній геометрії `VoxelShape` та рухомого `AABB` над окремими винятками для напрямків світу. Не змінювати робочі хвіртки без доведеної потреби. Не вимагати від гравця вручну перевірити всю матрицю; автоматизовані тести доповнюються цільовою перевіркою у Minecraft.
- Позначку про виправлення змінити лише після достатньої перевірки; не вважати гіпотезу доведеною за цим логом або лише за статичними тестами.

### 25.11.2026 — повторний тест переслідування тварин, дверей і замурованих NPC

- Для пастухів, розведення та загальних завдань забою: перевірка доступної робочої позиції за реальною дистанцією і видимістю, альтернативні Vanilla-позиції після невдалого маршруту, повторна побудова завершеного маршруту, коротке виключення цілі після 160 тікiв без фізичного прогресу. Не телепортуватися до тварини. Робочий таймер скидається при виході з дистанції.
- Виправлено асиметрію пріоритетів: дозвілля не може витіснити доступне робоче завдання лише через порядок перебору. Для забою підрахунок поголів'я та вибір цілі мають однаковий радіус 15, наземна тварина обробляється за реальною дистанцією до вибраної цілі, збережено правила мінімального поголів'я та перевірки запасів. Додано діагностичну причину HERD_AT_RESERVE. Причина конкретного простою Wilhelm Bernhard потребує повторної ігрової перевірки.
- NAV-TRAP-RESCUE: окрема аварійна евакуація з суцільних блоків або підтверджено замкненої комірки, до найближчої перевіреної безпечної позиції у завантажених чанках (радіус до 6). Звичайний невдалий маршрут, двері й натовп не є підтвердженням замурування.
- Двері: контактне відкривання закритої дерев'яної стулки лише за фізичного блокування руху, з чинним спільним lease та пріоритетом гравця; для відкритої стулки — зміщення точки руху в реально вільну частину проходу без обходу фізичних колізій. Широкий сканер дерев'яних дверей не відновлено.
- Карту чанків перевірено за ChunkMapSnapshot і GuiPanelParchment: зелений — завантажений чанк, червоний — збережений, але не завантажений. Червона область сама по собі не є витоком завантаження; змін механізму утримання не внесено.
- Статичні перевірки додано; Forge-збірка та фактична поведінка в Minecraft мають бути перевірені окремо.

### Результат повторного ігрового тестування — 23.11.2026 (двері й драбини)

**Повідомлення гравця після попередніх спроб виправлення (точний HEAD запущеного JAR не підтверджений):**

- **DOOR-02/06/07 — НЕ ВИПРАВЛЕНО.** Частина дверей відкривається нормально, частина залишається непрохідною: жителі стоять перед ними. Додатковий симптом: **двері часто не зачиняються за жителями після проходу**. Тобто попередні патчі не закрили задачу. Окремо потрібне виправлення керування часом відкривання/закривання.
- **GATE — спостережувано працює.** За словами гравця, з хвіртками проблем начебто немає. Це не автоматизований повний тест; не змінювати зайвий раз вже робочу гілку хвірток.
- **LADDER-01/02 — НЕ ВИПРАВЛЕНО, частковий прогрес.** NPC почали намагатися піднятися по драбині, але **успішного підйому немає**. Гравець попросив **тимчасово не вносити нових змін у драбини**, доки не виправимо двері.

**Статус наступних патчів:** будь-який новий код дверей позначається **НЕ ПЕРЕВІРЕНО В MINECRAFT** до наступного повідомлення користувача. Успішна компіляція чи smoke-тести не змінюють позначку «НЕ ВИПРАВЛЕНО» для останнього фактичного ігрового результату.

### STAIR-02 / LADDER-03 — сьомий ігровий лог, дві окремі відмови руху (23.11.2026)

**Джерело:** новий наданий гравцем `latest(7).log` (2213 рядків; інтегрований сервер ~15:15–15:16). Гравець докладно описав необхідність перевіряти **фізичну прохідність кожного ребра маршруту A→B**, а не лише точку B; окремо уточнив, що в той самий тест шахтар **не зміг повноцінно піднятися драбиною**. Це фактичні спостереження, а не результат нового виправлення.

**STAIR-02 — конкретна підтверджена ситуація:**
- `Eftokios Prinkips`, `stair=(27,68,648)`, `facing=south half=bottom shape=straight`, `mobHeight=1.80`.
- О 15:15:42: `feet=(26,68,648)`, `next=(27,69,648)`, `atNextClear=true`, `horizontalCollision=false`.
- О 15:15:45 та 15:15:48: `feet=(26,68,648)`, `next=(28,68,648)`, `atNextClear=true`, але **`horizontalCollision=true` та `velocityX=velocityZ=0`**, `stationaryTicks=60` → `120`. Тест `noCollision` **лише в кінцевій позиції** не перевіряє проміжну траєкторію, поворот або можливість підйому на сходинку під slab.
- Зворотний напрям теж має проблемний сегмент: 15:16:07 і 15:16:32 `feet=(28,68,648)`, `next=(26,68,648)`, `atNextClear=true`, `horizontalCollision=true`, `stationaryTicks=57`. У сесії шахтар **пізніше часом потрапляє на блок `(27,69,648)`**, отже перехід не повністю неможливий у всіх станах; провал відтворюється саме для деяких маршрутів/напрямків.
- Висновок: **endpoint-clear не означає edge-clear**, а `collisionTop=1.00` — максимум форми сходинки, не її висота в кожній точці; зі скриншота видно нависаючі елементи, але точний slab-colllider і його TOP/BOTTOM у логу відсутні. Для правильного рішення треба розраховувати перехід з урахуванням `VoxelShape`, висоти 1.80 та реального step-up. **Не слід просто забороняти будь-яку сходинку**: це повторить проблему DOOR-13 з обходом замість працездатного проходу.

**LADDER-03 — окремий підтверджений невдалий підйом:**
- `Eftokios Prinkips` на колоні драбини `(26,Y,655)` за `goal=bringbackresourceshome` з `target=(26,68,648)`: о 15:15:22 `feet=(26,60,655)`, `LADDER-ASSIST next=(26,61,655) dy=1.00 velocityY=0.20`; 15:15:27 `feet=(26,61,655)`; 15:15:31 `feet=(26,62,655)`.
- Далі повторні `LADDER-ASSIST` при `feet=(26,62,655)`, `next=(26,62,655)`, `dy=-0.29 / -0.32`, `velocityY=-0.12` о 15:15:31 і 15:15:35. Тобто контролер на драбині місцями **активно штовхає вниз**, коли цілочисельний next-node збігається з поточною перекладиною, хоча маршрут потрібен угору до `Y=68`. **Не доведено, що саме це є єдиною причиною зупинки**, проте поведінка невідповідна меті.
- О 15:15:40: **`NAV-TELEPORT stock recovery` із `(26,62,655)` до цілі `(26,68,648)`**, отже фізично шахтар НЕ завершив підйом. У цьому самому файлі майянський шахтар `Ah Tabai Chibirias` на іншій драбині застряг біля `(190,68,677)` і о 15:15:46 також отримав `NAV-TELEPORT stock recovery`.
- Гіпотеза: `PathNavigateSimple.tick()` використовує `dy=next.y - mob.getY()` і при `dy < 0` може генерувати `velocityY=-0.12` навіть у середині підйому, коли `next` — поточний блок і потрібне **просування до наступного rung**, або шлях постійно переплановується на той самий вузол. Для виправлення треба розвести **напрям маршруту по драбині** та **дрібну поправку висоти поточної перекладини**, без довільного руху угору крізь стелю.
- Усього в сесії: `LADDER-ASSIST=10` (5 для Eftokios, 5 для Ah Tabai), `STAIR-WAYPOINT=30` (10 для Eftokios), `NAV-TELEPORT=10` загалом (по одному на зазначених двох шахтарів). Кількість записів семпльована, не точна кількість фізичних спроб.

**Стан:** STAIR-02 🔴 НЕ ВИПРАВЛЕНО; LADDER-03 🔴 НЕ ВИПРАВЛЕНО. У цьому записі **немає нового рухового патча**; критерії наступного виправлення: не просто менше колізій або телепортацій, а **реальний перехід через сходи** та **повний підйом/спуск драбини** без телепортації. Попередні обмеження не змінювати двері, хвіртки та `main` збережено.

### STAIR-01 — блокер компіляції JavaDoc і виправлення (23.11.2026)

**Факт від користувача:** після STAIR-01 запуск `Update-Millenaire.bat` завершився `BUILD FAILED in 13s`, `1 actionable task: 1 executed`, `Gradle build failed (exit code 1). Installation cancelled`. Наданий скриншот містив **лише завершальну помилку wrapper**, не повну первинну помилку Gradle.

**Виявлена конкретна синтаксична помилка у внесеному нами Java-коді:** у `PathNavigateSimple.java`, на початку документації методу `traceNearbyStair`, було `A*/JPS`. Послідовність `*/` **передчасно закінчує JavaDoc**, через що текст, який залишився, стає некоректним кодом Java. Виправлено на `A-star and JPS` (коміт `d874a933fdd1f853ce16dc593edf6dacb46d3491`). Логіку навігації або геометричну діагностику не змінено. Це очевидна помилка синтаксису, але **без повного Gradle-виводу зі скриншота неможливо підтвердити, що вона була єдиною причиною збою**.

**Важливе уточнення процедури оновлення:** актуальний `Update-Millenaire.bat` запускає `Update-Millenaire.ps1`, а той **лише виконує локальну збірку та копіює JAR**; він **НЕ виконує `git pull`**. Потрібно окремо синхронізувати локальну гілку `fix/villager-door-access-and-village-territory` (`git pull --ff-only origin fix/villager-door-access-and-village-territory`) перед новою спробою збірки, інакше користувач компілюватиме стару версію з помилкою. Автоматичне оновлення скрипта поки не змінювали.

**Статус:** виправлення JavaDoc **закомічено, але повну Forge-компіляцію та Minecraft-тест не підтверджено**. Якщо збірка знову впаде, потрібні рядки **вище** `BUILD FAILED`, починаючи з `error:` або `* What went wrong:`. Старий JAR має лишитися в мод-папці, бо скрипт припиняє встановлення на стадії збірки. STAIR-01 функціонально залишається діагностикою, а не виправленням сходів.

### STAIR-01 — уточнення зі скриншота: вхід на сходинку без фізично доступного ВИХОДУ (23.11.2026)

**Новий ігровий звіт і другий скриншот від користувача:** у візантійській будівлі шахтаря `Eftokios Prinkips` намагається зайти на дерев'яну сходинку, після чого **не може спуститися з неї / продовжити рух у потрібному напрямку**, оскільки поряд у вузькому коридорі є напівблоки та низькі нависаючі блоки. Гравець уточнив, що головна проблема **не зводиться до стрибка в стелю**: NPC обирає сходинку як вузол маршруту, не врахувавши можливість наступного фізичного переходу з неї. На скриншоті видно шахтаря майже впритул до дерев'яних сходів і нависаючих блоків; точних F3 координат, стану `SlabBlock.TYPE` і даних воксельної колізії з картинки визначити не можна.

**Підтверджений факт коду:** `AStarWorker.getNextCandidates` викликає `AStarStatic.isViable(world, check,...)` для **кандидатного вузла**, а `AStarWorkerJPS.getGroundNodeHeight` вибирає ціле Y через `isViable`; тут немає явної перевірки **усього переходу A→B** за повною колізійною формою тіла NPC, враховуючи саме поворот/спуск зі сходів поруч із напівблоками. Це потенційна причина появи маршруту з фізично недоступним виходом, **не доведений конкретний колайдер без ігрового логу**.

**Правильна ціль виправлення:** перевіряти в генераторі *ребер* маршруту, що весь рух між сусідніми вузлами (включно з реальною висотою поверхні сходів, `VoxelShape` напівблоків і розміром AABB NPC) фізично здійсненний. Якщо вихід зі сходинки заблоковано, алгоритм має перепланувати шлях до того, як NPC на неї зайде; але не забороняти всі сходи й не вдавати, що обхід завжди доводить баг виправленим. Не використовувати наскрізну телепортацію, `noPhysics` чи збільшення висоти стрибка як виправлення планування.

**Поточний стан:** STAIR-01 🔴 **НЕ ВИПРАВЛЕНО**. У цьому уточненні **код не змінено**, збережено діагностику `STAIR-WAYPOINT` з попереднього патча. Для конкретної репродукції доцільно додати діагностику наступних 2–4 ребер (сходинка → запланована точка за нею) після успішного білду, перш ніж робити геометричний патч.

### STAIR-01 — уточнення конкретного місця: візантійська будівля шахтаря (23.11.2026)

**Новий факт від користувача:** невдалий підйом на сходинку відбувається **у візантійській будівлі шахтаря**; **безпосередньо над місцем, куди NPC намагається стрибнути, встановлено півблок (slab)**. Гравець зазначає, що він перешкоджає **стрибку на повний 1 блок угору**. Це конкретніша репродукція попереднього STAIR-01, а не нова успішна перевірка.

**Гіпотеза, не підтверджена колізійним логом:** цілочисельний вузол планувальника чи руховий контролер вимагає переходу `Y+1` або повного стрибка, хоча геометрія нижньої частини сходинки потенційно допускає підйом на `0,5` блока без такого стрибка. Півблок зверху може перешкоджати повній вертикальній траєкторії. **Але якщо реальний просвіт від поверхні сходинки до нижньої грані slab менший за висоту NPC, фізичний прохід також неможливий**. Орієнтація сходів, верхня/нижня половина slab і точні F3 координати ще невідомі; не стверджувати наперед, що будівля гарантовано прохідна.

**Рішення наразі:** `STAIR-WAYPOINT` уже логуватиме `collisionTop`, `exactY`, `next`, `nextHead`, `atNextClear`, `mobHeight` і колізії NPC біля сходів; не додаємо автоматичне високе підстрибування чи `noPhysics`. Зробити контрольований тест саме в цьому візантійському будинку після успішної компіляції STAIR-01, перевірити чи NPC фізично долає сходинку, а не обходить її. **СТАТУС: НЕ ВИПРАВЛЕНО.**

### STAIR-01 — повідомлення про підйом на сходи та застрягання під низькою стелею (23.11.2026)

**Новий фактичний звіт гравця:** «Жителі інколи не можуть нормально пройти сходи, то не можуть вистрибнути з інших блоків на них (бо вони вже як 2 рахуються), то не можуть обійти їх в кімнатах з низькими потолками». **Спостережено користувачем, НЕ ВИПРАВЛЕНО.** Немає конкретної F3 позиції сходинки або логу із геометрією сходів; не слід удавати, що колайдер і точна причина вже доведені.

**Кодова перевірка без зміни руху:** у власному `AStarStatic.isViable` вузол перевіряється за **цілими координатами блоків** із вимогою прохідності позиції ніг `y` та голови `y+1`; JPS `getGroundNodeHeight` пробує `y`, `y-1`, `y+1`. Такі вузли **не містять дробового рівня поверхні `VoxelShape`** сходинок, орієнтації `FACING` та геометрії фактичного вертикального просвіту для переходу між двома сусідніми блоками. Це **правдоподібний кандидат на причину** надто високого переходу або вибору непрохідної сходинки; не можна довести це без геометрії конкретного епізоду. Важливо: та сама базова перевірка `isViable` була й у `main`, тому новою регресією її називати не можна. Vanilla-планувальник використовує `VillagerWalkNodeEvaluator`; можливо, поведінка залежить від того, який планувальник активний і коли відбувається fallback.

**Що зроблено замість чергового неперевіреного рухового патча:** додано **лише логування** `STAIR-WAYPOINT` у `PathNavigateSimple.tick` за `logpathing=minor`, не частіше 1 запису на 60 тіків на NPC, якщо next-node збігається зі сходинкою / її верхом або NPC **не рухається щонайменше 40 тіків** і сходинка є в суміжних блоках. Лог містить `stair`, `FACING/HALF/SHAPE`, висоту верхньої точки collision shape, `feet/exactY/next/nextHead`, реальну висоту NPC `mobHeight`, `atNextClear` через `Level.noCollision(entity, AABB)`, `horizontalCollision/velocity/stationaryTicks/path`. Ці дані мають дозволити розрізнити неправильний вузол у плані, об'єктивно надто низьку стелю та збій рухового контролера. **Фізика, step-height, JPS/A*, двері, хвіртки, драбини не змінювалися.**

**Перевірки:** статична регресійна перевірка `test_stair_diagnostics_are_observational_and_rate_limited` додана до `tools/tests/test_door_lifecycle_wiring.py`; код треба перевірити компіляцією й реальним тестом. Повного Forge build/ігрової перевірки STAIR-01 **не було**. **Статус: 🔴 ігрова проблема відкрита; діагностика ⚪ НЕ ПЕРЕВІРЕНА.**

**Наступний точковий ігровий тест:** з `logpathing=minor` відтворити один випадок, коли NPC не піднімається на сходинку зі звичайного блока, і один — у вузькій кімнаті з низькою стелею; зберегти `latest.log`, F3 координати сходів і приблизний напрям руху. Перевіряти **фактичне проходження**, а не лише число зіткнень чи вибір іншого маршруту; змінювати висоту кроку/колізію лише після локалізації. `main` не змінювати.

### DOOR-15 — контрольований відкат до простого відкривання за маршрутом після невдачі DOOR-14 (23.11.2026)

**Фактичний відгук гравця:** «Не знаю чому, але ти чомусь не можеш виправити таку казалось би просту проблему, хоча раніше її взагалі майже й не було, і всі +- нормально працювали в цьому плані». Гравець надав шостий діагностичний `latest(6).log`. Це **не є підтвердженням виправлення DOOR-14**, а підстава знайти регресію відносно простішої, колись працездатної версії. **Ігровий статус: 🔴 НЕ ВИПРАВЛЕНО.** Не вважати «мало колізій» або «багато CLOSE» достатньою підставою для успіху.

**Що насправді видно у `latest(6).log` (14:02:33–14:07:59, 5851 рядків):**

- `DOOR-LEASE`: **118 OPENED**, **117 CLOSING**, **27 RENEWED**, **3 PLAYER-OVERRIDE**; це події менеджера, а **не 118 підтверджених проходів**.
- `DOOR-WAYPOINT`: **113**, `horizontalCollision=true` — **4**, `collision=false` — **109**. Ці числа знову не доводять прохід **саме через несправні входи**, оскільки NPC можуть обходити їх.
- `DOOR-BLOCKED=27` (`NO_VISIBLE_DOOR_CANDIDATE=24`, `AT_WAYPOINT=2`, `NO_DESTINATION=1`); `DOOR-SIDE-PASS=1`; `DOOR-SCANNER-HOLD-EXPIRED=2`; `Navigation recovery=147`; `NAV-TELEPORT=1`. Запис `DOOR-BLOCKED` називає найближчі двері, не обов'язково вузол маршруту.
- **Відтворена конфліктна поведінка нової логіки:** на ялинових дверях `(28,68,652)` (facing=east, hinge=right) зафіксовано **13 OPENED і 13 CLOSING**. Усі 13 OPENED ініціював **Eftokios Prinkips (actorId=376)**: **3 ROUTE, 2 SCANNER-OPEN, 8 SCANNER-HOLD**. Наприклад, `CLOSING tick=390585` → `OPENED source=SCANNER-HOLD tick=390587` при `actorFeet=(29,68,652)`; згодом `CLOSING tick=394905` → `OPENED tick=394907` з тієї ж позиції. У цьому ж логу о 14:05:41 з'являється `DOOR-SCANNER-HOLD-EXPIRED door=(28,68,652) ticksSinceOpen=1672` — час сканера давно вийшов за 120 тіків. **Це конкретний доказ, що нові таймери/сканер НЕ керують дверима стабільно; водночас NPC між окремими епізодами рухався по інших маршрутах, тому не слід називати його нерухомим усі 3 хвилини.**
- **Приклад DOOR-14 side-pass:** `Niompe Prinkips` `DOOR-SIDE-PASS (27,68,646) open=false` о 14:04:44, згодом `DOOR-WAYPOINT` при `feet=(27,68,647)`, а о 14:04:45 `DOOR-BLOCKED` із `feet=(27,68,646)`. Це підтверджує, що NPC **дійшов до дверного блока**, але без перевірки фактичного переходу на протилежний бік не доводить коректного користування цим входом. Були також `PLAYER-OVERRIDE` на цих дверях, тому сценарій не є чистим тестом NPC.
- Точний Git SHA JAR у `latest(6).log` відсутній (є лише назва `millenaire-8.1.2-port.0.jar`); нові `DOOR-SIDE-PASS` та `SCANNER-HOLD` підтверджують увімкнену інструментацію з новіших патчів, але не дають криптографічної ідентифікації збірки.

**Порівняння з `main` до нових дверних експериментів:** у `main` `MillVillager.handleDoorsAndFenceGates` просто шукає двері в **найближчих вузлах активного маршруту**, відчиняє їх, а після відходу NPC зачиняє. У `main` відсутні `VillageDoorAccess.java` та `DoorTransitionRules.java`; `VillagerWalkNodeEvaluator` не змінює `getNeighbors`. Поточний feature-branch розрісся сканером за курсом, panel-side евристиками, 120-тіковими таймерами й трьома планувальниками. Неможливо чесно назвати будь-яку з цих технік доведеною причиною **всіх** відмов, але повторне відкривання тим самим сканером без прогресу спостерігається безпосередньо.

**Кодова дія DOOR-15 — ІЗОЛЯЦІЙНИЙ ВІДКАТ, не нове «доведене виправлення»:**

1. Відновлено вихідне розширення сусідів **Vanilla `VillagerWalkNodeEvaluator`**, **classic `AStarWorker`** та **JPS `AStarWorkerJPS`**: прибрано виклики `DoorTransitionRules.allowsEdge`, через які жителі могли обходити двері. Інші правила пошуку маршруту, сходів, скринь і бар'єрів не змінено.
2. У `MillVillager.handleDoorsAndFenceGates` прибрано складну перевірку напрямків дверної панелі, `passedPanel`, `matchedDoorNode` і `DOOR-SIDE-PASS` для **відкривання за маршрутом**. Залишено просту логіку: найближчий дерев'яний дверний блок серед наступних вузлів активного маршруту → `VillageDoorAccess.openForResident(..., ROUTE)`. Саме такий принцип раніше був у `main`; деталі кандидата попереднього вузла та shared lease **не тотожні** старому коду.
3. **Тільки для дерев'яних дверей** відключено експериментальний emergency scanner (`if (BlockItemUtilities.isWoodenDoor(block)) continue`); це відключає джерело підтверджених OPEN/CLOSE циклів `SCANNER-HOLD`. Поточну перевірку/відкривання **хвірток** у scanner залишено. `VillageDoorAccess` як менеджер оренд і автовзакривання **збережено**; це ще **не повний відкат до `main`**.
4. Переписано `tools/tests/test_door_lifecycle_wiring.py` відповідно до нової тестової гіпотези (8 source-wiring перевірок). **9/9 структурних умов коду перевірено через вміст репозиторію**: усі три планувальники не мають дверного фільтра, шлях відкривання спрощено, gate-сканер і драбини лишилися. Повний Forge/Gradle build **не виконувався**, а нова поведінка в Minecraft **не тестувалася**.

**Статус:** старий ігровий тест — 🔴 **НЕ ВИПРАВЛЕНО**; rollback DOOR-15 — ⚪ **НЕ ПЕРЕВІРЕНО В ГРІ**. Всі попередні коміти залишені в Git-історії й можуть бути повернуті окремо. Ізоляційний тест потрібен, щоб дізнатися, чи базовий контролер знову дає NPC проходити через старі проблемні двері, **а не просто знижує кількість колізій**.

**Один чіткий наступний експеримент:** після `BUILD SUCCESSFUL` перевірити без ручного втручання та з `logpathing=minor` фізичне проходження одним NPC через `(28,68,652)` в обох напрямках, спостерігаючи OPENED/CLOSING та фактичний перетин блока. Далі порівняти другий раніше проблемний вхід `(29,66,548)`. Якщо NPC почне бачити двері, але JPS знову пропустить їх як вузол, це окрема гіпотеза; відсутній сканер може погіршити такі маршрути, і його слід відновлювати лише з точним тестом, без повернення автоматичного `SCANNER-HOLD`.

### DOOR-14 — не маскувати несправні двері обходом; урахувати стан панелі та припинити блокувати їх відкривання (23.11.2026)

**Фактичний результат, що призвів до змін:** гравець уточнив після `latest(5).log`: менша кількість колізій спричинена тим, що NPC **ОБХОДЯТЬ двері, які не можуть відчинити**. Це **НЕ є успішним проходом**. За словами гравця **DOOR-13 НЕ ВИПРАВИВ ОСНОВНУ ПРОБЛЕМУ** (🔴 НЕ ВИПРАВЛЕНО). Раніше зафіксовані `1/110` зіткнень та `122/122` цикли відкривання/зачинення зберігаються як вимірювання, а не підтвердження якості проходу.

**Кодова причина, виправлена як гіпотеза для наступного тесту:**

- **DOOR-13 відхиляв кожне бокове ребро дверного блока незалежно від OPEN.** Це прибирало двері з графа пошуку, тому жителі фізично менше впиралися в OPEN панелі, але мусили шукати обхід. **DOOR-14 розрізняє фізичні стани**: уздовж нормалі `FACING` можна планувати прохід з обох боків (двері мають відчинитися за потреби); **боковий перехід** центром блока з фізичною панеллю на краю **дозволено, тільки коли двері ЗАЧИНЕНІ**; якщо OPEN повертає панель поперек цієї смуги, боковий перехід не планується. Діагональні ребра через дверний блок лишаються забороненими. Логіка спільна для Vanilla, A*, JPS через вже вбудований `DoorTransitionRules.allowsEdge`.
- **Маршрутний контролер:** `MillVillager.handleDoorsAndFenceGates` перевіряє реальні прилеглі ребра біля дверного вузла: якщо NPC має прохід **уздовж зачиненої панелі**, не подає безпідставну команду OPEN, яка б повернула панель прямо в коридор. Якщо маршрут перетинає нормаль до закритої панелі — механізм відкривання лишається активним. З `logpathing=minor` додано `DOOR-SIDE-PASS` із `door/open/feet/path`, не частіше разу на 100 тіків для конкретного NPC/дверей.
- **Резервний сканер без маршруту:** `facingDoorPanel` перевіряє реальне розташування NPC відносно **СЕРЕДИНИ фізичної колізійної панелі у ЗАЧИНЕНОМУ стані, а не середини блока**, аби відчиняти панель **з обох її нормальних боків**, включно з NPC, який уже стоїть у самому блоці дверей. Але не відкривати двері при боковому підході, якщо це перекриє маршрут. Хвіртки лишено без цих обмежень.
- **Виправлення помилки DOOR-11 у повторних спробах:** попередній `DOOR-SCANNER-SUPPRESSED` блокував повторне відкривання для NPC, що стояв перед ЗАЧИНЕНИМИ дверима до 120 тіків; лог `latest(5).log` показав справжнє блокування, зокрема `Eftokios Prinkips` `feet=(29,68,652)`, `door=(28,68,652)`, `goal=mineiron2`, `path=none`, `ticksSinceOpen=36/52`. Тепер протягом максимум 120 тіків без фізичного прогресу **утримуємо доступ** (`SCANNER-HOLD` / `SCANNER-RENEW`), замість того щоб залишати двері закритими; після 120 тіків відпускаємо lease (`DOOR-SCANNER-HOLD-EXPIRED`), щоби менеджер сам зачинив двері. Справжнє просування NPC на ≥0,5 блока, зміна мети або інших дверей поновлюють 120-тікове вікно. Відкривання дверей явним вузлом `ROUTE` не обмежується. **Мета:** дати NPC достатньо часу пройти відкритий прохід і не зберігати його відкритим безстроково через один застряглий сканер; сам факт успішного проходу **поки не підтверджено**.
- **Тести:** у `DoorApproachRulesSmoke.java` додано перевірки відкритої/закритої бокової смуги, нормального підходу з двох сторін, **позиції NPC всередині блока біля обох країв дверної панелі**, і обмеженого NPC lease; у `test_door_lifecycle_wiring.py` перевіряється підключення маршрутного правила та роботи scanner hold. Окремий Java-клас із точними п'ятьма pure-методами скомпільовано `javac --release 17` і виконано **30/30 isolated PASS** (OpenJDK 21). Це **НЕ повний Forge build і НЕ Minecraft playtest**.

**Статуси:** останній фактичний звіт користувача про двері — 🔴 **НЕ ВИПРАВЛЕНО** (жителі обходять проблемні двері). Новий патч DOOR-14 — ⚪ **НЕ ПЕРЕВІРЕНО В MINECRAFT**, зокрема немає підтвердженого проходу туди-назад через ті самі входи. **Не називати обхід виправленням**. `main`, драбини й поведінку хвірток не змінено.

**Потрібно від користувача після збірки:** за `logpathing=minor` спостерігати не за кількістю `collision`, а за **успішним перетином конкретного дверного блока** `(29,66,548)`, `(25,68,651)`, `(28,68,652)`, `(31,66,553)` **туди й назад**; чи була команда OPEN, чи NPC пройшов смугу зі ЗАЧИНЕНОЮ панеллю (`DOOR-SIDE-PASS`), чи автоматика зачиняє після виходу. Якщо нове правило не спрацювало, новий `latest.log` повинен містити `DOOR-WAYPOINT`, `DOOR-LEASE`, `DOOR-SIDE-PASS` та `DOOR-SCANNER-HOLD-EXPIRED`. Якщо прямий вхід реальним вузлом ще недоступний, потрібні F3 координати та `FACING/HINGE/OPEN` конкретних дверей.

### Корекція висновку DOOR-13 після уточнення користувача (23.11.2026)

**НОВИЙ ФАКТИЧНИЙ ІГРОВИЙ ЗВОРОТНИЙ ЗВ'ЯЗОК:** гравець прямо уточнив: «Але там прогрес за рахунок того що жителі просто оминають двері які не можуть чомусь відчинити». **Попередню позначку «ЧАСТКОВО ВИПРАВЛЕНО» для основної проблеми відкривання/проходження слід скасувати.** Новий правильний статус за словами користувача: 🔴 **НЕ ВИПРАВЛЕНО** — жителі **обходять** проблемні двері, а не проходять через них після відкривання.

**Що доводить журнал, а чого ні:** падіння `DOOR-WAYPOINT collision=true` з 80/152 до 1/110 підтверджує, що у новій сесії було менше зіткнень з дверними вузлами, **але НЕ підтверджує успішне відкривання або прохід через попередньо несправні двері**. Записані 122 відкривання/122 зачинення стосуються інших активних проходів і хвірток; це не означає, що **всі** двері тепер працюють. У DOOR-13 ввели жорстку заборону бокового ребра на рівні **трьох планувальників**, що закономірно змушує NPC обирати обхід, навіть якщо для цієї конкретної геометрії можливий прохід при іншому стані дверей. Потрібен не новий довільний радіус сканування, а **перевірка фізичної прохідності для обох станів дверей** та способу їхнього відкривання відносно маршруту.

**Пріоритет:** не зливати DOOR-13 у `main` як виправлення дверей. Змінювати планувальник/дверний контролер так, щоб NPC могли використати коректно прохідні двері, а не просто менше стикалися з ними. Журнал попереднього заміру збережено нижче як технічний показник, але його позитивна інтерпретація **спростована геймплеєм**.

### DOOR-13 — результат повторного Minecraft-тесту з `latest(5).log` (23.11.2026, 13:25–13:30)

**Фактичний тест після патча зміни осі переходу крізь дерев'яні двері.** Гравець надав `latest(5).log` (5742 рядки, Minecraft запущено 13:25:38, серверний світ 13:26:39–13:30:26). Назва JAR — `millenaire-8.1.2-port.0.jar`, але **точний Git SHA збірки в логах не записаний**, отже причинність зміни та абсолютно точний склад JAR не доведені окремо від хронології тесту.

**Що виміряно саме в новому логу:**

- `DOOR-WAYPOINT=110`: лише **1** із 110 семплів має `horizontalCollision=true` та одночасно X/Z velocity 0, решта **109 без горизонтальної колізії**. Для порівняння в попередньому `latest(4).log` було **80 / 152** випадків `collision=true` (з них 67 з нульовою горизонтальною швидкістю). Частка `collision=true`: **52,6% → 0,9%**. Різні сесії, NPC, двері та активність, тому **це сильна позитивна ознака, але не контрольований A/B доказ**.
- Серед **110 записів `DOOR-WAYPOINT` немає випадків, де двері із `facing=east/west` мають цільову точку строго по Z від позиції NPC, або `facing=north/south` — строго по X**, як у старому проблемному зразку. Втім, **43** семпли мають відносний діагональний зсув від позиції NPC до *майбутнього* вузла дверей, а **33** — двері не в сусідньому блоці; `feet → next` не обов'язково є реальним ребром поточного маршруту. Тому **не можна стверджувати, що лог перевірив кожне ребро планувальників**; структурний код `DoorTransitionRules` має це гарантувати, потрібен ще конкретний ігровий тест.
- **Єдиний `horizontalCollision=true`**: `Zafrina K'u Ix` о 13:29:16, `feet=(169,73,721)`, `next=(168,73,721)`, `open=true facing=west`, `waypoint=1/6`, `velocity=(0,-0.0784,0)`. Цей перехід іде **вздовж дозволеної осі X**, отже не є старою проблемою неправильної осі Z. До 13:29:20 той самий NPC вже перебував на `(165,73,720)`, тобто **не залишився застряглим на цьому вході назавжди**. Фактичний колайдер не ідентифікований.
- `DOOR-LEASE=272`: **122 OPENED, 122 CLOSING, 27 RENEWED, 1 OCCUPIED-PANEL**. **Для кожної конкретної позиції** кількість OPENED та CLOSING за час сесії збігається. За типом останнього CLOSING: **91 дерев'яні двері (66 spruce / 25 oak)** і **31 хвіртка spruce**. Жодного безперервного незакритого NPC-lease в *журналі* не спостерігається, але це **не гарантія закривання вручну відкритих чи незареєстрованих дверей**.
- `DOOR-SCANNER-SUPPRESSED=4`: захист DOOR-11 від безперервного перевідкривання **справді виконується в Minecraft** (біля `206,72,741`, `2,68,629`, двічі `28,68,652`). Старі двері `31,66,553` у цьому тесті **не відчинялися**, тому ефективність anti-strobe на точно старому вході все ще не перевірена.
- `DOOR-BLOCKED=33`: **27 NO_VISIBLE_DOOR_CANDIDATE**, **4 AT_WAYPOINT**, **2 NO_DESTINATION**. Вони записують *найближчі* закриті двері, які **не обов'язково стоять на запланованому маршруті**, тому це **не 33 фактичні невдалі проходи**. Певні NPC ще не знаходять маршрут або стоять біля дверей, але зв'язок цих записів із непройденими фізично дверима не доведено.
- Навігація загалом залишається проблемною незалежно від цього конкретного покращення: `Navigation recovery=202`, `NAV-TELEPORT=2`, `NAV-CHOICE=2845` (`ASTAR-SEARCH=848`, `ASTAR-DEFERRED=823`, `VANILLA-FALLBACK=585`, `ASTAR-RESULT=585`, `ASTAR-NO-ACCESS=4`). Це окремі ознаки невдалих/відкладених маршрутів, не доказ, що саме двері спричинили їх.

**Статус за результатами:** 🟡 **DOOR-13 — ЧАСТКОВИЙ ПРОГРЕС, зафіксований у грі** (значно менше колізій та коректніші маршрути; відкривання/зачинення активних дверей працювало у цій сесії). **Повне виправлення всіх дверей НЕ ПІДТВЕРДЖЕНО.** Раніше відомі `(29,66,548)`, `(25,68,651)`, `(31,66,553)` не були повторно пройдені в цій сесії з такими самими умовами, і користувач не надав окремого текстового підтвердження, що всі проблемні двері тепер працюють.

**Новий код після цього логу:** не змінено. Не запускати наступний ризикований патч геометрії на підставі лише одного неідентифікованого зіткнення. Наступний контрольований ігровий тест: з `logpathing=minor` перевірити **ці ж три старі входи** (прохід туди й назад, зачинення після виходу), за повторної помилки вказати точні F3-координати та `DOOR-WAYPOINT / DOOR-LEASE`. Драбини, хвіртки і `main` не змінювати.

### DOOR-13 — п'ятий лог: колізія у ВІДКРИТИХ дверях та неправильна вісь маршруту (23.11.2026)

**Джерело:** п'ятий наданий користувачем `latest(4).log` (13:08:08–13:11:02, 3101 рядок), з діагностикою `DOOR-WAYPOINT` після DOOR-12. Точний SHA JAR не записується Minecraft, але лог підтверджує виконання нової інструментації.

**Фактичний результат Minecraft:** проблема проходження через двері **ЗАЛИШИЛАСЯ**. Стан: 🔴 **НЕ ВИПРАВЛЕНО**. З логів не можна робити висновок, що будь-який окремий NPC не зміг пройти абсолютно всі двері.

- `DOOR-WAYPOINT`: **152** записи; `open=true` в **151**, `collision=true` в **80**, із них **67** з нульовою швидкістю X/Z. **Безпосередньо підтверджено, що NPC не просувається з відкритими дверима наступним вузлом і часто має горизонтальну колізію**. `horizontalCollision` не вказує точний блок зіткнення; дверна панель — сильна гіпотеза, але не єдина можлива перешкода.
- `Anatolios Zaridis` неодноразово лишався на `(29,66,547)`, наступний блок — **відкриті двері `(29,66,548)`, facing=west**, але координати не змінювалися; декілька семплів із `collision=true, velocity=(0, ..., 0)`. `Lizas Raoul` також зупинявся на тій же позиції та перед тими ж дверима. У цьому журналі не було записів `DOOR-LEASE` саме для цих дверей, тобто менеджер не реєстрував їхнього відкривання/закривання в поточній сесії: вони могли бути відкриті до старту, вручну, іншим механізмом; **не можна приписувати їхній стан NPC-lease лише за `open=true`**.
- Аналогічно `Eftokios Prinkips` зупинявся на `(25,68,652)` перед відкритими дверима `(25,68,651)`, `facing=east`; `Anatolios` зупинявся на `(27,68,647)` перед `(27,68,646)`, `facing=east`; `Times Diaserenos` на `(67,67,539)` перед `(67,67,540)`, `facing=east`, із повторними `collision=true`.
- Спільна геометрія повторних невдалих випадків: двері мають **`facing=east|west`**, але шлях іде **уздовж Z** (наприклад `29/66/547 → 29/66/548`). Це означає пересування **паралельно площині ЗАЧИНЕНОЇ панелі**, тоді як у відкритому положенні панель розвертається і може перекривати такий перехід. **Гіпотеза: планувальники беззастережно трактували блок дверей як WALKABLE з будь-якого боку, не враховуючи фізичну вісь проходу**. Усі рухи, включно з реальним блокуванням від іншої сутності чи стіни, не підтверджені лише цим логом.
- `DOOR-LEASE`: **95** записів — `OPENED=44`, `CLOSING=43`, `RENEWED=8`. Зафіксовано нормальні цикли `OPENED/CLOSING`: `(187,71,674)` 10/10, `(23,67,610)` 8/8, `(23,67,611)` 4/4. Старі проблемні двері `(31,66,553)` у цьому журналі **не відкривалися**. `DOOR-SCANNER-SUPPRESSED` не з'явився (цей запис семпльований), тож успіх DOOR-11 не підтверджений. `DOOR-BLOCKED=4` (`AT_WAYPOINT=2`, `NO_VISIBLE_DOOR_CANDIDATE=2`). `Navigation recovery=68`, `NAV-TELEPORT=10`. Не ототожнювати кількість записів з фактичною кількістю багів.

**Кандидат на виправлення DOOR-13, внесений після аналізу (ще НЕ тестувався Minecraft):**

- Створено `DoorTransitionRules.java`: **вхід і вихід** із дерев'яних дверей дозволено вздовж **нормалі за `DoorBlock.FACING`**: для `east|west` — лише X, для `north|south` — лише Z. Перехід уздовж повернутої OPEN панелі та діагональний перехід через дерев'яні двері відхиляється. Вертикальний крок сам по собі не змінюємо. **Хвіртки, драбини і звичайні блоки залишилися без цих обмежень**. Це фільтр РЕБРА маршруту, не повна заборона дверного блока.
- Вставлено перевірку в **усі три планувальники**: `VillagerWalkNodeEvaluator.getNeighbors` (vanilla/recovery), `AStarWorker.getNextCandidates` (classic A*), `AStarWorkerJPS.jump` (JPS) для кожного стрибкового кроку. Вхід у двері з обох правильних боків не заборонено; кутові/бокові шляхи будуть переплановані. Це не змінює `VillageDoorAccess`, час закриття, роботу хвірток чи навмисно відкладені драбини.
- До `DoorApproachRulesSmoke.java` додано **11 тестів геометрії** обох осей і напрямків, діагонального та вертикального проходу; у `test_door_lifecycle_wiring.py` додано контроль підключення трьох планувальників. Окремо **відтворено точний метод осьового правила** у незалежному Java-класі, `javac --release 17` і `java` — **11/11 PASS** на OpenJDK 21; перевірено також **8/8 структурних умов підключення** у вихідному коді через GitHub. Це **не компіляція оригінального Forge-проєкту** і не повноцінна ігрова перевірка. **Успішного Forge build і Minecraft playtest DOOR-13 ще немає**.

**Залишковий ризик:** якщо в будівлі двері встановлено боком відносно єдиного можливого коридору, новий планувальник може чесно позначити цей напрям непрохідним і шукати інший вхід; він не здатен пройти крізь фізично повернуту панель. Якщо NPC все ще застрягають навіть за нормальною віссю, потрібні координати й зіткнення з сусідніми блоками та іншими істотами.

**Статус:** остання підтверджена ігрова поведінка 🔴 **НЕ ВИПРАВЛЕНО**; новий код DOOR-13 ⚪ **НЕ ПЕРЕВІРЕНО В MINECRAFT**. Не зливати в `main` до перевірки.

### DOOR-12 — четвертий лог: відчинені двері, але NPC застряг на вузлі дверей (23.11.2026)

**Джерело:** `latest(3).log` від гравця, Minecraft 1.18.2 / Forge 40.3.0 / Java 17, сесія ~12:53–12:56. Це **ігрова перевірка після DOOR-11**, але SHA встановленого JAR не міститься в журналі (назва `8.1.2-port.0` не є SHA). Журнал містить `next=` у `DOOR-APPROACH`, отже новіша діагностика активна.

**Фактичні дані журналу:**

- `DOOR-LEASE`: **123** записи, з них `OPENED=48`, `CLOSING=46`, `RENEWED=23`; решта 6 — `PLAYER-OVERRIDE` / `PLAYER-OVERRIDE-SKIP`. `DOOR-APPROACH=1363`; `DOOR-BLOCKED=10` (`NO_VISIBLE_DOOR_CANDIDATE=6`, `STOP_MOVING=4`). `NAV-CHOICE=1800`, `Navigation recovery=70`, `NAV-TELEPORT=15`. Ці числа не можна механічно порівнювати з попереднім логом через різні NPC, місця й тривалість.
- **Попередні проблемні двері `(31,66,553)` у цьому журналі жодного разу не відкривалися/зачинялися й не з'являються в `DOOR-LEASE`.** `DOOR-SCANNER-SUPPRESSED` у цьому журналі відсутній, але його запис *вибірковий раз на 100 тіків*, тому це не доводить відсутності обмежень. **DOOR-11 НЕ МОЖНА ПОЗНАЧИТИ «ВИПРАВЛЕНО»** за цим тестом: необхідний сценарій не відтворено.
- **Ялинові двері `(29,66,548)`**: гравець вручну взаємодіяв із ними чотири рази (`PLAYER-OVERRIDE` о ~12:55), тому частина станів пояснюється ручним керуванням. Пізніше `Lizas Raoul` (actorId 170) відчинив їх `source=ROUTE` на `tick=383354`. Після цього зафіксовано **16 семпльованих `RENEWED`** на `383400,383500,383600` і **жодного `CLOSING` до кінця файлу**. Причина повторних запитів на lease поки невідома, адже до цього патча `RENEWED` не містить `actorId`/`source`. Цей випадок може бути незачиненням, але і свідомим утриманням проходу при NPC поруч.
- **Найцінніший доказ:** `Lizas Raoul` о 12:56:37 **фізично стояв на `(29,66,547)`**, `stalled=true`, шлях `1/4`, і **наступним вузлом був блок відчинених дверей `(29,66,548)`**, далі `(29,66,549)`, `(30,66,550)`. `Anatolios Zaridis` на `(29,67,547)`, також `stalled=true`, мав `path=1/124` і такий самий наступний вузол дверей `(29,66,548)`. Отже, **двері включено у спланований шлях, але фактичного проходження немає**. Це може бути колізія, фізична висота, вузький коридор або інша причина блокування переміщення; сам лог ще не розрізняє їх.
- Інші двері працювали без циклу відкривання через 1–4 тіки, поміченого раніше: `(23,67,610)` — **10 OPENED/10 CLOSING**, `(23,67,611)` — **8/8**, `(187,71,674)` — **9/9**, причому кілька NPC проходили поруч. Це показує, що механізм зачинення загалом працює, але **не доводить**, що всі проходи прохідні.
- `NO_VISIBLE_DOOR_CANDIDATE` та `STOP_MOVING` **не можна автоматично вважати помилками дверей**: у деяких рядках майбутні вузли йдуть повз найближчий зачинений блок, а `stopMoving=true` — стан роботи NPC.

**Нові зміни, DOOR-12 — поки лише діагностика, поведінку не змінено:**

- `VillageDoorAccess.remember` тепер передає `actorId/actorName/actorFeet/source` також у семпльовані записи `DOOR-LEASE RENEWED` (не лише `OPENED`) і **не втрачає actor/source при відкритому стані**. Так можна визначити, хто безперервно продовжує lease, коли двері вже відчинені. Поведінку хвірток не змінено, їхні відомості лише потрапляють в лог.
- `PathNavigateSimple.tick()` кожні ~40 тіків за `logpathing=minor` пише `DOOR-WAYPOINT` **тільки при наступному вузлі маршруту всередині дерев'яних дверей на відстані не більш як 2,5 блока**: `next`, `open`, `facing`, `feet`, `exactY`, `collision`, `velocity`, `waypoint`. Це дозволить визначити, чи NPC фізично штовхається у блок, чи рух є, але навігація не просуває вузол. Нічого не телепортуємо і не змінюємо фізику. Логіка драбин не змінювалася.
- Додані регресійні перевірки на код діагностики в `test_door_lifecycle_wiring.py`. Повної Forge-компіляції та нового Minecraft-тесту після цих змін **не виконано**.

**Статуси:** DOOR-11 — **у частині його конкретного старого випадку `(31,66,553)` не підтверджено ні виправлення, ні повторного збою**. Проблема **можливого застрягання перед відкритими дверима `(29,66,548)` підтверджена в логах**, але причина фізичного непроходження ще не встановлена. DOOR-12 — ⚪ **НЕ ПЕРЕВІРЕНО В MINECRAFT**. Не позначати будь-яку проблему як повністю виправлену.

### DOOR-11 — другий інформативний лог: знайдено ініціаторів циклу (23.11.2026)

**Фактичний результат перевірки гравця:** надано третій `latest(2).log` (12:35:24–12:38:46), уже після патча DOOR-10 з `source`, `actorId`, `actorFeet`. **Цикл відкриття/закриття НЕ ЗНИК:** 🔴 **НЕ ВИПРАВЛЕНО**. Чи всі інші проблемні двері в усіх поселеннях працюють, цей лог не встановлює.

**Точні дані логу:**

- Усього `DOOR-LEASE`: **185** рядків (`OPENED=87`, `CLOSING=86`, `RENEWED=12`); `DOOR-BLOCKED=6` (`NO_VISIBLE_DOOR_CANDIDATE=5`, `AT_WAYPOINT=1`); `DOOR-APPROACH=1129`. Лог має **1576** подій `NAV-CHOICE`, зокрема `ASTAR-DEFERRED=306`, `VANILLA-FALLBACK=239`; `Navigation recovery=66`, `NAV-TELEPORT=11`. Ці події навігації **не доводять**, що причиною всіх проблем є двері.
- Конкретні ялинові двері `(31,66,553)`, `facing=west, hinge=right`: **42 OPENED / 41 CLOSING**. Із 42 відкривань **41 викликано резервним сканером `SCANNER-OPEN`**, тільки **1** — безпосередньо маршрутом `ROUTE`.
- Справжнього ініціатора встановлено: `Giannis Mourtzouplos` (`actorId=180`) відкрив ці двері **38 разів**, з них **37 повторних SCANNER-OPEN з незмінної позиції `(30,66,554)`**. Його метою була `childobserveconstruction`, `target=(29,66,547)`, а шлях в одному з довгих інтервалів стояв на **`path=3/11`**: NPC справді **не просувався**, але резервний сканер постійно відчиняв двері `(31,66,553)`. Після зміни цілі о ~12:37:57 NPC рушив далі, отже тривале зупинення пов'язано з конкретним маршрутом/локальною колізією, не з постійною нерухомістю сутності.
- Пізніше `Alexandra Prinkips` (`actorId=181`) відчинила ті самі двері ще **4 рази**, у **3** випадках із тієї ж позиції `(30,66,554)`, що свідчить про повторюваний збій у конкретній зоні.
- З 41 випадку `CLOSING` → наступний `OPENED` **38 відкривань сталися вже через 1–4 ігрові тіки**. Приклад: `CLOSING tick=379405`, `OPENED tick=379406`, `source=SCANNER-OPEN actorId=180`. Отже менеджер **закриває** двері, але невдалий резервний сканер відразу знову відкриває їх. Збільшення загального інтервалу автозачинення приховало б симптом, не виправивши зупинку NPC.
- Інші зафіксовані відмови `NO_VISIBLE_DOOR_CANDIDATE` поруч із дверима `(23,67,611)`, `(14,69,648)`, `(2,68,629)`, `(187,71,674)`; діагностика показує **найближчі двері**, але не доводить, що вони належать запланованому маршруту. Для локалізації потрібні вузли маршруту.

**Новий код (DOOR-11 — НЕ ПЕРЕВІРЕНО):**

- У `DoorApproachRules.suppressStationaryScannerReopen` додано обмежений anti-strobe контроль: **лише дерев'яні двері**, які той самий NPC щойно відкрив через `SCANNER-OPEN`, **не відкриваються повторно** протягом **120 ігрових тіків**, якщо NPC перемістився менше ніж на **0,5 блока**, а ціль і задача не змінилися. Справжній `ROUTE` має пріоритет і **не блокується**. Зміна цілі, дверей, переміщення або закінчення 120 тіків дозволяють повторну спробу. Це прибирає **цикл блимання**, але **НЕ гарантує проходження NPC**, якщо навігація знову веде у стіну.
- В `DOOR-APPROACH` при `stalled=true` і в `DOOR-BLOCKED` додано `next=...` з **до п'яти майбутніх вузлів маршруту**, щоб після наступного тесту можна було відрізнити маршрут крізь двері від помилково вибраних дверей збоку. Під час спрацювання обмеження виводиться `DOOR-SCANNER-SUPPRESSED` (обмежено 1/100 тіків на NPC).
- Логіка хвірток і драбин та `main` **не змінювалися**. Додано Java smoke-тести для часу, прогресу, іншої цілі/дверей і захист від регресії в `test_door_lifecycle_wiring.py`.

**Тести нового обмежувача:** скопійований окремий метод `suppressStationaryScannerReopen` скомпільовано `javac --release 17` та виконано **7/7 ізольованих перевірок PASS** (повтор без руху; поріг 120 тіків; переміщення; інша задача/ціль; інші двері; від'ємний час). Це **не запуск у Forge** і **не доказ правильного підключення повного `MillVillager.java`**. Окремі regression-тести додано до репозиторію, але повний Gradle build ще не виконувався.

**Статуси (не змішувати):** попередній фактичний тест — 🔴 **НЕ ВИПРАВЛЕНО**. Патч anti-strobe/діагностики — ⚪ **НЕ ПЕРЕВІРЕНО В MINECRAFT**. Заміна цього статусу можлива тільки після реального звіту користувача. Повний Forge build також **не підтверджено**.

### DOOR-10 — результати першого інформативного логу та джерело повторних відкривань (23.11.2026)

**Джерело:** другий наданий гравцем `latest(1).log`, 12:24:14–12:27:13 (≈2 хв. активної гри після завантаження). Рівень `logpathing=minor` **підтверджено робочим**: `DOOR-APPROACH` 994, `DOOR-LEASE` 135, `DOOR-BLOCKED` 9, `NAV-CHOICE` 1423. Усі числа — кількість **записів**, не кількість несправних дверей або NPC.

**Доказана з логу поведінка дверей:**

- Конкретні **ялинові двері** `(31,66,553)`, `facing=west, hinge=right`: **26 OPENED / 26 CLOSING**, ще **8 RENEWED** за період 12:25:42–12:26:56 (≈75 с). Із 25 послідовних переходів «CLOSING → наступне OPENED» **20 відбулися протягом 1–4 ігрових тіків**. Наприклад, `CLOSING tick=376405` та `OPENED tick=376406`. Висновок: двері реально закриває менеджер, але невідомий мешканець (чи мешканці) **майже одразу знову надсилає запит на відкривання**. Це може виглядати в грі як двері, що постійно залишаються відкритими, хоча їхній стан багато разів змінюється.
- `DOOR-BLOCKED`: **8× NO_VISIBLE_DOOR_CANDIDATE**, **1× AT_WAYPOINT**. Зокрема `Evdoxia Raoul` о 12:25:41 біля `(30,66,555)` мав найближчі зачинені двері `(31,66,553)`, `facing=west`, ціль `(31,66,551)`, маршрут `47/49`, але резервний скан не вибрав двері. Сам по собі `NO_VISIBLE_DOOR_CANDIDATE` **не доводить**, що саме ці двері стоять на активному маршруті: лог наводить лише найближчі двері у радіусі. Для встановлення реальної причини потрібні F3-координати проблемного входу.
- `DOOR-APPROACH`: **991× selected=null**, **3× selected=(31,66,553)**. Це **не** означає 991 помилку: діагностика запускається і для NPC далеко від дверей, а трасування має вибіркове семплювання 1/100 тіків на NPC.
- Навігація: `ASTAR-DEFERRED=422`, `ASTAR-SEARCH=359`, `ASTAR-RESULT=294`, `VANILLA-FALLBACK=344`, `ASTAR-NO-ACCESS=4`; `Navigation recovery=75`, `NAV-TELEPORT=9`. Це ширша проблема навантаженого/невдалого планування, але **немає доказу**, що кожний випадок спричинили двері.
- `DOOR-LEASE PLAYER-OVERRIDE` — **1** запис біля іншого блока `(24,71,646)`, отже цикл `(31,66,553)` **не пояснюється зареєстрованим ручним втручанням гравця**. У тому самому журналі є нормальні цикли `OPENED → CLOSING` для інших дерев'яних дверей і хвірток.
- Лог показує тільки ідентичність дверей, **але не NPC-ініціатора OPENED**. Без цього неможливо надійно визначити, чи хибно повторює відкривання один NPC на старому маршруті, чи до входу підходять різні NPC, чи маршрут веде повз двері.

**Патч діагностики після аналізу (зміни поведінки немає):** у `VillageDoorAccess.openForResident` додано сумісний overload `(Level, BlockPos, LivingEntity, String source)`. При успішному **першому відкриванні** `DOOR-LEASE OPENED` тепер пише `source=ROUTE|SCANNER-RENEW|SCANNER-OPEN|UNKNOWN|LEGACY`, `actorId`, `actorName`, `actorFeet` (якщо actor відомий). Три виклики з `MillVillager` передають явну причину. На зачинення, автоматичний 10-тіковий lease, player override, хвіртки та драбини код не впливає (`fdd1dfa`, `a73e6d9`).

**Висновок/статус:** 🔴 **двері НЕ ВИПРАВЛЕНО**, причина **циклічних повторних відкривань встановлена як наявність нових викликів OPEN після CLOSE, але конкретного NPC/маршруту ще не ідентифіковано**. ⚪ Нова діагностика **НЕ ПЕРЕВІРЕНА** у Forge/Minecraft (лише статичні перевірки). Наступний тест: зібрати новий JAR, знову зловити 2–3 цикли `(31,66,553)`, зіставити `OPENED source=... actorId=... actorFeet=...` з його `DOOR-BLOCKED` і маршрутом. Не змінювати A*/JPS чи інші двері загалом без підтвердження.

### Результат аналізу наданого `latest.log` — 23.11.2026, 12:13–12:16 (Minecraft)

**Джерело:** `latest.log`, наданий гравцем у чаті після невдалого тесту дверей. Середовище: Minecraft 1.18.2 / Forge 40.3.0 / Java 17; у логах підтверджено завантаження `millenaire-8.1.2-port.0.jar`, але **SHA вбудованого коду JAR із логу визначити не можна**.

- **Діагностика дверей відсутня:** `DOOR-BLOCKED` — 0, `DOOR-APPROACH` — 0, `DOOR-LEASE` — 0, `NAV-CHOICE` — 0. Це **не доказ відсутності проблем з дверима**: `MillConfigValues.LogPathing` за замовчуванням 0, а записи `DOOR-*` додаються тільки при `LogPathing >= 2`. Рівень `logpathing` у конфігу має бути **текстовим**: `logpathing=minor` (значення 2), а не `LogPathing=2` (для числа `readLogLevel` повертає 0). Код: `MillConfigParameter.getValueFromString` → `MillLog.readLogLevel`.
- **Маршрути мають інші проблеми:** 37 рядків `Navigation recovery`; 7 `NAV-TELEPORT stock recovery`. Зокрема шахтар `Eftokios Prinkips` (`bringbackresourceshome`) двічі запускав аварійне відновлення о 12:14:55 та 12:15:27. Це показує повторні невдалі маршрути NPC, **але не прив'язує їх до конкретних дверей** без діагностики.
- **Фатальних винятків під час тесту дверей у логах не зафіксовано.** Є рядки `Unknown config on line` для старих параметрів `village_list_key`, `quest_list_key`, `escort_key`, `generate_colour_chart`, `generate_goods_list`; вони не підтверджують причину дефекту дверей.
- **Вказівка щодо наступного тесту:** закрити Minecraft, відкрити `%APPDATA%\\ATLauncher\\instances\\Codefication1182\\mods\\millenaire-custom\\config-custom.txt`, додати або виправити **один** рядок `logpathing=minor`, зберегти, перезапустити. `ClientProxy.getCustomConfigFile()` підтверджує цей шлях, `ClientProxy.getLogFile()` — `mods\\millenaire-custom\\millenaire.log`. Відтворити біля зачинених та незачинених дверей, після виходу NPC з проходу, а потім надіслати новий `logs\\latest.log` та за можливості `mods\\millenaire-custom\\millenaire.log`.
- **Ігровий результат:** за словами користувача, проблеми з відчиненням і зачиненням **залишаються** (🔴 НЕ ВИПРАВЛЕНО). Наданий лог **не встановив конкретну причину дверей**. Не зливати патч у `main` до успішного підтвердженого тесту. Цей коміт **не змінює код навігації чи дверей**, лише документує спостереження і коректний спосіб діагностики.

### DOOR-09 — повторний невдалий тест дверей; вузька панель та діагностика (23.11.2026)

**Новий фактичний результат від гравця після попередніх змін DOOR-08:** «Часто двері за собою не закриваються, і всерівно деякі двері вони не можуть відкрити». Отже, **DOOR-08 НЕ ВИПРАВИВ ОБИДВІ ПРОБЛЕМИ**; для наявних дверей стан — 🔴 **НЕ ВИПРАВЛЕНО**. Конкретний SHA запущеного клієнтського JAR у цьому повідомленні не вказано. Хвіртки й драбини на цьому етапі не змінювалися.

**Нові кодові зміни, ще не перевірені у Minecraft:**

- **Автоматичне зачинення дерев'яних дверей:** у `VillageDoorAccess.onWorldTick` попередній `AABB(pos).inflate(0.12, 0.75, 0.12)` охоплював **увесь блок**, тому NPC, що зупинився у сусідній кімнаті, міг вважатися таким, що перебуває в дверному проході, і блокувати зачинення. Тепер для **дерев'яних дверей** зона перевірки тіл обчислюється з колізійної площини стану `OPEN=false`, зі скромним припуском 0.10 блока по X/Z та 0.85 за Y; якщо нестандартний блок не має закритої колізійної форми, збережено старий безпечний fallback. **Хвіртки** зберегли старий механізм. Патч: `ee149f0`.
- **Відкривання поруч із дверною рамою:** emergency-перевірка спершу робить raycast до найближчої точки колізійної панелі; якщо його блокує інший блок, **лише для дерев'яних дверей** додатково перевіряє центр фізичної панелі. Це має уникнути помилкових відмов через промінь, який ковзав по краю стіни. Справжня стіна блокує обидва промені; радіус і правило «лише одні двері» не змінені. Патч: `b4eabc7`.
- **Діагностика причин невідкривання:** `MillVillager` за `LogPathing >= 2` (не частіше одного разу на 100 тіків для NPC) записує `DOOR-BLOCKED reason=STOP_MOVING|COMBAT|NO_DESTINATION|NO_DIRECTION|AT_WAYPOINT|NO_VISIBLE_DOOR_CANDIDATE` із координатами й точними станами найближчих зачинених дерев'яних дверей (`facing, hinge, powered`), ціллю, маршрутом. Це дозволить перевірити гіпотезу про завершення цілі до проходження дверей, а не змінювати всі `Goal` навмання. Патч: `c7b6c60`.
- **Обмеження:** структура `Goal.isWithinActionRange`, драбини та хвіртки поки не змінені. Ці три патчі **не скомпільовані повним Forge build і не перевірені в Minecraft**. Подальший крок: встановити новий JAR, відтворити **ті самі двері** з обох напрямків і випадок незачинення, зібрати `DOOR-BLOCKED` і `DOOR-LEASE` із журналу.

**Результат патча DOOR-09:** ⚪ **НЕ ПЕРЕВІРЕНО В ГРІ**. Не змінювати на «виправлено» без підтвердження гравця.

### DOOR-08 — орієнтація дверної панелі та зачинення після проходу (23.11.2026)

**Спостереження гравця перед патчем:** двері в одному положенні NPC відчиняють, а якщо вони встановлені **біля протилежного краю того самого блока** — стоять перед ними. Також частина дверей лишається відчиненою після проходу. Хвіртки, за словами гравця, проблем не мають. **Результат до патча: НЕ ВИПРАВЛЕНО.**

**Зміни в коді, очікують ігрової перевірки:**

- `DoorApproachRules.closestPanelCoordinate`: під час сканування дверей використовуємо найближчу точку **фактичної колізійної площини закритої дерев'яної двері**, а не центр блока. У `MillVillager.handleDoorsAndFenceGates` беремо `doorState.setValue(DoorBlock.OPEN,false).getCollisionShape(...).bounds()` — це одна й та сама площина до і після відкривання, незалежно від початкового `FACING` і краю блока. До цієї точки обчислюються напрям, дистанція та raycast (`d2b50c1`, `dc55316`).
- `DoorApproachRules.passedPanel`: коли фізична площина дверей залишилася позаду вздовж напряму поточного маршруту, більше не продовжуємо NPC-lease з наступного вузла маршруту, який ще називає **блок** дверей. Уже відкриті двері у резервному сканері продовжуються лише за вузьким **напрямком маршруту**, а не через поворот погляду нерухомого NPC. Менеджер все ще **не зачиняє** двері, якщо в проході фізично стоїть жива істота; ручне керування гравця та redstone не змінено (`dea1491`, `042db8e`, `dc55316`).
- `VillageDoorAccess` за `LogPathing >= 2` тепер повідомляє `DOOR-LEASE OPENED|RENEWED|CLOSING|OCCUPIED|PLAYER-OVERRIDE` для перевірки причин довгого відкривання; події продовження/зайнятості обмежені за частотою (`0076487`).
- Тест `DoorApproachRulesSmoke.java` розширено на різні краї блока, обидва напрямки підходу X/Z та припинення lease після перетину площини (`40ec1e4`). **25 перевірок відповідної Java 17 геометрії пройшли** на OpenJDK 21 після відтворення джерела окремого `DoorApproachRules.java` і сценаріїв в ізольованому середовищі. **Forge-компіляція та гра не перевірені**; це не доказ коректного `getCollisionShape` у світі.

**Фактичний результат після цього патча:** ⚪ **НЕ ПЕРЕВІРЕНО** — гравець ще не тестував новий JAR. Поточний результат останнього повідомлення: 🔴 **двері не виправлені**, 🟡 **драбина лише частковий прогрес**.

### Журнал результатів тестування (зворотний зв'язок гравця)

**Правило ведення CHANGELOG:** коміт або пройдений unit/smoke test **не означає**, що проблема виправлена в Minecraft. Для кожної проблеми фіксуємо окремо **що змінено**, **що фактично спостерігав гравець після встановлення збірки**, **до якого коміту/етапу це стосується**, **поточний стан** і **що потрібно перевірити далі**. Коли користувач повідомляє в цьому чаті, що проблема залишилася, оновлюємо саме результат — **не позначаємо виправлення як успішне**. Попередні невдалі спроби зберігаємо для історії.

**Позначення:** 🔴 **НЕ ВИПРАВЛЕНО** — гравець повторно підтвердив баг; 🟡 **ЧАСТКОВО** — є помітне покращення, але бажаний результат не досягнуто; ⚪ **НЕ ПЕРЕВІРЕНО** — новий патч внесено, але після нього немає повідомлення про ігрову перевірку; 🟢 **ВИПРАВЛЕНО** — лише після явного підтвердження успішного повторного тесту в Minecraft. Усі дати нижче — дата повідомлень, а не гарантія точного часу складання JAR.

| Дата / тестований етап | Проблема | Фактичний результат, повідомлений гравцем | Статус результату |
| --- | --- | --- | --- |
| 23.11.2026, попередні патчі дверей і драбин, до переходу на A*/JPS-first | DOOR-04/05 | NPC і далі стоять перед частиною дверей; система не усунула баг. | 🔴 **НЕ ВИПРАВЛЕНО** |
| 23.11.2026, той самий етап | LADDER-01 | Жителі **не використовували драбину** як очікуваний короткий шлях кар'єром. | 🔴 **НЕ ВИПРАВЛЕНО** |
| 23.11.2026, інсталяція збірки `e2f7db4` | Підтвердження середовища | На скріншотах гравця: правильна тестова гілка, `BUILD SUCCESSFUL` та `SUCCESS! Millenaire installed.` Це підтверджує лише успішне збирання/встановлення **тієї** збірки, а не виправлення дверей або драбини. | ✅ **ЗБІРКУ ВСТАНОВЛЕНО**, баги залишалися |
| 23.11.2026, після переходу на A*/JPS-first і наступних змін; точний HEAD запущеної гри не зафіксовано | LADDER-01/02 | NPC **вже намагаються піднятися по драбині**, але **підйом не виходить**. Це прогрес у виборі шляху, не успішний підйом. | 🟡 **ЧАСТКОВО**; кінцева задача **НЕ ВИПРАВЛЕНА** |
| 23.11.2026, після кількох змін дверей; точний HEAD запущеної гри не зафіксовано | DOOR-02/06 | **Деякі двері** жителі відкривають нормально, **біля інших стоять і дивляться**, не проходять. | 🟡 **ЧАСТКОВО**; проблема **ЗАЛИШИЛАСЯ** |
| 23.11.2026, останній патч `DOOR-07` (`181952d`) | DOOR-07 | Код змінено: перевірка реального стояння, ширший сектор після застрягання, утримання lease вже відкритих дверей. **Повторного повідомлення про перевірку саме цього патча в грі ще немає.** Попередній невдалий результат не скасовуємо. | ⚪ **НЕ ПЕРЕВІРЕНО**; загальна проблема дверей **ВІДКРИТА** |
| 23.11.2026, останній патч підходу до драбини (`fd58f0b`) | LADDER-02 | Код змінено, щоб NPC наближався до поверхні драбини. **Немає підтвердження, що після саме цього патча підйом/спуск у Minecraft став успішним.** | ⚪ **НЕ ПЕРЕВІРЕНО**; загальна проблема драбин **ВІДКРИТА** |

| 23.11.2026, повідомлення після змін DOOR-08; точний SHA ігрової збірки не підтверджено | DOOR-08 / DOOR-09 | **Частина дверей і далі не відкривається; двері часто не зачиняються після проходу.** Новий патч DOOR-09 ще не встановлено й не перевірено. | 🔴 **НЕ ВИПРАВЛЕНО** для ігрового результату; ⚪ **DOOR-09 НЕ ПЕРЕВІРЕНО** |\n\n**Поточний фактично підтверджений стан:** двері працюють **не всі**; по драбині NPC **намагаються** піднятися, проте **не можуть**. Ніяких повідомлень про успішне повне вирішення цих двох проблем від гравця наразі немає. Останні коміти позначені лише як **кандидати на виправлення**, а не як доведені рішення.

**Для кожної наступної ітерації:** якщо гравець повідомляє результат у цьому чаті, додати новий датований рядок із результатом («працює / частково / не працює»), прив'язати до коміту, якщо він відомий, та оновити актуальний підсумок без видалення попередніх невдач. Якщо версія JAR невідома — прямо написати «не підтверджена», а не приписувати тест останньому коміту.

### Оновлення від 2026-11-23 — окремі двері залишаються непрохідними (DOOR-07)

- **Виправлено в коді (очікує Forge-компіляції та Minecraft-тесту):** резервний пошук дверей відтепер **не покладається на миттєву швидкість**: `MillVillager` відстежує реальне переміщення NPC у горизонтальній площині. Якщо він стоїть на місці близько 20 тіків, дозволено обрати один видимий близький прохід у дещо ширшому фронтальному секторі навіть при зміщеному вузлі JPS. Відкривання дверей лише за напрямом погляду як загальне правило **не застосовується**: погляд враховується тільки після фізичного застрягання як другорядна підказка. Під час нормального руху залишено вузький сектор, відсікання стіни й інших поверхів (`d8e5f1a`, `790c889`).
- **Виправлено в коді (очікує Minecraft-тесту):** при наближенні до відкритих дверей, які відсутні серед вузлів JPS, резервний скан **оновлює lease** через `VillageDoorAccess.openForResident` замість пропуску всіх відкритих дверей. Це запобігає передчасному автоматичному зачиненню після 10 тіків без продовження lease, коли NPC ще підходить; відкривання гравцем і redstone не змінюються (`790c889`).
- **Тести:** `DoorApproachRulesSmoke.java` доповнено сценаріями зміщених дверей, застрягання, дверей позаду/на іншому поверсі та нормального руху; окремі правила скомпільовані з `javac --release 17` і пройшли виконання на OpenJDK 21. Це **не підтверджує компіляцію Forge чи коректність дверей в грі** (`1c071bd`).
- **Документовано:** `DOOR-07` у [`PLAYTEST_ISSUES_2026-11-23_UA.md`](millenaire-1.18.2-forge/docs/PLAYTEST_ISSUES_2026-11-23_UA.md), із ризиками та тестами різних конструкцій дверей (`2eea3b3`).
- **Статус:** усе в тестовій гілці; версія порту `8.1.2-port.0` збережена, `main` не змінено.

### Оновлення від 2026-11-23 — дверні колізії та фізичний підйом

- **Виправлено (очікує Forge-компіляції й Minecraft-тесту):** у резервному відкриванні дверей `MillVillager` промінь до центра блока, який повернув `MISS`, більше не відхиляє дерев'яні двері: тонка дверна панель може не перетинати цей промінь. Якщо raycast потрапив у **інший блок**, двері як і раніше не відкриваються через стіну. Оновлено спільний `DoorApproachRules.clearSight()` та тести (`9a8a816`, `703210c`, `9e171a8`, `9a76b75`).
- **Виправлено (очікує Forge-компіляції й Minecraft-тесту):** `PathNavigateSimple.tick` тепер підводить NPC до **орієнтованої площини ванільної драбини** перед вертикальним прискоренням: обмежене горизонтальне наближення (`±0.085` блока/тік) та зміщення 0.12 блока до опорної стіни. Підйом як і раніше вимагає контакту з драбиною й правильної вертикальної цілі, не телепортує NPC (`fd58f0b`, `a3230d8`).
- **Розширено тести:** `LadderTransitionRulesSmoke` перевіряє дистанцію допоміжного підходу та обмеження горизонтальної швидкості; `DoorApproachRulesSmoke` перевіряє MISS від тонкої панелі й відсічення суцільної стіни. Тести додані в репозиторій, але **нові версії smoke-тестів і повна Forge-компіляція ще не виконані** (`00e53fa`, `9a76b75`).
- **Додано діагностику (лише `LogPathing >= 2`):** обмежені за частотою повідомлення `LADDER-ASSIST` про контакт із драбиною, поточну висоту й рух, та `DOOR-APPROACH` про обрану дверну точку. Дозволяють визначати, чи проблема в побудові маршруту чи у фактичному проходженні (`1074c3f`, `4a7edbf`).
- **Документовано:** нові регресії `DOOR-06` і `LADDER-02`, обмеження та перевірки [`PLAYTEST_ISSUES_2026-11-23_UA.md`](millenaire-1.18.2-forge/docs/PLAYTEST_ISSUES_2026-11-23_UA.md) (`9c4ca16`).
- **Обмеження:** не підтверджено коректність виходу на верхню площадку/хвіртку чи проходження дверей у конкретному будинку. `main` залишився незмінним, версія `8.1.2-port.0` не змінювалася.

### Оновлення від 2026-11-23 — A*/JPS як основний маршрут

- **Змінено (очікує повторної Forge-компіляції й ігрового тесту):** `MillVillager.computeNewPath` **спочатку запускає Millénaire A*/JPS**, а не повертається відразу після успіху `tryGroundRoute`. Vanilla `GroundPathNavigation` тепер є **резервним** при невдачі A*, застарілому знімку світу, відкладеному/відхиленому фоні чи фізичному застряганні (`8628048`, `107f6d0`).
- **Змінено (підготовлено):** watchdog після 4 секунд без реального прогресу перераховує основний маршрут; після 8 секунд спробує обмежений Vanilla-detour; попередній аварійний recovery/безпечна телепортація приблизно через 12 секунд не змінюється. На успішному fallback не перериваємо справний маршрут (`8628048`, `107f6d0`).
- **Виправлено (підготовлено):** `AStarPathPlannerJPS` перевіряє ванільні драбини не лише біля старту/цілі, а й у **обмеженому коридорі між ними** (до 25 поперечних зон 9×9, до 12 пробних висот кожна). Якщо додатковий класичний пошук через драбину відхилено через зайнятий пул, вже знайдений JPS-маршрут зберігається, не перетворюється на невдачу (`4a1aeb8`, `9189ba2`).
- **Додано:** необов'язкову діагностику вибору маршруту `NAV-CHOICE=ASTAR-SEARCH|ASTAR-RESULT|ASTAR-DEFERRED|ASTAR-NO-ACCESS|VANILLA-FALLBACK` при `LogPathing >= 2` з обмеженням частоти повідомлень для NPC (`f5fc0bd`).
- **Додано перевірки:** `tools/tests/test_navigation_priority.py` перевіряє порядок викликів і захист від повернення раннього Vanilla-пошуку; 9 аналогічних статичних перевірок по актуальному коду пройшли, **сам Python-скрипт і повну Forge-компіляцію для цього HEAD ще не запускали** (`07fac77`). Документація й тест-план: [`NAVIGATION_ASTAR_PRIMARY_2026-11-23_UA.md`](millenaire-1.18.2-forge/docs/NAVIGATION_ASTAR_PRIMARY_2026-11-23_UA.md) (`8facf95`).
- **Обмеження:** це зміна *пріоритету пошуку*, не повна заміна Minecraft-рухового контролера; використання драбини й коректність дверей **потребують реального тесту**. Зростання числа основних A*/JPS пошуків може вплинути на TPS; збережені ліміти на знімки чанків, worker і fallback. Версія залишається `8.1.2-port.0`.

### Оновлення від 2026-11-23 — резервні двері та вертикальні драбини

- **Виправлено (підготовлено, очікує Forge-перевірки):** `MillVillager.handleDoorsAndFenceGates` тепер має обмежене відновлення для зачиненого проходу, який не потрапив до маршруту: раз на 4 тіки під час зупинки вибирає **одні** найближчі двері чи хвіртку попереду за напрямком наступного вузла/цілі, перевіряє бокове відхилення, відстань, пряму видимість без стіни й викликає спільний `VillageDoorAccess`, що поважає ручний пріоритет гравця. Більше не відкриває одразу весь сектор дверей 5×5 (`9d7062b`).
- **Виправлено (підготовлено, очікує Forge-перевірки):** класичний `AStarWorker` тепер **реально використовує** вертикальні ladder-переходи між суцільними секціями драбини, лише за вільного простору для ніг/голови і з правильною обробкою вже відвіданих вузлів. До патча `checkPossibleLadder` був мертвим кодом із порожніми перевірками (`f81e389`, `96d470c`).
- **Додано (підготовлено, очікує Forge-перевірки):** `AStarPathPlannerJPS` при суттєвій різниці висот і сусідній драбині перевіряє додатковий класичний A* маршрут та порівнює розраховану вартість двох маршрутів. Пошук обмежений часовим бюджетом і локальним скануванням `PathWorldSnapshot` без завантаження чанків (`855f763`).
- **Додано (підготовлено, очікує Forge-перевірки):** `PathNavigateSimple.tick` здійснює керований фізичний підйом/спуск уздовж послідовних вузлів ванільної драбини, коли NPC дійсно стоїть на її колонці; без прямого встановлення координат/телепортації (`0a00e88`, `dd693ae`).
- **Додано тест:** спільні `LadderTransitionRules` та `LadderTransitionRulesSmoke.java`, ізольований тест на Java 17 **пройшов** для вільної/заблокованої драбини, вертикального руху, заборони польоту за межами колонки. Це **не** повна компіляція Forge чи тест у Minecraft (`f291934`, `87c610b`).
- **Документовано:** тести DOOR-05 і LADDER-01 для дверей та драбини кар'єру в `millenaire-1.18.2-forge/docs/PLAYTEST_ISSUES_2026-11-23_UA.md` (`3dd26fb`).
- **Змінено (безпосередньо протестовано на Java 17, інтеграція Forge очікується):** резервний вибір дверей використовує спільний `DoorApproachRules.score`, який відхиляє проходи позаду, збоку, на іншому рівні та надто далеко. Додано `DoorApproachRulesSmoke.java`, standalone тест пройшов; `ClipContext`, server tick та взаємодія з гравцем ще потребують ігрової перевірки (`1555332`, `d5e029a`, `15b7f4c`).
- **Змінено (очікує Forge-перевірки):** нові вертикальні A* переходи обмежено **ванільною драбинною секцією `Blocks.LADDER`**, щоби не планувати підйоми ліанами без відповідного рухового контролера (`aee4292`).
- **Документовано:** розширено сценарії тестування DOOR-05 і LADDER-01, статус ізольованих тестів та ризики інших `CLIMBABLE`-блоків (`841c9be`).

- **Обмеження:** вертикальний шлях наразі підтримує ванільні драбини, а не будь-які climbable-блоки; стандартний наземний шлях може залишитися кращим, якщо драбина поза зоною порівняння; потрібно перевірити спуск/підйом та вихід до хвіртки в реальній грі.

### Оновлення від 2026-11-23 — коректний стан дверей і ручне керування

- **Виправлено (очікує Forge-компіляції й ігрового тесту):** у `PathNavigateSimple.createPathFinder` тепер налаштовано **обидва прапорці** `VillagerWalkNodeEvaluator`: `setCanOpenDoors(true)` та `setCanPassDoors(true)`. Це дозволяє pathfinder планувати шлях через зачинені дерев'яні двері, а не розраховувати лише на випадковий резервний скан поруч із NPC (`f2f41145`).
- **Виправлено (очікує Forge-компіляції й ігрового тесту):** серверний `VillageDoorAccess` реагує на правий клік гравця по дерев'яних дверях/хвіртках, скидає попередню auto-close lease NPC і тимчасово надає гравцеві пріоритет на **100 тіків (5 секунд)**. Коли гравець відчиняє двері, NPC не закриває їх через стару lease; коли гравець закриває їх, NPC не відкриває відразу. Облік ведеться за нижньою половиною двоблокових дверей, час життя даних обмежено. Сигнал redstone та інші сторонні моди не переназначалися (`0a73f7fc`, `eceefa3e`).
- **Додано перевірки:** сценарій `DOOR-04` у [`PLAYTEST_ISSUES_2026-11-23_UA.md`](millenaire-1.18.2-forge/docs/PLAYTEST_ISSUES_2026-11-23_UA.md) — обидва боки дверей, upper/lower, відкриття та закриття гравцем, часове блокування, тварини, redstone, кілька NPC (`5e84e4c`).
- **Статус:** зміни збережено в тестовій гілці, але **успішної Forge-компіляції та тесту Minecraft ще немає**.

### Оновлення від 2026-11-23 — двері та будівництво у воді

- **Виправлено (потребує тесту в Minecraft):** у `MillVillager.handleDoorsAndFenceGates` попередній вузол дверей/хвіртки підтримується відкритим, лише доки **bounding box NPC фізично перетинає його область**. Не повертаємо безумовну перевірку попереднього вузла, яка колись спричиняла повторне відкривання дверей позаду жителів. Час утримання дверей і хвірток після виходу не збільшено (`749488e`).
- **Змінено (потребує тесту в Minecraft):** `GoalConstructionStepByStep` тепер призупиняє непотрібний рух, поки будівельник уже в межах доступу до блока, зокрема під час роботи у воді. `MillVillager.checkGoals` підтримує це зупинення на кожному тіку поточної дії, попри скидання `stopMoving` на початку `tick`. Рух до далеких наступних блоків відновлюється звичайно; **час роботи за блок і водна фізика не прискорені** (`132801f`, `0fecffe`).
- **Додано план перевірок:** `DOOR-03` та `BUILD-WATER-01` у [`PLAYTEST_ISSUES_2026-11-23_UA.md`](millenaire-1.18.2-forge/docs/PLAYTEST_ISSUES_2026-11-23_UA.md) (`7e97782`).
- **Статус:** внесені кодові виправлення не є підтвердженням розв'язання проблем у грі; повна Forge-компіляція та ігровий тест актуальної гілки поки не підтверджені.

### Оновлення від 2026-11-23 — системне відновлення руху і акацієві гаї

- **Виправлено (підготовлено до гри):** `MovementWatchdog` більше не трактує стрибки по Y на місці як прогрес; звичайний рух оцінює по X/Z, а справжнє лазіння по climbable-блоках допускає вертикальний прогрес. Застосовано послідовність `REPATH` після 80 тіків, `DETOUR` після 160 та `RECOVER` після 240 (`b682df43`).
- **Змінено (підготовлено до гри):** `DetourPathFinder`, `PathNavigateSimple`, `MillVillager` підтримують разову обмежену розширену A* спробу при тривалому застряганні, збережено світовий CPU-ліміт та наявні правила безпечної телепортації/заборони телепортації (`b38a7b41`, `45b70ce1`, `d267dac3`).
- **Виправлено (підготовлено до гри):** `BuildingResManager` перевіряє сусідні існуючі паростки й стовбури акації на позначених точках посадки; зменшена ймовірність конфлікту дерев у старих гаях (`5f08c370`).
- **Змінено (плани нових поселень):** у `acaciagrove_A0.png` великого візантійського гаю **8 → 4** точки посадки, у `acaciagrovesmall_A0.png` малого — **4 → 2**; маркери `acaciaspawn` переміщено разом із ґрунтом під ними. Вже збудовані гаї автоматично не перебудовуються. Потрібні тести росту і балансу деревини (`abf2f43e`).
- **Додано тест:** `tools/tests/MovementWatchdogSmoke.java` — самостійний Java 17 smoke test із сценаріями стрибків на місці, звичайного переміщення, лазіння та зміни цілі; ізольований тест компілювався й пройшов, **повна Forge-компіляція та тест у Minecraft не виконані** (`f90dc9bd`).
- **Документовано:** критерії та ризики тестування NAV-STUCK-01 і TREE-02 у `millenaire-1.18.2-forge/docs/`. Проблема вибіркового відчинення дверей DOOR-02 лишається відкритою.

### Оновлення від 2026-11-23 — повторне тестування

- **Виправлено (підготовлено, без ігрової перевірки):** будівельники під час отримання ресурсів із Town Hall тепер перевіряють фізичну доступність складської точки, а не лише відстань; отримання ресурсів не виконується через стіну чи з ями. Змінено `GoalGetResourcesForBuild` (`445c0b4e`).
- **Виправлено (підготовлено, без ігрової перевірки):** отримання будівельних ресурсів включено до логістичних завдань із перевіркою фізично досяжних точок у `MillVillager` (`17e436d0`).
- **Документовано:** нові ігрові регресії BLD-01, DOOR-02 та TREE-02 з доказами, обмеженнями й тест-кейсами (`c80d8fbd`, [`PLAYTEST_ISSUES_2026-11-23_UA.md`](millenaire-1.18.2-forge/docs/PLAYTEST_ISSUES_2026-11-23_UA.md)).
- **Поки не підтверджено в грі:** вибіркова відмова шахтаря проходити окремі двері залишається відкритою; акацієві гаї мають нову менш щільну схему та runtime-запобіжник, але старі споруди/паростки автоматично не пересаджуються, потрібен повторний тест росту.

### Додано

- Спільний серверний менеджер `VillageDoorAccess` для тимчасового відкривання дверей і хвірток кількома NPC та контрольованого закривання після звільнення проходу.
- `WorkAccess` для перевірки фізичної доступності складських позицій перед отриманням чи передачею товарів: не лише відстань, але й досяжність/відсутність стіни між NPC і точкою взаємодії.
- `BarrierPathRules` і `DetourPathFinder` для правил проходу через бар'єри та обмеженого порівняння альтернативних маршрутів за їхньою фактичною вартістю.
- Скрипти `Update-Millenaire.bat` та `Update-Millenaire.ps1` для локальної збірки й встановлення JAR; поліпшено запуск дочірнього PowerShell та повідомлення про помилки.
- Технічні документи в `millenaire-1.18.2-forge/docs/` із відтворенням багів, діагностикою маршрутів, тестами пастухів/дверей і росту акацій.

### Змінено

- Навігацію NPC: замість примусу йти конкретною хвірткою використовується звичайний доступний шлях та обмежене порівняння вартості альтернатив; **попередні експерименти з forced-gate waypoint були скасовані**.
- Логістику доставки/отримання ресурсів у `GoalBringBackResourcesHome`, `GoalDeliverGoodsHousehold`, `GoalDeliverResourcesShop`, `GoalGetGoodsForHousehold`, `GoalGetResourcesForShops`: дія вимагає коректного доступу до реального місця складу.
- Керування дверима/хвіртками: коротше утримання відчиненими (близько 10 тіків до періодичної перевірки після останнього запиту), перевірка зайнятості самого отвору, відсутність повторного відкривання пройдених вузлів маршруту. Двері, відкриті гравцем, не повинні автоматично закриватися без їхнього відкривання NPC.
- Стрижку овець: партії вовни з порогом **15 одиниць** (можливе невелике перевищення внаслідок однієї дії), підвищення пріоритету доставки повної партії до складу, підрахунок усіх 16 кольорів вовни без зміни їхніх кольорів.
- Лісорубів і акацієві гаї: вибір нижчих доступних колод, узгоджений вертикальний діапазон пошуку/підрахунку деревини, перевірки вільного місця для нових паростків акації.
- Обробку тривалості робіт із видобування: узгоджено конфігураційний час і перетворення мілісекунд в ігрові тіки.

### Виправлено / підготовлено виправлення

- Перевірку перетину радіусів територій поселень під час природної та ручної генерації.
- Ситуації, коли NPC стояв перед стіною, хоча склад був поруч за дверима/хвірткою; додані перевірки видимості, доступних точок підходу й недопущення передачі ресурсів через стіну.
- Оцінку близькості вузлів доступу з урахуванням **X, Y і Z**, а не неповної відстані.
- Евристику A*: усунено подвійне врахування `g`; виправлено порядок розширення та повторне відкриття вузлів за кращого шляху.
- Чергу пріоритетів і накопичену вартість JPS: оновлення кращих маршрутів, повторне відкриття вузлів, правильну довжину стрибків.
- Прохідність простору над **зачиненою хвірткою**: хвіртка не має помилково трактуватися як суцільна стіна.
- Помилку відсутнього імпорту `Comparator` для `InvItemAlphabeticalComparator`.
- Зависання логіки лісоруба на частково зрубаних деревах (раніше гаї з 1–4 колодами пропускалися).
- Спроби посадки акації в місцях, де ванільному дереву бракує простору для росту.

### Стан перевірки та обмеження

- **Це записи про зміни в коді, а не про доведену успішність у грі.** Станом на внесення цього журналу немає підтвердженого результату `compileJava` та повного ігрового регресійного тесту цієї гілки.
- Потрібно перевірити NPC біля дверей і хвірток, подвійні двері, взаємодію кількох NPC, фізичну доставку до складу, коректний вибір маршруту без телепортації, поведінку худоби, партії вовни, лісорубів і ріст акацій.
- Скорочення часу відкривання хвіртки **не гарантує**, що тварина не втече одночасно з NPC.
- Порівняння вартості маршрутів не гарантує математично глобального найкоротшого шляху в усіх умовах ванільної навігації.
- Експериментальні forced-gate зміни присутні в **історії комітів нижче**, але були пізніше видалені: їх **не слід** сприймати як поточну функціональність.
- До успішних тестів **не переносити** цю гілку в `main`.

## База проєкту (та сама версія `8.1.2-port.0`)

- **2026-11-22:** `a1cde6e6` — початкове джерело порту Millénaire 1.18.2 у `main`. Це вихідна точка порівняння, **не окремий опублікований реліз**. Виправлення, що вже містилися в цьому початковому знімку, не привласнюються гілці та не датуються заднім числом.

## Повний покомітний журнал поточної гілки

Нижче — усі **62** коміти поверх базового `main` на момент створення журналу. Оригінальні назви комітів залишені англійською для точного пошуку в GitHub. Час — **UTC**, новіші вище. Записи показують **послідовність розробки**, включно з проміжними спробами, які згодом було скасовано.

### 2026-11-23 (UTC)

- `1097a1b9` — Document acacia grove harvesting and vanilla sapling clearance tests (00:07 UTC).
- `4935dee7` — Skip blocked acacia planting markers that cannot support vanilla tree growth (00:06 UTC).
- `a9f244a3` — Select accessible lower tree trunks and consistently recognize acacia logs in groves (00:04 UTC).
- `fca2dc2a` — Let lumbermen clear partly harvested trees with fewer than five logs (00:04 UTC).

### 2026-11-22 (UTC)

- `fd248796` — Document sheep-batch logistics, colored wool use and door timing regression tests (23:59 UTC).
- `4909b540` — Stop reopening doors behind villagers while following a path (23:58 UTC).
- `40f1b01e` — Close wooden doors promptly like gates after the actual crossing is clear (23:58 UTC).
- `a18cbce2` — Deliver full sheep wool batches before more shearing or breeding, retaining colour (23:57 UTC).
- `6e3a8d2f` — Shear sheep in bounded wool batches and count all wool colours (23:57 UTC).
- `6018d5f2` — Restore Comparator import required by InvItemAlphabeticalComparator (23:28 UTC).
- `68947d1a` — Mark superseded forced-gate experiments as historical playtest findings (23:22 UTC).
- `321db9b7` — Document systemic cost-based navigation rework and gameplay regression tests (23:22 UTC).
- `f20d6f6c` — Log alternative route costs when pathing diagnostics are enabled (23:21 UTC).
- `4f534a4e` — Allow head clearance above closed fence gates so normal pathfinder can traverse them (23:20 UTC).
- `2ddb8126` — Never improve A* queue nodes through nonviable terrain transitions (23:20 UTC).
- `3f26a3c1` — Remove forced gate routing and retain generic navigation with automatic door interaction (23:20 UTC).
- `569e6d3d` — Compare physical costs of bounded alternate paths instead of accepting first reachable detour (23:19 UTC).
- `bbd8aa9d` — Fix JPS queue decrease-key, reopen improved nodes and correct neighbor costs (23:18 UTC).
- `a8c4d230` — Correct classic A* expansion order and reopen cheaper closed nodes (23:18 UTC).
- `10973b0c` — Use admissible A* heuristic; remove double-counted path cost (23:18 UTC).
- `b884b8d4` — Record 00:08 gate-bypass trace and inside-face staged route diagnostic (23:10 UTC).
- `efff351d` — Use inside-gate approach for courtyard exits and log forced-crossing rejection reasons (23:10 UTC).
- `52e319c5` — Explain confirmed gate-bypass log and staged gate crossing patch (23:02 UTC).
- `faece577` — Force courtyard exit across a reachable fence gate when A* bypasses opening (23:01 UTC).
- `2975aacb` — Audit log evidence: distinguish relevant gate path failures from unrelated gate probes (22:50 UTC).
- `b043d2d2` — Differentiate gate route failure reasons and reduce irrelevant navigation logs (22:49 UTC).
- `26f872c2` — Include gate-entry failure marker in playtest log instructions (22:42 UTC).
- `8c4356bd` — Add gate-entry failure logging and remove obsolete exit comment (22:42 UTC).
- `ebe6c9e4` — Document teleport-in bypass of remembered gate and diagnostics (22:41 UTC).
- `8c994f2a` — Log blocked gate exits and stock teleports for targeted repro (22:41 UTC).
- `49f170aa` — Keep gate waypoints partial and avoid loading unloaded remembered gates (22:40 UTC).
- `c671e04a` — Find courtyard exit gate even after villager teleports inside; log route selection (22:40 UTC).
- `3f4f72a7` — Track asymmetrical courtyard exit routes and recent-gate waypoint experiment (22:34 UTC).
- `5bb2a099` — Prefer recently traversed courtyard gate for return trips across enclosure (22:33 UTC).
- `76b3336a` — Track fast-close gate policy and remaining livestock escape limitation (22:20 UTC).
- `8cd88859` — Avoid opening pen gates while planning distant stock routes (22:19 UTC).
- `7ae69ef7` — Close livestock-pen gates promptly once crossing is clear (22:19 UTC).
- `e45a4a01` — Document gate-first rescue for physically stalled residents (22:09 UTC).
- `3b67b5b7` — Try gate entry route on physical stalls before teleport fallback (22:09 UTC).
- `e3efb06b` — Record failed Anna playtest and side-gate waypoint fix for retesting (22:08 UTC).
- `5e5ca413` — Allow gate access waypoint routes time to approach before automatic closing (22:08 UTC).
- `f5882079` — Probe accessible side gates as stock-transfer waypoints before teleport recovery (22:07 UTC).
- `4fc9cab0` — Run Gradle in child PowerShell and safely stage and replace JAR (21:54 UTC).
- `4e16ff14` — Fix BAT launcher and keep errors visible until keypress (21:54 UTC).
- `fa548543` — Document reproducible stock-access wall trap and navigation acceptance tests (21:51 UTC).
- `acc11959` — Rank access-node proximity in all three axes (21:50 UTC).
- `5a0adcb9` — Route stock-transfer NPCs to reachable visible access nodes, not walls (21:50 UTC).
- `8d6f94df` — Require physically accessible storage for GoalDeliverGoodsHousehold (21:50 UTC).
- `e3be504b` — Require physically accessible storage for GoalBringBackResourcesHome (21:50 UTC).
- `9cb3d712` — Require physically accessible storage for GoalDeliverResourcesShop (21:50 UTC).
- `7d8d32fd` — Require physically accessible storage for GoalGetGoodsForHousehold (21:50 UTC).
- `785d6b0e` — Require physically accessible storage for GoalGetResourcesForShops (21:50 UTC).
- `0a14734c` — Validate real line of sight for villager stock transfers (21:49 UTC).
- `3639b429` — Add automatic Millenaire build and installation scripts (21:33 UTC).
- `1cd1f1c6` — Accurately describe old mining tick interval (21:11 UTC).
- `6d02d89f` — Clarify mining duration tick units in the playtest audit (21:10 UTC).
- `f105ce96` — Honor goal durations already converted from milliseconds to game ticks (21:10 UTC).
- `8dbacb74` — Track reported navigation, production and debug-mode gameplay issues (21:09 UTC).
- `19e57881` — Respect configured mining duration for quarry and ore goals (21:08 UTC).
- `68d7977c` — Prevent overlapping village claim radii for natural and manual generation (21:07 UTC).
- `218c93e2` — Use shared door/gate leases and unblock stalled approach routes (21:06 UTC).
- `6b481aa7` — Add shared server-side door and gate access leases (21:06 UTC).

## Правила ведення журналу надалі

1. **З кожною зміною коду** доповнювати верхній розділ `[Unreleased]`: коротко описувати вплив на гру, категорію (додано / змінено / виправлено), технічний компонент, статус тестування та посилання на коміт після його створення.
2. Якщо фікс спершу експериментальний — явно вказувати **«підготовлено, не перевірено»**. Після компіляції й ігрової перевірки змінювати статус, не заднім числом.
3. Не записувати як поточну функцію рішення, що було **відкочене або замінене**; залишати їх у покомітній історії з позначкою про скасування.
4. Для **справжнього випуску** переносити відповідні записи з `[Unreleased]` в окремий розділ із **реальним** значенням `mod_version`, датою релізу й Git-тегом (якщо він створений). Не змінювати версію без фактичної зміни `gradle.properties`.
5. Після злиття гілки в `main` зберігати записи, але оновлювати статус гілки; `main` не вважається зміненим, доки merge не виконано.
