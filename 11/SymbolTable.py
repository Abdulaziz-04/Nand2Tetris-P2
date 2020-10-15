class SymbolTable:
    
    # constructor: creates a new empty symbol table
    def __init__(self):
        self.class_scope = []
        self.subrountine_scope = []
        
        self.subrountine_type_map = {}
        self.subrountine_kind_map = {}
        self.subrountine_index_map = {}

        self.class_type_map = {}
        self.class_kind_map = {}
        self.class_index_map = {}

        #value of count_map gives the current number of kind variables 
        self.count_map = {
            'static' : 0,
            'field' : 0,
            'argument' : 0,
            'local' : 0
        }

        return
    
    def printAll(self):
        print("class_scope")
        for new_row in self.class_scope:
            print(new_row.identifier_kind + " " +  new_row.identifier_type + " " + new_row.identifier_name + " " + str(new_row.identifier_number))
        print("subroutine_scope")
        for new_row in self.subrountine_scope:
            print(new_row.identifier_kind + " " +  new_row.identifier_type + " " + new_row.identifier_name + " " + str(new_row.identifier_number))
    

    def contains(self, name):
        for row in self.subrountine_scope:
            if row.identifier_name == name:
                return True
        for row in self.class_scope:
            if row.identifier_name == name:
                return True

        return False


    # starts a new subroutine scope 
    # i.e. resets the subroutine's symbol table
    def startSubroutine(self):
        self.subroutine_scope = []
        self.count_map["local"] = 0
        self.count_map["argument"] = 0
        #reset all the subroutine maps
        self.subrountine_type_map = {}
        self.subrountine_kind_map = {}
        self.subrountine_index_map = {}
        return
    #defines a new identifier of a given name, type, and kind and assigns it a running index
    #STATIC and FIELD identifiers have a class scope, while ARG and VAR identifiers have a subroutine scope
    def define(self, name, typee, kind, subroutine):
        
        new_row = scope_row(name, typee, kind, self.count_map[kind])
        if subroutine:
            self.subrountine_scope.append(new_row)
            #populate the maps
            self.subrountine_index_map[name] = self.count_map[kind]
            self.subrountine_kind_map[name] = kind
            self.subrountine_type_map[name] = typee
        else:
            self.class_scope.append(new_row)
            #populate the maps
            self.class_index_map[name] = self.count_map[kind]
            self.class_kind_map[name] = kind
            self.class_type_map[name] = typee

        self.count_map[kind] = self.count_map[kind] + 1
        
        return 
    
    #returns the number of variables of the given kind already defined in the current scope
    def varCount(self, kind):
        return self.count_map[kind]
    
    #returns the kind of the named identifier in the current scope if the identifier is unknown in the current scope, returns NONE
    def kindOf(self, name):
        if name in self.subrountine_kind_map.keys():
            return self.subrountine_kind_map[name]
        elif name in self.class_kind_map.keys():
            return self.class_kind_map[name]
        else:
            raise Exception(name + 'does not exist in the current scope')
    

    # returns the type of the named identifier in the current scope
    def typeOf(self, name):
        if name in self.subrountine_type_map.keys():
            return self.subrountine_type_map[name]
        elif name in self.class_type_map.keys():
            return self.class_type_map[name]
        else:
            raise Exception(name + 'does not exist in the current scope')
        
    
    # returns the index assigned to the named identifier
    def indexOf(self, name):
        if name in self.subrountine_index_map.keys():
            return self.subrountine_index_map[name]
        elif name in self.class_index_map.keys():
            return self.class_index_map[name]
        else:
            raise Exception(name + 'does not exist in the current scope')

class scope_row:
    def __init__(self, id_name, id_type, id_kind, id_number):
        self.identifier_name = id_name
        self.identifier_type = id_type
        self.identifier_kind = id_kind
        self.identifier_number = id_number