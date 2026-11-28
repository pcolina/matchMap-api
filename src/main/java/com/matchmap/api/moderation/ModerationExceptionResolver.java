package com.matchmap.api.moderation;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
class ModerationExceptionResolver extends DataFetcherExceptionResolverAdapter {

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    if (!(ex instanceof ModerationException moderationException)) {
      return null;
    }
    Map<String, Object> extensions = new LinkedHashMap<>();
    extensions.put("code", moderationException.code());
    if (moderationException.field() != null) {
      extensions.put("field", moderationException.field());
    }
    return GraphqlErrorBuilder.newError(env)
        .errorType(ErrorType.BAD_REQUEST)
        .message(moderationException.getMessage())
        .extensions(extensions)
        .build();
  }
}
