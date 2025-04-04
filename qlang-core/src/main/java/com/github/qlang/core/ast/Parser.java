package com.github.qlang.core.ast;

import com.github.qlang.core.ast.node.DivOp;
import com.github.qlang.core.ast.node.MinusOp;
import com.github.qlang.core.ast.node.ModOp;
import com.github.qlang.core.ast.node.MulOp;
import com.github.qlang.core.ast.node.NegOp;
import com.github.qlang.core.ast.node.Node;
import com.github.qlang.core.ast.node.Num;
import com.github.qlang.core.ast.node.PlusOp;
import com.github.qlang.core.ast.node.PosOp;
import com.github.qlang.core.ast.node.PowOp;
import com.github.qlang.core.ast.token.Token;
import com.github.qlang.core.ast.token.TokenType;
import com.github.qlang.core.ast.token.Tokenizer;
import com.github.qlang.core.ast.token.Tokens;
import com.github.qlang.core.exception.ParseException;

import java.util.ArrayList;
import java.util.List;

public class Parser extends Iterator<Token> {

    public Parser(String input) {
        this(new Tokenizer(input));
    }

    public Parser(Tokenizer tokenizer) {
        super(parseTokens(tokenizer));
    }

    private static Token[] parseTokens(Tokenizer tokenizer) {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = tokenizer.nextToken();
            tokens.add(token);
        } while (!token.is(TokenType.EOF));
        return tokens.toArray(new Token[0]);
    }

    public Node parse() {
        Node eat = eat();
        if (has()) {
            throw new ParseException("unexpected end, has more tokens");
        }
        return eat;
    }

    public Node eat() {
        return eatPlusMinus();
    }

    public Node eatPlusMinus() {
        Node left = eatMulDivMod();
        while (peek().in(TokenType.PLUS, TokenType.MINUS)) {
            Token op = peek();
            advance();
            Node right = eatMulDivMod();
            if (op.is(TokenType.PLUS)) {
                left = new PlusOp(left, right);
            } else if (op.is(TokenType.MINUS)) {
                left = new MinusOp(left, right);
            } else {
                throw new ParseException("unsupported operator: " + op);
            }
        }
        return left;
    }

    private Node eatMulDivMod() {
        Node left = eatPow();
        while (peek().in(TokenType.MUL, TokenType.DIV, TokenType.MOD)) {
            Token op = peek();
            advance();
            Node right = eatPow();
            if (op.is(TokenType.MUL)) {
                left = new MulOp(left, right);
            } else if (op.is(TokenType.DIV)) {
                left = new DivOp(left, right);
            } else if (op.is(TokenType.MOD)) {
                left = new ModOp(left, right);
            } else {
                throw new ParseException("unsupported operator: " + op);
            }
        }
        return left;
    }

    private Node eatPow() {
        Node left = eatPosNeg();
        while (peek().is(TokenType.POW)) {
            advance();
            Node right = eatPosNeg();
            left = new PowOp(left, right);
        }
        return left;
    }

    private Node eatPosNeg() {
        Token op = peek();
        if (op.in(TokenType.PLUS, TokenType.MINUS)) {
            advance();
            if (op.is(TokenType.PLUS)) {
                return new PosOp(eatPosNeg());
            } else {
                return new NegOp(eatPosNeg());
            }
        } else {
            return eatUnit();
        }
    }

    private Node eatUnit() {
        Token token = peek();
        advance();
        if (Tokens.LPAREN.equals(token)) {
            Node eat = eat();
            if (Tokens.RPAREN.equals(peek())) {
                advance(); // 跳过右括号
                return eat;
            }
            throw new ParseException("expected rparen, but: " + peek());
        } else if (token.is(TokenType.NUMBER)) {
            return new Num(token.getValue());
        } else {
            throw new ParseException("unexpected token: " + token);
        }
    }

    @Override
    public boolean has() {
        return !peek().is(TokenType.EOF);
    }
}
