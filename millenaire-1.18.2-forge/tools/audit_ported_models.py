from pathlib import Path
import json,zipfile
root=Path(__file__).resolve().parents[1];assets=root/'src/main/resources/assets'
jar=zipfile.ZipFile(root/'.gradle-user-home/caches/forge_gradle/minecraft_repo/versions/1.18.2/client.jar');vanilla=set(jar.namelist())
names=['sod','snowbrick','icebrick','stone_deco','wood_deco','earth_deco','extended_mud_brick','slab_stone_deco','slab_wood_deco']+json.loads((root/'migration/half-slab-models.json').read_text())+json.loads((root/'migration/path-models.json').read_text())
names+=json.loads((root/'migration/oriented-slab-models.json').read_text())
names+=json.loads((root/'migration/painted-stair-wall-models.json').read_text())
names+=json.loads((root/'migration/pane-stone-models.json').read_text())
names+=json.loads((root/'migration/production-models.json').read_text())
block_only=set(json.loads((root/'migration/crop-models.json').read_text()))
names+=list(block_only)+json.loads((root/'migration/rosette-models.json').read_text())
names+=json.loads((root/'migration/firepit-import-models.json').read_text())
names+=json.loads((root/'migration/tree-statue-models.json').read_text())
names+=json.loads((root/'migration/mock-models.json').read_text())
names+=json.loads((root/'migration/bed-models.json').read_text())
block_only.update(['villagebannerstanding','villagebannerwall','culturebannerstanding','culturebannerwall'])
names+=['villagebannerstanding','villagebannerwall','culturebannerstanding','culturebannerwall']
names+=['locked_chest']
visited=set();missing=[]; roots=set()

def load_model(name):
    ns,path=name.split(':',1) if ':' in name else ('minecraft',name)
    relative=f'assets/{ns}/models/{path}.json'
    if ns=='minecraft':
        return json.loads(jar.read(relative)) if relative in vanilla else {}
    file=assets/ns/'models'/(path+'.json')
    return json.loads(file.read_text(encoding='utf-8-sig')) if file.exists() else {}

def check_bindings(name):
    chain=[]; current=name; seen=set()
    while current and current not in seen:
        seen.add(current); data=load_model(current);chain.append(data);current=data.get('parent')
    textures={}
    for data in reversed(chain):textures.update(data.get('textures',{}))
    elements=next((data['elements'] for data in chain if 'elements' in data),[])
    refs=[face['texture'] for element in elements for face in element.get('faces',{}).values()]
    if 'particle' in textures:refs.append('#particle')
    for ref in refs:
        seen=set(); value=ref
        while value and value.startswith('#') and value not in seen:
            seen.add(value);value=textures.get(value[1:])
        if not value or value.startswith('#'):missing.append(('unresolved texture binding',name,ref))
def model(name):
    if name in visited:return
    visited.add(name)
    namespace,path=name.split(':',1) if ':' in name else ('minecraft',name)
    relative=f'assets/{namespace}/models/{path}.json'
    if namespace=='minecraft' and path in ['builtin/entity','builtin/generated']:return
    if namespace=='minecraft':
        if relative not in vanilla:missing.append(('model',name))
        return
    file=assets/namespace/'models'/(path+'.json')
    if not file.exists():missing.append(('model',name));return
    data=json.loads(file.read_text(encoding='utf-8-sig'))
    if 'parent' in data:model(data['parent'])
    for override in data.get('overrides',[]):
        roots.add(override['model']);model(override['model'])
    for texture in data.get('textures',{}).values():
        if texture.startswith('#'):continue
        ns,tex=texture.split(':',1) if ':' in texture else ('minecraft',texture)
        rel=f'assets/{ns}/textures/{tex}.png'
        exists=rel in vanilla if ns=='minecraft' else (assets/ns/'textures'/(tex+'.png')).exists()
        if not exists:missing.append(('texture',texture,str(file)))
for name in names:
    if name not in block_only:
        roots.add('millenaire:item/'+name);model('millenaire:item/'+name)
    data=json.loads((assets/'millenaire/blockstates'/(name+'.json')).read_text())
    for value in data.get('variants',{}).values():
        for item in value if isinstance(value,list) else [value]:
            roots.add(item['model']);model(item['model'])
    for part in data.get('multipart',[]):
        value=part['apply']
        for item in value if isinstance(value,list) else [value]:
            roots.add(item['model']);model(item['model'])
for item_name in ['villagebanner','culturebanner','bannerpattern','yumibow','inuitbow','seljukbow','mayanquestcrown','purse']+['bannerpattern_'+str(i) for i in range(1,28)]:
    roots.add('millenaire:item/'+item_name);model('millenaire:item/'+item_name)
for name in sorted(roots):check_bindings(name)
print('Models traversed',len(visited),'missing references',len(missing))
for item in missing:print(item)
(root/'migration/ported-model-audit.json').write_text(json.dumps({'model_count':len(visited),'missing':missing},indent=2),encoding='utf-8')
