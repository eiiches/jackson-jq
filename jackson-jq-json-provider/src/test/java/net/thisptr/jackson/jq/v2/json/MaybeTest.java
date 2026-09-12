package net.thisptr.jackson.jq.v2.json;

import java.util.NoSuchElementException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaybeTest {
	@Test
	void testAbsent() {
		Maybe<String> absent = Maybe.absent();
		assertThat(absent.isAbsent()).isTrue();
		assertThat(absent.isPresent()).isFalse();
		assertThatThrownBy(absent::get).isInstanceOf(NoSuchElementException.class);
		assertThat(absent.orElse("other")).isEqualTo("other");
		assertThat(absent.toString()).isEqualTo("Maybe.absent()");
	}

	@Test
	void testAbsentIsShared() {
		assertThat(Maybe.<String>absent()).isSameAs(Maybe.<Integer>absent());
	}

	@Test
	void testPresent() {
		Maybe<String> present = Maybe.of("value");
		assertThat(present.isPresent()).isTrue();
		assertThat(present.isAbsent()).isFalse();
		assertThat(present.get()).isEqualTo("value");
		assertThat(present.orElse("other")).isEqualTo("value");
		assertThat(present.toString()).isEqualTo("Maybe.of(value)");
	}

	// This is what a provider representing JSON null as Java null holds, and the case
	// java.util.Optional cannot express. NullAway cannot express a null payload either, hence the
	// suppression -- that limitation is the reason Maybe exists.
	@SuppressWarnings("NullAway")
	@Test
	void testPresentNullIsPresentRatherThanAbsent() {
		Maybe<@Nullable String> present = Maybe.of(null);
		assertThat(present.isPresent()).isTrue();
		assertThat(present.isAbsent()).isFalse();
		assertThat(present.get()).isNull();
		// A present null beats the fallback; an absent value would not.
		assertThat(present.orElse("other")).isNull();
		assertThat(present).isNotEqualTo(Maybe.absent());
		assertThat(present.toString()).isEqualTo("Maybe.of(null)");
	}

	@Test
	void testEqualsAndHashCode() {
		assertThat(Maybe.of("a")).isEqualTo(Maybe.of("a"));
		assertThat(Maybe.of("a").hashCode()).isEqualTo(Maybe.of("a").hashCode());
		assertThat(Maybe.of("a")).isNotEqualTo(Maybe.of("b"));
		assertThat(Maybe.of("a")).isNotEqualTo(Maybe.absent());
		assertThat(Maybe.absent()).isEqualTo(Maybe.absent());
	}
}
