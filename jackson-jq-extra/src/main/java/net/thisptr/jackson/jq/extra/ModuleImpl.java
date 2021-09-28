package net.thisptr.jackson.jq.extra;

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
import net.thisptr.jackson.jq.extra.functions.Uuid4Function;
import net.thisptr.jackson.jq.module.BuiltinModule;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.SimpleModule;

@AutoService(Module.class)
@BuiltinModule(path = "jackson-jq/extras")
public class ModuleImpl extends SimpleModule {

	public ModuleImpl() {
		addFunction(new HostnameFunction());
		addFunction(new RandomFunction());
		addFunction(new StrFTimeFunction());
		addFunction(new StrPTimeFunction());
		addFunction(new TimestampFunction());
		addFunction(new UriDecodeFunction());
		addFunction(new UriParseFunction());
		addFunction(new Uuid4Function());
	}

	private void addFunction(final Function f) {
		final BuiltinFunction annotation = f.getClass().getAnnotation(BuiltinFunction.class);
		for (final String fname : annotation.value())
			addFunction(fname, f);
	}
}
