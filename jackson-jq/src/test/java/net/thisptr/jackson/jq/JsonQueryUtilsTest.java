package net.thisptr.jackson.jq;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

public class JsonQueryUtilsTest {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Item {
		@JsonProperty("id")
		public int id;

		@JsonProperty("score")
		public double score;

		@JsonCreator
		public Item(@JsonProperty("id") final int id, @JsonProperty("score") final double score) {
			this.id = id;
			this.score = score;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Item other = (Item) obj;
			if (id != other.id)
				return false;
			if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score))
				return false;
			return true;
		}
	}

	public static final List<Item> input = Arrays.asList(new Item[] {
			new Item(1, 0.9),
			new Item(3, 1.2),
			new Item(2, 0.7),
	});

	public static final List<Item> sortResultById = Arrays.asList(new Item[] {
			new Item(1, 0.9),
			new Item(2, 0.7),
			new Item(3, 1.2),
	});

	public static final List<Item> sortResult = Arrays.asList(new Item[] {
			new Item(1, 0.9),
			new Item(2, 0.7),
			new Item(3, 1.2),
	});

	public static final List<Item> sortResultByScore = Arrays.asList(new Item[] {
			new Item(2, 0.7),
			new Item(1, 0.9),
			new Item(3, 1.2),
	});

	@Test
	public void sort() throws IOException {
		assertEquals(Arrays.asList(sortResult), JsonQueryUtils.apply(DefaultRootScope.getInstance(), JsonQuery.compile("sort"), input, new TypeReference<List<Item>>() {}));
		assertEquals(Arrays.asList(sortResultById), JsonQueryUtils.apply(DefaultRootScope.getInstance(), JsonQuery.compile("sort(.id)"), input, new TypeReference<List<Item>>() {}));
		assertEquals(Arrays.asList(sortResultByScore), JsonQueryUtils.apply(DefaultRootScope.getInstance(), JsonQuery.compile("sort(.score)"), input, new TypeReference<List<Item>>() {}));
		assertEquals(Arrays.asList(1, 2, 3), JsonQueryUtils.apply(DefaultRootScope.getInstance(), JsonQuery.compile("[.[].id] | sort | .[]"), input, Integer.class));
	}
}
