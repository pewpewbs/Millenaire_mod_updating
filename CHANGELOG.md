# Журнал змін Millénaire+ (CHANGELOG)

Цей файл фіксує **додані можливості, зміни поведінки, виправлення й їхню прив'язку до версії мода**. Номери версій беруться з `millenaire-1.18.2-forge/gradle.properties` (`mod_version`), а не вигадуються за кількістю комітів.

> **Стан на 2026-11-23:** код мода — `8.1.2-port.0`; Minecraft **1.18.2**, Forge **40.3.0**, Java **17**. Це версія порту в розробці, а **не підтверджений стабільний реліз**. Наведені нижче зміни перебувають у гілці `fix/villager-door-access-and-village-territory`, **не в `main`**.

## [Unreleased] — `8.1.2-port.0` (гілка `fix/villager-door-access-and-village-territory`)

Період змін: **22–23 листопада 2026 року** (за датами комітів UTC). Базовий коміт `main`: [`a1cde6e6`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/a1cde6e6b47bd40a04af19909d46230081b16258). На момент створення цього журналу гілка мала **62 додаткові коміти**.

### Оновлення від 2026-11-23 — резервні двері та вертикальні драбини

- **Виправлено (підготовлено, очікує Forge-перевірки):** `MillVillager.handleDoorsAndFenceGates` тепер має обмежене відновлення для зачиненого проходу, який не потрапив до маршруту: раз на 4 тіки під час зупинки вибирає **одні** найближчі двері чи хвіртку попереду за напрямком наступного вузла/цілі, перевіряє бокове відхилення, відстань, пряму видимість без стіни й викликає спільний `VillageDoorAccess`, що поважає ручний пріоритет гравця. Більше не відкриває одразу весь сектор дверей 5×5 ([`9d7062b`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/9d7062b552e4b03e7e46043198786c7707014ff2)).
- **Виправлено (підготовлено, очікує Forge-перевірки):** класичний `AStarWorker` тепер **реально використовує** вертикальні ladder-переходи між суцільними секціями драбини, лише за вільного простору для ніг/голови і з правильною обробкою вже відвіданих вузлів. До патча `checkPossibleLadder` був мертвим кодом із порожніми перевірками ([`f81e389`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/f81e389be5966d70e00e6ab5f53a938efd4238e1), [`96d470c`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/96d470c23dc980cb1cf48439b6c21fbe1663e9a3)).
- **Додано (підготовлено, очікує Forge-перевірки):** `AStarPathPlannerJPS` при суттєвій різниці висот і сусідній драбині перевіряє додатковий класичний A* маршрут та порівнює розраховану вартість двох маршрутів. Пошук обмежений часовим бюджетом і локальним скануванням `PathWorldSnapshot` без завантаження чанків ([`855f763`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/855f7636a6ef6e83a2ced5b00e372930fbe2a060)).
- **Додано (підготовлено, очікує Forge-перевірки):** `PathNavigateSimple.tick` здійснює керований фізичний підйом/спуск уздовж послідовних вузлів ванільної драбини, коли NPC дійсно стоїть на її колонці; без прямого встановлення координат/телепортації ([`0a00e88`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/0a00e88d8f9a3abe2bacbd6c70dd77a6474b0eca), [`dd693ae`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/dd693ae8e83441955e72a05ffc3e040665429b98)).
- **Додано тест:** спільні `LadderTransitionRules` та `LadderTransitionRulesSmoke.java`, ізольований тест на Java 17 **пройшов** для вільної/заблокованої драбини, вертикального руху, заборони польоту за межами колонки. Це **не** повна компіляція Forge чи тест у Minecraft ([`f291934`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/f291934be35514c944df88937d91f4b25ded174f), [`87c610b`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/87c610b8c2af29d082789f8b6c4ddaa9b508ab4d)).
- **Документовано:** тести DOOR-05 і LADDER-01 для дверей та драбини кар'єру в `millenaire-1.18.2-forge/docs/PLAYTEST_ISSUES_2026-11-23_UA.md` ([`3dd26fb`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/3dd26fbda539367d082128842bd4a33745b6a7d0)).
- **Змінено (безпосередньо протестовано на Java 17, інтеграція Forge очікується):** резервний вибір дверей використовує спільний `DoorApproachRules.score`, який відхиляє проходи позаду, збоку, на іншому рівні та надто далеко. Додано `DoorApproachRulesSmoke.java`, standalone тест пройшов; `ClipContext`, server tick та взаємодія з гравцем ще потребують ігрової перевірки ([`1555332`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/15553326ada67dfd34685942fba50eb722515f69), [`d5e029a`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/d5e029a98eff14e5b2f68bf026cd16c22fe990fb), [`15b7f4c`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/15b7f4ccfe7ce66aec91029287b12e464cf8aa2d)).
- **Змінено (очікує Forge-перевірки):** нові вертикальні A* переходи обмежено **ванільною драбинною секцією `Blocks.LADDER`**, щоби не планувати підйоми ліанами без відповідного рухового контролера ([`aee4292`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/aee4292b8ad0560418af729b39dc958176bba093)).
- **Документовано:** розширено сценарії тестування DOOR-05 і LADDER-01, статус ізольованих тестів та ризики інших `CLIMBABLE`-блоків ([`841c9be`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/841c9be293bdad601f4f01332b0fbafb01b013a6)).

- **Обмеження:** вертикальний шлях наразі підтримує ванільні драбини, а не будь-які climbable-блоки; стандартний наземний шлях може залишитися кращим, якщо драбина поза зоною порівняння; потрібно перевірити спуск/підйом та вихід до хвіртки в реальній грі. `main` не змінювався.

### Оновлення від 2026-11-23 — коректний стан дверей і ручне керування

- **Виправлено (очікує Forge-компіляції й ігрового тесту):** у `PathNavigateSimple.createPathFinder` тепер налаштовано **обидва прапорці** `VillagerWalkNodeEvaluator`: `setCanOpenDoors(true)` та `setCanPassDoors(true)`. Це дозволяє pathfinder планувати шлях через зачинені дерев'яні двері, а не розраховувати лише на випадковий резервний скан поруч із NPC ([`f2f41145`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/f2f411451cf70cf19a67ff7fd6b5fb6c676c1832)).
- **Виправлено (очікує Forge-компіляції й ігрового тесту):** серверний `VillageDoorAccess` реагує на правий клік гравця по дерев'яних дверях/хвіртках, скидає попередню auto-close lease NPC і тимчасово надає гравцеві пріоритет на **100 тіків (5 секунд)**. Коли гравець відчиняє двері, NPC не закриває їх через стару lease; коли гравець закриває їх, NPC не відкриває відразу. Облік ведеться за нижньою половиною двоблокових дверей, час життя даних обмежено. Сигнал redstone та інші сторонні моди не переназначалися ([`0a73f7fc`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/0a73f7fc56a71b23c61cb370acd8bb982620077e), [`eceefa3e`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/eceefa3e1a8c63ff1746732f9542fa5d84a1148b)).
- **Додано перевірки:** сценарій `DOOR-04` у [`PLAYTEST_ISSUES_2026-11-23_UA.md`](millenaire-1.18.2-forge/docs/PLAYTEST_ISSUES_2026-11-23_UA.md) — обидва боки дверей, upper/lower, відкриття та закриття гравцем, часове блокування, тварини, redstone, кілька NPC ([`5e84e4c`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/5e84e4c4b9cce2382b124043ca2244514a953f1d)).
- **Статус:** зміни збережено в тестовій гілці, але **успішної Forge-компіляції та тесту Minecraft ще немає**; `main` не змінено.

### Оновлення від 2026-11-23 — двері та будівництво у воді

- **Виправлено (потребує тесту в Minecraft):** у `MillVillager.handleDoorsAndFenceGates` попередній вузол дверей/хвіртки підтримується відкритим, лише доки **bounding box NPC фізично перетинає його область**. Не повертаємо безумовну перевірку попереднього вузла, яка колись спричиняла повторне відкривання дверей позаду жителів. Час утримання дверей і хвірток після виходу не збільшено ([`749488e`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/749488e275a143e1d32a9e45f7a8a8ab82c33052)).
- **Змінено (потребує тесту в Minecraft):** `GoalConstructionStepByStep` тепер призупиняє непотрібний рух, поки будівельник уже в межах доступу до блока, зокрема під час роботи у воді. `MillVillager.checkGoals` підтримує це зупинення на кожному тіку поточної дії, попри скидання `stopMoving` на початку `tick`. Рух до далеких наступних блоків відновлюється звичайно; **час роботи за блок і водна фізика не прискорені** ([`132801f`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/132801f8d4cc06f8731472108b7e799aa1d50626), [`0fecffe`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/0fecffe2300bc3289e94a1c6e111ee5c02bd0a9c)).
- **Додано план перевірок:** `DOOR-03` та `BUILD-WATER-01` у [`PLAYTEST_ISSUES_2026-11-23_UA.md`](millenaire-1.18.2-forge/docs/PLAYTEST_ISSUES_2026-11-23_UA.md) ([`7e97782`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/7e97782d94bcf0004bbcfd786f042e15a4b4c3b3)).
- **Статус:** внесені кодові виправлення не є підтвердженням розв'язання проблем у грі; повна Forge-компіляція та ігровий тест актуальної гілки поки не підтверджені.

### Оновлення від 2026-11-23 — системне відновлення руху і акацієві гаї

- **Виправлено (підготовлено до гри):** `MovementWatchdog` більше не трактує стрибки по Y на місці як прогрес; звичайний рух оцінює по X/Z, а справжнє лазіння по climbable-блоках допускає вертикальний прогрес. Застосовано послідовність `REPATH` після 80 тіків, `DETOUR` після 160 та `RECOVER` після 240 ([`b682df43`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/b682df43f5dc0c10df7f120ec0fe27f10a6f7ba7)).
- **Змінено (підготовлено до гри):** `DetourPathFinder`, `PathNavigateSimple`, `MillVillager` підтримують разову обмежену розширену A* спробу при тривалому застряганні, збережено світовий CPU-ліміт та наявні правила безпечної телепортації/заборони телепортації ([`b38a7b41`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/b38a7b4122b1189c6d5f91d4a854da78e857cddb), [`45b70ce1`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/45b70ce1b052825c3d249d22c71043ba558a9a8b), [`d267dac3`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/d267dac33aa258e8df2af68b9604a82ddb5fc3e4)).
- **Виправлено (підготовлено до гри):** `BuildingResManager` перевіряє сусідні існуючі паростки й стовбури акації на позначених точках посадки; зменшена ймовірність конфлікту дерев у старих гаях ([`5f08c370`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/5f08c370082edde99523b0dd23724a4d34db3428)).
- **Змінено (плани нових поселень):** у `acaciagrove_A0.png` великого візантійського гаю **8 → 4** точки посадки, у `acaciagrovesmall_A0.png` малого — **4 → 2**; маркери `acaciaspawn` переміщено разом із ґрунтом під ними. Вже збудовані гаї автоматично не перебудовуються. Потрібні тести росту і балансу деревини ([`abf2f43e`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/abf2f43ef6f1036789140d0d8350ccb1bc97a691)).
- **Додано тест:** `tools/tests/MovementWatchdogSmoke.java` — самостійний Java 17 smoke test із сценаріями стрибків на місці, звичайного переміщення, лазіння та зміни цілі; ізольований тест компілювався й пройшов, **повна Forge-компіляція та тест у Minecraft не виконані** ([`f90dc9bd`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/f90dc9bd99ea7e1be02274ff2c5f5e931a0f4807)).
- **Документовано:** критерії та ризики тестування NAV-STUCK-01 і TREE-02 у `millenaire-1.18.2-forge/docs/`. Проблема вибіркового відчинення дверей DOOR-02 лишається відкритою.

### Оновлення від 2026-11-23 — повторне тестування

- **Виправлено (підготовлено, без ігрової перевірки):** будівельники під час отримання ресурсів із Town Hall тепер перевіряють фізичну доступність складської точки, а не лише відстань; отримання ресурсів не виконується через стіну чи з ями. Змінено `GoalGetResourcesForBuild` ([`445c0b4e`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/445c0b4e6f7b9900827bbe3a315009372ccae9c7)).
- **Виправлено (підготовлено, без ігрової перевірки):** отримання будівельних ресурсів включено до логістичних завдань із перевіркою фізично досяжних точок у `MillVillager` ([`17e436d0`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/17e436d0d23754dcd6f05ba71558b30f1315b553)).
- **Документовано:** нові ігрові регресії BLD-01, DOOR-02 та TREE-02 з доказами, обмеженнями й тест-кейсами ([`c80d8fbd`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/c80d8fbd411107b21478cc2c93826749932f0813), [`PLAYTEST_ISSUES_2026-11-23_UA.md`](millenaire-1.18.2-forge/docs/PLAYTEST_ISSUES_2026-11-23_UA.md)).
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

- **2026-11-22:** [`a1cde6e6`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/a1cde6e6b47bd40a04af19909d46230081b16258) — початкове джерело порту Millénaire 1.18.2 у `main`. Це вихідна точка порівняння, **не окремий опублікований реліз**. Виправлення, що вже містилися в цьому початковому знімку, не привласнюються гілці та не датуються заднім числом.

## Повний покомітний журнал поточної гілки

Нижче — усі **62** коміти поверх базового `main` на момент створення журналу. Оригінальні назви комітів залишені англійською для точного пошуку в GitHub. Час — **UTC**, новіші вище. Записи показують **послідовність розробки**, включно з проміжними спробами, які згодом було скасовано.

### 2026-11-23 (UTC)

- [`1097a1b9`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/1097a1b9fddf6b66e0ea3a087a855dcf7c1fb3e3) — Document acacia grove harvesting and vanilla sapling clearance tests (00:07 UTC).
- [`4935dee7`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/4935dee7a0e18498a60bbf08d3b978d5c1d3bd28) — Skip blocked acacia planting markers that cannot support vanilla tree growth (00:06 UTC).
- [`a9f244a3`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/a9f244a37b08e156018c6a354c19dbb5e9781f25) — Select accessible lower tree trunks and consistently recognize acacia logs in groves (00:04 UTC).
- [`fca2dc2a`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/fca2dc2a5e4d8c379eee210521befab7dec6872a) — Let lumbermen clear partly harvested trees with fewer than five logs (00:04 UTC).

### 2026-11-22 (UTC)

- [`fd248796`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/fd248796cfeb60e9c336331307f430dc47fc67b2) — Document sheep-batch logistics, colored wool use and door timing regression tests (23:59 UTC).
- [`4909b540`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/4909b540b51192286386f7e88bb5cbbcf78eea49) — Stop reopening doors behind villagers while following a path (23:58 UTC).
- [`40f1b01e`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/40f1b01eacb75a387ce40084a51a3200fed0e583) — Close wooden doors promptly like gates after the actual crossing is clear (23:58 UTC).
- [`a18cbce2`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/a18cbce21e1947c737aeac6cc649ba63908f7942) — Deliver full sheep wool batches before more shearing or breeding, retaining colour (23:57 UTC).
- [`6e3a8d2f`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/6e3a8d2f2fea49a11fdfec8294061c07311f05db) — Shear sheep in bounded wool batches and count all wool colours (23:57 UTC).
- [`6018d5f2`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/6018d5f2c005a0bff05a51c3a545a2ccd9a3750e) — Restore Comparator import required by InvItemAlphabeticalComparator (23:28 UTC).
- [`68947d1a`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/68947d1a3e745248a74e9e38342855847c8306fb) — Mark superseded forced-gate experiments as historical playtest findings (23:22 UTC).
- [`321db9b7`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/321db9b72773ee106e608b31835c02c14be24252) — Document systemic cost-based navigation rework and gameplay regression tests (23:22 UTC).
- [`f20d6f6c`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/f20d6f6c8cd67f9b20d799878a0f843499397487) — Log alternative route costs when pathing diagnostics are enabled (23:21 UTC).
- [`4f534a4e`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/4f534a4efa8a7f97078474daf0c581e6b0b1a2df) — Allow head clearance above closed fence gates so normal pathfinder can traverse them (23:20 UTC).
- [`2ddb8126`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/2ddb81266e3dcd61f4d4d4823760853823ca69c8) — Never improve A* queue nodes through nonviable terrain transitions (23:20 UTC).
- [`3f26a3c1`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/3f26a3c1a52c9653cc8c3ad06460569fde659301) — Remove forced gate routing and retain generic navigation with automatic door interaction (23:20 UTC).
- [`569e6d3d`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/569e6d3da82c79d8229ef050d7072e66c75d6ce3) — Compare physical costs of bounded alternate paths instead of accepting first reachable detour (23:19 UTC).
- [`bbd8aa9d`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/bbd8aa9d1cff025752daa187a6d7d5d378ff3f73) — Fix JPS queue decrease-key, reopen improved nodes and correct neighbor costs (23:18 UTC).
- [`a8c4d230`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/a8c4d230bad0f116faef65a13dce9d362aace309) — Correct classic A* expansion order and reopen cheaper closed nodes (23:18 UTC).
- [`10973b0c`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/10973b0c5d51d2da2838388786fa12d4eb66cb43) — Use admissible A* heuristic; remove double-counted path cost (23:18 UTC).
- [`b884b8d4`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/b884b8d461185259b06aa08003a47898e748d40f) — Record 00:08 gate-bypass trace and inside-face staged route diagnostic (23:10 UTC).
- [`efff351d`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/efff351d896d89253de3ab6b052c8945bc897256) — Use inside-gate approach for courtyard exits and log forced-crossing rejection reasons (23:10 UTC).
- [`52e319c5`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/52e319c5e7bbdc97f5a3057b6fe052c2f5e2cba0) — Explain confirmed gate-bypass log and staged gate crossing patch (23:02 UTC).
- [`faece577`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/faece577991d84f03e8cf36ade6dab588431c048) — Force courtyard exit across a reachable fence gate when A* bypasses opening (23:01 UTC).
- [`2975aacb`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/2975aacb2ebf413aec3b0d7301e889d4c589ca91) — Audit log evidence: distinguish relevant gate path failures from unrelated gate probes (22:50 UTC).
- [`b043d2d2`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/b043d2d249b42a6992c3ea205c2f5ad9813b65db) — Differentiate gate route failure reasons and reduce irrelevant navigation logs (22:49 UTC).
- [`26f872c2`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/26f872c229746683e854d8da443db0d0c9249e6b) — Include gate-entry failure marker in playtest log instructions (22:42 UTC).
- [`8c4356bd`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/8c4356bd24d43780a9abc84bbb91e33916d9c609) — Add gate-entry failure logging and remove obsolete exit comment (22:42 UTC).
- [`ebe6c9e4`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/ebe6c9e4bea04c51fb204524efbe12ddb0454cc8) — Document teleport-in bypass of remembered gate and diagnostics (22:41 UTC).
- [`8c994f2a`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/8c994f2ab4e516b085a9cbf47e60c7ef3ede3b14) — Log blocked gate exits and stock teleports for targeted repro (22:41 UTC).
- [`49f170aa`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/49f170aa090994fc1c99cf08a27969d3fa5678bc) — Keep gate waypoints partial and avoid loading unloaded remembered gates (22:40 UTC).
- [`c671e04a`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/c671e04a7682f4a113cbacf5e855c9638c66aae8) — Find courtyard exit gate even after villager teleports inside; log route selection (22:40 UTC).
- [`3f4f72a7`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/3f4f72a7e1a1482acf1093370bbf33b948a1c483) — Track asymmetrical courtyard exit routes and recent-gate waypoint experiment (22:34 UTC).
- [`5bb2a099`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/5bb2a0998b2677eb2a0a0faedb78f90992d7aa68) — Prefer recently traversed courtyard gate for return trips across enclosure (22:33 UTC).
- [`76b3336a`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/76b3336a8d0db3587d34dbe8a883a7bee8311115) — Track fast-close gate policy and remaining livestock escape limitation (22:20 UTC).
- [`8cd88859`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/8cd88859adfcb3816c48fcdc02589fce795cf49f) — Avoid opening pen gates while planning distant stock routes (22:19 UTC).
- [`7ae69ef7`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/7ae69ef7e7e9bebdb8ea1c920f9f1a484528cd82) — Close livestock-pen gates promptly once crossing is clear (22:19 UTC).
- [`e45a4a01`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/e45a4a0178f8b57cc2f64ca56e61fed60d0bb55f) — Document gate-first rescue for physically stalled residents (22:09 UTC).
- [`3b67b5b7`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/3b67b5b7c549e391fd3fb0ab4999fbcef51d9873) — Try gate entry route on physical stalls before teleport fallback (22:09 UTC).
- [`e3efb06b`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/e3efb06b8c54c161c6d2fb661c8032f5acecb374) — Record failed Anna playtest and side-gate waypoint fix for retesting (22:08 UTC).
- [`5e5ca413`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/5e5ca413296203d933040747c8e6ed4c19cd39e8) — Allow gate access waypoint routes time to approach before automatic closing (22:08 UTC).
- [`f5882079`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/f5882079b241d096c9566cb5bb297c4982b909e4) — Probe accessible side gates as stock-transfer waypoints before teleport recovery (22:07 UTC).
- [`4fc9cab0`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/4fc9cab0c83d5319a16536b14e05b20b09d07228) — Run Gradle in child PowerShell and safely stage and replace JAR (21:54 UTC).
- [`4e16ff14`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/4e16ff1471baaa242238f9861a2861728ec4a404) — Fix BAT launcher and keep errors visible until keypress (21:54 UTC).
- [`fa548543`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/fa5485431a324d3ace90e2685646cc5010e109b8) — Document reproducible stock-access wall trap and navigation acceptance tests (21:51 UTC).
- [`acc11959`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/acc119596ea6af99076aee828c42fbf0b426ae7c) — Rank access-node proximity in all three axes (21:50 UTC).
- [`5a0adcb9`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/5a0adcb9abcb618a9c99cc38a90def70513d936b) — Route stock-transfer NPCs to reachable visible access nodes, not walls (21:50 UTC).
- [`8d6f94df`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/8d6f94df93a6880283660af33f6884ab32e4f653) — Require physically accessible storage for GoalDeliverGoodsHousehold (21:50 UTC).
- [`e3be504b`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/e3be504b055ff3c0d986369e7bc7a8598de1b7a8) — Require physically accessible storage for GoalBringBackResourcesHome (21:50 UTC).
- [`9cb3d712`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/9cb3d71243eeb2d765e52c8ab8c592b95a760d26) — Require physically accessible storage for GoalDeliverResourcesShop (21:50 UTC).
- [`7d8d32fd`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/7d8d32fd1e4bbc8f3dcf5f0a30c60bfaa4112f25) — Require physically accessible storage for GoalGetGoodsForHousehold (21:50 UTC).
- [`785d6b0e`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/785d6b0eae347d49aa7bec3c5a429ea3ea9d2370) — Require physically accessible storage for GoalGetResourcesForShops (21:50 UTC).
- [`0a14734c`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/0a14734c9e3a8b1cbf2df45ee7ca3349265be0f5) — Validate real line of sight for villager stock transfers (21:49 UTC).
- [`3639b429`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/3639b4292c21a0800a0db2c1a91c0c65674fd576) — Add automatic Millenaire build and installation scripts (21:33 UTC).
- [`1cd1f1c6`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/1cd1f1c6bb4cbcc5f888f34b11975ae53172251c) — Accurately describe old mining tick interval (21:11 UTC).
- [`6d02d89f`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/6d02d89f45aac7b1ec624f701e817c1e48bbbcf2) — Clarify mining duration tick units in the playtest audit (21:10 UTC).
- [`f105ce96`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/f105ce9621c854d7fb6535f3d8920f00555a01a2) — Honor goal durations already converted from milliseconds to game ticks (21:10 UTC).
- [`8dbacb74`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/8dbacb7404f34a14d074ec607ecf9e0695fd2d53) — Track reported navigation, production and debug-mode gameplay issues (21:09 UTC).
- [`19e57881`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/19e578810ca39d5b8c43e0d3695f47a0c201dd2a) — Respect configured mining duration for quarry and ore goals (21:08 UTC).
- [`68d7977c`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/68d7977c8c87dc0214eb3714c561090d593ea786) — Prevent overlapping village claim radii for natural and manual generation (21:07 UTC).
- [`218c93e2`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/218c93e24255de8dc44e6e1b98f622b7c4be647c) — Use shared door/gate leases and unblock stalled approach routes (21:06 UTC).
- [`6b481aa7`](https://github.com/pewpewbs/Millenaire_mod_updating/commit/6b481aa738e1f7319a9f4900f276f2fb826dddf4) — Add shared server-side door and gate access leases (21:06 UTC).

## Правила ведення журналу надалі

1. **З кожною зміною коду** доповнювати верхній розділ `[Unreleased]`: коротко описувати вплив на гру, категорію (додано / змінено / виправлено), технічний компонент, статус тестування та посилання на коміт після його створення.
2. Якщо фікс спершу експериментальний — явно вказувати **«підготовлено, не перевірено»**. Після компіляції й ігрової перевірки змінювати статус, не заднім числом.
3. Не записувати як поточну функцію рішення, що було **відкочене або замінене**; залишати їх у покомітній історії з позначкою про скасування.
4. Для **справжнього випуску** переносити відповідні записи з `[Unreleased]` в окремий розділ із **реальним** значенням `mod_version`, датою релізу й Git-тегом (якщо він створений). Не змінювати версію без фактичної зміни `gradle.properties`.
5. Після злиття гілки в `main` зберігати записи, але оновлювати статус гілки; `main` не вважається зміненим, доки merge не виконано.
