package org.millenaire.common.village;
import java.util.HashMap;
import java.util.Map;
/** A failed site search delays that project key, not the entire village. */
public final class ProjectSiteCooldown {
    public static final long RETRY_TICKS=1600L;
    private final Map<String,Long> retry=new HashMap<>();
    public void defer(String key,long now){if(key!=null)retry.put(key,now+RETRY_TICKS);}
    public boolean deferred(String key,long now){
        Long until=retry.get(key);
        if(until==null)return false;
        if(now>=until||until-now>RETRY_TICKS){retry.remove(key);return false;}
        return true;
    }
    public void clear(String key){retry.remove(key);}
}
