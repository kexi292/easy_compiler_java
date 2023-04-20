package lexer;

/**
 * @author kexi290
 */
public class Token {
    public final int tag;
    public Token(int t){tag=t;}
    public String toString(){
        return ""+(char)tag;
    }

}
