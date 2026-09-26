/*
 * Most of the function definitions in this file originate from builtin.jq (*1)
 * in the official jq repository.
 *
 * 1) https://github.com/jqlang/jq/blob/master/src/builtin.jq
 *
 * jq is copyright (C) 2012 Stephen Dolan
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.util.Arrays;
import java.util.List;

import net.thisptr.jackson.jq.v2.spi.FunctionParameter;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * The jq-language surface over the regex primitives.
 * <p>
 * Every definition here reads a string and passes its arguments straight through to
 * {@code _match_impl} or {@code _sub_impl}, so each publishes what that primitive answers. Left to
 * its body, a definition would instead report the primitive's whole {@code BOOLEAN|[Match]} union --
 * or, where the body dispatches on {@code $val|type}, which this analysis cannot read, nothing at
 * all.
 */
public class RegexJqLibrary implements JqLibrary {
	private static final Type STRING = StringType.getInstance();

	private static final List<JqFunction> FUNCTIONS = List.of(
			JqFunction.of("match", args("re", "mode"), "_match_impl(re; mode; false)|.[]", List.of(
					TypeScheme.of(FunctionType.of(STRING, Types.MATCH_OBJECT,
							FilterType.of(STRING, STRING),
							FilterType.of(STRING, STRING)))
			)),
			JqFunction.of("match", args("$val"), "($val|type) as $vt | if $vt == \"string\" then match($val; \"\") elif $vt == \"array\" and ($val | length) > 1 then match($val[0]; $val[1]) elif $vt == \"array\" and ($val | length) > 0 then match($val[0]; \"\") else error( $vt + \" not a string or array\") end", List.of(
					TypeScheme.of(FunctionType.of(STRING, Types.MATCH_OBJECT, // match(REGEX)
							FilterType.of(STRING, STRING))),
					TypeScheme.of(FunctionType.of(STRING, Types.MATCH_OBJECT, // match([REGEX])
							FilterType.of(STRING, ArrayType.of(List.of(STRING))))),
					TypeScheme.of(FunctionType.of(STRING, Types.MATCH_OBJECT, // match([REGEX, FLAGS])
							FilterType.of(STRING, ArrayType.of(List.of(STRING, STRING)))))
			)),
			JqFunction.of("test", args("re", "mode"), "_match_impl(re; mode; true)", List.of(
					TypeScheme.of(FunctionType.of(STRING, BooleanType.getInstance(),
							FilterType.of(STRING, STRING),
							FilterType.of(STRING, STRING)))
			)),
			JqFunction.of("test", args("$val"), "($val|type) as $vt | if $vt == \"string\" then test($val; \"\") elif $vt == \"array\" and ($val | length) > 1 then test($val[0]; $val[1]) elif $vt == \"array\" and ($val | length) > 0 then test($val[0]; \"\") else error( $vt + \" not a string or array\") end", List.of(
					TypeScheme.of(FunctionType.of(STRING, BooleanType.getInstance(), // test(REGEX)
							FilterType.of(STRING, STRING))),
					TypeScheme.of(FunctionType.of(STRING, BooleanType.getInstance(), // test([REGEX])
							FilterType.of(STRING, ArrayType.of(List.of(STRING))))),
					TypeScheme.of(FunctionType.of(STRING, BooleanType.getInstance(), // test([REGEX, FLAGS])
							FilterType.of(STRING, ArrayType.of(List.of(STRING, STRING)))))
			)),
			JqFunction.of("capture", args("re", "mods"), "match(re; mods) | reduce ( .captures | .[] | select(.name != null) | { (.name) : .string } ) as $pair ({}; . + $pair)", List.of(
					TypeScheme.of(FunctionType.of(STRING, Types.CAPTURES,
							FilterType.of(STRING, STRING),
							FilterType.of(STRING, STRING)))
			)),
			JqFunction.of("capture", args("$val"), "($val|type) as $vt | if $vt == \"string\" then capture($val; \"\") elif $vt == \"array\" and ($val | length) > 1 then capture($val[0]; $val[1]) elif $vt == \"array\" and ($val | length) > 0 then capture($val[0]; \"\") else error( $vt + \" not a string or array\") end", List.of(
					TypeScheme.of(FunctionType.of(STRING, Types.CAPTURES, // capture(REGEX)
							FilterType.of(STRING, STRING))),
					TypeScheme.of(FunctionType.of(STRING, Types.CAPTURES, // capture([REGEX])
							FilterType.of(STRING, ArrayType.of(List.of(STRING))))),
					TypeScheme.of(FunctionType.of(STRING, Types.CAPTURES, // capture([REGEX, FLAGS])
							FilterType.of(STRING, ArrayType.of(List.of(STRING, STRING)))))
			)),
			JqFunction.of("scan", args("re"), "scan(re; \"\")", List.of(
					TypeScheme.of(FunctionType.of(STRING, UnionType.of(STRING, ArrayType.of(Types.STRING_OR_NULL)),
							FilterType.of(STRING, STRING)))
			)),
			JqFunction.of("scan", args("re", "flags"), "match(re; flags + \"g\") | if (.captures|length > 0) then [ .captures | .[] | .string ] else .string end", List.of(
					TypeScheme.of(FunctionType.of(STRING, UnionType.of(STRING, ArrayType.of(Types.STRING_OR_NULL)),
							FilterType.of(STRING, STRING),
							FilterType.of(STRING, STRING)))
			)),
			JqFunction.of("_nwise", args("a", "$n"), "if a|length <= $n then a else a[0:$n] , _nwise(a[$n:]; $n) end"),
			JqFunction.of("_nwise", args("$n"), "_nwise(.; $n)"),
			JqFunction.of("splits", args("$re", "flags"), ". as $s | [ match($re; \"g\" + flags) | (.offset, .offset + .length) ] | [0] + . +[$s|length] | _nwise(2) | $s[.[0]:.[1] ]", List.of(
					TypeScheme.of(FunctionType.of(STRING, STRING,
							FilterType.of(STRING, STRING),
							FilterType.of(STRING, STRING)))
			)),
			JqFunction.of("splits", args("$re"), "splits($re; \"\")", List.of(
					TypeScheme.of(FunctionType.of(STRING, STRING, FilterType.of(STRING, STRING)))
			)),
			JqFunction.of("split", args("$re", "flags"), "[splits($re; flags)]", List.of(
					TypeScheme.of(FunctionType.of(STRING, ArrayType.of(STRING),
							FilterType.of(STRING, STRING),
							FilterType.of(STRING, STRING)))
			)),
			JqFunction.of("sub", args("$re", "s"), "_sub_impl($re; s; \"\")", List.of(
					TypeScheme.of(FunctionType.of(STRING, STRING,
							FilterType.of(STRING, STRING),
							FilterType.of(Types.CAPTURES, STRING)))
			)),
			JqFunction.of("sub", args("$re", "s", "flags"), "_sub_impl($re; s; flags)", List.of(
					TypeScheme.of(FunctionType.of(STRING, STRING,
							FilterType.of(STRING, STRING),
							FilterType.of(Types.CAPTURES, STRING),
							FilterType.of(STRING, STRING)))
			)),
			JqFunction.of("gsub", args("$re", "s", "flags"), "_sub_impl($re; s; flags + \"g\")", List.of(
					TypeScheme.of(FunctionType.of(STRING, STRING,
							FilterType.of(STRING, STRING),
							FilterType.of(Types.CAPTURES, STRING),
							FilterType.of(STRING, STRING)))
			)),
			JqFunction.of("gsub", args("$re", "s"), "_sub_impl($re; s; \"g\")", List.of(
					TypeScheme.of(FunctionType.of(STRING, STRING,
							FilterType.of(STRING, STRING),
							FilterType.of(Types.CAPTURES, STRING)))
			))
	);

	private static List<FunctionParameter> args(String... args) {
		return Arrays.stream(args).map(FunctionParameter::valueOf).toList();
	}

	@Override
	public List<JqFunction> getJqFunctions() {
		return FUNCTIONS;
	}
}
