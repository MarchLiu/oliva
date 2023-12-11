package io.github.marchliu.lexers.python.java;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.marchliu.lora.Entity;
import jaskell.parsec.common.Atom;
import jaskell.parsec.common.Combinator;
import jaskell.parsec.common.Parsec;
import jaskell.parsec.common.TxtState;
import jaskell.util.Failure;
import jaskell.util.Success;
import jaskell.util.Try;

import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static jaskell.parsec.common.Atom.one;
import static jaskell.parsec.common.Atom.pack;
import static jaskell.parsec.common.Combinator.*;
import static jaskell.parsec.common.Txt.*;

public class PythonLexer {
    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    TypeReference<List<Entity>> reference = new TypeReference<List<Entity>>() {
    };

    Parsec<Character, String> escapeChar = ch('\\')
            .then(one())
            .bind(c -> pack(STR. "\\\{ c }" ));
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

    Parsec<Character, String> quote = many(chIn("rf")).then(text("\"")).attempt();
    Parsec<Character, String> triQuote = many(chIn("rf")).then(text("\"\"\"")).attempt();

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
        if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '@') {
            return c;
        } else {
            throw state.trap(STR. "unexpect char '\{ c }'" );
        }
    }).bind(joinChars());

    Parsec<Character, String> singleLineComment = text("#").then(state -> {
        var newline = newline().attempt();
        StringBuilder sb = new StringBuilder().append("#");
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

    Parsec<Character, String> tap = state -> {
        var c = state.next();
        if (Character.isLetterOrDigit(c)) {
            return c.toString();
        } else {
            throw state.trap(STR. "current char [\{ c }] isn't a name char" );
        }
    };

    Parsec<Character, String> symbols = Combinator.<Character, Character>many1(state -> {
        var stop = choice(text("\"").attempt(),
                text("#").attempt(),
                space().bind(c -> pack(c.toString())).attempt(),
                tap).ahead();
        var test = stop.exec(state);
        if (test.isOk()) {
            var message = STR. "symbol stop at '\{ test.get() }' (\{ state.status() }) " ;
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

    Parsec<Character, String> tokenParser = choice(decimal().attempt(),
            validName.attempt(),
            symbols.attempt(),
            singleLineComment.attempt(),
            charLiteral.attempt(),
            strParser);
    Parsec<Character, List<String>> parser = sepBy(tokenParser, skipSpaces());

    List<String> tokens(String source) throws Exception {
        var state = new TxtState(source);
        var result = parser.exec(state);
        return result.get();
    }

    String load(String filename) throws IOException {
        var path = Paths.get(filename);
        var bytes = Files.readAllBytes(path);
        return new String(bytes);
    }

    List<Entity> shuffle(List<String> tokens) {
        List<Entity> result = new ArrayList<>();
        Random random = new Random();
        int pos = 0;
        while (pos < tokens.size()) {
            int step = random.nextInt(32, 128);
            int idx = Math.min(pos + step, tokens.size());
            List<String> material = tokens.subList(pos, idx);
            int headerSize = Math.min(random.nextInt(4, 16), material.size());
            var input = String.join(" ", material.subList(0, headerSize));
            var instruction = STR."python: \{input}";
            var output = String.join(" ", material);

            var entity = new Entity(instruction, input, output);
            result.add(entity);
            pos = idx;
        }
        return result;
    }

    public Try<List<Entity>> process(String path) {
        System.out.println(STR."python lexer processing: \{path}");
        return Try.tryIt(() -> {
            var source = load(path);
            var tokens = tokens(source);
            return shuffle(tokens);
        });
    }

    public void saveTrainData(String path, String content) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(path)) {
            byte[] strToBytes = content.getBytes();
            outputStream.write(strToBytes);
        }
    }

}
