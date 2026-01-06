class Klik < Formula
  desc "A picture-oriented file browser (JavaFX application)"
  homepage "https://github.com/philgentric/klikr"
  url "https://github.com/philgentric/klikr.git",
      branch: "main"
  version "1.0.0"
  license "MIT"

  depends_on "openjdk@25"
  depends_on "gradle" => :build

  def install
    # Build from source
    system "gradle", "clean", "build", "-x", "test", "--no-daemon"

    # Install the compiled JAR
    libexec.install Dir["build/libs/*.jar"].first => "xyzt.jar"

    # Copy the entire git repository to libexec for git pull support
    libexec.install Dir["*"]
    libexec.install Dir[".git"]

    # Create wrapper script
    (bin/"klikr").write <<~EOS
      #!/bin/bash
      exec "#{Formula["openjdk@25"].opt_bin}/java" -jar "#{libexec}/klikr.jar" "$@"
    EOS

    # Create update script
    (bin/"klikr-update").write <<~EOS
      #!/bin/bash
      cd "#{libexec}" && git pull && gradle clean build -x test --no-daemon
    EOS
    chmod 0755, bin/"klikr-update"
  end

  test do
    assert_match "klikr", shell_output("#{bin}/klikr --version 2>&1", 1)
  end
end