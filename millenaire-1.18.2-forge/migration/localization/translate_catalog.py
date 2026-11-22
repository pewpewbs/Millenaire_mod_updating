from pathlib import Path
import json,re,sys,time
import ctranslate2,sentencepiece
L=Path('work/localization');lang=sys.argv[1];engine=sys.argv[2] if len(sys.argv)>2 else 'argos'
if engine=='nllb':
 model=Path('work/translation-tools/nllb');sp=sentencepiece.SentencePieceProcessor(model_file=str(model/'sentencepiece.bpe.model'))
 mt=ctranslate2.Translator(str(model),device='cpu',compute_type='int8',intra_threads=6)
else:
 model=next((Path('work/translation-tools')/lang).rglob('sentencepiece.model')).parent
 sp=sentencepiece.SentencePieceProcessor(model_file=str(model/'sentencepiece.model'))
 mt=ctranslate2.Translator(str(model/'model'),device='cpu',compute_type='int8',intra_threads=4)
jobs=json.loads((L/'jobs.json').read_text(encoding='utf-8'))
suffix='-nllb' if engine=='nllb' else ''
cachefile=L/(lang+suffix+'-segments.json');cache=json.loads(cachefile.read_text(encoding='utf-8')) if cachefile.exists() else {}
for line in ((L/'glossary.tsv').read_text(encoding='utf-8')+'\n'+(L/'reviewed-dialogue.tsv').read_text(encoding='utf-8')).splitlines():
 fields=line.split('\t')
 if len(fields)==3:cache[fields[0]]=fields[1 if lang=='uk' else 2]
token=re.compile(r'(<(?!\$)[^>\n]+>|\$[A-Za-z_][A-Za-z_0-9]*\$?|%(?:\d+\$)?[-+0-9.]*[sdif]|§.|https?://\S+|\\[nrt]|\bMill[ée]naire\b|\bMinecraft\b)')
def parts(text):
 result=[]
 for chunk in token.split(text):
  if not chunk:continue
  if token.fullmatch(chunk) or not re.search('[A-Za-z]',chunk):result.append(('literal',chunk));continue
  # Preserve punctuation and whitespace while translating full sentences where possible.
  for sentence in re.split(r'(?<=[.!?])(?=\s+[A-Z])',chunk):
   if not re.search('[A-Za-z]',sentence):result.append(('literal',sentence));continue
   words=sentence.split();current=[]
   for word in words:
    if current and len(sp.encode(' '.join(current+[word])))>180:
     result.append(('translate',' '.join(current)));result.append(('literal',' '));current=[]
    current.append(word)
   if current:
    if sentence[:1].isspace():result.append(('literal',' '))
    result.append(('translate',' '.join(current)))
    result.append(('literal',' '))
 return result
plans={key:parts(value['source']) for key,value in jobs.items() if value['lang']==lang}
needed=list(dict.fromkeys(text for segments in plans.values() for kind,text in segments if kind=='translate' and text not in cache))
print(lang,'jobs',len(plans),'segments pending',len(needed),flush=True);start=time.time()
for offset in range(0,len(needed),24):
 batch=needed[offset:offset+24]
 encoded=[(['eng_Latn']+sp.encode(s,out_type=str)+['</s>']) if engine=='nllb' else sp.encode(s,out_type=str) for s in batch]
 assert all(len(s)<=200 for s in encoded)
 kwargs={'target_prefix':[['ukr_Cyrl' if lang=='uk' else 'rus_Cyrl'] for _ in batch]} if engine=='nllb' else {}
 outputs=mt.translate_batch(encoded,beam_size=2 if engine=='nllb' else 4,max_decoding_length=384,disable_unk=True,**kwargs)
 for source,output in zip(batch,outputs):
  text=sp.decode(output.hypotheses[0][1:] if engine=='nllb' else output.hypotheses[0]).strip()
  if not text:raise RuntimeError('Empty translation: '+source)
  cache[source]=text
 cachefile.write_text(json.dumps(cache,ensure_ascii=False,indent=2),encoding='utf-8')
 if offset%240==0:print(lang,offset+len(batch),'/',len(needed),'elapsed',round(time.time()-start),flush=True)
translated={key:''.join(cache[text] if kind=='translate' else text for kind,text in segments).strip() for key,segments in plans.items()}
(L/(lang+suffix+'-translated.json')).write_text(json.dumps(translated,ensure_ascii=False,indent=2),encoding='utf-8')
print(lang,'DONE',len(translated),flush=True)
