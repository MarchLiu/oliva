package io.github.marchliu.lexers;

import jaskell.parsec.common.Parsec;

import static jaskell.parsec.common.Atom.pack;

public record Token(String category, String token){
    public static Token create(String category, String token) {
        return new Token(category, token);
    }

    public static Parsec<Character, Token> symbol(String value) {
        return pack(Token.create("symbol", value));
    }

    public static Parsec<Character, Token> word(String value) {
        return pack(Token.create("word", value));
    }

    public static Parsec<Character, Token> text(String value) {
        return pack(Token.create("text", value));
    }

    public static Parsec<Character, Token> instruction(String value) {
        return pack(Token.create("instruction", value));
    }

    public static Parsec<Character, Token> literal(String value) {
        return pack(Token.create("literal", value));
    }
}