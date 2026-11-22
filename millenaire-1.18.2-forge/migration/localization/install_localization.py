from pathlib import Path
import json,shutil,datetime
root=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
stage=Path('work/localization/staged')
resources=root/'src/main/resources'
coverage=json.loads(Path('work/localization/coverage.json').read_text())
assert coverage['uk_files']==coverage['ru_files'] and coverage['uk_files']>60
assert coverage['uk_text_units']==coverage['ru_text_units']
current=json.loads((resources/'assets/millenaire/lang/en_us.json').read_text(encoding='utf-8'))
extra=json.loads(Path('work/localization/extra-ui.json').read_text(encoding='utf-8'))
for locale in ['uk_ua','ru_ru']:
 assert json.loads((stage/f'assets/millenaire/lang/{locale}.json').read_text(encoding='utf-8')).keys()==current.keys()|extra.keys()
backup=root/'migration'/('before-language-resources-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S'))
for source in stage.rglob('*'):
 if not source.is_file():continue
 rel=source.relative_to(stage);target=resources/rel
 if target.exists():
  saved=backup/rel;saved.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(target,saved)
 target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
english=resources/'assets/millenaire/lang/en_us.json'
saved=backup/'en_us.json';saved.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(english,saved)
data=json.loads(english.read_text(encoding='utf-8'))
extras=json.loads(Path('work/localization/extra-ui.json').read_text(encoding='utf-8'))
data.update({key:values[0] for key,values in extras.items()})
english.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for locale in ['uk_ua','ru_ru']:
 translated=json.loads((resources/f'assets/millenaire/lang/{locale}.json').read_text(encoding='utf-8'))
 assert translated.keys()==data.keys()
(resources/'todeploy/millenaire/localization-version.txt').write_text('uk-ru-2026-11-16-1\n',encoding='utf-8')
print('Installed:',coverage,'Backup:',backup)
