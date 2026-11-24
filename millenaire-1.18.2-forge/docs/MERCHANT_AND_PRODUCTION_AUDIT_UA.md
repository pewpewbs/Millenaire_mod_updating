# Міжпоселенська торгівля та виробничі ланцюжки — 24.11.2026

## Scope

Продовження LOCAL_LOGISTICS_AUDIT_UA.md після погодження користувачем. Виправляємо підтверджені дефекти передачі товарів купцями та generic mining; починаємо предметну перевірку профілів усіх семи культур. Не змінюємо navigation stage 2, алгоритм пошуку шляхів, ціни, рецепти або систему харчування з backlog.

## Merchant stock transfer

Вихідний Building.moveMerchant і swapMerchants ігнорували фактичний результат storeGoods. Swap додатково переносив першу партію до того, як звільнявся другий склад. Враховувався запит на кількість, а не фактичне перенесення. Count-based reconstruction також не зберігає довільні ItemStack tags.

Реалізація:
- 6b37eb4, 73fb0d8, 691320b: MerchantStockTransfer читає конкретні ItemStack усіх зареєстрованих chest slots обох вибраних inns. Відсутня/недоступна скриня не вважається порожньою. Збережено наявну семантику Point.getMillChest для вибраного віддаленого inn, без вимоги активності обох поселень; не додаємо окремих forced chunk tickets. Реальний distant/chunk-load сценарій ще потрібно перевірити.
- Стан планується на detached copies до будь-якого запису. Move залишає pre-existing destination stock; swap планує обидва склади порожніми перед вставкою cargo. Same-item/same-tags merging, stack/container limits, canPlaceItem, exact copies. Спільний chest між двома будівлями відхиляється; повторний chest у межах одного складу враховується один раз. Перед записом перевіряється, що snapshot не змінився. Якщо весь вантаж не поміщається, операція відкладена цілком; це навмисна conservative policy, щоб купець не залишив частину вантажу у старому inn.
- Existing GoodsTransaction знімком обох chest inventories забезпечує rollback при винятку до commit; результат перевіряється після запису, invalidate кешів, setChanged. Це atomic inventory operation в межах звичайного server-thread виконання, не гарантія crash consistency між region files, saveTownHall і merchant record migration.
- 2e63c07: Building.transferMerchantStock — preconditions на merchant records/town halls/sleeping positions; move/swap викликають його перед міграцією жителя. Імпорт/експорт — тільки успішно committed outward/inward maps. Облік за InvItem (історична семантика лічильників), самі ItemStacks переносяться без реконструкції. Відкладення при LogMerchant >=1: MERCHANT-STOCK-DEFERRED: DESTINATION_CAPACITY / SOURCE_CAPACITY / STOCK_UNLOADED_OR_MISSING / SHARED_CHEST / STOCK_CHANGED. При move позначаємо обидві будівлі для save і скидаємо nbNightsMerchant обох. Не гарантуємо транзакційне відновлення merchant records після винятку вже після inventory commit; це залишок для окремого persistence audit.
- 8d880c2, 16a72ed: місцевий merchant не обирає delivery до переповненого inn/TH, забір export обмежено місцем в inn з урахуванням carried cargo, фізичний доступ до selling point перевіряється існуючим WorkAccess. Не змінено торгові ціни, торгові relations, random inn selection, reserve/target quantities, окремий алгоритм distant demand. Кілька видів вантажу можуть оцінювати одні й ті самі вільні slots; реальна доставка залишає невміщений вантаж у NPC завдяки попередньому fix.

## Перевірка профілів і generic mining

Цільово переглянуто 14 різних профілів усіх семи культур (не всі 285):
- Byzantine: fisherman (fish -> fishraw -> cookfish -> fishcooked + tendfurnace; обидва outputs delivery), smelter (sand/clay/cobblestone -> cooksandbyz/cookclay/cookstonebyz; required sand/clay/cobblestone та wood), artisanwife (household supply / workshop goals).
- Indian: indian_miner (minestone, minesand, minesandstone, mineredsandstone, cookstone, повернення відповідних видобутих ресурсів).
- Inuit: inuit_fisherman (fishinuit -> fishraw / boneblock / seafood_raw; return), inuit_chef (required fishraw, seafood_raw, meats, potato, wood; cookfish/cookfishslab і страви).
- Japanese: japanese_miner, japanese_loneminer, japanese_peasantwife. Обидва шахтарі мають minegravel, але lone profile не мав bringBackHomeGood=gravel. aa990cd додає відсутній запис (як у звичайного шахтаря); не додає нового видобування.
- Mayan: mayan_miner (minestone/minesandstone/minesand, cookstone/cooksand, wood reserves, return).
- Norman: miner (minestone/minesand, cookstone, return), glassblower (required sand і wood, cooksand, stained glass recipes).
- Seljuk: turk_cook і turk_cookwife (kitchen cooking/tend; wife має requiredGood=wood,4 та supply goals; не дублюємо його без доказу).
Прочитано 8 mining/cooking configs: minestone, minesandstone, minegravel, cookstone, cooksand, cooksandbyz, cookfishslab, cookFishKitchen. Для спеціальних balance pairs (Byzantine sandstone/glass тощо) немає достатнього підтвердження задуму, тому не змінюємо. Japanese sand supply і планові source markers окремо перевірити; не додавати minesand до профілю лише за наявністю cooksand.

73c2fee: GoalGenericMining раніше повертав true з isDestPossibleSpecific незалежно від джерела, тоді як getDestination міг повернути null. Тепер під час eligibility перевіряються зареєстровані matching sourceTypes і фактичний BlockState у завантаженому чанку; early return при першому придатному джерелі, без додаткового RNG. Destination відфільтровує building limits за validateDest. Перед видачею loot перевіряється live source і реєстрація в цільовій будівлі, чинний validateDest; якщо source змінився, ресурси не видаються. Не змінюємо stuckAction/navigation access policy; зауваження: physical availability та route availability різні. Diagnostic reason NO_REGISTERED_MATCHING_LIVE_SOURCE. Не змінено loot amounts, priorities, duration та test yield policy. Generic crafting вже використовує GoodsTransaction для rollback при невміщеній готовій продукції; доцільно окремо оптимізувати selection, не забороняючи рецепти, які самі звільняють slots.

## Перевірки

Повторно отримано змінені файли GitHub; 16/16 статичних інтеграційних перевірок пройшли. Додано tools/tests/test_merchant_production_wiring.py та MerchantStockTransferSmoke.java (9 container-level сценаріїв, потребує Minecraft/Forge dev runtime з ініціалізованими реєстрами; не виконано). Повний Forge build, Python suite, ігрова перевірка, profiling — не виконано. Не заявляти end-to-end acceptance.

Acceptance:
1. Move: 64 cobble до складу без місця — deferred, 64 на джерелі, нуль імпорту/експорту, record не мігрує. Partial merge 20+44=64 — успіх. Swap двох повних складів однакового розміру — успіх без вільних проміжних slots. Неоднакова місткість — reject, обидва склади незмінні. Різні теги не зливаються; rollback на simulated failure. Missing/shared chest — reject. Перевірити з реальними Millenaire locked chests, active/distant inactive villages, reload та статистикою. Окремо перевірити save/crash semantics.
2. Merchant visits: inn full / TH full / зміна room під час руху, foreign demand, резерви будівництва; відсутність зникнення або дублювання товарів, без remote access.
3. Generic mining: джерело відсутнє, видалене, змінене або unloaded; eligibility false, діагностика пояснює, loot не нараховується. Після відновлення джерела goal знов доступний. Кілька work buildings із різними quota/source points, тест 1× і 20×.
4. Japanese lone miner: gravel накопичується та повертається до складу. Перевірити MILL-CONTENT-SYNC для старих світів. Якщо local profile customized, перевірити manifest conflict та вручну merge.
5. Наступна ітерація: повний machine-readable goal/role/recipe/plan/source/shop dependency audit усіх 285 profiles; доуточнити реальні production inputs, collection goals, trade goods, source marker availability, village type specializations, direct storeGoods calls (включно з harvest/gather), кеш після furnace changes, merchant record persistence. Не вважати перевірку 14 profiles повним аудитом 7 культур.

Гілка fix/villager-door-access-and-village-territory; main не змінювалася.
