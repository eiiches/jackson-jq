import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BndManifestTest {

	private static final String PROCESSOR_REQUIREMENT =
			"osgi.extender;filter:=\"(&(osgi.extender=osgi.serviceloader.processor)(version>=1.0.0)(!(version>=2.0.0)))\"";
	private static final String REGISTRAR_REQUIREMENT =
			"osgi.extender;filter:=\"(&(osgi.extender=osgi.serviceloader.registrar)(version>=1.0.0)(!(version>=2.0.0)))\"";

	@TempDir
	Path tempDir;

	private Path createJar(String name, Map<String, byte[]> entries) throws IOException {
		Path jarPath = tempDir.resolve(name);
		try (OutputStream out = Files.newOutputStream(jarPath);
			 JarOutputStream jar = new JarOutputStream(out)) {
			for (Map.Entry<String, byte[]> jarEntry : entries.entrySet()) {
				jar.putNextEntry(new JarEntry(jarEntry.getKey()));
				jar.write(jarEntry.getValue());
				jar.closeEntry();
			}
		}
		return jarPath;
	}

	private static byte[] moduleInfo(Class<?> type) throws IOException {
		try (InputStream in = type.getModule().getResourceAsStream("module-info.class")) {
			if (in == null) {
				throw new IOException("module-info.class is missing from " + type.getModule().getName());
			}
			return in.readAllBytes();
		}
	}

	@Test
	void derivesSortedUnqualifiedExports() {
		ModuleDescriptor descriptor = ModuleDescriptor.newModule("net.thisptr.sample")
				.exports("net.thisptr.sample.zeta")
				.exports("net.thisptr.sample.alpha")
				.exports("net.thisptr.sample.internal", Set.of("net.thisptr.friend"))
				.build();

		Map<String, String> properties = new HashMap<>();
		BndManifest.deriveOsgiHeaders(descriptor, properties);

		assertThat(properties)
				.containsOnly(Map.entry("Export-Package", "net.thisptr.sample.alpha,net.thisptr.sample.zeta"));
	}

	@Test
	void derivesSortedProviderCapabilitiesAndRegistrarRequirement() {
		ModuleDescriptor descriptor = ModuleDescriptor.newModule("net.thisptr.sample")
				.provides("net.thisptr.zeta.Service", List.of("net.thisptr.sample.ZetaService"))
				.provides("net.thisptr.alpha.Service", List.of("net.thisptr.sample.AlphaService"))
				.build();

		Map<String, String> properties = new HashMap<>();
		BndManifest.deriveOsgiHeaders(descriptor, properties);

		assertThat(properties.get("Provide-Capability")).isEqualTo(
				"osgi.serviceloader;osgi.serviceloader=\"net.thisptr.alpha.Service\";uses:=\"net.thisptr.alpha\","
						+ "osgi.serviceloader;osgi.serviceloader=\"net.thisptr.zeta.Service\";uses:=\"net.thisptr.zeta\"");
		assertThat(properties.get("Require-Capability")).isEqualTo(REGISTRAR_REQUIREMENT);
	}

	@Test
	void derivesSortedConsumerRequirementsAndProcessorRequirement() {
		ModuleDescriptor descriptor = ModuleDescriptor.newModule("net.thisptr.sample")
				.uses("net.thisptr.zeta.Service")
				.uses("net.thisptr.alpha.Service")
				.build();

		Map<String, String> properties = new HashMap<>();
		BndManifest.deriveOsgiHeaders(descriptor, properties);

		assertThat(properties.get("Require-Capability")).isEqualTo(
				"osgi.serviceloader;cardinality:=multiple;filter:=\"(osgi.serviceloader=net.thisptr.alpha.Service)\";resolution:=optional,"
						+ "osgi.serviceloader;cardinality:=multiple;filter:=\"(osgi.serviceloader=net.thisptr.zeta.Service)\";resolution:=optional,"
						+ PROCESSOR_REQUIREMENT);
		assertThat(properties).doesNotContainKey("Provide-Capability");
	}

	@Test
	void derivesBothExtenderRequirementsForProviderAndConsumer() {
		ModuleDescriptor descriptor = ModuleDescriptor.newModule("net.thisptr.sample")
				.provides("net.thisptr.spi.Service", List.of("net.thisptr.sample.ServiceImpl"))
				.uses("net.thisptr.spi.Service")
				.build();

		Map<String, String> properties = new HashMap<>();
		BndManifest.deriveOsgiHeaders(descriptor, properties);

		assertThat(properties.get("Require-Capability"))
				.isEqualTo("osgi.serviceloader;cardinality:=multiple;filter:=\"(osgi.serviceloader=net.thisptr.spi.Service)\";resolution:=optional," + PROCESSOR_REQUIREMENT + "," + REGISTRAR_REQUIREMENT);
	}

	@Test
	void doesNotOverrideExplicitProperties() {
		ModuleDescriptor descriptor = ModuleDescriptor.newModule("net.thisptr.sample")
				.exports("net.thisptr.sample.pkg")
				.provides("net.thisptr.spi.Service", List.of("net.thisptr.sample.ServiceImpl"))
				.uses("net.thisptr.spi.Service")
				.build();

		Map<String, String> properties = new HashMap<>();
		properties.put("Export-Package", "custom.pkg");
		properties.put("Provide-Capability", "custom.provide");
		properties.put("Require-Capability", "custom.require");

		BndManifest.deriveOsgiHeaders(descriptor, properties);

		assertThat(properties).containsOnly(
				Map.entry("Export-Package", "custom.pkg"),
				Map.entry("Provide-Capability", "custom.provide"),
				Map.entry("Require-Capability", "custom.require"));
	}

	@Test
	void readsRootModuleDescriptor() throws Exception {
		Path jar = createJar("root.jar", Map.of("module-info.class", moduleInfo(Object.class)));

		ModuleDescriptor descriptor = BndManifest.readModuleDescriptor(jar.toFile());

		assertThat(descriptor).isNotNull();
		assertThat(descriptor.name()).isEqualTo("java.base");
	}

	@Test
	void readsHighestVersionedModuleDescriptor() throws Exception {
		Path jar = createJar("mr.jar", Map.of(
				"module-info.class", moduleInfo(Object.class),
				"META-INF/versions/9/module-info.class", moduleInfo(java.util.logging.Logger.class),
				"META-INF/versions/17/module-info.class", moduleInfo(java.sql.Driver.class),
				"META-INF/versions/latest/module-info.class", moduleInfo(Object.class)));

		ModuleDescriptor descriptor = BndManifest.readModuleDescriptor(jar.toFile());

		assertThat(descriptor).isNotNull();
		assertThat(descriptor.name()).isEqualTo("java.sql");
	}

	@Test
	void returnsNullWithoutModuleDescriptor() throws Exception {
		Path jar = createJar("unnamed.jar", Map.of("sample.txt", new byte[] { 1 }));

		assertThat(BndManifest.readModuleDescriptor(jar.toFile())).isNull();
	}
}
