package org.example.bon.jova.quarkus.extension.deployment;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.Matchers.containsString;

public class BonJovaQuarkusExtensionTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Test
    @Disabled("until generation of rockstar endpoints is available")
    public void testRockstarEndpoint() {
        RestAssured.when().get("/rockstar/hello-world.rock")
                .then().statusCode(200)
                .and().body(containsString("Hello World!"));
    }
}
