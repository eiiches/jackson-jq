package net.thisptr.jackson.jq.v2.spi.version;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VersionRangeTest {

	// VersionRangeSpec itself isn't @Retention(RUNTIME); it's only reflectively visible when
	// nested inside a RUNTIME-retained annotation such as @FunctionRegistration (as used in
	// practice), so these holders mirror that real usage pattern rather than using
	// @VersionRangeSpec directly.
	@FunctionRegistration(name = "test", nargs = 0)
	private static class DefaultVersionRangeSpecHolder {
	}

	@FunctionRegistration(name = "test", nargs = 0, version = @VersionRangeSpec(min = @VersionSpec(major = 1, minor = 6, patch = 0)))
	private static class MinOnlyVersionRangeSpecHolder {
	}

	@FunctionRegistration(name = "test", nargs = 0, version = @VersionRangeSpec(min = @VersionSpec(major = -2, minor = -3, patch = -4)))
	private static class NonCanonicalNegativeVersionRangeSpecHolder {
	}

	@Test
	void testMaxBounds() {
		assertThat(VersionRange.valueOf("[1.3, 1.5]").contains(Version.valueOf("1.5"))).isTrue();
		assertThat(VersionRange.valueOf("[1.3, 1.5]").contains(Version.valueOf("1.6"))).isFalse();
		assertThat(VersionRange.valueOf("[1.3, 1.5)").contains(Version.valueOf("1.4"))).isTrue();
		assertThat(VersionRange.valueOf("[1.3, 1.5)").contains(Version.valueOf("1.5"))).isFalse();
		assertThat(VersionRange.valueOf("[1.3, 1.5)").contains(Version.valueOf("1.6"))).isFalse();
		assertThat(VersionRange.valueOf("[1.3,)").contains(Version.valueOf("1.2"))).isFalse();
		assertThat(VersionRange.valueOf("[1.3,)").contains(Version.valueOf("1.3"))).isTrue();
		assertThat(VersionRange.valueOf("[1.3,)").contains(Version.valueOf("1.4"))).isTrue();
		assertThat(VersionRange.valueOf("[,]").contains(Version.valueOf("1.0"))).isTrue();
	}

	@Test
	void testMinBounds() {
		assertThat(VersionRange.valueOf("[1.3, 1.5]").contains(Version.valueOf("1.2"))).isFalse();
		assertThat(VersionRange.valueOf("[1.3, 1.5]").contains(Version.valueOf("1.3"))).isTrue();
		assertThat(VersionRange.valueOf("[1.3, 1.5]").contains(Version.valueOf("1.4"))).isTrue();

		assertThat(VersionRange.valueOf("(1.3, 1.5]").contains(Version.valueOf("1.2"))).isFalse();
		assertThat(VersionRange.valueOf("(1.3, 1.5]").contains(Version.valueOf("1.3"))).isFalse();
		assertThat(VersionRange.valueOf("(1.3, 1.5]").contains(Version.valueOf("1.4"))).isTrue();

		assertThat(VersionRange.valueOf("[, 1.5]").contains(Version.valueOf("1.0"))).isTrue();
		assertThat(VersionRange.valueOf("(, 1.5]").contains(Version.valueOf("1.0"))).isTrue();
		assertThat(VersionRange.valueOf("[, 1.5]").contains(Version.valueOf("1.6"))).isFalse();
		assertThat(VersionRange.valueOf("(, 1.5]").contains(Version.valueOf("1.6"))).isFalse();
	}

	@Test
	void testMultiDigitAndThreeParts() {
		assertThat(VersionRange.valueOf("[1.0.0, 2.3.4)").contains(Version.valueOf("2.3.3"))).isTrue();
		assertThat(VersionRange.valueOf("[1.0.0, 2.3.4)").contains(Version.valueOf("2.3.4"))).isFalse();
		assertThat(VersionRange.valueOf("[10.20.30, 40.50.60]").contains(Version.valueOf("10.20.30"))).isTrue();
		assertThat(VersionRange.valueOf("[10.20.30, 40.50.60]").contains(Version.valueOf("40.50.60"))).isTrue();
	}

	@Test
	void testInvalidRanges() {
		assertThatThrownBy(() -> VersionRange.valueOf("[1.0, 2.0.0.0]")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> VersionRange.valueOf("[01.0, 2.0]")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> VersionRange.valueOf("1.0, 2.0")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> VersionRange.valueOf("[1.0]")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> VersionRange.valueOf("[2.0, 1.0]")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> VersionRange.valueOf("(2.0, 1.0)")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> VersionRange.valueOf("[1.0, 1.0)")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> VersionRange.valueOf("(1.0, 1.0]")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> VersionRange.valueOf("(1.0, 1.0)")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new VersionRange(Version.valueOf("2.0"), true, Version.valueOf("1.0"), true)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new VersionRange(Version.valueOf("1.0"), true, Version.valueOf("1.0"), false)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new VersionRange(Version.valueOf("1.0"), false, Version.valueOf("1.0"), true)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new VersionRange(Version.valueOf("1.0"), false, Version.valueOf("1.0"), false)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testInclusivityNormalizationOnAbsentBounds() {
		assertThat(VersionRange.valueOf("(, 1.5]")).isEqualTo(VersionRange.valueOf("[, 1.5]"));
		assertThat(VersionRange.valueOf("(, 1.5]")).hasSameHashCodeAs(VersionRange.valueOf("[, 1.5]"));
		assertThat(VersionRange.valueOf("[1.3, )")).isEqualTo(VersionRange.valueOf("[1.3, ]"));
		assertThat(VersionRange.valueOf("[1.3, )")).hasSameHashCodeAs(VersionRange.valueOf("[1.3, ]"));
		assertThat(VersionRange.valueOf("(,)")).isEqualTo(VersionRange.valueOf("[,]"));
		assertThat(VersionRange.valueOf("(,)")).hasSameHashCodeAs(VersionRange.valueOf("[,]"));
		assertThat(new VersionRange(null, false, Version.valueOf("1.5"), true)).isEqualTo(new VersionRange(null, true, Version.valueOf("1.5"), true));
		assertThat(new VersionRange(Version.valueOf("1.3"), true, null, false)).isEqualTo(new VersionRange(Version.valueOf("1.3"), true, null, true));
		assertThat(new VersionRange(null, false, null, false)).isEqualTo(new VersionRange(null, true, null, true));
	}

	@Test
	void testSinglePointRange() {
		VersionRange singlePoint = VersionRange.valueOf("[1.5, 1.5]");
		assertThat(singlePoint.contains(Version.valueOf("1.5"))).isTrue();
		assertThat(singlePoint.contains(Version.valueOf("1.4"))).isFalse();
		assertThat(singlePoint.contains(Version.valueOf("1.6"))).isFalse();
	}

	@Test
	void testToString() {
		assertThat(VersionRange.valueOf("[, 1.5]")).hasToString("(, 1.5.0]");
		assertThat(VersionRange.valueOf("[1.3, ]")).hasToString("[1.3.0, )");
		assertThat(VersionRange.valueOf("[,]")).hasToString("(, )");
		assertThat(VersionRange.valueOf("[1.0, 2.0]")).hasToString("[1.0.0, 2.0.0]");
	}

	@Test
	void testOfAndAccessors() {
		Version min = Version.of(1, 3);
		Version max = Version.of(1, 5);
		VersionRange range = VersionRange.of(min, true, max, false);
		assertThat(range.minVersion()).isEqualTo(min);
		assertThat(range.minInclusive()).isTrue();
		assertThat(range.maxVersion()).isEqualTo(max);
		assertThat(range.maxInclusive()).isFalse();

		VersionRange unbounded = VersionRange.of(null, true, null, true);
		assertThat(unbounded.minVersion()).isNull();
		assertThat(unbounded.minInclusive()).isFalse();
		assertThat(unbounded.maxVersion()).isNull();
		assertThat(unbounded.maxInclusive()).isFalse();
	}

	@Test
	void testFromVersionRangeSpecDefaultsAreGenuinelyUnbounded() {
		VersionRangeSpec spec = DefaultVersionRangeSpecHolder.class.getAnnotation(FunctionRegistration.class).version();
		VersionRange range = VersionRange.from(spec);
		assertThat(range).isEqualTo(VersionRange.valueOf("(,)"));
		assertThat(range.contains(Version.of(0, 0, 0))).isTrue();
		assertThat(range.contains(Version.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE))).isTrue();
	}

	@Test
	void testFromVersionRangeSpecWithOnlyMinLeavesMaxUnbounded() {
		VersionRangeSpec spec = MinOnlyVersionRangeSpecHolder.class.getAnnotation(FunctionRegistration.class).version();
		VersionRange range = VersionRange.from(spec);
		assertThat(range).isEqualTo(VersionRange.valueOf("[1.6.0,)"));
		assertThat(range.contains(Version.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE))).isTrue();
		assertThat(range.contains(Version.of(1, 5, 0))).isFalse();
	}

	@Test
	void testFromVersionRangeSpecRejectsNonCanonicalNegativeSentinel() {
		VersionRangeSpec spec = NonCanonicalNegativeVersionRangeSpecHolder.class.getAnnotation(FunctionRegistration.class).version();
		assertThatThrownBy(() -> VersionRange.from(spec)).isInstanceOf(IllegalArgumentException.class);
	}
}
