package net.thisptr.jackson.jq.v2.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.google.errorprone.annotations.Var;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.common.SizedWidget;
import dev.tamboui.widgets.input.TextInput;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;
import dev.tamboui.widgets.tree.GuideStyle;
import dev.tamboui.widgets.tree.TreeState;
import dev.tamboui.widgets.tree.TreeWidget;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

final class JsonTreePane {
	private static final int PAGE_SCROLL = 15;

	enum ViewMode {
		TREE,
		TEXT
	}

	private ViewMode viewMode;
	private List<JsonTreeNode> roots = Collections.emptyList();
	private final TreeState treeState = new TreeState();
	private List<TreeWidget.FlatEntry<JsonTreeNode>> lastFlatEntries = Collections.emptyList();
	private int treeViewportHeight = 1;

	JsonTreePane(ViewMode initialMode) {
		this.viewMode = Objects.requireNonNull(initialMode, "initialMode");
	}

	JsonTreePane() {
		this(ViewMode.TREE);
	}

	private List<String> textLines = Collections.emptyList();
	private int textScrollOffset;
	private int textViewportHeight = 1;
	private List<Integer> textMatches = new ArrayList<>();
	private int textMatchIndex = -1;

	private boolean searchActive;
	private final TextInputState searchInputState = new TextInputState();
	private String searchQuery = "";
	private List<JsonTreeNode> matches = new ArrayList<>();
	private int matchIndex = -1;

	<T> void setNodes(List<?> items, List<String> lines, JsonProvider<T> provider) {
		this.roots = JsonTreeNode.buildRoots(provider, items);
		this.textLines = lines != null ? lines : Collections.emptyList();
		this.treeState.selectFirst();
		this.textScrollOffset = 0;
		updateMatches();
	}

	<T> void setNodes(List<?> items, JsonProvider<T> provider) {
		setNodes(items, Collections.emptyList(), provider);
	}

	ViewMode viewMode() {
		return viewMode;
	}

	void setViewMode(ViewMode viewMode) {
		this.viewMode = viewMode;
	}

	void toggleViewMode() {
		this.viewMode = (this.viewMode == ViewMode.TREE) ? ViewMode.TEXT : ViewMode.TREE;
		if (!searchQuery.isEmpty()) {
			if (viewMode == ViewMode.TREE && !matches.isEmpty()) {
				jumpToMatch(matches.get(matchIndex));
			} else if (viewMode == ViewMode.TEXT && !textMatches.isEmpty()) {
				jumpToTextMatch(textMatches.get(textMatchIndex));
			}
		}
	}

	List<JsonTreeNode> roots() {
		return roots;
	}

	TreeState treeState() {
		return treeState;
	}

	List<TreeWidget.FlatEntry<JsonTreeNode>> lastFlatEntries() {
		return lastFlatEntries;
	}

	List<String> textLines() {
		return textLines;
	}

	int textScrollOffset() {
		return textScrollOffset;
	}

	int scrollOffset() {
		return viewMode == ViewMode.TREE ? treeState.offset() : textScrollOffset;
	}

	int viewportHeight() {
		return viewMode == ViewMode.TREE ? treeViewportHeight : textViewportHeight;
	}

	int selectedIndex() {
		return viewMode == ViewMode.TREE ? treeState.selected() : textScrollOffset;
	}

	boolean isSearchActive() {
		return searchActive;
	}

	String searchQuery() {
		return searchQuery;
	}

	String searchBuffer() {
		return searchInputState.text();
	}

	int searchCursor() {
		return searchInputState.cursorPosition();
	}

	List<JsonTreeNode> matches() {
		return Collections.unmodifiableList(matches);
	}

	int matchIndex() {
		return matchIndex;
	}

	List<Integer> textMatches() {
		return Collections.unmodifiableList(textMatches);
	}

	int textMatchIndex() {
		return textMatchIndex;
	}

	void clearSearch() {
		this.searchActive = false;
		this.searchQuery = "";
		this.searchInputState.clear();
		this.matches.clear();
		this.matchIndex = -1;
		this.textMatches.clear();
		this.textMatchIndex = -1;
	}

	private void updateMatches() {
		matches.clear();
		textMatches.clear();
		if (!searchQuery.isEmpty()) {
			for (JsonTreeNode root : roots) {
				root.findMatches(searchQuery, matches);
			}
			String lowerQuery = searchQuery.toLowerCase(Locale.ROOT);
			for (int i = 0; i < textLines.size(); i++) {
				if (textLines.get(i).toLowerCase(Locale.ROOT).contains(lowerQuery)) {
					textMatches.add(i);
				}
			}
		}
		if (matches.isEmpty()) {
			matchIndex = -1;
		} else {
			if (matchIndex < 0 || matchIndex >= matches.size()) {
				matchIndex = 0;
			}
		}
		if (textMatches.isEmpty()) {
			textMatchIndex = -1;
		} else {
			if (textMatchIndex < 0 || textMatchIndex >= textMatches.size()) {
				textMatchIndex = 0;
			}
		}
	}

	private void jumpToMatch(JsonTreeNode target) {
		target.expandAncestors();
		int visibleIndex = findVisibleIndex(target);
		if (visibleIndex >= 0) {
			treeState.select(visibleIndex);
		}
	}

	private void jumpToTextMatch(int lineIndex) {
		if (lineIndex < textScrollOffset) {
			textScrollOffset = lineIndex;
		} else if (lineIndex >= textScrollOffset + textViewportHeight) {
			textScrollOffset = lineIndex - textViewportHeight + 1;
		}
		int maxScroll = Math.max(0, textLines.size() - textViewportHeight);
		textScrollOffset = Math.max(0, Math.min(maxScroll, textScrollOffset));
	}

	private int findVisibleIndex(JsonTreeNode target) {
		int[] counter = new int[] { 0 };
		for (JsonTreeNode root : roots) {
			int found = findVisibleIndexHelper(root, target, counter);
			if (found >= 0) {
				return found;
			}
		}
		return -1;
	}

	private int findVisibleIndexHelper(JsonTreeNode current, JsonTreeNode target, int[] counter) {
		int idx = counter[0]++;
		if (current == target) {
			return idx;
		}
		if (current.isExpanded() && !current.isLeaf()) {
			for (JsonTreeNode child : current.children()) {
				int found = findVisibleIndexHelper(child, target, counter);
				if (found >= 0) {
					return found;
				}
			}
		}
		return -1;
	}

	void jumpMatch(int direction) {
		if (viewMode == ViewMode.TREE) {
			if (matches.isEmpty()) {
				return;
			}
			matchIndex = (matchIndex + direction + matches.size()) % matches.size();
			jumpToMatch(matches.get(matchIndex));
		} else {
			if (textMatches.isEmpty()) {
				return;
			}
			textMatchIndex = (textMatchIndex + direction + textMatches.size()) % textMatches.size();
			jumpToTextMatch(textMatches.get(textMatchIndex));
		}
	}

	private void selectNext() {
		if (!lastFlatEntries.isEmpty()) {
			treeState.selectNext(lastFlatEntries.size() - 1);
		}
	}

	private void selectLast() {
		if (!lastFlatEntries.isEmpty()) {
			treeState.selectLast(lastFlatEntries.size() - 1);
		}
	}

	private void expandSelected() {
		if (lastFlatEntries.isEmpty()) {
			return;
		}
		int idx = Math.min(treeState.selected(), lastFlatEntries.size() - 1);
		JsonTreeNode node = lastFlatEntries.get(idx).node();
		if (!node.isLeaf()) {
			if (node.isExpanded()) {
				if (!node.children().isEmpty() && idx + 1 < lastFlatEntries.size()) {
					treeState.select(idx + 1);
				}
			} else {
				node.setExpanded(true);
			}
		}
	}

	private void collapseSelected() {
		if (lastFlatEntries.isEmpty()) {
			return;
		}
		int idx = Math.min(treeState.selected(), lastFlatEntries.size() - 1);
		TreeWidget.FlatEntry<JsonTreeNode> entry = lastFlatEntries.get(idx);
		JsonTreeNode node = entry.node();
		if (node.isExpanded() && !node.isLeaf()) {
			node.setExpanded(false);
		} else {
			JsonTreeNode parent = entry.parent();
			if (parent != null) {
				for (int i = 0; i < lastFlatEntries.size(); i++) {
					if (lastFlatEntries.get(i).node() == parent) {
						treeState.select(i);
						break;
					}
				}
			}
		}
	}

	private void toggleSelected() {
		if (lastFlatEntries.isEmpty()) {
			return;
		}
		int idx = Math.min(treeState.selected(), lastFlatEntries.size() - 1);
		JsonTreeNode node = lastFlatEntries.get(idx).node();
		if (!node.isLeaf()) {
			node.toggleExpanded();
		}
	}

	private void onSearchQueryChanged() {
		searchQuery = searchInputState.text();
		updateMatches();
		if (viewMode == ViewMode.TREE) {
			if (!matches.isEmpty()) {
				matchIndex = 0;
				jumpToMatch(matches.get(0));
			}
		} else {
			if (!textMatches.isEmpty()) {
				textMatchIndex = 0;
				jumpToTextMatch(textMatches.get(0));
			}
		}
	}

	boolean handleKey(KeyEvent key) {
		if (searchActive) {
			if (key.code() == KeyCode.ESCAPE || key.isCancel() || key.isCtrlC()) {
				clearSearch();
				return true;
			}
			if (key.code() == KeyCode.ENTER || key.isConfirm()) {
				searchActive = false;
				if (viewMode == ViewMode.TREE) {
					if (!matches.isEmpty()) {
						if (matchIndex < 0 || matchIndex >= matches.size()) {
							matchIndex = 0;
						}
						jumpToMatch(matches.get(matchIndex));
					}
				} else {
					if (!textMatches.isEmpty()) {
						if (textMatchIndex < 0 || textMatchIndex >= textMatches.size()) {
							textMatchIndex = 0;
						}
						jumpToTextMatch(textMatches.get(textMatchIndex));
					}
				}
				return true;
			}
			// Ctrl+U: delete from cursor to start of line
			if (key.hasCtrl() && key.isChar('u')) {
				while (searchInputState.cursorPosition() > 0) {
					searchInputState.deleteBackward();
				}
				onSearchQueryChanged();
				return true;
			}
			// Ctrl+K: delete from cursor to end of line
			if (key.hasCtrl() && key.isChar('k')) {
				while (searchInputState.cursorPosition() < searchInputState.length()) {
					searchInputState.deleteForward();
				}
				onSearchQueryChanged();
				return true;
			}
			// Ctrl+W: delete word backward
			if (key.hasCtrl() && key.isChar('w')) {
				String cur = searchInputState.text();
				int col = searchInputState.cursorPosition();
				if (col > 0) {
					@Var int i = col - 1;
					while (i > 0 && Character.isWhitespace(cur.charAt(i))) {
						searchInputState.deleteBackward();
						i--;
					}
					while (i >= 0 && !Character.isWhitespace(cur.charAt(i))) {
						searchInputState.deleteBackward();
						i--;
					}
					onSearchQueryChanged();
				}
				return true;
			}
			// Ctrl+A / Home: move cursor to start of line
			if (key.isHome() || (key.hasCtrl() && key.isChar('a'))) {
				searchInputState.moveCursorToStart();
				return true;
			}
			// Ctrl+E / End: move cursor to end of line
			if (key.isEnd() || (key.hasCtrl() && key.isChar('e'))) {
				searchInputState.moveCursorToEnd();
				return true;
			}
			// Ctrl+H / Backspace
			if (key.isDeleteBackward() || key.code() == KeyCode.BACKSPACE || (key.hasCtrl() && key.isChar('h'))) {
				if (searchInputState.cursorPosition() == 0) {
					clearSearch();
					return true;
				}
				searchInputState.deleteBackward();
				onSearchQueryChanged();
				return true;
			}
			// Ctrl+D / Delete: delete forward
			if (key.isDeleteForward() || key.code() == KeyCode.DELETE || (key.hasCtrl() && key.isChar('d'))) {
				searchInputState.deleteForward();
				onSearchQueryChanged();
				return true;
			}
			if (key.isLeft()) {
				searchInputState.moveCursorLeft();
				return true;
			}
			if (key.isRight()) {
				searchInputState.moveCursorRight();
				return true;
			}
			if (!key.hasCtrl() && !key.hasAlt()) {
				String str = key.string();
				if (str != null && !str.isEmpty() && (key.code() == KeyCode.CHAR || str.charAt(0) >= 32)) {
					searchInputState.insert(str);
					onSearchQueryChanged();
					return true;
				}
			}
			return true;
		}

		// Normal mode in pane
		if (key.isCancel() || key.code() == KeyCode.ESCAPE) {
			if (!searchQuery.isEmpty()) {
				clearSearch();
				return true;
			}
		}
		if (key.isCharIgnoreCase('t')) {
			toggleViewMode();
			return true;
		}
		if (key.isChar('/')) {
			searchActive = true;
			searchInputState.clear();
			onSearchQueryChanged();
			return true;
		}
		if (key.isChar('n')) {
			jumpMatch(1);
			return true;
		}
		if (key.isChar('p') || key.isChar('N')) {
			jumpMatch(-1);
			return true;
		}
		if (key.hasCtrl() && key.isChar('d')) {
			scrollHalfPage(1);
			return true;
		}
		if (key.hasCtrl() && key.isChar('u')) {
			scrollHalfPage(-1);
			return true;
		}

		// Text mode navigation
		if (viewMode == ViewMode.TEXT) {
			int maxScroll = Math.max(0, textLines.size() - textViewportHeight);
			if (key.isDown() || key.isChar('j')) {
				textScrollOffset = Math.min(maxScroll, textScrollOffset + 1);
				return true;
			}
			if (key.isUp() || key.isChar('k')) {
				textScrollOffset = Math.max(0, textScrollOffset - 1);
				return true;
			}
			if (key.isHome() || key.isChar('g')) {
				textScrollOffset = 0;
				return true;
			}
			if (key.isEnd() || key.isChar('G')) {
				textScrollOffset = maxScroll;
				return true;
			}
			if (key.isPageDown()) {
				textScrollOffset = Math.min(maxScroll, textScrollOffset + PAGE_SCROLL);
				return true;
			}
			if (key.isPageUp()) {
				textScrollOffset = Math.max(0, textScrollOffset - PAGE_SCROLL);
				return true;
			}
			if (key.code() == KeyCode.ESCAPE) {
				if (!searchQuery.isEmpty()) {
					clearSearch();
					return true;
				}
				return false;
			}
			return false;
		}

		// Tree mode navigation
		if (key.isDown() || key.isChar('j')) {
			selectNext();
			return true;
		}
		if (key.isUp() || key.isChar('k')) {
			treeState.selectPrevious();
			return true;
		}
		if (key.isRight() || key.isChar('l')) {
			expandSelected();
			return true;
		}
		if (key.isLeft() || key.isChar('h')) {
			collapseSelected();
			return true;
		}
		if (key.isChar(' ')) {
			toggleSelected();
			return true;
		}
		if (key.isHome() || key.isChar('g')) {
			treeState.selectFirst();
			return true;
		}
		if (key.isEnd() || key.isChar('G')) {
			selectLast();
			return true;
		}
		if (key.isPageDown()) {
			if (!lastFlatEntries.isEmpty()) {
				int target = Math.min(lastFlatEntries.size() - 1, treeState.selected() + PAGE_SCROLL);
				treeState.select(target);
			}
			return true;
		}
		if (key.isPageUp()) {
			if (!lastFlatEntries.isEmpty()) {
				int target = Math.max(0, treeState.selected() - PAGE_SCROLL);
				treeState.select(target);
			}
			return true;
		}
		if (key.code() == KeyCode.ESCAPE) {
			if (!searchQuery.isEmpty()) {
				clearSearch();
				return true;
			}
			return false;
		}
		return false;
	}

	void render(
			Rect area,
			Buffer buffer,
			Frame frame,
			boolean isFocused,
			Block block,
			@Nullable String emptyMessage) {
		block.render(area, buffer);
		Rect inner = block.inner(area);
		if (inner.isEmpty()) {
			return;
		}

		boolean isEmpty = (viewMode == ViewMode.TREE) ? roots.isEmpty() : textLines.isEmpty();
		if (isEmpty) {
			if (emptyMessage != null) {
				Paragraph p = Paragraph.from(Line.from(Span.styled(emptyMessage, Style.EMPTY.dim())));
				p.render(inner, buffer);
			}
			return;
		}

		Rect contentArea;
		@Var @Nullable Rect searchArea = null;
		if (searchActive || !searchQuery.isEmpty()) {
			if (inner.height() > 1) {
				List<Rect> parts = Layout.vertical()
						.constraints(Constraint.fill(), Constraint.length(1))
						.split(inner);
				contentArea = parts.get(0);
				searchArea = parts.get(1);
			} else {
				contentArea = inner;
			}
		} else {
			contentArea = inner;
		}

		if (viewMode == ViewMode.TREE) {
			this.treeViewportHeight = Math.max(1, contentArea.height());
			TreeWidget<JsonTreeNode> treeWidget = TreeWidget.<JsonTreeNode>builder()
					.roots(roots)
					.children(JsonTreeNode::children)
					.isLeaf(JsonTreeNode::isLeaf)
					.expansionState(JsonTreeNode::isExpanded, JsonTreeNode::setExpanded)
					.nodeRenderer(this::renderNode)
					.guideStyle(GuideStyle.UNICODE)
					.highlightStyle(isFocused ? Style.EMPTY.reversed() : Style.EMPTY)
					.highlightSymbol(Line.from("> "))
					.scrollbar()
					.scrollbarThumbStyle(Style.EMPTY.fg(isFocused ? Color.CYAN : Color.GRAY))
					.scrollbarTrackStyle(Style.EMPTY.fg(isFocused ? Color.CYAN : Color.DARK_GRAY))
					.build();

			frame.renderStatefulWidget(treeWidget, contentArea, treeState);
			this.lastFlatEntries = treeWidget.lastFlatEntries();
		} else {
			renderTextView(contentArea, buffer, isFocused);
		}

		if (searchArea != null && searchArea.height() > 0) {
			renderSearchBar(searchArea, buffer, frame, isFocused);
		}
	}

	private void scrollHalfPage(int direction) {
		if (viewMode == ViewMode.TEXT) {
			int amount = Math.max(1, textViewportHeight / 2);
			int maxScroll = Math.max(0, textLines.size() - textViewportHeight);
			textScrollOffset = Math.max(0, Math.min(maxScroll, textScrollOffset + direction * amount));
		} else if (!lastFlatEntries.isEmpty()) {
			int amount = Math.max(1, treeViewportHeight / 2);
			int target = Math.max(0, Math.min(lastFlatEntries.size() - 1, treeState.selected() + direction * amount));
			treeState.select(target);
		}
	}

	private void renderTextView(Rect area, Buffer buffer, boolean isFocused) {
		this.textViewportHeight = Math.max(1, area.height());
		int maxScroll = Math.max(0, textLines.size() - textViewportHeight);
		if (textScrollOffset > maxScroll) {
			textScrollOffset = maxScroll;
		}

		List<Line> linesToRender = new ArrayList<>();
		int activeMatchLine = (!textMatches.isEmpty() && textMatchIndex >= 0 && textMatchIndex < textMatches.size())
				? textMatches.get(textMatchIndex)
				: -1;

		String lowerQuery = searchQuery.toLowerCase(Locale.ROOT);
		for (int lineIdx = 0; lineIdx < textLines.size(); lineIdx++) {
			String lineText = textLines.get(lineIdx);
			boolean isLineActiveMatch = (lineIdx == activeMatchLine);

			if (!searchQuery.isEmpty() && lineText.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
				List<Span> spans = new ArrayList<>();
				String lowerLine = lineText.toLowerCase(Locale.ROOT);
				@Var int start = 0;
				Style matchStyle = isLineActiveMatch
						? Style.EMPTY.bold().bg(Color.MAGENTA).fg(Color.WHITE)
						: Style.EMPTY.bold().bg(Color.YELLOW).fg(Color.BLACK);

				while (start < lineText.length()) {
					int matchPos = lowerLine.indexOf(lowerQuery, start);
					if (matchPos < 0) {
						spans.add(Span.raw(lineText.substring(start)));
						break;
					}
					if (matchPos > start) {
						spans.add(Span.raw(lineText.substring(start, matchPos)));
					}
					spans.add(Span.styled(lineText.substring(matchPos, matchPos + lowerQuery.length()), matchStyle));
					start = matchPos + lowerQuery.length();
				}
				linesToRender.add(Line.from(spans));
			} else {
				linesToRender.add(Line.from(lineText));
			}
		}

		Paragraph p = Paragraph.builder()
				.text(Text.from(linesToRender))
				.scroll(textScrollOffset)
				.build();
		p.render(area, buffer);

		if (textLines.size() > textViewportHeight && textViewportHeight > 0) {
			Scrollbar scrollbar = Scrollbar.builder()
					.orientation(ScrollbarOrientation.VERTICAL_RIGHT)
					.trackStyle(Style.EMPTY.fg(isFocused ? Color.CYAN : Color.DARK_GRAY))
					.thumbStyle(Style.EMPTY.fg(isFocused ? Color.WHITE : Color.GRAY))
					.build();
			ScrollbarState state = new ScrollbarState()
					.contentLength(textLines.size())
					.viewportContentLength(textViewportHeight)
					.position(textScrollOffset);
			Rect scrollbarRect = new Rect(area.right() - 1, area.top(), 1, textViewportHeight);
			scrollbar.render(scrollbarRect, buffer, state);
		}
	}

	private void renderSearchBar(Rect searchArea, Buffer buffer, Frame frame, boolean isFocused) {
		int total = (viewMode == ViewMode.TREE) ? matches.size() : textMatches.size();
		int current = (viewMode == ViewMode.TREE) ? matchIndex : textMatchIndex;
		String matchInfo;
		if (searchQuery.isEmpty()) {
			matchInfo = "";
		} else if (total == 0) {
			matchInfo = " [Pattern not found]";
		} else {
			matchInfo = " [" + (current + 1) + "/" + total + "]";
		}

		if (searchActive && isFocused) {
			int prefixWidth = 1;
			int infoWidth = matchInfo.length();
			int inputWidth = Math.max(1, searchArea.width() - prefixWidth - infoWidth);

			buffer.setString(searchArea.left(), searchArea.top(), "/", Style.EMPTY.bold().yellow());
			Rect inputRect = new Rect(searchArea.left() + prefixWidth, searchArea.top(), inputWidth, 1);
			TextInput inputWidget = TextInput.builder()
					.style(Style.EMPTY.bold().white())
					.cursorStyle(Style.EMPTY.reversed())
					.build();
			inputWidget.renderWithCursor(inputRect, buffer, searchInputState, frame);

			if (infoWidth > 0 && searchArea.left() + prefixWidth + inputWidth < searchArea.right()) {
				int infoX = searchArea.left() + prefixWidth + inputWidth;
				buffer.setString(infoX, searchArea.top(), matchInfo, total == 0 ? Style.EMPTY.red() : Style.EMPTY.cyan());
			}
		} else {
			List<Span> spans = new ArrayList<>();
			spans.add(Span.styled("/", Style.EMPTY.bold().yellow()));
			spans.add(Span.styled(searchQuery, Style.EMPTY.bold().white()));
			if (!matchInfo.isEmpty()) {
				spans.add(Span.styled(matchInfo, total == 0 ? Style.EMPTY.red() : Style.EMPTY.cyan()));
				spans.add(Span.styled(" (n/p/Esc)", Style.EMPTY.dim()));
			}
			Paragraph p = Paragraph.from(Line.from(spans));
			p.render(searchArea, buffer);
		}
	}

	private SizedWidget renderNode(JsonTreeNode node) {
		List<Span> spans = new ArrayList<>();
		String text = node.displayText();

		if (!searchQuery.isEmpty() && node.matchesQuery(searchQuery)) {
			String lowerText = text.toLowerCase(Locale.ROOT);
			String lowerQuery = searchQuery.toLowerCase(Locale.ROOT);
			boolean isActiveMatch = (!matches.isEmpty()
					&& matchIndex >= 0
					&& matchIndex < matches.size()
					&& matches.get(matchIndex) == node);
			Style matchStyle = isActiveMatch
					? Style.EMPTY.bold().bg(Color.MAGENTA).fg(Color.WHITE)
					: Style.EMPTY.bold().bg(Color.YELLOW).fg(Color.BLACK);

			@Var int start = 0;
			while (start < text.length()) {
				int matchPos = lowerText.indexOf(lowerQuery, start);
				if (matchPos < 0) {
					spans.add(Span.styled(text.substring(start), defaultNodeStyle(node)));
					break;
				}
				if (matchPos > start) {
					spans.add(Span.styled(text.substring(start, matchPos), defaultNodeStyle(node)));
				}
				spans.add(Span.styled(text.substring(matchPos, matchPos + lowerQuery.length()), matchStyle));
				start = matchPos + lowerQuery.length();
			}
		} else {
			if (node.key() != null) {
				spans.add(Span.styled("\"" + node.key() + "\": ", Style.EMPTY.bold().cyan()));
			} else if (node.index() >= 0) {
				spans.add(Span.styled("[" + node.index() + "]: ", Style.EMPTY.dim().cyan()));
			}
			spans.add(Span.styled(node.valueSummary(), defaultNodeStyle(node)));
		}

		return SizedWidget.of(Paragraph.from(Line.from(spans)));
	}

	private Style defaultNodeStyle(JsonTreeNode node) {
		switch (node.type()) {
			case STRING:
				return Style.EMPTY.green();
			case NUMBER:
				return Style.EMPTY.yellow();
			case BOOLEAN:
				return Style.EMPTY.magenta();
			case NULL:
				return Style.EMPTY.dim();
			case OBJECT:
			case ARRAY:
				return Style.EMPTY.white();
			case BINARY:
			default:
				return Style.EMPTY.dim();
		}
	}
}
