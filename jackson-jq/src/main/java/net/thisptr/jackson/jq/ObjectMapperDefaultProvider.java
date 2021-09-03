package net.thisptr.jackson.jq;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.thisptr.jackson.jq.internal.misc.JsonQueryJacksonModule;

/**
 * Default {@link ObjectMapperProvider}
 */
public class ObjectMapperDefaultProvider implements ObjectMapperProvider {

    @Override
    public ObjectMapper get() {
        return ObjectMapperHolder.INSTANCE;
    }

    private static final class ObjectMapperHolder {

        private static final ObjectMapper INSTANCE = new ObjectMapper().registerModule(JsonQueryJacksonModule.getInstance());
    }
}
