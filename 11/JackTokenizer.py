# Removes all comments and white space from the input stream and breaks
# it into Jack-language tokens, as specified by the Jack grammar. 

class JackTokenizer:
    keyword_set = {
        'class',
        'constructor',
        'function',
        'method',
        'field',
        'static',
        'var',
        'int',
        'char',
        'boolean',
        'void',
        'true',
        'false',
        'null',
        'this',
        'let',
        'do',
        'if',
        'else',
        'while',
        'return'
    } 

    symbol_set = {
        '{',
        '}',
        '(',
        ')',
        '[',
        ']',
        '.',
        ',',
        ';',
        '+',
        '-',
        '*',
        '/',
        '&',
        '|',
        '<',
        '>',
        '=',
        '~'
    }

    class_name = ''

    
    #opens the input file/stream and gets ready to tokenize it.
    def __init__(self, input_file):
        #print('Initializing the JackTokenizer')
        self.f = open(input_file, 'r')
        self.current_token = 'init'
        self.token_type = None
        

    def getCurrentToken(self):
        return self.current_token
    
    #do we have more tokens in the input?
    def hasMoreTokens(self):
        if self.token_type == 'EOF':
            return False 
        #else
        return True

    #Gets the next token from the input and makes it the current token
    #This method should only be called if hasMoreTokens() is true
    #Initially there is no current token
    def advance(self):
        
        buffer = self.f.read(1)
        while buffer == ' ' or buffer == '\n' or buffer == '\t':
            buffer = self.f.read(1)
        #is it a comment??
        if buffer == '/':
            next_char = self.readNextCharacterAndRewind()
            #multiple comment
            if next_char == '*':
                self.f.read(1)
                self.readUntilEndOfComment()
                self.advance()
                return self.current_token
            #single line comment
            if next_char == '/':
                self.f.read(1)
                self.readUntilEndOfLine()
                self.advance()
                return self.current_token

        #readUntilEndOfString sets tokentype to STRING_CONST
        if buffer == '"':
            self.readUntilEndOfString()
            self.token_type = 'STRING_CONST'
            return self.current_token
        
        #is it an integer??
        if buffer.isdigit():
            self.readUntilEndOfInteger(buffer)
            self.token_type = 'INT_CONST'
            return self.current_token

        #is it a letter or underscore??
        if self.charIsAlphaOrUnderscore(buffer):
            self.readUntilEndOfKeywordOrIdentifier(buffer)
            if self.current_token in self.keyword_set:
                self.token_type = 'KEYWORD'
            else:
                self.token_type = 'IDENTIFIER'
            return self.current_token

        #is it a symbol??
        if buffer in self.symbol_set:
            self.current_token = buffer
            self.token_type = 'SYMBOL'

            return self.current_token

        
        
        self.current_token = '@EOF@'
        self.token_type = 'EOF'
        return self.current_token
        

    def readNextCharacterAndRewind(self):
        last_pos = self.f.tell()
        next_char = self.f.read(1)
        self.f.seek(last_pos)
        return next_char    

    #consumes until it encounters \n in the file stream
    def readUntilEndOfLine(self):
        buffer = self.f.read(1)
        while buffer != '\n':
            buffer = self.f.read(1)

    #consumes until it encounters */ in the file stream
    def readUntilEndOfComment(self):
        while True:
            buffer = self.f.read(1)
            while buffer != '*':
                buffer = self.f.read(1)

            last_pos = self.f.tell()
            buffer = self.f.read(1)  
            if buffer == '/':
                break
            else:
                self.f.seek(last_pos)
        
    def readUntilEndOfString(self):
        token = ''
        while True:
            buffer = self.f.read(1)
            if buffer == '"':
                break
            else:
                token = token + buffer
        
        self.current_token = token

    def readUntilEndOfInteger(self, startChar):
        token = startChar
        while True:
            buffer = self.f.read(1)
            if not buffer.isdigit():
                position = self.f.tell()
                self.f.seek(position - 1)
                break
            else:
                token = token + buffer
        
        self.current_token = token
    

    def readUntilEndOfKeywordOrIdentifier(self, startChar):
        token = startChar
        while True:
            current_position = self.f.tell()
            buffer = self.f.read(1)
            
            
            if buffer.isdigit() or buffer.isalpha() or buffer == '_':
                token = token + buffer
                
        
            else:
                self.f.seek(current_position)
                break
        

        self.current_token = token
    
    def charIsAlphaOrUnderscore(self, char):
        if char.isalpha() or char == '_':
            return True
        else:
            return False

    #returns the type of the current token
    def tokenType(self):
        return self.token_type
    
    def keyWord(self):
        if self.token_type == 'KEYWORD':
            return self.current_token

    def symbol(self):
        if self.token_type == 'SYMBOL':
            reserved_xml_dict = {
                '<' : '&lt;',
                '>' : '&gt;',
                '&' : '&amp;'
            }
            if self.current_token in reserved_xml_dict.keys():
                return reserved_xml_dict.get(self.current_token)
            
            return self.current_token

    def identifier(self):
        if self.token_type == 'IDENTIFIER':
            return self.current_token
        
    def intVal(self):
        if self.token_type == 'INT_CONST':
            return self.current_token
    
    def stringVal(self):
        if self.token_type == 'STRING_CONST':
            return self.current_token   