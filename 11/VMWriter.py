class VMWriter:
    
    # creates a new file and prepares it for writing
    def __init__(self, output_stream):
        self.output = open(output_stream, 'w')
        return
    
    # writes a VM push command
    def writePush(self, segment, index):
        self.output.write('push ' + segment + ' ' + str(index) + '\n')
        return

    # writes a VM pop command
    def writePop(self, segment, index):
        self.output.write('pop ' + segment + ' ' + str(index) + '\n')
        return
    
    # writes a VM arithmetic command
    # the Jack compiler uses os calls to multiply and divide 
    
    #unaryOp map
    #the neg will not be in the map
    
    #unary operator map
    unary_op_map = {
        '~': 'not',
        '-': 'neg',
    }
    
    #binary operator map
    binary_op_map = {
        '+': 'add',
        '-': 'sub',
        '*': 'call Math.multiply 2',
        '/': 'call Math.divide 2',
        '=': 'eq',
        '>': 'gt',
        '<': 'lt',
        '|': 'or',
        '&': 'and'
        
    }

    #how to get arount the fact that not and sub are the same sumbol? 
    def writeArithmetic(self, command, binary):
        if binary:
            self.output.write(self.binary_op_map[command] + '\n')
        else:
            self.output.write(self.unary_op_map[command] + '\n')
        return

    # writes a VM label command
    def writeLabel(self, label):
        self.output.write('label ' + label + '\n')
        return

    # writes a VM goto command 
    def writeGoto(self, label):
        self.output.write('goto ' + label + '\n')
        return

    # writes a VM If-goto command
    def writeIf(self, label):
        self.output.write('if-goto ' + label + '\n')
        return
    
    # writes a VM call command
    def writeCall(self, name, n_args):
        self.output.write('call' + ' ' + name + ' ' + str(n_args) + '\n')
        pass

    # writes a VM function command
    def writeFunction(self, name, n_locals):
        self.output.write('function' + ' ' + name + ' ' + str(n_locals) + '\n')
        return

    # writes a VM return command
    def writeReturn(self):
        self.output.write('return\n')
        return

    # closes the output file
    def close(self): 
        self.output.close()
        return 

    