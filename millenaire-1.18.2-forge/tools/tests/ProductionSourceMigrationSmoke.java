import org.millenaire.common.buildingplan.ProductionSourceMigrationPolicy;
import static org.millenaire.common.buildingplan.ProductionSourceMigrationPolicy.Decision.*;

public class ProductionSourceMigrationSmoke {
    public static void main(String[] args) {
        int cases = 0;
        for (int mask = 0; mask < 512; mask++) {
            boolean[] b = new boolean[9];
            for (int i = 0; i < b.length; i++) b[i] = (mask & (1 << i)) != 0;
            if (b[5] && b[6]) continue; // a live block cannot be both stone and sand/gravel
            var actual = ProductionSourceMigrationPolicy.decide(b[0],b[1],b[2],b[3],b[4],b[5],b[6],b[7],b[8]);
            var expected = KEEP;
            if (b[1] && b[2]) {
                if (!b[0]) expected = WAIT;
                else if (b[6] && (b[3] || !b[4])) expected = REGISTER;
                else if (!b[6] && b[3] && b[5] && b[7] && b[8]) expected = REPLACE;
            }
            if (actual != expected) throw new AssertionError("mask=" + mask + " expected=" + expected + " actual=" + actual);
            cases++;
        }
        System.out.println("Production source migration: " + cases + " cases passed");
    }
}
