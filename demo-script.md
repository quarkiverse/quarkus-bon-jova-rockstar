## Demo flow

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