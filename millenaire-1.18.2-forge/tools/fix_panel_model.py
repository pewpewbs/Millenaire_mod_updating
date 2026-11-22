from pathlib import Path
import json, shutil

root = Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
assets = root / 'src/main/resources/assets/millenaire'
for relative in ['blockstates/panel.json', 'models/block/panel.json']:
    source = assets / relative
    backup = root / 'migration/before-panel-model' / relative
    backup.parent.mkdir(parents=True, exist_ok=True)
    if not backup.exists():
        shutil.copy2(source, backup)
    data = json.loads(source.read_text(encoding='utf-8-sig'))
    if relative.startswith('blockstates/'):
        for variant in data['variants'].values():
            variant['model'] = 'millenaire:block/panel'
    else:
        data['textures']['all'] = 'minecraft:block/oak_planks'
    source.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
print('Panel blockstate now resolves its existing model and modern oak texture.')
