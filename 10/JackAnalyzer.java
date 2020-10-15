import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JackAnalyzer {
    static BufferedReader fbr;
    static BufferedWriter fbw;
    static int termFlag=0;
    static int statementFlag=1;
    static int rdbrFlag=0;
    static ArrayList<File> files=new ArrayList<>();
    static String word="";
    static String[] keywords={"class","constructor","function","method","field","static","var","int","char","boolean",
                                "void","true","false","null","this","let","do","if","else","while","return"};
    static String[] symbols={"(",")",
                                "[","]",
                                "{","}",
                                ",",".",";",
                                "+","-","*","/",
                                "|","=","~","&",">","<"};

    public static void main(String args[]){
        try{
            //For a single File
                if(args[0].endsWith(".jack")){
                    String name=args[0].substring(0, args[0].indexOf("."));
                    File opFile=new File(name+"-"+"tmp.xml");
                    //If already exists,replace it
                    if(opFile.exists()){
                        opFile.delete();
                    }
                    BufferedWriter fbw=new BufferedWriter(new FileWriter(opFile));
                    BufferedReader fbr=new BufferedReader(new FileReader(args[0]));

                    JackTokenizer(fbr,fbw);
                    BracketAnalyzer(opFile);
                    CompilationEngine(args[0],"");
            }
            //For a folder
            else{
                File fname=new File(args[0]);
                files=GetAllFiles(fname);
                for(File i : files){
                    String curfName=i.getName().substring(0,i.getName().indexOf("."));
                    File opFile=new File(curfName+"-"+"tmp.xml");
                    BufferedReader fbr=new BufferedReader(new FileReader(i));
                    BufferedWriter fbw=new BufferedWriter(new FileWriter(opFile));
                    JackTokenizer(fbr,fbw);
                    BracketAnalyzer(opFile);
                    CompilationEngine(i.getParentFile().getAbsolutePath(),i.getName());
                }
            }

        }
        catch(FileNotFoundException fe){
            System.out.println(fe);
        }
        catch(IOException e){
            System.out.println(e);
        }
    }




    

    static void JackTokenizer(BufferedReader fbr,BufferedWriter fbw){
        try{
            String chr;
            String line;
            fbw.write("<tokens>\n");
            while((line=fbr.readLine())!=null){
                line=line.trim();
                if(!line.matches("^[//].*|^[\n]*|^[*].*")){
                    //Remove comments and whitespacing
                    if(line.contains("}")){
                        line=line.substring(0,line.indexOf("}")+1);
                    }
                    else if(line.contains(";")){
                        line=line.substring(0,line.indexOf(";")+1);
                    }
                    else if(line.contains("//")){
                        line=line.substring(0,line.indexOf("/"));
                    }
                    line+="\n";
                    //Get each character seperately
                    String[] cur=line.split("");
                    /*for(String i:cur){
                        System.out.print(i);
                    }*/
                    for(int i=0;i<cur.length-1;i++){
                        word+=cur[i];
                        //to get intger constants
                        Matcher intReg=Pattern.compile("\\d{1,5}\\s?").matcher(word);
                        //to get string pattern but not used
                        //Matcher stringReg=Pattern.compile("\"([^_][\\w\\W]*)\"").matcher(word);
                        word=word.trim();

                        //verify keywords
                        if(Verifykw(word)){
                            fbw.write("<keyword>"+word+"</keyword>\n");
                            word="";
                            //System.out.println(i);
                        }

                        //verify symbols returns a boolean symval returns xml accepted value
                        else if(Verifysymb(word)){
                            if(word.contentEquals(";")){
                                fbw.write("<spToken/>\n");
                                fbw.write("<symbol>"+word+"</symbol>\n");
                            }
                            else{
                                chr=Symval(word);
                                fbw.write("<symbol>"+chr+"</symbol>\n");
                            }
                            word="";
                        }

                        //verify integer constants
                        else if(intReg.matches()){
                            if(Verifysymb(cur[i+1])){
                                fbw.write("<integerConstant>"+intReg.group(0)+"</integerConstant>\n");
                                word="";
                            }
                        }

                        //verify string constants
                        else if(cur[i].contentEquals("\"")){
                            word=word.replace("\"","");
                            i+=1;
                            while(!(cur[i].contentEquals("\""))){
                                word+=cur[i];
                                i+=1;
                            }
                            fbw.write("<stringConstant>"+word+"</stringConstant>\n");
                            word="";
                        }

                        //verify identifiers with specifically implied conditions
                        else if((cur[i+1].contentEquals(" ") || cur[i+1].contentEquals("(")|| 
                                    cur[i+1].contentEquals(".")||Verifysymb(cur[i+1]))&&(!cur[i].contentEquals(" "))){
                            fbw.write("<identifier>"+word+"</identifier>\n");
                            word="";
                        }
                        }
                        }
                    }
            fbw.write("</tokens>\n");
            fbr.close();
            fbw.close();
        }
        catch(IOException e){
            System.out.println(e);

        }
}
    //Verification methods
    static boolean Verifykw(String w){
        for(String i:keywords){
            if(i.contentEquals(w)){
                return true;
            }
        }
        return false;
    }

    static boolean Verifysymb(String w){
        for(String i:symbols){
            if(i.contentEquals(w)){
                return true;
            }
        }
        return false;
    }

    static String Symval(String w){
        switch(w){
            case ">":
                return "&gt;";
            case "<":
                return "&lt;";
            case "&":
                return "&amp;";
            default:
                return w;
            
        }
    }

    //---------------------------------------------------COMPILATION ENGINE-----------------------------------------//

    static void CompilationEngine(String pdName,String fName){
        String line;
        String rName;
        String wName="";
        try{
            if(pdName.endsWith(".jack")){
                rName=pdName.substring(0, pdName.indexOf("."));
                wName=rName;
            }
            else{
                rName=fName.substring(0,fName.indexOf("."));
                wName=pdName+"//"+rName;
            }
            
            //Read a temporary tokenized file and write to the final result file
            fbr=new BufferedReader(new FileReader(rName+"-"+"tmp.xml"));
            fbw=new BufferedWriter(new FileWriter(wName+".xml"));
            // delete the  tmp file after closing 
            File f=new File(rName+"-"+"tmp.xml");
            //Major Classification -> CLASS SUBROUTINE FUNCTION
            while((line=fbr.readLine())!=null){
                if(line.contains("class")){
                    fbw.write("<class>\n"+line+"\n");
                    CompileClass();
                }
                else if(line.contains("static") || line.contains("field")){
                    fbw.write("<classVarDec>\n"+line+"\n");
                    CompileclassVarDec();
                }
                else if(line.contains("function") || line.contains("constructor") || line.contains("method")){
                    fbw.write("<subroutineDec>\n"+line+"\n");
                    CompilesubroutineDec();
                }
            }
            //BracketAnalyzer confirms the bracker pair
            fbw.write("<symbol>}</symbol>\n");
            fbw.write("</class>\n");

            fbr.close();
            fbw.close();
            //delete tokenized file
            f.delete();
        }
        catch(IOException e){
            System.out.println(e);
        }

    }

    //COMPILATION METHODS
    static void CompileClass(){
        try {
            fbw.write(fbr.readLine()+"\n");
            fbw.write(fbr.readLine()+"\n");
        } catch (IOException e) {
            System.out.println(e);
        }
    }


    static void CompileclassVarDec(){
        try {
            String word="";
            String line;
            while(!(line=fbr.readLine()).contains("<spToken/>")){
                word+=line+"\n";
            }
            word+=fbr.readLine()+"\n";
            word+="</classVarDec>\n";
            fbw.write(word);
        } catch (IOException e) {
            System.out.println(e);
        }
    }


    static void CompilesubroutineDec(){
        try {
            String word="";
            String line;
            fbw.write(fbr.readLine()+"\n");
            while(!(line=fbr.readLine()).contains("(")){
                word+=line+"\n";
            }
            word+=line+"\n";
            fbw.write(word);
            //Function Args
            CompileparameterList();
        } catch (IOException e) {
            System.out.println(e);
        }
    }


    static void CompileparameterList(){
        try {
            String word="";
            String line;
            fbw.write("<parameterList>\n");
            while(!(line=fbr.readLine()).contains(")")){
                word+=line+"\n";
            }
            fbw.write(word);
            fbw.write("</parameterList>\n"+line+"\n");
            //Function Body
            CompilesubroutineBody();
        } catch (IOException e) {
            System.out.println(e);
        }   
    }


    static void CompilesubroutineBody(){
        try {
            String line;
            fbw.write("<subroutineBody>\n");
            while(!(line=fbr.readLine()).contains("}")){
                if(line.contains("var")){
                    fbw.write("<varDec>\n"+line+"\n");
                    //Handle variable declarations
                    CompilevarDec();
                }
                else if(line.contains("{")){
                    fbw.write(line+"\n");
                }
                else{
                    CompileStatements(line);
                }
            }
            fbw.write(line+"\n"+"</subroutineBody>\n"+"</subroutineDec>\n");
        } catch (IOException e) {
            System.out.println(e);
        }
    }


    static void CompilevarDec(){
        try{
            String line;
            String word="";
            while(!(line=fbr.readLine()).contains(";")){
                if(!line.contentEquals("<spToken/>")){

                word+=line+"\n";

                }
            }
            word+=line+"\n";
            fbw.write(word+"</varDec>\n");
        }
        catch(IOException e){
            System.out.println(e);
        }
    }


    static void CompileStatements(String line){
        //Handle -> IF WHILE LET DO RETURN
        printStatement();
        statementFlag=0;
        try{
            if(line.contains("let")){
                termFlag=0;
                fbw.write("<letStatement>\n");
                fbw.write(line+"\n");
                CompileLet();
            }
            else if(line.contains("if")){
                CompileIf(line);
            }
            else if(line.contains("while")){
                fbw.write("<whileStatement>\n");
                fbw.write(line+"\n");    
                CompileWhile();
            }
            else if(line.contains("do")){
                termFlag=1;
                fbw.write("<doStatement>\n");
                fbw.write(line+"\n");
                CompileDo();
            }
            else if(line.contains("return")){
                fbw.write("<returnStatement>\n");
                fbw.write(line+"\n");
                CompileReturn();
            }
        }
        catch(IOException e){
            System.out.println(e);
        }
    }


    static void CompileLet(){
        try{
            String line;
            String word="";
            while(!(line=fbr.readLine()).contains(";")){
                CompileExp(line);
            }
            word+=line+"\n";
            fbw.write(word+"</letStatement>\n");
        }
        catch(IOException e){
            System.out.println(e);
        }
    }


    static void CompileDo(){
        try{
            rdbrFlag=1;
            String word="";
            String line;
            CompileExpList("");
            //line=fbr.readLine()
            if(!(line=fbr.readLine()).contains("<spToken/>")){
                line=fbr.readLine();
            }
            line=fbr.readLine();
            word+=line+"\n";
            fbw.write(word+"</doStatement>\n");
        }
        catch(IOException e){
            System.out.println(e);
        }
    }


    static void CompileReturn(){
        try{
            String line;
            String word="";
            while(!(line=fbr.readLine()).contains("<spToken/>")){
                word+=line+"\n";
            }
            line=fbr.readLine();
            if(word.isEmpty()){
                fbw.write(line+"\n");
            }
            else{
                fbw.write("<expression>\n<term>\n"+word+"</term>\n</expression>\n");
                fbw.write(line+"\n");
            }
            fbw.write("</returnStatement>\n"+"</statements>\n");
            statementFlag=1;
        }
        catch(IOException e){
            System.out.println(e);
        }
    }


    static void CompileIf(String cur){
        try{
            rdbrFlag=0;
            String line;
            if(cur.contains("if")){
                fbw.write("<ifStatement>\n");
                fbw.write(cur+"\n");
                while(!(line=fbr.readLine()).contains("{")){
                    if(line.contains("(")){
                        CompileExp(line);
                    }
                }
                fbw.write(line+"\n");
                fbw.write("<statements>\n");
                while(!(line=fbr.readLine()).contains("}")){
                    CompileStatements(line);
                }
                fbw.write("</statements>\n"+line+"\n");
                line=fbr.readLine();
                if(line.contains("if")){
                    fbw.write("</ifStatement>\n");
                    CompileIf(line);
                }
                else if(line.contains("else")){
                    CompileIf(line);
                }
                else{
                    fbw.write("</ifStatement>\n");
                    CompileStatements(line);
                }
            }
            else if(cur.contains("else")){
                fbw.write(cur+"\n");
                fbw.write(fbr.readLine()+"\n");
                fbw.write("<statements>\n");
                while(!(line=fbr.readLine()).contains("}")){
                    CompileStatements(line);
                }
                fbw.write("</statements>\n"+line+"\n"+"</ifStatement>\n");
            }
        }
        catch(IOException e){
            System.out.println(e);
        }
    }


    static void CompileWhile(){
        try{
            String line;
            String word="";
            while(!(line=fbr.readLine()).contains("{")){
                if(line.contains("(")){
                    CompileExp(line);
                }
            }
            fbw.write(line+"\n");
            fbw.write("<statements>\n");
            while(!(line=fbr.readLine()).contains("}")){
                CompileStatements(line);
            }
            fbw.write("</statements>\n");
            word+=line+"\n";
            fbw.write(word+"</whileStatement>\n");
        }
        catch(IOException e){
            System.out.println(e);
        }
    }

    

    static void CompileExp(String cur){
        try {
            String line;
            if(cur.contains("=")){
                fbw.write(cur+"\n"+"<expression>\n");
                while(!(line=fbr.readLine()).contains("<spToken/>")){
                    if(line.contains("<identifier>")){
                        fbw.write("<term>\n"+line+"\n");
                        line=fbr.readLine();
                        if(line.contains("[")){
                            fbw.write(line+"\n");
                            fbw.write("<expression>\n");
                            while(!(line=fbr.readLine()).contains("]")){
                                if(line.contains("this")){
                                    fbw.write("<term>\n"+line+"</term>\n");
                                }
                                else{
                                    CompileTerm(line);
                                }
                            }
                            fbw.write("</expression>\n");
                            fbw.write(line+"\n</term>\n");
                        }
                        else if(line.contains(".")){

                            CompileExpList(line);
                        }
                        else{

                            fbw.write("</term>\n");
                            if(line.contains("<spToken/>")){
                                break;
                            }
                            CompileTerm(line);
                        }
                    }
                    else if(line.contains("(")){
                            fbw.write("<term>\n"+line+"\n");
                            line=fbr.readLine();
                            fbw.write("<expression>\n<term>\n"+line+"\n");
                            line=fbr.readLine();
                            CompileTerm(line);
                            fbw.write("</term>\n</expression>\n");
                            line=fbr.readLine();
                            fbw.write(line+"\n"+"</term>\n");
                    }
                    else{
                        CompileTerm(line);
                    }
                }
                fbw.write("</expression>\n");
            }
            
            else if(cur.contains("(")){
                fbw.write(cur+"\n"+"<expression>\n");
                while(!(line=fbr.readLine()).contains(")")){
                    if(line.contains("(")){
                        CompileRdBrTerm(line);
                    }
                    else{
                        CompileTerm(line);
                    }
                }
                fbw.write("</expression>\n"+line+"\n");

            }
            else if(cur.contains("[")){
                fbw.write(cur+"\n<expression>\n");
                while(!(line=fbr.readLine()).contains("]")){
                    CompileTerm(line);
                }
                fbw.write("</expression>\n"+line+"\n");
            }
            else{
                fbw.write(cur+"\n");
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }


    static void CompileTerm(String cur){
        try {
            String line;
            if(cur.contains("integerConstant")|| cur.contains("stringConstant")){
                fbw.write("<term>\n"+cur+"\n</term>\n");
            }
            else if(cur.contains("<identifier>") || cur.contains("<keyword>")){
                fbw.write("<term>\n"+cur+"\n</term>\n");
            }
            else if(cur.contains("~")){
                fbw.write("<term>\n"+cur+"\n");
                if((line=fbr.readLine()).contains("(")){
                    fbw.write("<term>\n");
                    CompileExp(line);
                    fbw.write("</term>\n");
                }
                else{
                    CompileTerm(line);
                }
                fbw.write("</term>\n");
            }
            else{
                fbw.write(cur+"\n");
            }
            
            
        } catch (IOException e) {
            System.out.println(e);
        }
    }


    static void CompileExpList(String cur){
        try{
            String line;
            String word="";
            if(termFlag==0){
                fbw.write(cur+"\n");
            }
            while(!(line=fbr.readLine()).contains("(")){
                word+=line+"\n";
            }
            word+=line+"\n";
            fbw.write(word+"<expressionList>\n");
            while(!(line=fbr.readLine()).contains(")")){
                if(line.contains("(")){
                    fbw.write("<expression>\n");
                    CompileRdBrTerm(line);
                }
                else if(line.contains("symbol")){
                    System.out.println(line);
                    fbw.write(line+"\n");
                }
                else{
                    fbw.write("<expression>\n");
                    CompileTerm(line);
                    line=fbr.readLine();
                    if((line.contains(")"))){
                        fbw.write("</expression>\n");
                        break;
                    }
                    else if(!(line.contains(","))){
                        String tmp;
                        fbw.write(line+"\n");
                        tmp=fbr.readLine();
                        if(tmp.contains("(")){
                            CompileExp(tmp);
                        }
                        else{
                            CompileTerm(tmp);
                        }
                        fbw.write("</expression>\n");

                    }
                    else{

                        fbw.write("</expression>\n");
                        CompileTerm(line);
                    }
            } 
            }
            fbw.write("</expressionList>\n"+line+"\n");
            if(termFlag==0){
                fbw.write("</term>\n");
            }
        }
        catch(IOException e){
            System.out.println(e);
        }
    }


    static void CompileRdBrTerm(String cur){
        String line;
        try {
            fbw.write("<term>\n"+cur+"\n<expression>\n");
        if(rdbrFlag==0){
                while(!(line=fbr.readLine()).contains(")")){
                    if(line.contains("(")){
                        CompileRdBrTerm(line);
                    }
                    else{

                        CompileTerm(line);
                    }
                }
                fbw.write("</expression>\n");
                fbw.write(line+"\n"+"</term>\n");
        }
        
        if(rdbrFlag==1){
            while(!(line=fbr.readLine()).contains(",")){
                if(line.contains("(")){

                    CompileRdBrTerm(line);
                }
                else if(line.contains(")")){
                    fbw.write("</expression>\n"+line+"\n</term>\n");
                }
                else{
                    CompileTerm(line);
                }
            }
            fbw.write("</expression>\n");
            fbw.write(line+"\n");
        } 
    }
        catch (IOException e) {
            System.out.println(e);
        }
    }


//EXTRA UTILITIES
    static void BracketAnalyzer(File fname){
        int curlOpen=0,curlClose=0;
        int roundOpen=0,roundClose=0;
        int sqOpen=0,sqClose=0;
        String line;
        try {
            BufferedReader r=new BufferedReader(new FileReader(fname));
            while((line=r.readLine())!=null){
                if(line.contains("{")){
                    curlOpen++;
                }
                else if(line.contains("}")){
                    curlClose++;
                }
                if(line.contains("[")){
                    sqOpen++;
                }
                else if(line.contains("]")){
                    sqClose++;
                }
                if(line.contains("(")){
                    roundOpen++;
                }
                else if(line.contains(")")){
                    roundClose++;
                }
            }
            if((roundClose==roundOpen)&&(curlClose==curlOpen)&&(sqClose==sqOpen)){
                r.close();
            }
            else{
                r.close();
                throw new Exception("Invalid Parenthesis Count");
            }
            
        } catch (IOException e) {
            System.out.println(e);
        }
        catch(Exception e){
            System.out.println(e);
        }
    }


    static void printStatement(){
        if(statementFlag==1){
            try{
                fbw.write("<statements>\n");
            }
            catch(IOException e){
                System.out.println(e);
            }
        }
    }


    //Get all required files inside a folder
    static ArrayList<File> GetAllFiles(File fname){
        File[] files=fname.listFiles();
        Arrays.sort(files,(a,b)->a.getName().compareTo(b.getName()));
        ArrayList<File> reqfiles=new ArrayList<>();
        if(files!=null){
            for(File i : files){
                if(i.getName().endsWith(".jack")){
                    reqfiles.add(i);
                }
            }
        }
        return reqfiles;
    }
}


