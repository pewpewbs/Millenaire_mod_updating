/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 */
package org.millenaire.common.goal.leisure;

import java.util.List;
import net.minecraft.world.entity.Entity;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.culture.CultureLanguage;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.WorldUtilities;

@DocumentedElement.Documentation(value="Make the villager meet a villager looking for someone to chat, to initiate a dialog")
public class GoalGoChat
extends Goal {
    /** A dialogue is a pair of available residents already near each other. */
    private List<MillVillager> partners(MillVillager villager) {
        List<MillVillager> candidates=new java.util.ArrayList<>();
        if(villager.getTownHall()==null)return candidates;
        for(MillVillager v:villager.getTownHall().getKnownVillagers()){
            if(v==villager||!v.isAlive()||!Goal.gosocialise.key.equals(v.goalKey)||v.dialogueKey!=null
                    ||v.getPos().distanceToSquared((Entity)villager)>9.0)continue;
            var meeting=Goal.gosocialise.getCurrentGoalTarget(v);
            if(meeting!=null&&meeting.distanceTo((Entity)v)<=Goal.gosocialise.range(v))candidates.add(v);
        }
        return candidates;
    }

    public GoalGoChat() {
        this.leasure = true;
        this.travelBookShow = false;
        this.sprint = false;
    }

    @Override
    public int actionDuration(MillVillager villager) {
        return 40;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        List<MillVillager> candidates=partners(villager);
        return candidates.isEmpty()?null:this.packDest(null,null,(Entity)candidates.get(MillCommonUtilities.randomInt(candidates.size())));
    }

    @Override
    protected boolean isPossibleSpecific(MillVillager villager) throws Exception {
        return this.getDestination(villager) != null;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public void onAccept(MillVillager villager) throws Exception {
        List<MillVillager> candidates=partners(villager);
        java.util.Collections.shuffle(candidates,MillCommonUtilities.random);
        for(MillVillager target:candidates){
            CultureLanguage.Dialogue dialog=villager.getCulture().pickNewDialogue(villager,target);
            if(dialog==null)continue;
            int role=dialog.validRoleFor(villager);
            if(role!=1&&role!=2)continue;
            // Do not claim a partner until a compatible dialogue actually exists.
            target.clearGoal();
            target.setGoalInformation(null);
            target.goalKey=this.key;
            target.goalStarted=target.level.getGameTime();
            target.setGoalDestEntity((Entity)villager);
            villager.setGoalInformation(null);
            villager.setGoalDestEntity((Entity)target);
            boolean dialogueChat=true;
            for(Entity ent:WorldUtilities.getEntitiesWithinAABB(villager.level,MillVillager.class,villager.getPos(),5,5)){
                if(ent==villager||ent==target)continue;
                MillVillager v=(MillVillager)ent;
                if(this.key.equals(v.goalKey)&&v.dialogueChat){dialogueChat=false;break;}
            }
            long start=villager.level.getDayTime();
            villager.dialogueKey=dialog.key;
            villager.dialogueRole=role;
            villager.dialogueStart=start;
            villager.dialogueColour=(char)102;
            villager.dialogueChat=dialogueChat;
            target.dialogueKey=dialog.key;
            target.dialogueRole=3-role;
            target.dialogueStart=start;
            target.dialogueColour=(char)102;
            target.dialogueChat=dialogueChat;
            return;
        }
        // Nothing compatible: leave other residents available for the group.
        villager.clearGoal();
        super.onAccept(villager);
    }

    @Override
    protected boolean isStillValidSpecific(MillVillager villager) {
        return villager.getGoalDestEntity() instanceof MillVillager partner && partner.isAlive()
                && this.key.equals(partner.goalKey) && partner.getGoalDestEntity()==villager
                && villager.getPos().distanceToSquared((Entity)partner)<=25.0;
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        return villager.dialogueKey == null;
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        return 10;
    }

    @Override
    public int range(MillVillager villager) {
        return 3;
    }
}

