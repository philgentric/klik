package klikr.util.execute;

import java.nio.file.Path;

@Deprecated
public record Execution_context(boolean from_jar, Path folder, String cmd){}
