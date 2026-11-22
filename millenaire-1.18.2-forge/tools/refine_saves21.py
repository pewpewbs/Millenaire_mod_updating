from pathlib import Path
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge/src/main/java/org/millenaire/common')
p=r/'utilities/AtomicSaveFile.java';s=p.read_text();s=s.replace('public void commit() throws IOException {','public void commit() throws IOException {\n            if (CAPTURE.get()!=null) throw new IllegalStateException("Close capture before commit");');s=s.replace('@Override public void close() throws IOException { flush(); }','@Override public void close() throws IOException { flush(); }\n                    @Override public void write(byte[] bytes, int offset, int length) throws IOException { out.write(bytes, offset, length); }');p.write_text(s)
p=r/'village/Building.java';s=p.read_text();s=s.replace('this.pathsChanged = true;\n','this.pathsChanged = true;\n         this.markSaveNeeded();\n');p.write_text(s)
p=r/'village/ConstructionIP.java';s=p.read_text()
for method in ['clearBblocks()', 'clearBuildingLocation()', 'incrementBblockPos()', 'startNewConstruction(BuildingLocation bl, BuildingBlock[] bblocks)']:
 a='public void '+method+' {';assert a in s;s=s.replace(a,a+'\n        this.townHall.markSaveNeeded();')
s=s.replace('public void clearBblocks() {\n        this.townHall.markSaveNeeded();','public void clearBblocks() {\n        this.townHall.markSaveNeeded();\n        this.bblocksChanged = true;')
p.write_text(s)
print('Dirty revisions cover construction/roads; bulk writes preserved')
