package io.camunda.zeebe.zbctl.config;

import io.avaje.jsonb.Jsonb;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public class ReflectConfigGenerator {

  public static final String CONFIG_FILE =
      "clients/zbctl/src/main/resources/META-INF/native-image/io.camunda/zeebe-cli/reflect-config.json";
  private static final String PACKAGE_NAME = "io.camunda.client.protocol.rest";
  private static final Jsonb JSONB = Jsonb.builder().build();

  public static void main(final String... args) throws Exception {

    final var reflections = new Reflections(PACKAGE_NAME, new SubTypesScanner(false));
    final var classes = new HashSet<>(reflections.getSubTypesOf(Object.class));

    final var reflectionClasses =
        classes.stream().map(ReflectConfigGenerator::generateClass).toList();

    final var jsonString = JSONB.toJson(reflectionClasses);
    final Path filePath = Paths.get(CONFIG_FILE);
    Files.writeString(filePath, jsonString);
  }

  private static ClassReflectionConfig generateClass(final Class<?> clazz) {
    return new ClassReflectionConfig(
        clazz.getName(),
        true,
        true,
        true,
        Arrays.stream(clazz.getMethods()).map(ReflectConfigGenerator::generateMethods).toList());
  }

  private static MethodReflectionConfig generateMethods(final Method method) {
    return new MethodReflectionConfig(
        method.getName(), Arrays.stream(method.getParameterTypes()).map(Class::getName).toList());
  }

  public record ClassReflectionConfig(
      String name,
      boolean allDeclaredFields,
      boolean queryAllDeclaredMethods,
      boolean queryAllDeclaredConstructors,
      List<MethodReflectionConfig> methods) {}

  public record MethodReflectionConfig(String name, List<String> parameterTypes) {}
}
