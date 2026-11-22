from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common';backup=r/'migration/before-load22';backup.mkdir(exist_ok=False)
def edit(name,fn):
 p=base/name;q=backup/name;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q);p.write_text(fn(p.read_text(encoding='utf-8')),encoding='utf-8')
def world(s):
 a=s.index('        if (MillConfigValues.autoConvertProfiles',s.index('public UserProfile getProfile(Player'))
 b=s.index('        UserProfile profile = new UserProfile(this, player);',a)
 s=s[:a]+s[b:]
 s=s.replace('UserProfile profile = new UserProfile(this, player);','UserProfile profile = new UserProfile(this, player);\n        profile.setReadOnly(this.profilesLoadFailed);')
 s=s.replace('UserProfile profile = new UserProfile(this, uuid, name);','UserProfile profile = new UserProfile(this, uuid, name);\n        profile.setReadOnly(this.profilesLoadFailed);')
 a=s.index('    private void loadProfiles()');b=s.index('    private void loadVillagesAndLoneBuildingsLists()',a)
 s=s[:a]+'''    private boolean profilesLoadFailed = false;
    private boolean villagerRecordsLoadFailed = false;

    private void loadProfiles() {
        File dir = new File(this.millenaireDir, "profiles");
        if (!dir.exists()) return;
        File[] entries = dir.listFiles();
        if (entries == null) {
            this.profilesLoadFailed = true;
            MillLog.error(this, "Cannot list profiles; new profile writes disabled to preserve existing data: " + dir);
            return;
        }
        // Canonical UUID directories take precedence over legacy name directories.
        java.util.Arrays.sort(entries, java.util.Comparator.comparing(f -> UserProfile.parseProfileUUID(f.getName()) == null));
        for (File entry : entries) {
            if (!entry.isDirectory() || entry.isHidden()) continue;
            try {
                UserProfile profile = UserProfile.readProfile(this, entry);
                UserProfile existing = this.profiles.putIfAbsent(profile.uuid, profile);
                if (existing != null) {
                    existing.setReadOnly(true);
                    MillLog.error(this, "Conflicting profile directories; both preserved, writes disabled for " + profile.uuid);
                }
            } catch (Exception failure) {
                MillLog.printException("Profile not loaded, directory preserved: " + entry, failure);
                UUID uuid = UserProfile.parseProfileUUID(entry.getName());
                if (uuid != null) {
                    UserProfile protectedProfile = new UserProfile(this, uuid, uuid.toString());
                    protectedProfile.setReadOnly(true);
                    this.profiles.putIfAbsent(uuid, protectedProfile);
                }
            }
        }
    }

    private void loadVillagerRecords() {
        if (this.world.isClientSide) return;
        File file = new File(this.millenaireDir, "villagerRecords.gz");
        if (!file.exists()) return;
        try (FileInputStream input = new FileInputStream(file)) {
            CompoundTag root = NbtIo.readCompressed(input);
            if (!root.contains("villagersrecords", 9)) throw new IOException("Missing villager records list");
            Tag raw = root.get("villagersrecords");
            ListTag list = (ListTag) raw;
            if (!list.isEmpty() && list.getElementType() != 10) throw new IOException("Invalid villager record list type");
            java.util.Map<Long, VillagerRecord> loaded = new java.util.LinkedHashMap<>();
            for (int i=0;i<list.size();i++) {
                try {
                    VillagerRecord vr = VillagerRecord.read(this, list.getCompound(i), "vr");
                    if (vr == null || vr.getVillagerId() == 0 || loaded.containsKey(vr.getVillagerId()))
                        throw new IOException("Invalid or duplicate villager ID at record " + i);
                    loaded.put(vr.getVillagerId(), vr);
                } catch (Exception failure) {
                    this.villagerRecordsLoadFailed = true;
                    MillLog.printException("Villager record " + i + " not loaded; source file protected", failure);
                }
            }
            for (VillagerRecord record : loaded.values()) this.registerVillagerRecord(record, false);
            // Reading is never a migration/write operation. Failed records remain on disk.
        } catch (Exception failure) {
            this.villagerRecordsLoadFailed = true;
            MillLog.printException("Villager records file protected after load failure: " + file, failure);
        }
    }

'''+s[b:]
 a=s.index('    private void saveVillagerRecords()');b=s.index('    public void saveWorldConfig()',a);part=s[a:b]
 part=part.replace('if (this.world.isClientSide)', 'if (this.world.isClientSide || this.villagerRecordsLoadFailed)')
 c=part.index('        File tempFile =');part=part[:c]+'''        try {
            org.millenaire.common.utilities.AtomicSaveFile.write(
                new File(this.millenaireDir, "villagerRecords.gz").toPath(),
                out -> NbtIo.writeCompressed(mainTag, out));
        } catch (IOException failure) { MillLog.printException(failure); }
    }

'''
 return s[:a]+part+s[b:]
edit('world/MillWorldData.java',world)
def profile(s):
 a=s.index('    public static UserProfile readProfile(');b=s.index('    public UserProfile(MillWorldData world, Player player)',a)
 s=s[:a]+'''    private File storageDirectory;
    private boolean readOnly;
    public void setReadOnly(boolean value) { this.readOnly |= value; }

    public static UUID parseProfileUUID(String name) {
        try {
            UUID uuid = UUID.fromString(name);
            return uuid.toString().equalsIgnoreCase(name) ? uuid : null;
        } catch (IllegalArgumentException invalid) { return null; }
    }

    public static UserProfile readProfile(MillWorldData world, File dir) {
        UUID uuid = parseProfileUUID(dir.getName());
        UserProfile profile = uuid == null ? new UserProfile(world, dir.getName()) : new UserProfile(world, uuid);
        // Load and retain the real source directory. Never rename or rewrite on read.
        profile.storageDirectory = dir;
        try {
            profile.loadProfileConfig(new File(dir, "config.txt"));
            profile.loadProfileTags();
            profile.loadActionData(new File(dir, "actiondata.txt"));
            profile.loadQuestInstances(new File(dir, "quests.txt"));
            profile.loadUnlockedContent(new File(dir, "unlockedcontent.txt"));
        } catch (RuntimeException failure) {
            profile.readOnly = true;
            MillLog.printException("Profile partially read; writes disabled: " + dir, failure);
        }
        return profile;
    }

'''+s[b:]
 s=s.replace('File dir = new File(new File(this.mw.millenaireDir, "profiles"), this.uuid.toString());','File dir = this.storageDirectory != null ? this.storageDirectory : new File(new File(this.mw.millenaireDir, "profiles"), this.uuid.toString());')
 # Every caught read failure protects the whole profile; close readers even on parsing exceptions.
 a=s.index('    private void loadActionData(');b=s.index('    public void migrateToPlayer(',a);part=s[a:b]
 import re
 part=re.sub(r'try \{\n\s*BufferedReader reader = MillCommonUtilities.getReader\((\w+)\);',r'try (BufferedReader reader = MillCommonUtilities.getReader(\1)) {',part)
 part=part.replace('catch (Exception e) {','catch (Exception e) {\n                this.readOnly = true;').replace('catch (IOException e) {','catch (IOException e) {\n                this.readOnly = true;')
 # Preserve colon-containing action values rather than silently dropping them.
 part=part.replace('line.split(":")','line.split(":", 2)')
 part=part.replace('if (line.trim().length() <= 0 || line.startsWith("//") || (qi = QuestInstance.loadFromString(this.mw, line, this)) == null) continue;', 'if (line.trim().length() <= 0 || line.startsWith("//")) continue;\n                    qi = QuestInstance.loadFromString(this.mw, line, this);\n                    if (qi == null) { this.readOnly = true; continue; }')
 s=s[:a]+part+s[b:]
 a=s.index('    public void migrateToPlayer(');b=s.index('    public void receiveDeclareReleaseNumberPacket',a)
 s=s[:a]+'''    public void migrateToPlayer(Player player) {
        if (!this.uuid.equals(player.getUUID()))
            throw new IllegalStateException("Cross-UUID profile migration requires explicit identity verification");
        this.playerName = player.getGameProfile().getName();
    }

'''+s[b:]
 # Protect all individual save entry points, not just saveProfile.
 a=s.index('    private void saveActionData()');b=s.index('    public ',s.index('    private void saveUnloadedContent()',a))
 part=s[a:b].replace('if (this.mw.world.isClientSide)', 'if (this.mw.world.isClientSide || this.readOnly)')
 # Atomic per-file text writes, retaining UTF-8 used by getWriter.
 part=re.sub(r'try \{\n\s*BufferedWriter writer = MillCommonUtilities.getWriter\((\w+)\);',r'try {\n            org.millenaire.common.utilities.AtomicSaveFile.write(\1.toPath(), stream -> {\n            BufferedWriter writer = new BufferedWriter(new java.io.OutputStreamWriter(stream, java.nio.charset.StandardCharsets.UTF_8));',part)
 part=part.replace('            writer.flush();\n        }','            writer.flush();\n            });\n        }')
 s=s[:a]+part+s[b:]
 return s
edit('world/UserProfile.java',profile)
def record(s):
 a=s.index('        if (!nbttagcompound.contains(label + "_id")');b=s.index('        vr.nb =',a)
 s=s[:a]+'''        String idKey = nbttagcompound.contains(label + "_lid") ? label + "_lid" : label + "_id";
        if (!nbttagcompound.contains(idKey, 3) && !nbttagcompound.contains(idKey, 4)) return null;
        long id = nbttagcompound.getLong(idKey);
        if (id == 0L || id == Long.MIN_VALUE) return null;
        VillagerRecord vr = new VillagerRecord(mw, Culture.getCultureByName(nbttagcompound.getString(label + "_culture")));
        vr.setVillagerId(Math.abs(id));
'''+s[b:];return s
edit('village/VillagerRecord.java',record)
print('Protected profile loading and NPC records applied')
