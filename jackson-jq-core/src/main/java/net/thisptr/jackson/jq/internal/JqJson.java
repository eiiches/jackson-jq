package net.thisptr.jackson.jq.internal;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import net.thisptr.jackson.jq.VersionRange;
import net.thisptr.jackson.jq.internal.annotations.InterfaceAudience;
import net.thisptr.jackson.jq.internal.misc.VersionRangeDeserializer;

@InterfaceAudience("https://github.com/quarkiverse/quarkus-jackson-jq")
@JsonIgnoreProperties(ignoreUnknown = true)
public class JqJson {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JqFuncDef {
        @JsonProperty("name")
        public String name;

        @JsonProperty("args")
        public List<String> args = new ArrayList<>();

        @JsonProperty("body")
        public String body;

        @JsonProperty("version")
        @JsonDeserialize(using = VersionRangeDeserializer.class)
        public VersionRange version;
    }

    @JsonProperty("functions")
    public List<JqFuncDef> functions = new ArrayList<>();
}
