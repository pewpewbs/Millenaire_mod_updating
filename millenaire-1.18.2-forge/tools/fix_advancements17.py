from pathlib import Path
import json,shutil,re
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');res=r/'src/main/resources';b=r/'migration/before-advancements17';b.mkdir(exist_ok=False)
en=json.loads((res/'assets/millenaire/lang/en_us.json').read_text(encoding='utf-8'))
data={}
def put(k,name,desc):
 data['advancements.'+k+'.name']=name;data['advancements.'+k+'.desc']=desc
rows=[
('firstcontact','Millénaire','Познайомтеся з мешканцем поселення Millénaire.'),
('cresus','Мільйонер Millénaire','Заробіть золотий деньє.'),
('summoningwand','Поклик поселення','Купіть жезл закликання та створіть із його допомогою поселення.'),
('amateurarchitect','Архітектор-аматор','Створіть поселення з власноруч збудованою ратушею.'),
('medievalmetropolis','Середньовічний мегаполіс','Керуйте поселенням, у якому живе щонайменше 100 мешканців.'),
('thequest','Завдання','Виконайте завдання від мешканця поселення.'),
('maitreapenser','Духовний наставник','Зустріньте садху або алхіміка.'),
('explorer','Дослідник','Дослідіть відокремлену будівлю.'),
('marcopolo','Марко Поло','Відвідайте поселення трьох різних культур в одному світі.'),
('magellan','Фернан Магеллан','Відвідайте поселення всіх культур в одному світі.'),
('selfdefense','Самооборона','Здолайте бандита.'),
('pantheon','Пантеон','Відвідайте таємничий Пантеон.'),
('darkside','Темний бік','Убийте мешканця поселення.'),
('scipio','Карфаген має бути зруйнований','Знищте поселення за допомогою жезла заперечення.'),
('attila','Аттіла','Здобудьте репутацію «Бич Божий» у трьох культурах.'),
('cheers','За Minecraft!','Випийте алкогольний напій із Millénaire.'),
('hired','Найманий помічник','Найміть мешканця поселення.'),
('masterfarmer','Досвідчений фермер','Посадіть сільськогосподарську культуру з Millénaire.'),
('greathunter','Нічого не марнуйте','Скористайтеся знаннями інуїтів, щоб добути м’ясо з незвичного джерела.'),
('friendindeed','Справжній друг','Досягніть рівня репутації «Друг поселення» в одному з поселень.'),
('rainbow','Барви веселки','Перефарбуйте індійську цеглу за допомогою кольорової побілки.'),
('marvel_norman','Нотр-Дам у Minecraft','Допоможіть норманам збудувати собор.'),
('wq_norman','Великий вибух','Завершіть розділ «Алхімік» у сюжетному завданні «Створення світу».'),
('wq_indian','Заборонені знання','Завершіть розділ «Садху» у сюжетному завданні «Створення світу».'),
('wq_mayan','Божественна корона','Завершіть розділ «Повалений король» у сюжетному завданні «Створення світу».'),
('puja','Трохи допомоги від богів','Заплатіть пандиту за проведення пуджі.'),
('sacrifice','Кров для голодних богів','Заплатіть шаману за проведення жертвопринесення.'),
('viking','Повернення вікінгів','Організуйте успішний набіг силами поселення під вашим керуванням.'),
('seljuk_istanbul','Стамбул, а не Константинополь','Організуйте успішний набіг вашого сельджуцького поселення на візантійське.'),
('byzantines_nottoday','Не сьогодні!','Відбийте напад сельджуків силами візантійського поселення під вашим керуванням.'),
('mp_weapon','Воїн із далеких країв','Атакуйте іншого гравця зброєю ближнього бою з Millénaire.'),
('mp_hiredgoon','Найманий убивця','Найміть мешканця поселення та накажіть йому атакувати іншого гравця.'),
('mp_raidonplayer','Усі ваші бази належать мені','Організуйте успішний набіг вашого поселення на поселення під керуванням іншого гравця.'),
('mp_friendlyvillage','Утопія','Здобудьте статус «Друг поселення» там, де його мають щонайменше троє інших гравців.'),
('mp_neighbourtrade','Додатковий прибуток','Нехай інший гравець поторгує з поселенням під вашим керуванням.')]
for row in rows:put(*row)
cultures={
 'norman':('норманському','норманським','норманське','Друзі назавжди','Норманська довершеність','Його Величність Стів'),
 'indian':('індійському','індійським','індійське','Брат поселення','Великий архітектор','Маленький магараджа'),
 'mayan':('поселенні майя','лідером майя','поселення майя','Священні узи','Найкращий управитель','Бог-король поселення'),
 'japanese':('японському','японським','японське','Коннічіва','Схід сонця','Кульбабовий трон'),
 'byzantines':('візантійському','візантійським','візантійське','Вітаю, римлянине!','Новий Новий Рим','Народжений поза порфірою'),
 'inuits':('поселенні інуїтів','лідером інуїтів','поселення інуїтів','Вітаю, інуїте!','Разом безпечніше','Справжній інуїт'),
 'seljuk':('сельджуцькому','сельджуцьким','сельджуцьке','Шлях братерства','Нова Конья','Сельджуцький бей')}
for k,(loc,leader,settlement,repname,completename,leadername) in cultures.items():
 in_place=loc if loc.startswith('поселенні') else loc+' поселенні'
 if k=='mayan':dative='поселенню майя'
 elif k=='inuits':dative='поселенню інуїтів'
 else:dative=loc+' поселенню'
 lead=leader if leader.startswith('лідером') else leader+' лідером'
 place=settlement if settlement.startswith('поселення') else settlement+' поселення'
 put('rep_'+k,repname,'Досягніть рівня репутації «Один із нас» у '+in_place+'.')
 put('complete_'+k,completename,'Допоможіть '+dative+' завершити всі будівлі, маючи в ньому репутацію «Один із нас» або вищу.')
 put('leader_'+k,leadername,'Станьте '+lead+' та створіть '+place+' під своїм керуванням.')
expected={k for k in en if k.startswith('advancements.')};assert set(data)==expected,(expected-set(data),set(data)-expected)
def save(p,fn):
 dest=b/p.relative_to(r);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,dest);p.write_text(fn(p.read_text(encoding='utf-8')),encoding='utf-8')
def modern(s):
 d=json.loads(s);d.update(data);return json.dumps(d,ensure_ascii=False,indent=2)+'\n'
save(res/'assets/millenaire/lang/uk_ua.json',modern)
def legacy(s):
 lines=[x for x in s.splitlines() if not x.split('=',1)[0].strip().startswith('advancements.')]
 return '\n'.join(lines)+'\n'+'\n'.join(k+'='+v for k,v in data.items())+'\n'
for p in [res/'assets/millenaire/lang/uk_ua.lang',res/'todeploy/millenaire/languages/uk/uk_ua.lang']:save(p,legacy)
save(res/'todeploy/millenaire/localization-version.txt',lambda s:'uk-ru-2026-11-17-5\n')
Path('work/advancements17-reviewed.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
print('Reviewed and synchronized',len(data),'Ukrainian advancement strings;',len(data)//2,'achievements.')
