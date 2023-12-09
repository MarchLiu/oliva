package io.github.marchliu.lexers;

import java.util.List;

public interface Tokenizer {
    List<String> cuts(String source);
}
