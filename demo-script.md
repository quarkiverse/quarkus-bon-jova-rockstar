## High Level Demo flow

## Plain Java

Simple static void main

## Quarkus

Let’s run it on Quarkus

- extra steps to invoke .rock programs

### Unit tests

To see the application running on Quarkus, there needs to be a driver, like a unit test.

       - then, as a bonus, live reload works
      - then, as a bonus, can have other dev ui stuff like rockometer

      Idea: Paste in real songs, see how far they go - then, if it fails, comment out the line with parentheses (which will work, because live reload)

### Web acccess

But we want to access it by the web!

- extra parser stuff to process a SOMETHING and turn it into @Get
- only do @Get for easy life

# Draft Flow with Details

## Slide-free introduction

### Prep

Browser tabs to open:
https://levelup.gitconnected.com/top-6-traits-of-rockstar-developers-f0fc19cde3ca
https://uk.indeed.com/jobs?q=Rockstar+Developer&l=London&from=mobRdr&utm_source=%2Fm%2F&utm_medium=redir&utm_campaign=dt&vjk=bb419dba20b4b653
https://web.archive.org/web/20180703102005/https://twitter.com/paulstovell/status/1013960369465782273
https://codewithrockstar.com/
https://www.google.com/search?client=firefox-b-e&sca_esv=598617038&q=hair+metal&tbm=isch&source=lnms&sa=X&ved=2ahUKEwjZjPXS9d-DAxWmSEEAHfyhB4IQ0pQJegQICxAB&biw=1147&bih=694&dpr=2.61
https://codewithrockstar.com/docs
https://en.wikipedia.org/wiki/Esoteric_programming_language
https://quarkus.io/worldtour/
https://github.com/holly-cummins/bon-jova-rockstar-implementation
https://stackoverflow.com/questions/77796380/add-custom-file-type-to-quarkusdev-watchlist-for-live-reload/77802283#77802283

#### Introduction

[medium tab]
In our industry, we often use the term ‘rockstar developers’ for these incredibly high-peforming people. It’s a bit of a
problematic idea, because they may not necessarily have the best effect on the team overall, a bit too much ego and too
little collaboration.

[indeed tab]
But despite that, it’s a common phrase in recruiter adverts.

[webarchive tab]
Eventually, someone got annoyed and suggested making a language called rockstar so anyone could be a Rockstar developer.

[rockstar tab]
And then, because this is how geeks work, someone took that idea and implemented it.

“Rockstar is a computer programming language designed for creating programs that are also hair metal power ballads.”

[image search tab]
As a refresher on what hair metal is, it’s this.

[specs tab]
So although it’s totally ridiculous, there’s a whole spec, and it’s interesting and non-trivial. The programs, if
well-written, really do read like rock ballads, and they’re computationally expressive. But not concise.

[esoteric wikipedia tab]
I hadn’t known until I started researching it, but there’s a whole huge genre of these languages. “An esoteric
programming language (sometimes shortened to esolang) is a programming language designed to test the boundaries of
computer programming language design, as a proof of concept, as software art, as a hacking interface to another language
or as a joke”

Scroll down to Brainfuck and Chicken. Rockstar is a lot more readable than these, but maybe just as silly.

[world tour tab]
It’s also a great fit with Quarkus iconography.

[github tab]
So here’s the extension.

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

## Demo

We can compile on the command line

mvn dependency:build-classpath to get the classpath of the implementation, then put it on a java classpath with
target/classes

Use vi to make a `hello.rock` file in `~`

Then launch it with

```
java -cp bon-jova-rockstar-compiler/target/classes:~/.m2/repository/io/quarkus/gizmo/gizmo/1.7.0/gizmo-1.7.0.jar:~
/.m2/repository/org/ow2/asm/asm/9.5/asm-9.5.jar:~/.m2/repository/org/ow2/asm/asm-util/9.5/asm-util-9.5.jar:~
/.m2/repository/org/ow2/asm/asm-tree/9.5/asm-tree-9.5.jar:~
/.m2/repository/org/ow2/asm/asm-analysis/9.5/asm-analysis-9.5.jar:~
/.m2/repository/io/smallrye/jandex/3.1.3/jandex-3.1.3.jar:~
/.m2/repository/org/antlr/antlr4-runtime/4.10.1/antlr4-runtime-4.10.1.jar io.quarkiverse.bonjova.RockFileCompiler ~/hello.rock
```

(alternative: use jbang, less hardcoding of the path)

Then java the demo.class file
Idea the demo..class file to see it decompiled

Obviously, this is a very annoying way of running .rock files. No live coding.

Show app, show extension in pom (for real demo, create it)
Show unit test, change rock file, show it failing

Visit http://localhost:8080, it will tell you about the endpoints
Visit http://localhost:8080/rockstar/hello_world

Change the text to something suitably rock and roll, re-load in the browser, it will be updated

You can also open files in the `target/classes` file to see the decompiled code as you edit it.

Next look at guess.rock, then ask people guess what it is, visit in browser
