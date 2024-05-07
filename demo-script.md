## Demo flow

See also the [slide-free-intro.md](slide-free-intro.md).

🎵 indicates things which should be sung.

### Enable Dictation

First, [enable Dictation](https://support.apple.com/en-gb/guide/mac-help/mh40584/mac) on your MacBook.

### Creating the first .rock file and compiling it

#### Work out the classpath

We can compile on the command line, but it needs a complex classpath. If the one below doesn't work, use these steps to
figure it out. Use

```shell

mvn dependency:build-classpath -DincludeScope=compile
 ```

to get the classpath of the implementation, then put it on a java classpath with `target/classes` of the Bon Jova
compiler project (use an absolute path).

Make to a scratch directory, say `~/devoxx`. Open it in your IDE, for example `idea devoxx`.
Use vi to make a `hello.rock` file in `~`

Enter the following, or use the Dictation feature by pressing the F5/🎙️key. 

> A microphone overlay will appear while Dictation is active.
> Press the same key again to stop dictation.

```shell
Christine says hello world
Shout Christine
```

This is using the poetic string syntax. Then launch it with

```
🎵 java -cp ~/Code/demos/rockstar/bon-jova/quarkus-bon-jova-rockstar/compiler/target/classes:~/.m2/repository/io/quarkus/gizmo/gizmo/1.7.0/gizmo-1.7.0.jar:~/.m2/repository/org/ow2/asm/asm/9.5/asm-9.5.jar:~/.m2/repository/org/ow2/asm/asm-util/9.5/asm-util-9.5.jar:~/.m2/repository/org/ow2/asm/asm-tree/9.5/asm-tree-9.5.jar:~/.m2/repository/org/ow2/asm/asm-analysis/9.5/asm-analysis-9.5.jar:~/.m2/repository/io/smallrye/jandex/3.1.3/jandex-3.1.3.jar:~/.m2/repository/org/antlr/antlr4-runtime/4.13.0/antlr4-runtime-4.13.0.jar io.quarkiverse.bonjova.compiler.RockFileCompiler ~/hello.rock
```

Point out that a class file has been created, and launch it with java:

```shell
ls -l ~/*class
java -cp . hello
```

Repeat this process for concept-demo 1 through 6. Sing the code 🎵.

For the beach one, ask people to guess the output. Explain the syntax concepts involved in the code.

Obviously, this is a very annoying way of running .rock files. There's no live coding, there's two steps to launch, the
classpath is long ... we need help!

### Quarkus extension

#### Setting up an application

Show http://quarkus.io/extensions and filter for alt-languages to show the extension is in the marketplace.

```sh
quarkus create app
cd code-with-quarkus
idea .
```

### TDD with Quarkus

Add a source directory, so that Quarkus can monitor our rockstar files. Normally, this wouldn't be necessary to do it
manually, but maven does not know about rockstar. In the pom, in the `<build>` section, add

```xml

<sourceDirectory>src/main/rockstar</sourceDirectory>
```

(A `sourcedir` live template can help.)

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
(If live reload doesn't work, check you added the `<sourceDirectory>` in the pom.)

Refactor to `Christine says hello world\nSay Christine`. The test should continue passing.

### Dev UI, and exposing via the web

What if I don't want to do TDD?

Visit http://localhost:8080, it will tell you about the endpoints
Visit http://localhost:8080/rockstar/hello_world and see the output

Change the text to something suitably rock and roll, re-load in the browser, and it will be updated.

You can also open files in the `target/classes` file to see the decompiled code as you edit it.

#### Rock score

Show how the rock score changes when code is swapped from an 'clear' version to an idiomatic one.

### Mandelbrot

Use the `mw` live template to fill in the mandelbrot code in a file called `mandelbrot.rock.` Visit it
at http://localhost:8080/rockstar/mandelbrot to see the calculated Mandelbrot set.