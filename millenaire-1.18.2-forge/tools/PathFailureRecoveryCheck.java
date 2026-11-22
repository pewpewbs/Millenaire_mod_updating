import org.millenaire.common.entity.PathFailureRecovery;
public class PathFailureRecoveryCheck {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  var recovery=new PathFailureRecovery();check(!recovery.failed(0),"first failure retries");
  check(!recovery.failed(1),"second failure retries");check(!recovery.failed(199),"grace period");check(recovery.failed(200),"repeated failures after grace permit fallback");
  check(!recovery.failed(201),"fallback cannot repeat immediately");check(!recovery.failed(401),"must fail three times again");check(recovery.failed(402),"new failure cycle");
  recovery.failed(500);recovery.reset();check(!recovery.failed(1000),"success or destination change resets failures");
  check(!recovery.failed(1001),"reset count");recovery.reset();recovery.failed(2000);check(!recovery.failed(50),"clock rewind restarts grace");
  System.out.println("PASS: recovery policy retries transient failures, waits 200 ticks and three failures, resets after success/destination change and rate-limits emergency recovery. Live movement not covered.");
 }
}