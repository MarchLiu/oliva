package io.github.marchliu.lexers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.marchliu.lexers.c.CLexer;
import io.github.marchliu.lexers.java.JavaLexer;
import io.github.marchliu.lexers.nlp.NlpLexer;
import io.github.marchliu.lexers.python.PythonLexer;
import io.github.marchliu.lexers.scala.java.ScalaLexer;
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
    private final ScalaLexer scalaLexer = new ScalaLexer();
    private final PythonLexer pythonLexer = new PythonLexer();
    private final NlpLexer nlpLexer = new NlpLexer();
    private final AtomicInteger total = new AtomicInteger();
    private final AtomicInteger counter = new AtomicInteger();
    private final List<String> javaSources = new ArrayList<>();
    private final List<String> cSources = new ArrayList<>();
    private final List<String> scalaSources = new ArrayList<>();
    private final List<String> pySources = new ArrayList<>();
    private final List<String> textSources = new ArrayList<>();

    private void save(String path, String content) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(path)) {
            byte[] strToBytes = content.getBytes();
            outputStream.write(strToBytes);
        }
    }

    public void prepare(String projectDir) {
        try (Stream<Path> stream = Files.walk(Paths.get(projectDir))) {
            List<Path> paths = stream.toList();
            List<String> filenames = paths.stream()
                    .filter(Files::isRegularFile)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .toList();

            cSources.addAll(filenames.stream()
                    .filter(p -> p.endsWith(".c"))
                    .toList());
            javaSources.addAll(filenames.stream()
                    .filter(p -> p.endsWith(".java"))
                    .toList());
            scalaSources.addAll(filenames.stream()
                    .filter(p -> p.endsWith(".scala") || p.endsWith(".sbt"))
                    .toList());
            textSources.addAll(filenames.stream()
                    .filter(p -> p.endsWith(".txt"))
                    .toList());
            pySources.addAll(filenames.stream()
                    .filter(p -> p.endsWith(".py"))
                    .toList());
            total.set(cSources.size()
                    + javaSources.size()
                    + scalaSources.size()
                    + textSources.size()
                    + pySources.size());
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public Try<List<Entity>> process() {
        List<Entity> entities = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        try {
            List<Try<List<Entity>>> subset = new ArrayList<>();

            subset.addAll(cSources.stream().map(fn -> {
                System.out.printf("[%d/%d] ",
                        counter.incrementAndGet(),
                        total.get());
                return cLexer.process(fn);
            }).toList());

            subset.addAll(javaSources.stream()
                    .map(fn -> {
                        System.out.printf("[%d/%d] ",
                                counter.incrementAndGet(),
                                total.get());
                        return javaLexer.process(fn);
                    }).toList());

            subset.addAll(scalaSources.stream()
                    .map(fn -> {
                        System.out.printf("[%d/%d] ",
                                counter.incrementAndGet(),
                                total.get());
                        return scalaLexer.process(fn);
                    }).toList());

            subset.addAll(textSources.stream().map(fn -> {
                System.out.printf("[%d/%d] ",
                        counter.incrementAndGet(),
                        total.get());
                return nlpLexer.process(fn);
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
                        lexer.prepare(s);
                    }

                    switch (lexer.process()) {
                        case Success(var ents): {
                            entities.addAll(ents);
                            break;
                        }
                        case Failure(var error):
                            error.printStackTrace();
                    }
                    var content = lexer.mapper.writeValueAsString(entities);
                    lexer.save(tgt.first(), content);
                }).onFailure(err -> {
                    System.err.println(err.getMessage());
                    err.printStackTrace();
                });
    }
}
