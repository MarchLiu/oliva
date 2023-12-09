package io.github.marchliu.lexers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.marchliu.lexers.c.CLexer;
import io.github.marchliu.lexers.java.JavaLexer;
import io.github.marchliu.lora.Entity;
import jaskell.argsparser.ArgParser;
import jaskell.argsparser.Option;
import jaskell.argsparser.Parameter;
import jaskell.parsec.common.Atom;
import jaskell.parsec.common.Combinator;
import jaskell.parsec.common.Parsec;
import jaskell.parsec.common.TxtState;
import jaskell.util.Failure;
import jaskell.util.Success;
import jaskell.util.Try;
import jaskell.util.Tuple2;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static jaskell.parsec.common.Atom.one;
import static jaskell.parsec.common.Atom.pack;
import static jaskell.parsec.common.Combinator.*;
import static jaskell.parsec.common.Txt.*;

public class LexerRouter {
    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    TypeReference<List<Entity>> reference = new TypeReference<List<Entity>>() {
    };
    private final CLexer cLexer = new CLexer();
    private final JavaLexer javaLexer = new JavaLexer();

    private void save(String path, String content) throws IOException {
        try(FileOutputStream outputStream = new FileOutputStream(path)) {
            byte[] strToBytes = content.getBytes();
            outputStream.write(strToBytes);
        }
    }

    public Try<List<Entity>> process(String projectDir) {
        List<Entity> entities = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(Paths.get(projectDir))) {
            List<Path> paths = stream.toList();
            List<String> filenames = paths.stream()
                    .filter(Files::isRegularFile)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .toList();

            var subset = new ArrayList<>(filenames.stream()
                    .filter(p -> p.endsWith(".java"))
                    .map(javaLexer::process)
                    .toList());

            subset.addAll(filenames.stream()
                    .filter(p -> p.endsWith(".c"))
                    .map(cLexer::process)
                    .toList());

            subset.stream().filter(Try::isOk).forEach(items -> {
                items.foreach(entities::addAll);
            });
            errors.addAll(subset.stream()
                    .filter(Try::isErr)
                    .map(Try::error)
                    .toList());

            for (var error : errors) {
                error.printStackTrace();
            }
            return Try.success(entities);
        } catch (Exception err) {
            return Try.failure(err);
        }
    }

    public static void main(String[] args) {
        var lexer = new LexerRouter();

        var source = Option.create("source")
                .help("source project directory")
                .required(true);
        var target = Option.create("target")
                .help("where save lora train dataset")
                .required(true);

        var argParser = ArgParser.create()
                .option(source)
                .option(target);

        argParser.parse(args)
                .onFailure(err -> {
                    System.err.println(err.getMessage());
                }).flatMap(result ->
                        Tuple2.liftA(result.option("source"),
                                result.option("target")))
                .onSuccess(tuple -> {
                    var src = tuple.item0();
                    var tgt = tuple.item1();
                    List<Entity> entities = new ArrayList<>();
                    List<Exception> errors = new ArrayList<>();
                    for(var s: src){
                        switch (lexer.process(s)){
                            case Success(var ents): {
                                entities.addAll(ents);
                                break;
                            }
                            case Failure(var error):
                                error.printStackTrace();
                        }
                    }
                    var content = lexer.mapper.writeValueAsString(entities);
                    lexer.save(tgt.first(), content);
                }).onFailure(err -> {
                    System.err.println(err.getMessage());
                    err.printStackTrace();
                });
    }
}
