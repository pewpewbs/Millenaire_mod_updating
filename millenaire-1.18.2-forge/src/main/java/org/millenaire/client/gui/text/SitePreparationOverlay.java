package org.millenaire.client.gui.text;

import java.util.regex.Pattern;

/** Pure projection of the synchronized site hint onto the village map.
 * World bounds are inclusive; map draw rectangles have exclusive ends. */
public final class SitePreparationOverlay {
    private SitePreparationOverlay() {}
    public static final String PREPARE = "ui.site.prepare";
    public static final long HALF_PERIOD_MS = 700L;
    public static final int RED = 0xFFFF0000;
    private static final Pattern BOUNDS = Pattern.compile("X: (-?\\d+)\\.\\.(-?\\d+), Z: (-?\\d+)\\.\\.(-?\\d+)");
    public record Rect(int left,int top,int right,int bottom) {}

    public static Rect project(String issue,String hint,int startX,int startZ,int length,int width) {
        if(!PREPARE.equals(issue)||hint==null||length<=0||width<=0)return null;
        var matcher=BOUNDS.matcher(hint);
        if(!matcher.matches())return null;
        try {
            int minX=Integer.parseInt(matcher.group(1)),maxX=Integer.parseInt(matcher.group(2));
            int minZ=Integer.parseInt(matcher.group(3)),maxZ=Integer.parseInt(matcher.group(4));
            if(minX>maxX||minZ>maxZ)return null;
            long left=Math.max(0L,(long)minX-startX),top=Math.max(0L,(long)minZ-startZ);
            long right=Math.min((long)length,(long)maxX-startX+1L);
            long bottom=Math.min((long)width,(long)maxZ-startZ+1L);
            return left<right&&top<bottom?new Rect((int)left,(int)top,(int)right,(int)bottom):null;
        }catch(NumberFormatException ignored){return null;}
    }
    public static boolean visible(long monotonicMillis) {
        return Math.floorMod(monotonicMillis,2*HALF_PERIOD_MS)<HALF_PERIOD_MS;
    }
}
