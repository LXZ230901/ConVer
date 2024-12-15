package New;

//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//
//public class MathEXP {
//    public static int[][] _visitDirection={{0,1},{1,0},{1,1},{0,-1},{-1,0},{-1,-1},{1,-1},{-1,1},{2,1},{-2,-1},{-2,1},{2,-1},{1,2},{-1,-2},{1,-2},{-1,2}};
//    public static class AnswerNumber{
//        int _answerNumber;
//        public AnswerNumber(int answerNumber)
//        {
//            _answerNumber=answerNumber;
//        }
//        public int getAnswerNumber()
//        {
//            return _answerNumber;
//        }
//        public void setAnswerNumber(int answerNumber)
//        {
//            _answerNumber=answerNumber;
//        }
//        public void addAnswerNumber()
//        {
//            _answerNumber+=1;
//        }
//    }
//    public static void ergodic(int xIndex,int yIndex,AnswerNumber answerNumber,int[][] visitSingal,int xDirection,int yDirection,int visitedNodeNumber)
//    {
//        int actualXIndex=xIndex+xDirection;
//        int actualYIndex=yIndex+yDirection;
//        if(actualXIndex<3&&actualXIndex>=0
//            &&actualYIndex<3&&actualYIndex>=0)
//        {
//            if(visitSingal[actualXIndex][actualYIndex]==0)
//            {
//                visitSingal[actualXIndex][actualYIndex]=1;
//                visitedNodeNumber++;
//                if(visitedNodeNumber>=4)
//                {
//                    answerNumber.addAnswerNumber();
//                }
//                for(int i=0;i<_visitDirection.length;i++)
//                {
//                    ergodic(actualXIndex,actualYIndex,answerNumber,visitSingal,_visitDirection[i][0],_visitDirection[i][1],visitedNodeNumber);
//                }
//                visitSingal[actualXIndex][actualYIndex]=0;
//            }else{
//                ergodic(xIndex,yIndex,answerNumber,visitSingal,xDirection*2,yDirection*2,visitedNodeNumber);
//            }
//        }
//    }
//    public static void main(String[] args)
//    {
//        Path path= Paths.get("execCommandsFailures0");
//        StringBuilder commands=new StringBuilder();
//        for(int i=0;i<=24;i++)
//        {
//            commands.append("get smt-reachability finalNodeRegex=\"core-"+i+"\",dstIps=[\"70.0."+i+".0/24\"],failures=0").append("\n");
//        }
//        for(int i=2;i<=11;i++)
//        {
//            for(int j=5;j<=9;j++)
//            {
//                int num=i*10+j;
//                commands.append("get smt-reachability finalNodeRegex=\"aggregation-"+num+"\",dstIps=[\"70.0."+num+".0/24\"],failures=0").append("\n");
//            }
//        }
//        for(int i=2;i<=11;i++)
//        {
//            for(int j=0;j<=4;j++)
//            {
//                int num=i*10+j;
//                commands.append("get smt-reachability finalNodeRegex=\"edge-"+num+"\",dstIps=[\"70.0."+num+".0/24\"],failures=0").append("\n");
//            }
//        }
//        System.out.println(commands);
//        try {
//            Files.write(path, commands.toString().getBytes(), StandardOpenOption.CREATE);
//        }catch (IOException e)
//        {
//            System.out.println("error");
//        }
//    }
//}
