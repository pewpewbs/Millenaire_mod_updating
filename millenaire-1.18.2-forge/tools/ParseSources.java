import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.nio.file.*;
import java.util.*;

class ParseSources {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, java.nio.charset.StandardCharsets.UTF_8);
             var paths = Files.walk(Path.of(args[0]))) {
            var files = paths.filter(p -> p.toString().endsWith(".java")).toList();
            var units = fm.getJavaFileObjectsFromPaths(files);
            JavacTask task = (JavacTask)compiler.getTask(null, fm, diagnostics, List.of("-proc:none", "-Xmaxerrs", "10000"), null, units);
            task.parse();
            int errors = 0;
            for (var d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    errors++;
                    System.out.println(d.getSource().getName() + ":" + d.getLineNumber() + ": " + d.getMessage(Locale.ROOT));
                }
            }
            System.out.println("FILES=" + files.size() + " PARSE_ERRORS=" + errors);
            if (errors > 0) System.exit(1);
        }
    }
}
