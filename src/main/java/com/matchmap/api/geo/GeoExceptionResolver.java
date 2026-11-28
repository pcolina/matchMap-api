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
    if (!(ex instanceof GeoException geoException)) {
      return null;
    }
    Map<String, Object> extensions = new LinkedHashMap<>();
    extensions.put("code", geoException.code());
    if (geoException.field() != null) {
      extensions.put("field", geoException.field());
    }
    return GraphqlErrorBuilder.newError(env)
        .errorType(ErrorType.BAD_REQUEST)
        .message(geoException.getMessage())
        .extensions(extensions)
        .build();
  }
}
