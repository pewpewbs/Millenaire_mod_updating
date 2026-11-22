package org.millenaire.common.block;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.common.entity.TileEntityImportTable;

public class BlockImportTable extends Block implements EntityBlock {
    public BlockImportTable(String name) { super(Properties.of(Material.WOOD).strength(1.0F).sound(SoundType.WOOD)); setRegistryName("millenaire", name); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileEntityImportTable(pos, state); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) { return List.of(new ItemStack(this)); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityImportTable table)) return InteractionResult.PASS;
        table.activate(player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @OnlyIn(Dist.CLIENT) public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.solid()); }
}
