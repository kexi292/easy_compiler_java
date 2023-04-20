package lexer;

/**
 * @author kexi290
 * 用来处理数字
 */
public class Num extends Token{
    public final int value;
    public Num(int v) {
        super(Tag.NUM);
        value=v;
    }
    public String toString(){return "" + value;}

}
