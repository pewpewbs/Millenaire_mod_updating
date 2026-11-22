from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
b=r/'migration/before-path-refresh';b.mkdir(exist_ok=False)
base=r/'src/main/java/org/millenaire/common'
p=base/'entity/MillVillager.java';s=p.read_text();shutil.copy2(p,b/p.name)
old='if (this.stopMoving || this.pathPlannerJPS.isBusy()) {'
assert s.count(old)==1
s=s.replace(old,'// Keep following a usable route while its replacement is calculated.\n                if (this.stopMoving) {')
p.write_text(s)
p=base/'pathing/atomicstryker/AStarWorker.java';s=p.read_text();shutil.copy2(p,b/p.name)
s=s.replace('public boolean isRunning = false;', 'public volatile boolean isRunning = false;').replace('private boolean isBusy = false;', 'private volatile boolean isBusy = false;')
a=s.index('    @Override\n    public void run() {');z=s.index('    public ArrayList<AStarNode> runSync()',a)
s=s[:a]+'''    @Override
    public void run() {
        this.isRunning = true;
        this.isBusy = true;
        this.timeLimit = System.currentTimeMillis() + 150L;
        try {
            ArrayList<AStarNode> result = null;
            try {
                result = this.getPath(this.startNode, this.targetNode, this.config.allowDropping);
            }
            catch (ThreadSafeUtilities.ChunkAccessException e) {
                MillLog.error(this, "Chunk access violation while calculating a path for " + this.boss);
            }
            catch (Throwable e) {
                MillLog.printException("Exception while calculating a path:", e);
            }
            // A failed search must notify the planner exactly once, including exceptions.
            if (result == null) {
                this.boss.onNoPathAvailable();
            } else {
                this.boss.onFoundPath(result);
            }
        } finally {
            this.isBusy = false;
            this.isRunning = false;
        }
    }

'''+s[z:]
p.write_text(s)
print('Updated path refresh and worker completion; originals backed up:',b)
