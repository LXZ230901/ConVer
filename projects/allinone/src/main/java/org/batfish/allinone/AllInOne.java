package org.batfish.allinone;

import com.google.common.base.Strings;
import com.uber.jaeger.Configuration;
import com.uber.jaeger.Configuration.ReporterConfiguration;
import com.uber.jaeger.Configuration.SamplerConfiguration;
import com.uber.jaeger.samplers.ConstSampler;
import io.opentracing.util.GlobalTracer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.batfish.allinone.config.Settings;
import org.batfish.client.Client;
import org.batfish.common.BatfishLogger;

public class AllInOne {

  private static String[] getArgArrayFromString(String argString) {
    if (Strings.isNullOrEmpty(argString)) {
      return new String[0];
    }
    return argString.trim().split("\\s+");
  }

  private final String[] _args;

  private Client _client;

  private BatfishLogger _logger;

  private Settings _settings;

  public AllInOne(String[] args) {
    _args = args;
  }

  public void run() {
    try {
      //主要是将输入的参数最终保存在＿settings,后续对其进行操作
      _settings = new Settings(_args);
    } catch (Exception e) {
      System.err.println("org.batfish.allinone: Initialization failed: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }


  String argString =
        String.format(
            "%s -%s %s -%s %s",
            _settings.getClientArgs(),
            org.batfish.client.config.Settings.ARG_LOG_LEVEL,
            _settings.getLogLevel(),
            org.batfish.client.config.Settings.ARG_RUN_MODE,
            _settings.getRunMode());
    System.out.println("input_argus"+argString);
    //argString是输入的参数
    if (_settings.getLogFile() != null) {
      argString +=
          String.format(
              " -%s %s", org.batfish.client.config.Settings.ARG_LOG_FILE, _settings.getLogFile());
    }

    if (_settings.getCommandFile() != null) {
      argString +=
          String.format(
              " -%s %s",
              org.batfish.client.config.Settings.ARG_COMMAND_FILE, _settings.getCommandFile());
    }

    if (_settings.getTestrigDir() != null) {
      argString +=
          String.format(
              " -%s %s",
              org.batfish.client.config.Settings.ARG_TESTRIG_DIR, _settings.getTestrigDir());
    }

    if (_settings.getTracingEnable() && !GlobalTracer.isRegistered()) {
      initTracer();
    }

    argString +=
        String.format(
            " -%s %s",
            org.batfish.client.config.Settings.ARG_TRACING_ENABLE, _settings.getTracingEnable());

    // if we are not running the client, we were like not specified a cmdfile.
    // lets do a dummy cmdfile do client initialization does not barf
    if (!_settings.getRunClient() && _settings.getCommandFile() == null) {
      argString +=
          String.format(
              " -%s %s", org.batfish.client.config.Settings.ARG_COMMAND_FILE, "dummy_allinone");
    }

    String[] initialArgArray = getArgArrayFromString(argString);
    //这一部分还是形成最终的运行参数
    List<String> clientArgs = new ArrayList<>(Arrays.asList(initialArgArray));
    final String[] argArray = clientArgs.toArray(new String[] {});

    System.out.println("－－－－－－－－argArray:－－－－－－－");
    for(int i=0;i<argArray.length;i++)
    {
      System.out.println(argArray[i]);
    }

    try {
      _client = new Client(argArray);//这才是真正客户端运行的函数，里边根据不同的参数选择运行的过程
      //_logger其实就对应着最终的输出，这里的输出是对应命令行的输出
      _logger = _client.getLogger();
      _logger.debugf("Started client with args: %s\n", Arrays.toString(argArray));
    } catch (Exception e) {
      System.err.printf(
          "Client initialization failed with args: %s\nExceptionMessage: %s\n",
          argString, e.getMessage());
      System.exit(1);
    }

    runCoordinator();//这才是真正的接受前端输入的函数,在这个函数当中启动batfishworkmgr的服务

    runBatfish();//其实就相当于启动batfish后端,然后前后端通过url来访问

    if (_settings.getRunClient()) {
      _client.run(new LinkedList<String>());//这才是真正的运行的函数？
      // The program does not terminate without it if the user misses the
      // quit command
      System.exit(0);
    } else {
      // sleep indefinitely, in chunks, since the client does not keep us
      // alive
      try {
        while (true) {
          Thread.sleep(10 * 60 * 1000); // 10 minutes
          _logger.info("allinone: still alive ....\n");
        }
      } catch (Exception ex) {
        String stackTrace = ExceptionUtils.getStackTrace(ex);
        System.err.println(stackTrace);
      }
    }
  }

  private void initTracer() {
    GlobalTracer.register(
        new Configuration(
                _settings.getServiceName(),
                new SamplerConfiguration(ConstSampler.TYPE, 1),
                new ReporterConfiguration(
                    false,
                    _settings.getTracingAgentHost(),
                    _settings.getTracingAgentPort(),
                    /* flush interval in ms */ 1000,
                    /* max buffered Spans */ 10000))
            .getTracer());
  }

  private void runBatfish() {
    //获取ｂａｔｆｉｓｈ需要的参数，然后根据此参数来运行ｂａｔｆｉｓｈ
    String batfishArgs =
        String.format(
            "%s -%s %s -%s %s -%s %s",
            _settings.getBatfishArgs(),
            org.batfish.config.Settings.ARG_RUN_MODE,
            _settings.getBatfishRunMode(),
            org.batfish.config.Settings.ARG_COORDINATOR_REGISTER,
            "true",
            org.batfish.config.Settings.ARG_TRACING_ENABLE,
            _settings.getTracingEnable());
    System.out.println("-----------BATfishArgs:------------");
    System.out.println(batfishArgs);
    System.out.println("-----------End-------------");
    String[] initialArgArray = getArgArrayFromString(batfishArgs);
    List<String> args = new ArrayList<>(Arrays.asList(initialArgArray));
    final String[] argArray = args.toArray(new String[] {});
    _logger.debugf("Starting batfish worker with args: %s\n", Arrays.toString(argArray));

    //这里应该就是指创建ｂａｔｆｉｓｈ线程
    Thread thread =
        new Thread("batfishThread") {
          @Override
          public void run() {
            try {
              //以这个参数来运行ｂａｔｆｉｓｈ
              org.batfish.main.Driver.main(argArray, _logger);
            } catch (Exception e) {
              _logger.errorf(
                  "Initialization of batfish failed with args: %s\nExceptionMessage: %s\n",
                  Arrays.toString(argArray), e.getMessage());
            }
          }
        };
    //开始这个线程
    thread.start();
  }

  private void runCoordinator() {
    String coordinatorArgs =
        String.format(
            "%s -%s %s",
            _settings.getCoordinatorArgs(),
            org.batfish.coordinator.config.Settings.ARG_TRACING_ENABLE,
            _settings.getTracingEnable());
    String[] initialArgArray = getArgArrayFromString(coordinatorArgs);

    System.out.println("－－－－－－－－－－runCoordinator_argus:－－－－－－－－－－");
    for(int i=0;i<initialArgArray.length;i++)
    {
      System.out.println(initialArgArray[i]);
    }

    List<String> args = new ArrayList<>(Arrays.asList(initialArgArray));
    final String[] argArray = args.toArray(new String[] {});
    _logger.debugf("Starting coordinator with args: %s\n", Arrays.toString(argArray));

    Thread thread =
        new Thread("coordinatorThread") {
          @Override
          public void run() {
            try {
              org.batfish.coordinator.Main.main(argArray, _logger);//创建线程启动服务
            } catch (Exception e) {
              _logger.errorf(
                  "Initialization of coordinator failed with args: %s\nExceptionMessage: %s\n",
                  Arrays.toString(argArray), e.getMessage());
            }
          }
        };

    thread.start();
  }
}
