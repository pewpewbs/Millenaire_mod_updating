package org.millenaire.common.utilities;
public final class CountedForms {
    private CountedForms() {}
    public static String suffix(String language, int count) {
        if (language == null) return "";
        String code = language.toLowerCase(java.util.Locale.ROOT).replace('-', '_').split("_")[0];
        if (!code.equals("uk") && !code.equals("ru")) return "";
        long n = Math.abs((long)count), last = n % 10, lastTwo = n % 100;
        if (last == 1 && lastTwo != 11) return ".one";
        if (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) return ".few";
        return ".many";
    }
}
