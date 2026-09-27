package net.thisptr.jackson.jq.v2.test.comparator;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

public record FloatTolerance(
		@JsonProperty("ulps") @Nullable Long ulps
) {
}
