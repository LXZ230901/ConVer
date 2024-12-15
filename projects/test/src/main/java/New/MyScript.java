package New;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MyScript {
    public static void main(String[] args) {
        // 在这里编写您的脚本代码
        // 执行特定的 Java 命令
        executeJavaCommand();
    }

    private static void executeJavaCommand() {
        try {
            // 使用 Java Runtime 类执行特定的 Java 命令
            List<String> commands=new ArrayList<>();
            commands.add("initCommands");
            commands.add("execCommandsFailures0");
            commands.add("execCommandsFailures1");
            commands.add("execCommandsFailures2");
            commands.add("execCommandsFailures3");
            for(int i=0;i<5;i++)
            {
                String execCommand=commands.get(i);
                LocalDateTime startTime = LocalDateTime.now();
                String command = "java  -jar projects/allinone/target/allinone-bundle-0.36.0.jar -runmode batch -cmdfile "+execCommand;
                Process process = Runtime.getRuntime().exec(command);

                // 可选：处理命令执行的输出、错误流
                // 例如，读取命令执行的输出信息
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                // 等待命令执行完成
                int exitCode = process.waitFor();
                LocalDateTime endTime = LocalDateTime.now();
                Duration duration = Duration.between(startTime, endTime);
                long execTimes=duration.toMillis();

                Path timePath = Paths.get("execTime-"+execCommand);
                Path outputPath= Paths.get("execAnswer-"+execCommand);

                String execTimeContent="startTime:"+startTime+"; endTime:"+endTime+";\n subTime:"+execTimes;

                try {
                    Files.write(timePath, execTimeContent.getBytes(), StandardOpenOption.CREATE);
                    Files.write(outputPath, output.toString().getBytes(), StandardOpenOption.CREATE);
                    System.out.println(execCommand+"-文件写入成功。");
                } catch (IOException e) {
                    System.out.println(execCommand+"-写入文件时出现错误: " + e.getMessage());
                }
                System.out.println(execCommand+"-命令执行完成，退出码：" + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}