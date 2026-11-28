package com.matchmap.api.geo;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
class GeoExceptionResolver extends DataFetcherExceptionResolverAdapter {

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    if (!(ex instanceof PinException pinException)) {
      return null;
    }
    Map<String, Object> extensions = new LinkedHashMap<>();
    extensions.put("code", pinException.code());
    if (pinException.field() != null) {
      extensions.put("field", pinException.field());
    }
    return GraphqlErrorBuilder.newError(env)
        .errorType(ErrorType.BAD_REQUEST)
        .message(pinException.getMessage())
        .extensions(extensions)
        .build();
  }
}
