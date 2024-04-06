package io.quarkiverse.bonjova.rockout;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class RockOut implements QuarkusApplication {
    @Override
    public int run(String... args) throws Exception {
        System.out.println("Hello from Rock Out!");
        System.out.println("Waiting for input...");

        Thread.sleep(2000);

        System.out.println("Exiting Rock Out!");
        return 0;
    }
}
