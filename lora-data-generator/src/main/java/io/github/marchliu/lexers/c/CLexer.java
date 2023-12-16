package io.github.marchliu.lexers.c;

import io.github.marchliu.lexers.Lexer;
import io.github.marchliu.lexers.Token;
import jaskell.parsec.common.Atom;
import jaskell.parsec.common.Combinator;
import jaskell.parsec.common.Parsec;
import jaskell.util.Failure;
import jaskell.util.Success;

import java.io.EOFException;
import java.util.List;

import static jaskell.parsec.common.Atom.one;
import static jaskell.parsec.common.Atom.pack;
import static jaskell.parsec.common.Combinator.*;
import static jaskell.parsec.common.Txt.*;

public class CLexer implements Lexer {

    Parsec<Character, String> escapeChar = ch('\\')
            .then(one())
            .bind(c -> pack(STR."\\\{c}"));
    Parsec<Character, String> oneChar = Atom.<Character>one().bind(c -> pack(c.toString()));
    Parsec<Character, String> charParser = choice(escapeChar.attempt(), oneChar);
    StringBuilder sb = new StringBuilder("[\n");

    Parsec<Character, String> strParserBy(String stop) {
        var tap = attempt(text(stop));
        return state -> {
            StringBuilder sb = new StringBuilder().append(stop);
            while (true) {
                var test = tap.exec(state);
                if (test.isOk()) {
                    sb.append(test.get());
                    return sb.toString();
                } else {
                    switch (charParser.exec(state)) {
                        case Success(var segment):
                            sb.append(segment);
                            break;
                        case Failure(var err):
                            throw err;
                    }
                }
            }
        };
    }

    Parsec<Character, String> quote = text("\"").attempt();
    Parsec<Character, String> triQuote = text("\"\"\"").attempt();

    Parsec<Character, String> strParser = state -> {
        if (triQuote.exec(state).isOk()) {
            var parser = strParserBy("\"\"\"");
            return parser.exec(state).get();
        } else if (quote.exec(state).isOk()) {
            var parser = strParserBy("\"");
            return parser.exec(state).get();
        }
        throw state.trap("not string literal");
    };

    Parsec<Character, String> validName = Combinator.<Character, Character>many1(state -> {
        var c = state.next();
        if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '#') {
            return c;
        } else {
            throw state.trap(STR."unexpect char '\{c}'");
        }
    }).bind(joinChars());

    Parsec<Character, String> singleLineComment = text("//").then(state -> {
        var newline = newline().attempt();
        StringBuilder sb = new StringBuilder().append("//");
        while (true) {
            switch (newline.exec(state)) {
                case Success(var nl): {
                    sb.append(nl);
                    return sb.toString();
                }
                case Failure(var error): {
                    if (error instanceof EOFException) {
                        return sb.toString();
                    } else {
                        var c = state.next();
                        sb.append(c);
                    }
                }
            }
        }
    });

    Parsec<Character, String> multiLineComment = text("/*").then(state -> {
        var stop = text("*/").attempt();
        StringBuilder sb = new StringBuilder().append("/*");
        while (true) {
            switch (stop.exec(state)) {
                case Success(var stp): {
                    sb.append(stp);
                    return sb.toString();
                }
                case Failure(var error): {
                    if (error instanceof EOFException) {
                        return sb.toString();
                    } else {
                        var c = state.next();
                        sb.append(c);
                    }
                }
            }
        }
    });

    Parsec<Character, String> tap = state -> {
        var c = state.next();
        if (Character.isLetterOrDigit(c)) {
            return c.toString();
        } else {
            throw state.trap(STR."current char [\{c}] isn't a name char");
        }
    };

    Parsec<Character, String> symbols = Combinator.<Character, Character>many1(state -> {
        var stop = choice(text("\"").attempt(),
                text("//").attempt(),
                text("/*").attempt(),
                space().bind(c -> pack(c.toString())).attempt(),
                tap).ahead();
        var test = stop.exec(state);
        if (test.isOk()) {
            var message = STR."symbol stop at '\{test.get()}' (\{state.status()}) ";
            throw state.trap(message);
        } else {
            return state.next();
        }
    }).bind(joinChars());

    Parsec<Character, String> charLiteral =
            between(ch('\''), ch('\''),
                    choice(ch('\\')
                                    .then(one())
                                    .bind(value -> pack(String.format("\\%c", value))),
                            chNone("\\").bind(value -> pack(value.toString()))));

    Parsec<Character, Token> tokenParser = choice(decimal().bind(Token::word).attempt(),
            validName.bind(Token::word).attempt(),
            symbols.bind(Token::symbol).attempt(),
            singleLineComment.bind(Token::text).attempt(),
            multiLineComment.bind(Token::text).attempt(),
            charLiteral.bind(Token::literal).attempt(),
            strParser.bind(Token::literal));

    Parsec<Character, List<Token>> parser = sepBy(tokenParser, skipSpaces());

    @Override
    public Parsec<Character, List<Token>> getParser() {
        return parser;
    }

    @Override
    public String getName() {
        return "c lexer";
    }
}
