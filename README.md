# Bon Jova

An implementation of Rockstar as a JVM language

[Rockstar](https://codewithrockstar.com) is an example of an “esoteric language,” designed to be interesting rather than
intuitive, efficient or especially functional.
Rockstar’s interesting feature is that its programs use the lyrical conventions of eighties rock ballads.
Rockstar has been implemented in [many languages](https://codewithrockstar.com/code), but not as a JVM language. (There
are existing Java implementations, but they rely on interpretation, rather than compilation to bytecode.)
This is clearly (clearly!) a gap that needed fixing.

This implementation uses the [Antlr grammar for Rockstar](https://github.com/ascheja/rockstar-antlr4/tree/master) by
Andreas Scheja as a starting point.

# Using the extension

Create a Quarkus app, and add the extension to your pom; it will then compile `.rock` files from `src/main/rockstar` and
any other `<sourceDirectory>` defined in the `pom.xml`.

# Developing the extension

## Getting started

In the top-level directory, run

`mvn install`

This will build the Rockstar parser, the Quarkus extension, and the sample app.
You can then run the sample app with

```
cd samples
mvn quarkus:dev
```

(To test out your local extension changes, make sure to change the extension version in the app pom to `-SNAPSHOT`.)

### IDE integration

After cloning the repo, you will need to run `mvn compile` in the `bon-jova-rockstar-compiler` folder to generate the
Antlr
Java classes, or your IDE will report compile failures and missing classes. (A top-level `mvn install` will also work.)
The generated classes will appear in `target/generated-sources`, so you may also need to configure your IDE to treat
that folder as a generated sources folder.

## What's implemented

See [the spec](https://github.com/RockstarLang/rockstar/blob/main/spec.md) for details and the definitive list of
capabilites.
Some of these may work but have not been validated with tests.

## General

- [ ] Enforcement of the file extension and encoding
- [X] Comments

## ✓ Variables and constants

- [X] Simple variables
- [X] Common variables
- [X] Proper variables
- [X] Dynamic typing of variables
- [X] Case insensitivity of variable names
- [X] Pronoun variable references
- [X] Number literals
- [X] Poetic number literals
- [X] String literals
- [X] Poetic string literals

## ✓ Types

- [X] Mysterious
- [X] Null equality to 0 and false
- [X] Null aliases `nothing`, `nowhere`, `nobody`, `gone`
- [X] True aliases `right`, `yes`, `ok`
- [X] False aliases `wrong`, `no`, `lies`

## Arrays

- [X] Reading at numerical indexes
- [X] Array initialisation by setting a value at an index
- [X] Non-numeric array keys
- [ ] Use the array index syntax to read (but not write) specific characters from a string

## Queue operations

- [X] Pushing
- [X] Popping
- [X] Special `roll x into y` syntax for removing the first element from an array and assigning it to a variable
- [ ] Special syntax for pushing poetic literals onto a queue

## Splitting and joining strings

- [X] Split a string in Rockstar, use the `cut` mutation (aliases `split` and `shatter`)
- [ ] Split strings in place
- [X] Delimiters
- [X] The `join` support

## ✓ Casting

- [X] Casting strings to doubles
- [X] Casting using bases other than 10
- [X] Casting expressions into a variable
- [X] Casting numbers to strings using UTF-8 conversions

## ✓ Single quotes

- [X] Equivalency to `is'
- [X] Ignoring in other cases

## Number operations

- [X] Incrementing and decrementing
- [X] Arithmetic
  operators
- [X] Aliases for arithmetic operators
- [ ] Compound assignment using `let`
- [X] Rounding
- [X] Rounding with pronouns
- [X] List arithmetic
- [ ] Operator precedence

## ✓ Comparison and logical operations

- [X] Equality tests
- [X] Comparisons to mysterious and null
- [X] Conjunction
- [X] Disjunction
- [X] Joint denial
- [X] Negation

## ✓ Input and output

- [X] Input from stdin
- [X] Output to stdout

## ✓ Flow control

- [X] Conditionals
- [X] Loops
- [X] Blocks, break, and continue
- [X] Functions
- [X] The `with` keyword for parameters

