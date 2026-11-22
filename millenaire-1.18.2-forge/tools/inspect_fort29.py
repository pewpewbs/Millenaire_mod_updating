from pathlib import Path
import struct,zlib,gzip,shutil,json
root=Path('C:/Users/sviat/AppData/Roaming/ATLauncher/instances/Codefication1182/saves/Новий світ (1)')
copy=Path('work/fort29-save');
if not copy.exists():shutil.copytree(root,copy)
class NBT:
 def __init__(self,b):self.b=b;self.i=0
 def read(self,n):v=self.b[self.i:self.i+n];self.i+=n;return v
 def num(self,f):return struct.unpack('>'+f,self.read(struct.calcsize('>'+f)))[0]
 def string(self):return self.read(self.num('H')).decode('utf-8')
 def tag(self,t):
  if t in range(1,7):return self.num({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
  if t==7:return self.read(self.num('i'))
  if t==8:return self.string()
  if t==9:
   st=self.num('b');n=self.num('i');return [self.tag(st) for _ in range(n)]
  if t==10:
   d={}
   while True:
    st=self.num('b')
    if st==0:return d
    name=self.string();d[name]=self.tag(st)
  if t in (11,12):return [self.num('i' if t==11 else 'q') for _ in range(self.num('i'))]
  raise ValueError(t)
 def root(self):t=self.num('b');self.string();return self.tag(t)
def chunk(cx,cz):
 p=copy/'region'/f'r.{cx//32}.{cz//32}.mca'
 if not p.exists():return {}
 b=p.read_bytes();loc=int.from_bytes(b[((cx%32)+(cz%32)*32)*4:][:4],'big');off=(loc>>8)*4096
 if not off:return {}
 n=int.from_bytes(b[off:off+4],'big');typ=b[off+4];raw=b[off+5:off+4+n]
 return NBT(zlib.decompress(raw) if typ==2 else gzip.decompress(raw) if typ==1 else raw).root()
blocks={}
for cx in range(-2,4):
 for cz in range(34,40):
  c=chunk(cx,cz)
  for s in c.get('sections',[]):
   sy=s['Y'];bs=s.get('block_states',{});pal=bs.get('palette',[]);data=bs.get('data',[])
   if not pal or not 3<=sy<=5:continue
   bits=max(4,(len(pal)-1).bit_length());per=64//bits
   for i in range(4096):
    pi=((data[i//per] & ((1<<64)-1))>>((i%per)*bits))&((1<<bits)-1) if data else 0
    x=cx*16+i%16;y=sy*16+i//256;z=cz*16+(i//16)%16
    blocks[f'{x},{y},{z}']=pal[pi]
Path('work/fort29-blocks.json').write_text(json.dumps(blocks),encoding='utf-8')
def symbol(x,y,z):
 b=blocks.get(f'{x},{y},{z}',{}).get('Name','?').split(':')[-1]
 if b in ['air','cave_air','tall_grass','grass']:return ' '
 if 'door' in b:return 'D'
 if 'gate' in b:return 'G'
 if 'fence' in b or 'wall' in b:return 'F'
 if 'stair' in b:return 's'
 if 'slab' in b:return '_'
 if 'chest' in b:return 'C'
 if 'water' in b:return '~'
 return '#'
for y in [65,66,67,68,69]:
 print('HEIGHT',y,'x -5..30 z580..612')
 for z in range(580,613):print(z,''.join(symbol(x,y,z) for x in range(-5,31)))
print('COPY',copy.resolve())
