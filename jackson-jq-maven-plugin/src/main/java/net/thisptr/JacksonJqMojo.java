package net.thisptr;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.thisptr.jackson.jq.*;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.functions.EnvFunction;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "jq", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class JacksonJqMojo extends AbstractMojo {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Parameter(property = "inputFile", required = false)
    private File inputFile;

    @Parameter(property = "outputFile", required = false)
    private File outputFile;

    @Parameter(property = "module", required = false)
    private File module;

    @Parameter(property = "filter", defaultValue = ".", required = true)
    private String filter = ".";

    @Parameter(property = "nullInput", defaultValue = "false", required = true)
    private boolean nullInput = false;

    @Parameter(property = "compact", defaultValue = "false", required = true)
    private boolean compact = false;

    @Parameter(property = "raw", defaultValue = "false", required = true)
    private boolean raw = false;

    @Parameter(property = "version", defaultValue = "1.6", required = true)
    private String versionStr = "1.6";

    public void execute() throws MojoExecutionException {

        if (!compact) {
            MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        }

        InputStream is = new ByteArrayInputStream("null".getBytes());;
        if (!nullInput && inputFile != null) {
            try {
                is = new FileInputStream(inputFile);
            } catch (FileNotFoundException e) {
                throw new MojoExecutionException("Input file not found", e);
            }
        }

        OutputStream os = System.out;
        if (outputFile != null) {
            try {
                os = new FileOutputStream(outputFile);
            } catch (FileNotFoundException e) {
                throw new MojoExecutionException("Could not create output file", e);
            }
        }

        if (module != null) {
            try (Stream<String> lines = Files.readAllLines(module.toPath()).stream()) {
                filter = lines.collect(Collectors.joining(System.lineSeparator()));
            } catch (IOException e) {
                throw new MojoExecutionException("Error while reading module", e);
            }
        }

        Version version = Versions.JQ_1_6;
        if (StringUtils.isNotBlank(versionStr)) {
            version = Version.valueOf(versionStr);
            if (!Versions.versions().contains(version)) {
                throw new MojoExecutionException("Unsupported JQ version: " + version);
            }
        }

        final Scope scope = Scope.newEmptyScope();
        BuiltinFunctionLoader.getInstance().loadFunctions(version, scope);
        scope.addFunction("env", 0, new EnvFunction());

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is)); PrintWriter pw = new PrintWriter(os)) {
            final JsonQuery jq = JsonQuery.compile(filter, version);
            final JsonParser parser = MAPPER.getFactory().createParser(reader);
            while (!parser.isClosed()) {
                final JsonNode tree = parser.readValueAsTree();
                if (tree == null) {
                    continue;
                }
                jq.apply(scope, tree, (out) -> {
                    if (out.isTextual() && raw) {
                        pw.println(out.asText());
                    } else {
                        try {
                            pw.println(MAPPER.writeValueAsString(out));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    pw.flush();
                });
            }
        } catch (JsonQueryException e) {
            throw new MojoExecutionException("Error while executing JQ filter", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while executing JQ filter", e);
        }
    }

    public File getInputFile() {
        return inputFile;
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public File getModule() {
        return module;
    }

    public void setModule(File module) {
        this.module = module;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public boolean isNullInput() {
        return nullInput;
    }

    public void setNullInput(boolean nullInput) {
        this.nullInput = nullInput;
    }

    public boolean isCompact() {
        return compact;
    }

    public void setCompact(boolean compact) {
        this.compact = compact;
    }

    public boolean isRaw() {
        return raw;
    }

    public void setRaw(boolean raw) {
        this.raw = raw;
    }

    public String getVersionStr() {
        return versionStr;
    }

    public void setVersionStr(String versionStr) {
        this.versionStr = versionStr;
    }
}
