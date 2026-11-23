# Millénaire / NAV-OPT — журнал аудиту, змін і передачі контексту

**Дата:** 23 листопада 2026 року
**Репозиторій:** `pewpewbs/Millenaire_mod_updating`
**Робоча гілка:** `fix/villager-door-access-and-village-territory`
**Стек:** Minecraft 1.18.2, Forge 40.3.0, Java 17.
**Безпека:** НЕ зливати в `main` без результату Forge build і регресійного Minecraft playtest.

## 1. Вихідне спостереження та ідея гравця

- Підйом NPC по драбині після патча LADDER-03 гравець **підтвердив у грі**. Це результат попереднього етапу, **не** тест нової NAV-OPT-збірки.
- За новим спостереженням, NPC **знаходить драбину знизу, але не знаходить маршрут до неї згори**.
- Пропозиція гравця: для A→B порівнювати пошук A→B і B→A, запам'ятовувати вже знайдені маршрути та пробувати обернений варіант, якщо він дешевший.
- Важлива відмінність: маршрут **не гарантовано оборотний** у Minecraft (одностороннє падіння, перепад висоти, верхній вихід, відкривання дверей). Кожне ребро зворотного шляху має пройти перевірку в потрібному напрямку, у живому світі перед використанням.
- Додаткова мета: системна оптимізація навігації, детальні діагностичні журнали та можливість відразу запускати регресійні тести.

## 2. Точний стан до NAV-OPT

Перевірені файли:
- `PathWorldSnapshot`: копіює палітри секцій світу на головному потоці, максимум 256 чанків області й 1024 непорожні секції, не завантажує нових чанків. До NAV-OPT дозволяв лише **одну** побудову snapshot на світ і тик, повторні запити відкладав.
- `PathSearchExecutor`: 2 фонові потоки, FIFO-черга 64, скасування завдань.
- `AStarWorker`: класичний A*, 150 мс бюджет пошуку, прямі секції драбини.
- `AStarWorkerJPS`: JPS, перевірка розгорнутих ребер; не будує вертикальні переходи самостійно.
- `AStarPathPlannerJPS`: JPS→класичний A*, можливе порівняння біля драбин.
- `TransitionClearanceRules/AStarStatic`: спрямована перевірка фізичного проходу між точками, включно зі сходами, плитами та драбинами.
- `NavigationMetrics`: до змін були час native pathfinder, snapshot, кількість скопійованих секцій, відкладені пошуки й черга, але **не було** детальної статистики CPU JPS/A*, пам'яті маршрутів і зворотних пошуків.
- `MillVillager.computeNewPath`: відкладений snapshot/відхилений worker раніше міг викликати `onNoPathAvailable`, що веде до Vanilla fallback без реального висновку «шляху немає».

## 3. Реалізовані зміни NAV-OPT — Фаза 1

### NAV-OPT-01: пам'ять маршрутів

Файл: `src/main/java/org/millenaire/common/pathing/RouteMemory.java`.

- Координати маршруту без посилань на чанки/NPC/World, `List.copyOf`, LRU.
- Обмеження: до **96** маршрутів на світ, до **96** точок в одному маршруті, термін життя **200 тіків** (10 с при 20 TPS).
- Сховище `AStarPathPlannerJPS` окреме для кожного `Level` через `WeakHashMap`.
- Запит повертає і прямий, і зворотний кандидати за однакових конфігурацій навігації. Обирається **дешевший з фізично перевірених**.
- Перед використанням перевіряються блоки **поточного світу**, прохідність кожного орієнтованого ребра, діагональні кути та реальні секції драбини; зміни світу інвалідовують кеш.
- Для обмеження ризику TPS live-перевірка кешу має сукупний ліміт **2 мс**. Перевищення часу означає cache miss, але не означає, що маршрут некоректний.
- Кешування точне за початком/метою: **поки НЕ** реалізовано зшивання маршрутів із середини чи глобальний граф доріг.

### NAV-OPT-02: вимірювання продуктивності

Файл: `src/main/java/org/millenaire/common/pathing/NavigationMetrics.java`.

Показники `NAV-OPT-STATS`, не частіше ніж раз на 200 тіків на світ (за наявності запитів):
- `native`, `snapshot` (кількість, середній/максимальний час);
- `jps`, `astar` (пошуки, середній/максимальний час, expanded);
- `copied_sections`, `snapshot_reused`, `deferred`, `stale_routes`;
- `cache_hit`, `cache_reverse`, `cache_miss`, `cache_invalidated`, `routes_stored`;
- `reverse_tried`, `reverse_found`, `active`, `queued`, `rejected`.

**Це загальні лічильники процесу**; вони не є показником CPU одного поселення. Середній TPS/FPS у них не вимірюється.

### NAV-OPT-03: правильна обробка відкладених запитів

Файли: `AStarPathPlannerJPS.java`, `MillVillager.java`.

- `hadImmediatePath()`: вдалий кеш-хіт, шлях уже передано NPC.
- `wasDeferred()`: бюджет snapshot/черга тимчасово не дозволили пошук — **це не NO_PATH**.
- При deferral NPC зберігає діючий маршрут і повторює запит після малого зсунуто-часового cooldown, без негайного запуску Vanilla fallback. Ризик: при великій конкуренції запит може довше чекати; перевірити чергу й поведінку 30–100 NPC.

### NAV-OPT-04: зворотний класичний A* як обережний recovery

Файл: `AStarWorker.java`.

- Для вертикальних маршрутів із різницею висоти 2–48 блоків та горизонтальною відстанню до 96 блоків.
- Виконується **лише якщо прямий класичний шлях не знайдено або він є великим обхідним маршрутом**; для звичайних прямих маршрутів подвійний пошук не запускається.
- Зворотний пошук B→A має окремий бюджет 150 мс. Отриманий шлях фізично перевіряється у напрямі A→B. Загальний бюджет на пошук і валідацію зворотної альтернативи обмежено ~175 мс.
- Порівнюється вартість прямого і зворотного валідного кандидата; обирається дешевший. **Це не гарантує глобально оптимального маршруту в усьому світі.**
- Результати зворотного пошуку: `NAV-OPT-REVERSE from=... to=... candidateValid=... reason=... forwardCost=... reverseCost=... chosen=... tookMs=...`.

### NAV-OPT-05: обмежене повторне використання snapshot

Файл: `PathWorldSnapshot.java`.

- Повторне використання **лише в тому самому серверному тіку**, якщо запитувана область повністю вміщується в раніше скопійованій.
- `WeakReference` до знімка, одна копія може обслуговувати кілька пошуків; не вводиться ризикове міжтікове кешування після будівництва.
- Раніше заборонена друга копія snapshot у тому самому тіку залишається відкладеною, якщо область не вміщується.
- Статистика `snapshot_reused`.

## 4. Журнал комітів (хронологічно)

- `84d4f09` створення `RouteMemory`.
- `a3e72c3` обидва варіанти маршрутів і selective invalidation.
- `a6c34dd` метрики JPS/A*, cache, reverse.
- `65b80be` підключення route cache й `NAV-OPT-STATS` до планувальника.
- `a489ce1` виправлена семантика DEFERRED ≠ NO_PATH.
- `a4fd9b2` пошук у зворотному напрямку.
- `2bd85a2` окремий deadline для reverse-валидації.
- `00637e2` повторне використання snapshot в одному тіку.
- `42917c9` метрика snapshot reuse.
- `a71a95e` Java regression smoke для route memory.
- `dcc805f` source-level інтеграційні перевірки.
- `5633f20` **виправлена помилка компіляції**, виявлена реальним Java smoke.
- `0e2eff9` оновлено старий тест navigation priority під нову семантику.
- `193011b` ліміт 2 мс на валідацію кеш-хіта.
- `81c7a80` запис причини відхилення зворотного шляху: `NO_REVERSE_PATH`, `ENDPOINT_MISMATCH`, `VALIDATION_TIMEOUT`, `BLOCKED_EDGE x,y,z->x,y,z`, `UNLOADED_CHUNK`.

УВАГА: повний перелік найновіших комітів можна подивитися в GitHub; цей журнал відображає поточну фазу NAV-OPT, а не всі попередні 200+ комітів у гілці.

## 5. Тести й чесний статус

**ПРОЙДЕНО — реальне виконання Java 17:**
- У контейнері локально скомпільовано **функціонально еквівалентну копію** `RouteMemory` і виконано ті самі сценарії, що збережені в `tools/tests/RouteMemorySmoke.java`: прямий/зворотний порядок, конфігурації, TTL, два кандидати, selective invalidation, LRU та незалежність інстансів. `javac --release 17` і `java RouteMemorySmoke` дали PASS. **Офіційний файл із GitHub після Pull потрібно запустити окремо** — його байти не були автоматично перенесені в контейнер.
- Перша компіляція завершилася **FAIL**, бо `record Entry` затіняв `Map.Entry` у `removeEldestEntry`.
- Після перейменування `Entry → CacheEntry` повторний `javac --release 17` та запуск Java дали **PASS**.

**ПРОЙДЕНО — джерельні перевірки інтеграції на текстах з GitHub:**
- 12 перевірок: кеш не обходить live world/edge validation, обмеження часу, відсутність unsafe reverse, deferral ≠ failure, один тик для reuse snapshot, метрики, компіляційний конфлікт усунутий.
- Окремо додано `tools/tests/test_navigation_optimization_wiring.py`; перед релізом запускати його **локально**, оскільки він не є Forge-тестом.

**НЕ ВИКОНАНО / НЕ ПІДТВЕРДЖЕНО:**
- `gradlew.bat compileJava` для актуального HEAD (у робочому середовищі немає повного Forge SDK/репозиторію).
- Minecraft gameplay на **новому** NAV-OPT JAR.
- Фактичні `NAV-OPT-STATS` з гри: жодних значень TPS/CPU/частоти cache hit ще не виміряно.
- На сходах і драбині можливі асиметричні переходи, тому зворотний маршрут може бути відхилений; це нормально і безпечніше за телепортацію крізь блок.
- HPA* / глобальний hierarchical graph, інкрементальний міжтіковий versioned snapshot cache, пріоритезована черга та повне інкрементальне перепланування **ЩЕ НЕ РЕАЛІЗОВАНІ**. Вони потребують профілів реального навантаження і окремих тестів.

## 5.1. Результати тестів на комп'ютері користувача — 23.11.2026, 18:17 (Europe/Berlin)

**Джерело:** користувач надіслав повний вивід Windows PowerShell із запущеними тестами з папки `millenaire-1.18.2-forge`. Нижченаведені результати — його фактичні логи, а не оцінки асистента.

- **Середовище:** Eclipse Temurin OpenJDK `17.0.21.1`, компілятор `javac 17.0.21.1` (попередньо використовувалася Java 8 без доступного `javac`; встановлення JDK 17 та `PATH` виправлено).
- **Реальна компіляція точних локальних вихідних файлів і запуск:** `javac --release 17 -d build/route-cache-smoke src/main/java/org/millenaire/common/pathing/RouteMemory.java tools/tests/RouteMemorySmoke.java` — без помилок; `java -cp build/route-cache-smoke RouteMemorySmoke` → `PASS: forward/reverse lookup, independent directions, TTL, config, LRU and isolation`.
- **Python source-level перевірка:** `py -3 tools/tests/test_navigation_optimization_wiring.py` → **6/6 OK**; назви тестів: `test_cache_world_isolated_and_bounded`, `test_deferred_is_not_mistaken_for_missing_path`, `test_diagnostic_counter_names`, `test_reverse_search_is_bounded_and_validated_directionally`, `test_route_cache_always_checks_current_world`, `test_snapshot_reuse_only_same_tick`.
- **Python source-level перевірка:** `py -3 tools/tests/test_navigation_priority.py` → **9/9 OK**; включно з `test_vertical_edges_require_two_real_ladder_rungs`, `test_primary_path_never_begins_with_vanilla`, `test_physical_stall_can_try_vanilla` та іншими перевірками інтеграції.
- **Сумарно:** один Java smoke PASS + 15 Python перевірок OK. Це **не** Forge-компіляція моду та **не** підтвердження працездатності алгоритмів у Minecraft.
- **Поточний блокер:** ще немає результату `gradlew.bat compileJava` або `Update-Millenaire.bat` і нового `latest.log` зі зворотними спусками. Наступний крок — збірка, потім тест з верхньої площадки драбини.

## 6. Як протестувати локально (Windows)

1. GitHub Desktop: `Fetch origin` → `Pull origin`, гілка `fix/villager-door-access-and-village-territory`. Зробити backup Minecraft світу.
2. У `millenaire-1.18.2-forge` виконати `javac --release 17 -d build/route-cache-smoke src/main/java/org/millenaire/common/pathing/RouteMemory.java tools/tests/RouteMemorySmoke.java` та `java -cp build/route-cache-smoke RouteMemorySmoke`.
3. Виконати `py -3 tools/tests/test_navigation_optimization_wiring.py` та `py -3 tools/tests/test_navigation_priority.py`.
4. Запустити `Update-Millenaire.bat` або `gradlew.bat compileJava` — для інтегрованого патча **обов'язково `BUILD SUCCESSFUL`**.
5. У конфігу Millénaire ввімкнути `logpathing=minor` (потрібно `LogPathing >= 2` для деталізованих `NAV-OPT-CACHE` та `NAV-OPT-REVERSE`). Збережіть `latest.log` і `millenaire.log`.
6. Відтворити **5 циклів** шахтаря вниз/угору у кар'єрі біля `(26,60–68,655)`, зокрема починати **з верхньої площадки**. Порівняти `NAV-OPT-REVERSE`, `NAV-OPT-CACHE`, `LADDER-ASSIST`, `LADDER-EXIT`, `NAV-STALLED`, `NAV-TELEPORT`.
7. Після першого успішного переходу A→B зробити B→A. Очікування: якщо шлях оборотний, можливий `NAV-OPT-CACHE reverse=true`; якщо ні — `cache_invalidated` або зворотний A* пошук.
8. Розмістити/прибрати блок на раніше прохідному маршруті та перевірити, що застарілий кеш **не використовується**.
9. Повторити тест із 20 і 50 NPC, дочекатися 2–3 звітів `NAV-OPT-STATS`, порівняти CPU/TPS з попереднім білдом. Умови світу, цілі NPC й дальність рендерингу мають бути однаковими.

## 7. Що передати наступній моделі

- Відкрити цей журнал і `docs/PLAYTEST_ISSUES_2026-11-23_UA.md` (особливо LADDER-03 і STAIR-01/02).
- **Не вважати логічні smoke-тести повною Forge-збіркою.**
- Спочатку перевірити `compileJava` і актуальні NPC логи, а вже потім змінювати асиметричні ребра драбин.
- Найбільші технічні ризики фази: додаткові витрати часу reverse A* при складних вертикальних запитах, дорогі live-перевірки cache hit, повторні відкладені запити при багатьох NPC і можливі рідкі випадки з нестандартною геометрією.
- Наступні архітектурні напрямки: порівняти метрики із baseline; за потреби обмежити або пріоритезувати запити через NavigationScheduler; потім інкрементальний snapshot cache та лише після цього оцінювати вигоду HPA*. Не припускати, що будь-яка з цих систем уже готова.

## 8. Правило ведення журналу

Для кожного наступного патча: дата, конкретний ігровий симптом, підтверджена/підозрювана причина, перелік змінених файлів, SHA комітів, чи був успішний `compileJava`, назви smoke-тестів і результати, відомі обмеження, лог-маркери та відтворюваний ігровий сценарій. Жодних вигаданих FPS/TPS, `BUILD SUCCESSFUL` чи результатів гри.


## 9. Ігровий тест NAV-OPT — latest(9).log (23.11.2026, 18:45–18:56)

**Джерело:** користувач завантажив повний `latest(9).log` (16 618 рядків, приблизно 3,75 MB). Проаналізовано фактичний текст логу, а не старий аудит. Користувач повідомляє, що нове «± працює», а основні помітні проблеми — старі двері. Це суб'єктивне спостереження **не дорівнює** гарантії всіх маршрутів.

**Підтверджені факти в логах:**
- Forge 40.3.0, Minecraft 1.18.2, Millénaire `millenaire-8.1.2-port.0.jar` завантажується. Записи нових `NAV-OPT-` доводять, що запущена збірка містить ці діагностичні механізми.
- Останній накопичувальний `NAV-OPT-STATS` о 18:55:37: `jps searches=3057 avg=30.082ms max=150.089ms expanded=634640`; `astar searches=1805 avg=21.944ms max=150.868ms expanded=1836968`. Це час пошуку у робочих потоках, **не** затримка серверного тіку. `native count=3283 avg=4.647ms max=202.682ms`.
- `snapshot count=2959 avg=0.368ms max=3.418ms copied_sections=981606` — велика сумарна кількість копіювань; `snapshot_reused=100`, `deferred=1896`. `cache_hit=905` із них `cache_reverse=41`, `cache_miss=17094`, `cache_invalidated=9`, `routes_stored=1728`, `stale_routes=0`, `active=0 queued=0 rejected=0` у момент останнього звіту. Це накопичені значення, `queued=0` у цю мить **не** доводить, що черги не було раніше.
- `reverse_tried=226`, `reverse_found=11`. Із 226 `NAV-OPT-REVERSE` рядків: `ENDPOINT_MISMATCH=206`, `NO_REVERSE_PATH=3`, `NONE(valid)=17`. Після успішної валідації 11 маршрутів було прийнято як кращі. Раніше лог міг писати `chosen=REVERSED`, навіть коли **обидва** кандидати `null`; це **лише дефект логування**, не доказ використання null-маршруту.
- Серйозна проблема частоти повторного пошуку: **192 із 226** зворотних спроб стосуються *одних і тих самих* `from=[26|71|647]` → `to=[29|66|660]` (шахтар Eftokios Prinkips), приблизно між 18:54:58 і 18:55:08, майже всі `ENDPOINT_MISMATCH`. У 18:55:07 накопичувальні метрики вказують на **приріст** ~178 `reverse_tried`, 923 `deferred` і 6 983 `cache_miss` лише за попередні 10 секунд. Це індикатор патологічного перепланування, а не корисного кешування.
- Драбини: `LADDER-ASSIST=61` (напрям `1` — 49, `0` — 12), `LADDER-EXIT=10`; всі 10 `LADDER-EXIT` — Eftokios. Отже, підтверджено **підйом і вихід угору**. `direction=-1` у цьому файлі **відсутній**, тож спуск із верхнього майданчика ще не підтверджений цим логом.
- Двері: `DOOR-APPROACH=5731`, `DOOR-WAYPOINT=807`, `DOOR-LEASE=679`, `DOOR-BLOCKED=121` (з них `NO_VISIBLE_DOOR_CANDIDATE=104`, `AT_WAYPOINT=11`, `STOP_MOVING=6`). Деякі `DOOR-APPROACH selected=null` є діагностикою відсутності вибраного кандидатного дверного блоку, **не кожне таке повідомлення означає збій**. Повторювані `DOOR-BLOCKED` свідчать про невирішений дефект.
- `NAV-TELEPORT=47` — зафіксовані **stock recovery teleports** зокрема Anatolios Zaridis (14) і Niompe Prinkips (15). Це не загальна кількість усіх можливих телепортацій, оскільки інші `jumpToDest()` можуть логуватися окремо/не логуватися.
- `Can't keep up! ... 3389ms or 67 ticks behind` — **один** запис одразу після завантаження світу о 18:45:14. Не можна робити висновок про сталий TPS із цього одного рядка.
- У видимому логу немає Java stack trace / Exception; є конфігураційні й ресурсні попередження, які не варто плутати з навігацією.

**Висновок:** нова логіка використання кешу та руху по драбинах **працює у грі**, але повторні невдалі запити та асиметрія вертикальних маршрутів залишаються. FPS/TPS performance baseline для підтвердження прискорення ще **немає**.

### 9.1. Негайні малоризикові виправлення за фактичним логом

- **NAV-OPT-06 diagnostics:** `AStarWorker.java` тепер пише фактичні `actualStart=x,y,z` та `actualGoal=x,y,z` при `ENDPOINT_MISMATCH`. Виправлено оманливе `chosen=REVERSED`, коли не існує ні прямого, ні зворотного маршруту — тепер `chosen=NONE`. Коміт `bee1184`.
- **NAV-OPT-06 duplicate failure backoff:** `AStarPathPlannerJPS.java` при справжньому кінцевому `NO_PATH` запам'ятовує початок, ціль та навігаційну конфігурацію й відкладає **лише ідентичний** запит приблизно на 20–26 тіків (1–1,3 с за 20 TPS). Новий початок, ціль чи конфігурація **не блокуються**; знайдений маршрут очищає стан. Після відкладення не запускається додатковий синхронний fallback. Коміт `cdb63cb`.
- **NAV-OPT-06 metrics:** додано `no_path_cooldown=` у `NAV-OPT-STATS`. Коміт `212c4ab`.
- **NAV-OPT-06 source regression:** два тести перевіряють точні критерії cooldown і достовірність нової reverse діагностики; коміт `57bd232`.

**Важливо:** ці коміти були створені **після** `latest(9).log`, тому цей файл **не підтверджує** ефект зворотного дедуплікування та нового логування. Інтегрована Forge-компіляція цих комітів та повторний ігровий тест — **очікуються**. Не зараховувати результати старої збірки як тести нового коду.

### 9.2. Наступний раунд випробувань

1. `git pull` у робочій гілці, `py -3 tools/tests/test_navigation_optimization_wiring.py` (очікується 8 тестів), `gradlew.bat compileJava`, `Update-Millenaire.bat`; **перевірити BUILD SUCCESSFUL**.
2. Зробити backup світу й повторити 5 циклів шахтаря **з верхнього майданчика вниз** (без ручного переміщення NPC). Шукати `LADDER-ASSIST direction=-1` та фізичний спуск; це ще не було підтверджено.
3. Спостерігати `from=[26|71|647]` до шахти `to=[29|66|660]`. Нові `ENDPOINT_MISMATCH actualStart/actualGoal` покажуть, наскільки фактично розходяться endpoints. Не змінювати алгоритм зворотного шляху, доки це не відомо.
4. За приблизно 10 хвилин отримати кілька `NAV-OPT-STATS`, порівняти `no_path_cooldown`, `reverse_tried`, `deferred`, `cache_miss` та JPS/A* searches/sec із попереднім логом.
5. Для дверей окремий тикет: `DOOR-BLOCKED reason=NO_VISIBLE_DOOR_CANDIDATE` та stock teleports; не змішувати з NAV-OPT-06 і не робити необґрунтованих змін без геометрії конкретних дверей.


### 9.3. Повторна локальна перевірка NAV-OPT-06 (PowerShell, 23.11.2026)

Користувач після запуску `Update-Millenaire.bat` і оновлення репозиторію виконав із папки `millenaire-1.18.2-forge`:

```powershell
py -3 tools/tests/test_navigation_optimization_wiring.py
```

**Реальний результат: `Ran 8 tests in 0.002s — OK`.** Вісім перевірок: `test_cache_world_isolated_and_bounded`, `test_deferred_is_not_mistaken_for_missing_path`, `test_diagnostic_counter_names`, `test_failed_search_cooldown_preserves_new_goals`, `test_reverse_diagnostics_distinguish_no_route_from_reversed`, `test_reverse_search_is_bounded_and_validated_directionally`, `test_route_cache_always_checks_current_world`, `test_snapshot_reuse_only_same_tick`.

Тести перевіряють **текстове зв'язування коду**, а не компіляцію Forge чи поведінку в Minecraft. Скриншот початку `Update-Millenaire.bat` підтверджує `git pull` і запуск Gradle, але поки що **не** містить завершального `BUILD SUCCESSFUL` — користувач його ще не надав. Наступна перевірка: завершення збірки + повторний `latest.log` для діагностики `no_path_cooldown`, `ENDPOINT_MISMATCH actualStart/actualGoal` і спуску по драбині.

## 10. NAV-OPT-06 повторний playtest: `latest(10).log` (23.11.2026)

**Джерело:** повний наданий користувачем `latest(10).log`, 4 254 рядки, ~929 KiB. Запуск клієнта: 19:11:13; у світі 19:12:37–19:15:37 (приблизно 3 хв). Forge 40.3.0, Minecraft 1.18.2, мод `millenaire-8.1.2-port.0.jar`. Наявність `no_path_cooldown` та `reason=ENDPOINT_MISMATCH actualStart/actualGoal` у живих логах підтверджує, що в грі використовується код NAV-OPT-06. Публікація ігрового логу **не** надає окремого Gradle `BUILD SUCCESSFUL`.

### 10.1. Результати та порівняння з `latest(9).log`

- Останній накопичувальний `NAV-OPT-STATS` о **19:15:23**: `jps searches=715 avg=36.724ms max=150.320ms expanded=174750`, `astar searches=392 avg=31.027ms max=150.695ms expanded=513009`; `native count=415 avg=4.919ms max=112.796ms`; `snapshot count=687 avg=0.427ms max=3.235ms`, `copied_sections=239411`.
- У тому самому звіті `deferred=209`, `snapshot_reused=28`, `no_path_cooldown=25`, `cache_hit=150`, `cache_reverse=13`, `cache_miss=2299`, `cache_invalidated=0`, `routes_stored=529`, `reverse_tried=9`, `reverse_found=4`, `active=0 queued=0 rejected=0`. Це накопичені лічильники на 19:15:23; до кінця сесії журнал містить **152** повідомлення `NAV-OPT-CACHE` (із них **15** `reverse=true`) і **10** `NAV-OPT-REVERSE` (із них **5** `chosen=REVERSED`).
- Десять зворотних спроб: **6** `candidateValid=true`; **3** `ENDPOINT_MISMATCH`; **1** `NO_REVERSE_PATH`; кінцевий вибір: **5** `REVERSED`, **4** `FORWARD`, **1** `NONE`. Попередній масовий цикл сотень зворотних пошуків шахтаря в цьому тесті не повторився, однак відрізняються тривалість сесії, цілі, поведінка NPC, і немає доказів, що саме cooldown повністю усунув причину.
- При `ENDPOINT_MISMATCH` виявилися реальні розходження: `from=[190|71|675]`, `to=[190|65|681]` → `actualStart=189,65,677`, `actualGoal=190,65,681`; та двічі `from=[188|71|673]`, `to=[191|67|679]` → `actualStart=190,71,675`, `actualGoal=191,67,679`. Гіпотеза — tolerance/дострокове завершення досягнення цілі в A*: **потрібен окремий аудит умови завершення, не можна просто вважати такий шлях еквівалентним точному**.
- Від шахтаря `Eftokios Prinkips`: `from=[30|65|658]` → `to=[26|68|648]` п'ять разів отримував `forwardCost=29.70`, `reverseCost=23.24`, `chosen=REVERSED`: мінус **~21,8% розрахункової довжини/вартості**, але не вимір фактичного часу проходження.
- `LADDER-ASSIST=9`, **усі direction=1** (є повідомлення `aligned=false contact=false` під час підходу), `LADDER-EXIT=1` для Eftokios о 19:13:49. `direction=-1` **немає**: фактичний спуск по драбині окремо ще не підтверджено. NPC також переміщалися зі схилів/сходів на нижчі рівні, але це не доказ руху вниз саме по драбині.
- `DOOR-BLOCKED=23` (20 `NO_VISIBLE_DOOR_CANDIDATE`, 2 `AT_WAYPOINT`, 1 `NO_DESTINATION`). `NAV-TELEPORT=14` (Anatolios Zaridis 4, Lizas Raoul 2, Arminios Prinkips 2, Nkiolanta Maliasenos 2, Niompe Prinkips 2, Theotota Mourtzouplos 1, Elena Prinkips 1). `DOOR-APPROACH=1655` і `DOOR-WAYPOINT=164`; `selected=null` сам по собі не є помилкою.
- Зафіксовано один `Can't keep up!` на початку завантаження світу (3 009 мс, 60 ticks behind), немає доказів сталої деградації TPS. Справжніх crash stack traces у цьому файлі немає, присутні конфігураційні й ресурсні попередження.

### 10.2. Нова знайдена проблема NAV-OPT-07: гарячий цикл успішних cache-hit

Між 19:14:23 та 19:14:28 NPC `Nkiolanta Maliasenos` повторювала **105** разів `NAV-OPT-CACHE action=HIT reverse=false from=[6|68|631] to=[1|68|627]` (дорога до `deliverresourcesshop`). NPC фактично перебувала близько `(6,67,631)`, з `DOOR-WAYPOINT ... collision=true` для наступного вузла `(5,68,631)`. Одна й та сама успішна кеш-відповідь перезапитувалася майже щотіку, іноді кілька разів за секунду. **Це не CPU-керований повторний A*, але кожен cache-hit проходить перевірку live-world колізій і повторно видає маршрут**. Понад дві третини всіх `NAV-OPT-CACHE` повідомлень цього тесту спричинені одним NPC — отже, необроблений `cache_hit` є завищеною оцінкою *корисного* повторного використання маршрутів.

**Патч після цього логу (не перевірений у ньому):**
- `NAV-OPT-07`: `AStarPathPlannerJPS` запам'ятовує тільки останній **успішно виданий із кешу** start/goal/options + серверний tick. Повторний ідентичний запит протягом **10 тіків** одержує `deferred=true`, без повторної live-колізійної перевірки. Початок/мета/конфігурація змінилися — негайний звичайний пошук, без затримки. Ця зміна **не вирішує саму колізію дверного проходу**.
- `NavigationMetrics`: додає `cache_replan_deferred` у `NAV-OPT-STATS` — лічильник відсічених повторних запитів.
- `tools/tests/test_navigation_optimization_wiring.py`: додано source-level тест, отже очікується **9** перевірок після оновлення гілки.
- Коміти: `ae5c585` (planner), `3f917b6` (лічильник), `eb4f92a` (регресійний тест).
- **Безпечність до перевірки Forge:** відкладений запит зберігає попередню навігацію. Справжня зміна start/goal/options не відкладається. При відсутності маршруту можливий додатковий час очікування до 10 тіків — треба тестувати в Minecraft.

### 10.3. Наступне тестування

1. Оновити робочу гілку `fix/villager-door-access-and-village-territory`, виконати `py -3 tools/tests/test_navigation_optimization_wiring.py` (очікується **9 OK**), потім `Update-Millenaire.bat` і перевірити **BUILD SUCCESSFUL**; не зливати у `main`.
2. Зробити backup світу. Повторити рух до складу біля `(6,67–68,631)`. Очікується менша кількість однакових `NAV-OPT-CACHE`, але `cache_replan_deferred > 0`. Перевірити, що NPC не почав чекати за відсутності перешкоди.
3. Окремо шахтаря перевірити на драбині **зверху вниз**: `LADDER-ASSIST direction=-1` + фізичне переміщення вниз без `NAV-TELEPORT`.
4. Зібрати `latest.log` і порівняти прирости лічильників за **однакові 60 с**, а не тільки підсумки різної довжини сесій; вимірювання TPS потрібне окремо.
5. Двері `DOOR-BLOCKED` і телепортації залишаються окремим незакритим напрямком `DOOR-15`; жоден з NAV-OPT-07 не змінює обробник дверей.


## 10.4. NAV-OPT-07: локальна перевірка 9/9

**Джерело:** скриншот PowerShell, наданий користувачем 23.11.2026. Команда виконана з кореня репозиторію з явним шляхом до тесту:

```powershell
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_optimization_wiring.py
```

**Фактичний результат:** `Ran 9 tests in 0.001s` / `OK`. Усі дев'ять позначено `ok`, включно з новим `test_identical_cached_routes_are_throttled_without_blocking_new_goals`, перевірками лімітів кешу, зворотних маршрутів, поточного світу, відкладених запитів, метрик та snapshot reuse.

**Межі підтвердження:** це Python source-level wiring-тести. Немає наданого завершального виводу `BUILD SUCCESSFUL` для NAV-OPT-07 та немає нового Minecraft `latest.log` після цього патча. Наступний етап: завершення `Update-Millenaire.bat`, перевірка фактичного встановлення JAR та 3–5 хвилин гри; у журналі очікується поле `cache_replan_deferred`, зіставити повтори `NAV-OPT-CACHE`, `DOOR-BLOCKED`, `NAV-TELEPORT` і `LADDER-ASSIST direction=-1`.
