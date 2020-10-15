/*
SP -> 0
LCL -> 1
ARG -> 2
THIS -> 3
THAT -> 4
temp -> 5 to 12 
general purpose -> 13 to 15
static -> 16 to 255 stored as foo.i
pointer ->  |-> 0 for THIS 
            |-> 1 for THAT
STACK -> 256+ 


*addr=seg+i
@i
D=A
@seg
A=M
D=D+M
A=D
D=M


*SP=*addr
A=M
M=D

SP++
@SP
M=M+1
*/
import java.io.*;
import java.util.*;

class VMTranslator{
    //label counter
    static String lc="0";
    //function label counters
    static String funcCount="0";
    //fileCount to keep track of the static variables for each file
    static String fileCount="-1";
    //To store all files of a folder
    static ArrayList<File> files=new ArrayList<>();
    public static void main(String args[]){
//-----------------------------CHECK FOR FILE OR DIRECTORY FOR NAVIGATION------------------------------------------//
        try{
            //for single files
            if(args[0].endsWith(".vm")){
                String name=args[0].substring(0,args[0].indexOf("."));
                File resultFile=new File(name+".asm");
                //if it exists delete the file to avoid appending
                if(resultFile.exists()){
                    resultFile.delete();
                }
                BufferedWriter fbw=new BufferedWriter(new FileWriter(name+".asm"));
                BufferedReader fbr=new BufferedReader(new FileReader(args[0]));
                CodeWriter(fbr,name,fbw);
            }
            else{
                //for folders
                String name=args[0];
                //split it to get the folder name and set location
                String[] folder=name.split("\\\\");
                File fname=new File(args[0]);
                File resultFile=new File(name+"//"+folder[folder.length-1]+".asm");
                files=getVMFiles(fname);
                //if it exists delete it to avoid appending 
                if(resultFile.exists()){
                    resultFile.delete();
                }
                for (File i: files){
                    fileCount=increment(fileCount);
                    BufferedReader fbr=new BufferedReader(new FileReader((i)));
                    BufferedWriter fbw=new BufferedWriter(new FileWriter(name+"//"+folder[folder.length-1]+".asm",true));
                    fbw.write("@261\nD=A\n@0\nM=D\n@Sys.init\n0;JMP\n");
                    CodeWriter(fbr,name,fbw);
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



//-------------------------------------------------CODEWRITER CHECKS FOR KEYWORDS-----------------------//
    static void  CodeWriter(BufferedReader fbr,String name,BufferedWriter fbw){
        try{
            String line;
            while((line=fbr.readLine())!=null){
                line=line.trim();
                if(!line.matches("\n*")&&!line.matches("//.*")){
                    //Ignore comments
                    if(line.contains("//")){
                        line=line.substring(0,line.indexOf("/"));
                    }
                    if(line.contains("push")){
                        String[] parts=line.split(" ");
                        fbw.write(Pushcmd(parts));
                    }
                    else if(line.contains("pop")){
                        String[] parts=line.split(" ");
                        fbw.write(Popcmd(parts));
                    }
                    else if(line.contains("label")){
                        String[] parts=line.split(" ");
                        fbw.write("("+parts[1]+")\n");
                    }
                    //if-goto must be checked first as both contains will be considered for the only goto case
                    else if(line.contains("if")){
                        String[] parts=line.split(" ");
                        fbw.write(stackDec()+stackPtrOp()+"@"+parts[1]+"\nD;JNE\n");
                    }
                    else if(line.contains("goto")){
                        String[] parts=line.split(" ");
                        fbw.write("@"+parts[1]+"\n0;JMP\n");
                    }
                    else if(line.contains("function")){
                        funcCount=increment(funcCount);
                        String[] parts=line.split(" ");
                        fbw.write("("+parts[1]+")\n");
                        fbw.write("@"+parts[2]+"\nD=A\n(LP."+funcCount+")\n");
                        fbw.write("@NL"+funcCount+"\nD;JEQ\n@SP\nA=M\nM=0\n");
                        fbw.write(stackInc()+"D=D-1\n@LP."+funcCount+"\nD;JNE\n");
                        fbw.write("(NL"+funcCount+")\n");
                    }
                    else if(line.contains("return")){
                        fbw.write(funcReturn());
                    }
                    else if(line.contains("call")){
                        String[] parts=line.split(" ");
                        fbw.write(functionCall(parts[1], parts[2]));
                    }
                    else{
                        fbw.write(Opcmd(line.trim()));
                        
                    }
                }
            }
            fbr.close();
            fbw.close();
        }
        catch(IOException e){
            System.out.print(e);
        }
    }



    
//-------------------------------------------------------------PUSH COMMANDS---------------------------------------//
    static String Pushcmd(String[] parts){
        String line="";
        switch(parts[1]){
            case "local":
                line+=ptr(parts[2]);
                line+="@LCL\n";
                line+=setAddr();
                line+=stackPtrMem();
                break;
            case "argument":
                line+=ptr(parts[2]);
                line+="@ARG\n";
                line+=setAddr();
                line+=stackPtrMem();
                break;
            case "this":
                line+=ptr(parts[2]);
                line+="@THIS\n";
                line+=setAddr();
                line+=stackPtrMem();
                break;
            case "that":
                line+=ptr(parts[2]);
                line+="@THAT\n";
                line+=setAddr();
                line+=stackPtrMem();
                break;
            case "constant":
                line+=ptr(parts[2]);
                line+=stackPtrMem();
                break;
            case "temp":
                line+=ptr(parts[2]);
                line+="@5\nD=D+A\nA=D\nD=M\n";
                line+=stackPtrMem();
                break;
            case "static":
                line+="@var."+files.get(Integer.parseInt(fileCount)).getName()+"_"+parts[2]+"\nD=M\n";
                line+=stackPtrMem();
                break;
            case "pointer":
                if(parts[2].contains("0")){
                    line+="@3\nD=M\n";
                }
                else{
                    line+="@4\nD=M\n";
                }
                line+=stackPtrMem();
        }
        line+=stackInc();
        return line;
    }


//--------------------------------------------POP COMMANDS------------------------------------------------//
    static String Popcmd(String[] parts){
        String line="";
        switch(parts[1]){
            case "constant":
                line+=stackDec();
                return line;
            case "local":
                line+=ptr(parts[2]);
                line+="@LCL\n";
                break;
            case "argument":
                line+=ptr(parts[2]);
                line+="@ARG\n";
                break;
            case "this":
                line+=ptr(parts[2]);
                line+="@THIS\n";
                break;
            case "that":
                line+=ptr(parts[2]);
                line+="@THAT\n";
                break;
            case "temp":
                line+=ptr(parts[2]);
                line+="@5\nD=D+A\n@13\nM=D\n";
                line+=stackDec();
                line+=stackPtrOp();
                line+="@13\nA=M\nM=D\n";
                return line;
            case "static":
            //Critical point -check out if required
                line+=stackDec();
                line+=stackPtrOp();
                line+="@var."+files.get(Integer.parseInt(fileCount)).getName()+"_"+parts[2]+"\nM=D\n";
                return line;
            case "pointer":
                line+=stackDec();
                line+=stackPtrOp();
                if(parts[2].contains("0")){
                    line+="@3\nM=D\n";
                }
                else{
                    line+="@4\nM=D\n";
                }
                return line;
        }
        line+=extReg();
        line+=stackDec();
        line+=stackPtrOp();
        line+="@R13\nA=M\nM=D\n";
        return line;
    }



//-----------------------------------------------OPCODES COMMANDS---------------------------------------//
    static String Opcmd(String op){
        String line="";
        line+=stackDec();
        line+=stackPtrOp();
        if(op.contains("not")){
            line+="D=!D\nM=D\n";
            line+=stackInc();
            return line;
        }
        if(op.contains("neg")){
            line+="D=-D\nM=D\n";
            line+=stackInc();
            return line;
        }
        line+=stackDec();
        switch(op){
            case "add":
                line+="A=M\nD=D+M\nM=D\n";

                break;
            case "sub":
                line+="A=M\nD=M-D\nM=D\n";

                break;
            case "gt":
                line+=loopCmd1();
                line+="D;JGE\n";
                line+=loopCmd2();
                return line;
            case "lt":
                line+=loopCmd1();
                line+="D;JLE\n";
                line+=loopCmd2();
                return line;
            case "eq":
                line+=loopCmd1();
                line+="D;JNE\n";
                line+=loopCmd2();
                return line;
            case "and":
                line+="A=M\nD=D&M\nM=D\n";
                line+=stackPtrMem();
                break;
            case "or":
                line+="A=M\nD=D|M\nM=D\n";
                line+=stackPtrMem();
                break;
        }
        line+=stackInc();
        return line;
    }



//---------------------------SPECIAL STRINGS TO AVOID REPETITION------------------------------------------//
    static String setAddr(){
        return "A=M\nD=D+A\nA=D\nD=M\n";
    }
    static String stackPtrOp(){
        return "A=M\nD=M\n";
    }
    static String stackPtrMem(){
        return "@SP\nA=M\nM=D\n";
    }
    static String stackInc(){
        return "@SP\nM=M+1\n";
    }
    static String stackDec(){
        return "@SP\nM=M-1\n";
    }
    static String loopCmd1(){
        return "A=M\nD=D-M\n@LOOP"+lc+"\n";
    }
    static String loopCmd2(){
        String line="@SP\nA=M\nM=-1\n";
        String tmp;
        tmp =increment(lc);
        line+=spLoop(tmp);
        line+="(LOOP"+lc+")\n@SP\nA=M\nM=0\n";
        line+=spLoop(tmp);
        line+=loopCmd3(tmp);
        lc=increment(lc);
        lc=increment(lc);
        return line;
    }
    static String spLoop(String tmp){
        return "@LOOP"+tmp+"\n0;JMP\n";
    }
    static String loopCmd3(String tmp){
        return "(LOOP"+tmp+")\n@SP\nM=M+1\n";
    }
    static String increment(String lc){
        int x=Integer.parseInt(lc);
        x++;
        String tc=Integer.toString(x);
        return tc;
    }
    static String extReg(){
        return "A=M\nD=D+A\n@R13\nM=D\n";
    }
    static String ptr(String i){
        return "@"+i+"\nD=A\n";
    }


//----------------------------------SETUP FOR DIRECTOORY-------------------------------------------//
    static ArrayList<File> getVMFiles(File folder){
        File[] files=folder.listFiles();
        Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
        ArrayList<File> reqFiles=new ArrayList<>();
        if(files!=null){
            for(File i:files){
                if(i.getName().endsWith(".vm")){
                    reqFiles.add(i);
                }
            }
        }
        return reqFiles;
    }


//--------------------------------------FUNCTION BASED COMMANDS-------------------------------------//

    static String functionCall(String funcName,String funcArgs){
        funcCount=increment(funcCount);
        String line="";
        //Store Return Address in a variable
        line+="@"+"Ret.addr"+funcCount+"\nD=A\n";
        line+=stackPtrMem();
        line+=stackInc();


        //Store the original values of LCL/ARG/THIS/THAT
        line+="@LCL\nD=M\n"+stackPtrMem()+stackInc();
        line+="@ARG\nD=M\n"+stackPtrMem()+stackInc();
        line+="@THIS\nD=M\n"+stackPtrMem()+stackInc();
        line+="@THAT\nD=M\n"+stackPtrMem()+stackInc();


        //Set ARG=SP-5-argCount
        line+="@SP\nD=M\n@5\nD=D-A\n";
        line+="@"+funcArgs+"\nD=D-A\n@ARG\nM=D\n";


        //Set LCL=SP
        line+="@SP\nD=M\n@LCL\nM=D\n";


        //Add Label for JMP
        line+="@"+funcName+"\n0;JMP\n(Ret.addr"+funcCount+")\n";
        return line;
    }
    static String funcReturn(){
        String line="";
        //FRAME=LCL
        line+="@LCL\nD=M\n@R15\nM=D\n";

        //retAddr=*(FRAME-5)
        line+="@5\nD=A\n@R15\nA=M-D\nD=M\n@R14\nM=D\n";

        //*ARG=pop()
        line+=stackDec()+stackPtrOp();
        line+="@ARG\nA=M\nM=D\n";

        //SP=ARG+1
        line+="@ARG\nD=M+1\n@SP\nM=D\n";

        //SEGMENT=*(FRAME-x)   (temp R15 is used as FRAME)
        line+="@1\nD=A\n@R15\nA=M-D\nD=M\n@THAT\nM=D\n";
        line+="@2\nD=A\n@R15\nA=M-D\nD=M\n@THIS\nM=D\n";
        line+="@3\nD=A\n@R15\nA=M-D\nD=M\n@ARG\nM=D\n";
        line+="@4\nD=A\n@R15\nA=M-D\nD=M\n@LCL\nM=D\n";


        line+="@R14\nA=M\n0;JMP\n";
        return line;
    }
}