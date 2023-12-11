package io.github.marchliu.lexers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.marchliu.lexers.c.CLexer;
import io.github.marchliu.lexers.java.JavaLexer;
import io.github.marchliu.lexers.python.java.PythonLexer;
import io.github.marchliu.lora.Entity;
import jaskell.argsparser.ArgParser;
import jaskell.argsparser.Option;
import jaskell.util.Failure;
import jaskell.util.Success;
import jaskell.util.Try;
import jaskell.util.Tuple2;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class LexerRouter {
    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final CLexer cLexer = new CLexer();
    private final JavaLexer javaLexer = new JavaLexer();
    private final PythonLexer pythonLexer = new PythonLexer();
    private final AtomicInteger total = new AtomicInteger();
    private final AtomicInteger counter = new AtomicInteger();

    private void save(String path, String content) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(path)) {
            byte[] strToBytes = content.getBytes();
            outputStream.write(strToBytes);
        }
    }

    public void accumulate(String projectDir) {
        try (Stream<Path> stream = Files.walk(Paths.get(projectDir))) {
            List<Path> paths = stream.toList();
            List<String> filenames = paths.stream()
                    .filter(Files::isRegularFile)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .toList();

            var javaSources = filenames.stream()
                    .filter(p -> p.endsWith(".java"))
                    .toList();
            var cSources = filenames.stream()
                    .filter(p -> p.endsWith(".c"))
                    .toList();
            var pySources = filenames.stream()
                    .filter(p -> p.endsWith(".py"))
                    .toList();
            total.addAndGet(javaSources.size() + cSources.size() + pySources.size());
        } catch (Exception err) {
            err.printStackTrace();
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

            var javaSources = filenames.stream()
                    .filter(p -> p.endsWith(".java"))
                    .toList();
            var cSources = filenames.stream()
                    .filter(p -> p.endsWith(".c"))
                    .toList();
            var pySources = filenames.stream()
                    .filter(p -> p.endsWith(".py"))
                    .toList();

            var subset = new ArrayList<>(javaSources.stream()
                    .map(fn -> {
                        System.out.printf("[%d/%d] ",
                                counter.incrementAndGet(),
                                total.get());
                        return javaLexer.process(fn);
                    }).toList());

            subset.addAll(cSources.stream().map(fn -> {
                System.out.printf("[%d/%d] ",
                        counter.incrementAndGet(),
                        total.get());
                return cLexer.process(fn);
            }).toList());

            subset.addAll(pySources.stream().map(fn -> {
                System.out.printf("[%d/%d] ",
                        counter.incrementAndGet(),
                        total.get());
                return pythonLexer.process(fn);
            }).toList());

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
                .header("Oliva is a assistant program. It just cut source code to lora training data.")
                .formatter("%1$-20s %2$-20s %3$-60s\n")
                .option(source)
                .option(target)
                .footer("Power by Jaskell");

        argParser.parse(args)
                .onFailure(err -> {
                    System.err.println(err.getMessage());
                }).onSuccess(result -> {
                    result.autoHelp();

                    var tuple = Tuple2.liftA(result.option("source"),
                            result.option("target")).get();

                    var src = tuple.item0();
                    var tgt = tuple.item1();
                    List<Entity> entities = new ArrayList<>();
                    for (var s : src) {
                        lexer.accumulate(s);
                    }

                    for (var s : src) {
                        switch (lexer.process(s)) {
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
