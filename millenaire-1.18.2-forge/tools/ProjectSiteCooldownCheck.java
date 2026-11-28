import org.millenaire.common.village.ProjectSiteCooldown;
public class ProjectSiteCooldownCheck {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args){
        ProjectSiteCooldown c=new ProjectSiteCooldown();
        c.defer("cattlefarm",100);
        check(c.deferred("cattlefarm",100)&&!c.deferred("farm",100),"independent");
        check(c.deferred("cattlefarm",1699)&&!c.deferred("cattlefarm",1700),"expiry");
        c.defer("farm",2000);check(!c.deferred("farm",0),"clock reset");
        c.defer("farm",100);c.clear("farm");check(!c.deferred("farm",100),"clear");
        System.out.println("PASS: independent project cooldowns");
    }
}
