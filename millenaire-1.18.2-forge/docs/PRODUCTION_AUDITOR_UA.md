# Статичний аналізатор виробництва Millénaire

Інструмент: tools/audit_production.py. Python 3.10+; стандартна бібліотека, опціонально Pillow для PNG. Не є частиною src/main, не пакується у звичайний Forge JAR, не виконується у Minecraft. Не змінює конфігурації, лише записує JSON-звіт за --out.

## Запуск

З кореня Git checkout:

```powershell
cd millenaire-1.18.2-forge
python tools/audit_production.py
```

Звіт: tools/reports/production_audit.json. --culture mayan (фільтр консолі, JSON охоплює всі культури), --strict (код виходу 1 тільки для ERROR), --out <path> (інший звіт), --data/--java (інші локальні джерела). Опціональні маркери джерел у PNG:

```powershell
python -m pip install Pillow
python tools/audit_production.py --scan-png
```

Регресійні тести (штучні конфігурації, без Forge):
```powershell
python tools/tests/test_production_audit.py
```

## Що аналізує

- Усі TXT профілі у cultures/*/villagers; назви static Java goals із Goal.java, generic goals із goals/generic*/.
- Generic mining/harvesting/gather/slaughter, crafting input/output, cooking input і відома таблиця можливих результатів; seed та collect_good як потреби, а не виробництво. Обмежений перелік явно відомих Java outputs, інші hardcoded/loot outputs не реконструюються.
- itemlist.txt: зіставлення ключів, legacy id/meta та особливість wood_any; повна реконструкція Minecraft flattening/recipe registry не реалізована.
- Building variants, initial/upgrade residents, custom building TXT, village/lonebuilding references centre/start/core/secondary/extra/never; approximate initial and possible roles, не гарантія, що всі варіанти та upgrades одночасно доступні.
- shops deliverto/buys/sells, traded_goods buying price/foreign merchant price. Trade listing — можливий канал, не доказ реального імпорту.
- Граф role -> goal -> input/output, потенційне замикання рецептів до фіксованої точки (без зовнішніх seeds і з purchase/foreign seeds); цикли без seed не вважаються досяжними у цій моделі. Виробничі constraints, пріоритети та limits збережено у goal_catalog.
- Перевірки відомих return assignments, furnace maintenance scope/partner, fuel supply, shopping supplies, unknown goals/keys, недійсних quantities та можливих розривів starting supply.
- --scan-png читає RGB source markers із blocklist.txt і шукає їх у непрозорих пікселях стандартних plan PNG. Наявність маркера в будь-якому варіанті/upgrade не доводить, що він присутній у конкретній будівлі поточного рівня; відсутність — REVIEW.

## Інтерпретація

ERROR — структурна суперечність у проіндексованих bundled даних (невідомий goal, дубльований goal key, непозитивна/невалідна quantity), потребує перевірки фактичного loader і custom overrides перед виправленням. REVIEW — кандидат на ручний аудит; не автоматичний bug. Порядок завдань, реальні NPC, доступність печі, паливо, робочі точки, pathfinding, баланс кількостей, trade throughput, runtime loot/recipe registry, custom overlays, точні витрати будівель із планів PNG та реальний стан світу не симулюються. Граф є потенційним, не доказом повної автономності економіки. Поле line вказує перше входження відповідного параметра (0 для загального висновку).

Не змінювати рецепти чи професії автоматично за REVIEW. Спочатку перевірити джерело, роль, призначення будівлі, можливість торгівлі, існуючий код і реальний лог. Наступний крок після першого повного запуску — відсортувати знахідки та перевіряти конкретні ланцюжки за доказами.

## Статус

Гілка fix/villager-door-access-and-village-territory. 76954f0, 54c1ec2, f5ece59, 7612ce7 — інструмент; 8efed87 — сім синтетичних тестів (stage/chain, missing return/unknown goal, maintenance scope, quantity, wood aliases, cycle, PNG transparency). Повторно прочитано GitHub source, 12/12 статичних перевірок наявності функцій. Повний Python запуск і синтетичні тести ще не виконано: контейнер не має мережевого доступу до GitHub, локального checkout усіх конфігурацій немає. Кількість findings/профілів фактичного звіту ще не виміряно. Ігровий Java-код, конфігурації, build.gradle і main не змінено.
