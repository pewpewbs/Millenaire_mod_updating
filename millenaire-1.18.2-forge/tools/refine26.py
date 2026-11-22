from pathlib import Path
b=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge/src/main/java/org/millenaire/common')
p=b/'entity/PathNavigateSimple.java';s=p.read_text();marker='    @Override public boolean moveTo(Path requested, double speed) {';assert marker in s
s=s.replace(marker,'''    @Override protected Path createPath(java.util.Set<net.minecraft.core.BlockPos> targets, int padding, boolean above, int accuracy) {
        // Search beyond the default mob range so the exit on the far side of a fort is visible.
        return super.createPath(targets, padding, above, accuracy, 64.0F);
    }
'''+marker);p.write_text(s,encoding='utf-8')
p=b/'utilities/MoneyTransactions.java';s=p.read_text();s=s.replace('        var tag=stack.getTag();long a=', '''        var tag=stack.getTag();
        for(String key:new String[]{"ml_Purse_DENIER","ml_Purse_DENIERargent","ml_Purse_DENIERor"})
            if(tag.contains(key)&&!tag.contains(key,3))throw new IllegalArgumentException("Invalid purse number type");
        long a=''');p.write_text(s,encoding='utf-8')
p=b/'item/ItemPurse.java';s=p.read_text();s=s.replace('        if (this.totalDeniers(purse) > 0) {','''        try { org.millenaire.common.utilities.MoneyTransactions.count(playerIn.getInventory()); }
        catch (IllegalArgumentException invalid) { return InteractionResultHolder.fail(purse); }
        if (this.totalDeniers(purse) > 0) {''');p.write_text(s,encoding='utf-8')
p=b/'ui/ContainerTrade.java';s=p.read_text();s=s.replace('if (nbItems == 0) return;','if (nbItems == 0) return;\n                  if (MillCommonUtilities.countMoney(player.getInventory()) >= 4096) MillAdvancements.CRESUS.grant(player);').replace('if (nbItemsx == 0) return;','if (nbItemsx == 0) return;\n               if (MillCommonUtilities.countMoney(player.getInventory()) >= 4096) MillAdvancements.CRESUS.grant(player);');p.write_text(s,encoding='utf-8')
print('Bounded wider route search, validated purse fields, retained successful trade advancement')
