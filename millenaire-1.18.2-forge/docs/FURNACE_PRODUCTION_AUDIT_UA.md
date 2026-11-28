# Аудит печей та виробничого циклу — 24.11.2026

## Вихідні дані та оригінальна поведінка

Користувач повідомив: у візантійського рибалки майже повна скриня сирої риби, у печі є паливо та 3 готові рибини, NPC продовжує ловити. У попередньому latest(20261124-152922).log пошук fisherman_byzantine з goal=fish дав 111 рядків, cookfish і tendfurnace — 0. Це події навігації, не лічильник завершених операцій. Точний SHA встановленого JAR та фактичні запаси ратуші невідомі.

Джерела:
- https://www.millenaire.org/downloads — 6.2.0-beta.14: поділ специфічного cooking і maintenance (паливо та вилучення готового); beta.15: виправлення вилучення готового; beta.16: японське куряче м'ясо та індійська цегла.
- https://www.millenaire.org/encyclopedia/culture/byzantines/villagers/fisherman_byzantine — Psaras ловить і готує рибу.
- https://new.millenaire.org/encyclopedia/culture/byzantines/villagers/hostesse_byzantine — Oikodespina готує баранину, курку, рибу, свинину, яловичину та сувлакі.
- https://new.millenaire.org/encyclopedia/culture/byzantines/villagers/smelter_byzantine — Chytis виплавляє ресурси.
- https://www.millenaire.org/encyclopedia/culture/norman/villagers/miner — Mineur видобуває й обробляє ресурси.
- https://www.millenaire.org/encyclopedia/culture/seljuk/villagers/turk_cook — Aşçı готує кілька видів м'яса та рибу.
Поточна енциклопедія стосується новішого rewrite, тому конкретні квоти, пріоритети і назви goal беруться з нашої 8.1.2 конфігурації, а не копіюються з 9.x.

## Перевірені спільні задачі та професії

Прочитано 21 genericcooking і 4 generictendfurnace визначення. У дереві cultures — 285 профілів (50 Byzantine, 28 Indian, 34 Inuit, 31 Japanese, 37 Mayan, 60 Norman, 45 Seljuk). Вручну цільово перевірено 60 різних профілів за професіями, включно з окремими будівлями та членами сімей; не стверджуємо, що всі 285 повністю перевірено. Додано відтворюваний повний інвентаризатор профілів.

- Byzantine: fisherman — fish/cookfish/tendfurnace; smelter — cookclay/cooksandbyz/cookstonebyz/tendfurnace; lone shepherd — cooklamb/tendfurnace; lighthouse keeper — relight/tendfurnace; tavern hostess — 5 cooking без maintenance, host має makewine, не maintenance. Виправлено hostess.
- Indian: miner — cookstone/tendfurnace; peasantwife — cookchicken/tendfurnace; peasant — cookindianbrick/tendfurnacebrickkiln; adivasipeasant — cookindianbrick/cookstone/cookchicken/tendfurnace/tendfurnacetownhall. Окремо перевірити покриття tagged brickkiln у lone adivasi, якщо такий є, не призначати роботу без наявного workplace.
- Inuit: fisherman fishinuit; chef — cookfish/cookfishslab/cooksteak/cookpotato/tendfurnace; lone fisherman, wife та sanduitwife мають maintenance; husband cookpotato без власного tend, дружина має tend. Не додавати приготування кожному рибалці, якщо передбачено окремого кухаря.
- Japanese: miner/loneminer — cookstone/cooksand/tendfurnace; fisherman — fish/cookfish/tendfurnace; peasantwife — cookchickenkitchen/tendfurnacekitchen.
- Mayan: miner — cookstone/cooksand/tendfurnace; chickenfarmer — cookchicken/tendfurnace.
- Norman: miner/loneminer — cookstone/tendfurnace; glassblower — cooksand/tendfurnace; cattlefarmermale — cooksteak/tendfurnace; pigherd — cookpork/tendfurnace.
- Seljuk: cook and cookwife — cookBeefKitchen/cookMuttonKitchen/cookFishKitchen/cookChickenKitchenseljuk/tendfurnacekitchen; wife також tendfurnace; fisherman — fish/cookfish/tendfurnace; loneminer — cookstone/tendfurnace; shepherd — tendfurnace. Не додавати нові рецепти звичайному turk_miner без доказу.

Не змінювали незрозумілі або спеціально задані itemsbalance (cookclay stone/brick, cooksandbyz sandstone/glass), рецепти, ціни, building limits, townhall limits, швидкість виплавки, navigation stage 2. Уточнити спеціальні пари окремо за оригінальними 8.1.2 даними; сучасна енциклопедія не документує їхню точну семантику.

## Реалізація

- 6f4f56f, c3d391b: GoalFish: використовує наявний cap 128 сирої/готової тріски, враховує склад будинку, інвентар NPC і фактичне місце риболовлі. Перевірка перед плануванням та перед отриманням улову, пріоритет за запасом будинку (не за старою ціллю). GoalFishInuit успадковує правила. Не видаляти надлишок зі старих світів.
- abca52d: GoalGenericTendFurnace: перевіряє наявність готового output і можливість зберегти його, незалежно від палива та розміру cooking batch; для звичайної печі і трьох output slots firepit. Вилучає тільки фактично збережену кількість, лишає залишок, не відкидає NBT. Повна скриня не запускає output-only action.
- 2ae1846, 590ed05: Goal.passesResourceBalance: базова семантика збережена. Для local cooking з matching input balance додаються фактичні запаси workplace та інвентар NPC до запасів ратуші. Не дублювати townhall count. Для townhall та спеціальних balance pairs — оригінальна політика. Це обґрунтована зміна балансу, не гарантія безперервної виплавки за будь-яких квот.
- b7c7384: cooking output-only fallback перевіряє можливість зберігання і не вимагає minimumToCook для готового output. Вхідний minimumToCook та production quota залишаються.
- d922e90, 0b4ff53: Byzantine hostess отримала tendfurnace і невеликий запас 4 колоди кожного стандартного виду (minimumfuel=3; кількість 4 — наше рішення, не точна цифра з вікі). ContentDeployer додає тільки відсутні записи в існуючу stock конфігурацію за наявності cooksteak, зберігає змінені користувачем quantities, резервує оригінал, не змінює millenaire-custom. Лог TAVERN-FURNACE-MIGRATION лише коли внесено зміни.
- e639720: tools/tests/test_furnace_production_wiring.py: source wiring regression + інвентар всіх профілів. Повідомлення REVIEW — можливе непокриття в окремій ролі, а не доказ дефекту (інший житель може обслуговувати ту саму піч).

## Перевірка та acceptance

Повторно прочитано змінені файли з GitHub і виконано 14 статичних перевірок їхнього зв'язку (14/14). Повний Python audit, Forge compileJava/build та runtime test не виконано: повний робочий checkout і Forge залежності в цьому режимі недоступні. Не називати це завершеним acceptance.

Після git pull і Update-Millenaire.bat:
1. Старий світ: перевірити рибалку з великою кількістю сирої риби, паливом та 3 готовими рибинами; fish має припинитися при cap, cookfish має завантажити сировину за допустимих quota/balance, tendfurnace — вилучити готове. Перевірити фактичні кількості до/після.
2. При відсутності сировини/палива, при quota reached, 1 готовому item, повній скрині, частковому вільному місці, кількох працівниках: не губити і не дублювати предмети. Перевірити tagged output, який навмисно не переміщується старим storeGoods API.
3. Перевірити всі 7 культур, домашні, tagged kitchen, brickkiln, townhall, firepit, tavern, lone building. Для нового/старого світу і CRLF/LF перевірити ідемпотентність migration, резервні копії та custom overrides.
4. Запустити python tools/tests/test_furnace_production_wiring.py з checkout; переглянути кожен REVIEW за реальною будівлею і відповідальними жителями. Перевірити фізичну доступність печі; не підміняти production bug navigation workaround.
5. Якщо рибалка досі не готує, потрібні свіжий лог, фактичні запаси ратуші та будинку, вміст усіх слотів печі, зареєстрований furnace point та work position. Старий лог не доводить єдину причину cookfish=0.

Гілка fix/villager-door-access-and-village-territory, main не змінювалася. Навігаційні класи та stage 2 handoff не змінені.


## Backlog — майбутнє розширення продовольчої економіки (24.11.2026)

**Статус: відкладена ідея, не затверджена до реалізації.** Користувач просить лише зафіксувати концепцію на майбутнє. Не включати до поточного етапу виправлення багів, не змінювати код, рецепти або баланс ресурсів у межах цього запису.

### Мотивація

За спостереженням користувача, у поселеннях накопичуються значні запаси їжі без помітного споживання. У поточній моделі їжа переважно витрачається на ріст дітей, підвищення шансів зачаття, виробництво страв і торгівлю; дорослі не мають регулярної потреби в харчуванні для виживання. Це підтверджують MillVillager.attemptChildConception(), механізм росту дітей, VillagerConfig та villagerconfig/default.txt. Після стабілізації населення попит може падати, а виробничі будівлі — втрачати економічну значущість. Реальні темпи виробництва та споживання ще не вимірювалися.

### Можлива концепція

- Регулярне споживання їжі жителями, прив'язане до ігрового часу та чисельності населення; розмір порцій і частоту визначити після вимірювання продуктивності господарств.
- Різноманітність раціону: різні продукти і культурні страви мають економічний сенс. Зберегти культурні обмеження та наявні бонуси росту/зачаття, не вимагати екзотичних продуктів для базового забезпечення.
- Продовольчі резерви та розподіл між домашніми скринями, виробниками і ратушею. Не плутати фізичний запас із доступним після резервування, не рахувати один предмет двічі.
- Стани забезпечення поселення: дефіцит, достатній запас, надлишок. Можливі м'які наслідки для народжуваності та продуктивності; конкретні коефіцієнти ще не визначені.
- Адаптивне виробництво за попитом, резервами, споживанням та місткістю складів. Надлишок має стимулювати торгівлю, а не нескінченне накопичення.
- Експорт надлишків і закупівля за дефіциту: аграрні поселення стають постачальниками, промислові та військові — споживачами. Спершу перевірити фактичні механізми міжсільської торгівлі.
- Можливість увімкнути/вимкнути нову систему налаштуванням; сумісність зі старими світами та custom-культурами.

### Обмеження та передумови

Не вводити смерть від голоду або неконтрольований каскад занепаду поселень через збої навігації, доставки чи доступу до печей. Не видаляти накопичену їжу штучно. Спочатку завершити поточні виправлення виробництва, логістики й навігації. Перед проєктуванням зібрати baseline за всіма 7 культурами: виробництво, фактичне споживання, населення, резерви, торгівля та місткість складів за ігрову добу. Перевірити баланс малих і великих поселень, різних спеціалізацій, дефіциту палива та тимчасових перебоїв постачання. Не збільшувати навантаження AI через окремі часті пошуки маршрутів для кожної порції.

Критерій майбутнього покращення: їжа має стабільний економічний попит, господарства залишаються корисними після завершення розвитку населення, міжпоселенська торгівля набуває практичного сенсу, а тимчасовий дефіцит не руйнує поселення. Усі параметри потребують окремого узгодження з користувачем перед реалізацією.

