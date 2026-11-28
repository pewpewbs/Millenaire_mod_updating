# Виправлення за першим повним production audit — 24.11.2026

## Вихідні дані

Користувач успішно запустив python tools/audit_production.py --scan-png після encoding fix (3ec9848). production_audit.json: 7 культур, 285 профілів, 497 унікальних завдань, 3 ERROR і 353 REVIEW. Категорії: STARTING_SUPPLY_UNCONFIRMED 231, NO_CONFIGURED_PRODUCER 50, MISSING_HOME_RETURN 29, SHOP_SUPPLY_UNCONFIRMED 29, NO_KNOWN_TRANSPORT_GOAL 9, NO_HOME_SOURCE_MARKER_SEEN 3, COOK_BALANCE_DIFFERS_FROM_INPUT 2. Не вважати REVIEW підтвердженими багами.

## Підтверджені конфлікти

GoalGeneric.loadGenericGoals використовує lower-case filename stem як глобальний key і goals.put. MakeSandstone і MakeSugar у seljuk перекривали інші рецепти. Перейменовано на makesandstoneseljuk і makesugarseljuk; оновлено turk_loneminer, turk_stonecrafter, turk_cook. Перевірено initial.tag=stonecrafter у lone mine. Збережено пропорції, tags, priority, duration, limits; label/sentence keys збережено (для sugar явно задано старі). Видалено byte-identical inuit/makebonemeal, canonical genericcrafting/makebonemeal лишається.

8506a1c: migration ContentBundleUpdater. Manifest відомі файли обробляє як раніше; для pre-manifest retired seljuk paths перевіряються SHA-256 stock bytes (LF/CRLF), identical bonemeal — хеш canonical bundle. Лише незмінені файли переміщуються в резервну копію і видаляються. Custom edited stock retained з MILL-CONTENT-CONFLICT, потребує ручного узгодження; custom directory не змінюється. Активний goalKey у save може до наступного циклу використовувати старий key; перевірити після reload.

94292a3, 6d7ef7a: Byzantine cookclay balance clay,brick; cooksandbyz sand,glass. Попередні пари використовували не input рецептів, могли блокувати їх при достатній сировині. Не змінювали minimumToCook, quotas, tags.

## Джерела в планах шахт

a2cee7a: 4 binary PNG base plans, по 2 пікселі stonesource → sandsource або gravelsource. Оригінальні PNG отримані з GitHub; decoded blob SHA підтверджено. Локально прочитано всі 13 upgrade PNG цих варіантів, використано floor coordinate layout PngPlanLoader, base/upgrade startlevel, white/transparent empty overlay, clear red. Обрано вже наявні source positions на явній твердій опорі, із сусідньою відкритою двоблоковою колоною, без перезапису source/support на досліджених рівнях. Це статична перевірка планів, не runtime navigation validation. Координати нижче: zero-based floor, horizontal image-local x, image y; widthPos у Java дзеркальний.
- Indian quarry A width14 start -6: (1,2,5), (1,6,6); 8 stone +2 sand; stone/sandstone/red sandstone sources лишаються.
- Indian quarry B width14 start -6: (2,3,3), (3,9,8); 7 stone +2 sand.
- Japanese lonemine A width20 start -6, upgrade1 -8: (7,4,2), (7,2,4); 5 stone +2 gravel; oak plank support.
- Mayan mayanmining A width12 start -8: (2,4,5), (2,3,9); 17 stone +2 sand +2 sandstone.
Збережено dimensions, RGB values інших пікселів, решту plan files. Pillow перевірив рівно 2 змінені пікселі на кожен base plan, нові git blob SHA підтверджені після commit. Це змінює розподіл двох існуючих виробничих точок; не додає нових блоків до проходів. Не форсувати перебудову існуючих світів: saved resManager source registrations та фактичні блоки старих шахт ще потребують окремої міграційної перевірки. 

## Доставка

9 profiles (5857e36, 75a8d6f, 8dee76d, db6ba7e, b366d5b, 4d38e5c, 95812e0, cff6116, 8cd5605):
- turk_lumberman: collectGood/bringBackHomeGood wood_any, охоплює 6 wood variants через existing wildcard handling.
- inuit_lumberman і inuit_lonelumberman: додано return wood_acacia/wood_darkoak.
- inuit_lonefisherman: bringBackResourcesHome + fishraw/boneblock, аналогічно звичайному fisherman. Не додаємо cooking task без підтвердження печі/потреби.
- inuit_sanduitwife: potato/pathsnow.
- farmiller_wife_byzantine, turk_peasant: wheat.
- lonemonk Norman та turk_shepherd: bone (bonus output).
Залишено seed return warnings для перевірки семантики; не забираємо seeds автоматично у жителя, який сам висаджує їх. Дитячі/декоративні/спеціальні roles і інші supply warnings не змінюємо без доказу.

## Аналізатор

30c71ab: potential entity drops від slaughteranimal/huntmonster з урахуванням collectGood; підтверджено MillVillager tick pickup навіть без GoalGatherGoods. Позначено inferred і collection=potential_drop, не гарантія actual loot або наявності mob. Shearing — white wool та declared color variants; Java gatherbrick -> mudbrick. Planting inputs можуть залишатися в інвентарі, marked retained_planting_input; це не доказ відсутності surplus. PNG alpha=255 узгоджено з PngPlanLoader. Quartz ore marker mapping додано. Не симулювати live mob drop tables, actual import, construction costs або pathfinding.

## Перевірки та обмеження

Після змін GitHub tree (не truncated): 455 generic goal TXT, 0 duplicate lowercase stems; 4 нові PNG SHA відповідають локально перевіреним файлам. Додано test_production_repairs.py (5 tests), нові synthetic tests у test_production_audit.py і 3 migration checks у ContentBundleUpdaterSmoke.java. Повний Forge build, нові Python suites та повний повторний production audit ще не виконані: немає локального checkout, DNS до GitHub із контейнера недоступний. Не заявляти новий фактичний REVIEW count.

Наступний запуск із project root:
python tools/tests/test_production_audit.py
python tools/tests/test_production_repairs.py
python tools/audit_production.py --scan-png --strict

Порівняти з baseline, перевірити залишкові errors, початкове забезпечення/торгівлю, runtime source registration старих шахт. Не маскувати REVIEW шляхом blanket config edits. Після git pull і build/install перевірити MILL-CONTENT-SYNC, conflicts, actual NPC production у всіх 7 cultures. Навігаційні алгоритми, main та food economy backlog не змінювали.
