import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.item.Items;import net.minecraft.world.InteractionHand;import org.millenaire.common.utilities.Point;import org.millenaire.common.entity.MillVillager;import java.awt.image.BufferedImage;import java.io.*;import javax.imageio.ImageIO;
public class HarvestExportCheck{
    public boolean performAction(MillVillager villager) {
        BlockState bs;
        Point cropPoint = villager.getGoalDestPoint();
        if (cropPoint == null || villager.getTownHall() == null || villager.level.isClientSide) return true;
        if (cropPoint.getBlock(villager.level) == Blocks.COCOA && (Integer)(bs = cropPoint.getBlockActualState(villager.level)).getValue(CocoaBlock.AGE) >= 2) {
            if (!villager.setBlockstate(cropPoint, Blocks.AIR.defaultBlockState())) return true;
            int nbcrop = 2;
            float irrigation = villager.getTownHall().getVillageIrrigation();
            double rand = Math.random();
            if (rand < (double)(irrigation / 100.0f)) {
                ++nbcrop;
            }
            villager.addToInv(Items.COCOA_BEANS, 0, nbcrop);
            villager.swing(InteractionHand.MAIN_HAND);
        }
        return true;
    }

    private static void writeExport(BufferedImage image, File file) throws IOException {
        java.nio.file.Files.createDirectories(file.toPath().getParent());
        if (!ImageIO.write(image, "png", file)) throw new IOException("No PNG writer for " + file);
    }


 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var unsafe=(sun.misc.Unsafe)f.get(null);var v=new MillVillager();v.level=(net.minecraft.server.level.ServerLevel)unsafe.allocateInstance(net.minecraft.server.level.ServerLevel.class);v.dest=new Point(-1,-40,3);var goal=new HarvestExportCheck();
 for(int age=0;age<2;age++){Point.cells.put(v.dest.pos,Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE,age));goal.performAction(v);check(v.beans==0&&v.swings==0,"Unripe cocoa retained");}
 Point.cells.put(v.dest.pos,Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE,2));v.reject=true;goal.performAction(v);check(v.beans==0&&v.swings==0,"Failed harvest no yield");v.reject=false;goal.performAction(v);check(v.beans==2&&v.swings==1&&Point.cells.get(v.dest.pos).isAir(),"Ripe cocoa modern beans");goal.performAction(v);check(v.beans==2,"No duplicate harvest");Point.cells.put(v.dest.pos,Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE,2));v.house.irrigation=100;goal.performAction(v);check(v.beans==5,"Irrigation bonus");v.dest=null;check(goal.performAction(v),"Missing target safe");
 File dir=new File(args[0]);File png=new File(dir,"new/sub/icon.png");var image=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,0x80ff1234);writeExport(image,png);var decoded=ImageIO.read(png);check(png.isFile()&&decoded.getWidth()==32&&decoded.getRGB(0,0)==0x80ff1234,"PNG parent directories and alpha");writeExport(image,png);check(png.isFile(),"Existing output can be replaced");
 System.out.println("PASS: extracted production cocoa harvest action with world/storage fixtures: maturity, failed write, correct beans, repeat prevention, irrigation and null target. Extracted PNG writer verified with real filesystem/ImageIO: nested directories, alpha and overwrite. Rendering, sugarcane, building markers and live AI untested.");}}
