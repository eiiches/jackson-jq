package net.thisptr.jackson.jq.internal.misc;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.jcodings.specific.UTF8Encoding;
import org.joni.NameEntry;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Syntax;

import net.thisptr.jackson.jq.exception.JsonQueryException;

public class OnigUtils {
	public static class Pattern {
		public final Regex regex;
		public final boolean global;
		public final String[] names;

		public Pattern(final String regexText, final String flags) throws JsonQueryException {
			final int modifiers = parseModifiers(flags) | Option.CAPTURE_GROUP;
			final byte[] regexBytes = regexText.getBytes(StandardCharsets.UTF_8);
			this.regex = new Regex(regexBytes, 0, regexBytes.length, modifiers, UTF8Encoding.INSTANCE, Syntax.PerlNG);
			this.global = isGlobal(flags);
			this.names = names(regex);
		}

		private static String[] names(final Regex regex) {
			final String[] names = new String[regex.numberOfCaptures() + 1];
			if (regex.numberOfNames() == 0)
				return names;
			for (final Iterator<NameEntry> iter = regex.namedBackrefIterator(); iter.hasNext();) {
				final NameEntry backref = iter.next();
				final String name = new String(backref.name, backref.nameP, backref.nameEnd - backref.nameP, StandardCharsets.UTF_8);
				for (final int index : backref.getBackRefs()) {
					names[index] = name;
				}
			}
			return names;
		}
	}

	public static boolean isGlobal(final String flags) {
		if (flags == null)
			return false;
		return flags.contains("g");
	}

	public static int parseModifiers(final String flags) throws JsonQueryException {
		if (flags == null)
			return Option.NONE;

		int result = Option.NONE;
		for (final byte ch : flags.getBytes(StandardCharsets.UTF_8)) {
			switch (ch) {
				case 'g':
					// ignore for now
					break;
				case 'i':
					result |= Option.IGNORECASE;
					break;
				case 'm':
					result |= Option.MULTILINE;
					break;
				case 'n':
					result |= Option.FIND_NOT_EMPTY;
					break;
				case 'p':
					result |= Option.MULTILINE | Option.SINGLELINE;
					break;
				case 's':
					result |= Option.SINGLELINE;
					break;
				case 'l':
					result |= Option.FIND_LONGEST;
					break;
				case 'x':
					result |= Option.EXTEND;
					break;
				default:
					throw new JsonQueryException("%s is not a valid modifier string", flags);
			}
		}
		return result;
	}
}
