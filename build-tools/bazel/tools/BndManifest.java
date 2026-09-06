import java.io.File;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Provides;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

/**
 * Calculates an OSGi manifest with bnd and stamps it onto an existing jar.
 */
public final class BndManifest {
	private BndManifest() {
	}

	public static void main(String[] args) throws Exception {
		File input = null;
		File output = null;
		List<File> classpath = new ArrayList<>();
		Map<String, String> properties = new HashMap<>();

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
				case "--input":
					input = new File(args[++i]);
					break;
				case "--output":
					output = new File(args[++i]);
					break;
				case "--classpath":
					classpath.add(new File(args[++i]));
					break;
				case "--property":
					String property = args[++i];
					int equals = property.indexOf('=');
					if (equals < 1)
						throw new IllegalArgumentException("--property expects NAME=VALUE: " + property);
					properties.put(property.substring(0, equals), property.substring(equals + 1));
					break;
				default:
					throw new IllegalArgumentException("unknown option: " + args[i]);
			}
		}
		if (input == null || output == null)
			throw new IllegalArgumentException("--input and --output are required");

		ModuleDescriptor descriptor = readModuleDescriptor(input);
		if (descriptor != null) {
			deriveOsgiHeaders(descriptor, properties);
		}

		Builder builder = new Builder();
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			builder.setProperty(entry.getKey(), entry.getValue());
		}

		try (Builder closeableBuilder = builder; Jar jar = closeableBuilder.setJar(input)) {
			closeableBuilder.setClasspath(classpath.toArray(new File[0]));
			jar.setManifest(closeableBuilder.calcManifest());
			jar.write(output);
			if (!closeableBuilder.isOk())
				throw new IllegalStateException("bnd failed: " + closeableBuilder.getErrors());
		}
	}

	static ModuleDescriptor readModuleDescriptor(File jarFile) throws Exception {
		try (JarFile jar = new JarFile(jarFile)) {
			JarEntry moduleInfoEntry = null;
			int highestVersion = -1;

			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if ("module-info.class".equals(name)) {
					if (moduleInfoEntry == null) {
						moduleInfoEntry = entry;
					}
				} else if (name.startsWith("META-INF/versions/") && name.endsWith("/module-info.class")) {
					String versionStr = name.substring("META-INF/versions/".length(), name.length() - "/module-info.class".length());
					try {
						int version = Integer.parseInt(versionStr);
						if (version > highestVersion) {
							highestVersion = version;
							moduleInfoEntry = entry;
						}
					} catch (NumberFormatException ignored) {
					}
				}
			}

			if (moduleInfoEntry != null) {
				try (InputStream in = jar.getInputStream(moduleInfoEntry)) {
					return ModuleDescriptor.read(in);
				}
			}
		}
		return null;
	}

	static void deriveOsgiHeaders(ModuleDescriptor descriptor, Map<String, String> properties) {
		if (!properties.containsKey("Export-Package")) {
			Set<String> exports = new TreeSet<>();
			for (Exports export : descriptor.exports()) {
				if (export.targets().isEmpty()) {
					exports.add(export.source());
				}
			}
			if (!exports.isEmpty()) {
				properties.put("Export-Package", String.join(",", exports));
			}
		}

		if (!properties.containsKey("Provide-Capability")) {
			List<String> capabilities = new ArrayList<>();
			List<Provides> providesClauses = new ArrayList<>(descriptor.provides());
			providesClauses.sort(Comparator.comparing(Provides::service));
			for (Provides provides : providesClauses) {
				String service = provides.service();
				int dot = service.lastIndexOf('.');
				String pkg = dot > 0 ? service.substring(0, dot) : service;
				capabilities.add(String.format("osgi.serviceloader;osgi.serviceloader=\"%s\";uses:=\"%s\"", service, pkg));
			}
			if (!capabilities.isEmpty()) {
				properties.put("Provide-Capability", String.join(",", capabilities));
			}
		}

		if (!properties.containsKey("Require-Capability")) {
			List<String> requirements = new ArrayList<>();
			for (String uses : new TreeSet<>(descriptor.uses())) {
				requirements.add(String.format("osgi.serviceloader;cardinality:=multiple;filter:=\"(osgi.serviceloader=%s)\";resolution:=optional", uses));
			}
			if (!descriptor.uses().isEmpty()) {
				requirements.add("osgi.extender;filter:=\"(&(osgi.extender=osgi.serviceloader.processor)(version>=1.0.0)(!(version>=2.0.0)))\"");
			}
			if (!descriptor.provides().isEmpty()) {
				requirements.add("osgi.extender;filter:=\"(&(osgi.extender=osgi.serviceloader.registrar)(version>=1.0.0)(!(version>=2.0.0)))\"");
			}
			if (!requirements.isEmpty()) {
				properties.put("Require-Capability", String.join(",", requirements));
			}
		}
	}
}
