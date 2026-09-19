package net.thisptr.jackson.jq.v2.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

final class JsonTreeNode {
	private static final int DEFAULT_EXPAND_DEPTH = 1;

	private final @Nullable String key;
	private final int index;
	private final Object node;
	private final JsonNodeType type;
	private final @Nullable JsonTreeNode parent;
	private final List<JsonTreeNode> children;
	private final boolean leaf;
	private final String valueSummary;
	private final String displayText;
	private final int depth;
	private boolean expanded;

	private JsonTreeNode(
			@Nullable String key,
			int index,
			Object node,
			JsonNodeType type,
			@Nullable JsonTreeNode parent,
			List<JsonTreeNode> children,
			boolean leaf,
			String valueSummary,
			String displayText,
			int depth,
			boolean expanded) {
		this.key = key;
		this.index = index;
		this.node = node;
		this.type = type;
		this.parent = parent;
		this.children = children;
		this.leaf = leaf;
		this.valueSummary = valueSummary;
		this.displayText = displayText;
		this.depth = depth;
		this.expanded = expanded;
	}

	@Nullable
	String key() {
		return key;
	}

	int index() {
		return index;
	}

	Object node() {
		return node;
	}

	JsonNodeType type() {
		return type;
	}

	@Nullable
	JsonTreeNode parent() {
		return parent;
	}

	List<JsonTreeNode> children() {
		return children;
	}

	boolean isLeaf() {
		return leaf;
	}

	boolean isExpanded() {
		return expanded;
	}

	void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	void toggleExpanded() {
		this.expanded = !this.expanded;
	}

	String valueSummary() {
		return valueSummary;
	}

	String displayText() {
		return displayText;
	}

	int depth() {
		return depth;
	}

	void expandAncestors() {
		@Var JsonTreeNode p = this.parent;
		while (p != null) {
			p.setExpanded(true);
			p = p.parent;
		}
	}

	boolean matchesQuery(String query) {
		if (query == null || query.isEmpty()) {
			return false;
		}
		String q = query.toLowerCase(Locale.ROOT);
		if (key != null && key.toLowerCase(Locale.ROOT).contains(q)) {
			return true;
		}
		return displayText.toLowerCase(Locale.ROOT).contains(q);
	}

	void findMatches(String query, List<JsonTreeNode> matches) {
		if (matchesQuery(query)) {
			matches.add(this);
		}
		for (JsonTreeNode child : children) {
			child.findMatches(query, matches);
		}
	}

	@SuppressWarnings("unchecked")
	static <T> List<JsonTreeNode> buildRoots(JsonProvider<T> provider, List<?> rawItems) {
		if (rawItems == null || rawItems.isEmpty()) {
			return Collections.emptyList();
		}
		List<JsonTreeNode> roots = new ArrayList<>();
		boolean multipleRoots = rawItems.size() > 1;
		for (int i = 0; i < rawItems.size(); i++) {
			T item = (T) rawItems.get(i);
			int rootIndex = multipleRoots ? i : -1;
			JsonTreeNode root = buildNode(provider, item, null, rootIndex, null, 0);
			roots.add(root);
		}
		return Collections.unmodifiableList(roots);
	}

	private static <T> JsonTreeNode buildNode(
			JsonProvider<T> provider,
			T node,
			@Nullable String key,
			int index,
			@Nullable JsonTreeNode parent,
			int depth) {
		JsonNodeType type = provider.getNodeType(node);
		List<JsonTreeNode> childNodes = new ArrayList<>();
		boolean isLeaf;
		String summary;

		if (type == JsonNodeType.OBJECT) {
			int count = provider.getObjectMemberCount(node);
			isLeaf = (count == 0);
			summary = "{ " + count + (count == 1 ? " key" : " keys") + " }";
		} else if (type == JsonNodeType.ARRAY) {
			int length = provider.getArrayLength(node);
			isLeaf = (length == 0);
			summary = "[ " + length + (length == 1 ? " item" : " items") + " ]";
		} else if (type == JsonNodeType.STRING) {
			isLeaf = true;
			summary = provider.format(node);
		} else if (type == JsonNodeType.NULL) {
			isLeaf = true;
			summary = "null";
		} else if (type == JsonNodeType.BINARY) {
			isLeaf = true;
			summary = "<binary " + provider.getBinaryAsByteArray(node).length + " bytes>";
		} else {
			isLeaf = true;
			summary = provider.format(node);
		}

		StringBuilder display = new StringBuilder();
		if (key != null) {
			display.append('"').append(escapeJsonString(key)).append("\": ");
		} else if (index >= 0) {
			display.append('[').append(index).append("]: ");
		}
		display.append(summary);

		boolean initialExpanded = (depth <= DEFAULT_EXPAND_DEPTH && !isLeaf);
		JsonTreeNode current = new JsonTreeNode(
				key,
				index,
				node,
				type,
				parent,
				childNodes,
				isLeaf,
				summary,
				display.toString(),
				depth,
				initialExpanded);

		if (!isLeaf) {
			if (type == JsonNodeType.OBJECT) {
				Iterator<Map.Entry<String, T>> it = provider.getObjectMembers(node);
				while (it.hasNext()) {
					Map.Entry<String, T> entry = it.next();
					childNodes.add(buildNode(
							provider,
							entry.getValue(),
							entry.getKey(),
							-1,
							current,
							depth + 1));
				}
			} else if (type == JsonNodeType.ARRAY) {
				int length = provider.getArrayLength(node);
				for (int i = 0; i < length; i++) {
					T element = provider.getArrayElement(node, i);
					childNodes.add(buildNode(
							provider,
							element,
							null,
							i,
							current,
							depth + 1));
				}
			}
		}

		return current;
	}

	private static String escapeJsonString(String str) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			switch (c) {
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				default:
					if (c < ' ') {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
					break;
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return displayText;
	}
}
