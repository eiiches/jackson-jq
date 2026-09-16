package net.thisptr.jackson.jq.v2.ext.re2;

import com.google.errorprone.annotations.Var;

final class UnicodeIndex {
	private final int[] codePointIndexes;

	UnicodeIndex(String value) {
		codePointIndexes = new int[value.length() + 1];
		@Var int codePointIndex = 0;
		for (int offset = 0; offset < value.length(); ) {
			int width = Character.charCount(value.codePointAt(offset));
			codePointIndexes[offset] = codePointIndex;
			if (width == 2)
				codePointIndexes[offset + 1] = codePointIndex;
			offset += width;
			codePointIndex++;
		}
		codePointIndexes[value.length()] = codePointIndex;
	}

	int at(int utf16Index) {
		return codePointIndexes[utf16Index];
	}
}
