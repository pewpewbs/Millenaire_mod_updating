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

## 11. NAV-OPT-07 — ігрова перевірка latest(20261123-183456).log

**Джерело:** завантажений користувачем `latest(20261123-183456).log` (4 584 рядки, 1 018 085 байт, 19:30:39–19:34:44), Minecraft 1.18.2 / Forge 40.3.0 / Millénaire 8.1.2. Початок серверної сесії близько 19:31:38. Виявлено `cache_replan_deferred=` — це **фактичний runtime-тест** NAV-OPT-07 у Minecraft, але немає окремого виводу Gradle `BUILD SUCCESSFUL` для цієї збірки.

### 11.1. Останній накопичувальний NAV-OPT-STATS: 19:34:32

| Метрика | Значення |
|---|---:|
| JPS | 973 searches, avg **34.809ms**, max 164.806ms, expanded 208702 |
| classic A* | 459 searches, avg **33.848ms**, max 150.749ms, expanded 632321 |
| Vanilla native | 676 calls, avg **5.090ms**, max 317.768ms |
| Snapshot | 951 captures, avg **0.414ms**, max 4.226ms, copied_sections 322329 |
| Snapshot reuse | **23** |
| Deferred capture/search | **347** |
| Failed-path cooldown suppressed | **190** |
| Same cached-route replay deferred | **101** |
| Cache hit / reverse cache | **69 / 20** |
| Cache miss / invalidated / stored | **4016 / 47 / 574** |
| Reverse A* attempts / chosen | **7 / 3** |
| Active workers / queue / rejected | **0 / 0 / 0** у момент цього звіту |

В останній частині файлу є ще одна зворотна спроба (після останнього `NAV-OPT-STATS`): загалом **8** `NAV-OPT-REVERSE` записів і **3** `chosen=REVERSED` у всьому файлі. Одне відхилення зворотного маршруту: `ENDPOINT_MISMATCH actualStart=128,63,697 actualGoal=130,69,676` для `from=[126|62|695] to=[130|69|676]`. Наявність `chosen=NONE` тепер коректно відображає відсутність обох маршрутів.

### 11.2. Чи спрацювало NAV-OPT-07

**Так:** `cache_replan_deferred=101` — доведено, що ідентичні повторні кеш-запити фізично відкладалися. Гаряча пара координат `[6|68|631]→[1|68|627]` тепер має **7** `NAV-OPT-CACHE action=HIT` між 19:32:25–19:32:28 (Niompe Prinkips), тоді як у попередньому лог-файлі схожий цикл видав **105** cache-hits за 5 секунд. Це різні сесії й NPC, тому **не** стверджувати буквальне прискорення 15×. Ідентичний кеш-виклик все ще може траплятися після закінчення 10-тікового cooldown. Причина фізичного застрягання біля дверей залишається.

**Але:** `no_path_cooldown` швидко зріс до **190** у межах 19:32:21–19:32:31; `cache_invalidated` до **47** між 19:32:21–19:32:41. З одним лише агрегованим лічильником **неможливо** визначити NPC/ребро, які спричинили велику кількість невдалих спроб чи інвалідацій. `cache_miss=4016` частково включає запити, які не мали відповідного збереженого маршруту, і не є доказом 4016 пошкоджених кешів.

**Важлива обережність у порівнянні продуктивності:** останній NAV-OPT-STATS цього тесту нараховує 973 JPS та 459 A* проти 715/392 у попередньому `latest(10).log`. Тривалість спостереження подібна (~171 с vs ~163 с від першого до останнього NAV-OPT-STATS), але розподіл цілей NPC і світу різний. У новому тесті абсолютна кількість пошуків **більша**; не заявляти про загальне прискорення без контрольованого A/B, TPS, CPU та незалежних вибірок.

### 11.3. Драбини, двері та відновлення руху

- `LADDER-ASSIST=15`, **усі `direction=1`**, `LADDER-EXIT=1`. Спуск (`direction=-1`) досі **не підтверджений** цією сесією. `Anatolios Zaridis` багаторазово стоїть біля шахти `(27,60–61,655)` із `next=(26,61,655)`, `aligned=false contact=false`; NPC не завжди вдало заходить на вертикальну колонку. `Eftokios Prinkips` має `LADDER-EXIT` о 19:32:19 і справді виходить угору.
- `DOOR-BLOCKED=25`: **21 NO_VISIBLE_DOOR_CANDIDATE**, **4 AT_WAYPOINT**; `DOOR-APPROACH=1742`, `DOOR-WAYPOINT=168`. `selected=null` сам по собі не означає дефект.
- `NAV-TELEPORT=14` (Anatolios Zaridis 4, Niompe Prinkips 4, Lizas Raoul 2, інші по 1). Це stock recovery; не гарантує, що це всі можливі телепортації в моді.
- Один `Can't keep up!` на завантаженні (4062ms / 81 ticks behind), без серії сталих лагів у логах. Не слід робити висновок про фактичний TPS у грі.
- Java Exception stack traces під час гри не виявлено; присутні стартові конфігураційні WARN/ERROR і попередження ресурсів.

### 11.4. Зміни після цього логу — NAV-OPT-08 (не перевірені грою)

Головна мета — з'ясувати причини 47 інвалідацій і 190 відкладених повторів невдалого пошуку, **без зміни логіки вибору маршруту**.

- `AStarPathPlannerJPS.reusePath`: для кожного `cache_invalidated` додається `NAV-OPT-CACHE action=INVALIDATE reason=...`, напрямок `reverse=`, пара `from/to`, конкретне `rejectedEdge=` із причин `NON_ADJACENT_EDGE`, `BLOCKED_NODE`, `BLOCKED_TRANSITION`, `NO_ORTHOGONAL_CORNER`, `CHUNK_UNAVAILABLE`, `ENDPOINT_MISMATCH`. Колізії не обчислюються вдруге лише заради журналу. При перевищенні 2ms `validationDeadline` кеш **не** інвалідується і новий rejection log не видається, як і раніше.
- При `NO_PATH` запис `NAV-OPT-COOLDOWN action=NO_PATH from=... to=... retryTick=...` — **один раз на реальну невдачу**, а не на кожен придушений повтор. Це дозволить зв'язати spike `no_path_cooldown` із фактичними маршрутами.
- Додано source-level regression у `tools/tests/test_navigation_optimization_wiring.py`; після Pull очікується **10** Python-тестів `OK`.
- GitHub-коміти: `d9e2ac7` (причина cache invalidation), `700aa82` (NO_PATH debug), `30b1f08` (регресійна перевірка). Всі у `fix/villager-door-access-and-village-territory`, не у `main`.

**Статус:** код у гілці, але **немає нового Gradle compile / Minecraft playtest** для NAV-OPT-08. Наступний запуск `Update-Millenaire.bat`, source-level тест 10/10, тоді 3–5 хв ігрової сесії з `logpathing=minor`. Новий `latest.log` допоможе відокремити справжні змінені блоки від багів у валідаторі та виявити точні координати проходу. Не змінювати door/ladder geometry навмання без цієї діагностики.

## 12. Передача складних проблем і NAV-OPT-09 — об'єднання незавершених пошуків

**Рішення користувача, 23.11.2026:** проблеми **дверей**, фізичного **спуску/входу на драбини**, колізій сходинок і нестандартних переходів **передати пізніше іншій, більш потужній моделі для глибокого технічного аудиту**. Поточному етапу навігаційних оптимізацій НЕ змінювати ці алгоритми або фізичні правила. Не трактувати цей перенос як виправлення: `DOOR-BLOCKED`, `NAV-TELEPORT`, відсутність `LADDER-ASSIST direction=-1`, `ENDPOINT_MISMATCH` **залишаються відкритими**. Вихідні докази та координати — у розділах 9–11 цього файлу та `docs/PLAYTEST_ISSUES_2026-11-23_UA.md`. `main` не змінювати.

### 12.1. Нове підтверджене архітектурне місце для оптимізації

У поточному коді до NAV-OPT-09 існував **подвійний безумовний виклик скасування**: на вході в `MillVillager.computeNewPath()` і знову всередині `AStarPathPlannerJPS.getPath(...)`. Якщо той самий житель часто повторює запит за незмінних старту, мети й конфігурації, це могло перервати фоновий пошук і змусити CPU працювати над новим знімком і пошуком замість завершення існуючого. Виводи `latest(9)`–`latest(20261123-183456)` показували високі лічильники JPS/A*, але **не вимірювали кількість таких скасувань**; сам механізм підтверджений в коді, фактичний виграш **не підтверджений**.

### 12.2. Реалізовано NAV-OPT-09 (нова збірка ще НЕ перевірена в грі)

1. `NavigationRequestKey.java` — незмінний повний ключ запиту: точні координати **початку й цілі**, напрямок, усі п'ять базових прапорців руху, `tolerance`, `toleranceHorizontal`, `toleranceVertical`. Порівнюється `record.equals`, а не лише 32-бітовий хеш; A→B і B→A **не** ототожнюються на рівні незавершених пошуків. Це не замінює перевірку зворотних готових маршрутів.
2. `AStarPathPlannerJPS.getPath(...)` **перед скасуванням** перевіряє `busy && requested.equals(activeRequest)`. Для точного збігу зберігає поточний worker, чергу, snapshot та generation, повідомляє `hadJoinedSearch()` і збільшує `inflight_joined`. Нова мета/старт/config дозволяє старе скасування з generation-check, щоб застаріла відповідь не замінила новий маршрут. `activeRequest` скидається після фінальної доставки/скасування/відмови черги; якщо JPS передає спробу класичному A*, той самий запит лишається активним.
3. `MillVillager.computeNewPath()` більше **не** скасовує роботу наперед до перевірки ідентичності. Скасування збережено, коли NPC уже у точці мети, потрапив у діапазон дії, ціль не має доступного вузла, або виник виняток доступу до чанка. Для повторного незавершеного запиту додається `NAV-CHOICE=ASTAR-JOIN` (з існуючим rate limit логування).
4. `NavigationMetrics`: нове поле `inflight_joined` у накопичувальному `NAV-OPT-STATS`.
5. `PathSearchExecutor` лишається **FIFO із 2 потоками й чергою 64**; змінено тільки запис часу очікування між прийняттям завдання та початком виконання, без нової пріоритизації. Нові поля `queue_started`, `queue_wait_avg_ms`, `queue_wait_max_ms` у `NAV-OPT-STATS`. Скасоване завдання до початку виконання не включається у середню затримку. Це накопичувальні метрики, **не** ковзне вікно.
6. Додано окремі без-Forge Java 17 регресії: `tools/tests/NavigationRequestKeySmoke.java` (рівність/зміна endpoints, напрямку, політики, null) і `tools/tests/PathSearchExecutorSmoke.java` (FIFO, відкладене виконання у черзі, відміна завдання, метрики).
7. `tools/tests/test_navigation_optimization_wiring.py`: додано три source-level тести, тепер **13** структурних перевірок. Вони не замінюють компіляцію Forge чи Minecraft-тест.

**Коміти NAV-OPT-09 у `fix/villager-door-access-and-village-territory`:**
- `81ce912` — immutable `NavigationRequestKey`;
- `e5f8718` — Java smoke для request identity;
- `0778e3b` — скоалесовані запити у планувальнику;
- `80232d8` — виправлено безумовне скасування у `MillVillager`;
- `f0d6fd6` — `inflight_joined`;
- `c4bb2c9` — облік очікування в `PathSearchExecutor`;
- `63bfb5f` — статистика очікування у `NavigationMetrics`;
- `2e6a5cb` — Java smoke для FIFO/queue;
- `21040e1` — wiring regressions.

### 12.3. Обов'язкові тести (Windows; якщо запускаєш з КОРЕНЯ репозиторію)

```powershell
# Спершу завантажити нові коміти, зібрати й установити JAR
.\Update-Millenaire.bat

# Source-level integration guards; очікується 13 tests, OK
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_optimization_wiring.py

# Окремі Java smoke: запускати із папки моду
cd .\millenaire-1.18.2-forge
javac --release 17 -d build/nav-request-smoke src/main/java/org/millenaire/common/pathing/NavigationRequestKey.java tools/tests/NavigationRequestKeySmoke.java
java -cp build/nav-request-smoke NavigationRequestKeySmoke
javac --release 17 -d build/nav-executor-smoke src/main/java/org/millenaire/common/pathing/atomicstryker/PathSearchExecutor.java tools/tests/PathSearchExecutorSmoke.java
java -cp build/nav-executor-smoke PathSearchExecutorSmoke
```

Потрібно підтвердити `BUILD SUCCESSFUL`; **на момент цього запису для NAV-OPT-09 немає ні повної Gradle-компіляції, ні ігрового логу, ні звіту від користувача про ці два нові Java smoke**.

### 12.4. Наступний ігровий експеримент та можливі ризики

- 5–10 хв навколо активного поселення, `LogPathing >= 2`; перевірити `NAV-OPT-STATS inflight_joined`, `queue_wait_avg_ms`, `queue_wait_max_ms`, а також `NAV-CHOICE=ASTAR-JOIN` і відсутність затримок реакції NPC при **зміні** мети.
- Тест: NPC має поточну мету, отримує кілька тотожних запитів, потім зміна мети під час довгого пошуку; новий шлях **має** перемогти старий. Перевірити, що під час зникнення мети скасування відбувається негайно.
- Відсутність `inflight_joined > 0` **не є** тестовою помилкою: це означає, що навантаження не створило однакових незавершених запитів. Самі лічильники не доводять прискорення TPS; потрібні контрольовані A/B сесії з аналогічним навантаженням і часом.
- **Ризик:** раніше безумовне скасування могло випадково усувати вже неактуальні пошуки. Нове порівняння має враховувати всі параметри руху. Перевірити, що нестандартне змінення цілі на тому самому блоці (але інша поведінкова мета) не вимагає відмінного маршруту: базовий планувальник не знає `goalKey`, лише координати/config, і логічно вважає їх ідентичними. Якщо це некоректно для певних завдань — додати семантичний goal identity на рівні викликача.
- **Далі (після вимірювань):** окремий rate-per-10-second telemetry, облік `search_cancelled_before_start`, queue aging та профілювання live-collision cost. Пріоритезовану чергу й HPA* не вводити без доказу bottleneck.

### 12.5. Явні заборони для майбутньої моделі

**Не вважати завершеними:** двері (`DOOR-BLOCKED`), непідтверджений спуск драбинами, алгоритм у складних сходах/плитах, кінцеві точки зворотного A* (tolerance). Користувач окремо просив залишити ці складні геометричні проблеми для іншої моделі; не робити з `NAV-OPT-09` висновок про їх виправлення. Усі фактичні результати гри, числа та статуси Build мають позначатися джерелом і версією патча.


## 13. NAV-OPT-10 — алокації, чесні інтервальні метрики, справедливість snapshot

**Дата:** 23.11.2026. **Вимога користувача:** продовжувати загальну оптимізацію зараз, а **всі нові зміни тестувати одним раундом пізніше**. Не запитувати поточного запуску Minecraft після кожного коміту. Водночас слід зберігати точні тести для спільного запуску. Це НЕ дозвіл пропускати компіляційні/регресійні перевірки перед release.

### 13.1. Інженерне обґрунтування

- У `ThreadSafeUtilities.getBlockState(BlockGetter, int,int,int)` майже кожне читання блоку створювало `new BlockPos`, навіть коли джерелом був immutable `PathWorldSnapshot`. A*/JPS викликають цей метод багато разів на маршрут, тому тимчасові алокації збільшують тиск на GC.
- `AStarPathPlannerJPS.nearbyLadderOnVerticalRoute()` теоретично робив до `25 * 9 * 9 * 12 = 24300` читань стану блоків із окремим `BlockPos` на кожне. Це верхня математична межа, **не** виміряний середній час. Логіка перевірки геометрії сходинок/драбин **не змінюється**.
- `PathWorldSnapshot.capture()` до NAV-OPT-10 займав єдиний бюджет snapshot на world+tick **перед** перевіркою `>256` чанків області. Один завідомо надмірний запит міг заборонити коротші допустимі запити NPC у тому самому тіку. Це підтверджений аналізом порядку коду недолік; практична частота **ще не виміряна**.
- Старий `NAV-OPT-STATS` містив лише накопичені лічильники. Дві сесії різної тривалості непорівнювані без нормалізації за реальною тривалістю.

### 13.2. Реалізовані зміни NAV-OPT-10

1. **Вивантаження блоків без алокації координат:** `PathWorldSnapshot.getBlockState(int x,int y,int z)` читає immutable палітру за примітивними координатами, з ТОЧНО тими самими правилами `BEDROCK` для незавантажених чанків/виходу за висоту і `AIR` для порожніх секцій. `getBlockState(BlockPos)` тепер делегує примітивному методу.
2. `ThreadSafeUtilities.getBlockState()` і `getBlock()` використовують цей fast path **тільки** для `PathWorldSnapshot`. Живі світи `ServerLevel/ClientLevel` і всі інші `BlockGetter` залишають старий захист `validateCoords` і стандартну семантику.
3. **Скан драбин без тисяч короткоживучих об'єктів:** `nearbyLadderOnVerticalRoute` використовує один локальний `BlockPos.MutableBlockPos` та заздалегідь обчислені висоти. Кількість перевірених блоків, порядок і bounding area НЕ змінені. Додано `ladder_scans`, `ladder_probes`, `ladder_hits`, `ladder_scan_avg_ms`.
4. **Fairness при великих маршрутах:** перевірка площі `>256` чанків відбувається **до** `synchronized(LAST_CAPTURE)` і `LAST_CAPTURE.put`. Такий запит відхиляється, але вже НЕ займає квоту snapshot на тик. Метрики розділено на `snapshot_area_rejected`, `snapshot_tick_budget_deferred`, `snapshot_section_rejected` (базовий `deferred` збережений).
5. **Монотонна діагностика за реальний час:** новий незалежний від Minecraft `NavigationRateWindow` бере різницю накопичених лічильників між двома викликами `NavigationMetrics.report()` та ділить на фактичну різницю `System.nanoTime`. Додає до `NAV-OPT-STATS` поля `window_scope=process window_wall_s jps_s astar_s snapshots_s sections_s cache_hit_pct cache_invalidated_s failed_retry_deferred_s cache_replay_deferred_s inflight_joined_s queue_wait_window_avg_ms ladder_scans_s ladder_probes_s`. Перший виклик показує `window_state=warmup`; якщо минула менш ніж 1 с, `window_state=pending`. Метрики **глобальні для процесу** (всі dimensions), не per-village, FPS чи TPS. Нульові дельти і reset обробляються без негативних rates.
6. Додано `tools/tests/NavigationRateWindowSmoke.java`, який перевіряє wall-clock deltas, avg queue wait за інтервал, cache-hit %, reset та короткий інтервал. `tools/tests/test_navigation_optimization_wiring.py` тепер містить **17** структурних тестів: fast snapshot path, mutable ladder probe, rate integration і те, що oversized area перевіряється перед резервуванням квоти.

**Коміти NAV-OPT-10:**
- `1231b3e` — `NavigationRateWindow.java`;
- `91e2ba6` — інтеграція window та ladder metrics;
- `083a70b` — менші алокації при ladder scan;
- `0a93705` — primitive snapshot lookup;
- `da84e11` — fast path із `ThreadSafeUtilities`;
- `5b04f6e` — Java smoke для rate window;
- `e13980b` — три source-level регресії;
- `3fbb894` — виправлення квоти snapshot;
- `b644286` — деталізація відхилень snapshot;
- `99c4113` — source regression на порядок перевірки площі та квоти.

### 13.3. Статус тестування (принципово розділяти)

**Перевірено зіставлення тексту вихідних файлів після запису в GitHub** (перевірка наявності методів, маркерів, збереження викликів); **НЕ** виконано повної Forge-компіляції, Java smoke та Minecraft playtest для NAV-OPT-10. Оскільки користувач просить один об'єднаний цикл тестування пізніше, наразі результати smoke тестів позначати як **NOT RUN**, а не `PASS`. Фізична механіка дверей, драбин, ступенів, `ENDPOINT_MISMATCH` та HPA* лишаються поза цією фазою.

### 13.4. Єдиний майбутній набір перевірок (після завершення пакета оптимізацій)

Запускати з **кореневої** папки `Millenaire_mod_updating` у PowerShell:

```powershell
.\Update-Millenaire.bat
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_optimization_wiring.py
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_priority.py
cd .\millenaire-1.18.2-forge
javac --release 17 -d build/nav-window-smoke src/main/java/org/millenaire/common/pathing/NavigationRateWindow.java tools/tests/NavigationRateWindowSmoke.java
java -cp build/nav-window-smoke NavigationRateWindowSmoke
javac --release 17 -d build/nav-request-smoke src/main/java/org/millenaire/common/pathing/NavigationRequestKey.java tools/tests/NavigationRequestKeySmoke.java
java -cp build/nav-request-smoke NavigationRequestKeySmoke
javac --release 17 -d build/nav-executor-smoke src/main/java/org/millenaire/common/pathing/atomicstryker/PathSearchExecutor.java tools/tests/PathSearchExecutorSmoke.java
java -cp build/nav-executor-smoke PathSearchExecutorSmoke
javac --release 17 -d build/route-cache-smoke src/main/java/org/millenaire/common/pathing/RouteMemory.java tools/tests/RouteMemorySmoke.java
java -cp build/route-cache-smoke RouteMemorySmoke
```

Очікувано `17` Python wiring тестів, `9` navigation priority тестів + чотири Java smoke `PASS`, але **це очікування, не виміряний результат**. Для Gradle обов'язково зафіксувати `BUILD SUCCESSFUL` і шлях встановленого JAR.

Ігрова сесія: ті самі поселення/радіус симуляції/чисельність NPC, 5–10 хв; `logpathing=minor`; збирати `NAV-OPT-STATS` щонайменше за 3 звітні інтервали. Зіставляти `jps_s`, `astar_s`, `snapshots_s`, `sections_s`, `ladder_scans_s`, `cache_hit_pct`, `inflight_joined_s`, queue delays і фактичні паузи TPS. Перший `window_state=warmup` не є виміром. Після руху NPC порівняти з baseline; не змішувати різні сесії як строгий бенчмарк.

### 13.5. Пріоритети наступної оптимізації

1. Після об'єднаних тестів визначити, де витрати: queue wait, snapshot copy, JPS expansions або ladder scans.
2. Кешування між тіками потребує версій секцій та інвалідації **при кожній зміні блоку**; не додавати blind TTL, який може створити небезпечні маршрути.
3. Пріоритетна черга та HPA* — велика архітектурна зміна; без вимірів і тестового harness ризикує погіршити starvation, correctness та memory.
4. Геометричні проблеми дверей/драбин — відкладені на більш потужну модель за явним рішенням користувача, не змінені тут.



## 14. NAV-OPT-11 — спільні секції PathWorldSnapshot у межах одного тіку

**Дата:** 23.11.2026. **Поточна вимога користувача:** продовжувати оптимізацію snapshot, при цьому всі ігрові/Gradle тести NAV-OPT-09/10/11 провести **разом пізніше**. Проблеми дверей, сходів, фізичного спуску по драбині та tolerance-асиметрії — окремо для наступної потужнішої моделі. Навігаційна геометрія не змінювалася.

### 14.1. Критично важливе рішення: кеш між тіками НЕ вмикати без повного інвалідатора

Розглянуто доступні Forge 1.18.2 `net.minecraftforge.event.world.BlockEvent`. Є події для розміщення/зламу, fluid placement, piston etc., але **не підтверджено** повний універсальний hook на будь-яку зміну `LevelChunkSection.setBlockState` (рідинні, redstone, природні та сторонні модові мутації можуть оминати окремі BlockEvent). У vanilla `PalettedContainer` може змінювати внутрішній вміст **in-place**, тому перевірки об'єктної тотожності `getStates()==previousSource` **недостатньо** як доказ незмінності блоку.

**Висновок:** міжтікове повторне використання копій із time-to-live або лише Forge block events потенційно поверне застарілі колізії й непрохідні маршрути. Це рішення навмисно **не реалізоване** в NAV-OPT-11. Для майбутньої моделі: дослідити всеохоплююче версіонування секцій на рівні низькорівневих мутацій Minecraft/Forge 40.3.0 (можливо окремий патч до chunk section чи гарантований інтеграційний callback), розрізняти unload/reload, dimension, build height, synchronized snapshot leases і тестувати усі типи block changes. Не підміняти точність евристикою «чанк dirty» або одним event.

### 14.2. Підтверджений структурний недолік старого snapshot кешу

Раніше `PathWorldSnapshot.capture()` обслуговував **лише один повний snapshot на world+tick**. Якщо область нового запиту повністю містилася у прямокутнику попереднього знімка — вже були `snapshot_reused` і повторне використання всього snapshot. Але для двох **частково перекривних чи різних** областей другий запит відкладався, навіть коли необхідні секції здебільшого вже були скопійовані. Це обмежувало паралельність пошуків NPC і створювало зайві повторні звернення в наступних тіках.

### 14.3. Реалізовано: bounded per-tick sharing

1. `src/main/java/org/millenaire/common/pathing/SnapshotSectionPool.java` — Java-клас без залежності від Forge. Кеш за ключем секції (`SectionPos`) містить **вже відокремлену копію палітри** і тотожність вихідного `PalettedContainer`. При зміні `gameTime` повністю очищає посилання й лічильники. При зміні вихідного об'єкта палітри в тому самому тіку видаляє відповідний запис. ВАЖЛИВО: тотожність не виявляє *in-place* мутації, тому кеш **ніколи не переживає тік**.
2. `PathWorldSnapshot.capture()` у межах одного серверного тіку може створити **до 4 окремих snapshot** на світ, за умови **не більш ніж 256 сумарних chunk visits** і **не більш ніж 1024 нових palette copies**. Запити, які цілком входять в останній знімок, як і раніше можуть отримати той самий об'єкт без додаткового копіювання; незалежні/частково перекривні області збираються з раніше скопійованих секцій плюс нові копії в межах бюджету. Якщо ліміт досягнуто — повертається відкладення, а не необмежена алокація.
3. При створенні нового `PathWorldSnapshot` передаються **вже відокремлені копії**, і конструктор **більше не повторює `palette.copy()`**. Мапа й множина чанків лишаються незмінними (`Map.copyOf`, `Set.copyOf`). Робочі потоки A*/JPS лише читають копії, ніколи не звертаються до живого чанка і не змінюють палітру.
4. `NavigationMetrics`: нові накопичувальні поля `snapshot_shared_sections` — використання скопійованих раніше секцій, `snapshot_additional_captures` — другі/наступні завершені snapshot протягом того ж тіку, `snapshot_section_reuse_pct` — відсоток повторно використаних серед усіх копій і використань. `copied_sections` тепер є **реальною кількістю нових `palette.copy()`**, а не просто загальним числом секцій у кожному створеному знімку. Ці величини глобальні накопичувальні, без гарантії позитивного TPS.
5. `tools/tests/SnapshotSectionPoolSmoke.java` — standalone Java 17 test на часткове перекриття, повторне використання, жорсткі межі секцій/кількості запитів/chunk visits, source identity, очищення на зміні тіку, незалежні пули для різних світів та fail-closed режим.
6. `tools/tests/test_navigation_optimization_wiring.py`: додано два структурних тести на обмеження pool, одноразове копіювання й делегування конструктора; після NAV-OPT-11 має бути **19** source-level тестів.

**Важлива межа безпеки:** протягом **одного** тіку vanilla/інші моди можуть змінювати `PalettedContainer` *in-place* між двома знімками. Навіть per-tick cache може тоді використовувати застарілий вміст до кінця тіку — такий ризик був присутній і в попередньому superset-reuse. Серверна перевірка кожного ребра в `MillVillager.applyPathCalculatedSinceLastTick()` перед фізичним використанням маршруту відхиляє невалідний шлях і переходить до fallback. Але такий кеш **може марно витратити CPU на застарілий пошук і не знайти новий коротший маршрут**. Під час майбутнього глибшого аудиту це теж треба врахувати. Жодного обіцяння абсолютної актуальності знімків без надійної інвалідації.

**Коміти NAV-OPT-11:**
- `a4e0815` — bounded `SnapshotSectionPool`;
- `0991190` — інтеграція до `PathWorldSnapshot`;
- `7c15c4a` — метрики додаткових знімків і reused sections;
- `814784f` — Java 17 `SnapshotSectionPoolSmoke`;
- `638f0cb` — source-level guards, 19 тестів;
- `a6c822b` — відсоток фактичного повторного використання секцій.

### 14.4. Спільне тестування, коли завершимо пакет оптимізації

Виконувати з кореня `Millenaire_mod_updating`:
```powershell
.\Update-Millenaire.bat
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_optimization_wiring.py
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_priority.py
cd .\millenaire-1.18.2-forge
javac --release 17 -d build/nav-snapshot-pool-smoke src/main/java/org/millenaire/common/pathing/SnapshotSectionPool.java tools/tests/SnapshotSectionPoolSmoke.java
java -cp build/nav-snapshot-pool-smoke SnapshotSectionPoolSmoke
```

Також виконати **всі** Java smoke попередніх етапів (розділ 13.4). `BUILD SUCCESSFUL` та Java `PASS` для NAV-OPT-11 **ще не підтверджені**: ці тести готові, але на прохання користувача не запускаються окремим циклом зараз. У грі: кілька NPC одночасно йдуть до різних частково перекривних областей поселення. Дивитися `snapshot_shared_sections`, `snapshot_additional_captures`, `snapshot_section_reuse_pct`, `snapshot_tick_budget_deferred`, `snapshots_s`, `sections_s` та `stale_routes`. Розрізняти коректність A*/JPS і реальну ефективність (TPS, CPU, GC).

### 14.5. Що залишено для наступної моделі

- **Міжтіковий versioned cache** з гарантованою інвалідацією кожної зміни блоку та unload/reload; поточний per-tick pool — лише архітектурний крок, не фінальна оптимізація.
- Можливість різних `world.tick` і `getGameTime()` / re-enter same tick після block mutation, необхідність обмеження stale-capture.
- Окремо залишилися двері, спуск/вихід із драбини, колізії й `ENDPOINT_MISMATCH`; користувач призначив їх іншій моделі.
- Не зливати в `main` і не називати оптимізацію доведеною до повного Forge build, Java smoke та реальних замірів.



## 15. NAV-OPT-12 — сигнали версій та інвалідація на відомі зміни блоків

**Дата:** 23.11.2026. **Запит користувача:** продовжувати оптимізацію `PathWorldSnapshot`, без проміжного запуску Minecraft; тестувати всі етапи NAV-OPT-09/10/11/12 спільно. Користувач передасть окремий аудит геометрії дверей/драбин більш потужній моделі.

### 15.1. Перевірені обмеження Forge 1.18.2

У Forge 40.x є `LevelChunkSection.setBlockState(int,int,int,BlockState)`, який може змінювати `PalettedContainer<BlockState>` без заміни його Java-об'єкта. У `net.minecraftforge.event.world.BlockEvent` є конкретні події `BreakEvent`, `EntityPlaceEvent`, `FluidPlaceBlockEvent`, `CropGrowEvent.Post`, `PistonEvent.Post`; існують також `ChunkEvent.Load/Unload` і `WorldEvent.Unload`. Але **жодне джерело не підтверджує, що вони всі разом є універсальним post-mutation хукoм для будь-якого `LevelChunkSection.setBlockState`**.

Джерела API:
- [Forge 1.18.2 LevelChunkSection#setBlockState](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/level/chunk/LevelChunkSection.html)
- [Forge 1.18.2 BlockEvent](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraftforge/event/world/BlockEvent.html)
- [Forge 1.18.2 ChunkEvent](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraftforge/event/world/ChunkEvent.html)

**Інженерне рішення:** не дозволяти кешування секцій між різними серверними тіками на підставі цих подій. Інкрементальні «версії» поки **лише недостовірні hints**. Без універсального mutation hook не можна вважати відсутність отриманих подій доказом незмінності. Залишити `SnapshotSectionPool.resetIfNewTick` як обов'язковий fail-closed механізм.

### 15.2. Реалізовано NAV-OPT-12: найкраща доступна інвалідація всередині тіку

1. `SnapshotSectionPool.java`:
    - `invalidateSection(tick, sectionKey)` видаляє вже скопійовану секцію;
    - `invalidateAll(tick)` очищає всі кешовані секції цього світу;
    - ОБИДВА методи залишають поточний бюджет `snapshots`, `chunkVisits`, `copies` **без повернення витрачених квот**, щоб масова зміна блоків не могла викликати необмежену повторну алокацію;
    - публічні методи пулу синхронізовано, оскільки деякі Forge-події, особливо chunk lifecycle, можуть викликатися поза очевидним серверним tick-кодом.
2. `PathWorldSnapshot.java`:
    - `onKnownBlockMutation(ServerLevel,BlockPos)` інвалідує **одну** секцію (`SectionPos`) та інвалідує weakref до останнього повного snapshot **лише якщо змінений чанк всередині його прямокутника**;
    - `onKnownWorldMutation(ServerLevel)` інвалідує весь cached snapshot і пул при багатоблоковій зміні чи lifecycle чанка;
    - `onWorldUnload(ServerLevel)` одразу прибирає pooled палетні копії і `LAST_CAPTURE` для світу;
    - нові snapshot після інвалідації проходять звичайний `pool.begin()`, тобто **система не обходить старі ліміти**.
3. `MillEventController.java` (цей клас **уже зареєстрований** у `Mill.Mill()` через `MinecraftForge.EVENT_BUS.register(new MillEventController())`):
    - `BlockEvent.EntityPlaceEvent` — одинична секція; для `EntityMultiPlaceEvent` очищення всього світу;
    - `BlockEvent.BreakEvent`, `FluidPlaceBlockEvent`, `CropGrowEvent.Post` — одинична секція;
    - `PistonEvent.Post` — всі секції, бо рух поршня може змінити кілька чанків;
    - `ChunkEvent.Load/Unload` — все поточне кешування світу;
    - `WorldEvent.Unload` — забрати обидва кеші світу.
4. `NavigationMetrics` тепер має `snapshot_known_sections_invalidated` і `snapshot_known_captures_invalidated` (накопичені за процес).
5. `SnapshotSectionPoolSmoke.java` — доповнений smoke-тест: точкова інвалідація, збереження інших секцій, повне очищення без скидання copy/CPU бюджетів та новий тик.
6. `test_navigation_optimization_wiring.py` — два додаткові structural tests: інвалідація й квоти та наявність зареєстрованих Forge-подій. Після NAV-OPT-12 очікується **21** тест.

**Коміти в робочій гілці:**
- `d237455` — інвалідація пулу без refund ресурсів;
- `4b4120e` — точкова/world invalidation в PathWorldSnapshot;
- `615f531` — метрики;
- `13649de` — thread-safe пул;
- `6e97bb9` — Forge event integration;
- `06db9bf` — оновлення Java smoke;
- `23f8dd0` — Python wiring, 21 tests.

### 15.3. Конкретні межі гарантій

- **Не універсально:** відсутні complete callbacks для кожного in-place `PalettedContainer` edit, сторонні моди можуть змінювати блоки без цих подій. In-flight snapshot вже зібраний до події лишається старою read-only копією, але поточна `MillVillager.applyPathCalculatedSinceLastTick` має фінальну live-world перевірку вузлів і спрямованих ребер.
- `BlockEvent.BreakEvent` спрацьовує перед остаточним зламом і може бути скасований. Іноді інвалідація буде **зайвою**, але не дозволить повторно використати відому застарілу копію. Порядок слухачів та сторонні модові зміни не дають абсолютної гарантії.
- `snapshot_known_sections_invalidated` і `snapshot_known_captures_invalidated` показують **тільки** інвалідації, спричинені обробленими відомими подіями; не означають «всі зміни світу» і не доводять неможливості stale snapshots.
- Для міжтікового кешу потрібен гарантований mutation counter у `LevelChunkSection` або `PalettedContainer` (із безпечним version-check на server thread), окремі epoch при chunk unload/reload, тестування in-place mutation, piston, redstone, fluids і сторонніх модів, а також реальне вимірювання ціни нових hook-викликів.

### 15.4. Наступна спільна перевірка (пізніше, не зараз)

Після `Update-Millenaire.bat` (обов'язково `BUILD SUCCESSFUL`) з кореня репозиторію:

```powershell
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_optimization_wiring.py
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_priority.py
cd .\millenaire-1.18.2-forge
javac --release 17 -d build/nav-snapshot-pool-smoke src/main/java/org/millenaire/common/pathing/SnapshotSectionPool.java tools/tests/SnapshotSectionPoolSmoke.java
java -cp build/nav-snapshot-pool-smoke SnapshotSectionPoolSmoke
```

Також виконати всі чотири Java smoke з розділу 13.4. У Minecraft окремо поставити/зламати блок, активувати поршні, вилити воду та провести декілька NPC через перекривні області, перевіряючи `snapshot_shared_sections`, `snapshot_known_sections_invalidated`, `snapshot_known_captures_invalidated`, `snapshot_tick_budget_deferred`, `stale_routes` та частоту `snapshots_s`.

**Статус:** GitHub source-level checks — очікуються наприкінці цього етапу; повна Forge compile, Java smoke, Minecraft лог NAV-OPT-12 — **NOT RUN** за рішенням користувача. `main` не чіпати; не обіцяти виміряне прискорення до A/B benchmarks.



## 16. NAV-OPT-13 — усунення re-cache після PRE-mutation Forge events

**Дата:** 23.11.2026. **Контекст:** користувач попросив продовжувати оптимізацію snapshot, а компіляцію, Java/пітон тести й Minecraft playtest провести всі разом після завершення пакета змін. `main` не змінювати. Фізичні проблеми дверей/сходинок/драбин і reverse `ENDPOINT_MISMATCH` залишити для поглибленого аудиту іншої моделі.

### 16.1. Знайдено реальну логічну вразливість NAV-OPT-12

Саме лише `SnapshotSectionPool.invalidateSection(nowTick,key)` **видаляло** стару копію. Але Forge `BlockEvent.BreakEvent` (та низка інших подій) може відбутися **до** фактичної мутації блоку. Послідовність:

1. Існує знімок / або його ще немає.
2. Приходить PRE-change повідомлення; `invalidateSection` видаляє поточну копію, або нічого не знаходить, якщо кеш ще пустий.
3. До запису нового `BlockState` інший виклик `PathWorldSnapshot.capture` копіює **старий** `PalettedContainer`.
4. Після зміни блоку другий пошук за той самий серверний tick міг отримати з `pool.get` застарілу копію — **event source check через тотожність object reference цього не виявить**, бо палітра мутує in-place.
5. Раніше цілий `Capture` також міг бути повторно використаний як superset, хоча PRE-event уже трапився до його створення.

Таким чином NAV-OPT-12 інвалідація була **недостатньою**, особливо при подіях до першого snapshot. Це не статистична гіпотеза, а детермінована помилка протоколу **відстеження відомих змін**. Реальний ігровий репродьюсер ще не виконано; не стверджувати, що NPC вже фізично пройшов крізь блок.

### 16.2. NAV-OPT-13: реалізація bounded poison + ревізії

- `SnapshotSectionPool`: новий `poisonedSections: Set<Long>` і `poisonedWorld` **тільки в межах цього server game tick**. `invalidateSection` завжди позначає section key як poisoned, навіть якщо в кеші **ще не було** палітри; `invalidateAll` позначає весь pool poisoned. `get` НІКОЛИ не повертає poisoned секцію, `remember` рахує фактичну копію у `copies`, але **не кладе її назад у pool**, поки poison діє. Не-poisoned секції можуть продовжити працювати як раніше.
- Масові події обмежені пам'яттю: коли `poisonedSections.size() >= maxCopies` (поточний ліміт 1024), відбувається `poisonedWorld=true` і очищення множини, без необмеженого росту кількості ключів. Після нового тіку `resetIfNewTick` прибирає poison і старі посилання на source palettes, скидає лічильники й локальну `revision`.
- Для цілих snapshot введено `pool.mayReuseSnapshots(tick)`. Якщо **хоч одна** відома зміна сталася у світі протягом тіку, відключається повторне використання **всього раніше зібраного** `LAST_CAPTURE` в цьому тіку; нові знімки все одно допускаються через старий bounded `pool.begin`. Це свідомо консервативно — відома зміна в іншому чанку також блокує full-snapshot reuse цього світу до кінця тіку.
- `PathWorldSnapshot.onKnownBlockMutation` і `onKnownWorldMutation` тепер використовують `SECTION_POOLS.computeIfAbsent` **навіть до першого `capture`**, щоб PRE-change подія не зникла непоміченою.
- `SnapshotSectionPool.revision(tick)` збільшується на кожне повідомлення про зміну. `PathWorldSnapshot.capture` записує `captureRevision` **після резервування квоти** і **перед копіюванням**. Перед публікацією знімка під `synchronized(LAST_CAPTURE)` перевіряє, що `pool.revision(tick) == captureRevision`. Якщо подія прийшла під час копіювання — `NavigationMetrics.snapshotMutationRaceRejected()` + `deferred()`, повертає `null` замість підозрілої копії. Це не відшкодовує витрачені квоти, щоб запобігти CPU stampede.
- `NavigationMetrics` додає накопичені `snapshot_poisoned_bypasses` (скопійовано замість кеш-хіта через poison) і `snapshot_mutation_race_rejected` (відхилено знімок, зібраний через відому подію). Якщо обидва нульові, це не доводить, що світ не змінювався.
- `tools/tests/SnapshotSectionPoolSmoke.java` додано standalone Java кейси: PRE-event **до first capture**; not-refund of copy quota; blocking re-cache old state, revision change during copy; global poison після piston/chunk; tick reset; bounded poison-keys overflow. **Не виконані ще.**
- `tools/tests/test_navigation_optimization_wiring.py` додано два source-level tests: перевіряють pre-event pool initialization, poison use, порядок дозволу superset reuse, зміну ревізії і відхилення до `new WeakReference<>(result)`. Має бути **23** Python wiring-тести (очікувано, не результат запуску).

**Нові коміти:**
- `b7c3b26` — poison protocol and revision in `SnapshotSectionPool`;
- `8ff2937` — event-first poison and revision guard у `PathWorldSnapshot`;
- `e466a54` — діагностика poison/race;
- `b09690b` — нові Java smoke tests;
- `c2cb5dc` — два нові wiring guards.

### 16.3. Чого NAV-OPT-13 НЕ гарантує

1. **Це НЕ універсальний revision counter для всіх `setBlockState`**. Якщо мутують палітру через API, що не генерує жодну перехоплену Forge-подію, poison не ввімкнеться. Тому **кеш між тіками лишається вимкненим**, як і раніше.
2. PRE-event попереджає про зміну, але не каже точно, коли вона завершилася. У межах тіку `PathWorldSnapshot.capture` все ще може зробити **власну нову копію до фактичного оновлення блоку**; цей snapshot повертається *тільки* одному worker, а не записується у shared poisoned pool. Завдяки live-world directed-edge validation у `MillVillager.applyPathCalculatedSinceLastTick` готовий некоректний маршрут має відхилятися; однак сам пошук міг витратити CPU на неактуальні дані.
3. Між `captureRevision` та перевіркою на публікації може бути невідома без-event мутація — без нижчого hook не детектується. Навіть відома мутація **після** повернення snapshot не змінює вже detached palette. Фінальна перевірка маршруту необхідна.
4. Event callback може викликатися з іншого потоку. Синхронізація `LAST_CAPTURE` + усіх публічних методів пулу і `revision` зменшує ризик гонок, але повна сумісність Forge event lifecycle (особливо chunk loading) вимагає Gradle+ігрового тесту.
5. Безпечна правильність важливіша за hit rate. При активній забудові `snapshot_poisoned_bypasses` може зрости, а `snapshot_section_reuse_pct` впасти — **очікувана** ціна тимчасового захисту.

### 16.4. Майбутній спільний тест

Із кореня `Millenaire_mod_updating`, після `Update-Millenaire.bat` і підтвердженого `BUILD SUCCESSFUL`:

```powershell
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_optimization_wiring.py
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_priority.py
cd .\millenaire-1.18.2-forge
javac --release 17 -d build/nav-snapshot-pool-smoke src/main/java/org/millenaire/common/pathing/SnapshotSectionPool.java tools/tests/SnapshotSectionPoolSmoke.java
java -cp build/nav-snapshot-pool-smoke SnapshotSectionPoolSmoke
```

Виконати й інші smoke-тести NAV-OPT-09/10 та `RouteMemorySmoke` із розділу 13.4. Ігровий сценарій: кілька NPC; **до/під час** обчислення маршруту зламати/поставити блок, активувати поршень, оновити рідину; перевіряти `snapshot_poisoned_bypasses`, `snapshot_known_sections_invalidated`, `snapshot_mutation_race_rejected`, `stale_routes`, `NAV-OPT-STATS`, `DOOR-BLOCKED`, `NAV-TELEPORT`. Критично: відсутність крашів і відхилення реально застарілих шляхів. Усі нові Java/Python/Gradle/Minecraft тести цього етапу поки **NOT RUN**, якщо користувач не повідомить результат.

### 16.5. Наступний напрямок (перед фінальним тестуванням)

Для справжнього cross-tick versioned cache треба інтеграція з низькорівневим `LevelChunkSection.setBlockState` / `PalettedContainer` або універсальний mutation listener **після** всіх типів змін (vanilla, fluids, redstone, third-party mods) плюс chunk unload/reload epochs. Не додавати Mixin або ASM навмання без гарантії сумісності Forge 40.3.0 й без перевірки bytecode/діапазонів версій. Альтернативна безпечна оптимізація — **менше копіювати протягом тіку, не виходячи за його межі**, або профілювати найбільші джерела CPU. Геометричні проблеми відкладені на іншу модель за окремим рішенням користувача.



## 17. NAV-OPT-14 — повернення до будь-якого придатного same-tick snapshot

**Дата:** 23.11.2026. **Вимога користувача:** продовжити оптимізації без проміжного Minecraft/Forge тесту; після завершення пакета протестувати все разом. Працювати **тільки** у `fix/villager-door-access-and-village-territory`; `main` не змінювати.

### 17.1. Місце втрати продуктивності в NAV-OPT-11/13

До NAV-OPT-14 `PathWorldSnapshot` мав лише **один** `LAST_CAPTURE` на ServerLevel. Знімок A, потім інший B у тому самому тіку — B заміняв слабке посилання на A. Навіть якщо наступний запит до області A повністю вміщався в ще існуючий A, система пробувала заново збирати snapshot, перевіряти до 256 чанків і облік секцій. `SnapshotSectionPool` зменшував кількість реальних `PalettedContainer.copy()`, проте **не** усував повністю повторний обхід чанків, створення HashMap/HashSet та об'єкта `PathWorldSnapshot`. Повністю reused `LAST_CAPTURE` був доступний тільки якщо попередній *останній* snapshot містив новий прямокутник.

### 17.2. Реалізація

1. Додано **`SnapshotCaptureIndex<T>`** — автономний Java 17 клас із жорстким лімітом entries. Кожен запис містить прямокутник чанків і `WeakReference<T>`; значення і навіть WeakReference не мають права затримувати ServerLevel. Індекс із **4** записів відповідає ліміту `MAX_SNAPSHOTS_PER_TICK = 4`; при перевищенні ємності видаляється найстаріший запис, при `gameTime` іншого тіку **всі записи очищаються**. Вимерлі weak refs відкидаються під час вставлення. Для запиту перебираються максимум 4 записи; повертається **найменший живий snapshot**, який повністю покриває прямокутник `minX..maxX,minZ..maxZ`, без злиття частково перекривних знімків. Запис `Match<T>` вказує `nonLatest`, якщо вибрано не найновіший запис.
2. `PathWorldSnapshot` замість одного `Map<ServerLevel, Capture> LAST_CAPTURE` має `Map<ServerLevel, SnapshotCaptureIndex<PathWorldSnapshot>> CAPTURE_INDICES` та єдиний `SNAPSHOT_LOCK`, під яким працюють і `CAPTURE_INDICES`, і `SECTION_POOLS`. Це зберігає атомарну перевірку ledger `pool.mayReuseSnapshots(tick)`, читання індексу і резервування `pool.begin(tick,chunkVisits)`. Cache hit відбувається **до** дорогого обходу чанків та до витрачання квоти на новий snapshot. При створенні нового snapshot збережено `captureRevision` і відмову при зміні відомої ревізії в процесі копіювання; новий знімок додається в index лише якщо `pool.mayReuseSnapshots(tick)` досі дозволяє reuse.
3. Відомі Forge-мутації `onKnownBlockMutation` і `onKnownWorldMutation` **консервативно інвалідують увесь index цього світу**, не лише останній snapshot. Ті самі обробники лишають poison у `SnapshotSectionPool`, тому жодні вже побудовані superset snapshots не будуть повторно використані до кінця тіку; quota `snapshots/chunkVisits/copies` **не скидаються**. `onWorldUnload` прибирає обидва пер-світові кеші.
4. `SnapshotSectionPool.usedSnapshots()` дає точніший облік додаткових *допущених* спроб snapshot, ніж колишній bool `previous.tick()==tick`. Успішний додатковий snapshot збільшує `snapshot_additional_captures`. `snapshot_known_captures_invalidated` після NAV-OPT-14 означає **кількість живих слабко-доступних повних snapshot, вилучених із index після відомої мутації**, а не число всіх сповіщень або null placeholders. Запроваджено новий лічильник `snapshot_non_latest_reused`: він є **підмножиною** `snapshot_reused`, коли було вибрано знімок, який не є останнім у bounded index. `snapshot_shared_sections` рахує повторне використання конкретних копій секцій в іншому *новому* snapshot; це **інша категорія**, не тотожна full-snapshot reuse.
5. Збережені межі NAV-OPT-11: не більше **4** нових повних snapshot, **256** відвідувань чанків, **1024** palette copies на світ у тіку. Збережені заборони NAV-OPT-13: **ніяких** full-snapshot hits при `poisonedWorld` або непорожній `poisonedSections`, невдалий `revision` відкидає результат до публікації, невідомі зміни блоку можуть бути непомічені, отже **кешування між тіками все ще ВИМКНЕНЕ**.

### 17.3. Тести підготовлено (НЕ запускалися)

- `tools/tests/SnapshotCaptureIndexSmoke.java` — deterministic Java 17 кейси: A→B→A, найменший супермножинний прямокутник, часткове перекриття НЕ допускається, weak index capacity та oldest eviction, invalidation, новий/повернутий tick, некоректні межі, нульова ємність.
- `tools/tests/SnapshotReuseProtocolSmoke.java` — інтеграційні plain Java 17 кейси `SnapshotCaptureIndex` + `SnapshotSectionPool`: два одночасно збережені знімки, PRE-change `invalidateSection` + `index.invalidate`, заборона повторного видавання старого snapshot/секції, збереження budget, позначення секції poisoned і reset на новому тіку.
- `tools/tests/test_navigation_optimization_wiring.py` — адаптовано 4 старі source assertions до нового `CAPTURE_INDICES`/`SNAPSHOT_LOCK`, додано 2 регресійні тести, **всього 25** structural tests.
- GitHub джерела перечитуються через connector, інваріанти перевіряються статично, але `javac`, `python`, `Gradle`, Minecraft playtest NAV-OPT-14 **NOT RUN** до спільної перевірки. Наявність `Smoke.java` — не доказ PASS.

**Коміти NAV-OPT-14:**
- `6a3eb26` — `SnapshotCaptureIndex.java` з bounded index;
- `8f597d8` — доступ до кількості допущених snapshot у `SnapshotSectionPool`;
- `c0e0053` — повна інтеграція індексу в `PathWorldSnapshot`;
- `a2c3361` — безпечне 64-бітове порівняння площ;
- `377d3c8` — нова метрика `snapshot_non_latest_reused`;
- `3593c38` — Java 17 standalone test for index;
- `e5e554e` — зміни/доповнення source-level тестів до 25;
- `9cdf187` — Java 17 cross-component smoke for index + pool.

### 17.4. Єдина перевірка всіх NAV-OPT етапів

У корені `Millenaire_mod_updating`:
```powershell
.\Update-Millenaire.bat
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_optimization_wiring.py
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_priority.py
cd .\millenaire-1.18.2-forge

javac --release 17 -d build/nav-capture-index-smoke src/main/java/org/millenaire/common/pathing/SnapshotCaptureIndex.java tools/tests/SnapshotCaptureIndexSmoke.java
java -cp build/nav-capture-index-smoke SnapshotCaptureIndexSmoke
javac --release 17 -d build/nav-snapshot-protocol-smoke src/main/java/org/millenaire/common/pathing/SnapshotSectionPool.java src/main/java/org/millenaire/common/pathing/SnapshotCaptureIndex.java tools/tests/SnapshotReuseProtocolSmoke.java
java -cp build/nav-snapshot-protocol-smoke SnapshotReuseProtocolSmoke
```

Також виконати Java smoke `SnapshotSectionPoolSmoke`, `RouteMemorySmoke`, `NavigationRequestKeySmoke`, `PathSearchExecutorSmoke`, `NavigationRateWindowSmoke` з попередніх розділів 13–16. Перевірити `BUILD SUCCESSFUL`, 25 Python wiring `OK`, 9 priority `OK`, 7 Java smoke `PASS` (ці результати **очікуються**, не підтверджені). У Minecraft забезпечити сценарій NPC A→B→A в одному світі, зіставити `snapshot_non_latest_reused`, `snapshot_reused`, `snapshot_additional_captures`, `snapshot_shared_sections`, `snapshot_known_captures_invalidated`, `snapshot_poisoned_bypasses`, `snapshot_mutation_race_rejected`, `stale_routes`, навантаження CPU/TPS/GC.

### 17.5. Ризики й завдання для сильнішої моделі

- Навіть у межах одного тіку **невідома без-event in-place зміна** не виявляється через object identity або poison. Індекс може повторно видати знімок A із ранньої частини тіку, який уже не відображає новий стан. Фінальна live-world перевірка ребер у `MillVillager` захищає від прийняття невірного маршруту, але пошук на застарілому snapshot може не знайти актуальний шлях або спожити CPU. Це потрібно оцінити окремим playtest при активному будівництві/руйнуванні.
- `WeakReference` дозволяє GC видалити раніші snapshot; `snapshot_non_latest_reused=0` навіть під навантаженням не доводить дефекту, якщо ранні snapshot вже звільнені або запити не перекриваються. Немає безумовного прискорення.
- Довгий snapshot build при async Forge event інвалідованому під час копіювання має відповідати `snapshot_mutation_race_rejected`. Аномально високі `snapshot_known_captures_invalidated` і `snapshot_tick_budget_deferred` можуть показати, що консервативна інвалідація надто агресивна.
- Складна геометрія дверей, сходинок, спуску по драбинах та reverse `ENDPOINT_MISMATCH` залишаються для іншої потужнішої моделі, **не** змінювалися в NAV-OPT-14.
- `main` не чіпати. Жодну нову реалізацію не називати Forge-compiled чи playtest-verified до отримання реального результату.



## 18. NAV-OPT-15 — кеш простих колонок і профілювання swept-колізій

**Дата:** 23.11.2026 (21:43+01:00). Користувач продовжує просити послідовну оптимізацію без проміжного ігрового тестування; усе перевірятиметься спільним запуском після завершення серії. Змінено тільки робочу гілку `fix/villager-door-access-and-village-territory`, НЕ `main`. Не розпочинати проблеми фізичної геометрії дверей/драбин/плит: вони передані для глибшого аудиту іншій моделі.

### 18.1. Виявлені витрати

1. `AStarStatic.isPassableBlock()` на тому самому `ix/iy/iz` раніше читав блок під ногами до **трьох разів** для `BarrierPathRules.blocksStepAbove`, `ChestPathRules.isChest`, `BlockItemUtilities.isFence` та стін/ґрат. Усього один `belowState` достатній для цих незмінних синхронних обчислень; умови перевірок не змінені.
2. `AStarStatic.isSimpleFlatColumn()` на кожному плоскому переході A*/JPS заново перевіряв ноги, голову й повну колізійну форму підлоги в тій самій координаті. Ці перевірки безпосередньо залежать від `BlockGetter`, але **не залежать** від окремої `AStarConfig` у цій функції. Тільки для **detached immutable PathWorldSnapshot** можна кешувати точний булевий результат (також `false`) на весь строк життя одного snapshot. У живому `Level` нічого не кешується.
3. `isBodyCollisionFree()` створював новий `BlockPos(bx,by,bz)` для кожного пробного вокселя, хоча `getCollisionShape`/перевірка `hasChunkAt` використовують координату негайно й не змінюють її поза ітерацією. Один локальний `MutableBlockPos` дає ті самі координати з меншими алокаціями.
4. `getAccessNodesSorted()` після `Collections.sort` багаторазово видаляв `resultList.remove(0)`; це могло робити копіювання елементів масиву для кожного вийнятого кандидата. `resultList.toArray(new AStarNode[0])` зберігає точний відсортований порядок за O(n), без серії зсувів.
5. Попередні метрики знали JPS/A* total time, але **не** показували кількість і середній час дорогих swept-shape перевірок, через що не можна було локалізувати вплив складних форм.

### 18.2. NAV-OPT-15 — внесені зміни

- **`SnapshotColumnMemo.java`**: маленька автономна Java 17 структура для `BlockPos.asLong(x,y,z)`. Фіксована ємність `2048` (power-of-two), `AtomicReferenceArray<Entry>` з direct-mapped slot. Порівняння **повного ключа** перед поверненням; колізія хешу може лише витіснити раніший кеш-хіт, але НІКОЛИ не поверне результат іншої колонки. Також кешує `false` із відмінністю між `Boolean.FALSE` та `null`. Підтримує одночасні потоки worker без locks, не накопичує необмежені записи.
- **`PathWorldSnapshot`**: кожен новий detached snapshot створює свій `private final SnapshotColumnMemo`, методи `getSimpleFlatColumn` / `rememberSimpleFlatColumn`. Кеш **не** розділений між різними snapshot, світами, server ticks або live Level. Навіть коли NAV-OPT-14 index повторно видає той самий snapshot, його кеш лишається коректним щодо його **власного** immutable world view. Для доступу використовується packed position `BlockPos.asLong`.
- **`AStarStatic.isSimpleFlatColumn()`**: перевірка memo only when `world instanceof PathWorldSnapshot`; cache-hit повертає оригінальний true/false. На miss виконується оригінальна перевірка порожніх ніг, голови й `Block.isShapeFullBlock(state.getCollisionShape(world,pos))`, але з одним локальним `MutableBlockPos`. Булевий результат зберігається лише в snapshot. **Самої геометричної формули НЕ змінено.**
- **`AStarStatic.isPassableBlock()`**: один `ThreadSafeUtilities.getBlockState(world,ix,iy-1,iz)` для нижнього блока; з нього отримуються ті самі barrier/chest/fence/iron-bars/wall результати. Всі конфігураційні дозволи води, дверей і листя збережені.
- **`AStarStatic.isBodyCollisionFree()`**: одна `BlockPos.MutableBlockPos` на один body-sampling pass. Збережено порядок обходу, точні `AABB` межі, перевірку на завантаженість чанка для live-world, doors/gates/leaves та `shape.toAabbs` з `body.intersects(part.move(bx,by,bz))`.
- **`AStarStatic.getAccessNodesSorted()`**: прибрано `remove(0)` після `Collections.sort`; повертається відсортований масив.
- **`NavigationMetrics`**: додано `flat_column_memo_hit` / `flat_column_memo_miss` тільки для snapshot колонок; для дорогого `TransitionClearanceRules.canTraverse` — `swept_checks`, `swept_blocked`, `swept_avg_ms`, `swept_max_ms`. Вимірювання `System.nanoTime()` **лише на swept-branch**, щоб не додавати таймер до гарячого успішного flat fast-path. Це накопичувальні метрики за весь процес і не підтверджують прискорення без control group.
- **`tools/tests/SnapshotColumnMemoSmoke.java`** — standalone Java 17: true/false, відсутній ключ, хеш-колізії, розділення двох snapshot, bounded capacity, одночасні worker threads із перенесенням винятків у main thread і заборона непотужності-двійки.
- **`tools/tests/test_navigation_optimization_wiring.py`** — три нових source-level тести: memo wiring/місткість, оригінальні collision shape і live Level guards, одна below-block load та telemetry; тепер **28** структурних тестів. Ці перевірки не запускаються замість Forge compiler.

**Коміти NAV-OPT-15:**
- `48edf8e` — створення `SnapshotColumnMemo`;
- `4dd7871` — локальний memo в `PathWorldSnapshot`;
- `bf21991` — оптимізації `AStarStatic`;
- `1366e0f` — swept/flat-column telemetry;
- `261e284` — Java memo smoke;
- `0c364cd` — wiring suite до 28;
- `aaac858` — фіксація невдалих assertions із worker threads;
- `33d2e5f` — явний `Boolean` у lookup.
- `b1b4ec8` — виправлено source-level guard на explicit `hit.position() != key` у `SnapshotColumnMemo.get`, щоб очікування тесту відповідало фактичному fail-closed коду.

### 18.3. Обмеження, ризики й метрики

- `SnapshotColumnMemo` **не можна** використовувати для `ServerLevel` чи загального `BlockGetter`: live-world змінюється між викликами. Навіть snapshot може бути застарілим щодо справжнього світу, тому `MillVillager.applyPathCalculatedSinceLastTick()` зобов'язаний фінально перевірити маршрут у live-world, як і раніше. Також залишається можливість, що async пошук не знайде нову прохідну область, якщо snapshot зроблено до зміни блоку.
- `SnapshotColumnMemo` кешує повний `isSimpleFlatColumn` boolean, який розрахований лише за `world.getBlockState(pos)` і `getCollisionShape(world,pos)`; він **не** переноситься на політику дверей чи ladder physics. Якщо shape залежить від іншого `BlockGetter`/часу в межах snapshot, потрібен додатковий аудит, але збережені detached palettes immutable.
- Direct-mapped replacement може давати miss на хеш-колізії. Це питання ефективності, **не** точності; 2048 slots = жорстка memory upper bound для кожного snapshot. Зі зростанням кількості живих snapshot (JPS worker queue) загальне використання пам'яті множиться на кількість одночасно утримуваних snapshot.
- Зміна `MutableBlockPos` не повинна змінювати позиції до завершення `BlockState.getCollisionShape`; тут позиція локальна, наступний `set` відбувається лише після повернення попередньої колізійної форми.
- `swept_avg_ms` вимірює лише **дорогий swept branch**, не вартість усіх transition/node validations. `flat_column_memo_hit` рахує запити, а не CPU економію. Аналізувати hit ratio та swept total time разом з `jps_s`, `astar_s`, `stale_routes`, TPS/GC.

### 18.4. Пакетний тест НАПРИКІНЦІ (не запускати окремо без рішення користувача)

З кореневої папки `Millenaire_mod_updating`:
```powershell
.\Update-Millenaire.bat
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_optimization_wiring.py
py -3 .\millenaire-1.18.2-forge\tools\tests\test_navigation_priority.py
cd .\millenaire-1.18.2-forge
javac --release 17 -d build/nav-column-smoke src/main/java/org/millenaire/common/pathing/SnapshotColumnMemo.java tools/tests/SnapshotColumnMemoSmoke.java
java -cp build/nav-column-smoke SnapshotColumnMemoSmoke
```

Також виконати всі Java smoke NAV-OPT-09/10/11/13/14 та `RouteMemorySmoke` з розділів 13–17: **всього 8 окремих Java smoke** після NAV-OPT-15 (уточнити інвентар, якщо додані нові). `28` Python wiring tests та `BUILD SUCCESSFUL` — **очікування, не факт**. Minecraft playtest: багато NPC із повторними маршрутами по рівній дорозі та на складній геометрії, виміряти `flat_column_memo_hit/miss` і `swept_checks/swept_avg_ms` за інтервали, порівняти затримки/колізії та TPS. Спочатку тестувати з backup світу.

**Статус на момент запису:** лише GitHub source-level consistency verification, НЕ `javac PASS`, НЕ Forge compiled, НЕ Minecraft verified. Не повідомляти користувачу або наступній моделі, що прискорення підтверджено без фактичного A/B тесту.

### 18.5. Наступні напрями оптимізації

1. За метриками `swept_checks` / `swept_avg_ms` з'ясувати, чи насправді bottleneck — shape sampling. Якщо так, дослідити **батчування однакових shape lookups у межах worker** без зміни допустимої геометрії; live-world direction/collision validation не кешувати між ticks.
2. Профілювання `AStarOpenSet`, hash map closedNodes, `AStarNode` allocation і потоку workers; не змінювати priority/order без regression.
3. Не вмикати міжтіковий versioned snapshot cache, доки низькорівневий mutation hook, unload/reload epoch, correctness tests і A/B performance не підтверджені.
4. Геометрія дверей/драбин/плит та `ENDPOINT_MISMATCH` — окремо для іншої потужнішої моделі; **не змінено**.

