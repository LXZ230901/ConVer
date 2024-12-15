package New;


import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.batfish.common.Warning;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.vendor.VendorConfiguration;
import New.Configs.*;
public class Driver {
//  public static void main(String[] args)
//  {
//    Logger logger = new Logger(Logger.Level.DEBUG);
//    Settings settings=null;
//    if(args.length>0)
//    {
//        settings = new Settings(args[0]);
//    }
//    String VendorPath=settings.getConfigPath();
//    ConfigurationParser parser = new ConfigurationParser(logger,
//        settings.getConfigPath(), false);
//    long startTime = System.currentTimeMillis();
//    Map<String, VendorConfiguration> vendorConfigurations = parser.parse();
//    System.out.println("-----------------------Begin:----------------------");
//    System.out.println(vendorConfigurations.toString());
//    System.out.println("------------------------End------------------------");
//
//    Path outputPath= Paths.get("home/uptech/batfish-ab");
//    CommonUtil.createDirectories(outputPath);
//    Map<Path, VendorConfiguration> output = new TreeMap<>();
//    vendorConfigurations.forEach(
//        (name, vc) -> {
//          if (name.contains(File.separator)) {
//            System.out.println("iptable");
//          } else {
//
//            Path currentOutputPath = outputPath.resolve(name);
//            output.put(currentOutputPath, vc);
//          }
//        });
//  }
}
