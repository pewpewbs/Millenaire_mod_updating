"""Run against compiled mod + actual Forge 1.18.2 classes. Run build first."""
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
jdk = next((root.parent / '.millenaire-tools').glob('jdk-17*')) / 'bin'
out = root / 'build/navigation-geometry-check'
out.mkdir(parents=True, exist_ok=True)
cp = str(root / 'build/classes/java/main') + ';' + (root / 'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(jdk/'javac.exe'), '-encoding', 'UTF-8', '-cp', cp, '-d', str(out),
                str(root/'tools/tests/NavigationGeometryCheck.java')], check=True, cwd=root)
subprocess.run([str(jdk/'java.exe'), '-cp', str(out)+';'+cp, 'NavigationGeometryCheck'], check=True, cwd=root)
