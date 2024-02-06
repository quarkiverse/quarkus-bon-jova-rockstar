package io.quarkiverse.bonjova.quarkus.extension.deployment;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;

public class BonJovaQuarkusExtensionProcessorTest {

    private static final Set<File> outputFiles = Set.of(
            new File("target/classes/hello_world.class"), new File("hello_hanno_hello_holly.rock"));

    static Asset asset = new Asset() {
        @Override
        public InputStream openStream() {
            // getResourceAsStream should work but I can't make it work, so just convert the string to a stream
            return new ByteArrayInputStream("Say \"Hello World\"".getBytes(StandardCharsets.UTF_8));
        }
    };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)

                    .addAsResource(
                            asset,
                            "src/main/rockstar/hello_world.rock"));

    /*
     * Ideally we wouldn't create any rockstar classes in target/classes, but I can't quite figure out how to change the output
     * directory
     * on the app model the app model creates from the Arquillian archive.
     */
    @AfterAll
    public static void removeOutputFiles() throws IOException {
        outputFiles.forEach(File::delete);
    }

    @Test
    void testRockstarEndpoint() {
        RestAssured.when()
                .get("/rockstar/hello_world.rock")
                .then()
                .statusCode(200)
                .and()
                .body(containsString("Hello World"));
    }

    @Test
    void testRockstarEndpointForFileThatDoesNotExist() {
        RestAssured.when()
                .get("/rockstar/nonexistent.rock")
                .then()
                .statusCode(404);
    }

    @Test
    @Disabled("not working in CI")
    void testRockstarProgramWithArguments() {
        RestAssured.when()
                .get("/rockstar/hello_hanno_hello_holly.rock?arg=Hanno&arg=Holly")
                .then()
                .statusCode(200)
                .and()
                .body(containsString("Hello Hanno"))
                .and()
                .body(containsString("Hello Holly"));
    }
}
