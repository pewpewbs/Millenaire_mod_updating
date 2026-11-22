from pathlib import Path
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
src=Path('work/prepare_crop_goal_check.py').read_text()
insertion='''  reset(v);v.seeds=1;v.house.resources.soils.add(new Point(-4,-41,2));v.destination=null;check(goal.getDestination(v)!=null,"Idle farmer starts without previous destination");check(goal.isPossibleGenericGoal(v),"Idle farming goal eligible");
'''
src=src.replace('  reset(v);v.seeds=1;goal.performAction(v);',insertion+'  reset(v);v.seeds=1;goal.performAction(v);',1)
exec(compile(src,'crop23-fixture','exec'))
src=Path('work/check_chest_roads20.py').read_text()
insertion='''
  for(var barrier:List.of(Blocks.OAK_FENCE.defaultBlockState(),Blocks.NETHER_BRICK_FENCE.defaultBlockState(),Blocks.COBBLESTONE_WALL.defaultBlockState(),Blocks.IRON_BARS.defaultBlockState())){
   world.blocks.put(origin,barrier);
   check(BarrierPathRules.isBarrier(barrier),"shared barrier classifier");
   check(evaluator.getBlockPathType(world,0,0,0)==BlockPathTypes.BLOCKED,"barrier body blocked");
   check(evaluator.getBlockPathType(world,0,1,0)==BlockPathTypes.BLOCKED,"barrier cannot be stepped over");
   check(evaluator.getBlockPathType(world,1,0,0)==BlockPathTypes.WALKABLE,"route beside barrier retained");
  }
  check(!BarrierPathRules.isBarrier(Blocks.OAK_FENCE_GATE.defaultBlockState()),"gate still handled as gate");
  check(BarrierPathRules.blocksStepAbove(Blocks.OAK_FENCE_GATE.defaultBlockState()),"cannot step over closed gate");
  check(!BarrierPathRules.blocksStepAbove(Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.OPEN,true)),"open gate not solid barrier");
  var uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);var unsafe=(sun.misc.Unsafe)uf.get(null);
  var resident=(org.millenaire.common.entity.MillVillager.EntityGenericMale)unsafe.allocateInstance(org.millenaire.common.entity.MillVillager.EntityGenericMale.class);
  var pf=net.minecraft.world.entity.Entity.class.getDeclaredField("position");pf.setAccessible(true);pf.set(resident,net.minecraft.world.phys.Vec3.ZERO);
  var breed=(org.millenaire.common.goal.GoalBreedAnimals)unsafe.allocateInstance(org.millenaire.common.goal.GoalBreedAnimals.class);
  check(!breed.isWithinActionRange(resident,new org.millenaire.common.utilities.Point(4.5,0,0)),"must approach before feeding");
  check(breed.isWithinActionRange(resident,new org.millenaire.common.utilities.Point(2,0,0)),"feeding distance reachable");
  check(!breed.isWithinActionRange(resident,new org.millenaire.common.utilities.Point(0,2.5,0)),"different floor not feeding range");
  System.out.println("PASS barriers blocked at body/above, gates preserved, breeding arrival inside feeding range");
'''
src=src.replace('  System.out.println("PASS six chest states',insertion+'  System.out.println("PASS six chest states')
Path('work/check_barriers23.py').write_text(src,encoding='utf-8')
print('Prepared crop-start, barrier and breeding-range checks')
