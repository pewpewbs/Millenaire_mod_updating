import org.millenaire.common.ui.PujaSacrifice;import org.millenaire.common.village.Building;import net.minecraft.network.FriendlyByteBuf;public class PujaPacketHarness {
    public static PujaSacrifice readOrUpdateNullablePuja(FriendlyByteBuf ds, Building b, PujaSacrifice puja) {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        short type = ds.readShort();
        if (puja == null) {
            puja = new PujaSacrifice(b, type);
        }
        short enchantmentId = ds.readShort();
        puja.type = type;
        puja.currentTarget = null;
        for (var target : type == 0 ? PujaSacrifice.PUJA_TARGETS : PujaSacrifice.MAYAN_TARGETS) {
            if (net.minecraft.core.Registry.ENCHANTMENT.getId(target.enchantment) == enchantmentId) puja.currentTarget = target;
        }
        puja.pujaProgress = ds.readShort();
        puja.offeringNeeded = ds.readInt();
        puja.offeringProgress = ds.readInt();
        return puja;
    }
    public static void writeNullablePuja(PujaSacrifice puja, FriendlyByteBuf data) {
        data.writeBoolean(puja == null);
        if (puja != null) {
            data.writeShort((int)puja.type);
            if (puja.currentTarget != null) {
                data.writeShort(net.minecraft.core.Registry.ENCHANTMENT.getId(puja.currentTarget.enchantment));
            } else {
                data.writeShort(-1);
            }
            data.writeShort((int)puja.pujaProgress);
            data.writeInt(puja.offeringNeeded);
            data.writeInt(puja.offeringProgress);
        }
    }
}