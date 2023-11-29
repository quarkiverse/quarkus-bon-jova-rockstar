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

## Getting started

After cloning the repo, you will need to run `mvn compile` to generate the Antlr Java classes. These will appear
in `target/generated-sources`, so you may also need to configure your IDE to treat that folder as a generated sources
folder.