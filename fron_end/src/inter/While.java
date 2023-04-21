package inter;
import symbols.*;
public class While extends Stmt{
    Expr expr; Stmt stmt;
    public While() {expr = null; stmt = null; }
    public void init(Expr x, Stmt s ) {
        expr = x; stmt = s;
        if(expr.type != Type.Bool ) {
            expr.error("boolean required in while");
        }
    }
    public void gen(int b,int a) {
        /** 保存标号a */
        expr.jumping(0,a);
        /** 用于stmt的标号 */
        int label = newlabel();
        emitlabel(label); stmt.gen(label,b);
        emit("goto L" +b);
    }
}
