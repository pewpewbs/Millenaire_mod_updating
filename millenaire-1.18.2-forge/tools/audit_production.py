#!/usr/bin/env python3
"""Read-only audit of bundled Millenaire economy. Python 3.10+, optional Pillow."""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
from dataclasses import dataclass, field
import json
from pathlib import Path
import re
import sys

PROJECT = Path(__file__).resolve().parents[1]
DEFAULT_DATA = PROJECT / "src/main/resources/todeploy/millenaire"
DEFAULT_JAVA = PROJECT / "src/main/java/org/millenaire/common"
COOK_OUTPUT = {
    "cobblestone": "stone", "sand": "glass", "clay": "brick",
    "fishraw": "fishcooked", "beefraw": "beefcooked",
    "chickenmeat": "chickenmeatcooked", "porkchops": "porkchopscooked",
    "mutton": "cooked_mutton", "potato": "baked_potato",
    "seafood_raw": "seafood_cooked", "ironore": "iron",
    "wood_any": "charcoal", "mudbrick": "paintedbrickwhite",
}
# Explicitly limited Java-goal knowledge; unknown Java outputs are not errors.
JAVA_OUTPUT = {
    "fish": {"fishraw"}, "fishinuit": {"fishraw", "boneblock"},
    "choptrees": {"wood", "wood_pine", "wood_birch", "wood_jungle", "wood_acacia", "wood_darkoak"}, "gathersilk": {"silk"},
    "gathersnails": {"dye_purple"}, "harvestcocoa": {"dye_brown"},
    "gatherbrick": {"mudbrick"},
    "harvestwarts": {"netherwart"}, "harvestsugarcane": {"sugarcane"},
}
# Potential vanilla drops, not explicit loot guarantees. The role's collectGood
# determines whether it can pick these up (MillVillager also collects on tick).
ANIMAL_DROPS = {
    "cow": {"beefraw", "leather"}, "chicken": {"chickenmeat", "feather"},
    "pig": {"porkchops"}, "sheep": {"mutton", "wool_white", "wool_black",
                                  "wool_gray", "wool_lightgray", "wool_brown", "wool_pink"},
    "squid": {"dye_black"}, "rabbit": {"rabbit", "rabbit_hide"},
}
MONSTER_DROPS = {"bone", "rottenflesh", "arrow", "string", "gunpowder"}
SOURCE_MARKERS = {
    "minecraft:" + block: block + "source"
    for block in ("stone", "sand", "sandstone", "clay", "gravel", "granite",
                  "diorite", "andesite", "snow", "ice", "red_sandstone", "quartz")
}
SOURCE_MARKERS["minecraft:red_sandstone"] = "redsandstonesource"
SOURCE_MARKERS["minecraft:nether_quartz_ore"] = "quartzsource"
TRANSPORT = {"bringbackresourceshome", "gathergoods", "deliverresourcesshop",
             "delivergoodshousehold", "gethousethresources", "getgoodshousehold"}

def norm(value):
    return value.strip().lower()

@dataclass
class Config:
    path: Path
    fields: dict[str, list[tuple[str, int]]] = field(default_factory=lambda: defaultdict(list))
    def values(self, key):
        return [v for v, _ in self.fields.get(key, [])]
    def last(self, key, default=""):
        return (self.values(key) or [default])[-1]
    def line(self, key):
        return (self.fields.get(key) or [("", 0)])[0][1]

def read_legacy_text(path):
    """Preserve UTF-8/BOM and support older Western-European config files.

    Never modify source bytes. Report fallback decoding so non-ASCII legacy
    identifiers can be checked manually if necessary.
    """
    raw = path.read_bytes()
    try:
        return raw.decode("utf-8-sig")
    except UnicodeDecodeError:
        for encoding in ("cp1252", "latin-1"):
            try:
                decoded = raw.decode(encoding)
            except UnicodeDecodeError:
                continue
            print(f"ENCODING_FALLBACK {encoding}: {path}", file=sys.stderr)
            return decoded
    raise AssertionError("Latin-1 must decode every byte")


def read_config(path):
    obj = Config(path)
    for number, line in enumerate(read_legacy_text(path).splitlines(), 1):
        line = line.strip()
        if not line or line.startswith(("//", "#")) or "=" not in line:
            continue
        key, value = line.split("=", 1)
        obj.fields[norm(key)].append((norm(value), number))
    return obj

def txts(path):
    return sorted(p for p in path.rglob("*.txt") if p.is_file())

def first(value):
    return norm(value.split(",", 1)[0])

def items(config, *keys):
    return {first(v) for k in keys for v in config.values(k) if first(v)}

def csv_items(config, *keys):
    return {norm(i) for k in keys for v in config.values(k) for i in v.split(",") if norm(i)}

def positive(value):
    try:
        terms = [int(t.strip()) for t in value.split("*")]
        return bool(terms) and all(t > 0 for t in terms)
    except ValueError:
        return False

def index_items(path):
    result = {}
    for line in read_legacy_text(path).splitlines():
        if not line.strip() or line.lstrip().startswith(("//", "#")):
            continue
        cols = line.split(";")
        if len(cols) >= 3:
            result[norm(cols[0])] = (norm(cols[1]), norm(cols[2]))
    return result

def matches(a, b, aliases):
    if a == b:
        return True
    x, y = aliases.get(a), aliases.get(b)
    if not x or not y:
        return False
    # Legacy oak-log wildcard includes both old log families in Java.
    if x == ("minecraft:log", "-1") and y[0] in ("minecraft:log", "minecraft:log2"):
        return True
    if y == ("minecraft:log", "-1") and x[0] in ("minecraft:log", "minecraft:log2"):
        return True
    return x[0] == y[0] and (x[1] == y[1] or "-1" in (x[1], y[1]))

def covered(item, pool, aliases):
    return any(matches(item, other, aliases) for other in pool)

def reachable(edges, roles, aliases, seeds=()):
    """Potential recipe closure, deliberately not a live-world simulation."""
    available = set(seeds)
    relevant = [edge for edge in edges if edge["role"] in roles]
    while True:
        added = {edge["output"] for edge in relevant if all(
            covered(i, available, aliases) for i in edge["inputs"])} - available
        if not added:
            return available
        available.update(added)

def scope(cfg):
    if cfg.last("townhallgoal") == "true":
        return "townhall"
    return cfg.last("buildingtag") or "home"

def plan_key(path):
    # Standard variant files, e.g. mayanmining_A.txt.
    return re.sub(r"_[a-z]$", "", norm(path.stem))

def read_trade(path):
    buy, foreign, all_goods = set(), set(), set()
    if path.is_file():
        for line in read_legacy_text(path).splitlines():
            if not line.strip() or line.lstrip().startswith(("//", "#")):
                continue
            cols = [norm(v) for v in line.split(",")]
            if len(cols) < 3:
                continue
            all_goods.add(cols[0])
            # Positive buying price is a possible player supply channel,
            # not proof that a player or merchant actually supplies the item.
            if positive(cols[2]):
                buy.add(cols[0])
            if len(cols) > 5 and positive(cols[5]):
                foreign.add(cols[0])
    return buy, foreign, all_goods

@dataclass
class Goal:
    key: str
    kind: str
    cfg: Config | None
    inputs: set[str] = field(default_factory=set)
    outputs: set[str] = field(default_factory=set)
    inventory: bool = False
    source: str = ""
    uncertain: bool = False
    drop_outputs: set[str] = field(default_factory=set)

def load_goals(data, java):
    result, duplicates = {}, []
    source = (java / "goal/Goal.java").read_text(encoding="utf-8")
    for name in re.findall(r'goals\.put\("([^"]+)"', source):
        name = norm(name)
        result[name] = Goal(name, "java", None, outputs=JAVA_OUTPUT.get(name, set()),
                            inventory=name in JAVA_OUTPUT and name != "harvestwarts", uncertain=True)
    if "huntmonster" in result:
        result["huntmonster"].drop_outputs = MONSTER_DROPS
    for path in txts(data / "goals"):
        kind = path.relative_to(data / "goals").parts[0]
        if not kind.startswith("generic"):
            continue
        cfg, key = read_config(path), norm(path.stem)
        goal = Goal(key, kind, cfg)
        if kind == "genericcrafting":
            goal.inputs, goal.outputs = items(cfg, "input"), items(cfg, "output")
        elif kind == "genericmining":
            goal.outputs, goal.inventory = items(cfg, "loot"), True
            goal.source = cfg.last("sourceblockstate")
        elif kind == "genericcooking":
            goal.inputs = items(cfg, "itemtocook")
            goal.outputs = {COOK_OUTPUT[i] for i in goal.inputs if i in COOK_OUTPUT}
            goal.uncertain = True  # actual output is resolved by Minecraft recipes
        elif kind in ("genericharvesting", "genericgatherblocks", "genericslaughteranimal"):
            goal.outputs = items(cfg, "harvestitem", "irrigationbonuscrop", "bonusitem")
            goal.inventory = cfg.last("collectinbuilding") != "true"
            goal.uncertain = True
            if kind == "genericslaughteranimal":
                goal.drop_outputs = ANIMAL_DROPS.get(cfg.last("animalkey").split(":")[-1], set())
        elif kind == "genericplanting":
            goal.inputs = items(cfg, "seed")
        elif kind == "generictakefrombuilding":
            goal.inputs = items(cfg, "collect_good")
            goal.uncertain = True  # transfer, never manufacture goods
        else:
            goal.uncertain = True
        if key in result:
            duplicates.append((cfg, key))
        result[key] = goal
    return result, duplicates

def markers(data):
    palette = {}
    for line in read_legacy_text(data / "blocklist.txt").splitlines():
        if line.lstrip().startswith(("//", "#")):
            continue
        cols = line.split(";")
        if len(cols) > 4 and norm(cols[0]).endswith("source"):
            try:
                rgb = tuple(int(v) for v in cols[4].split("/"))
                if len(rgb) == 3 and all(0 <= c <= 255 for c in rgb):
                    palette[rgb] = norm(cols[0])
            except ValueError:
                pass
    return palette

def scan_plans(root, palette):
    # Optional visual evidence, not a reconstruction of BuildingPlan or level costs.
    try:
        from PIL import Image
    except ImportError:
        return None
    evidence = defaultdict(set)
    for path in sorted(root.rglob("*.png")):
        match = re.fullmatch(r"(.+)_([a-z])(\d+)", norm(path.stem))
        if not match:
            continue
        with Image.open(path) as image:
            colors = image.convert("RGBA").getcolors(image.width * image.height)
        for _, (r, g, b, a) in colors or []:
            if a == 255 and (r, g, b) in palette:
                evidence[match[1]].add(palette[(r, g, b)])
    return evidence

def audit(data, java, scan_png=False):
    aliases = index_items(data / "itemlist.txt")
    goals, duplicates = load_goals(data, java)
    findings, cultures, edges = [], {}, []
    def emit(level, code, culture, cfg, message, key="", village=""):
        findings.append(dict(level=level, code=code, culture=culture, village=village,
                             file=cfg.path.relative_to(data).as_posix(), line=cfg.line(key),
                             message=message))
    for cfg, name in duplicates:
        emit("ERROR", "DUPLICATE_GOAL", "", cfg, name)
    for name, goal in sorted(goals.items()):
        cfg = goal.cfg
        if not cfg:
            continue
        for field_name in ("input", "output", "loot", "itemtocook", "buildinglimit",
                           "townhalllimit", "villagelimit", "harvestitem", "bonusitem"):
            for value in cfg.values(field_name):
                item = first(value)
                if item not in aliases:
                    emit("REVIEW", "UNRESOLVED_ITEM_KEY", "", cfg, item, field_name)
                if field_name in ("input", "output", "loot") and (
                        "," not in value or not positive(value.split(",", 1)[1])):
                    emit("ERROR", "INVALID_QUANTITY", "", cfg, value, field_name)
        if goal.kind == "genericcooking":
            balance = cfg.last("itemsbalance").split(",")
            if len(balance) == 2 and goal.inputs and not covered(balance[0], goal.inputs, aliases):
                emit("REVIEW", "COOK_BALANCE_DIFFERS_FROM_INPUT", "", cfg,
                     f"balance={cfg.last('itemsbalance')}; input={sorted(goal.inputs)}", "itemsbalance")
    for root in sorted((data / "cultures").iterdir()):
        if not root.is_dir() or not (root / "villagers").is_dir():
            continue
        culture = root.name
        role_files = [read_config(p) for p in txts(root / "villagers")]
        roles = {}
        for cfg in role_files:
            key = norm(cfg.path.stem)
            if key in roles:
                emit("REVIEW", "DUPLICATE_ROLE", culture, cfg, key)
            roles[key] = cfg
        plans = defaultdict(list)
        for p in txts(root / "buildings") + txts(root / "custombuildings"):
            plans[plan_key(p)].append(read_config(p))
        plan_roles, role_plans, initial_plan_roles = defaultdict(set), defaultdict(set), defaultdict(set)
        for key, variants in plans.items():
            for cfg in variants:
                for field_name, values in cfg.fields.items():
                    if field_name.rsplit(".", 1)[-1] not in ("male", "female"):
                        continue
                    for value, _ in values:
                        resident = first(value)
                        if resident in roles:
                            plan_roles[key].add(resident)
                            role_plans[resident].add(key)
                            if field_name in ("initial.male", "initial.female", "male", "female"):
                                initial_plan_roles[key].add(resident)
                        elif resident and resident not in ("none", "null"):
                            emit("REVIEW", "UNRESOLVED_RESIDENT", culture, cfg, resident, field_name)
        shops = [read_config(p) for p in txts(root / "shops")]
        shop_needs = set().union(*(csv_items(s, "deliverto") for s in shops))
        buy, foreign, trade_goods = read_trade(root / "traded_goods.txt")
        assigned = {r: set(cfg.values("goal")) for r, cfg in roles.items()}
        producers, consumers = defaultdict(set), defaultdict(set)
        png = scan_plans(root / "buildings", markers(data)) if scan_png else None
        first_edge = len(edges)
        for r, cfg in roles.items():
            active = assigned[r]
            inputs = items(cfg, "requiredgood")
            returned = items(cfg, "bringbackhomegood")
            collected = items(cfg, "collectgood")
            planting_inputs = set().union(*(goals[g].inputs for g in active
                if g in goals and goals[g].kind == "genericplanting"))
            for name in sorted(active):
                goal = goals.get(name)
                if goal is None:
                    emit("ERROR", "UNKNOWN_GOAL", culture, cfg, name, "goal")
                    continue
                inputs.update(goal.inputs)
                # Drops are possible supply only if a matching collector is declared.
                # Sheep can have arbitrary dyes; count natural/declared wool variants
                # rather than claim that every color is actually available.
                pickup_outputs = {i for i in goal.drop_outputs if i in aliases and covered(i, collected, aliases)}
                direct_outputs = set(goal.outputs)
                if name == "shearsheep":
                    direct_outputs.update(i for i in aliases if i.startswith("wool_") and
                        (i == "wool_white" or covered(i, collected | returned, aliases)))
                for output in sorted(direct_outputs | pickup_outputs):
                    inventory_output = goal.inventory or output in pickup_outputs or name == "shearsheep"
                    retained_seed = inventory_output and covered(output, planting_inputs, aliases)
                    producers[output].add(r)
                    edges.append(dict(culture=culture, role=r, goal=name, output=output,
                                      inputs=sorted(goal.inputs), source=goal.source,
                                      inferred=goal.uncertain or output in pickup_outputs or name == "shearsheep",
                                      collection="potential_drop" if output in pickup_outputs else "direct",
                                      retained_planting_input=retained_seed))
                    if inventory_output and not retained_seed and "bringbackresourceshome" in active and not covered(output, returned, aliases):
                        emit("REVIEW", "MISSING_HOME_RETURN", culture, cfg,
                             f"{name} -> {output}; no matching bringBackHomeGood", "goal")
                    elif inventory_output and not retained_seed and not active.intersection(TRANSPORT):
                        emit("REVIEW", "NO_KNOWN_TRANSPORT_GOAL", culture, cfg,
                             f"{name} -> {output}; may be consumed directly", "goal")
                if goal.kind == "genericmining" and png is not None:
                    marker = SOURCE_MARKERS.get(goal.source)
                    # Positive evidence across variants only; absence is review.
                    known_plans = role_plans[r]
                    if marker and known_plans and scope(goal.cfg) == "home" and not any(
                            marker in png.get(p, set()) for p in known_plans):
                        emit("REVIEW", "NO_HOME_SOURCE_MARKER_SEEN", culture, cfg,
                             f"{name}: {marker}; plans={sorted(known_plans)}", "goal")
                if goal.kind == "genericcooking":
                    partners = {r}
                    for p in role_plans[r]:
                        partners.update(plan_roles[p])
                    # Cross-building work may use a worker from a different home.
                    if scope(goal.cfg) != "home":
                        partners = set(roles)
                    if not any(goals[g].kind == "generictendfurnace" and
                               scope(goals[g].cfg) == scope(goal.cfg)
                               for partner in partners for g in assigned[partner] if g in goals):
                        emit("REVIEW", "FURNACE_MAINTENANCE_UNCONFIRMED", culture, cfg,
                             f"{name}: scope={scope(goal.cfg)}; check other workers", "goal")
            for item in inputs:
                consumers[item].add(r)
        for shop in shops:
            for item in sorted(csv_items(shop, "deliverto")):
                if not covered(item, producers, aliases) and not covered(item, buy | foreign, aliases):
                    emit("REVIEW", "SHOP_SUPPLY_UNCONFIRMED", culture, shop,
                         f"{item}; no indexed local producer or player/foreign price", "deliverto")
        culture_edges = edges[first_edge:]
        if any(goals[g].kind == "genericcooking" for gs in assigned.values() for g in gs if g in goals):
            if not covered("wood_any", set(producers) | buy | foreign, aliases):
                cooking_roles = sorted(r for r, gs in assigned.items()
                    if any(g in goals and goals[g].kind == "genericcooking" for g in gs))
                emit("REVIEW", "FURNACE_FUEL_SUPPLY_UNCONFIRMED", culture, roles[cooking_roles[0]],
                     "No indexed log producer or purchase listing; existing fuel and unmodeled sources may cover.")
        for item, needing in sorted(consumers.items()):
            if covered(item, producers, aliases):
                continue
            channel = "player purchase" if covered(item, buy, aliases) else (
                "foreign merchant listing" if covered(item, foreign, aliases) else (
                "trade listing only" if covered(item, trade_goods, aliases) else "not identified"))
            emit("REVIEW", "NO_CONFIGURED_PRODUCER", culture, roles[sorted(needing)[0]],
                 f"{item}; consumers={', '.join(sorted(needing))}; supply={channel}; Java/loot/import may cover")
        villages = [read_config(p) for p in txts(root / "villages") + txts(root / "lonebuildings")]
        village_rows = []
        for cfg in villages:
            name = norm(cfg.path.stem)
            initial = items(cfg, "centre", "start")
            possible = (initial | items(cfg, "core", "secondary", "extra")) - items(cfg, "never")
            initial_roles = set().union(*(initial_plan_roles[p] for p in sorted(initial)))
            possible_roles = set().union(*(plan_roles[p] for p in sorted(possible)))
            for key in sorted(possible - plans.keys()):
                emit("REVIEW", "UNRESOLVED_BUILDING_REFERENCE", culture, cfg, key, village=name)
            for item, needing in sorted(consumers.items()):
                if not needing.intersection(initial_roles):
                    continue
                local_producers = {r for output, rs in producers.items() if matches(item, output, aliases) for r in rs}
                if local_producers.intersection(initial_roles):
                    continue
                later = local_producers.intersection(possible_roles)
                channel = "later building" if later else ("trade listed" if covered(item, trade_goods, aliases) else "unknown")
                emit("REVIEW", "STARTING_SUPPLY_UNCONFIRMED", culture, cfg,
                     f"{item}; starting roles={sorted(needing & initial_roles)}; {channel}; not a runtime deadlock proof",
                     village=name)
            internal = reachable(culture_edges, possible_roles, aliases)
            assisted = reachable(culture_edges, possible_roles, aliases, buy | foreign)
            needed = {item for item, rs in consumers.items() if rs.intersection(possible_roles)}
            village_rows.append(dict(name=name, file=cfg.path.relative_to(data).as_posix(),
                                     initial_buildings=sorted(initial), possible_buildings=sorted(possible),
                                     initial_roles=sorted(initial_roles), possible_roles=sorted(possible_roles),
                                     potential_internal_outputs=sorted(internal),
                                     potential_external_assisted_outputs=sorted(assisted - (buy | foreign)),
                                     unresolved_recipe_inputs=sorted(i for i in needed if not covered(i, assisted, aliases))))
        cultures[culture] = dict(profiles=len(role_files), indexed_roles=len(roles),
                                 plans=len(plans), shops=len(shops), villages=village_rows,
                                 producers={i: sorted(rs) for i, rs in sorted(producers.items())},
                                 consumers={i: sorted(rs) for i, rs in sorted(consumers.items())},
                                 shop_needs=sorted(shop_needs),
                                 shop_catalog={s.path.stem: {"deliverto": sorted(csv_items(s,"deliverto")),
                                     "buys": sorted(csv_items(s,"buys")), "sells": sorted(csv_items(s,"sells"))}
                                     for s in shops}, player_buy=sorted(buy),
                                 foreign_listing=sorted(foreign), png_scanned=png is not None)
    findings.sort(key=lambda f: (f["level"] != "ERROR", f["culture"], f["code"], f["file"], f["message"]))
    catalog = {key: dict(kind=g.kind, inputs=sorted(g.inputs), outputs=sorted(g.outputs),
                         possible_entity_drops=sorted(g.drop_outputs),
                         source=g.source, inferred=g.uncertain,
                         config=g.cfg.path.relative_to(data).as_posix() if g.cfg else None,
                         constraints={k: g.cfg.values(k) for k in ("priority", "priorityrandom",
                             "buildinglimit", "townhalllimit", "villagelimit", "itemsbalance",
                             "minimumtocook", "buildingtag", "requiredtag", "townhallgoal")
                             if g.cfg and g.cfg.values(k)}) for key,g in sorted(goals.items())}
    return dict(schema=1, scope="bundled static data, not live world", goal_catalog=catalog,
                summary=dict(cultures=len(cultures), profiles=sum(c["profiles"] for c in cultures.values()),
                             goals=len(goals), findings=dict(Counter(f["level"] for f in findings))),
                cultures=cultures, edges=edges, findings=findings)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data", type=Path, default=DEFAULT_DATA)
    parser.add_argument("--java", type=Path, default=DEFAULT_JAVA)
    parser.add_argument("--out", type=Path, default=PROJECT / "tools/reports/production_audit.json")
    parser.add_argument("--culture", help="Filter displayed findings; JSON always covers all cultures")
    parser.add_argument("--scan-png", action="store_true", help="Optional Pillow source-marker scan")
    parser.add_argument("--strict", action="store_true", help="Exit 1 for ERROR findings (not REVIEW)")
    args = parser.parse_args()
    result = audit(args.data.resolve(), args.java.resolve(), args.scan_png)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result["summary"], ensure_ascii=False))
    for finding in result["findings"]:
        if args.culture and finding["culture"] not in ("", args.culture):
            continue
        print(f"{finding['level']} {finding['code']} {finding['file']}:{finding['line']} {finding['message']}")
    print(f"JSON report: {args.out}")
    if args.scan_png and not all(c["png_scanned"] for c in result["cultures"].values()):
        print("PNG scan unavailable; install Pillow to enable it.", file=sys.stderr)
    return int(args.strict and any(f["level"] == "ERROR" for f in result["findings"]))

if __name__ == "__main__":
    sys.exit(main())
