package net.thisptr.jackson.jq;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface ObjectMapperProvider {

    ObjectMapper get();
}
