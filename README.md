# easy_compiler_java
According to 《Compilers: Principles, Techniques, and Tools》
参考附录A中的简易编译器（java)。

参考资料：

包结构：

```
--main        
--lexer  	  
--symbols     
--parser      
--inter       
```

 各个包中主体代码大致内容：

```
--main        程序从这执行
--lexer  	 词法分析器
--symbols     符号表和类型
--parser      语法分析器
--inter       表达式的中间代码、布尔表达式的跳转代码、语句的中间代码
```

