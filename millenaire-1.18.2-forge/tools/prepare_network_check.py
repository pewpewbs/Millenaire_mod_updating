from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[1];cp=(root/'build/migration-classpath.txt').read_text().strip();out=root/'build/network-check';out.mkdir(exist_ok=True)
q=lambda s:'"'+str(s).replace('\\','/')+'"'
args=['-encoding','UTF-8','-classpath',cp,'-d',out,root/'src/main/java/org/millenaire/common/network/MillPacketPayload.java',root/'tools/NetworkPayloadCheck.java']
(root/'build/network-check-javac.args').write_text('\n'.join(q(v) for v in args),encoding='utf-8')
(root/'build/network-check-java.args').write_text('\n'.join(q(v) for v in ['-cp',str(out)+';'+cp,'org.millenaire.common.network.NetworkPayloadCheck']),encoding='utf-8')
