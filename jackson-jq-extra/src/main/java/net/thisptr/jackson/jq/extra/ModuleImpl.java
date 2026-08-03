package net.thisptr.jackson.jq.extra;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;
import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.extra.functions.HostnameFunction;
import net.thisptr.jackson.jq.extra.functions.RandomFunction;
import net.thisptr.jackson.jq.extra.functions.StrFTimeFunction;
import net.thisptr.jackson.jq.extra.functions.StrPTimeFunction;
import net.thisptr.jackson.jq.extra.functions.TimestampFunction;
import net.thisptr.jackson.jq.extra.functions.UriDecodeFunction;
import net.thisptr.jackson.jq.extra.functions.UriParseFunction;
import net.thisptr.jackson.jq.extra.functions.Uuid35Function;
import net.thisptr.jackson.jq.extra.functions.Uuid4Function;
import net.thisptr.jackson.jq.module.BuiltinModule;
import net.thisptr.jackson.jq.module.Module;

@AutoService(Module.class)
@BuiltinModule(path = "jackson-jq/extras")
public class ModuleImpl implements Module {
	private final Map<String, Function> functions = new HashMap<>();

	public ModuleImpl() {
		addFunction(new HostnameFunction());
		addFunction(new RandomFunction());
		addFunction(new StrFTimeFunction());
		addFunction(new StrPTimeFunction());
		addFunction(new TimestampFunction());
		addFunction(new UriDecodeFunction());
		addFunction(new UriParseFunction());
		addFunction(new Uuid4Function());
		addFunction("uuid3/1", new Uuid35Function(3));
		addFunction("uuid5/1", new Uuid35Function(5));
	}

	private void addFunction(final Function f) {
		final BuiltinFunction annotation = f.getClass().getAnnotation(BuiltinFunction.class);
		for (final String fname : annotation.value())
			addFunction(fname, f);
	}

	private void addFunction(final String fnameAndNarg, final Function f) {
		functions.put(fnameAndNarg, f);
	}

	@Override
	public Function getFunction(final String fname, final int nargs) {
		return functions.get(fname + "/" + nargs);
	}

	@Override
	public Map<String, Function> getAllFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
