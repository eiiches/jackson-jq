package net.thisptr.jackson.jq.extra.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;
import net.thisptr.jackson.jq.*;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.path.Path;

import java.util.Collections;
import java.util.List;

@AutoService(Function.class)
@BuiltinFunction({ "fromdateiso8601/0" })
public class FromDateIso8601Function extends StrPTimeFunction {
    @Override
    public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
        super.apply(
                scope,
                Collections.singletonList(new StringLiteral("%Y-%m-%dT%H:%M:%SZ")),
                in,
                ipath,
                output,
                version
        );
    }
}
