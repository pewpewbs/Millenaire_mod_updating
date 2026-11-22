from pathlib import Path
p=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge/src/main/java/org/millenaire/common/world/UserProfile.java');s=p.read_text()
s=s.replace('        return profile;\n    }\n\n    public UserProfile', '        if (profile.readOnly) MillLog.error(profile, "Profile is read-only after incomplete load; original files preserved: " + dir);\n        return profile;\n    }\n\n    public UserProfile',1)
s=s.replace('''            try {
                String line;
                BufferedReader reader = MillCommonUtilities.getReader(configFile);''','''            try (BufferedReader reader = MillCommonUtilities.getReader(configFile)) {
                String line;''')
s=s.replace('''                BufferedReader reader = MillCommonUtilities.getReader(questDataFile);
                while''','''                try (BufferedReader reader = MillCommonUtilities.getReader(questDataFile)) {
                while''')
s=s.replace('''                reader.close();
            }
        }
        catch (IOException e)''','''                }
            }
        }
        catch (IOException e)''')
# Malformed nonempty records must not disappear silently on the next write.
s=s.replace('''if (line.trim().length() <= 0 || line.startsWith("//") || (temp = line.split("=")).length != 2) continue;''','''if (line.trim().length() <= 0 || line.startsWith("//")) continue;
                    temp = line.split("=", 2);
                    if (temp.length != 2) { this.readOnly = true; continue; }''')
s=s.replace('''                    line = reader.readLine();
                }
                if (MillConfigValues.LogWorldGeneration''','''                    else if (!line.trim().isEmpty()) { this.readOnly = true; }
                    line = reader.readLine();
                }
                if (MillConfigValues.LogWorldGeneration''')
# Above pattern also targets tags loop, where nonempty test is already exhaustive; harmless.
p.write_text(s,encoding='utf-8')
print('Load readers closed and incomplete input protected')
