from pathlib import Path
import json,re,collections
L=Path('work/localization');stage=L/'staged'
catalog=json.loads((L/'catalog.json').read_text(encoding='utf-8'))
translated={}
for lang in ['uk','ru']:
 p=L/(lang+'-nllb-translated.json')
 if not p.exists() and lang=='ru':p=L/(lang+'-translated.json')
 translated.update(json.loads(p.read_text(encoding='utf-8')))
glossary={}
for line in ((L/'glossary.tsv').read_text(encoding='utf-8')+'\n'+(L/'reviewed-dialogue.tsv').read_text(encoding='utf-8')).splitlines():
 if not line.strip():continue
 source,uk,ru=line.split('\t');glossary[('uk',source)]=uk;glossary[('ru',source)]=ru
TOKEN=re.compile(r'(<(?!\$)[^>\n]+>|\$[A-Za-z_][A-Za-z_0-9]*\$?|%(?:\d+\$)?[-+0-9.]*[sdif]|§.|https?://\S+|\\[nrt]|\bMill[ée]naire\b|\bMinecraft\b)')
stats=collections.Counter();review=[];errors=[]
def render(part,lang,path):
 source=part['body'];result=glossary.get((lang,source),part.get('translation'))
 if source.startswith('/'):result=source.replace(' or ',' або ' if lang=='uk' else ' или ')
 if result is None:result=translated[part['job']]
 result=re.sub(r'(Mill[ée]naire|Minecraft)(?=[А-Яа-яІіЇїЄєҐґ])',r'\1 ',result)
 result=re.sub(r'(?<=[А-Яа-яІіЇїЄєҐґ])(Mill[ée]naire|Minecraft)',r' \1',result)
 # These separators are parsed as syntax in Millenaire text files.
 if not path.endswith('.json'):result=''.join(piece if TOKEN.fullmatch(piece) else piece.replace(';',',').replace('=','：').replace('\n',' ') for piece in TOKEN.split(result))
 if collections.Counter(TOKEN.findall(source))!=collections.Counter(TOKEN.findall(result)):errors.append((path,part.get('id'),source,result))
 stats[lang+'_text_units']+=1
 plain=TOKEN.sub('',result)
 if re.search('[A-Za-z]{3}',plain):review.append({'lang':lang,'file':path,'id':part.get('id'),'source':source,'translation':result})
 return result
for document in catalog:
 p=stage/document['path'];p.parent.mkdir(parents=True,exist_ok=True);lang=document['lang']
 if 'values' in document:
  data={key:render(part,lang,document['path']) for key,part in document['values'].items()}
  for key,values in json.loads((L/'extra-ui.json').read_text(encoding='utf-8')).items():data[key]=values[1 if lang=='uk' else 2]
  p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
  locale='uk_ua' if lang=='uk' else 'ru_ru'
  legacy=stage/f'todeploy/millenaire/languages/{lang}/{locale}.lang'
  legacy.write_text('\n'.join(key+'='+value for key,value in data.items())+'\n',encoding='utf-8')
 else:
  lines=[]
  for row in document['rows']:
   lines.append(row['raw'] if 'raw' in row else ''.join(part['prefix']+render(part,lang,document['path'])+part['suffix'] for part in row['parts']))
  p.write_text('\n'.join(lines)+'\n',encoding='utf-8')
 stats[lang+'_files']+=1
(L/'latin-review.json').write_text(json.dumps(review,ensure_ascii=False,indent=2),encoding='utf-8')
(L/'coverage.json').write_text(json.dumps(dict(stats),indent=2),encoding='utf-8')
print(dict(stats),'Latin-script review entries:',len(review))
(L/'token-errors.json').write_text(json.dumps(errors,ensure_ascii=False,indent=2),encoding='utf-8')
assert not errors, f'{len(errors)} token mismatches; see token-errors.json'
