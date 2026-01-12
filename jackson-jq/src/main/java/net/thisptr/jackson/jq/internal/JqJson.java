package net.thisptr.jackson.jq.internal;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.VersionRange;
import net.thisptr.jackson.jq.internal.annotations.InterfaceAudience;

@InterfaceAudience("https://github.com/quarkiverse/quarkus-jackson-jq")
public class JqJson {
    public static class JqFuncDef {
        public String name;
        public List<String> args = new ArrayList<>();
        public String body;
        public VersionRange version;
    }

    public List<JqFuncDef> functions = new ArrayList<>();
}
