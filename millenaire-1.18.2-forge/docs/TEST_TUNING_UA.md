# Тестові множники (cheats) — 24.11.2026

## Призначення і керування

У меню M → Налаштування → Тестування (чіти) додано окремий екран. Параметри діють на всіх активних жителів Millénaire у поточній серверній сесії, усі культури, не на гравців і не на глобальний tick rate. Кожна кнопка циклічно перемикає значення, окрема кнопка повертає всі до 1×. Значення підтверджує сервер; поки очікується відповідь, кнопки заблоковані. Підтримано en_us, uk_ua, ru_ru.

| Параметр | Значення |
|---|---|
| Рух | 1–3×, крок 0.25 |
| Робота | 1–10× з кроком 1, потім 15×, 20× |
| Видобування | 1–10× з кроком 1, потім 15×, 20× |
| Будівництво | 1–10× з кроком 1, потім 15×, 20× |

Клієнт показує меню лише за hasPermissions(2). Сервер повторно перевіряє ServerPlayer.hasPermissions(2) на кожен запит і зміну, дозволені значення перевіряються whitelist. Для singleplayer потрібні чіти; на dedicated server — відповідні права оператора. Параметри належать MinecraftServer, не зберігаються у NBT, config.txt або клієнтському профілі. Після перезапуску світу — 1×. Не потребують DEV mode, який може змінювати інші механіки. Зміни логуються як MILL-TEST-TUNING: move=...%, work=...%, yield=...%, build=...%. Відкриття меню саме собою нічого не змінює. Операторські налаштування діють протягом поточної сесії, поки не скинуті; втрата права не дозволяє змінювати їх, але не скидає глобальну конфігурацію за інших операторів.

## Реалізація та межі

- TestTuning.java: незалежні серверні множники, default 100%, межі, авторизація, лог, session map. Рух — transient MULTIPLY_TOTAL attribute modifier з власним UUID; reset прибирає саме його і не змінює базову швидкість професії або sprint modifier. Не змінює правила pathfinding, collision, ladder velocity, телепортацію чи відновлення шляху. Фактична швидкість обмежена фізикою Minecraft.
- MillVillager.java: застосування movement modifier лише на server tick, коригування існуючого goal.actionDuration. Мінімум один tick для позитивної тривалості, 0 залишається 0. Робота — explicit production task whitelist: generic cooking/crafting/mining/gather/harvest/plant/slaughter, maintenance, legacy production, fishing, shearing, animal breeding, etc. Не змінює sleep, leisure, combat, chat, trading чи travel cooldowns. Не прискорює ванільний час плавлення, ріст рослин/тварин, reoccurDelay, AI planning, денний цикл.
- Будівництво: GoalConstructionStepByStep, GoalBuildPath, GoalClearOldPath використовують окремий factor замість work. Не встановлювати блоки пачками і не пропускати потрібні перевірки; не змінювати витрату будматеріалів. На малих базових тривалостях 20× обмежено одним tick, тому реальний gain не завжди 20×.
- Видобування: виклики resourceAmount лише в GoalMinerMineResource, GoalGenericMining, GoalFish/GoalFishInuit, GoalLumbermanChopTrees, GoalGenericHarvestCrop, GoalGenericGatherBlocks, GoalGenericSlaughterAnimal (тільки explicit bonus), GoalHarvestCacao, GoalHarvestWarts, GoalIndianHarvestSugarCane, GoalByzantineGatherSilk, GoalByzantineGatherSnails, GoalShearSheep. Не масштабувати загальний addToInv/storeGoods, торгівлю, перенесення, виробництво зі споживанням інгредієнтів, готову продукцію печей, збір уже виготовленої цегли, ванільні/сторонні entity loot tables. Зберегти звичайні квоти й логіку цілей; більша партія може перетнути окремі soft limits. Риболовля окремо обмежує партію залишком до наявної квоти 128. Не видаляти надлишок зі старих світів.
- Network: protocol 1.18.2-4, request 207 і state 112, strict 8-byte request, direction-restricted existing channel. Усі клієнти/сервер повинні використовувати той самий новий JAR.
- GuiConfig / GuiTestTuning: окремий screen, циклічні кнопки замість slider (відповідають наявному стилю налаштувань), не pause screen, reset та повернення. Не зберігає множники у звичайну конфігурацію.
- Статичні regression checks: tools/tests/test_test_tuning_wiring.py. Не підмінюють повну компіляцію чи runtime тест.

## Acceptance

Повторно отримано актуальні файли з GitHub, 14/14 базових статичних перевірок зв'язку пройшли. Повний Forge build, Python test suite та playtest не виконані у цьому режимі; доступу до робочої інсталяції Minecraft немає. Після оновлення:
1. Звичайний світ без чітів: меню не показане, усі множники 1×, прямі пакети від гравця без permission 2 не змінюють settings. Перевірити invalid field/value, повторний запит та reset.
2. Світ з чітами: M → Налаштування → Тестування; окремо встановити 2× руху, 5× роботи, 5× видобування, 5× будівництва. Перевірити server reply, лог, зміну без restart.
3. Перевірити реальний рух, таймер роботи, фактичний вихід шахтаря/рибалки/фермера та швидкість будівельника, відсутність дублікації при доставці/торгівлі, збереження витрат. Не трактувати зменшення часу очікування як гарантію 5× throughput.
4. Скинути 1×, перевірити відновлення baseline. Перезапустити світ, переконатися, що попередні множники не перенесені. Окремий контроль для всіх культур та dedicated server.
5. Для звичайного порівняльного profiling navigation використовувати лише 1× або фіксувати однакові множники. Швидкий рух може підсвітити або посилити наявні фізичні проблеми; не використовувати test speed для маскування stage 2.

Гілка fix/villager-door-access-and-village-territory. main та stage 2 алгоритми навігації не змінювалися.
