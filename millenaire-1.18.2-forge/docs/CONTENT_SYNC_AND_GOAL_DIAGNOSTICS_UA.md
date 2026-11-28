# Універсальне оновлення контенту та діагностика цілей NPC — 24.11.2026

## Обсяг робіт

Користувач погодив реалізацію двох наступних напрямків: універсальне оновлення стандартних файлів моду (раніше існуючий каталог millenaire версії 8.1.2 міг ігнорувати новий контент JAR) та діагностика завдань NPC у режимі чітів. Не реалізовувати продовольчу економіку з backlog; не змінювати алгоритми руху, сходів, дверей, драбин чи виробничі квоти у межах цієї роботи.

## ContentBundleUpdater

Активний Forge startup: Mill → ContentDeployer.deployBundledContent → ContentBundleUpdater.update до MillConfigValues.initConfig. Замість переінсталяції всього каталогу при зміні версії та пропуску оновлень при збігу версії виконується звіряння SHA-256. Облік: mods/millenaire/.millenaire-stock-content.properties. Усі файли з bundled cultures, goals, villagerconfig, quests, help, languages і stock blocklist.txt, blocktypes.txt, itemlist.txt, localization-version.txt. Профілі, плани, PNG, рецепти та переклади підтримуються як байти. config.txt, config-server.txt, hof, світи, millenaire-custom не перезаписуються. Відсутні каталоги створюються початковим deploy. Два попередні адресні міграційні методи (Mayan sandstone, tavern furnace) замінено універсальним оновленням; LanguageBundleUpdater більше не викликається активним Forge startup. Legacy deployContent(File) залишається неактивним кодом; не викликати його замість deployBundledContent.

Політика:
- Перша синхронізація без попереднього manifest: стандартний вміст JAR є авторитетним (stock folder і раніше позначався як overwrite on update). Усі різні наявні стандартні файли копіюються у mods/millenaire-content-backups/stock-UUID/original, потім замінюються новими. Не можемо визначити, чи старий файл був локально редагованим без відомої бази; про це попередити користувача. Custom overrides не чіпаємо. Невідомі локальні додаткові файли зберігаються.
- Після першої синхронізації: unchanged stock оновлюється, local-only зміна зберігається, simultaneous upstream/local change зберігає local активним і записує original та incoming у backup. MILL-CONTENT-CONFLICT виводить конкретний шлях. Для цього конфлікту новий SHA записується як upstream baseline, щоб не створювати однакові резервні копії щоразу; конфлікт вирішити вручну.
- Нові/відсутні stock files встановлюються. Видалені upstream files видаляються лише коли попередній manifest підтверджує, що local дорівнює попередньому stock, та створено резервну копію. Видалений upstream локально змінений файл зберігається. Untracked local files не видаляються; за потреби окремо перевірити конфлікт дубльованих goal IDs/custom overrides. 
- File-by-file staged write + atomic move where supported; manifest записується останнім. Це не глобальна транзакція всього дерева; при аварії наступний запуск повторює reconciliation. Не виходити за managed roots; symlink components відхиляються. Невідома/небезпечна структура або I/O error зупиняє deploy, а не маскує пошкодження.
- MILL-CONTENT-SYNC: bootstrap=... Report[installed=..., updated=..., removed=..., conflicts=..., preserved=...] backup=... . Перша синхронізація може бути довшою за рахунок обчислення хешів; виміряти реальний startup overhead.

## Діагностика NPC

M → Налаштування → Діагностика AI жителів. Тільки permission level 2 (на сервері повторна перевірка), клієнт обирає жителя в радіусі 16 блоків, попередній/наступний і rescan. Екран не ставить гру на паузу, отримує актуальні серверні snapshots кожні 40 ticks. Запит не викликає isPossible/getDestination/priority. Watch діє 1200 ticks після останнього запиту; weak-key map, 96 кандидатів і максимум 4 notes на goal. Rate limit 10 ticks per operator, same level, distance ≤16, alive. Показуються поточний goal, destination, last observed destination і вік спостережень, остання natural selection, AVAILABLE/BLOCKED/NOT_EVALUATED, останній фактично порівняний priority (не фіксована оцінка; якщо пріоритет не порівнювався, він не показується). Старі відповіді для іншого entityId відкидаються. UI en/uk/ru, технічні reason codes англійською.

GoalDiagnostics підключено до MillVillager.setNextGoal, не додає викликів isPossible чи priority, не змінює порядок існуючих RNG calls, не обчислює цілі самостійно. Відкриття вікна може показувати WAITING_FOR_NEXT_NATURAL_GOAL_SELECTION — це нормально, якщо довгий goal ще виконується. Не форсувати скидання goal і не зупиняти NPC. Goal.setVillagerDest окремо записує результат природного визначення destination: NO_DESTINATION або NO_REGISTERED_MATCHING_LIVE_SOURCE для generic mining. Це не результат pathfinding і не доказ фізичної доступності.

Причини: day/night/hour, townhall/village (фактично TH stock у поточній реалізації) і building limits (available/limit), resource balance, simultaneous count, repeat delay, no workplace; додатково cooking input minimum, local balance, відсутність зареєстрованої печі/firepit, немає придатного slot/output; furnace maintenance, fishing stock cap/spot. Поки не всі специфічні умови кожної професії інструментовано: SPECIFIC_CONDITION_FALSE (not instrumented) / GENERIC_CONDITION_FALSE — чесний fallback, а не вигадана причина. Наявність AVAILABLE не означає, що маршрут знайдено. Наступна ітерація: розширювати reason codes за реальними спостереженнями, окремо від фізичної навігації.

Network protocol 1.18.2-5; request 208 / state 113, validated 4-byte request and bounded response. Оновити JAR на клієнті і сервері одночасно.

## Перевірки

Виконано standalone javac --release 17 для точного ContentBundleUpdater.java (git blob 93f5ed281ba26d89280239e01c702e564865d133), ContentBundleUpdaterSmoke: 11/11 PASS (legacy update, backup, settings/custom, idempotency, update/add, clean deletion with backup, local/untracked, conflict, repeated conflict, missing stock restore, logs). Додано tools/tests/ContentBundleUpdaterSmoke.java і test_goal_diagnostics_wiring.py; оновлено попередні furnace/test-tuning wiring assertions. Перевірено 14/14 статичних умов інтеграції на повторно прочитаних GitHub source files. Повний Forge build, Python suite і Minecraft runtime не виконані: немає локального checkout/Forge dependency cache і мережевого доступу з контейнера. Не заявляти повний acceptance.

Після оновлення:
1. Зробити резервну копію світу. Запустити гру, перевірити MILL-CONTENT-SYNC, backup, фактичні конфігурації Mayan miner / Byzantine hostess, config.txt і millenaire-custom. Другий запуск не має повторно копіювати ідентичні файли.
2. Підійти до рибалки/шахтаря, відкрити diagnostics і дочекатися natural goal selection, перевірити reason, stock values, last destination. Не робити висновків про блокування маршруту лише з AVAILABLE/NO_DESTINATION.
3. Перевірити відсутність menu та відмову прямих packets у гравця без прав, range/dimension, disconnect, кількох операторів, GUI pagination, повільний NPC, невідомі специфічні умови, 1× та прискорене тестування.
4. Запустити Forge compileJava/build і test_goal_diagnostics_wiring.py, test_furnace_production_wiring.py, test_test_tuning_wiring.py; перевірити startup на Forge union filesystem і Windows. Перевірити лог та скриншот реального UI.
5. За результатами визначити наступні production/logistics bugs; не розширювати food consumption system без окремого рішення користувача.

Гілка fix/villager-door-access-and-village-territory, main не змінено.
