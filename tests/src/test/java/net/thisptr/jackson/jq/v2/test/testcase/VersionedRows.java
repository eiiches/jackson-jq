package net.thisptr.jackson.jq.v2.test.testcase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Collapses a result observed per jq version into the {@code v:} ranges an assertion row takes.
 */
public final class VersionedRows {
	/**
	 * One assertion row: the range it covers, and the result that holds throughout it.
	 *
	 * @param <T> what was observed on each version
	 */
	public record Row<T>(String range, T value) {
	}

	/**
	 * Joins runs of consecutive versions sharing a result into one {@code [inclusive, exclusive)}
	 * range each, open-ended when the run reaches the newest configured version.
	 *
	 * <p>A version absent from {@code results} ends the run before it: nothing was observed there,
	 * so no range may reach across it.
	 *
	 * @param versions the configured versions, oldest first
	 * @param results what was observed, keyed by version
	 * @param <T> what was observed on each version
	 * @return one row per range, in version order
	 */
	public static <T> List<Row<T>> merge(List<Version> versions, Map<Version, T> results) {
		return merge(versions, results, Objects::equals);
	}

	/**
	 * Joins runs of consecutive versions sharing a result, deciding what "the same result" means with
	 * {@code sameResult} rather than {@link Object#equals}.
	 *
	 * <p>A row records one result for its whole range, so a run may only be joined when nothing that
	 * reads the rows can tell its members apart. Where that is coarser than {@code equals} -- an
	 * output compared by numeric value, say, rather than by how the number happens to be written --
	 * pass the coarser test here, or the generator writes rows no assertion can distinguish.
	 *
	 * <p>Each version is compared against the first version of the run, which is the result the row
	 * ends up recording, so {@code sameResult} need not be transitive.
	 *
	 * @param versions the configured versions, oldest first
	 * @param results what was observed, keyed by version
	 * @param sameResult whether two observed results belong in one row
	 * @param <T> what was observed on each version
	 * @return one row per range, in version order
	 */
	public static <T> List<Row<T>> merge(List<Version> versions, Map<Version, T> results, BiPredicate<T, T> sameResult) {
		List<Row<T>> rows = new ArrayList<>();
		@Var int start = -1;
		@Var @Nullable T current = null;
		for (int i = 0; i < versions.size(); i++) {
			@Nullable T result = results.get(versions.get(i));
			if (current != null && (result == null || !sameResult.test(current, result))) {
				rows.add(row(versions, start, i - 1, results));
				current = null;
			}
			if (result != null && current == null) {
				start = i;
				current = result;
			}
		}
		if (current != null)
			rows.add(row(versions, start, versions.size() - 1, results));
		return rows;
	}

	private static <T> Row<T> row(List<Version> versions, int first, int last, Map<Version, T> results) {
		String lower = VersionSpelling.of(versions.get(first));
		String range = last + 1 == versions.size()
				? "[" + lower + ", )"
				: "[" + lower + ", " + VersionSpelling.of(versions.get(last + 1)) + ")";
		return new Row<>(range, Objects.requireNonNull(results.get(versions.get(first))));
	}

	private VersionedRows() {
	}
}
