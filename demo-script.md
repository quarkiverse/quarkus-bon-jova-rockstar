## Demo flow

See also the [slide-free-intro.md](slide-free-intro.md).

### Important elements of rockstar syntax

- Some elements just map in a fairly obvious way, eg
    - ‘shout’ or ‘say’ for println
    - ‘Take it to the top’ for continue
- Variables are dynamically typed, don’t need to be declared before first use, initialised with ‘is’, can have several
  words in the same name
- Literal declaration is where it gets head-melty
    - Poetic string literals don’t need quotes if declared with ‘says’
    - Poetic number literals each word represents a digit, the number is the letter count, module 10. Full stops are
      decimals.

Lots of rockstar declarations out there, including an interpreter in Java, but none that compiled to byte code.

### Creating the first .rock file and compiling it

We can compile on the command line

mvn dependency:build-classpath to get the classpath of the implementation, then put it on a java classpath with
target/classes

Use vi to make a `hello.rock` file in `~`

Then launch it with

```
java -cp ~/Code/demos/rockstar/bon-jova/quarkus-bon-jova-rockstar/compiler/target/classes:~/.m2/repository/io/quarkus/gizmo/gizmo/1.7.0/gizmo-1.7.0.jar:~/.m2/repository/org/ow2/asm/asm/9.5/asm-9.5.jar:~/.m2/repository/org/ow2/asm/asm-util/9.5/asm-util-9.5.jar:~/.m2/repository/org/ow2/asm/asm-tree/9.5/asm-tree-9.5.jar:~/.m2/repository/org/ow2/asm/asm-analysis/9.5/asm-analysis-9.5.jar:~/.m2/repository/io/smallrye/jandex/3.1.3/jandex-3.1.3.jar:~/.m2/repository/org/antlr/antlr4-runtime/4.13.0/antlr4-runtime-4.13.0.jar io.quarkiverse.bonjova.compiler.RockFileCompiler ~/hello.rock
```

Then see a class file has been created, and launch it with java:

```shell
ls -l ~/*class
java -cp ~ hello
```

Repeat this process for concept-demo 1 through 6.

For the beach one, ask people to guess the output.

You can idea the demo..class file to see it decompiled

Obviously, this is a very annoying way of running .rock files. No live coding.

# Quarkus extension

## Setting up an application

Show http://quarkus.io/extensions and filter for alt-languages to show the extension is in the marketplace.

```sh
quarkus create app
cd code-with-quarkus
idea .
```

### TDD with Quarkus

In the idea terminal, bring up a terminal and run

```sh
quarkus ext add bon-jova
quarkus dev
```

Type 'r' to run the tests. In IDEA, make a test called `HelloWorld.java`. Use the `rocktest-enter` template to create
the test.
The test should fail.

Create a directory `src/main/rockstar` and then create a file called hello.rock.

Add in the hello world, `Say "hello world"`, the test should pass.
Refactor to `Christine says hello world\nSay Christine`. The test should continue passing.

### Dev UI, and exposing via the web

What if I don't want to do TDD?

Visit http://localhost:8080, it will tell you about the endpoints
Visit http://localhost:8080/rockstar/hello_world and see the output

Change the text to something suitably rock and roll, re-load in the browser, it will be updated

You can also open files in the `target/classes` file to see the decompiled code as you edit it.

Next look at guess.rock, then ask people guess what it is, visit in browser

### Mandelbrot