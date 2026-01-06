package klikr.util.execute;

import java.nio.file.Path;

public record Execution_context(boolean from_jar, Path folder, String cmd){}
