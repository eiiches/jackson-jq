/*
 * Most of the function definitions in this file originate from builtin.jq (*1)
 * in the official jq repository.
 *
 * 1) https://github.com/stedolan/jq/blob/master/src/builtin.jq
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
package net.thisptr.jackson.jq.v2.core.internal.builtins.library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.FunctionParameter;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

public class CoreJqLibrary implements JqLibrary {
	private static final Type ARRAY_ANY = ArrayType.of(AnyType.getInstance());
	private static final Type OBJECT_ANY = ObjectType.of(AnyType.getInstance());
	private static final Type SCALAR = UnionType.of(
			NullType.getInstance(), BooleanType.getInstance(), NumericType.getInstance(), StringType.getInstance());

	private static List<TypeScheme<FunctionType>> filterScheme(Type bound, Type... otherTypes) {
		TypeVariable t = TypeVariable.of("T");
		List<Type> inputs = new ArrayList<>();
		inputs.add(t);
		inputs.addAll(Arrays.asList(otherTypes));
		return List.of(TypeScheme.of(Map.of(t, bound),
				FunctionType.of(UnionType.of(inputs), t)));
	}

	private static final List<TypeScheme<FunctionType>> ARRAYS = filterScheme(
			ARRAY_ANY,
			OBJECT_ANY,
			BooleanType.getInstance(),
			NumericType.getInstance(),
			StringType.getInstance(),
			NullType.getInstance());

	private static final List<TypeScheme<FunctionType>> OBJECTS = filterScheme(
			OBJECT_ANY,
			ARRAY_ANY,
			BooleanType.getInstance(),
			NumericType.getInstance(),
			StringType.getInstance(),
			NullType.getInstance());

	private static final List<TypeScheme<FunctionType>> ITERABLES = filterScheme(
			UnionType.of(ARRAY_ANY, OBJECT_ANY),
			BooleanType.getInstance(),
			NumericType.getInstance(),
			StringType.getInstance(),
			NullType.getInstance());

	private static final List<TypeScheme<FunctionType>> SCALARS = filterScheme(
			SCALAR,
			ARRAY_ANY,
			OBJECT_ANY);

	private static final List<TypeScheme<FunctionType>> VALUES = filterScheme(
			UnionType.of(ARRAY_ANY, OBJECT_ANY, BooleanType.getInstance(), NumericType.getInstance(), StringType.getInstance()),
			NullType.getInstance());

	private static final List<TypeScheme<FunctionType>> NUMBERS = filterScheme(
			NumericType.getInstance(),
			ARRAY_ANY,
			OBJECT_ANY,
			BooleanType.getInstance(),
			StringType.getInstance(),
			NullType.getInstance());

	private static final List<TypeScheme<FunctionType>> STRINGS =
			List.of(TypeScheme.of(FunctionType.of(AnyType.getInstance(), StringType.getInstance())));

	private static final List<TypeScheme<FunctionType>> BOOLEANS =
			List.of(TypeScheme.of(FunctionType.of(AnyType.getInstance(), BooleanType.getInstance())));

	private static final List<TypeScheme<FunctionType>> NULLS =
			List.of(TypeScheme.of(FunctionType.of(AnyType.getInstance(), NullType.getInstance())));

	private static final List<TypeScheme<FunctionType>> NUMERIC =
			List.of(TypeScheme.of(FunctionType.of(AnyType.getInstance(), NumericType.getInstance())));

	/**
	 * {@code combinations} picks one element from each of its input's arrays, so an input that is not an
	 * array of arrays has nothing to pick from. Its body says as much by indexing into {@code .[0]}, but
	 * only for an input whose type already reaches that far; declaring it states the same thing about
	 * every call.
	 */
	private static final List<TypeScheme<FunctionType>> COMBINATIONS = combinations();

	/**
	 * {@code flatten} unnests the arrays inside an array, to a depth its argument sets. How deep the result
	 * is nested back is not a substitution of anything its input says, so the element type it publishes is
	 * the widest one -- which is still more than its body, analysed against a guard on {@code type} this
	 * analysis cannot read, is able to work out.
	 */
	private static final List<TypeScheme<FunctionType>> FLATTEN = flatten(0);

	private static final List<TypeScheme<FunctionType>> FLATTEN_DEPTH = flatten(1);

	private static List<TypeScheme<FunctionType>> flatten(int parameters) {
		TypeVariable element = TypeVariable.of("T");
		return List.of(TypeScheme.of(Map.of(element, AnyType.getInstance()),
				FunctionType.of(ArrayType.of(element), ArrayType.of(AnyType.getInstance()), Collections.nCopies(parameters, FilterType.of(ArrayType.of(element), NumericType.getInstance())).toArray(FilterType[]::new))));
	}

	/**
	 * {@code recurse} emits its input and then everything below it, so it accepts every value and yields
	 * every value. Its body says more than that for an input whose shape is known -- it walks the shape --
	 * but only by analysing a descent the caller did not write and cannot change. {@code ..} is the same
	 * operation and keeps that precision, since the compiler answers it directly.
	 */
	private static final List<TypeScheme<FunctionType>> RECURSE =
			List.of(TypeScheme.of(FunctionType.of(AnyType.getInstance(), AnyType.getInstance())));

	private static List<TypeScheme<FunctionType>> combinations() {
		TypeVariable element = TypeVariable.of("T");
		return List.of(TypeScheme.of(Map.of(element, AnyType.getInstance()),
				FunctionType.of(ArrayType.of(ArrayType.of(element)), ArrayType.of(element))));
	}

	private static final List<JqFunction> FUNCTIONS = List.of(
			JqFunction.of("@text", args(), "tostring"),
			JqFunction.of("@json", args(), "tojson"),
			// jq 1.5's `..` skips null-valued members, so their paths are not yielded; from 1.6 on it visits them.
			JqFunction.of("paths", args(), "paths(true)", VersionRange.valueOf("[1.6, )")),
			JqFunction.of("paths", args(), "paths(. != null)", VersionRange.valueOf("[, 1.6)")),
			JqFunction.of("arrays", args(), "select(type == \"array\")", ARRAYS),
			JqFunction.of("booleans", args(), "select(type == \"boolean\")", BOOLEANS),
			JqFunction.of("del", args("f"), "delpaths([path(f)])"),
			JqFunction.of("nulls", args(), "select(type == \"null\")", NULLS),
			JqFunction.of("objects", args(), "select(type == \"object\")", OBJECTS),
			JqFunction.of("numbers", args(), "select(type == \"number\")", NUMBERS),
			JqFunction.of("strings", args(), "select(type == \"string\")", STRINGS),
			JqFunction.of("finites", args(), "select(isfinite)", NUMERIC),
			JqFunction.of("normals", args(), "select(isnormal)", NUMERIC),
			JqFunction.of("values", args(), "booleans, numbers, strings, arrays, objects", VALUES),
			JqFunction.of("iterables", args(), "arrays, objects", ITERABLES),
			JqFunction.of("scalars", args(), "nulls, booleans, numbers, strings", SCALARS),
			JqFunction.of("isfinite", args(), "type == \"number\" and (isinfinite | not)"),
			JqFunction.of("add", args(), "reduce .[] as $item (null; . + $item)"),
			JqFunction.of("min", args(), "min_by(.)"),
			JqFunction.of("max", args(), "max_by(.)"),
			JqFunction.of("sort", args(), "sort_by(.)"),
			JqFunction.of("unique", args(), "group_by(.) | map(.[0])"),
			JqFunction.of("unique_by", args("f"), "group_by(f) | map(.[0])"),
			JqFunction.of("with_entries", args("f"), "to_entries | map(f) | from_entries"),
			JqFunction.of("select", args("pred"), "if pred then . else empty end"),
			JqFunction.of("map", args("f"), "[.[] | f]"),
			JqFunction.of("recurse", args("f"), "def r: ., (f | select(. != null) | r); r", VersionRange.valueOf("[, 1.6)")),
			JqFunction.of("recurse", args("f"), "def r: ., (f | r); r", VersionRange.valueOf("[1.6, )")),
			JqFunction.of("recurse", args("f", "cond"), "def r: ., (f | select(cond) | r); r"),
			JqFunction.of("recurse", args(), "recurse(.[]?)", RECURSE),
			JqFunction.of("recurse_down", args(), "recurse", RECURSE),
			JqFunction.of("last", args(), ".[-1]"),
			JqFunction.of("last", args("stream"), "reduce stream as $i (null; $i)"),
			JqFunction.of("first", args(), ".[0]"),
			JqFunction.of("first", args("g"), "label $out | foreach g as $item ([false, null]; if .[0]==true then break $out else [true, $item] end; .[1])", VersionRange.valueOf("[, 1.6)")),
			JqFunction.of("first", args("g"), "label $out | g | ., break $out", VersionRange.valueOf("[1.6, )")),
			JqFunction.of("nth", args("n"), "n as $n | .[$n]"),
			JqFunction.of("transpose", args(), "if . == [] then [] else . as $in | (map(length) | max // 0) as $max | length as $length | reduce range(0; $max) as $j ([]; . + [reduce range(0; $length) as $i ([]; . + [$in[$i][$j]])]) end"),
			JqFunction.of("limit", args("$n", "exp"), "if $n < 0 then exp else label $out | foreach exp as $item ([$n, null]; if .[0] < 1 then break $out else [.[0] -1, $item] end; .[1]) end"),
			JqFunction.of("nth", args("n", "g"), "n as $n | if $n < 0 then error(\"nth doesn't support negative indices\") else last(limit($n + 1; g)) end"),
			JqFunction.of("any", args("generator", "condition"), "[label $out | foreach generator as $i (false; if . then break $out elif $i | condition then true else . end; if . then . else empty end)] | length == 1"),
			JqFunction.of("any", args("condition"), "any(.[]; condition)"),
			JqFunction.of("any", args(), "any(.)"),
			JqFunction.of("all", args("generator", "condition"), "[label $out | foreach generator as $i (true; if .|not then break $out elif $i | condition then . else false end; if .|not then . else empty end)] | length == 0"),
			JqFunction.of("all", args("condition"), "all(.[]; condition)"),
			JqFunction.of("all", args(), "all(.)"),
			JqFunction.of("flatten", args(), "_flatten(-1)", FLATTEN),
			JqFunction.of("flatten", args("$x"), "if $x < 0 then error(\"flatten depth must not be negative\") else _flatten($x) end",
					FLATTEN_DEPTH),
			JqFunction.of("_flatten", args("$x"), "reduce .[] as $i ([]; if $i | type == \"array\" and $x != 0 then . + ($i | _flatten($x-1)) else . + [$i] end)"),
			JqFunction.of("ascii_downcase", args(), "explode | map( if 65 <= . and . <= 90 then . + 32  else . end) | implode"),
			JqFunction.of("ascii_upcase", args(), "explode | map( if 97 <= . and . <= 122 then . - 32  else . end) | implode"),
			JqFunction.of("until", args("cond", "next"), "def _until: if cond then . else (next|_until) end; _until"),
			JqFunction.of("while", args("cond", "update"), "def _while: if cond then ., (update | _while) else empty end; _while"),
			JqFunction.of("leaf_paths", args(), "paths(scalars)"),
			// jq 1.7 redefined walk/1 in terms of map_values, so a generator f keeps its first output per
			// object member instead of its last.
			JqFunction.of("walk", args("f"), ". as $in | if type == \"object\" then reduce keys_unsorted[] as $key ( {}; . + { ($key):  ($in[$key] | walk(f)) } ) | f elif type == \"array\" then map( walk(f) ) | f else f end", VersionRange.valueOf("[1.6, 1.7)")),
			JqFunction.of("walk", args("f"), "def w: if type == \"object\" then map_values(w) elif type == \"array\" then map(w) else . end | f; w", VersionRange.valueOf("[1.7, )")),
			JqFunction.of("in", args("xs"), ". as $x | xs | has($x)"),
			JqFunction.of("inside", args("xs"), ". as $x | xs | contains($x)"),
			JqFunction.of("combinations", args(), "if length == 0 then [] else .[0][] as $x | (.[1:] | combinations) as $y | [$x] + $y end",
					COMBINATIONS),
			JqFunction.of("combinations", args("n"), ". as $dot | [range(n) | $dot] | combinations"),
			JqFunction.of("map_values", args("f"), "_modify(.[]; f)"),
			JqFunction.of("_modify", args("paths", "update"), "reduce path(paths) as $p ([., []]; . as $dot | null | label $out | ($dot[0] | getpath($p)) as $value | (($value | update | (., break $out) as $updated | $dot | setpath([0] + $p; $updated)), ($dot | setpath([1, (.[1] | length)]; $p)))) | . as $dot | $dot[0] | delpaths($dot[1])", VersionRange.valueOf("[1.7, )")),
			JqFunction.of("_modify", args("paths", "update"), "reduce path(paths) as $p (.; label $out | (setpath($p; getpath($p) | update) | ., break $out), delpaths([$p]))", VersionRange.valueOf("[1.6, 1.7)")),
			JqFunction.of("_modify", args("paths", "update"), "reduce path(paths) as $p (.; setpath($p; getpath($p) | update))", VersionRange.valueOf("[, 1.6)")),
			JqFunction.of("pick", args("pathexps"), ". as $in | reduce path(pathexps) as $a (null; setpath($a; $in|getpath($a)) )", VersionRange.valueOf("[1.7, )")),
			JqFunction.of("ltrimstr", args("$left"), "if startswith($left) then .[$left | length:] else . end", VersionRange.valueOf("[1.8.0, )")),
			JqFunction.of("rtrimstr", args("$right"), "if endswith($right) then .[:$right | -length] else . end", VersionRange.valueOf("[1.8.0, 1.8.2)")),
			JqFunction.of("rtrimstr", args("$right"), "if endswith($right) then .[:length - ($right | length)] else . end", VersionRange.valueOf("[1.8.2, )")),
			JqFunction.of("trimstr", args("$val"), "ltrimstr($val) | rtrimstr($val)", VersionRange.valueOf("[1.8.0, )")));

	private static List<FunctionParameter> args(String... args) {
		return Arrays.stream(args).map(FunctionParameter::valueOf).toList();
	}

	@Override
	public List<JqFunction> getJqFunctions() {
		return FUNCTIONS;
	}
}
