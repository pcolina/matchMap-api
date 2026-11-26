package com.matchmap.api.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import graphql.language.AstPrinter;
import graphql.language.FieldDefinition;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ObjectTypeExtensionDefinition;
import graphql.language.TypeDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.execution.GraphQlSource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GraphqlContractIT {

  private static final Path CONTRACT_DIRECTORY = Path.of("contracts/graphql");

  @Autowired GraphQlSource graphQlSource;

  @Test
  void servedSchemaMatchesContract() throws IOException {
    TypeDefinitionRegistry contract = loadContract();
    GraphQLSchema served = graphQlSource.schema();

    for (String name : List.of("Query", "Mutation", "User", "RegisterInput")) {
      assertThat(servedFields(served, name)).as(name).isEqualTo(contractFields(contract, name));
    }
  }

  private TypeDefinitionRegistry loadContract() throws IOException {
    TypeDefinitionRegistry registry = new TypeDefinitionRegistry();
    try (Stream<Path> files = Files.list(CONTRACT_DIRECTORY)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".graphqls")).toList()) {
        registry.merge(new SchemaParser().parse(file.toFile()));
      }
    }
    return registry;
  }

  private Map<String, String> contractFields(TypeDefinitionRegistry contract, String name) {
    Map<String, String> fields = new TreeMap<>();
    TypeDefinition<?> definition = contract.types().get(name);
    if (definition instanceof ObjectTypeDefinition object) {
      addFields(fields, object.getFieldDefinitions());
      for (ObjectTypeExtensionDefinition extension :
          contract.objectTypeExtensions().getOrDefault(name, List.of())) {
        addFields(fields, extension.getFieldDefinitions());
      }
    } else if (definition instanceof InputObjectTypeDefinition input) {
      for (InputValueDefinition field : input.getInputValueDefinitions()) {
        fields.put(field.getName(), AstPrinter.printAst(field.getType()));
      }
    }
    return fields;
  }

  private void addFields(Map<String, String> fields, List<FieldDefinition> definitions) {
    for (FieldDefinition field : definitions) {
      fields.put(field.getName(), AstPrinter.printAst(field.getType()));
    }
  }

  private Map<String, String> servedFields(GraphQLSchema schema, String name) {
    Map<String, String> fields = new TreeMap<>();
    var type = schema.getType(name);
    if (type instanceof GraphQLObjectType object) {
      object
          .getFieldDefinitions()
          .forEach(
              field -> fields.put(field.getName(), GraphQLTypeUtil.simplePrint(field.getType())));
    } else if (type instanceof GraphQLInputObjectType input) {
      input
          .getFieldDefinitions()
          .forEach(
              field -> fields.put(field.getName(), GraphQLTypeUtil.simplePrint(field.getType())));
    }
    return fields;
  }
}
