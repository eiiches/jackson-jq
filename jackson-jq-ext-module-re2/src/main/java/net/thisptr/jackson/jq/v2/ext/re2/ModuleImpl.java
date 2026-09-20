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
package net.thisptr.jackson.jq.v2.ext.re2;

import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;

@ModuleRegistration(path = "jackson-jq/re2")
public final class ModuleImpl implements JqModule<Object> {
	private static final String SOURCE = """
			import "jackson-jq/re2/_impl" as re2_impl;
			def match(re; mode): re2_impl::_match_impl(re; mode; false) | .[];
			def match($val): ($val|type) as $vt | if $vt == "string" then match($val; null) elif $vt == "array" and ($val | length) > 1 then match($val[0]; $val[1]) elif $vt == "array" and ($val | length) > 0 then match($val[0]; null) else error($vt + " not a string or array") end;
			def test(re; mode): re2_impl::_match_impl(re; mode; true);
			def test($val): ($val|type) as $vt | if $vt == "string" then test($val; null) elif $vt == "array" and ($val | length) > 1 then test($val[0]; $val[1]) elif $vt == "array" and ($val | length) > 0 then test($val[0]; null) else error($vt + " not a string or array") end;
			def capture(re; mods): match(re; mods) | reduce (.captures | .[] | select(.name != null) | {(.name): .string}) as $pair ({}; . + $pair);
			def capture($val): ($val|type) as $vt | if $vt == "string" then capture($val; null) elif $vt == "array" and ($val | length) > 1 then capture($val[0]; $val[1]) elif $vt == "array" and ($val | length) > 0 then capture($val[0]; null) else error($vt + " not a string or array") end;
			def scan(re; flags): match(re; flags + "g") | if (.captures|length > 0) then [.captures | .[] | .string] else .string end;
			def scan(re): scan(re; "");
			def splits($re; flags): def _nwise(a; $n): if a|length <= $n then a else a[0:$n], _nwise(a[$n:]; $n) end; def _nwise($n): _nwise(.; $n); . as $s | [match($re; "g" + flags) | (.offset, .offset + .length)] | [0] + . + [$s|length] | _nwise(2) | $s[.[0]:.[1]];
			def splits($re): splits($re; null);
			def split($re; flags): [splits($re; flags)];
			def sub($re; s): re2_impl::_sub_impl($re; s; "");
			def sub($re; s; flags): re2_impl::_sub_impl($re; s; flags);
			def gsub($re; s; flags): re2_impl::_sub_impl($re; s; flags + "g");
			def gsub($re; s): re2_impl::_sub_impl($re; s; "g");
			""";

	@Override
	public String getSourceCode() {
		return SOURCE;
	}

	@Override
	public JqModule<Object> relativeImport(String importPath, String searchPath) throws JsonQueryException {
		throw new JsonQueryException("jackson-jq/re2 has no relative modules");
	}

	@Override
	public Object relativeData(String importPath, String searchPath) throws JsonQueryException {
		throw new JsonQueryException("jackson-jq/re2 has no relative data");
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ModuleImpl;
	}

	@Override
	public int hashCode() {
		return ModuleImpl.class.hashCode();
	}

	@Override
	public String toString() {
		return "jackson-jq/re2";
	}
}
