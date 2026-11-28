from pathlib import Path
import json
import re
import unittest

ROOT = Path(__file__).resolve().parents[2] / "src/main"
JAVA = ROOT / "java/org/millenaire/common"
LANG = ROOT / "resources/assets/millenaire/lang"

class EntityLocalizationTests(unittest.TestCase):
    def test_decoration_types_have_translations(self):
        src = (JAVA / "entity/EntityWallDecoration.java").read_text(encoding="utf-8")
        name = src.split("protected net.minecraft.network.chat.Component getTypeName()",1)[1].split(
            "public Item getDropItem()",1)[0]
        suffixes = re.findall(r'case [A-Z_]+ -> "([a-z]+)"', name)
        self.assertEqual(len(set(suffixes)), 10)
        self.assertIn("TranslatableComponent", name)
        for locale in ("en_us", "uk_ua"):
            lang = json.loads((LANG / (locale + ".json")).read_text(encoding="utf-8"))
            for suffix in [""] + suffixes:
                key = "entity.millenaire.walldecoration" + ("." + suffix if suffix else "")
                self.assertTrue(lang.get(key), key)

    def test_known_entity_registry_ids_have_fallback_names(self):
        sources = [JAVA / "entity/MillVillager.java", JAVA / "entity/EntityWallDecoration.java",
                   JAVA / "forge/Mill.java"]
        ids = set()
        for source in sources:
            code = source.read_text(encoding="utf-8")
            ids.update(re.findall(r'(?:GENERIC_[A-Z_]+|WALL_DECORATION|ENTITY_TARGETED_[A-Z_]+)\s*=\s*new ResourceLocation\((?:"millenaire"|MODID),\s*"([^"]+)"\)', code))
        self.assertIn("walldecoration", ids)
        self.assertIn("genericvillager", ids)
        for locale in ("en_us", "uk_ua"):
            lang = json.loads((LANG / (locale + ".json")).read_text(encoding="utf-8"))
            for entity_id in ids:
                self.assertTrue(lang.get("entity.millenaire." + entity_id), entity_id)

if __name__ == "__main__":
    unittest.main()
