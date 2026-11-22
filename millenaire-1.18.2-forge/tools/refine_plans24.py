from pathlib import Path
p=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge/src/main/java/org/millenaire/common/buildingplan/SavedPlanReader.java');s=p.read_text()
s=s.replace('return new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(file)));','''try (InputStream input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes((int)MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IOException("Plan grew beyond file size limit");
            return new DataInputStream(new ByteArrayInputStream(bytes));
        }''')
s=s.replace('    private static void end(','''    private static BuildingBlock block(DataInputStream in) throws IOException {
        try { return new BuildingBlock(point(in), in); }
        catch (RuntimeException malformed) { throw new IOException("Invalid saved block state", malformed); }
    }
    private static void end(''')
s=s.replace('new BuildingBlock(point(in),in)','block(in)')
p.write_text(s,encoding='utf-8')
print('Bounded read even if file grows; malformed NBT reported as IO failure')
