"""Japanese wheat is an initial construction input, not an optional luxury."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/todeploy/millenaire/cultures/japanese/villages"

def entries(name):
    return [line.strip() for line in (ROOT / (name + ".txt")).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.lstrip().startswith("//")]

class JapaneseWheatBootstrap(unittest.TestCase):
    def test_standard_villages_start_with_wheat_and_grove(self):
        for name in ("nogyo", "gunji", "seiji", "trading", "japanese_controlled"):
            with self.subTest(village=name):
                lines=entries(name)
                self.assertEqual(lines.count("start=japanesefarm"),1)
                self.assertIn("start=japanesegrove",lines)
                self.assertNotIn("never=japanesefarm",lines)

    def test_custom_controlled_keeps_manual_layout(self):
        lines=entries("japanese_customcontrolled")
        self.assertIn("customcentre=custom_villagecentre",lines)
        self.assertIn("core=japanesefarm",lines)
        self.assertNotIn("start=japanesefarm",lines)

if __name__ == "__main__":
    unittest.main()
