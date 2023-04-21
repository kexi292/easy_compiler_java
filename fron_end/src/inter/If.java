package inter;
import symbols.*;
public class If extends Stmt {
    Expr expr; Stmt stmt;
    public If(Expr x , Stmt s ) {
        expr = x ;
        stmt = s;
        if(expr.type != Type.Bool ) {
            expr.error("boolean required in if");
        }
    }
    public void gen(int b, int a ) {
        /** stmt的代码标号 */
        int label = newlabel();
        /** 为真时控制流穿越，为假时转向a */
        expr.jumping(0,a);
        emitlabel(label);
        stmt.gen(label,a);
    }
}
