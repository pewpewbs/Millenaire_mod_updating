package org.millenaire.common.item;
import java.util.Locale;
import net.minecraft.world.level.block.entity.BannerPattern;

/** Stable 1.12 pattern hashes are retained in village banners and culture files. */
public final class MillBannerPatterns {
    public static final String[] NAMES = {"byzantine", "byzantine_1", "byzantine_2", "seljuk", "mayan", "inuit", "indian", "indian_1", "indian_2", "indian_3", "indian_4", "indian_5", "norman", "mayan_1", "mayan_2", "mayan_3", "mayan_4", "inuit_1", "inuit_2", "inuit_3", "inuit_4", "japanese", "japanese_agr", "japanese_mil", "japanese_rel", "japanese_tra", "seljuk_rel", "seljuk_mil"};
    public static final String[] HASHES = {"byz", "by1", "by2", "sjk", "may", "inu", "ind", "in1", "in2", "in3", "in4", "in5", "nor", "ma1", "ma2", "ma3", "ma4", "iu1", "iu2", "iu3", "iu4", "jap", "jaa", "jam", "jar", "jat", "sjkr", "sjkm"};
    private static final BannerPattern[] PATTERNS = new BannerPattern[NAMES.length];
    private MillBannerPatterns() {}
    public static synchronized void initialize() {
        for (int i=0;i<PATTERNS.length;i++) if (PATTERNS[i]==null)
            PATTERNS[i]=BannerPattern.create("MILLENAIRE_"+NAMES[i].toUpperCase(Locale.ROOT),"millenaire:"+NAMES[i],HASHES[i],true);
    }
    public static BannerPattern get(int index) {
        if (index<0 || index>=NAMES.length) throw new IllegalArgumentException("Unknown Millenaire banner pattern: "+index);
        initialize(); return PATTERNS[index];
    }
}
