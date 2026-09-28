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
package net.thisptr.jackson.jq.v2.ext.joni;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;

/**
 * jq's regular-expression functions, backed by Joni.
 * <p>
 * An application that wants them callable by their bare names hands a new instance to
 * {@code EnvironmentBuilder.includeModule(...)}; a query can reach them itself by importing or
 * including {@code "jackson-jq/joni"}. Nothing installs them implicitly.
 */
@ModuleRegistration(path = "jackson-jq/joni")
public final class JoniRegexModule implements JqModule<Object> {
	private static final Module PRIMITIVES = new JoniRegexPrimitives();

	/**
	 * The jq-language surface over the regex primitives.
	 * <p>
	 * Every definition states the signature it answers with a {@code #jackson-jq:type} comment, because
	 * left to its body a definition would instead report the primitive's whole {@code BOOLEAN|[Match]}
	 * union -- or, where the body dispatches on {@code $val|type}, which type checking cannot read,
	 * nothing at all.
	 * <p>
	 * A shape a definition names whole is rendered from {@link Types} rather than spelt out, so that the
	 * signature it states cannot drift from the primitive it calls: printing and parsing a type are
	 * inverses, so the text below is the same shape {@link MatchImplFunction} and
	 * {@link SubImplFunction} publish. {@code scan} is the exception -- it names no shape of theirs, and
	 * a union it assembles itself is clearer written out.
	 */
	private static final String SOURCE = """
			include "impl" {search: "./"};
			#jackson-jq:type (STRING -> STRING; STRING -> STRING) => (STRING -> ${MATCH})
			def match(re; mode): _match_impl(re; mode; false) | .[];
			#jackson-jq:type (STRING -> STRING) => (STRING -> ${MATCH})
			#jackson-jq:type (STRING -> [STRING]) => (STRING -> ${MATCH})
			#jackson-jq:type (STRING -> [STRING, STRING]) => (STRING -> ${MATCH})
			def match($val): ($val|type) as $vt | if $vt == "string" then match($val; "") elif $vt == "array" and ($val | length) > 1 then match($val[0]; $val[1]) elif $vt == "array" and ($val | length) > 0 then match($val[0]; "") else error($vt + " not a string or array") end;
			#jackson-jq:type (STRING -> STRING; STRING -> STRING) => (STRING -> BOOLEAN)
			def test(re; mode): _match_impl(re; mode; true);
			#jackson-jq:type (STRING -> STRING) => (STRING -> BOOLEAN)
			#jackson-jq:type (STRING -> [STRING]) => (STRING -> BOOLEAN)
			#jackson-jq:type (STRING -> [STRING, STRING]) => (STRING -> BOOLEAN)
			def test($val): ($val|type) as $vt | if $vt == "string" then test($val; "") elif $vt == "array" and ($val | length) > 1 then test($val[0]; $val[1]) elif $vt == "array" and ($val | length) > 0 then test($val[0]; "") else error($vt + " not a string or array") end;
			#jackson-jq:type (STRING -> STRING; STRING -> STRING) => (STRING -> ${CAPTURES})
			def capture(re; mods): match(re; mods) | reduce (.captures | .[] | select(.name != null) | {(.name): .string}) as $pair ({}; . + $pair);
			#jackson-jq:type (STRING -> STRING) => (STRING -> ${CAPTURES})
			#jackson-jq:type (STRING -> [STRING]) => (STRING -> ${CAPTURES})
			#jackson-jq:type (STRING -> [STRING, STRING]) => (STRING -> ${CAPTURES})
			def capture($val): ($val|type) as $vt | if $vt == "string" then capture($val; "") elif $vt == "array" and ($val | length) > 1 then capture($val[0]; $val[1]) elif $vt == "array" and ($val | length) > 0 then capture($val[0]; "") else error($vt + " not a string or array") end;
			#jackson-jq:type (STRING -> STRING; STRING -> STRING) => (STRING -> STRING|[*:STRING|NULL])
			def scan(re; flags): match(re; flags + "g") | if (.captures|length > 0) then [.captures | .[] | .string] else .string end;
			#jackson-jq:type (STRING -> STRING) => (STRING -> STRING|[*:STRING|NULL])
			def scan(re): scan(re; "");
			#jackson-jq:type (STRING -> STRING; STRING -> STRING) => (STRING -> STRING)
			def splits($re; flags): def _nwise(a; $n): if a|length <= $n then a else a[0:$n], _nwise(a[$n:]; $n) end; def _nwise($n): _nwise(.; $n); . as $s | [match($re; "g" + flags) | (.offset, .offset + .length)] | [0] + . + [$s|length] | _nwise(2) | $s[.[0]:.[1]];
			#jackson-jq:type (STRING -> STRING) => (STRING -> STRING)
			def splits($re): splits($re; "");
			#jackson-jq:type (STRING -> STRING; STRING -> STRING) => (STRING -> [*:STRING])
			def split($re; flags): [splits($re; flags)];
			#jackson-jq:type (STRING -> STRING; ${CAPTURES} -> STRING) => (STRING -> STRING)
			def sub($re; s): _sub_impl($re; s; "");
			#jackson-jq:type (STRING -> STRING; ${CAPTURES} -> STRING; STRING -> STRING) => (STRING -> STRING)
			def sub($re; s; flags): _sub_impl($re; s; flags);
			#jackson-jq:type (STRING -> STRING; ${CAPTURES} -> STRING; STRING -> STRING) => (STRING -> STRING)
			def gsub($re; s; flags): _sub_impl($re; s; flags + "g");
			#jackson-jq:type (STRING -> STRING; ${CAPTURES} -> STRING) => (STRING -> STRING)
			def gsub($re; s): _sub_impl($re; s; "g");
			"""
			.replace("${MATCH}", Types.MATCH_OBJECT.toString())
			.replace("${CAPTURES}", Types.CAPTURES.toString());

	/**
	 * Required by ServiceLoader.
	 */
	public JoniRegexModule() {
	}

	@Override
	public String getSourceCode() {
		return SOURCE;
	}

	@Override
	public Module loadModule(String importPath, @Nullable String searchPath) {
		if (importPath.equals("impl") && "./".equals(searchPath))
			return PRIMITIVES;
		throw new ModuleNotFoundException(importPath);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof JoniRegexModule;
	}

	@Override
	public int hashCode() {
		return JoniRegexModule.class.hashCode();
	}

	@Override
	public String toString() {
		return "jackson-jq/joni";
	}
}
