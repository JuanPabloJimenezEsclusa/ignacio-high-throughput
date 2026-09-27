package dev.jpje.imperativethroughput.parity;

import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@DisplayName("Build Pin Parity Test")
class BuildPinParityTest {

  private static final String POM_FILE = "pom.xml";
  private static final String GRADLE_FILE = "build.gradle";
  private static final Pattern SPRING_BOOT_PLUGIN = Pattern.compile(
    "id\\s*\\(?\\s*['\"]org\\.springframework\\.boot['\"]\\s*\\)?\\s+version\\s+['\"]([^'\"]+)['\"]");
  private static final Pattern JAVA_TOOLCHAIN = Pattern.compile("JavaLanguageVersion\\.of\\(\\s*(\\d+)\\s*\\)");
  private static final Pattern VERSION_ASSIGNMENT = Pattern.compile("\\bversion\\s*=\\s*['\"]([^'\"]+)['\"]");

  private static Path repositoryRoot;

  @BeforeAll
  static void locateRepositoryRoot() {
    Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null) {
      if (Files.isRegularFile(current.resolve(POM_FILE)) && Files.isRegularFile(current.resolve(GRADLE_FILE))) {
        repositoryRoot = current;
      }
      current = current.getParent();
    }
    if (repositoryRoot == null) {
      throw new IllegalStateException(
        "Could not locate the repository root (a directory containing both %s and %s) starting from %s"
          .formatted(POM_FILE, GRADLE_FILE, System.getProperty("user.dir")));
    }
  }

  @Test
  @DisplayName("Should keep the Spring Boot version in sync between the Maven parent and the Gradle plugin")
  void shouldKeepSpringBootVersionInSync() throws Exception {
    // When
    final Element project = parsePom().getDocumentElement();
    final Element parent = directChild(project, "parent");
    assertThat(parent)
      .as("Expected a <parent> element in %s", POM_FILE)
      .isNotNull();

    final String mavenVersion = directText(parent, "version");
    final String gradleVersion = firstMatch(readString(), SPRING_BOOT_PLUGIN);

    // Then
    assertThat(gradleVersion)
      .as("Spring Boot version drift: %s <parent> spring-boot-starter-parent=%s but %s id 'org.springframework.boot'=%s",
        POM_FILE, mavenVersion, GRADLE_FILE, gradleVersion)
      .isNotNull()
      .isEqualTo(mavenVersion);
  }

  @Test
  @DisplayName("Should keep the Java version in sync between the Maven property and the Gradle toolchain")
  void shouldKeepJavaVersionInSync() throws Exception {
    // When
    final Element project = parsePom().getDocumentElement();
    final Element properties = directChild(project, "properties");
    assertThat(properties)
      .as("Expected a <properties> element in %s", POM_FILE)
      .isNotNull();

    final String mavenVersion = directText(properties, "java.version");
    final String gradleVersion = firstMatch(readString(), JAVA_TOOLCHAIN);

    // Then
    assertThat(gradleVersion)
      .as("Java version drift: %s <java.version>=%s but %s JavaLanguageVersion.of(%s)",
        POM_FILE, mavenVersion, GRADLE_FILE, gradleVersion)
      .isNotNull()
      .isEqualTo(mavenVersion);
  }

  @Test
  @DisplayName("Should keep the project version in sync between Maven and Gradle allprojects")
  void shouldKeepProjectVersionInSync() throws Exception {
    // When
    final Element project = parsePom().getDocumentElement();
    final String mavenVersion = directText(project, "version");
    final String gradleVersion = firstMatch(extractBlock(readString()), VERSION_ASSIGNMENT);

    // Then
    assertThat(gradleVersion)
      .as("Project version drift: %s <version>=%s but %s allprojects version=%s",
        POM_FILE, mavenVersion, GRADLE_FILE, gradleVersion)
      .isNotNull()
      .isEqualTo(mavenVersion);
  }

  private static Document parsePom() throws Exception {
    try (final var input = Files.newInputStream(repositoryRoot.resolve(POM_FILE))) {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(false);
      return factory.newDocumentBuilder().parse(input);
    }
  }

  private static String readString() throws Exception {
    return Files.readString(repositoryRoot.resolve(BuildPinParityTest.GRADLE_FILE));
  }

  private static Element directChild(final Element parent, final String tag) {
    final NodeList children = parent.getChildNodes();
    for (int index = 0; index < children.getLength(); index++) {
      final Node node = children.item(index);
      if (node.getNodeType() == Node.ELEMENT_NODE && tag.equals(node.getNodeName())) {
        return (Element) node;
      }
    }
    return null;
  }

  private static String directText(final Element parent, final String tag) {
    final Element child = directChild(parent, tag);
    return child == null ? null : child.getTextContent().trim();
  }

  private static String firstMatch(final String source, final Pattern pattern) {
    final Matcher matcher = pattern.matcher(source);
    return matcher.find() ? matcher.group(1) : null;
  }

  private static String extractBlock(final String source) {
    final Matcher matcher = Pattern.compile("\\b" + Pattern.quote("allprojects") + "\\s*\\{").matcher(source);
    if (!matcher.find()) {
      return "";
    }
    int depth = 0;
    for (int index = matcher.end() - 1; index < source.length(); index++) {
      final char current = source.charAt(index);
      if (current == '{') {
        depth++;
      } else if (current == '}') {
        depth--;
        if (depth == 0) {
          return source.substring(matcher.end(), index);
        }
      }
    }
    return "";
  }
}
