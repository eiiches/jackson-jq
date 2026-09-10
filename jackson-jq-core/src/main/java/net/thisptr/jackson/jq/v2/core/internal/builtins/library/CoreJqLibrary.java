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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.FunctionParameter;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

@AutoService(JqLibrary.class)
public class CoreJqLibrary implements JqLibrary {
	private static final List<JqFunction> FUNCTIONS = Collections.unmodifiableList(Arrays.asList(
			JqFunction.of("@text", args(), "tostring"),
			JqFunction.of("@json", args(), "tojson"),
			JqFunction.of("paths", args(), "paths(. != null)"),
			JqFunction.of("arrays", args(), "select(type == \"array\")"),
			JqFunction.of("booleans", args(), "select(type == \"boolean\")"),
			JqFunction.of("del", args("f"), "delpaths([path(f)])"),
			JqFunction.of("nulls", args(), "select(type == \"null\")"),
			JqFunction.of("objects", args(), "select(type == \"object\")"),
			JqFunction.of("numbers", args(), "select(type == \"number\")"),
			JqFunction.of("strings", args(), "select(type == \"string\")"),
			JqFunction.of("finites", args(), "select(isfinite)"),
			JqFunction.of("normals", args(), "select(isnormal)"),
			JqFunction.of("values", args(), "booleans, numbers, strings, arrays, objects"),
			JqFunction.of("iterables", args(), "arrays, objects"),
			JqFunction.of("scalars", args(), "nulls, booleans, numbers, strings"),
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
			JqFunction.of("recurse", args("f"), "def r: ., (f | select(. != null) | r); r"),
			JqFunction.of("recurse", args("f", "cond"), "def r: ., (f | select(cond) | r); r"),
			JqFunction.of("recurse", args(), "recurse(.[]?)"),
			JqFunction.of("recurse_down", args(), "recurse"),
			JqFunction.of("last", args(), ".[-1]"),
			JqFunction.of("last", args("stream"), "reduce stream as $i (null; $i)"),
			JqFunction.of("first", args(), ".[0]"),
			JqFunction.of("first", args("g"), "label $out | foreach g as $item ([false, null]; if .[0]==true then break $out else [true, $item] end; .[1])", VersionRange.valueOf("[, 1.6)")),
			JqFunction.of("first", args("g"), "label $out | g | ., break $out", VersionRange.valueOf("[1.6, )")),
			JqFunction.of("nth", args("n"), "n as $n | .[$n]"),
			JqFunction.of("transpose", args(), "if . == [] then [] else . as $in | (map(length) | max) as $max | length as $length | reduce range(0; $max) as $j ([]; . + [reduce range(0; $length) as $i ([]; . + [$in[$i][$j]])]) end"),
			JqFunction.of("limit", args("$n", "exp"), "if $n < 0 then exp else label $out | foreach exp as $item ([$n, null]; if .[0] < 1 then break $out else [.[0] -1, $item] end; .[1]) end"),
			JqFunction.of("nth", args("n", "g"), "n as $n | if $n < 0 then error(\"nth doesn't support negative indices\") else last(limit($n + 1; g)) end"),
			JqFunction.of("any", args("generator", "condition"), "[label $out | foreach generator as $i (false; if . then break $out elif $i | condition then true else . end; if . then . else empty end)] | length == 1"),
			JqFunction.of("any", args("condition"), "any(.[]; condition)"),
			JqFunction.of("any", args(), "any(.)"),
			JqFunction.of("all", args("generator", "condition"), "[label $out | foreach generator as $i (true; if .|not then break $out elif $i | condition then . else false end; if .|not then . else empty end)] | length == 0"),
			JqFunction.of("all", args("condition"), "all(.[]; condition)"),
			JqFunction.of("all", args(), "all(.)"),
			JqFunction.of("flatten", args(), "_flatten(-1)"),
			JqFunction.of("flatten", args("$x"), "if $x < 0 then error(\"flatten depth must not be negative\") else _flatten($x) end"),
			JqFunction.of("_flatten", args("$x"), "reduce .[] as $i ([]; if $i | type == \"array\" and $x != 0 then . + ($i | _flatten($x-1)) else . + [$i] end)"),
			JqFunction.of("ascii_downcase", args(), "explode | map( if 65 <= . and . <= 90 then . + 32  else . end) | implode"),
			JqFunction.of("ascii_upcase", args(), "explode | map( if 97 <= . and . <= 122 then . - 32  else . end) | implode"),
			JqFunction.of("until", args("cond", "next"), "def _until: if cond then . else (next|_until) end; _until"),
			JqFunction.of("while", args("cond", "update"), "def _while: if cond then ., (update | _while) else empty end; _while"),
			JqFunction.of("leaf_paths", args(), "paths(scalars)"),
			JqFunction.of("walk", args("f"), ". as $in | if type == \"object\" then reduce keys[] as $key ( {}; . + { ($key):  ($in[$key] | walk(f)) } ) | f elif type == \"array\" then map( walk(f) ) | f else f end", VersionRange.valueOf("[1.6, )")),
			JqFunction.of("in", args("xs"), ". as $x | xs | has($x)"),
			JqFunction.of("inside", args("xs"), ". as $x | xs | contains($x)"),
			JqFunction.of("combinations", args(), "if length == 0 then [] else .[0][] as $x | (.[1:] | combinations) as $y | [$x] + $y end"),
			JqFunction.of("combinations", args("n"), ". as $dot | [range(n) | $dot] | combinations"),
			JqFunction.of("map_values", args("f"), ".[] |= f"),
			JqFunction.of("_modify", args("paths", "update"), "reduce path(paths) as $p (.; label $out | (setpath($p; getpath($p) | update) | ., break $out), delpaths([$p]))", VersionRange.valueOf("[1.6, )")),
			JqFunction.of("_modify", args("paths", "update"), "reduce path(paths) as $p (.; setpath($p; getpath($p) | update))", VersionRange.valueOf("[, 1.6)")),
			JqFunction.of("pick", args("pathexps"), ". as $in | reduce path(pathexps) as $a (null; setpath($a; $in|getpath($a)) )", VersionRange.valueOf("[1.7, )")),
			JqFunction.of("ltrimstr", args("$left"), "if startswith($left) then .[$left | length:] else . end", VersionRange.valueOf("[1.8.0, )")),
			JqFunction.of("rtrimstr", args("$right"), "if endswith($right) then .[:$right | -length] else . end", VersionRange.valueOf("[1.8.0, 1.8.2)")),
			JqFunction.of("rtrimstr", args("$right"), "if endswith($right) then .[:length - ($right | length)] else . end", VersionRange.valueOf("[1.8.2, )")),
			JqFunction.of("trimstr", args("$val"), "ltrimstr($val) | rtrimstr($val)", VersionRange.valueOf("[1.8.0, )"))));

	private static List<FunctionParameter> args(String... args) {
		return Arrays.stream(args).map(FunctionParameter::valueOf).collect(Collectors.toList());
	}

	@Override
	public List<JqFunction> getJqFunctions() {
		return FUNCTIONS;
	}
}
