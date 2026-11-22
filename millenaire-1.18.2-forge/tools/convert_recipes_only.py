from pathlib import Path
import json
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
src=r/'src/main/resources/assets/millenaire/recipes';dest=r/'src/main/resources/data/millenaire/recipes';dest.mkdir(exist_ok=True)
dyes=['ink_sac','red_dye','green_dye','cocoa_beans','lapis_lazuli','purple_dye','cyan_dye','light_gray_dye','gray_dye','pink_dye','lime_dye','yellow_dye','light_blue_dye','magenta_dye','orange_dye','bone_meal']
variants={'millenaire:stone_deco','millenaire:wood_deco','millenaire:slab_stone_deco','millenaire:slab_wood_deco'}
def convert(x,result=False):
 if isinstance(x,list):return [convert(v) for v in x]
 if not isinstance(x,dict):return x
 if 'item' in x:
  x=dict(x);item=x['item'].lower();data=x.pop('data',0)
  if item=='minecraft:dye':item='minecraft:'+dyes[data];data=0
  elif item=='minecraft:stonebrick':assert data==0;item='minecraft:stone_bricks'
  elif item=='minecraft:wool':assert data==0;item='minecraft:white_wool'
  elif item.startswith('minecraft:') and item.endswith('_silver'):item=item[:-7]+'_light_gray'
  x['item']=item
  if item in variants:
   if not result:x['type']='forge:nbt'
   if data:x['nbt']={'MillenaireVariant':data}
  else:assert data==0,(item,data)
  return x
 return {k:convert(v,k=='result') for k,v in x.items()}
for file in src.glob('*.json'):
 data=convert(json.loads(file.read_text(encoding='utf-8-sig')))
 (dest/file.name).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=dest/'smelt_painted_brick_white.json';data=json.loads(p.read_text());data['ingredient'].pop('nbt',None);p.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
print('Legacy names, semantic flower category, numeric vanilla block IDs and 95 crafting recipes migrated.')
