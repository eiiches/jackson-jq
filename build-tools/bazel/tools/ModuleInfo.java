import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Finds and reads the effective module descriptor from a regular or multi-release jar.
 */
final class ModuleInfo {
	private static final String VERSIONED_PREFIX = "META-INF/versions/";
	private static final String MODULE_INFO = "module-info.class";

	private ModuleInfo() {
	}

	static ModuleDescriptor read(JarFile jar) throws IOException {
		JarEntry selected = null;
		int selectedVersion = -1;
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry candidate = entries.nextElement();
			int version = version(candidate.getName());
			if (version > selectedVersion) {
				selected = candidate;
				selectedVersion = version;
			}
		}
		if (selected == null)
			return null;
		try (InputStream in = jar.getInputStream(selected)) {
			return ModuleDescriptor.read(in);
		}
	}

	static ModuleDescriptor read(Map<String, byte[]> entries) throws IOException {
		byte[] selected = null;
		int selectedVersion = -1;
		for (Map.Entry<String, byte[]> candidate : entries.entrySet()) {
			int version = version(candidate.getKey());
			if (version > selectedVersion) {
				selected = candidate.getValue();
				selectedVersion = version;
			}
		}
		if (selected == null)
			return null;
		return ModuleDescriptor.read(new ByteArrayInputStream(selected));
	}

	private static int version(String name) {
		if (MODULE_INFO.equals(name))
			return 0;
		if (!name.startsWith(VERSIONED_PREFIX) || !name.endsWith("/" + MODULE_INFO))
			return -1;

		String version = name.substring(VERSIONED_PREFIX.length(), name.length() - MODULE_INFO.length() - 1);
		try {
			return Integer.parseInt(version);
		} catch (NumberFormatException ignored) {
			return -1;
		}
	}
}
