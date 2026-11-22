from pathlib import Path
import collections,json,re
base=Path('work/localization'); stage=base/'staged'
catalog=json.loads((base/'catalog.json').read_text(encoding='utf-8'))
token=re.compile(r'(<(?!\$)[^>\n]+>|\$[A-Za-z_][A-Za-z_0-9]*\$?|%(?:\d+\$)?[-+0-9.]*[sdif]|§.|https?://\S+|\\[nrt]|\bMill[ée]naire\b|\bMinecraft\b)')
checked=0
for document in catalog:
 text=(stage/document['path']).read_text(encoding='utf-8')
 assert '\ufffd' not in text,document['path']
 if 'values' in document:
  output=json.loads(text)
  extras=json.loads((base/'extra-ui.json').read_text(encoding='utf-8'))
  assert output.keys()==document['values'].keys()|extras.keys()
  for key,part in document['values'].items():
   assert collections.Counter(token.findall(part['body']))==collections.Counter(token.findall(output[key])),key
   assert output[key].strip() or not part['body'].strip(),key
 else:
  lines=text.splitlines(); assert len(lines)==len(document['rows']),document['path']
  for row,line in zip(document['rows'],lines):
   if 'raw' in row:assert row['raw']==line,(document['path'],row)
   else:
    assert line.startswith(row['parts'][0]['prefix']),(document['path'],row)
    original=''.join(p['prefix']+p['body']+p['suffix'] for p in row['parts'])
    assert collections.Counter(token.findall(original))==collections.Counter(token.findall(line)),(document['path'],row)
    delimiters=''.join(p['prefix']+p['suffix'] for p in row['parts'])
    assert delimiters.count(';')==line.count(';'),(document['path'],row)
    # Keys and native-language dialogue prefixes must stay byte-for-byte intact.
    if '=' in row['parts'][0]['prefix']:assert line.split('=',1)[0]==row['parts'][0]['prefix'].split('=',1)[0]
 checked+=1
print('PASS:',checked,'localized documents: syntax, line structure, identifiers, placeholders, Unicode and JSON keys')
