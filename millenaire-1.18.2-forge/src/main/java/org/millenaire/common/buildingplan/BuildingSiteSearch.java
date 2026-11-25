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
