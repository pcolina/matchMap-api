package com.matchmap.api.account;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
class AccountGraphqlExceptionResolver extends DataFetcherExceptionResolverAdapter {

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    if (!(ex instanceof AccountException accountException)) {
      return null;
    }
    Map<String, Object> extensions = new LinkedHashMap<>();
    extensions.put("code", accountException.code());
    if (accountException.field() != null) {
      extensions.put("field", accountException.field());
    }
    return GraphqlErrorBuilder.newError(env)
        .errorType(ErrorType.BAD_REQUEST)
        .message(accountException.getMessage())
        .extensions(extensions)
        .build();
  }
}
