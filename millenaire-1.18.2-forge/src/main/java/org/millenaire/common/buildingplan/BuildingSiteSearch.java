package org.millenaire.common.buildingplan;
/** Pure enumeration of candidate building centres and allowed orientations. */
public final class BuildingSiteSearch {
    private BuildingSiteSearch() {}
    @FunctionalInterface public interface Probe<T> { T test(int x,int z); }
    private static <T> T inside(int x,int z,int l,int w,Probe<T> p) {
        return x>=0&&z>=0&&x<l&&z<w?p.test(x,z):null;
    }
    public static <T> T find(int cx,int cz,int min,int max,int l,int w,Probe<T> p) {
        for(int r=Math.max(0,min);r<=max;r++){
            if(r==0){T v=inside(cx,cz,l,w,p);if(v!=null)return v;continue;}
            for(int d=-r;d<=r;d++){
                T v=inside(cx+d,cz-r,l,w,p);if(v!=null)return v;
                v=inside(cx+d,cz+r,l,w,p);if(v!=null)return v;
            }
            for(int d=-r+1;d<r;d++){
                T v=inside(cx-r,cz+d,l,w,p);if(v!=null)return v;
                v=inside(cx+r,cz+d,l,w,p);if(v!=null)return v;
            }
        }
        return null;
    }
    public static int[] orientations(int requested,int fixed,int preferred){
        if(requested>=0)return new int[]{Math.floorMod(requested,4)};
        if(fixed>=0)return new int[]{Math.floorMod(fixed,4)};
        int p=Math.floorMod(preferred,4);
        return new int[]{p,(p+1)%4,(p+3)%4,(p+2)%4};
    }
    public enum SiteStatus { PREPARE_TERRAIN, PROTECTED_OR_UNCERTAIN, NO_LAYOUT_SPACE }
    public record Assessment(SiteStatus status, int minX, int minZ, int maxX, int maxZ, int work) {
        public String issue() {
            return switch(status) {
                case PREPARE_TERRAIN -> "ui.site.prepare";
                case PROTECTED_OR_UNCERTAIN -> "ui.site.review";
                case NO_LAYOUT_SPACE -> "ui.site.nolayout";
            };
        }
        public String hint() {
            return status == SiteStatus.PREPARE_TERRAIN
                    ? "X: "+minX+".."+maxX+", Z: "+minZ+".."+maxZ : null;
        }
    }
    /** Summed-area tables inspect the entire footprint in constant time.
     * Occupancy is structural; unknown/protected/hazardous terrain is not
     * reported as ordinary clearing work. No live world access. */
    public static final class FootprintIndex {
        private final int length,width;
        private final int[][] occupied,unsafe,work;
        public FootprintIndex(int length,int width,java.util.function.IntBinaryOperator flags) {
            this.length=length;this.width=width;
            occupied=new int[length+1][width+1];unsafe=new int[length+1][width+1];work=new int[length+1][width+1];
            for(int x=0;x<length;x++)for(int z=0;z<width;z++){
                int f=flags.applyAsInt(x,z);
                add(occupied,x,z,(f&1)!=0?1:0);
                add(unsafe,x,z,(f&2)!=0?1:0);
                add(work,x,z,(f&4)!=0?1:0);
            }
        }
        private static void add(int[][] a,int x,int z,int v){
            a[x+1][z+1]=v+a[x][z+1]+a[x+1][z]-a[x][z];
        }
        private static int sum(int[][] a,int x,int z,int xx,int zz){
            return a[xx+1][zz+1]-a[x][zz+1]-a[xx+1][z]+a[x][z];
        }
        public Assessment inspect(int x,int z,int hx,int hz,int offsetX,int offsetZ){
            int a=x-hx,b=z-hz,c=x+hx,d=z+hz;
            if(a<0||b<0||c>=length||d>=width||sum(occupied,a,b,c,d)>0)return null;
            boolean uncertain=sum(unsafe,a,b,c,d)>0;
            return new Assessment(uncertain?SiteStatus.PROTECTED_OR_UNCERTAIN:SiteStatus.PREPARE_TERRAIN,
                    a+offsetX,b+offsetZ,c+offsetX,d+offsetZ,sum(work,a,b,c,d));
        }
    }
    public static Assessment prefer(Assessment previous,Assessment candidate){
        if(candidate==null)return previous;
        if(previous==null)return candidate;
        if(candidate.status()==SiteStatus.PREPARE_TERRAIN&&previous.status()!=SiteStatus.PREPARE_TERRAIN)return candidate;
        if(candidate.status()==previous.status()&&candidate.work()<previous.work())return candidate;
        return previous;
    }
    public static String failures(int[] errors){
        String[] names={"OK","OUTSIDE_MAP","OCCUPIED","FORBIDDEN","TERRAIN","DANGER","UNREACHABLE","TOO_CLOSE_TO_TAG","TOO_FAR_FROM_TAG"};
        StringBuilder b=new StringBuilder();
        for(int i=1;i<errors.length&&i<names.length;i++)if(errors[i]>0){
            if(b.length()>0)b.append(' ');
            b.append(names[i]).append('=').append(errors[i]);
        }
        return b.toString();
    }
}
