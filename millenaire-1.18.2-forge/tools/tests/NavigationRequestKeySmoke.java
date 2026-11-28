import org.millenaire.common.pathing.NavigationRequestKey;

/** Standalone: javac --release 17 -d build/nav-request-smoke
 *  src/main/java/org/millenaire/common/pathing/NavigationRequestKey.java
 *  tools/tests/NavigationRequestKeySmoke.java
 *  java -cp build/nav-request-smoke NavigationRequestKeySmoke
 */
public final class NavigationRequestKeySmoke {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static NavigationRequestKey.Movement movement(boolean drop, boolean doors) {
        return new NavigationRequestKey.Movement(
                doors, false, drop, false, true, false, 0, 0);
    }
    public static void main(String[] args) {
        var a = new NavigationRequestKey.Endpoint(10, 64, 10);
        var b = new NavigationRequestKey.Endpoint(20, 65, 22);
        var config = movement(false, true);
        var first = new NavigationRequestKey(a, b, config);
        var again = new NavigationRequestKey(
                new NavigationRequestKey.Endpoint(10,64,10),
                new NavigationRequestKey.Endpoint(20,65,22),
                movement(false,true));
        check(first.equals(again), "identical request must coalesce");
        check(first.hashCode() == again.hashCode(), "stable equality contract");
        check(!first.equals(new NavigationRequestKey(b, a, config)),
                "reverse direction is different");
        check(!first.equals(new NavigationRequestKey(
                new NavigationRequestKey.Endpoint(10,65,10),b,config)),
                "start changing by one block prevents coalescing");
        check(!first.equals(new NavigationRequestKey(
                a,new NavigationRequestKey.Endpoint(20,66,22),config)),
                "goal changing by one block prevents coalescing");
        check(!first.equals(new NavigationRequestKey(a,b,movement(true,true))),
                "drop permission change prevents coalescing");
        check(!first.equals(new NavigationRequestKey(a,b,movement(false,false))),
                "door permission change prevents coalescing");
        var other = new NavigationRequestKey.Movement(true,false,false,false,true,true,3,1);
        check(!first.equals(new NavigationRequestKey(a,b,other)),
                "tolerance change prevents coalescing");
        check(!first.equals(new NavigationRequestKey(a,b,
                new NavigationRequestKey.Movement(true,false,false,false,true,true,3,2))),
                "vertical tolerance change prevents coalescing");
        boolean rejected = false;
        try { new NavigationRequestKey(null,b,config); }
        catch (NullPointerException expected) { rejected = true; }
        check(rejected, "null endpoint must be rejected");
        System.out.println("PASS: in-flight request identity, direction, position, policy and null safety");
    }
}
