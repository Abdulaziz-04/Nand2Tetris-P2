import sys
import os
from JackTokenizer import JackTokenizer
from CompilationEngine import CompilationEngine
from VMWriter import VMWriter


jack_files = []

if os.path.isdir(sys.argv[1]):
    #for folders
    for i in os.listdir(sys.argv[1]):
        if i.endswith('.jack'):
            jack_files.append(sys.argv[1] + '/' + i)  
else:
    if sys.argv[1].endswith('.jack'):
        jack_files.append(sys.argv[1])
    

    
for i in jack_files:
    tok = JackTokenizer(i)
    vm_writer = VMWriter(i.replace('.jack', '.vm'))
    engine = CompilationEngine(tok, i.replace('.jack', '.xml'), vm_writer)
    engine.compileClass()
