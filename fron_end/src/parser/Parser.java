package parser;
import java.io.*;
import lexer.*;
import symbols.*;
import inter.*;

public class Parser {
    /** 这个语法分析器的词法分析器 */
    private Lexer lex;
    /** 向前看词法单元 */
    private Token look;
    /** 当前或顶层的符号表 */
    Env top = null;
    /** 用于变量声明的存储位置 */
    int used = 0;
    public Parser(Lexer l) throws IOException { lex = l; move();}
    void move() throws IOException { look = lex.scan(); }
    void error(String s) { throw new Error("near line "+ Lexer.line +": "+s ); }
    void match(int t) throws IOException {
        if( look.tag == t ) {
            move();
        }else {
            error("syntax error");
        }
    }
    /** program -> block   */
    public void program() throws IOException {
        Stmt s = block();
        int begin = s.newlabel(); int after = s.newlabel();
        s.emitlabel(begin); s.gen(begin,after); s.emitlabel(after);
    }
    /** block -> {decls stmts} */
    Stmt block() throws IOException {
        match('{'); Env savedEnv = top; top = new Env(top);
        decls(); Stmt s = stmts();
        match('}');top = savedEnv;
        return s;
    }
    void decls() throws IOException {
        /** D -> type ID */
        while( look.tag == Tag.BASIC ) {
            Type p = type(); Token tok = look; match(Tag.ID); match(';');
            Id id = new Id((Word)tok,p,used);
            top.put(tok,id);
            used = used + p.width;
        }
    }
    Type type() throws IOException {
        /** 期望 look.tag == Tag.BASIC */
        Type p= (Type)look;
        match(Tag.BASIC);
        /** T -> basic */
        if( look.tag != '[' ) {
            return p;
        }else {
            /** 返回数组类型 */
            return dims(p);
        }
    }
    Type dims(Type p) throws IOException {
        match('['); Token tok = look; match(Tag.NUM); match(']');
        if( look.tag == '[') {
            p = dims(p);
        }
        return new Array(((Num)tok).value,p);
    }
    Stmt stmts() throws IOException {
        if( look.tag == '}' ) {
            return Stmt.Null;
        }else {
            return new Seq(stmt(),stmts());
        }
    }
    Stmt stmt() throws IOException {
        Expr x; Stmt s,s1,s2;
        /** 用于为break语句保存外层的循环语句 */
        Stmt savedStmt;
        switch( look.tag ) {
            case ';':
                move();
                return Stmt.Null;
            case Tag.IF:
                match(Tag.IF);match('(');x=bool();match(')');
                s1 = stmt();
                if( look.tag != Tag.ELSE ) {
                    return new If(x,s1);
                }
                match(Tag.ELSE);
                s2 = stmt();
                return new Else(x,s1,s2);
            case Tag.WHILE:
                While whilenode = new While();
                savedStmt = Stmt.Enclosing;Stmt.Enclosing = whilenode;
                match(Tag.WHILE);match('(');x=bool();match(')');
                s1 = stmt();
                whilenode.init(x,s1);
                /** 重置Stmt.Enclosing */
                Stmt.Enclosing = savedStmt;
                return whilenode;
            case Tag.DO:
                Do donode = new Do();
                savedStmt = Stmt.Enclosing; Stmt.Enclosing = donode;
                match(Tag.DO);
                s1 = stmt();
                match(Tag.WHILE); match('('); x = bool(); match(')'); match(';');
                donode.init(s1,x);
                /** 重置Stmt.Enclosing */
                Stmt.Enclosing = savedStmt;
                return donode;
            case Tag.BREAK:
                match(Tag.BREAK); match(';');
                return new Break();
            case '{':
                return block();
            default:
                return assign();
        }
    }
    Stmt assign() throws IOException {
        Stmt stmt; Token t = look;
        match(Tag.ID);
        Id id = top.get(t);
        if( id == null ) {
            error(t.toString()+" undeclared");
        }

        if( look.tag == '=') {
            /** S -> id = E; */
            move(); stmt = new Set(id,bool());
        }else {
            /** S -> L = E; */
            Access x = offset(id);
            match('='); stmt = new SetElem(x,bool());
        }
        match(';');
        return stmt;
    }

    Expr bool() throws IOException {
        Expr x = join();
        while( look.tag == Tag.OR ) {
            Token tok = look; move(); x = new Or(tok,x,join());
        }
        return x;
    }
    Expr join() throws IOException {
        Expr x = equality();
        while( look.tag == Tag.AND ) {
            Token tok = look; move(); x = new And(tok,x,equality());
        }
        return x;
    }
    Expr equality() throws IOException {
        Expr x = rel();
        while( look.tag == Tag.EQ || look.tag == Tag.NE ) {
            Token tok = look; move(); x = new Rel(tok,x,rel());
        }
        return x;
    }
    Expr rel() throws  IOException {
        Expr x = expr();
        switch ( look.tag ) {
            case '<' : case Tag.LE : case Tag.GE : case '>':
                Token tok = look; move(); return new Rel(tok,x,expr());
            default:
                return x;
        }
    }
    Expr expr() throws IOException {
        Expr x = term();
        while( look.tag == '+' || look.tag == '-' ) {
            Token tok = look; move(); x =new Arith(tok,x,unary());
        }
        return x;
    }
    Expr term() throws IOException {
        Expr x = unary();
        while( look.tag == '*' || look.tag == '/' ) {
            Token tok = look; move(); x = new Arith(tok,x,unary());
        }
        return x;
    }
    Expr unary() throws  IOException {
        if( look.tag == '-' ) {
            move(); return new Unary(Word.minus,unary());
        }else if( look.tag == '!' ) {
            Token tok = look; move(); return new Not(tok,unary());
        }else {
            return factor();
        }
    }
    Expr factor() throws IOException {
        Expr x = null;
        switch ( look.tag ) {
            case '(':
                move(); x = bool();match(')');
                return x;
            case Tag.NUM:
                x = new Constant(look,Type.Int); move(); return x;
            case Tag.REAL:
                x = new Constant(look,Type.Float); move(); return x;
            case Tag.TRUE:
                x = Constant.True; move(); return x;
            case Tag.FALSE:
                x = Constant.False; move(); return x;
            default:
                error("syntax error");
                return x;
            case Tag.ID:
                String s = look.toString();
                Id id = top.get(look);
                if(id == null ){
                    error(look.toString() + " undeclared");
                }
                move();
                if( look.tag !='[') {
                    return id;
                }else {
                    return offset(id);
                }

        }
    }
    /** I -> [E] | [E] I*/
    Access offset(Id a) throws IOException {
        /**继承 id */
        Expr i; Expr w; Expr t1,t2; Expr loc;
        Type type = a.type;
        /**第一个下标，I->[E]*/
        match('[');i = bool(); match(']');
        type = ((Array)type).of;
        w = new Constant(type.width);
        t1 = new Arith(new Token('*'),i,w);
        loc = t1;
        while( look.tag == '[') {
            match('['); i = bool();match(']');
            type = ((Array)type).of;
            w = new Constant(type.width);
            t1 = new Arith(new Token('*'),i,w);
            t2 = new Arith(new Token('+'),i,w);
            loc = t2;
        }
        return new Access(a,loc,type);
    }

}
