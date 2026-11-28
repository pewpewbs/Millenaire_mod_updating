# Аудит внутрішньої логістики — 24.11.2026

## Обсяг

Наступний етап після універсального оновлення контенту та діагностики AI: спільні локальні supply/transport goals усіх культур. Це перша ітерація аудиту виробничих ланцюжків, а не вичерпна перевірка кожної професії, рецепта і поселення. Не змінювати алгоритми навігації, стадію фізичної доступності сходів/дверей/драбин, ціни, задані culture quotas або backlog продовольчої економіки.

## Підтверджені дефекти вихідного коду

- MillVillager.putInBuilding спочатку забирає запитану кількість з інвентарю, ігнорує фактичний результат Building.storeGoods та повертає вилучену кількість замість збереженої. Частково або повністю заповнена скриня призводить до втрати вантажу.
- Wildcard meta=-1 — запит сімейства, не реальний предмет. Загальна видача могла перетворювати вилучені різновиди в один key. Окремий legacy log workaround не поширювався на всі інші families.
- GoalGetResourcesForShops не перевіряв місткість магазину; при виконанні порівнював позицію shop.getPos() з goal selling point, а не ідентичність source building. Пріоритети повторно рахували один запас для кількох магазинів.
- GoalDeliverResourcesShop обирав перший магазин із сумісним товаром без перевірки місця, міг повторно планувати марні доставки.
- GoalGetGoodsForHousehold мав поріг goods/2 (для 1 — нуль, тому постачання ніколи не починалося), забирав до повної норми замість реального дефіциту, не віднімав уже наявний вантаж; priority перевіряв лише shops, хоча destination міг бути будь-якою будівлею. Після часткової доставки залишок не мав спеціального шляху повторного постачання.
- GoalBringBackResourcesHome не враховував місце в скринях при оцінці придатного вантажу.
- Building.takeGoods викликав furnace/firepit helpers без meta, а ті порівнювали тільки Item. Це не узгоджено з конкретними stock keys. Cast firepit із довільного block entity міг викликати exception для застарілої resource point.
- MillCommonUtilities.putItemsInChest міг не розпізнати неканонічний empty stack та зменшити вже overfilled stack при обчисленні від'ємного залишку.

## Реалізація

- 85e16b4, 659dd47: putInBuilding повертає тільки фактично збережену кількість; залишок повертає у власний інвентар NPC. Wildcard pickup/deposit працює з concrete keys, без синтетичного meta=-1. Не масштабувати логістику TestTuning.yield. Це збереження кількості при штатному частковому записі, не універсальна crash-atomic транзакція: виняток усередині storage write, arbitrary NBT та сторонні inventory hooks потребують окремого аудиту.
- ce5719a, 3ffb6e2, 565c334: Building.storageRoom та stockKeys, єдиний принцип місткості stack і container, пропуск unloaded chunks у новій перевірці місткості, empty stack detection, overfilled stack не зменшується.
- bd7dff1, f5a464c: exact metadata у furnace/firepit output take, без небезпечного cast.
- a8f5855, 580c9d7, 629e798: LocalLogistics — спільні розрахунки. Для household target використовує суму поточних requiresGoods мешканців (узгоджується з наявним per-resident резервуванням, це рішення даної ітерації; перевірити баланс). Поріг restock ceil(target/2), точний дефіцит, cargo, existing reserved surplus, actual room. Wildcard вибирає конкретний доступний різновид і поважає aggregate та exact reserve. Не сумує вільні slots для різновидів. Для shop не додає нові target quotas: використовує shopNeeds, existing nbGoodAvailable(...forShop=true), room і наявний cargo.
- 33876ce, 57b3328: pickup тільки для іншого магазину із місцем; delivery обирає магазин з найбільшою оцінкою придатного вантажу; priority не множить той самий cargo на кількість магазинів. Перевірки повторюються при виконанні.
- 9483ddc, 60f5b7e, 2dfa625, d5ba2fd: household supply за дефіцитом, retry retained cargo через вже існуючий nextGoal=delivergoodshousehold, не додає нових професій або окремого AI goal. Delivery обмежується актуальним target.
- db20460: повернення ресурсів додому враховує реальну місткість. Усі п'ять локальних транспортних goals перевіряють actual selling point + existing isWithinActionRange/WorkAccess перед дією. Не змінює шлях, не дозволяє remote transfer.
- Для goal diagnostics додано NO_EXPORTABLE_STOCK_OR_SHOP_CAPACITY, NO_RECEIVABLE_SHOP_CARGO_OR_DELIVERY_COOLDOWN, NO_HOUSEHOLD_DEFICIT_OR_AVAILABLE_SUPPLY_OR_STORAGE, NO_DELIVERABLE_GOODS_OR_HOME_STORAGE. Це групові причини, не детальний per-item звіт.

## Перевірка

Повторно отримано 10 змінених Java-файлів із GitHub; 16/16 статичних умов інтеграції пройшли. Додано tools/tests/test_local_logistics_wiring.py. Повний Python suite, javac/Forge build, Minecraft runtime та профілювання не виконано. Не заявляти, що всі виробничі ланцюжки семи культур пройшли end-to-end перевірку.

Acceptance:
1. У світі з чітами 5–20× yield заповнити скриню до стану 20 units room, NPC має 64 units; після доставки 20 у скрині, 44 залишаються в інвентарі. Повна скриня — 0 втрат, 0 успішно доставлених. Повторити для готової риби, каменю, різних log/wool variants і overfilled stack.
2. Два магазини, один заповнений: не відправляти NPC до заповненого, коли другий може прийняти товар. Якщо обидва заповнені, не забирати додатковий товар. Перевірити зміну місткості між плануванням та дією, не дублювати предмети.
3. Household target 10, вдома 6, NPC несе 2 — максимальний pickup при вже активному постачанні 2, якщо джерело має достатній вільний запас. Target 1 при 0 запускає постачання. Кілька мешканців, кілька кур'єрів, wildcard reserves, джерело з будівельними резервами, повний склад і повторна доставка.
4. Перевірити поведінку на старому світі у всіх семи культурах. Фізична доступність, no-path і routing залишаються окремими питаннями; не обходити їх віддаленою доставкою.
5. Порівняти витрати часу на goal selection із попередньою версією: нові capacity scans коротко замикаються при нульовій потребі/запасі, але витрати на великих поселеннях ще не виміряні. Оцінка місткості для різних предметів може використовувати ті самі empty slots; фактичне виконання rechecks і зберігає залишок, проте повноцінного між-NPC reservation/cargo tracking поки немає.

## Наступний етап

Підтверджено окрему проблему в Building.moveMerchant/swapMerchants: обидва методи ігнорують результат destination.storeGoods; swap переносить першу партію до того, як звільнено другий склад. Це може втрачати товари та некоректно рахувати імпорт/експорт. Не виправлено в цьому етапі, щоб не змішувати локальну логістику з міграцією merchant records. Наступна ітерація: перевірити обидва inventory snapshots і transaction/rollback для merchant transfer, звітність фактичних кількостей; після цього — конкретні профілі/recipes/shops і залежності виробничих ланцюгів культур. Також перевірити direct production writes, які не використовують putInBuilding, та cache invalidation після зовнішніх змін скринь/печей.

Гілка fix/villager-door-access-and-village-territory. main не змінено.
