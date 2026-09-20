package net.thisptr.jackson.jq.v2.spi.version;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VersionTest {

	@Test
	void testValidVersions() {
		Version v1 = Version.valueOf("1.0");
		assertThat(v1.major()).isEqualTo(1);
		assertThat(v1.minor()).isEqualTo(0);
		assertThat(v1.patch()).isEqualTo(0);

		Version v2 = Version.valueOf("1.2.3");
		assertThat(v2.major()).isEqualTo(1);
		assertThat(v2.minor()).isEqualTo(2);
		assertThat(v2.patch()).isEqualTo(3);

		Version v3 = Version.valueOf("0.1.0");
		assertThat(v3.major()).isEqualTo(0);
		assertThat(v3.minor()).isEqualTo(1);
		assertThat(v3.patch()).isEqualTo(0);

		Version v4 = Version.valueOf("10.20.30");
		assertThat(v4.major()).isEqualTo(10);
		assertThat(v4.minor()).isEqualTo(20);
		assertThat(v4.patch()).isEqualTo(30);
	}

	@Test
	void testOfComponents() {
		Version v1 = Version.of(1, 2, 3);
		assertThat(v1.major()).isEqualTo(1);
		assertThat(v1.minor()).isEqualTo(2);
		assertThat(v1.patch()).isEqualTo(3);

		Version v2 = Version.of(1, 2);
		assertThat(v2.major()).isEqualTo(1);
		assertThat(v2.minor()).isEqualTo(2);
		assertThat(v2.patch()).isEqualTo(0);
	}

	@Test
	void testInvalidVersions() {
		assertThatThrownBy(() -> Version.valueOf("1")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.valueOf("1.0.0.0")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.valueOf("01.0.0")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.valueOf("1.0.0-SNAPSHOT")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.valueOf("")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.valueOf("abc")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.valueOf("-1.0")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.valueOf("1.-1.0")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.valueOf("1.0.-1")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.of(-1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.of(0, -1, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.of(0, 0, -1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.of(-1, -1, -1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.of(-1, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Version.of(0, -1)).isInstanceOf(IllegalArgumentException.class);
	}
}
