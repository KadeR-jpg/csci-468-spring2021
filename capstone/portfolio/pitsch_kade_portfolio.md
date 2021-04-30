# CSCI Compilers 468 Spring 2021
## Kade Pitsch And Robert Jenko

## Sections
* ### [Source](#section-one)
* ### [Teamwork](#section-two-teamwork)
* ### [Design Patterns](#section-three-design-pattern)
* ### [Technical Writing](#section-four-technical-writing)
* ### [UML](#section-five-uml)
* ### [Design trade-offs](#section-six-design-trade-offs)
* ### [Software Dev. Cycle](#section-seven-software-development-life-cycle-model)


# Section One: Source
The source is contained in `source.zip` that is in this directory
# Section Two: Teamwork
For this course i was the main contributor for this project.This includes the code for 
tokenization, evaluation and byte-code. My partner {team-member 2} was responsible for 
providing documentation and 3 tests for my code base. This includes providing well structured
and concise documentation for my code. Also my partner {team-member 2} will write 3 tests for
my code that could written for tokenization, evaluation and/or byte-code. These test will be expected 
to be unique and run able with my current code. Team member contributions were 50/50 as we both
provided the same things to each other i would
assume that we spent about two to four hours on each others
projects.  

## Documentation for Team-Member 2 from
## Tokenization
Tokenization is where our recursive descent parsing starts. Starting with the `tokenize()` function,
consuming whitespace that does not matter for catscript.
``` java
    private void tokenize() {
        consumeWhitespace();
        while (!tokenizationEnd()) {
            scanToken();
            consumeWhitespace();
        }
        tokenList.addToken(EOF, "<EOF>", position, position, line, lineOffset);
    }
```
`tokenize()` then calls `scanToken()` which will associate a type wih our token.


``` java
    private void scanToken() {
        if (scanNumber()) {
            return;
        }
        if (scanString()) {
            return;
        }
        if (scanIdentifier()) {
            return;
        }
        scanSyntax();
    }
```
scanning the tokens until an associated type is returned. This is how the recursive descent parsing will work throughout
the rest of the compiler.

Each of these types have associated functions that are further parse the token. For example if we encounter a string we will
first verify that we are actually trying to parse a string. I am just going to pick apart one of these functions so we can 
see what is going on 
``` java
    private boolean scanString() {
        boolean endquote = true;
/*1*/   if (peek() == '"') {
            takeChar();
/*2*/       int start = position;
            while (!tokenizationEnd()) {
                if (peek() == '"') {
/*3*/               String value = src.substring(start, position);
/*4*/               tokenList.addToken(STRING, value, start, position, line, lineOffset);
                    takeChar();
                    return true;
                }
/*5*/           if (peek() == '\\') {
                    takeChar();
                    if (peek() == '"') {
                        takeChar();
                    }
                }

/*6*/           if (tokenizationEnd()) {
                    tokenList.addToken(ERROR, "No closing String", start, position, line, lineOffset);
                    return true;
                }
                takeChar();
            }
/*7*/       tokenList.addToken(ERROR, "No closing String", start, position, line, lineOffset);
            return true;
        }
        return false;
    }
```
`scanString()` returns a boolean after parsing and tokenizing our string. We start at `/*1*/` checking for sure that we are looking
at a string. A Catscript string is java style string so defined by two double quotes enclosing something.
Once we know that we are scanning s string the `"` is consumed and we continue with the tokenization process.
We need to know how the position of where we started s on `/*2*/` we specify where we started reading. Then we start reading the string 
until we reach the end of the string specified by another unescaped `"` character. `/*3*/` is where we actually take the full position 
of the string and set it equal to a Java string. Then on `/*4*/` we tell our Catscript parser what out string token is.
`/*5*/` is what happens if we encounter a comment in Catscript which is then treated as a string. The same process is carried out.
`/*6*/ & /*7*/` are some basic error handlers. If we encounter the tokenization end before we actually close the string with a `"`
There are 2 other tokenization options for if we encounter a number or an Identifier, they follow the same form.
The other one that is super important to the compiler is the `scanSyntax()` function. It is a really big one so i will
just clip some of the more interesting ones and explain.

``` java
else if (matchAndConsume('=')) {
            if (matchAndConsume('=')) {
                tokenList.addToken(EQUAL_EQUAL, "==", start, position, line, lineOffset);
            } else {
                tokenList.addToken(EQUAL, "=", start, position, line, lineOffset);
            }

        }
```
Here we have our equality operator token which triggers when we match an `=` sign which tells us we are doing some equal or equality operation
if we just have a `=` then we are setting something equal to something else. Otherwise read another `=` and we have the equality operator `==` which
is going to return a boolean but that will be handled in our parser.

``` java
 else if (matchAndConsume('/')) {
            if (matchAndConsume('/')) {
                while (peek() != '\n' && !tokenizationEnd()) {
                    takeChar();
                }
            } else {
                tokenList.addToken(SLASH, "/", start, position, line, lineOffset);
            }
        }

```
This is where we are handling the comments in Catscript. If we have a `/` consume it, do we have another one? Consume it and read until we hit 
`tokenizationEnd()` or a `\n` character. If only one slash then just add that token.

I think this covers the fundamentals of what is happening in the tokenization step. There are some important helper functions like 
`matchAndConsume()` but we did not write those so we will just leave them at that, helper functions.

## Parsing
Parsing is huge compared to Tokenization so i will go over the functions that i *Team Member 2* think 
* Are the most important
* Encapsulate the fundamentals of parsing in catscript.

## Byte-Code
Byte-Code was written by wizards, unreadable to mere mortals. Sorry i do not make the rules ¯\\_(ツ)_/¯

## Documentation From Partner {Team-member 2}

# Section Three: Design Pattern
Design patterns that we used 
# Section Four: Technical Writing
# Section Five: UML
Describe one of the UML diagrams that are provided in the folders.

# Section Six: Design trade-offs
Recursive Descent vs Parser Generators
# Section Seven: Software development life cycle model
Test Driven Development, talk about your experience about itl
