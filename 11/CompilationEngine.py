from SymbolTable import SymbolTable
from VMWriter import VMWriter

class CompilationEngine:
    
    KEYWORD = 'KEYWORD'
    SYMBOL = 'SYMBOL'
    IDENTIFIER = 'IDENTIFIER'
    INT_CONST = 'INT_CONST'
    STRING_CONST = 'STRING_CONST'

    CLASS = 'class'
    METHOD = 'method'
    FUNCTION = 'function'
    CONSTRUCTOR = 'constructor'
    INT = 'int'
    BOOLEAN = 'boolean'
    CHAR = 'char'
    VOID = 'void'
    VAR = 'var'
    STATIC = 'static'
    FIELD = 'field'
    LET = 'let'
    DO = 'do'
    IF = 'if'
    ELSE = 'else'
    WHILE = 'while'
    RETURN = 'return'
    TRUE = 'true'
    FALSE = 'false'
    NULL = 'null'
    THIS = 'this'

    SEMICOLON = ';'
    L_PARENTHESES = '('
    R_PARENTHESES = ')'
    L_BRACKET = '['
    R_BRACKET = ']'

    

    #creates a new compilation engine with the given input
    #and output. The next routine called must be compileClass()
    def __init__(self, tokenizer, output_stream, vm_writer):
        print('CompilationEngine  initialized')
        #self.output = open(output_stream, 'w')
        self.tokenizer = tokenizer
        self.symboltable = SymbolTable()
        self.vm_writer = vm_writer
        self.label = 0

        #goes to the first token in the stream
        self.tokenizer.advance()
        

    def produceLabel(self):
        label = 'LABEL_' + str(self.label)
        self.label = self.label + 1
        return label

    def eat_token_type(self, tok_type):
        #for identifiers
        tok = self.tokenizer.current_token
        if self.tokenizer.tokenType() == tok_type:
            self.tokenizer.advance()
            return tok
        else:
            raise Exception('Expected a token of type ' + tok_type + ' but found ' + tok)

    def eat_token(self, tok_type, valid_set):        
        #for valid keywords and brackets
        tok = self.tokenizer.current_token
        if self.tokenizer.tokenType() == tok_type and tok in valid_set:
            self.tokenizer.advance()
            return tok
        else:
            raise Exception('Expected a token of type ' + tok_type + ' from the set: ' + valid_set + ' but found ' + tok)

    def compileClass(self):
        self.eat_token(self.KEYWORD, {self.CLASS})
        self.tokenizer.class_name = self.eat_token_type(self.IDENTIFIER) #className
        self.eat_token(self.SYMBOL, {'{'})
        #classVarDec*
        while self.tokenizer.tokenType() == self.KEYWORD and self.tokenizer.keyWord() in {'static', 'field'}:
            self.compileClassVarDec()  
        #subroutineDec*
        while self.tokenizer.tokenType() == self.KEYWORD and self.tokenizer.keyWord() in {'constructor', 'function', 'method'}:
            self.compileSubroutine()
            
        self.eat_token(self.SYMBOL, {'}'})
        self.vm_writer.close()
        return
    
    def compileVoidOrType(self):
        if (self.tokenizer.tokenType() == self.KEYWORD and self.tokenizer.keyWord() in {'int', 'char', 'boolean', 'void'}):
            self.tokenizer.advance()
        elif (self.tokenizer.tokenType() == self.IDENTIFIER):
            self.tokenizer.advance()
        else:
            raise Exception('expected int, char, boolean, or className')

    def compileType(self):
        curr_tok = self.tokenizer.current_token
        tok_type = self.tokenizer.tokenType()
        if (tok_type == self.KEYWORD and curr_tok in {self.INT, self.CHAR, self.BOOLEAN}):
            self.tokenizer.advance()
        elif (tok_type == self.IDENTIFIER):
            self.tokenizer.advance()
        else:
            raise Exception('expected int, char, boolean, or className')

        return curr_tok

    def compileClassVarDec(self):
        #set the parameters
        identifier_kind = self.eat_token(self.KEYWORD, {self.STATIC, self.FIELD})
        identifier_type = self.compileType()
        identifier_name = self.eat_token_type(self.IDENTIFIER)
        self.symboltable.define(identifier_name, identifier_type, identifier_kind, False)

        #if the line continues,set the same parameters
        while self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == ',':
            self.tokenizer.advance()
            identifier_name = self.eat_token_type(self.IDENTIFIER)
            self.symboltable.define(identifier_name, identifier_type, identifier_kind, False)

        self.eat_token(self.SYMBOL, {';'})
        
        return

    def compileSubroutine(self):
        #reset subrountine symbol table
        self.symboltable.startSubroutine()

        subroutine_type = self.eat_token(self.KEYWORD, {self.CONSTRUCTOR, self.FUNCTION, self.METHOD})
        if subroutine_type == self.METHOD:
            self.symboltable.define('this', self.tokenizer.class_name, 'argument', True)
        
        self.compileVoidOrType() #('void' | type)
    
        subroutine_identifier = self.eat_token_type(self.IDENTIFIER) # subroutineName:identifier
    
        self.eat_token(self.SYMBOL, {'('})
        self.compileParameterList() #parameterList:
        self.eat_token(self.SYMBOL, {')'})
        
        #subroutineBody
        self.eat_token(self.SYMBOL, {'{'})
        
        #varDec*
        while self.tokenizer.tokenType() == self.KEYWORD and self.tokenizer.keyWord() == self.VAR:
            self.compileVarDec()
            
        #we can now write to the vm file the declaration
        self.vm_writer.writeFunction(self.tokenizer.class_name + '.' + subroutine_identifier, self.symboltable.count_map['local'])
        #if constructor
        if subroutine_type == self.CONSTRUCTOR:
            self.vm_writer.writePush('constant', self.symboltable.count_map['field'])
            self.vm_writer.writeCall('Memory.alloc', 1)
            self.vm_writer.writePop('pointer', 0)
        
        if subroutine_type == self.METHOD:
            self.vm_writer.writePush('argument', 0)
            self.vm_writer.writePop('pointer', 0)
        
        self.compileStatements()
        self.eat_token(self.SYMBOL, {'}'})

        return

    def compileParameterList(self):
        #perhaps empty
        if self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == ')':
            return
        
        identifier_type = self.compileType()
        identifier_name = self.eat_token_type(self.IDENTIFIER)
        self.symboltable.define(identifier_name, identifier_type, 'argument', True)
        
        # comma ,,,,,
        while self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == ',': 
            self.tokenizer.advance()
            identifier_type = self.compileType()
            identifier_name = self.eat_token_type(self.IDENTIFIER)
            self.symboltable.define(identifier_name, identifier_type, 'argument', True)
            
        return

    def compileVarDec(self):
        self.eat_token(self.KEYWORD, {self.VAR}) #var
        identifier_kind = 'local'
        identifier_type = self.compileType()

        identifier_name = self.eat_token_type(self.IDENTIFIER) #varName
        self.symboltable.define(identifier_name, identifier_type, identifier_kind, True) 

        while self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == ',':
            self.tokenizer.advance()
            identifier_name = self.eat_token_type(self.IDENTIFIER)
            self.symboltable.define(identifier_name, identifier_type, identifier_kind, True) 
            

        self.eat_token(self.SYMBOL, {';'})
        
        return
    
    def compileStatements(self):
        statements_set = {self.LET, self.IF, self.WHILE, self.DO, self.RETURN}
        while self.tokenizer.tokenType() == self.KEYWORD and self.tokenizer.keyWord() in statements_set:
            tok = self.tokenizer.current_token
            if tok == self.LET: self.compileLet() 
            if tok == self.IF: self.compileIf()
            if tok == self.WHILE: self.compileWhile()
            if tok == self.DO: self.compileDo()
            if tok == self.RETURN: self.compileReturn()
                
        return

    def compileDo(self):
        self.eat_token(self.KEYWORD, {self.DO})
        initial_identifier = self.eat_token_type(self.IDENTIFIER)
        is_method_call = False

        if self.tokenizer.current_token not in {'.', '('}:
            raise Exception('Excepted either a period or a left parentheses but instead: ' + self.tokenizer.current_token)

        # varName|className.subroutine()
        if self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == '.':
            self.tokenizer.advance()
            dotId = self.eat_token_type(self.IDENTIFIER) #subroutineName:identifier
            #is the initial_identifier a className or a varName?
            if self.symboltable.contains(initial_identifier):
                is_method_call = True
                segment = self.symboltable.kindOf(initial_identifier)
                if segment == 'field': segment = 'this'
                self.vm_writer.writePush(segment, self.symboltable.indexOf(initial_identifier))
                full_call_identifier = self.symboltable.typeOf(initial_identifier) + '.' + dotId
            else:
                full_call_identifier = initial_identifier + '.' + dotId
        
        #subroutine()
        elif self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == '(':
            is_method_call = True
            self.vm_writer.writePush('pointer', 0)
            full_call_identifier = self.tokenizer.class_name + '.' + initial_identifier
        
        
        
        self.eat_token(self.SYMBOL, {'('})
        n_parameters = self.compileExpressionList() #expressionList
        self.eat_token(self.SYMBOL, {')'})
        #add the extra argument for the method call
        if is_method_call: n_parameters = n_parameters + 1

        self.eat_token(self.SYMBOL, {';'}) #semi-colon ;;;;;;
        self.vm_writer.writeCall(full_call_identifier, n_parameters)
        self.vm_writer.writePop('temp', 0)
    
        return

    def compileLet(self):
        self.eat_token(self.KEYWORD, {self.LET}) 
        lhs_var = self.eat_token_type(self.IDENTIFIER) #varName
        is_array = False
        #what type of variable is this?
        
        #array indexing into the variable
        if self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == '[':
            is_array = True
            self.tokenizer.advance()
            segment = self.symboltable.kindOf(lhs_var)
            if segment == 'field': segment = 'this'
            self.vm_writer.writePush(segment, self.symboltable.indexOf(lhs_var))
            self.compileExpression()
            self.vm_writer.writeArithmetic('+', True)
            self.eat_token(self.SYMBOL, {']'})
            

        self.eat_token(self.SYMBOL, {'='})
        self.compileExpression()
        self.eat_token(self.SYMBOL, {';'})

        if is_array:
            self.vm_writer.writePop('temp', 0)
            self.vm_writer.writePop('pointer', 1)
            self.vm_writer.writePush('temp', 0)
            self.vm_writer.writePop('that', 0)
        else:
            segment = self.symboltable.kindOf(lhs_var)
            if segment == 'field': segment = 'this'
            self.vm_writer.writePop(segment, self.symboltable.indexOf(lhs_var))
    

        return

    def compileWhile(self):
        self.eat_token(self.KEYWORD, {self.WHILE})
        
        label_1 = self.produceLabel()
        label_2 = self.produceLabel()
        
        self.vm_writer.writeLabel(label_1)
        self.eat_token(self.SYMBOL, {'('})
        self.compileExpression()
        self.eat_token(self.SYMBOL, {')'})
        
        self.vm_writer.writeArithmetic('~', False) #negate the expression
        self.vm_writer.writeIf(label_2)
        
        self.eat_token(self.SYMBOL, {'{'})
        self.compileStatements()
        self.eat_token(self.SYMBOL, {'}'})

        self.vm_writer.writeGoto(label_1) #goto LABEL_1
        self.vm_writer.writeLabel(label_2) #label LABEL_2
        
        return

    def compileReturn(self):
        self.eat_token(self.KEYWORD, {self.RETURN})
        
        if self.tokenizer.current_token != ';': # expression?
            self.compileExpression()
            self.eat_token(self.SYMBOL, {';'})
        else:                                   #no expression 
            self.eat_token(self.SYMBOL, {';'})
            self.vm_writer.writePush('constant', 0)
        
        self.vm_writer.writeReturn()
        return

    def compileIf(self):
        self.eat_token(self.KEYWORD, {self.IF})

        label_1 = self.produceLabel()
        label_2 = self.produceLabel() #might not be used if there is no else
        
        self.eat_token(self.SYMBOL, {'('})
        self.compileExpression() #expression
        self.eat_token(self.SYMBOL, {')'})
        
        self.vm_writer.writeArithmetic('~', False) #negate the expression
        self.vm_writer.writeIf(label_1)

        self.eat_token(self.SYMBOL, {'{'})
        self.compileStatements()
        self.eat_token(self.SYMBOL, {'}'})
    
        #maybe else
        if self.tokenizer.tokenType() == self.KEYWORD and self.tokenizer.keyWord() == self.ELSE:
            self.vm_writer.writeGoto(label_2) #goto LABEL_2
            self.vm_writer.writeLabel(label_1) #label LABEL_1
            self.tokenizer.advance()
            self.eat_token(self.SYMBOL, {'{'})
            self.compileStatements()
            self.eat_token(self.SYMBOL, {'}'})
            self.vm_writer.writeLabel(label_2) #label LABEL_2
        else:
            self.vm_writer.writeLabel(label_1) #label LABEL_1    
        

        return

    #perhaps make a set above to contain all the operators?
    op_set = {'+', '-', '*', '/', '&', '|', '<', '>', '=', '&lt;', '&gt;', '&amp;'}
    def compileExpression(self):
        self.compileTerm()

        while self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() in self.op_set:
            operator = self.tokenizer.current_token
            self.tokenizer.advance()
            self.compileTerm()
            self.vm_writer.writeArithmetic(operator, True)
        return

    def compileTerm(self):
        #integerConstant
        if self.tokenizer.tokenType() == self.INT_CONST:
            #push the constant to the stack
            self.vm_writer.writePush('constant', self.tokenizer.current_token) 
            self.tokenizer.advance()
            return
        
        #stringConstant
        if self.tokenizer.tokenType() == self.STRING_CONST:
            tok = self.tokenizer.current_token
            self.vm_writer.writePush('constant', len(tok))
            self.vm_writer.writeCall('String.new', 1)

            for char in tok:
                self.vm_writer.writePush('constant', ord(char))
                self.vm_writer.writeCall('String.appendChar', 2)

            self.tokenizer.advance()
            return

        #keywordConstant
        key_const_set = {self.TRUE, self.FALSE, self.NULL, self.THIS}
        if self.tokenizer.tokenType() == self.KEYWORD and self.tokenizer.keyWord() in key_const_set:
            keyword_constant = self.tokenizer.current_token
            
            if keyword_constant == self.TRUE:
                self.vm_writer.writePush('constant', 1)
                self.vm_writer.writeArithmetic('-', False)
            
            if keyword_constant in {self.FALSE, self.NULL}:
                self.vm_writer.writePush('constant', 0)
            
            if keyword_constant == self.THIS:
                self.vm_writer.writePush('pointer', 0)
            
            self.tokenizer.advance()
            return

        #varName | varName[expression] | subroutineName() | className.subroutine() | varName.subrountine()
        if self.tokenizer.tokenType() == self.IDENTIFIER:
            main_identifier = self.tokenizer.current_token 
            self.tokenizer.advance()
            
            #varName[expression]
            if self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == '[':
                self.tokenizer.advance()
                #push the array variable onto the stack
                segment = self.symboltable.kindOf(main_identifier)
                if segment == 'field': segment = 'this'
                self.vm_writer.writePush(segment, self.symboltable.indexOf(main_identifier))
                self.compileExpression()
                self.eat_token(self.SYMBOL, {']'}) #close the array
                self.vm_writer.writeArithmetic('+', True)
                self.vm_writer.writePop('pointer', 1)
                self.vm_writer.writePush('that', 0)
                return
                
            #subrountineName()
            if self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == '(':
                self.tokenizer.advance()
                n_parameters = self.compileExpressionList()
                self.eat_token(self.SYMBOL, {')'})
                #should we do this right here without a semicolan??
                self.vm_writer.writeCall(main_identifier, n_parameters)
                return

            #className|varName.subroutine()   
            if self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == '.': 
                self.tokenizer.advance()
                
                is_var_name = self.symboltable.contains(main_identifier)

                dotId = self.eat_token_type(self.IDENTIFIER)
                #if it is a variable.
                
                if is_var_name:
                    segment = self.symboltable.kindOf(main_identifier)
                    if segment == 'field': segment = 'this'
                    self.vm_writer.writePush(segment, self.symboltable.indexOf(main_identifier))
                    main_identifier = self.symboltable.typeOf(main_identifier) + '.' + dotId
                else:
                    main_identifier = main_identifier + '.' +  dotId
            
                self.eat_token(self.SYMBOL, {'('})
                
                n_parameters = self.compileExpressionList()
                #what about methods? they get an extra argument no? 
                
                self.eat_token(self.SYMBOL, {')'})
                if is_var_name: n_parameters = n_parameters + 1
                self.vm_writer.writeCall(main_identifier, n_parameters)
                return
                


            #push the variable to the stack (if it exists in the current scope)
            segment = self.symboltable.kindOf(main_identifier)
            if segment == 'field': segment = 'this'
            self.vm_writer.writePush(segment, self.symboltable.indexOf(main_identifier))
            return 

        #(expression)
        if self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == '(':
            self.tokenizer.advance()
            self.compileExpression()
            self.eat_token(self.SYMBOL, {')'})
            return

        #unary opp
        if self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() in {'-', '~'}:
            operator = self.tokenizer.current_token
            self.tokenizer.advance()
            self.compileTerm()
            self.vm_writer.writeArithmetic(operator, False)
            return
    
        #if we got here, raise exception
        raise Exception('end of the term and nothing was found')

    def compileExpressionList(self):
        num_expressions = 0

        if self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == ')':
            return num_expressions
        
        self.compileExpression()
        num_expressions = num_expressions + 1

        while self.tokenizer.tokenType() == self.SYMBOL and self.tokenizer.symbol() == ',':
            self.tokenizer.advance()
            self.compileExpression()
            num_expressions = num_expressions + 1

        return num_expressions