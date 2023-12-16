package io.github.marchliu.lexers.nlp;

import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import io.github.marchliu.lexers.Lexer;
import io.github.marchliu.lexers.Token;
import io.github.marchliu.lora.Entity;
import jaskell.parsec.common.Parsec;
import jaskell.parsec.common.Space;
import jaskell.util.Try;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static jaskell.parsec.common.Combinator.many1;
import static jaskell.parsec.common.Txt.joinChars;

public class NlpLexer implements Lexer {


    public List<String> tokens(String source) {
        var tokens = StandardTokenizer.segment(source);
        return tokens.stream()
                .map(t -> t.word)
                .toList();
    }

    @Override
    public List<Entity> shuffle(List<String> tokens) {
        List<Entity> result = new ArrayList<>();
        Random random = new Random();
        int pos = 0;
        while (pos < tokens.size()) {
            int step = random.nextInt(32, 128);
            int idx = Math.min(pos + step, tokens.size());
            List<String> material = tokens.subList(pos, idx);
            int headerSize = Math.min(random.nextInt(4, 16), material.size());

            var input = String.join("", material.subList(0, headerSize));
            var instruction = "";
            var output = String.join("", material);

            var entity = new Entity(instruction, input, output);
            result.add(entity);
            pos = idx;
        }
        return result;
    }

    @Override
    public String getName() {
        return "nlp";
    }

    @Override
    public Parsec<Character, List<Token>> getParser() {
        List<String> result = new ArrayList<>();
        Parsec<Character, String> space = many1(new Space())
                .attempt()
                .bind(joinChars());
        return state -> {
            try {
                StringBuilder buffer = new StringBuilder();
                while (true){
                    if(space.exec(state).isOk()){
                        result.addAll(tokens(buffer.toString()));
                        result.add(" ");
                        buffer = new StringBuilder();
                    } else {
                        buffer.append(state.next());
                    }
                }
            } catch (EOFException e) {
                return result.stream()
                        .map(t->Token.create("text", t))
                        .toList();
            }
        };
    }

    @Override
    public Try<List<String>> tokenize(String source) {
        // 通过这个切分操作保留印欧语系单词之间的空格。
        var array = source.split("\\s");
        List<String> buffer = new ArrayList<>();
        for (var segment : array) {
            buffer.addAll(tokens(segment));
            buffer.add(" ");
        }
        return Try.success(buffer);
    }
}
