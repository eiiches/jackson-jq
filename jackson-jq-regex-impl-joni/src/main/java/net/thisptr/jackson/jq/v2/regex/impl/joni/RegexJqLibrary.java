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
import java.util.Collections;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.JqLibrary;

@AutoService(JqLibrary.class)
public class RegexJqLibrary implements JqLibrary {
	private static final List<JqFunc> FUNCTIONS = Collections.unmodifiableList(Arrays.asList(
				jq("match", args("re", "mode"), "_match_impl(re; mode; false)|.[]"),
				jq("match", args("$val"), "($val|type) as $vt | if $vt == \"string\" then match($val; null) elif $vt == \"array\" and ($val | length) > 1 then match($val[0]; $val[1]) elif $vt == \"array\" and ($val | length) > 0 then match($val[0]; null) else error( $vt + \" not a string or array\") end"),
				jq("test", args("re", "mode"), "_match_impl(re; mode; true)"),
				jq("test", args("$val"), "($val|type) as $vt | if $vt == \"string\" then test($val; null) elif $vt == \"array\" and ($val | length) > 1 then test($val[0]; $val[1]) elif $vt == \"array\" and ($val | length) > 0 then test($val[0]; null) else error( $vt + \" not a string or array\") end"),
				jq("capture", args("re", "mods"), "match(re; mods) | reduce ( .captures | .[] | select(.name != null) | { (.name) : .string } ) as $pair ({}; . + $pair)"),
				jq("capture", args("$val"), "($val|type) as $vt | if $vt == \"string\" then capture($val; null) elif $vt == \"array\" and ($val | length) > 1 then capture($val[0]; $val[1]) elif $vt == \"array\" and ($val | length) > 0 then capture($val[0]; null) else error( $vt + \" not a string or array\") end"),
				jq("scan", args("re"), "scan(re; \"\")"),
				jq("scan", args("re", "flags"), "match(re; flags + \"g\") | if (.captures|length > 0) then [ .captures | .[] | .string ] else .string end"),
				jq("_nwise", args("a", "$n"), "if a|length <= $n then a else a[0:$n] , _nwise(a[$n:]; $n) end"),
				jq("_nwise", args("$n"), "_nwise(.; $n)"),
				jq("splits", args("$re", "flags"), ". as $s | [ match($re; \"g\" + flags) | (.offset, .offset + .length) ] | [0] + . +[$s|length] | _nwise(2) | $s[.[0]:.[1] ]"),
				jq("splits", args("$re"), "splits($re; null)"),
				jq("split", args("$re", "flags"), "[splits($re; flags)]"),
				jq("sub", args("$re", "s"), "_sub_impl($re; s; \"\")"),
				jq("sub", args("$re", "s", "flags"), "_sub_impl($re; s; flags)"),
				jq("gsub", args("$re", "s", "flags"), "_sub_impl($re; s; flags + \"g\")"),
				jq("gsub", args("$re", "s"), "_sub_impl($re; s; \"g\")")));

	private static JqFunc jq(final String name, final List<String> args, final String body) {
		return new JqFunc(name, args, body, null);
	}

	private static List<String> args(final String... args) {
		return Arrays.asList(args);
	}

	@Override
	public List<JqFunc> getFunctions() {
		return FUNCTIONS;
	}
}
