package io.github.marchliu.lexers;

import io.github.marchliu.lexers.nlp.NlpLexer;
import io.github.marchliu.lora.Entity;
import jaskell.parsec.common.Parsec;
import jaskell.parsec.common.TxtState;
import jaskell.util.Try;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public interface Lexer {
    NlpLexer nlpLexer = new NlpLexer();

    String getName();

    default Try<List<Entity>> process(String path) {
        System.out.println(STR."\{getName()} processing: \{path}");
        return Try.tryIt(() -> load(path))
                .flatMap(this::tokenize)
                .map(this::shuffle);
    }

    default Try<List<String>> tokenize(String source) {
        return Try.tryIt(() -> new TxtState(source))
                .flatMap(state -> getParser().exec(state))
                .map(parsed -> {
                    List<String> result = new ArrayList<>();
                    parsed.forEach(token -> {
                        if (token.category().equals("text")) {
                            result.addAll(nlpLexer.tokens(token.token()));
                        } else {
                            result.add(token.token());
                        }
                    });
                    return result;
                });
    }

    default String load(String filename) throws IOException {
        var path = Paths.get(filename);
        var bytes = Files.readAllBytes(path);
        return new String(bytes);
    }

    default List<Entity> shuffle(List<String> tokens) {
        List<Entity> result = new ArrayList<>();
        Random random = new Random();
        int pos = 0;
        while (pos < tokens.size()) {
            int step = random.nextInt(32, 128);
            int idx = Math.min(pos + step, tokens.size());
            List<String> material = tokens.subList(pos, idx);
            int headerSize = Math.min(random.nextInt(4, 16), material.size());
            var intput = String.join(" ", material.subList(0, headerSize));
            var instruction = STR."\{getName()}: \{intput}";
            var output = String.join(" ", material);

            var entity = new Entity(instruction, intput, output);
            result.add(entity);
            pos = idx;
        }
        return result;
    }

    Parsec<Character, List<Token>> getParser();
}
