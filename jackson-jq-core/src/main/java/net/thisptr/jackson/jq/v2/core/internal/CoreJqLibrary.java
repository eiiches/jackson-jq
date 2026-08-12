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
package net.thisptr.jackson.jq.v2.core.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.JqLibrary;

@AutoService(JqLibrary.class)
public class CoreJqLibrary implements JqLibrary {
	private static final List<JqFunc> FUNCTIONS = Collections.unmodifiableList(Arrays.asList(
				jq("@text", "tostring"),
				jq("@json", "tojson"),
				jq("paths", "paths(. != null)"),
				jq("arrays", "select(type == \"array\")"),
				jq("booleans", "select(type == \"boolean\")"),
				jq("del", args("f"), "delpaths([path(f)])"),
				jq("nulls", "select(type == \"null\")"),
				jq("objects", "select(type == \"object\")"),
				jq("numbers", "select(type == \"number\")"),
				jq("strings", "select(type == \"string\")"),
				jq("finites", "select(isfinite)"),
				jq("normals", "select(isnormal)"),
				jq("values", "booleans, numbers, strings, arrays, objects"),
				jq("iterables", "arrays, objects"),
				jq("scalars", "nulls, booleans, numbers, strings"),
				jq("isfinite", "type == \"number\" and (isinfinite | not)"),
				jq("add", "reduce .[] as $item (null; . + $item)"),
				jq("min", "min_by(.)"),
				jq("max", "max_by(.)"),
				jq("sort", "sort_by(.)"),
				jq("unique", "group_by(.) | map(.[0])"),
				jq("unique_by", args("f"), "group_by(f) | map(.[0])"),
				jq("with_entries", args("f"), "to_entries | map(f) | from_entries"),
				jq("select", args("pred"), "if pred then . else empty end"),
				jq("map", args("f"), "[.[] | f]"),
				jq("recurse", args("f"), "def r: ., (f | select(. != null) | r); r"),
				jq("recurse", args("f", "cond"), "def r: ., (f | select(cond) | r); r"),
				jq("recurse", "recurse(.[]?)"),
				jq("recurse_down", "recurse"),
				jq("last", ".[-1]"),
				jq("last", args("stream"), "reduce stream as $i (null; $i)"),
				jq("first", ".[0]"),
				jq("first", args("g"), "label $out | foreach g as $item ([false, null]; if .[0]==true then break $out else [true, $item] end; .[1])", "[, 1.6)"),
				jq("first", args("g"), "label $out | g | ., break $out", "[1.6, )"),
				jq("nth", args("n"), "n as $n | .[$n]"),
				jq("transpose", "if . == [] then [] else . as $in | (map(length) | max) as $max | length as $length | reduce range(0; $max) as $j ([]; . + [reduce range(0; $length) as $i ([]; . + [$in[$i][$j]])]) end"),
				jq("limit", args("$n", "exp"), "if $n < 0 then exp else label $out | foreach exp as $item ([$n, null]; if .[0] < 1 then break $out else [.[0] -1, $item] end; .[1]) end"),
				jq("nth", args("n", "g"), "n as $n | if $n < 0 then error(\"nth doesn't support negative indices\") else last(limit($n + 1; g)) end"),
				jq("any", args("generator", "condition"), "[label $out | foreach generator as $i (false; if . then break $out elif $i | condition then true else . end; if . then . else empty end)] | length == 1"),
				jq("any", args("condition"), "any(.[]; condition)"),
				jq("any", "any(.)"),
				jq("all", args("generator", "condition"), "[label $out | foreach generator as $i (true; if .|not then break $out elif $i | condition then . else false end; if .|not then . else empty end)] | length == 0"),
				jq("all", args("condition"), "all(.[]; condition)"),
				jq("all", "all(.)"),
				jq("flatten", "_flatten(-1)"),
				jq("flatten", args("$x"), "if $x < 0 then error(\"flatten depth must not be negative\") else _flatten($x) end"),
				jq("_flatten", args("$x"), "reduce .[] as $i ([]; if $i | type == \"array\" and $x != 0 then . + ($i | _flatten($x-1)) else . + [$i] end)"),
				jq("ascii_downcase", "explode | map( if 65 <= . and . <= 90 then . + 32  else . end) | implode"),
				jq("ascii_upcase", "explode | map( if 97 <= . and . <= 122 then . - 32  else . end) | implode"),
				jq("until", args("cond", "next"), "def _until: if cond then . else (next|_until) end; _until"),
				jq("while", args("cond", "update"), "def _while: if cond then ., (update | _while) else empty end; _while"),
				jq("leaf_paths", "paths(scalars)"),
				jq("walk", args("f"), ". as $in | if type == \"object\" then reduce keys[] as $key ( {}; . + { ($key):  ($in[$key] | walk(f)) } ) | f elif type == \"array\" then map( walk(f) ) | f else f end", "[1.6, )"),
				jq("in", args("xs"), ". as $x | xs | has($x)"),
				jq("inside", args("xs"), ". as $x | xs | contains($x)"),
				jq("combinations", "if length == 0 then [] else .[0][] as $x | (.[1:] | combinations) as $y | [$x] + $y end"),
				jq("combinations", args("n"), ". as $dot | [range(n) | $dot] | combinations"),
				jq("map_values", args("f"), ".[] |= f"),
				jq("_modify", args("paths", "update"), "reduce path(paths) as $p (.; label $out | (setpath($p; getpath($p) | update) | ., break $out), delpaths([$p]))", "[1.6, )"),
				jq("_modify", args("paths", "update"), "reduce path(paths) as $p (.; setpath($p; getpath($p) | update))", "[, 1.6)"),
				jq("pick", args("pathexps"), ". as $in | reduce path(pathexps) as $a (null; setpath($a; $in|getpath($a)) )", "[1.7, )")));

	private static JqFunc jq(final String name, final String body) {
		return new JqFunc(name, Collections.emptyList(), body, null);
	}

	private static JqFunc jq(final String name, final List<String> args, final String body) {
		return new JqFunc(name, args, body, null);
	}

	private static JqFunc jq(final String name, final List<String> args, final String body, final String version) {
		return new JqFunc(name, args, body, version);
	}

	private static List<String> args(final String... args) {
		return Arrays.asList(args);
	}

	@Override
	public List<JqFunc> getFunctions() {
		return FUNCTIONS;
	}
}
