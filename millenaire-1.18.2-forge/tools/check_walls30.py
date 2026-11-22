from pathlib import Path
s=Path('work/check_routes26.py').read_text()
extra='''
  Blocks.TORCH.builtInRegistryHolder().bindTags(java.util.List.of(net.minecraft.tags.BlockTags.WALL_POST_OVERRIDE));
  var accessor=(net.minecraft.world.level.LevelAccessor)java.lang.reflect.Proxy.newProxyInstance(ChestRoadCheck.class.getClassLoader(),new Class[]{net.minecraft.world.level.LevelAccessor.class},(proxy,method,a)->{
   if(method.getName().equals("getBlockState"))return world.getBlockState((BlockPos)a[0]);
   if(method.getName().equals("getFluidState"))return world.getFluidState((BlockPos)a[0]);
   if(method.getName().equals("getMinBuildHeight"))return -64;
   if(method.getName().equals("getHeight"))return 384;
   if(method.getName().equals("isClientSide"))return false;
   throw new UnsupportedOperationException(method.getName());
  });
  for(Block support:List.of(Blocks.COBBLESTONE,Blocks.ACACIA_LOG,Blocks.ACACIA_PLANKS)){
   world.blocks.clear();world.blocks.put(origin.east(),support.defaultBlockState());
   var wall=Block.updateFromNeighbourShapes(Blocks.COBBLESTONE_WALL.defaultBlockState(),accessor,origin);
   check(wall.getValue(WallBlock.EAST_WALL)!=WallSide.NONE,"wall connects to solid support "+support);
  }
  world.blocks.clear();world.blocks.put(origin.east(),Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING,Direction.NORTH));
  var connected=Block.updateFromNeighbourShapes(Blocks.COBBLESTONE_WALL.defaultBlockState(),accessor,origin);
  check(connected.getValue(WallBlock.EAST_WALL)!=WallSide.NONE,"wall joins crosswise gate");
  world.blocks.put(origin.east(),Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING,Direction.EAST));
  var parallel=Block.updateFromNeighbourShapes(Blocks.COBBLESTONE_WALL.defaultBlockState(),accessor,origin);
  check(parallel.getValue(WallBlock.EAST_WALL)==WallSide.NONE,"gate orientation follows vanilla rules");
  world.blocks.clear();world.blocks.put(origin.north(),Blocks.COBBLESTONE.defaultBlockState());world.blocks.put(origin.south(),Blocks.COBBLESTONE.defaultBlockState());world.blocks.put(origin.above(),Blocks.TORCH.defaultBlockState());
  var underTorch=Block.updateFromNeighbourShapes(Blocks.COBBLESTONE_WALL.defaultBlockState(),accessor,origin);
  world.blocks.put(origin,underTorch);
  check(underTorch.getValue(WallBlock.UP),"torch keeps central wall post");
  check(Blocks.TORCH.defaultBlockState().canSurvive(accessor,origin.above()),"torch survives replacement wall");
  for(boolean open:new boolean[]{true,false}){
   var gate=Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.OPEN,open);
   check(gate.getLightBlock(world,origin)==0,"gate does not absorb skylight");
   check(gate.propagatesSkylightDown(world,origin),"gate transmits skylight");
  }
  world.blocks.clear();
  var from=new net.minecraft.world.phys.Vec3(-1.5,0.8,0.5);var to=new net.minecraft.world.phys.Vec3(1.5,0.8,0.5);
  check(org.millenaire.common.goal.WorkAccess.visible(world,from,to,null),"open work area");
  for(Block obstacle:List.of(Blocks.COBBLESTONE,Blocks.OAK_FENCE,Blocks.COBBLESTONE_WALL)){
   world.blocks.put(origin,obstacle.defaultBlockState());
   check(!org.millenaire.common.goal.WorkAccess.visible(world,from,to,null),"work blocked by "+obstacle);
  }
  world.blocks.put(origin,Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING,Direction.EAST));
  check(!org.millenaire.common.goal.WorkAccess.visible(world,from,to,null),"closed gate blocks work");
  world.blocks.put(origin,Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING,Direction.EAST).setValue(FenceGateBlock.OPEN,true));
  check(org.millenaire.common.goal.WorkAccess.visible(world,from,to,null),"open gate permits access");
  world.blocks.put(origin,Blocks.FURNACE.defaultBlockState());
  check(org.millenaire.common.goal.WorkAccess.visible(world,from,new net.minecraft.world.phys.Vec3(0.5,0.5,0.5),origin),"target furnace itself may be hit");
  world.blocks.put(origin.west(),Blocks.COBBLESTONE.defaultBlockState());
  check(!org.millenaire.common.goal.WorkAccess.visible(world,from,new net.minecraft.world.phys.Vec3(0.5,0.5,0.5),origin),"wall before furnace blocks work");
  System.out.println("PASS work access through open space, walls/fences/closed gate blocked, open gate pass, furnace target and intervening wall");
  System.out.println("PASS stone/acacia wall connections, oriented gate connections, torch support on wall replacement, open/closed gate skylight");
'''
s=s.replace('  System.out.println("PASS gate route nodes',extra+'  System.out.println("PASS gate route nodes')
exec(compile(s,'check_walls30','exec'))


