package org.batfish.main;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Verify;
import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishException.BatfishStackTrace;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.CleanBatfishException;
import org.batfish.common.CoordConsts;
import org.batfish.common.Directory;
import org.batfish.common.Pair;
import org.batfish.common.Snapshot;
import org.batfish.common.Version;
import org.batfish.common.Warning;
import org.batfish.common.Warnings;
import org.batfish.common.plugin.BgpTablePlugin;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.common.plugin.DataPlanePlugin.ComputeDataPlaneResult;
import org.batfish.common.plugin.DataPlanePluginSettings;
import org.batfish.common.plugin.ExternalBgpAdvertisementPlugin;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.PluginClientType;
import org.batfish.common.plugin.PluginConsumer;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.config.Settings.EnvironmentSettings;
import org.batfish.config.Settings.TestrigSettings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.DeviceType;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.ForwardingAction;
import org.batfish.datamodel.ForwardingAnalysis;
import org.batfish.datamodel.ForwardingAnalysisImpl;
import org.batfish.datamodel.GenericConfigObject;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpsecVpn;
import org.batfish.datamodel.IsisLevel;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.RipNeighbor;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AclLinesAnswerElement;
import org.batfish.datamodel.answers.AclLinesAnswerElement.AclReachabilityEntry;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.AnswerStatus;
import org.batfish.datamodel.answers.AnswerSummary;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.DataPlaneAnswerElement;
import org.batfish.datamodel.answers.FlattenVendorConfigurationAnswerElement;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.answers.InitStepAnswerElement;
import org.batfish.datamodel.answers.NodAnswerElement;
import org.batfish.datamodel.answers.NodFirstUnsatAnswerElement;
import org.batfish.datamodel.answers.NodSatAnswerElement;
import org.batfish.datamodel.answers.ParseAnswerElement;
import org.batfish.datamodel.answers.ParseEnvironmentBgpTablesAnswerElement;
import org.batfish.datamodel.answers.ParseEnvironmentRoutingTablesAnswerElement;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.answers.ReportAnswerElement;
import org.batfish.datamodel.answers.RunAnalysisAnswerElement;
import org.batfish.datamodel.answers.ValidateEnvironmentAnswerElement;
import org.batfish.datamodel.assertion.AssertionAst;
import org.batfish.datamodel.collections.BgpAdvertisementsByVrf;
import org.batfish.datamodel.collections.MultiSet;
import org.batfish.datamodel.collections.NamedStructureEquivalenceSet;
import org.batfish.datamodel.collections.NamedStructureEquivalenceSets;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.collections.RoutesByVrf;
import org.batfish.datamodel.collections.TreeMultiSet;
import org.batfish.datamodel.pojo.Environment;
import org.batfish.datamodel.questions.InterfacesSpecifier;
import org.batfish.datamodel.questions.InvalidReachabilitySettingsException;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.ReachabilitySettings;
import org.batfish.datamodel.questions.smt.HeaderLocationQuestion;
import org.batfish.datamodel.questions.smt.HeaderQuestion;
import org.batfish.datamodel.questions.smt.RoleQuestion;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BgpTableFormat;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.ParseTreePrettyPrinter;
import org.batfish.grammar.assertion.AssertionCombinedParser;
import org.batfish.grammar.assertion.AssertionExtractor;
import org.batfish.grammar.assertion.AssertionParser.AssertionContext;
import org.batfish.grammar.juniper.JuniperCombinedParser;
import org.batfish.grammar.juniper.JuniperFlattener;
import org.batfish.grammar.topology.GNS3TopologyCombinedParser;
import org.batfish.grammar.topology.GNS3TopologyExtractor;
import org.batfish.grammar.topology.TopologyExtractor;
import org.batfish.grammar.vyos.VyosCombinedParser;
import org.batfish.grammar.vyos.VyosFlattener;
import org.batfish.job.BatfishJobExecutor;
import org.batfish.job.ConvertConfigurationJob;
import org.batfish.job.FlattenVendorConfigurationJob;
import org.batfish.job.ParseEnvironmentBgpTableJob;
import org.batfish.job.ParseEnvironmentRoutingTableJob;
import org.batfish.job.ParseVendorConfigurationJob;
import org.batfish.representation.aws.AwsConfiguration;
import org.batfish.representation.host.HostConfiguration;
import org.batfish.representation.iptables.IptablesVendorConfiguration;
import org.batfish.role.InferRoles;
import org.batfish.role.NodeRoleDimension;
import org.batfish.role.NodeRolesData;
import org.batfish.symbolic.abstraction.BatfishCompressor;
import org.batfish.symbolic.abstraction.Roles;
import org.batfish.symbolic.smt.PropertyChecker;
import org.batfish.symwork.SymBgpRoute;
import org.batfish.symwork.SymBounder;
import org.batfish.symwork.SymCommunity;
import org.batfish.symwork.SymLocalPreference;
import org.batfish.vendor.VendorConfiguration;
import org.batfish.z3.AclLine;
import org.batfish.z3.AclLineIndependentSatisfiabilityQuerySynthesizer;
import org.batfish.z3.AclReachabilityQuerySynthesizer;
import org.batfish.z3.BlacklistDstIpQuerySynthesizer;
import org.batfish.z3.CompositeNodJob;
import org.batfish.z3.EarliestMoreGeneralReachableLineQuerySynthesizer;
import org.batfish.z3.IngressPoint;
import org.batfish.z3.MultipathInconsistencyQuerySynthesizer;
import org.batfish.z3.NodFirstUnsatJob;
import org.batfish.z3.NodJob;
import org.batfish.z3.NodSatJob;
import org.batfish.z3.QuerySynthesizer;
import org.batfish.z3.ReachEdgeQuerySynthesizer;
import org.batfish.z3.ReachabilityQuerySynthesizer;
import org.batfish.z3.StandardReachabilityQuerySynthesizer;
import org.batfish.z3.Synthesizer;
import org.batfish.z3.SynthesizerInputImpl;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/** This class encapsulates the main control logic for Batfish. */
public class Batfish extends PluginConsumer implements IBatfish {

  private static final String BASE_TESTRIG_TAG = "BASE";

  private static final String DELTA_TESTRIG_TAG = "DELTA";

  private static final String DIFFERENTIAL_FLOW_TAG = "DIFFERENTIAL";

  /** The name of the [optional] topology file within a test-rig */
  private static final String TOPOLOGY_FILENAME = "topology.net";

  public static void applyBaseDir(
      TestrigSettings settings, Path containerDir, String testrig, String envName) {
    Path testrigDir = containerDir.resolve(Paths.get(BfConsts.RELPATH_TESTRIGS_DIR, testrig));
    settings.setName(testrig);
    settings.setBasePath(testrigDir);
    EnvironmentSettings envSettings = settings.getEnvironmentSettings();
    settings.setSerializeVendorPath(
        testrigDir.resolve(BfConsts.RELPATH_VENDOR_SPECIFIC_CONFIG_DIR));
    settings.setTestRigPath(testrigDir.resolve(BfConsts.RELPATH_TEST_RIG_DIR));
    settings.setParseAnswerPath(testrigDir.resolve(BfConsts.RELPATH_PARSE_ANSWER_PATH));
    settings.setNodeRolesPath(
        testrigDir.resolve(
            Paths.get(BfConsts.RELPATH_TEST_RIG_DIR, BfConsts.RELPATH_NODE_ROLES_PATH)));
    settings.setInferredNodeRolesPath(
        testrigDir.resolve(
            Paths.get(BfConsts.RELPATH_TEST_RIG_DIR, BfConsts.RELPATH_INFERRED_NODE_ROLES_PATH)));
    settings.setTopologyPath(testrigDir.resolve(BfConsts.RELPATH_TESTRIG_TOPOLOGY_PATH));
    settings.setPojoTopologyPath(testrigDir.resolve(BfConsts.RELPATH_TESTRIG_POJO_TOPOLOGY_PATH));
    if (envName != null) {
      envSettings.setName(envName);
      Path envPath = testrigDir.resolve(BfConsts.RELPATH_ENVIRONMENTS_DIR).resolve(envName);
      envSettings.setEnvironmentBasePath(envPath);
      envSettings.setCompressedDataPlanePath(
          envPath.resolve(BfConsts.RELPATH_COMPRESSED_DATA_PLANE));
      envSettings.setCompressedDataPlaneAnswerPath(
          envPath.resolve(BfConsts.RELPATH_COMPRESSED_DATA_PLANE_ANSWER));
      envSettings.setDataPlanePath(envPath.resolve(BfConsts.RELPATH_DATA_PLANE));
      envSettings.setDataPlaneAnswerPath(envPath.resolve(BfConsts.RELPATH_DATA_PLANE_ANSWER_PATH));
      envSettings.setParseEnvironmentBgpTablesAnswerPath(
          envPath.resolve(BfConsts.RELPATH_ENVIRONMENT_BGP_TABLES_ANSWER));
      envSettings.setParseEnvironmentRoutingTablesAnswerPath(
          envPath.resolve(BfConsts.RELPATH_ENVIRONMENT_ROUTING_TABLES_ANSWER));
      envSettings.setSerializeEnvironmentBgpTablesPath(
          envPath.resolve(BfConsts.RELPATH_SERIALIZED_ENVIRONMENT_BGP_TABLES));
      envSettings.setSerializeEnvironmentRoutingTablesPath(
          envPath.resolve(BfConsts.RELPATH_SERIALIZED_ENVIRONMENT_ROUTING_TABLES));
      envSettings.setValidateEnvironmentAnswerPath(
          envPath.resolve(BfConsts.RELPATH_VALIDATE_ENVIRONMENT_ANSWER));
      Path envDirPath = envPath.resolve(BfConsts.RELPATH_ENV_DIR);
      envSettings.setEnvPath(envDirPath);
      envSettings.setNodeBlacklistPath(envDirPath.resolve(BfConsts.RELPATH_NODE_BLACKLIST_FILE));
      envSettings.setInterfaceBlacklistPath(
          envDirPath.resolve(BfConsts.RELPATH_INTERFACE_BLACKLIST_FILE));
      envSettings.setEdgeBlacklistPath(envDirPath.resolve(BfConsts.RELPATH_EDGE_BLACKLIST_FILE));
      envSettings.setSerializedTopologyPath(envDirPath.resolve(BfConsts.RELPATH_ENV_TOPOLOGY_FILE));
      envSettings.setDeltaConfigurationsDir(
          envDirPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR));
      envSettings.setExternalBgpAnnouncementsPath(
          envDirPath.resolve(BfConsts.RELPATH_EXTERNAL_BGP_ANNOUNCEMENTS));
      envSettings.setEnvironmentBgpTablesPath(
          envDirPath.resolve(BfConsts.RELPATH_ENVIRONMENT_BGP_TABLES));
      envSettings.setEnvironmentRoutingTablesPath(
          envDirPath.resolve(BfConsts.RELPATH_ENVIRONMENT_ROUTING_TABLES));
      envSettings.setPrecomputedRoutesPath(envPath.resolve(BfConsts.RELPATH_PRECOMPUTED_ROUTES));
      envSettings.setDeltaCompiledConfigurationsDir(
          envPath.resolve(BfConsts.RELPATH_VENDOR_INDEPENDENT_CONFIG_DIR));
      envSettings.setDeltaVendorConfigurationsDir(
          envPath.resolve(BfConsts.RELPATH_VENDOR_SPECIFIC_CONFIG_DIR));
    }
  }

  static void checkTopology(Map<String, Configuration> configurations, Topology topology) {
    for (Edge edge : topology.getEdges()) {
      if (!configurations.containsKey(edge.getNode1())) {
        throw new BatfishException(
            String.format("Topology contains a non-existent node '%s'", edge.getNode1()));
      }
      if (!configurations.containsKey(edge.getNode2())) {
        throw new BatfishException(
            String.format("Topology contains a non-existent node '%s'", edge.getNode2()));
      }
      // nodes are valid, now checking corresponding interfaces
      Configuration config1 = configurations.get(edge.getNode1());
      Configuration config2 = configurations.get(edge.getNode2());
      if (!config1.getInterfaces().containsKey(edge.getInt1())) {
        throw new BatfishException(
            String.format(
                "Topology contains a non-existent interface '%s' on node '%s'",
                edge.getInt1(), edge.getNode1()));
      }
      if (!config2.getInterfaces().containsKey(edge.getInt2())) {
        throw new BatfishException(
            String.format(
                "Topology contains a non-existent interface '%s' on node '%s'",
                edge.getInt2(), edge.getNode2()));
      }
    }
  }

  public static String flatten(
      String input,
      BatfishLogger logger,
      Settings settings,
      ConfigurationFormat format,
      String header) {
    switch (format) {
      case JUNIPER:
        {
          JuniperCombinedParser parser = new JuniperCombinedParser(input, settings);
          ParserRuleContext tree = parse(parser, logger, settings);
          JuniperFlattener flattener = new JuniperFlattener(header);
          ParseTreeWalker walker = new ParseTreeWalker();
          walker.walk(flattener, tree);
          return flattener.getFlattenedConfigurationText();
        }

      case VYOS:
        {
          VyosCombinedParser parser = new VyosCombinedParser(input, settings);
          ParserRuleContext tree = parse(parser, logger, settings);
          VyosFlattener flattener = new VyosFlattener(header);
          ParseTreeWalker walker = new ParseTreeWalker();
          walker.walk(flattener, tree);
          return flattener.getFlattenedConfigurationText();
        }

        // $CASES-OMITTED$
      default:
        throw new BatfishException("Invalid format for flattening");
    }
  }

  public static void initQuestionSettings(Settings settings) {
    String questionName = settings.getQuestionName();
    Path containerDir = settings.getContainerDir();
    if (questionName != null) {
      Path questionPath =
          containerDir.resolve(BfConsts.RELPATH_QUESTIONS_DIR).resolve(questionName);
      settings.setQuestionPath(questionPath.resolve(BfConsts.RELPATH_QUESTION_FILE));
    }
  }

  public static void initTestrigSettings(Settings settings) {
    String testrig = settings.getTestrig();
    String envName = settings.getEnvironmentName();
    Path containerDir = settings.getContainerDir();
    if (testrig != null) {
      applyBaseDir(settings.getBaseTestrigSettings(), containerDir, testrig, envName);
      String deltaTestrig = settings.getDeltaTestrig();
      String deltaEnvName = settings.getDeltaEnvironmentName();
      TestrigSettings deltaTestrigSettings = settings.getDeltaTestrigSettings();
      if (deltaTestrig != null && deltaEnvName == null) {
        deltaEnvName = envName;
        settings.setDeltaEnvironmentName(envName);
      } else if (deltaTestrig == null && deltaEnvName != null) {
        deltaTestrig = testrig;
        settings.setDeltaTestrig(testrig);
      }
      if (deltaTestrig != null) {
        applyBaseDir(deltaTestrigSettings, containerDir, deltaTestrig, deltaEnvName);
      }
      if (settings.getDiffActive()) {
        settings.setActiveTestrigSettings(settings.getDeltaTestrigSettings());
      } else {
        settings.setActiveTestrigSettings(settings.getBaseTestrigSettings());
      }
      initQuestionSettings(settings);
    } else if (containerDir != null) {
      throw new CleanBatfishException("Must supply argument to -" + BfConsts.ARG_TESTRIG);
    }
  }

  /**
   * Returns a sorted list of {@link Path paths} contains all files under the directory indicated by
   * {@code configsPath}. Directories under {@code configsPath} are recursively expanded but not
   * included in the returned list.
   *
   * <p>Temporary files(files start with {@code .} are omitted from the returned list.
   *
   * <p>This method follows all symbolic links.
   */
  static List<Path> listAllFiles(Path configsPath) {
    List<Path> configFilePaths;
    try (Stream<Path> allFiles = Files.walk(configsPath, FileVisitOption.FOLLOW_LINKS)) {
      configFilePaths =
          allFiles
              .filter(
                  path ->
                      !path.getFileName().toString().startsWith(".") && Files.isRegularFile(path))
              .sorted()
              .collect(Collectors.toList());
    } catch (IOException e) {
      throw new BatfishException("Failed to walk path: " + configsPath, e);
    }
    return configFilePaths;
  }

  public static void logWarnings(BatfishLogger logger, Warnings warnings) {
    for (Warning warning : warnings.getRedFlagWarnings()) {
      logger.redflag(logWarningsHelper(warning));
    }
    for (Warning warning : warnings.getUnimplementedWarnings()) {
      logger.unimplemented(logWarningsHelper(warning));
    }
    for (Warning warning : warnings.getPedanticWarnings()) {
      logger.pedantic(logWarningsHelper(warning));
    }
  }

  private static String logWarningsHelper(Warning warning) {
    return "   " + warning.getTag() + ": " + warning.getText() + "\n";
  }

  public static ParserRuleContext parse(
      BatfishCombinedParser<?, ?> parser, BatfishLogger logger, Settings settings) {
    ParserRuleContext tree;
    try {
      tree = parser.parse();//真正的执行函数
    } catch (BatfishException e) {
      throw new ParserBatfishException("Parser error", e);
    }
    List<String> errors = parser.getErrors();
    int numErrors = errors.size();
    if (numErrors > 0) {
      logger.error(numErrors + " ERROR(S)\n");
      for (int i = 0; i < numErrors; i++) {
        String prefix = "ERROR " + (i + 1) + ": ";
        String msg = errors.get(i);
        String prefixedMsg = CommonUtil.applyPrefix(prefix, msg);
        logger.error(prefixedMsg + "\n");
      }
      throw new ParserBatfishException("Parser error(s)");
    } else if (!settings.getPrintParseTree()) {
      logger.info("OK\n");
    } else {
      logger.info("OK, PRINTING PARSE TREE:\n");
      logger.info(ParseTreePrettyPrinter.print(tree, parser) + "\n\n");
    }
    System.out.println("--------------------back-parse-tree:-----------------------");
    System.out.println(ParseTreePrettyPrinter.print(tree, parser));
    System.out.println("--------------------------End-----------------------");
    return tree;
  }

  private final Map<String, BiFunction<Question, IBatfish, Answerer>> _answererCreators;

  private TestrigSettings _baseTestrigSettings;

  private SortedMap<BgpTableFormat, BgpTablePlugin> _bgpTablePlugins;

  private final Cache<Snapshot, SortedMap<String, Configuration>> _cachedCompressedConfigurations;

  private final Cache<Snapshot, SortedMap<String, Configuration>> _cachedConfigurations;

  private final Cache<TestrigSettings, DataPlane> _cachedCompressedDataPlanes;

  private final Cache<TestrigSettings, DataPlane> _cachedDataPlanes;

  private final Map<EnvironmentSettings, SortedMap<String, BgpAdvertisementsByVrf>>
      _cachedEnvironmentBgpTables;

  private final Map<EnvironmentSettings, SortedMap<String, RoutesByVrf>>
      _cachedEnvironmentRoutingTables;

  private final Cache<TestrigSettings, ForwardingAnalysis> _cachedForwardingAnalyses;

  private TestrigSettings _deltaTestrigSettings;

  private Set<ExternalBgpAdvertisementPlugin> _externalBgpAdvertisementPlugins;

  private BatfishLogger _logger;

  private Settings _settings;

  private final BatfishStorage _storage;

  // this variable is used communicate with parent thread on how the job
  // finished (null if job finished successfully)
  private String _terminatingExceptionMessage;

  private TestrigSettings _testrigSettings;

  private final List<TestrigSettings> _testrigSettingsStack;

  private Map<String, DataPlanePlugin> _dataPlanePlugins;

  public Batfish(
      Settings settings,
      Cache<Snapshot, SortedMap<String, Configuration>> cachedCompressedConfigurations,
      Cache<Snapshot, SortedMap<String, Configuration>> cachedConfigurations,
      Cache<TestrigSettings, DataPlane> cachedCompressedDataPlanes,
      Cache<TestrigSettings, DataPlane> cachedDataPlanes,
      Map<EnvironmentSettings, SortedMap<String, BgpAdvertisementsByVrf>>
          cachedEnvironmentBgpTables,
      Map<EnvironmentSettings, SortedMap<String, RoutesByVrf>> cachedEnvironmentRoutingTables,
      Cache<TestrigSettings, ForwardingAnalysis> cachedForwardingAnalyses) {
    super(settings.getSerializeToText());
    _settings = settings;
    _bgpTablePlugins = new TreeMap<>();
    _cachedCompressedConfigurations = cachedCompressedConfigurations;
    _cachedConfigurations = cachedConfigurations;
    _cachedCompressedDataPlanes = cachedCompressedDataPlanes;
    _cachedDataPlanes = cachedDataPlanes;
    _cachedEnvironmentBgpTables = cachedEnvironmentBgpTables;
    _cachedEnvironmentRoutingTables = cachedEnvironmentRoutingTables;
    _cachedForwardingAnalyses = cachedForwardingAnalyses;
    _externalBgpAdvertisementPlugins = new TreeSet<>();
    _testrigSettings = settings.getActiveTestrigSettings();
    _baseTestrigSettings = settings.getBaseTestrigSettings();
    _logger = _settings.getLogger();
    _deltaTestrigSettings = settings.getDeltaTestrigSettings();
    _terminatingExceptionMessage = null;
    _answererCreators = new HashMap<>();
    _testrigSettingsStack = new ArrayList<>();
    _dataPlanePlugins = new HashMap<>();
    _storage = new BatfishStorage(_settings.getContainerDir(), _logger, this::newBatch);
  }

  private Answer analyze() {
    Answer answer = new Answer();
    AnswerSummary summary = new AnswerSummary();
    String analysisName = _settings.getAnalysisName();
    String containerName = _settings.getContainerDir().getFileName().toString();
    Path analysisQuestionsDir =
        _settings
            .getContainerDir()
            .resolve(
                Paths.get(
                        BfConsts.RELPATH_ANALYSES_DIR, analysisName, BfConsts.RELPATH_QUESTIONS_DIR)
                    .toString());
    if (!Files.exists(analysisQuestionsDir)) {
      throw new BatfishException(
          "Analysis questions dir does not exist: '" + analysisQuestionsDir + "'");
    }
    RunAnalysisAnswerElement ae = new RunAnalysisAnswerElement();
    try (Stream<Path> questions = CommonUtil.list(analysisQuestionsDir)) {
      questions.forEach(
          analysisQuestionDir -> {
            String questionName = analysisQuestionDir.getFileName().toString();
            Path analysisQuestionPath = analysisQuestionDir.resolve(BfConsts.RELPATH_QUESTION_FILE);
            _settings.setQuestionPath(analysisQuestionPath);
            Answer currentAnswer;
            try (ActiveSpan analysisQuestionSpan =
                GlobalTracer.get().buildSpan("Getting answer to analysis question").startActive()) {
              assert analysisQuestionSpan != null; // make span not show up as unused
              analysisQuestionSpan.setTag("analysis-name", analysisName);
              currentAnswer = answer();
            }
            // Ensuring that question was parsed successfully
            if (currentAnswer.getQuestion() != null) {
              try {
                // TODO: This can be represented much cleanly and easily with a Json
                _logger.infof(
                    "Ran question:%s from analysis:%s in container:%s; work-id:%s, status:%s, "
                        + "computed dataplane:%s, parameters:%s\n",
                    questionName,
                    analysisName,
                    containerName,
                    getTaskId(),
                    currentAnswer.getSummary().getNumFailed() > 0 ? "failed" : "passed",
                    currentAnswer.getQuestion().getDataPlane(),
                    BatfishObjectMapper.writeString(
                        currentAnswer.getQuestion().getInstance().getVariables()));
              } catch (JsonProcessingException e) {
                throw new BatfishException(
                    String.format(
                        "Error logging question %s in analysis %s", questionName, analysisName),
                    e);
              }
            }
            initAnalysisQuestionPath(analysisName, questionName);
            outputAnswer(currentAnswer);
            ae.getAnswers().put(questionName, currentAnswer);
            _settings.setQuestionPath(null);
            summary.combine(currentAnswer.getSummary());
          });
    }
    answer.addAnswerElement(ae);
    answer.setSummary(summary);
    return answer;
  }

  public Answer answer() {
    Question question = null;

    // return right away if we cannot parse the question successfully
    try (ActiveSpan parseQuestionSpan =
        GlobalTracer.get().buildSpan("Parse question").startActive()) {
      assert parseQuestionSpan != null; // avoid not used warning
      question = Question.parseQuestion(_settings.getQuestionPath());
    } catch (Exception e) {
      Answer answer = new Answer();
      BatfishException exception = new BatfishException("Could not parse question", e);
      answer.setStatus(AnswerStatus.FAILURE);
      answer.addAnswerElement(exception.getBatfishStackTrace());
      return answer;
    }

    if (GlobalTracer.get().activeSpan() != null) {
      ActiveSpan activeSpan = GlobalTracer.get().activeSpan();
      activeSpan
          .setTag("container-name", getContainerName())
          .setTag("testrig_name", getTestrigName());
      if (question.getInstance() != null) {
        activeSpan.setTag("question-name", question.getInstance().getInstanceName());
      }
    }

    if (_settings.getDifferential()) {
      question.setDifferential(true);
    }
    boolean dp = question.getDataPlane();
    boolean diff = question.getDifferential();
    boolean diffActive = _settings.getDiffActive() && !diff;
    _settings.setDiffActive(diffActive);
    _settings.setDiffQuestion(diff);

    try (ActiveSpan loadConfigurationSpan =
        GlobalTracer.get().buildSpan("Load configurations").startActive()) {
      assert loadConfigurationSpan != null; // avoid not used warning
      // Ensures configurations are parsed and ready
      loadConfigurations();
    }

    try (ActiveSpan initQuestionEnvSpan =
        GlobalTracer.get().buildSpan("Init question environment").startActive()) {
      assert initQuestionEnvSpan != null; // avoid not used warning
      initQuestionEnvironments(question, diff, diffActive, dp);
    }

    AnswerElement answerElement = null;
    BatfishException exception = null;
    try (ActiveSpan getAnswerSpan = GlobalTracer.get().buildSpan("Get answer").startActive()) {
      assert getAnswerSpan != null; // avoid not used warning
      if (question.getDifferential()) {
        answerElement = Answerer.create(question, this).answerDiff();
      } else {
        answerElement = Answerer.create(question, this).answer();
      }
    } catch (Exception e) {
      exception = new BatfishException("Failed to answer question", e);
    }

    Answer answer = new Answer();
    answer.setQuestion(question);

    if (exception == null) {
      // success
      answer.setStatus(AnswerStatus.SUCCESS);
      answer.addAnswerElement(answerElement);
    } else {
      // failure
      answer.setStatus(AnswerStatus.FAILURE);
      answer.addAnswerElement(exception.getBatfishStackTrace());
    }
    return answer;
  }

  /**
   * Identifies any independently unmatchable ACL lines (i.e. they have unsatisfiable match
   * condition) in the given set of ACL lines.
   *
   * @param configurations Map of hostnames -> configuration objects
   * @param linesToCheck Map of hostnames -> ACL names -> set of line numbers for which to check
   *     matchability
   * @return Map of hostname -> ACL names -> set of line numbers that are independently unmatchable
   */
  public SortedMap<String, SortedMap<String, SortedSet<Integer>>>
      computeIndependentlyUnmatchableAclLines(
          Map<String, Configuration> configurations,
          Map<String, Map<String, Set<Integer>>> linesToCheck) {
    List<NodSatJob<AclLine>> jobs = new ArrayList<>();
    Synthesizer aclSynthesizer = synthesizeAcls(configurations);
    linesToCheck.forEach(
        (hostname, aclNames) -> {
          aclNames.forEach(
              (aclName, lineNumbers) -> {
                Configuration c = configurations.get(hostname);
                IpAccessList acl = c.getIpAccessLists().get(aclName);
                int numLines = acl.getLines().size();
                if (numLines == 0) {
                  _logger.redflag(
                      "RED_FLAG: Acl \"" + hostname + ":" + aclName + "\" contains no lines\n");
                  return;
                }
                for (int lineNumber : lineNumbers) {
                  AclLineIndependentSatisfiabilityQuerySynthesizer query =
                      new AclLineIndependentSatisfiabilityQuerySynthesizer(
                          hostname, aclName, lineNumber);
                  NodSatJob<AclLine> job = new NodSatJob<>(_settings, aclSynthesizer, query);
                  jobs.add(job);
                }
              });
        });
    Map<AclLine, Boolean> satisfiabilityByLine = new TreeMap<>();
    computeNodSatOutput(jobs, satisfiabilityByLine);
    SortedMap<String, SortedMap<String, ImmutableSortedSet.Builder<Integer>>> output =
        new TreeMap<>();
    satisfiabilityByLine.forEach(
        (aclLine, satisfiable) -> {
          if (!satisfiable) {
            output
                .computeIfAbsent(aclLine.getHostname(), h -> new TreeMap<>())
                .computeIfAbsent(aclLine.getAclName(), a -> ImmutableSortedSet.naturalOrder())
                .add(aclLine.getLine());
          }
        });
    return CommonUtil.toImmutableSortedMap(
        output,
        Entry::getKey /* hostname */,
        linesByAclNameByHostnameNameEntry ->
            CommonUtil.toImmutableSortedMap(
                linesByAclNameByHostnameNameEntry.getValue(),
                Entry::getKey /* aclName */,
                linesByAclNameEntry -> linesByAclNameEntry.getValue().build()));
  }

  @Override
  public AnswerElement answerAclReachability(
      String aclNameRegexStr, NamedStructureEquivalenceSets<?> aclEqSets) {

    Pattern aclNameRegex;
    try {
      aclNameRegex = Pattern.compile(aclNameRegexStr);
    } catch (PatternSyntaxException e) {
      throw new BatfishException(
          "Supplied regex for nodes is not a valid Java regex: \"" + aclNameRegexStr + "\"", e);
    }

    AclLinesAnswerElement answerElement = new AclLinesAnswerElement();
    Map<String, Configuration> configurations = loadConfigurations();

    // Run first batch of nod jobs to find unreachable lines
    List<NodSatJob<AclLine>> jobs =
        generateUnreachableAclLineJobs(aclNameRegex, aclEqSets, configurations, answerElement);
    Map<AclLine, Boolean> linesReachableMap = new TreeMap<>();
    computeNodSatOutput(jobs, linesReachableMap);

    // Create two maps with hostnames as keys and Map(acl name -> acl lines) as values. One map will
    // contain all ACLs with unreachable lines, the other will have all fully reachable ACLs.
    // Also create a map of hostname -> aclName -> Set<unreachable lines> to use for finding
    // independently unmatchable lines.
    Map<String, Map<String, List<AclLine>>> hostnamesToAclsWithUnreachableLinesMap =
        new TreeMap<>();
    Map<String, Map<String, List<AclLine>>> hostnamesToFullyReachableAclsMap = new TreeMap<>();
    Map<String, Map<String, Set<Integer>>> hostnamesToUnreachableLinesMap = new TreeMap<>();
    for (Entry<AclLine, Boolean> e : linesReachableMap.entrySet()) {
      AclLine line = e.getKey();
      String aclName = line.getAclName();
      String hostname = line.getHostname();

      Map<String, List<AclLine>> fullyReachableAcls =
          hostnamesToFullyReachableAclsMap.computeIfAbsent(hostname, k -> new TreeMap<>());
      Map<String, List<AclLine>> aclsWithUnreachableLines =
          hostnamesToAclsWithUnreachableLinesMap.computeIfAbsent(hostname, k -> new TreeMap<>());

      if (!e.getValue()) {
        // Current line is unreachable. Add it to hostnamesToUnreachableLinesMap.
        hostnamesToUnreachableLinesMap
            .computeIfAbsent(hostname, h -> new TreeMap<>())
            .computeIfAbsent(aclName, a -> new TreeSet<>())
            .add(line.getLine());
        // Get lines so far of this ACL. Could be in fullyReachableAcls or aclsWithUnreachableLines,
        // or this could be the first line (and be independently unsatisfiable).
        // If ACL was thus far completely reachable, move its lines into aclsWithUnreachableLines.
        List<AclLine> aclRecordedAsReachable = fullyReachableAcls.remove(aclName);
        if (aclRecordedAsReachable != null) {
          aclRecordedAsReachable.add(line);
          aclsWithUnreachableLines.put(aclName, aclRecordedAsReachable);
        } else {
          // Otherwise, either this is the first line (and is independently unsatisfiable) or there
          // were already unreachable lines in this ACL. Add line to appropriate ACL line list.
          aclsWithUnreachableLines.computeIfAbsent(aclName, k -> new ArrayList<>()).add(line);
        }
      } else {
        // Current line is reachable.
        // Get lines so far of this ACL. Could be in fullyReachableAcls, aclsWithUnreachableLines,
        // or this could be the first line. Create/get the ACL's list of lines.
        List<AclLine> aclLineList = aclsWithUnreachableLines.get(aclName);
        if (aclLineList == null) {
          aclLineList = fullyReachableAcls.computeIfAbsent(aclName, k -> new ArrayList<>());
        }
        // Add line to the ACL's list of lines.
        aclLineList.add(line);
      }
    }

    // Find any independently unmatchable lines.
    SortedMap<String, SortedMap<String, SortedSet<Integer>>> unmatchableLines =
        computeIndependentlyUnmatchableAclLines(configurations, hostnamesToUnreachableLinesMap);

    // Run second batch of nod jobs to get earliest more general lines for each unreachable line
    // Produces a map of acl line -> line number of earliest more general reachable line
    List<NodFirstUnsatJob<AclLine, Integer>> step2Jobs =
        generateEarliestMoreGeneralAclLineJobs(
            hostnamesToAclsWithUnreachableLinesMap, linesReachableMap, configurations);
    Map<AclLine, Integer> blockingLinesMap = new TreeMap<>();
    computeNodFirstUnsatOutput(step2Jobs, blockingLinesMap);

    // For each line:
    // - Add <hostname, aclName> pair to allAclHostPairs set
    // - Create an AclReachabilityEntry
    // - If the line is unreachable:
    //    - Add <hostname, aclName> pair to aclHostPairsWithUnreachableLines set
    //    - If line is independently unmatchable, set reachability entry's message appropriately
    //    - Else check earliestMoreGeneralReachableLine; if it exists, add to reachability entry
    //      - If it doesn't exist, assume multiple blocking lines; set reachability entry's message
    // - Add the reachability entry to the answer element
    Set<Pair<String, String>> aclHostPairsWithUnreachableLines = new TreeSet<>();
    Set<Pair<String, String>> allAclHostPairs = new TreeSet<>();
    int numUnreachableLines = 0;
    int numLines = linesReachableMap.entrySet().size();
    for (Entry<AclLine, Boolean> e : linesReachableMap.entrySet()) {
      AclLine line = e.getKey();
      String hostname = line.getHostname();
      String aclName = line.getAclName();
      int lineNumber = line.getLine();
      boolean lineIsReachable = e.getValue();
      SortedMap<String, SortedSet<Integer>> aclsWithUnmatchableLinesOnThisHost =
          firstNonNull(unmatchableLines.get(hostname), new TreeMap<>());
      SortedSet<Integer> unmatchableLinesOnThisAcl =
          firstNonNull(aclsWithUnmatchableLinesOnThisHost.get(aclName), new TreeSet<>());

      // TODO add ipAccessList to ACL mapping so we don't have to get it multiple times for one ACL
      IpAccessList ipAccessList = configurations.get(hostname).getIpAccessLists().get(aclName);
      IpAccessListLine ipAccessListLine = ipAccessList.getLines().get(lineNumber);
      String lineName = firstNonNull(ipAccessListLine.getName(), ipAccessListLine.toString());
      AclReachabilityEntry reachabilityEntry = new AclReachabilityEntry(lineNumber, lineName);

      Pair<String, String> hostnameAclPair = new Pair<>(hostname, aclName);
      allAclHostPairs.add(hostnameAclPair);

      if (!lineIsReachable) {
        _logger.debugf(
            "%s:%s:%d:'%s' is UNREACHABLE\n\t%s\n",
            hostname, aclName, lineNumber, lineName, ipAccessListLine.toString());
        numUnreachableLines++;
        aclHostPairsWithUnreachableLines.add(hostnameAclPair);

        /*
         * We are using earliestMoreGeneralLineIndex and earliestMoreGeneralLineName inappropriately
         * here. For both the multiple blocking lines case and the independently unmatchable case,
         * we set the line index to -1 and set the line name to a hard-coded message (see below).
         * For right now, it's hard to fix because the JsonPathAnswerElement for ACL reachability
         * doesn't account for these two cases and changing that would be a breaking change.
         * TODO Stop using the earliestMoreGeneralLine variables for other answer categories.
         */
        if (unmatchableLinesOnThisAcl.contains(lineNumber)) {
          reachabilityEntry.setEarliestMoreGeneralLineIndex(-1);
          reachabilityEntry.setEarliestMoreGeneralLineName(
              "This line will never match any packet, independent of preceding lines.");
        } else {
          Integer earliestMoreGeneralReachableLineNumber = blockingLinesMap.get(line);
          line.setEarliestMoreGeneralReachableLine(earliestMoreGeneralReachableLineNumber);

          if (earliestMoreGeneralReachableLineNumber != null) {
            IpAccessListLine earliestMoreGeneralLine =
                ipAccessList.getLines().get(earliestMoreGeneralReachableLineNumber);
            reachabilityEntry.setEarliestMoreGeneralLineIndex(
                earliestMoreGeneralReachableLineNumber);
            reachabilityEntry.setEarliestMoreGeneralLineName(
                firstNonNull(
                    earliestMoreGeneralLine.getName(), earliestMoreGeneralLine.toString()));
            if (!earliestMoreGeneralLine.getAction().equals(ipAccessListLine.getAction())) {
              reachabilityEntry.setDifferentAction(true);
            }
          } else {
            // If line is unreachable but earliestMoreGeneralReachableLine is null, there must be
            // multiple partially-blocking lines. Provide that info in any way we can...
            reachabilityEntry.setEarliestMoreGeneralLineIndex(-1);
            reachabilityEntry.setEarliestMoreGeneralLineName(
                "Multiple earlier lines partially block this line, making it unreachable.");
          }
        }
        answerElement.addUnreachableLine(hostname, ipAccessList, reachabilityEntry);

      } else {
        _logger.debugf("%s:%s:%d:'%s' is REACHABLE\n", hostname, aclName, lineNumber, lineName);
        answerElement.addReachableLine(hostname, ipAccessList, reachabilityEntry);
      }
    }

    // Log results and return answerElement.
    for (Pair<String, String> qualifiedAcl : aclHostPairsWithUnreachableLines) {
      String hostname = qualifiedAcl.getFirst();
      String aclName = qualifiedAcl.getSecond();
      _logger.debugf("%s:%s has at least 1 unreachable line\n", hostname, aclName);
    }
    int numAclsWithUnreachableLines = aclHostPairsWithUnreachableLines.size();
    int numAcls = allAclHostPairs.size();
    double percentUnreachableAcls = 100d * numAclsWithUnreachableLines / numAcls;
    double percentUnreachableLines = 100d * numUnreachableLines / numLines;
    _logger.debugf("SUMMARY:\n");
    _logger.debugf(
        "\t%d/%d (%.1f%%) acls have unreachable lines\n",
        numAclsWithUnreachableLines, numAcls, percentUnreachableAcls);
    _logger.debugf(
        "\t%d/%d (%.1f%%) acl lines are unreachable\n",
        numUnreachableLines, numLines, percentUnreachableLines);

    return answerElement;
  }

  private List<NodSatJob<AclLine>> generateUnreachableAclLineJobs(
      Pattern aclNameRegex,
      NamedStructureEquivalenceSets<?> aclEqSets,
      Map<String, Configuration> configurations,
      AclLinesAnswerElement answerElement) {
    List<NodSatJob<AclLine>> jobs = new ArrayList<>();

    for (Entry<String, ?> e : aclEqSets.getSameNamedStructures().entrySet()) {
      String aclName = e.getKey();
      if (!aclNameRegex.matcher(aclName).matches()) {
        continue;
      }
      // skip juniper srx inbound filters, as they can't really contain
      // operator error
      // todo: Verify comment and bring this back (https://github.com/batfish/batfish/issues/1275)
      //      if (aclName.contains("~ZONE_INTERFACE_FILTER~")
      //          || aclName.contains("~INBOUND_ZONE_FILTER~")) {
      //        continue;
      //      }

      Set<?> s = (Set<?>) e.getValue();
      for (Object o : s) {
        NamedStructureEquivalenceSet<?> aclEqSet = (NamedStructureEquivalenceSet<?>) o;
        String hostname = aclEqSet.getRepresentativeElement();
        SortedSet<String> eqClassNodes = aclEqSet.getNodes();
        answerElement.addEquivalenceClass(aclName, hostname, eqClassNodes);
        Configuration c = configurations.get(hostname);
        IpAccessList acl = c.getIpAccessLists().get(aclName);
        int numLines = acl.getLines().size();
        if (numLines == 0) {
          _logger.redflag("RED_FLAG: Acl \"" + hostname + ":" + aclName + "\" contains no lines\n");
          continue;
        }
        AclReachabilityQuerySynthesizer query =
            new AclReachabilityQuerySynthesizer(hostname, aclName, numLines);
        Synthesizer aclSynthesizer = synthesizeAcls(Collections.singletonMap(hostname, c));
        NodSatJob<AclLine> job = new NodSatJob<>(_settings, aclSynthesizer, query);
        jobs.add(job);
      }
      // Add current ACL back into ACL list before moving on to next ACL
    }
    return jobs;
  }

  private List<NodFirstUnsatJob<AclLine, Integer>> generateEarliestMoreGeneralAclLineJobs(
      Map<String, Map<String, List<AclLine>>> unreachableAclLinesMap,
      Map<AclLine, Boolean> aclLinesReachabilityMap, // map of acl line -> isReachable boolean
      Map<String, Configuration> configurations) {
    List<NodFirstUnsatJob<AclLine, Integer>> jobs = new ArrayList<>();
    for (Entry<String, Map<String, List<AclLine>>> e : unreachableAclLinesMap.entrySet()) {
      String hostname = e.getKey();
      Configuration c = configurations.get(hostname);
      List<String> nodeInterfaces =
          ImmutableList.sortedCopyOf(
              c.getInterfaces()
                  .values()
                  .stream()
                  .map(Interface::getName)
                  .collect(Collectors.toList()));
      Synthesizer aclSynthesizer = synthesizeAcls(Collections.singletonMap(hostname, c));
      Map<String, List<AclLine>> byAclName = e.getValue();
      for (Entry<String, List<AclLine>> e2 : byAclName.entrySet()) {
        String aclName = e2.getKey();
        // Generate job for earlier blocking lines in this ACL
        IpAccessList ipAccessList = c.getIpAccessLists().get(aclName);
        List<AclLine> lines = e2.getValue();
        for (int i = 0; i < lines.size(); i++) {
          AclLine line = lines.get(i);
          boolean reachable = aclLinesReachabilityMap.get(line);
          if (!reachable) {
            List<AclLine> toCheck = new ArrayList<>();
            for (int j = 0; j < i; j++) {
              AclLine earlierLine = lines.get(j);
              boolean earlierIsReachable = aclLinesReachabilityMap.get(earlierLine);
              if (earlierIsReachable) {
                toCheck.add(earlierLine);
              }
            }
            EarliestMoreGeneralReachableLineQuerySynthesizer query =
                new EarliestMoreGeneralReachableLineQuerySynthesizer(
                    line,
                    toCheck,
                    ipAccessList,
                    c.getIpSpaces(),
                    c.getIpAccessLists(),
                    nodeInterfaces);
            NodFirstUnsatJob<AclLine, Integer> job =
                new NodFirstUnsatJob<>(_settings, aclSynthesizer, query);
            jobs.add(job);
          }
        }
      }
    }
    return jobs;
  }

  public static Warnings buildWarnings(Settings settings) {
    return new Warnings(
        settings.getPedanticAsError(),
        settings.getPedanticRecord() && settings.getLogger().isActive(BatfishLogger.LEVEL_PEDANTIC),
        settings.getRedFlagAsError(),
        settings.getRedFlagRecord() && settings.getLogger().isActive(BatfishLogger.LEVEL_REDFLAG),
        settings.getUnimplementedAsError(),
        settings.getUnimplementedRecord()
            && settings.getLogger().isActive(BatfishLogger.LEVEL_UNIMPLEMENTED),
        settings.getPrintParseTree());
  }

  private void checkBaseDirExists() {
    Path baseDir = _testrigSettings.getBasePath();
    if (baseDir == null) {
      throw new BatfishException("Test rig directory not set");
    }
    if (!Files.exists(baseDir)) {
      throw new CleanBatfishException("Test rig does not exist: \"" + baseDir.getFileName() + "\"");
    }
  }

  @Override
  public void checkDataPlane() {
    checkDataPlane(_testrigSettings);
  }

  public void checkDataPlane(TestrigSettings testrigSettings) {
    EnvironmentSettings envSettings = testrigSettings.getEnvironmentSettings();
    if (!Files.exists(envSettings.getDataPlanePath())) {
      throw new CleanBatfishException(
          "Missing data plane for testrig: \""
              + testrigSettings.getName()
              + "\", environment: \""
              + envSettings.getName()
              + "\"\n");
    }
  }

  public void checkDiffEnvironmentExists() {
    checkDiffEnvironmentSpecified();
    checkEnvironmentExists(_deltaTestrigSettings);
  }

  private void checkDiffEnvironmentSpecified() {
    if (_settings.getDeltaEnvironmentName() == null) {
      throw new CleanBatfishException(
          "No differential environment specified for differential question");
    }
  }

  public void checkDifferentialDataPlaneQuestionDependencies() {
    checkDiffEnvironmentSpecified();
    checkDataPlane(_baseTestrigSettings);
    checkDataPlane(_deltaTestrigSettings);
  }

  @Override
  public void checkEnvironmentExists() {
    checkEnvironmentExists(_testrigSettings);
  }

  public void checkEnvironmentExists(TestrigSettings testrigSettings) {
    if (!environmentExists(testrigSettings)) {
      throw new CleanBatfishException(
          "Environment not initialized: \""
              + testrigSettings.getEnvironmentSettings().getName()
              + "\"");
    }
  }

  private void checkQuestionsDirExists() {
    checkBaseDirExists();
    Path questionsDir = _testrigSettings.getBasePath().resolve(BfConsts.RELPATH_QUESTIONS_DIR);
    if (!Files.exists(questionsDir)) {
      throw new CleanBatfishException(
          "questions dir does not exist: \"" + questionsDir.getFileName() + "\"");
    }
  }

  private Answer compileEnvironmentConfigurations(TestrigSettings testrigSettings) {
    Answer answer = new Answer();
    EnvironmentSettings envSettings = testrigSettings.getEnvironmentSettings();
    Path deltaConfigurationsDir = envSettings.getDeltaConfigurationsDir();
    Path vendorConfigsDir = envSettings.getDeltaVendorConfigurationsDir();
    if (deltaConfigurationsDir != null) {
      if (Files.exists(deltaConfigurationsDir)) {
        answer.append(serializeVendorConfigs(envSettings.getEnvPath(), vendorConfigsDir));
        answer.append(serializeIndependentConfigs(vendorConfigsDir));
      }
      return answer;
    } else {
      throw new BatfishException("Delta configurations directory cannot be null");
    }
  }

  public Set<Flow> computeCompositeNodOutput(
      List<CompositeNodJob> jobs, NodAnswerElement answerElement) {
    _logger.info("\n*** EXECUTING COMPOSITE NOD JOBS ***\n");
    _logger.resetTimer();
    Set<Flow> flows = new TreeSet<>();
    BatfishJobExecutor.runJobsInExecutor(
        _settings, _logger, jobs, flows, answerElement, true, "Composite NOD");
    _logger.printElapsedTime();
    return flows;
  }

  private CompressDataPlaneResult computeCompressedDataPlane() {
    CompressDataPlaneResult result = computeCompressedDataPlane(new HeaderSpace());
    _cachedCompressedConfigurations.put(getSnapshot(), new TreeMap<>(result._compressedConfigs));
    saveDataPlane(result._compressedDataPlane, result._answerElement, true);
    return result;
  }

  public class CompressDataPlaneResult {
    public final Map<String, Configuration> _compressedConfigs;
    public final DataPlane _compressedDataPlane;
    public final DataPlaneAnswerElement _answerElement;

    public CompressDataPlaneResult(
        Map<String, Configuration> compressedConfigs,
        DataPlane compressedDataPlane,
        DataPlaneAnswerElement answerElement) {
      _compressedConfigs = compressedConfigs;
      _compressedDataPlane = compressedDataPlane;
      _answerElement = answerElement;
    }
  }

  private CompressDataPlaneResult computeCompressedDataPlane(HeaderSpace headerSpace) {
    // Since compression mutates the configurations, we must clone them before that happens.
    // A simple way to do this is to create a deep clone of each entry using Java serialization.
    _logger.info("Computing compressed dataplane\n");
    Map<String, Configuration> clonedConfigs =
        loadConfigurations()
            .entrySet()
            .parallelStream()
            .collect(toMap(Entry::getKey, entry -> SerializationUtils.clone(entry.getValue())));

    Map<String, Configuration> configs =
        new BatfishCompressor(this, clonedConfigs).compress(headerSpace);
    Topology topo = CommonUtil.synthesizeTopology(configs);
    DataPlanePlugin dataPlanePlugin = getDataPlanePlugin();
    ComputeDataPlaneResult result = dataPlanePlugin.computeDataPlane(false, configs, topo);

    _storage.storeCompressedConfigurations(configs, _testrigSettings.getName());
    return new CompressDataPlaneResult(configs, result._dataPlane, result._answerElement);
  }

  @Override
  public DataPlaneAnswerElement computeDataPlane(boolean differentialContext) {
    checkEnvironmentExists();
    ComputeDataPlaneResult result = getDataPlanePlugin().computeDataPlane(differentialContext);
    saveDataPlane(result._dataPlane, result._answerElement, false);
    return result._answerElement;
  }

  /* Write the dataplane to disk and cache, and write the answer element to disk.
   */
  private void saveDataPlane(
      DataPlane dataPlane, DataPlaneAnswerElement answerElement, boolean compressed) {
    Path dataPlanePath =
        compressed
            ? _testrigSettings.getEnvironmentSettings().getCompressedDataPlanePath()
            : _testrigSettings.getEnvironmentSettings().getDataPlanePath();

    Path answerElementPath =
        compressed
            ? _testrigSettings.getEnvironmentSettings().getCompressedDataPlaneAnswerPath()
            : _testrigSettings.getEnvironmentSettings().getDataPlaneAnswerPath();

    Cache<TestrigSettings, DataPlane> cache =
        compressed ? _cachedCompressedDataPlanes : _cachedDataPlanes;

    cache.put(_testrigSettings, dataPlane);

    _logger.resetTimer();
    newBatch("Writing data plane to disk", 0);
    try (ActiveSpan writeDataplane =
        GlobalTracer.get().buildSpan("Writing data plane").startActive()) {
      assert writeDataplane != null; // avoid unused warning
      serializeObject(dataPlane, dataPlanePath);
      serializeObject(answerElement, answerElementPath);
    }
    _logger.printElapsedTime();
  }

  private void computeEnvironmentBgpTables() {
    EnvironmentSettings envSettings = _testrigSettings.getEnvironmentSettings();
    Path outputPath = envSettings.getSerializeEnvironmentBgpTablesPath();
    Path inputPath = envSettings.getEnvironmentBgpTablesPath();
    serializeEnvironmentBgpTables(inputPath, outputPath);
  }

  private void computeEnvironmentRoutingTables() {
    EnvironmentSettings envSettings = _testrigSettings.getEnvironmentSettings();
    Path outputPath = envSettings.getSerializeEnvironmentRoutingTablesPath();
    Path inputPath = envSettings.getEnvironmentRoutingTablesPath();
    serializeEnvironmentRoutingTables(inputPath, outputPath);
  }

  Topology computeEnvironmentTopology(Map<String, Configuration> configurations) {
    _logger.resetTimer();
    Topology topology = computeTestrigTopology(_testrigSettings.getTestRigPath(), configurations);
    topology.prune(getEdgeBlacklist(), getNodeBlacklist(), getInterfaceBlacklist());
    _logger.printElapsedTime();
    return topology;
  }

  public <KeyT, ResultT> void computeNodFirstUnsatOutput(
      List<NodFirstUnsatJob<KeyT, ResultT>> jobs, Map<KeyT, ResultT> output) {
    _logger.info("\n*** EXECUTING NOD UNSAT JOBS ***\n");
    _logger.resetTimer();
    BatfishJobExecutor.runJobsInExecutor(
        _settings,
        _logger,
        jobs,
        output,
        new NodFirstUnsatAnswerElement(),
        true,
        "NOD First-UNSAT");
    _logger.printElapsedTime();
  }

  public Set<Flow> computeNodOutput(List<NodJob> jobs) {
    _logger.info("\n*** EXECUTING NOD JOBS ***\n");
    _logger.resetTimer();
    Set<Flow> flows = new TreeSet<>();
    BatfishJobExecutor.runJobsInExecutor(
        _settings, _logger, jobs, flows, new NodAnswerElement(), true, "NOD");
    _logger.printElapsedTime();
    return flows;
  }

  public <KeyT> void computeNodSatOutput(List<NodSatJob<KeyT>> jobs, Map<KeyT, Boolean> output) {
    _logger.info("\n*** EXECUTING NOD SAT JOBS ***\n");
    _logger.resetTimer();
    BatfishJobExecutor.runJobsInExecutor(
        _settings, _logger, jobs, output, new NodSatAnswerElement(), true, "NOD SAT");
    _logger.printElapsedTime();
  }

  private Topology computeTestrigTopology(
      Path testRigPath, Map<String, Configuration> configurations) {
    Path topologyFilePath = testRigPath.resolve(TOPOLOGY_FILENAME);
    Topology topology;
    // Get generated facts from topology file
    if (Files.exists(topologyFilePath)) {
      topology = processTopologyFile(topologyFilePath);//判断有没有拓扑文件,如果有的话那么直接加载,如果没有的话那么就需要进行计算
      _logger.infof(
          "Testrig:%s in container:%s has topology file", getTestrigName(), getContainerName());
    } else {
      // guess adjacencies based on interface subnetworks
      _logger.info("*** (GUESSING TOPOLOGY IN ABSENCE OF EXPLICIT FILE) ***\n");
      topology = CommonUtil.synthesizeTopology(configurations);
    }
    return topology;
  }

  private Map<String, Configuration> convertConfigurations(
      Map<String, GenericConfigObject> vendorConfigurations,
      ConvertConfigurationAnswerElement answerElement) {
    _logger.info("\n*** CONVERTING VENDOR CONFIGURATIONS TO INDEPENDENT FORMAT ***\n");
    _logger.resetTimer();
    Map<String, Configuration> configurations = new TreeMap<>();//基本了解一些解析的思想
    List<ConvertConfigurationJob> jobs = new ArrayList<>();
    for (Entry<String, GenericConfigObject> config : vendorConfigurations.entrySet()) {
      GenericConfigObject vc = config.getValue();
      ConvertConfigurationJob job = new ConvertConfigurationJob(_settings, vc, config.getKey());
      jobs.add(job);
    }
    BatfishJobExecutor.runJobsInExecutor(
        _settings,
        _logger,
        jobs,
        configurations,
        answerElement,
        _settings.getHaltOnConvertError(),
        "Convert configurations to vendor-independent format");

    _logger.printElapsedTime();
    return configurations;//最终与配置无关的形式保存为这种形式
  }

  private boolean dataPlaneDependenciesExist(TestrigSettings testrigSettings) {
    Path dpPath = testrigSettings.getEnvironmentSettings().getDataPlaneAnswerPath();
    return Files.exists(dpPath);
  }

  private boolean compressedDataPlaneDependenciesExist(TestrigSettings testrigSettings) {
    Path path = testrigSettings.getEnvironmentSettings().getCompressedDataPlaneAnswerPath();
    return Files.exists(path);
  }

  @Override
  public boolean debugFlagEnabled(String flag) {
    return _settings.debugFlagEnabled(flag);
  }

  private SortedMap<String, BgpAdvertisementsByVrf> deserializeEnvironmentBgpTables(
      Path serializeEnvironmentBgpTablesPath) {
    _logger.info("\n*** DESERIALIZING ENVIRONMENT BGP TABLES ***\n");
    _logger.resetTimer();
    Map<Path, String> namesByPath = new TreeMap<>();
    try (DirectoryStream<Path> serializedBgpTables =
        Files.newDirectoryStream(serializeEnvironmentBgpTablesPath)) {
      for (Path serializedBgpTable : serializedBgpTables) {
        String name = serializedBgpTable.getFileName().toString();
        namesByPath.put(serializedBgpTable, name);
      }
    } catch (IOException e) {
      throw new BatfishException("Error reading serialized BGP tables directory", e);
    }
    SortedMap<String, BgpAdvertisementsByVrf> bgpTables =
        deserializeObjects(namesByPath, BgpAdvertisementsByVrf.class);
    _logger.printElapsedTime();
    return bgpTables;
  }

  private SortedMap<String, RoutesByVrf> deserializeEnvironmentRoutingTables(
      Path serializeEnvironmentRoutingTablesPath) {
    _logger.info("\n*** DESERIALIZING ENVIRONMENT ROUTING TABLES ***\n");
    _logger.resetTimer();
    Map<Path, String> namesByPath = new TreeMap<>();
    try (DirectoryStream<Path> serializedRoutingTables =
        Files.newDirectoryStream(serializeEnvironmentRoutingTablesPath)) {
      for (Path serializedRoutingTable : serializedRoutingTables) {
        String name = serializedRoutingTable.getFileName().toString();
        namesByPath.put(serializedRoutingTable, name);
      }
    } catch (IOException e) {
      throw new BatfishException("Error reading serialized routing tables directory", e);
    }
    SortedMap<String, RoutesByVrf> routingTables =
        deserializeObjects(namesByPath, RoutesByVrf.class);
    _logger.printElapsedTime();
    return routingTables;
  }

  /**
   * Deserialize a bunch of objects
   *
   * @param namesByPath Mapping of object paths to their names 将配置文件的路径和名字对应掐里
   * @param outputClass the class type for {@link S}
   * @param <S> desired type of objects
   * @return a map of objects keyed by their name (from {@code namesByPath})
   */
  public <S extends Serializable> SortedMap<String, S> deserializeObjects(
      Map<Path, String> namesByPath, Class<S> outputClass) {
    String outputClassName = outputClass.getName();
    BatfishLogger logger = getLogger();
    AtomicInteger readCompleted =
        newBatch(
            "Reading, unpacking, and deserializing files containing '"
                + outputClassName
                + "' instances",
            namesByPath.size());
    return namesByPath
        .entrySet()
        .parallelStream()
        .map(
            e -> {
              logger.debugf(
                  "Reading and unzipping: %s '%s' from %s%n",
                  outputClassName, e.getValue(), e.getKey());
              S object = deserializeObject(e.getKey(), outputClass);
              logger.debug(" ...OK\n");
              readCompleted.incrementAndGet();
              return new SimpleImmutableEntry<>(e.getValue(), object);
            })
        .collect(
            ImmutableSortedMap.toImmutableSortedMap(
                String::compareTo, Entry::getKey, Entry::getValue));
  }

  public Map<String, GenericConfigObject> deserializeVendorConfigurations(
      Path serializedVendorConfigPath) {
    _logger.info("\n*** DESERIALIZING VENDOR CONFIGURATION STRUCTURES ***\n");
    _logger.resetTimer();
    Map<Path, String> namesByPath = new TreeMap<>();//获取配置文件的路径和文件的名字
    try (DirectoryStream<Path> serializedConfigs =
        Files.newDirectoryStream(serializedVendorConfigPath)) {
      for (Path serializedConfig : serializedConfigs) {//获取vendor下的每一个目录
        String name = serializedConfig.getFileName().toString();
        namesByPath.put(serializedConfig, name);
      }
    } catch (IOException e) {
      throw new BatfishException("Error reading vendor configs directory", e);
    }
    Map<String, GenericConfigObject> vendorConfigurations =
        deserializeObjects(namesByPath, GenericConfigObject.class);
    _logger.printElapsedTime();
    return vendorConfigurations;
  }

  private void disableUnusableVlanInterfaces(Map<String, Configuration> configurations) {
    for (Configuration c : configurations.values()) {
      String hostname = c.getHostname();

      Map<Integer, Interface> vlanInterfaces = new HashMap<>();
      Map<Integer, Integer> vlanMemberCounts = new HashMap<>();
      Set<Interface> nonVlanInterfaces = new HashSet<>();
      Integer vlanNumber = null;
      // Populate vlanInterface and nonVlanInterfaces, and initialize
      // vlanMemberCounts:
      for (Interface iface : c.getInterfaces().values()) {
        if ((iface.getInterfaceType() == InterfaceType.VLAN)
            && ((vlanNumber = CommonUtil.getInterfaceVlanNumber(iface.getName())) != null)) {
          vlanInterfaces.put(vlanNumber, iface);
          vlanMemberCounts.put(vlanNumber, 0);
        } else {
          nonVlanInterfaces.add(iface);
        }
      }
      // Update vlanMemberCounts:
      for (Interface iface : nonVlanInterfaces) {
        List<SubRange> vlans = new ArrayList<>();
        if (iface.getSwitchportMode() == SwitchportMode.TRUNK) { // vlan trunked interface
          Collection<SubRange> allowed = iface.getAllowedVlans();
          if (!allowed.isEmpty()) {
            // Explicit list of allowed VLANs
            vlans.addAll(allowed);
          } else {
            // No explicit list, so all VLANs are allowed.
            vlanInterfaces.keySet().forEach(v -> vlans.add(new SubRange(v, v)));
          }
          // Add the native VLAN as well.
          vlanNumber = iface.getNativeVlan();
          vlans.add(new SubRange(vlanNumber, vlanNumber));
        } else if (iface.getSwitchportMode() == SwitchportMode.ACCESS) { // access mode ACCESS
          vlanNumber = iface.getAccessVlan();
          vlans.add(new SubRange(vlanNumber, vlanNumber));
          // Any other Switch Port mode is unsupported
        } else if (iface.getSwitchportMode() != SwitchportMode.NONE) {
          _logger.warnf(
              "WARNING: Unsupported switch port mode %s, assuming no VLANs allowed: \"%s:%s\"\n",
              iface.getSwitchportMode(), hostname, iface.getName());
        }

        for (SubRange sr : vlans) {
          for (int vlanId = sr.getStart(); vlanId <= sr.getEnd(); ++vlanId) {
            vlanMemberCounts.compute(vlanId, (k, v) -> (v == null) ? 1 : (v + 1));
          }
        }
      }
      // Disable all "normal" vlan interfaces with zero member counts:
      SubRange normalVlanRange = c.getNormalVlanRange();
      for (Map.Entry<Integer, Integer> entry : vlanMemberCounts.entrySet()) {
        if (entry.getValue() == 0) {
          vlanNumber = entry.getKey();
          if ((vlanNumber >= normalVlanRange.getStart())
              && (vlanNumber <= normalVlanRange.getEnd())) {
            Interface iface = vlanInterfaces.get(vlanNumber);
            if ((iface != null) && iface.getAutoState()) {
              _logger.warnf(
                  "WARNING: Disabling unusable vlan interface because no switch port is assigned "
                      + "to it: \"%s:%d\"\n",
                  hostname, vlanNumber);
              iface.setActive(false);
              iface.setBlacklisted(true);
            }
          }
        }
      }
    }
  }

  private void disableUnusableVpnInterfaces(Map<String, Configuration> configurations) {
    CommonUtil.initRemoteIpsecVpns(configurations);
    for (Configuration c : configurations.values()) {
      for (IpsecVpn vpn : c.getIpsecVpns().values()) {
        IpsecVpn remoteVpn = vpn.getRemoteIpsecVpn();
        if (remoteVpn == null
            || !vpn.compatibleIkeProposals(remoteVpn)
            || !vpn.compatibleIpsecProposals(remoteVpn)
            || !vpn.compatiblePreSharedKey(remoteVpn)) {
          String hostname = c.getHostname();
          Interface bindInterface = vpn.getBindInterface();
          if (bindInterface != null) {
            bindInterface.setActive(false);
            bindInterface.setBlacklisted(true);
            String bindInterfaceName = bindInterface.getName();
            _logger.warnf(
                "WARNING: Disabling unusable vpn interface because we cannot determine remote "
                    + "endpoint: \"%s:%s\"\n",
                hostname, bindInterfaceName);
          }
        }
      }
    }
  }

  private boolean environmentBgpTablesExist(EnvironmentSettings envSettings) {
    Path answerPath = envSettings.getParseEnvironmentBgpTablesAnswerPath();
    return Files.exists(answerPath);
  }

  private boolean environmentExists(TestrigSettings testrigSettings) {
    checkBaseDirExists();
    Path envPath = testrigSettings.getEnvironmentSettings().getEnvPath();
    if (envPath == null) {
      throw new CleanBatfishException(
          "No environment specified for testrig: " + testrigSettings.getName());
    }
    return Files.exists(envPath);
  }

  private boolean environmentRoutingTablesExist(EnvironmentSettings envSettings) {
    Path answerPath = envSettings.getParseEnvironmentRoutingTablesAnswerPath();
    return Files.exists(answerPath);
  }

  private void flatten(Path inputPath, Path outputPath) {
    Map<Path, String> configurationData =
        readConfigurationFiles(inputPath, BfConsts.RELPATH_CONFIGURATIONS_DIR);
    Map<Path, String> outputConfigurationData = new TreeMap<>();
    Path outputConfigDir = outputPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR);
    CommonUtil.createDirectories(outputConfigDir);
    _logger.info("\n*** FLATTENING TEST RIG ***\n");
    _logger.resetTimer();
    List<FlattenVendorConfigurationJob> jobs = new ArrayList<>();
    for (Entry<Path, String> configFile : configurationData.entrySet()) {
      Path inputFile = configFile.getKey();
      String fileText = configFile.getValue();
      Warnings warnings = buildWarnings(_settings);
      String name = inputFile.getFileName().toString();
      Path outputFile = outputConfigDir.resolve(name);
      FlattenVendorConfigurationJob job =
          new FlattenVendorConfigurationJob(_settings, fileText, inputFile, outputFile, warnings);
      jobs.add(job);
    }
    BatfishJobExecutor.runJobsInExecutor(
        _settings,
        _logger,
        jobs,
        outputConfigurationData,
        new FlattenVendorConfigurationAnswerElement(),
        _settings.getFlatten() || _settings.getHaltOnParseError(),
        "Flatten configurations");
    _logger.printElapsedTime();
    for (Entry<Path, String> e : outputConfigurationData.entrySet()) {
      Path outputFile = e.getKey();
      String flatConfigText = e.getValue();
      String outputFileAsString = outputFile.toString();
      _logger.debugf("Writing config to \"%s\"...", outputFileAsString);
      CommonUtil.writeFile(outputFile, flatConfigText);
      _logger.debug("OK\n");
    }
    Path inputTopologyPath = inputPath.resolve(TOPOLOGY_FILENAME);
    Path outputTopologyPath = outputPath.resolve(TOPOLOGY_FILENAME);
    if (Files.isRegularFile(inputTopologyPath)) {
      String topologyFileText = CommonUtil.readFile(inputTopologyPath);
      CommonUtil.writeFile(outputTopologyPath, topologyFileText);
    }
  }

  private void generateStubs(String inputRole, int stubAs, String interfaceDescriptionRegex) {
    // Map<String, Configuration> configs = loadConfigurations();
    // Pattern pattern = Pattern.compile(interfaceDescriptionRegex);
    // Map<String, Configuration> stubConfigurations = new TreeMap<>();
    //
    // _logger.info("\n*** GENERATING STUBS ***\n");
    // _logger.resetTimer();
    //
    // // load old node-roles to be updated at end
    // RoleSet stubRoles = new RoleSet();
    // stubRoles.add(STUB_ROLE);
    // Path nodeRolesPath = _settings.getNodeRolesPath();
    // _logger.info("Deserializing old node-roles mappings: \"" +
    // nodeRolesPath
    // + "\" ...");
    // NodeRoleMap nodeRoles = deserializeObject(nodeRolesPath,
    // NodeRoleMap.class);
    // _logger.info("OK\n");
    //
    // // create origination policy common to all stubs
    // String stubOriginationPolicyName = "~STUB_ORIGINATION_POLICY~";
    // PolicyMap stubOriginationPolicy = new PolicyMap(
    // stubOriginationPolicyName);
    // PolicyMapClause clause = new PolicyMapClause();
    // stubOriginationPolicy.getClauses().add(clause);
    // String stubOriginationRouteFilterListName =
    // "~STUB_ORIGINATION_ROUTE_FILTER~";
    // RouteFilterList rf = new RouteFilterList(
    // stubOriginationRouteFilterListName);
    // RouteFilterLine rfl = new RouteFilterLine(LineAction.ACCEPT,
    // Prefix.ZERO,
    // new SubRange(0, 0));
    // rf.addLine(rfl);
    // PolicyMapMatchRouteFilterListLine matchLine = new
    // PolicyMapMatchRouteFilterListLine(
    // Collections.singleton(rf));
    // clause.getMatchLines().add(matchLine);
    // clause.setAction(PolicyMapAction.PERMIT);
    //
    // Set<String> skipWarningNodes = new HashSet<>();
    //
    // for (Configuration config : configs.values()) {
    // if (!config.getRoles().contains(inputRole)) {
    // continue;
    // }
    // for (BgpNeighbor neighbor : config.getBgpProcess().getNeighbors()
    // .values()) {
    // if (!neighbor.getRemoteAs().equals(stubAs)) {
    // continue;
    // }
    // Prefix neighborPrefix = neighbor.getIp();
    // if (neighborPrefix.getPrefixLength() != 32) {
    // throw new BatfishException(
    // "do not currently handle generating stubs based on dynamic bgp
    // sessions");
    // }
    // Ip neighborAddress = neighborPrefix.getIp();
    // int edgeAs = neighbor.getLocalAs();
    // /*
    // * Now that we have the ip address of the stub, we want to find the
    // * interface that connects to it. We will extract the hostname for
    // * the stub from the description of this interface using the
    // * supplied regex.
    // */
    // boolean found = false;
    // for (Interface iface : config.getInterfaces().values()) {
    // Prefix prefix = iface.getIp();
    // if (prefix == null || !prefix.contains(neighborAddress)) {
    // continue;
    // }
    // // the neighbor address falls within the network assigned to this
    // // interface, so now we check the description
    // String description = iface.getDescription();
    // Matcher matcher = pattern.matcher(description);
    // if (matcher.find()) {
    // String hostname = matcher.group(1);
    // if (configs.containsKey(hostname)) {
    // Configuration duplicateConfig = configs.get(hostname);
    // if (!duplicateConfig.getRoles().contains(STUB_ROLE)
    // || duplicateConfig.getRoles().size() != 1) {
    // throw new BatfishException(
    // "A non-generated node with hostname: \""
    // + hostname
    // + "\" already exists in network under analysis");
    // }
    // else {
    // if (!skipWarningNodes.contains(hostname)) {
    // _logger
    // .warn("WARNING: Overwriting previously generated node: \""
    // + hostname + "\"\n");
    // skipWarningNodes.add(hostname);
    // }
    // }
    // }
    // found = true;
    // Configuration stub = stubConfigurations.get(hostname);
    //
    // // create stub if it doesn't exist yet
    // if (stub == null) {
    // stub = new Configuration(hostname);
    // stubConfigurations.put(hostname, stub);
    // // create flow sink interface for stub with common deatils
    // String flowSinkName = "TenGibabitEthernet100/100";
    // Interface flowSink = new Interface(flowSinkName, stub);
    // flowSink.setAddress(Prefix.ZERO);
    // flowSink.setActive(true);
    // flowSink.setBandwidth(10E9d);
    //
    // stub.getInterfaces().put(flowSinkName, flowSink);
    // stub.setBgpProcess(new BgpProcess());
    // stub.getPolicyMaps().put(stubOriginationPolicyName,
    // stubOriginationPolicy);
    // stub.getRouteFilterLists()
    // .put(stubOriginationRouteFilterListName, rf);
    // stub.setConfigurationFormat(ConfigurationFormat.CISCO);
    // stub.setRoles(stubRoles);
    // nodeRoles.put(hostname, stubRoles);
    // }
    //
    // // create interface that will on which peering will occur
    // Map<String, Interface> stubInterfaces = stub.getInterfaces();
    // String stubInterfaceName = "TenGigabitEthernet0/"
    // + (stubInterfaces.size() - 1);
    // Interface stubInterface = new Interface(stubInterfaceName,
    // stub);
    // stubInterfaces.put(stubInterfaceName, stubInterface);
    // stubInterface.setAddress(
    // new Prefix(neighborAddress, prefix.getPrefixLength()));
    // stubInterface.setActive(true);
    // stubInterface.setBandwidth(10E9d);
    //
    // // create neighbor within bgp process
    // BgpNeighbor edgeNeighbor = new BgpNeighbor(prefix, stub);
    // edgeNeighbor.getOriginationPolicies()
    // .add(stubOriginationPolicy);
    // edgeNeighbor.setRemoteAs(edgeAs);
    // edgeNeighbor.setLocalAs(stubAs);
    // edgeNeighbor.setSendCommunity(true);
    // edgeNeighbor.setDefaultMetric(0);
    // stub.getBgpProcess().getNeighbors()
    // .put(edgeNeighbor.getIp(), edgeNeighbor);
    // break;
    // }
    // else {
    // throw new BatfishException(
    // "Unable to derive stub hostname from interface description: \""
    // + description + "\" using regex: \""
    // + interfaceDescriptionRegex + "\"");
    // }
    // }
    // if (!found) {
    // throw new BatfishException(
    // "Could not determine stub hostname corresponding to ip: \""
    // + neighborAddress.toString()
    // + "\" listed as neighbor on router: \""
    // + config.getHostname() + "\"");
    // }
    // }
    // }
    // // write updated node-roles mappings to disk
    // _logger.info("Serializing updated node-roles mappings: \"" +
    // nodeRolesPath
    // + "\" ...");
    // serializeObject(nodeRoles, nodeRolesPath);
    // _logger.info("OK\n");
    // _logger.printElapsedTime();
    //
    // // write stubs to disk
    // serializeIndependentConfigs(stubConfigurations,
    // _testrigSettings.getSerializeIndependentPath());
  }

  @Override
  public Map<String, BiFunction<Question, IBatfish, Answerer>> getAnswererCreators() {
    return _answererCreators;
  }

  public TestrigSettings getBaseTestrigSettings() {
    return _baseTestrigSettings;
  }

  public Map<String, Configuration> getConfigurations(
      Path serializedVendorConfigPath, ConvertConfigurationAnswerElement answerElement) {

    //serializedVendorConfigPath即为解析完的配置文件
    //反序列化配置文件
    Map<String, GenericConfigObject> vendorConfigurations =
        deserializeVendorConfigurations(serializedVendorConfigPath);


    Map<String, Configuration> configurations =
        convertConfigurations(vendorConfigurations, answerElement);

    postProcessConfigurations(configurations.values());
    return configurations;
  }

  @Override
  public String getContainerName() {
    return _settings.getContainerDir().getFileName().toString();
  }

  @Override
  public DataPlanePlugin getDataPlanePlugin() {
    DataPlanePlugin plugin = _dataPlanePlugins.get(_settings.getDataPlaneEngineName());
    if (plugin == null) {
      throw new BatfishException(
          String.format(
              "Dataplane engine %s is unavailable or unsupported",
              _settings.getDataPlaneEngineName()));
    }
    return plugin;
  }

  @Override
  public DataPlanePluginSettings getDataPlanePluginSettings() {
    return _settings;
  }

  @Override
  public String getDifferentialFlowTag() {
    // return _settings.getQuestionName() + ":" +
    // _baseTestrigSettings.getEnvName()
    // + ":" + _baseTestrigSettings.getEnvironmentSettings().getEnvName()
    // + ":" + _deltaTestrigSettings.getEnvName() + ":"
    // + _deltaTestrigSettings.getEnvironmentSettings().getEnvName();
    return DIFFERENTIAL_FLOW_TAG;
  }

  @Nonnull
  private SortedSet<Edge> getEdgeBlacklist() {
    SortedSet<Edge> blacklistEdges = Collections.emptySortedSet();
    Path edgeBlacklistPath = _testrigSettings.getEnvironmentSettings().getEdgeBlacklistPath();
    if (edgeBlacklistPath != null && Files.exists(edgeBlacklistPath)) {
      blacklistEdges = parseEdgeBlacklist(edgeBlacklistPath);
    }
    return blacklistEdges;
  }

  @Override
  public Environment getEnvironment() {
    SortedSet<Edge> edgeBlackList = getEdgeBlacklist();
    SortedSet<NodeInterfacePair> interfaceBlackList = getInterfaceBlacklist();
    SortedSet<String> nodeBlackList = getNodeBlacklist();
    // TODO: add bgp tables and external announcements as well
    return new Environment(
        getEnvironmentName(),
        getTestrigName(),
        edgeBlackList,
        interfaceBlackList,
        nodeBlackList,
        null,
        null,
        null);
  }

  private SortedMap<String, BgpAdvertisementsByVrf> getEnvironmentBgpTables(
      Path inputPath, ParseEnvironmentBgpTablesAnswerElement answerElement) {
    if (Files.exists(inputPath.getParent()) && !Files.exists(inputPath)) {
      return new TreeMap<>();
    }
    SortedMap<Path, String> inputData = readFiles(inputPath, "Environment BGP Tables");
    SortedMap<String, BgpAdvertisementsByVrf> bgpTables =
        parseEnvironmentBgpTables(inputData, answerElement);
    return bgpTables;
  }

  public String getEnvironmentName() {
    return _testrigSettings.getEnvironmentSettings().getName();
  }

  private SortedMap<String, RoutesByVrf> getEnvironmentRoutingTables(
      Path inputPath, ParseEnvironmentRoutingTablesAnswerElement answerElement) {
    if (Files.exists(inputPath.getParent()) && !Files.exists(inputPath)) {
      return new TreeMap<>();
    }
    SortedMap<Path, String> inputData = readFiles(inputPath, "Environment Routing Tables");
    SortedMap<String, RoutesByVrf> routingTables =
        parseEnvironmentRoutingTables(inputData, answerElement);
    return routingTables;
  }

  @Override
  public Topology getEnvironmentTopology() {
    try {
      return BatfishObjectMapper.mapper()
          .readValue(
              CommonUtil.readFile(
                  _testrigSettings.getEnvironmentSettings().getSerializedTopologyPath()),
              Topology.class);
    } catch (IOException e) {
      throw new BatfishException("Could not getEnvironmentTopology: ", e);
    }
  }

  @Override
  public String getFlowTag() {
    return getFlowTag(_testrigSettings);
  }

  public String getFlowTag(TestrigSettings testrigSettings) {
    // return _settings.getQuestionName() + ":" + testrigSettings.getEnvName() +
    // ":"
    // + testrigSettings.getEnvironmentSettings().getEnvName();
    if (testrigSettings == _deltaTestrigSettings) {
      return DELTA_TESTRIG_TAG;
    } else if (testrigSettings == _baseTestrigSettings) {
      return BASE_TESTRIG_TAG;
    } else {
      throw new BatfishException("Could not determine flow tag");
    }
  }

  @Override
  public GrammarSettings getGrammarSettings() {
    return _settings;
  }

  @Override
  public FlowHistory getHistory() {
    FlowHistory flowHistory = new FlowHistory();
    if (_settings.getDiffQuestion()) {
      String flowTag = getDifferentialFlowTag();
      // String baseEnvTag = _baseTestrigSettings.getEnvName() + ":"
      // + _baseTestrigSettings.getEnvironmentSettings().getEnvName();
      String baseEnvTag = getFlowTag(_baseTestrigSettings);
      // String deltaName = _deltaTestrigSettings.getEnvName() + ":"
      // + _deltaTestrigSettings.getEnvironmentSettings().getEnvName();
      String deltaEnvTag = getFlowTag(_deltaTestrigSettings);
      pushBaseEnvironment();
      Environment baseEnv = getEnvironment();
      populateFlowHistory(flowHistory, baseEnvTag, baseEnv, flowTag);
      popEnvironment();
      pushDeltaEnvironment();
      Environment deltaEnv = getEnvironment();
      populateFlowHistory(flowHistory, deltaEnvTag, deltaEnv, flowTag);
      popEnvironment();
    } else {
      String flowTag = getFlowTag();
      // String name = testrigSettings.getEnvName() + ":"
      // + testrigSettings.getEnvironmentSettings().getEnvName();
      String envTag = flowTag;
      Environment env = getEnvironment();
      populateFlowHistory(flowHistory, envTag, env, flowTag);
    }
    _logger.debug(flowHistory.toString());
    return flowHistory;
  }

  @Nonnull
  private SortedSet<NodeInterfacePair> getInterfaceBlacklist() {
    SortedSet<NodeInterfacePair> blacklistInterfaces = Collections.emptySortedSet();
    Path interfaceBlacklistPath =
        _testrigSettings.getEnvironmentSettings().getInterfaceBlacklistPath();
    if (interfaceBlacklistPath != null && Files.exists(interfaceBlacklistPath)) {
      blacklistInterfaces = parseInterfaceBlacklist(interfaceBlacklistPath);
    }
    return blacklistInterfaces;
  }

  @Override
  public BatfishLogger getLogger() {
    return _logger;
  }

  @Nonnull
  private SortedSet<String> getNodeBlacklist() {
    SortedSet<String> blacklistNodes = Collections.emptySortedSet();
    Path nodeBlacklistPath = _testrigSettings.getEnvironmentSettings().getNodeBlacklistPath();
    if (nodeBlacklistPath != null && Files.exists(nodeBlacklistPath)) {
      blacklistNodes = parseNodeBlacklist(nodeBlacklistPath);
    }
    return blacklistNodes;
  }

  /**
   * Gets the {@link NodeRolesData} for the testrig
   *
   * @return The {@link NodeRolesData} object.
   */
  @Override
  public NodeRolesData getNodeRolesData() {
    Path nodeRoleDataPath = _settings.getContainerDir().resolve(BfConsts.RELPATH_NODE_ROLES_PATH);
    try {
      return NodeRolesData.read(nodeRoleDataPath);
    } catch (IOException e) {
      _logger.errorf("Could not read roles data from %s: %s", nodeRoleDataPath, e);
      return null;
    }
  }

  /**
   * Gets the {@link NodeRoleDimension} object give dimension name
   *
   * @param dimension The dimension name
   * @return The {@link NodeRoleDimension} object. Throws {@link java.util.NoSuchElementException}
   *     if the dimension with the provided name does not exist
   */
  @Override
  public NodeRoleDimension getNodeRoleDimension(String dimension) {
    Path nodeRoleDataPath = _settings.getContainerDir().resolve(BfConsts.RELPATH_NODE_ROLES_PATH);
    NodeRoleDimension nodeRoleDimension = null;
    try {
      nodeRoleDimension = NodeRolesData.getNodeRoleDimensionByName(nodeRoleDataPath, dimension);
    } catch (IOException e) {
      _logger.errorf("Could not read roles data from %s: %s", nodeRoleDataPath, e);
    }
    return nodeRoleDimension;
  }

  @Override
  public Map<String, String> getQuestionTemplates() {
    if (_settings.getCoordinatorHost() == null) {
      throw new BatfishException("Cannot get question templates: coordinator host is not set");
    }
    String protocol = _settings.getSslDisable() ? "http" : "https";
    String url =
        String.format(
            "%s://%s:%s%s/%s",
            protocol,
            _settings.getCoordinatorHost(),
            _settings.getCoordinatorPoolPort(),
            CoordConsts.SVC_CFG_POOL_MGR,
            CoordConsts.SVC_RSC_POOL_GET_QUESTION_TEMPLATES);
    Map<String, String> params = new HashMap<>();
    params.put(CoordConsts.SVC_KEY_VERSION, Version.getVersion());

    JSONObject response = (JSONObject) Driver.talkToCoordinator(url, params, _logger);
    if (response == null) {
      throw new BatfishException("Could not get question templates: Got null response");
    }
    if (!response.has(CoordConsts.SVC_KEY_QUESTION_LIST)) {
      throw new BatfishException("Could not get question templates: Response lacks question list");
    }

    try {
      Map<String, String> templates =
          BatfishObjectMapper.mapper()
              .readValue(
                  response.get(CoordConsts.SVC_KEY_QUESTION_LIST).toString(),
                  new TypeReference<Map<String, String>>() {});
      return templates;
    } catch (JSONException | IOException e) {
      throw new BatfishException("Could not cast response to Map: ", e);
    }
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getRoutes(
      boolean useCompression) {
    return getDataPlanePlugin().getRoutes(loadDataPlane(useCompression));
  }

  public Settings getSettings() {
    return _settings;
  }

  @Override
  public ImmutableConfiguration getSettingsConfiguration() {
    return _settings.getImmutableConfiguration();
  }

  private Snapshot getSnapshot() {
    return new Snapshot(
        _testrigSettings.getName(), _testrigSettings.getEnvironmentSettings().getName());
  }

  private Set<Edge> getSymmetricEdgePairs(SortedSet<Edge> edges) {
    Set<Edge> consumedEdges = new LinkedHashSet<>();
    for (Edge edge : edges) {
      if (consumedEdges.contains(edge)) {
        continue;
      }
      Edge reverseEdge = new Edge(edge.getInterface2(), edge.getInterface1());
      consumedEdges.add(edge);
      consumedEdges.add(reverseEdge);
    }
    return consumedEdges;
  }

  @Override
  public String getTaskId() {
    return _settings.getTaskId();
  }

  public String getTerminatingExceptionMessage() {
    return _terminatingExceptionMessage;
  }

  @Override
  public Directory getTestrigFileTree() {
    Path trPath = _testrigSettings.getTestRigPath();
    Directory dir = new Directory(trPath);
    return dir;
  }

  @Override
  public String getTestrigName() {
    return _testrigSettings.getName();
  }

  public TestrigSettings getTestrigSettings() {
    return _testrigSettings;
  }

  @Override
  public PluginClientType getType() {
    return PluginClientType.BATFISH;
  }

  private void histogram(Path testRigPath) {
    Map<Path, String> configurationData =
        readConfigurationFiles(testRigPath, BfConsts.RELPATH_CONFIGURATIONS_DIR);
    // todo: either remove histogram function or do something userful with
    // answer
    Map<String, VendorConfiguration> vendorConfigurations =
        parseVendorConfigurations(
            configurationData,
            new ParseVendorConfigurationAnswerElement(),
            ConfigurationFormat.UNKNOWN);
    _logger.info("Building feature histogram...");
    MultiSet<String> histogram = new TreeMultiSet<>();
    for (VendorConfiguration vc : vendorConfigurations.values()) {
      Set<String> unimplementedFeatures = vc.getUnimplementedFeatures();
      histogram.add(unimplementedFeatures);
    }
    _logger.info("OK\n");
    for (String feature : histogram.elements()) {
      int count = histogram.count(feature);
      _logger.outputf("%s: %s\n", feature, count);
    }
  }

  private void initAnalysisQuestionPath(String analysisName, String questionName) {
    Path questionDir =
        _testrigSettings
            .getBasePath()
            .resolve(
                Paths.get(
                        BfConsts.RELPATH_ANALYSES_DIR,
                        analysisName,
                        BfConsts.RELPATH_QUESTIONS_DIR,
                        questionName)
                    .toString());
    questionDir.toFile().mkdirs();
    Path questionPath = questionDir.resolve(BfConsts.RELPATH_QUESTION_FILE);
    _settings.setQuestionPath(questionPath);
  }

  @Override
  public void initBgpAdvertisements(Map<String, Configuration> configurations) {
    Set<BgpAdvertisement> globalBgpAdvertisements = getDataPlanePlugin().getAdvertisements();
    for (Configuration node : configurations.values()) {
      node.initBgpAdvertisements();
      for (Vrf vrf : node.getVrfs().values()) {
        vrf.initBgpAdvertisements();
      }
    }
    for (BgpAdvertisement bgpAdvertisement : globalBgpAdvertisements) {
      BgpAdvertisementType type = bgpAdvertisement.getType();
      String srcVrf = bgpAdvertisement.getSrcVrf();
      String dstVrf = bgpAdvertisement.getDstVrf();
      switch (type) {
        case EBGP_ORIGINATED:
          {
            String originationNodeName = bgpAdvertisement.getSrcNode();
            Configuration originationNode = configurations.get(originationNodeName);
            if (originationNode != null) {
              originationNode.getBgpAdvertisements().add(bgpAdvertisement);
              originationNode.getOriginatedAdvertisements().add(bgpAdvertisement);
              originationNode.getOriginatedEbgpAdvertisements().add(bgpAdvertisement);
              Vrf originationVrf = originationNode.getVrfs().get(srcVrf);
              originationVrf.getBgpAdvertisements().add(bgpAdvertisement);
              originationVrf.getOriginatedAdvertisements().add(bgpAdvertisement);
              originationVrf.getOriginatedEbgpAdvertisements().add(bgpAdvertisement);
            } else {
              throw new BatfishException(
                  "Originated bgp advertisement refers to missing node: \""
                      + originationNodeName
                      + "\"");
            }
            break;
          }

        case IBGP_ORIGINATED:
          {
            String originationNodeName = bgpAdvertisement.getSrcNode();
            Configuration originationNode = configurations.get(originationNodeName);
            if (originationNode != null) {
              originationNode.getBgpAdvertisements().add(bgpAdvertisement);
              originationNode.getOriginatedAdvertisements().add(bgpAdvertisement);
              originationNode.getOriginatedIbgpAdvertisements().add(bgpAdvertisement);
              Vrf originationVrf = originationNode.getVrfs().get(srcVrf);
              originationVrf.getBgpAdvertisements().add(bgpAdvertisement);
              originationVrf.getOriginatedAdvertisements().add(bgpAdvertisement);
              originationVrf.getOriginatedIbgpAdvertisements().add(bgpAdvertisement);
            } else {
              throw new BatfishException(
                  "Originated bgp advertisement refers to missing node: \""
                      + originationNodeName
                      + "\"");
            }
            break;
          }

        case EBGP_RECEIVED:
          {
            String recevingNodeName = bgpAdvertisement.getDstNode();
            Configuration receivingNode = configurations.get(recevingNodeName);
            if (receivingNode != null) {
              receivingNode.getBgpAdvertisements().add(bgpAdvertisement);
              receivingNode.getReceivedAdvertisements().add(bgpAdvertisement);
              receivingNode.getReceivedEbgpAdvertisements().add(bgpAdvertisement);
              Vrf receivingVrf = receivingNode.getVrfs().get(dstVrf);
              receivingVrf.getBgpAdvertisements().add(bgpAdvertisement);
              receivingVrf.getReceivedAdvertisements().add(bgpAdvertisement);
              receivingVrf.getReceivedEbgpAdvertisements().add(bgpAdvertisement);
            }
            break;
          }

        case IBGP_RECEIVED:
          {
            String recevingNodeName = bgpAdvertisement.getDstNode();
            Configuration receivingNode = configurations.get(recevingNodeName);
            if (receivingNode != null) {
              receivingNode.getBgpAdvertisements().add(bgpAdvertisement);
              receivingNode.getReceivedAdvertisements().add(bgpAdvertisement);
              receivingNode.getReceivedIbgpAdvertisements().add(bgpAdvertisement);
              Vrf receivingVrf = receivingNode.getVrfs().get(dstVrf);
              receivingVrf.getBgpAdvertisements().add(bgpAdvertisement);
              receivingVrf.getReceivedAdvertisements().add(bgpAdvertisement);
              receivingVrf.getReceivedIbgpAdvertisements().add(bgpAdvertisement);
            }
            break;
          }

        case EBGP_SENT:
          {
            String sendingNodeName = bgpAdvertisement.getSrcNode();
            Configuration sendingNode = configurations.get(sendingNodeName);
            if (sendingNode != null) {
              sendingNode.getBgpAdvertisements().add(bgpAdvertisement);
              sendingNode.getSentAdvertisements().add(bgpAdvertisement);
              sendingNode.getSentEbgpAdvertisements().add(bgpAdvertisement);
              Vrf sendingVrf = sendingNode.getVrfs().get(srcVrf);
              sendingVrf.getBgpAdvertisements().add(bgpAdvertisement);
              sendingVrf.getSentAdvertisements().add(bgpAdvertisement);
              sendingVrf.getSentEbgpAdvertisements().add(bgpAdvertisement);
            }
            break;
          }

        case IBGP_SENT:
          {
            String sendingNodeName = bgpAdvertisement.getSrcNode();
            Configuration sendingNode = configurations.get(sendingNodeName);
            if (sendingNode != null) {
              sendingNode.getBgpAdvertisements().add(bgpAdvertisement);
              sendingNode.getSentAdvertisements().add(bgpAdvertisement);
              sendingNode.getSentIbgpAdvertisements().add(bgpAdvertisement);
              Vrf sendingVrf = sendingNode.getVrfs().get(srcVrf);
              sendingVrf.getBgpAdvertisements().add(bgpAdvertisement);
              sendingVrf.getSentAdvertisements().add(bgpAdvertisement);
              sendingVrf.getSentIbgpAdvertisements().add(bgpAdvertisement);
            }
            break;
          }

        default:
          throw new BatfishException("Invalid bgp advertisement type");
      }
    }
  }

  @Override
  public void initBgpOriginationSpaceExplicit(Map<String, Configuration> configurations) {
    // ProtocolDependencyAnalysis protocolDependencyAnalysis = new
    // ProtocolDependencyAnalysis(
    // configurations);
    // DependencyDatabase database = protocolDependencyAnalysis
    // .getDependencyDatabase();
    //
    // for (Entry<String, Configuration> e : configurations.entrySet()) {
    // PrefixSpace ebgpExportSpace = new PrefixSpace();
    // String name = e.getKey();
    // Configuration node = e.getValue();
    // BgpProcess proc = node.getBgpProcess();
    // if (proc != null) {
    // Set<PotentialExport> bgpExports = database.getPotentialExports(name,
    // RoutingProtocol.BGP);
    // for (PotentialExport export : bgpExports) {
    // DependentRoute exportSourceRoute = export.getDependency();
    // if (!exportSourceRoute.dependsOn(RoutingProtocol.BGP)
    // && !exportSourceRoute.dependsOn(RoutingProtocol.IBGP)) {
    // Prefix prefix = export.getIp();
    // ebgpExportSpace.addPrefix(prefix);
    // }
    // }
    // proc.setOriginationSpace(ebgpExportSpace);
    // }
    // }
  }

  @Override
  public InitInfoAnswerElement initInfo(boolean summary, boolean verboseError) {
    ParseVendorConfigurationAnswerElement parseAnswer = loadParseVendorConfigurationAnswerElement();
    InitInfoAnswerElement answerElement = mergeParseAnswer(summary, verboseError, parseAnswer);
    mergeConvertAnswer(summary, verboseError, answerElement);
    _logger.info(answerElement.prettyPrint());
    return answerElement;
  }

  @Override
  public InitInfoAnswerElement initInfoBgpAdvertisements(boolean summary, boolean verboseError) {
    ParseEnvironmentBgpTablesAnswerElement parseAnswer =
        loadParseEnvironmentBgpTablesAnswerElement();
    InitInfoAnswerElement answerElement = mergeParseAnswer(summary, verboseError, parseAnswer);
    _logger.info(answerElement.prettyPrint());
    return answerElement;
  }

  @Override
  public InitInfoAnswerElement initInfoRoutes(boolean summary, boolean verboseError) {
    ParseEnvironmentRoutingTablesAnswerElement parseAnswer =
        loadParseEnvironmentRoutingTablesAnswerElement();
    InitInfoAnswerElement answerElement = mergeParseAnswer(summary, verboseError, parseAnswer);
    _logger.info(answerElement.prettyPrint());
    return answerElement;
  }

  private void initQuestionEnvironment(Question question, boolean dp, boolean differentialContext) {
    EnvironmentSettings envSettings = _testrigSettings.getEnvironmentSettings();
    if (!environmentExists(_testrigSettings)) {
      Path envPath = envSettings.getEnvPath();
      // create environment required folders
      CommonUtil.createDirectories(envPath);
    }
    if (!environmentBgpTablesExist(envSettings)) {
      computeEnvironmentBgpTables();
    }
    if (!environmentRoutingTablesExist(envSettings)) {
      computeEnvironmentRoutingTables();
    }
    if (dp) {
      if (!dataPlaneDependenciesExist(_testrigSettings)) {
        computeDataPlane(differentialContext);
      }

      if (!compressedDataPlaneDependenciesExist(_testrigSettings)) {
        // computeCompressedDataPlane();
      }
    }
  }

  private void initQuestionEnvironments(
      Question question, boolean diff, boolean diffActive, boolean dp) {
    if (diff || !diffActive) {
      pushBaseEnvironment();
      initQuestionEnvironment(question, dp, false);
      popEnvironment();
    }
    if (diff || diffActive) {
      pushDeltaEnvironment();
      initQuestionEnvironment(question, dp, true);
      popEnvironment();
    }
  }

  @Override
  public void initRemoteRipNeighbors(
      Map<String, Configuration> configurations, Map<Ip, Set<String>> ipOwners, Topology topology) {
    for (Entry<String, Configuration> e : configurations.entrySet()) {
      String hostname = e.getKey();
      Configuration c = e.getValue();
      for (Entry<String, Vrf> e2 : c.getVrfs().entrySet()) {
        Vrf vrf = e2.getValue();
        RipProcess proc = vrf.getRipProcess();
        if (proc != null) {
          proc.setRipNeighbors(new TreeMap<>());
          String vrfName = e2.getKey();
          for (String ifaceName : proc.getInterfaces()) {
            Interface iface = vrf.getInterfaces().get("ifaceName");
            SortedSet<Edge> ifaceEdges =
                topology.getInterfaceEdges().get(new NodeInterfacePair(hostname, ifaceName));
            boolean hasNeighbor = false;
            Ip localIp = iface.getAddress().getIp();
            if (ifaceEdges != null) {
              for (Edge edge : ifaceEdges) {
                if (edge.getNode1().equals(hostname)) {
                  String remoteHostname = edge.getNode2();
                  String remoteIfaceName = edge.getInt2();
                  Configuration remoteNode = configurations.get(remoteHostname);
                  Interface remoteIface = remoteNode.getInterfaces().get(remoteIfaceName);
                  Vrf remoteVrf = remoteIface.getVrf();
                  String remoteVrfName = remoteVrf.getName();
                  RipProcess remoteProc = remoteVrf.getRipProcess();
                  if (remoteProc != null) {
                    if (remoteProc.getRipNeighbors() == null) {
                      remoteProc.setRipNeighbors(new TreeMap<>());
                    }
                    if (remoteProc.getInterfaces().contains(remoteIfaceName)) {
                      Ip remoteIp = remoteIface.getAddress().getIp();
                      Pair<Ip, Ip> localKey = new Pair<>(localIp, remoteIp);
                      RipNeighbor neighbor = proc.getRipNeighbors().get(localKey);
                      if (neighbor == null) {
                        hasNeighbor = true;

                        // initialize local neighbor
                        neighbor = new RipNeighbor(localKey);
                        neighbor.setVrf(vrfName);
                        neighbor.setOwner(c);
                        neighbor.setInterface(iface);
                        proc.getRipNeighbors().put(localKey, neighbor);

                        // initialize remote neighbor
                        Pair<Ip, Ip> remoteKey = new Pair<>(remoteIp, localIp);
                        RipNeighbor remoteNeighbor = new RipNeighbor(remoteKey);
                        remoteNeighbor.setVrf(remoteVrfName);
                        remoteNeighbor.setOwner(remoteNode);
                        remoteNeighbor.setInterface(remoteIface);
                        remoteProc.getRipNeighbors().put(remoteKey, remoteNeighbor);

                        // link neighbors
                        neighbor.setRemoteRipNeighbor(remoteNeighbor);
                        remoteNeighbor.setRemoteRipNeighbor(neighbor);
                      }
                    }
                  }
                }
              }
            }
            if (!hasNeighbor) {
              Pair<Ip, Ip> key = new Pair<>(localIp, Ip.ZERO);
              RipNeighbor neighbor = new RipNeighbor(key);
              neighbor.setVrf(vrfName);
              neighbor.setOwner(c);
              neighbor.setInterface(iface);
              proc.getRipNeighbors().put(key, neighbor);
            }
          }
        }
      }
    }
  }

  @Override
  public SortedMap<String, Configuration> loadConfigurations() {
    Snapshot snapshot = getSnapshot();
    _logger.debugf("Loading configurations for %s\n", snapshot);
    return loadConfigurations(snapshot);
  }

  private SortedMap<String, Configuration> loadCompressedConfigurations(Snapshot snapshot) {
    // Do we already have configurations in the cache?
    SortedMap<String, Configuration> configurations =
        _cachedCompressedConfigurations.getIfPresent(snapshot);
    if (configurations != null) {
      return configurations;
    }
    _logger.debugf("Loading configurations for %s, cache miss", snapshot);

    // Next, see if we have an up-to-date, environment-specific configurations on disk.
    configurations = _storage.loadCompressedConfigurations(snapshot.getTestrig());
    if (configurations != null) {
      return configurations;
    } else {
      computeCompressedDataPlane();
      configurations = _cachedCompressedConfigurations.getIfPresent(snapshot);
      if (configurations == null) {
        throw new BatfishException("Could not compute compressed configs");
      }
      return configurations;
    }
  }

  /**
   * Returns the configurations for given snapshot, which including any environment-specific
   * features.
   */
  private SortedMap<String, Configuration> loadConfigurations(Snapshot snapshot) {
    // Do we already have configurations in the cache?
    SortedMap<String, Configuration> configurations = _cachedConfigurations.getIfPresent(snapshot);
    if (configurations != null) {
      return configurations;
    }
    _logger.debugf("Loading configurations for %s, cache miss", snapshot);

    // Next, see if we have an up-to-date, environment-specific configurations on disk.
    configurations = _storage.loadConfigurations(snapshot.getTestrig());
    if (configurations != null) {
      _logger.debugf("Loaded configurations for %s off disk", snapshot);
      applyEnvironment(configurations);
    } else {
      // Otherwise, we have to parse the configurations. Fall back to old, hacky code.
      configurations = parseConfigurationsAndApplyEnvironment();
    }

    _cachedConfigurations.put(snapshot, configurations);
    return configurations;
  }

  @Nonnull
  private SortedMap<String, Configuration> parseConfigurationsAndApplyEnvironment() {
    _logger.infof("Repairing configurations for testrig %s", _testrigSettings.getName());
    repairConfigurations();
    SortedMap<String, Configuration> configurations =
        _storage.loadConfigurations(_testrigSettings.getName());
    Verify.verify(
        configurations != null,
        "Configurations should not be null when loaded immediately after repair.");
    applyEnvironment(configurations);
    return configurations;
  }

  @Override
  public ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElementOrReparse() {
    ConvertConfigurationAnswerElement ccae =
        _storage.loadConvertConfigurationAnswerElement(_testrigSettings.getName());
    if (ccae != null
        && Version.isCompatibleVersion(
            "Service", "Old processed configurations", ccae.getVersion())) {
      return ccae;
    }

    repairConfigurations();
    ccae = _storage.loadConvertConfigurationAnswerElement(_testrigSettings.getName());
    if (ccae != null
        && Version.isCompatibleVersion(
            "Service", "Old processed configurations", ccae.getVersion())) {
      return ccae;
    } else {
      throw new BatfishException(
          "Version error repairing configurations for convert configuration answer element");
    }
  }

  @Override
  public DataPlane loadDataPlane() {
    return loadDataPlane(false);
  }

  @Override
  public void nodeReachAbility(String srcNode,String dstNode)
  {
//    IncrementalDataPlane dp=(IncrementalDataPlane) loadDataPlane();
//    HashMap<String,Map<Integer,List<SymBgpRoute>>> rib=dp.getBgpRib();
//    List<SymPrefixList> dstPrefix=dp._prefixOwners.get(dstNode);
////    for(String name:prefixOwners.keySet())
////    {
////      System.out.println("node:"+name);
////      for(int i=0;i<prefixOwners.get(name).size();i++)
////      {
////        for(BDD ip:prefixOwners.get(name).get(i)._prefixList.keySet())
////        {
////          System.out.println("qian zhui:");
////          ip.printSet();
////          System.out.println("chang du:"+prefixOwners.get(name).get(i)._prefixList.get(ip));
////        }
////      }
////    }
//    Map<Integer,List<SymBgpRoute>> srcRib=dp.getBgpRib().get(srcNode);
//    for(SymPrefixList prefix:dstPrefix)
//    {
//      if(!srcRib.keySet().contains(prefix))
//      {
//        System.out.println("zhen dui yu qian zhui:");
//        for(BDD ip:prefix._prefixList.keySet())
//        {
//          ip.printSet();
//          System.out.println("chang du:"+prefix._prefixList.get(ip));
//        }
//        System.out.println("bu ke da!");
//        continue;
//      }
//      List<SymBgpRoute> initalRoute=srcRib.get(prefix);
//      List<SymBgpRoute> routes=new ArrayList<>();
//      BDDFactory factory=initalRoute.get(0).getTopologyFactory();
//      BDD newTopologyC=factory.one();
//      for(int i=0;i<initalRoute.size();i++)
//      {
//        if(!newTopologyC.and(initalRoute.get(i).getTopologyCondition()).equals(factory.zero()))
//        {
//          SymBgpRoute temp=cloneBgpRoute(initalRoute.get(i));
//          temp.setTopologyCondition(newTopologyC.and(initalRoute.get(i).getTopologyCondition()));
//          routes.add(temp);
//        }
//        newTopologyC=newTopologyC.and(initalRoute.get(i).getTopologyCondition().not());
//        if(newTopologyC.equals(factory.zero()))
//        {
//          break;
//        }
//      }
//      int firstExternalPos=-1;
//      int firstInternalPos=-1;
//      int secondExternalPos=-1;
//      for(int i=0;i<routes.size();i++)
//      {
//        if(routes.get(i)._external&&firstExternalPos==-1)
//        {
//          firstExternalPos=i;
//        }else if(!routes.get(i)._external&&firstInternalPos==-1)
//        {
//          firstInternalPos=i;
//        }else if(routes.get(i)._external&&firstExternalPos!=-1&&firstInternalPos!=-1)
//        {
//          secondExternalPos=i;
//          break;
//        }
//      }
//      if(firstExternalPos == -1)
//      {
//        for(int j=0;j<routes.size();j++)
//        {
//          System.out.println("---------------"+"ke da nei bu lu you:"+j+"---------------");
//          System.out.println("yuan jie dian:"+routes.get(j).getOriginatorIp());
//          System.out.println("as-path:"+routes.get(j).getAsPath().getAsPathString());
//          System.out.println("localpreference:lower:"+routes.get(j).getLocalPreference().getLowerbound()+";upper:"+routes.get(j).getLocalPreference().getUpperbound());
//          System.out.println("as-pathSize:lower:"+routes.get(j).getAsPathSize().getLowerbound()+";upper:"+routes.get(j).getAsPathSize().getUpperbound());
//          System.out.println("dui ying de tuo pu tiao jian:");
//          routes.get(j).getTopologyCondition().printSet();
//        }
//      }else if(firstExternalPos==0)
//      {
//        System.out.println("bu ke da!");
//        int lastExternalPos=firstInternalPos==-1?routes.size():firstInternalPos;
//        System.out.println("you yu wai bu lu you de cun zai:");
//        for(int j=0;j<lastExternalPos;j++)
//        {
//          System.out.println("---------------"+"wai bu lu you:"+j+"---------------");
//          System.out.println("yuan jie dian:"+routes.get(j).getOriginatorIp());
//          System.out.println("as-path:"+routes.get(j).getAsPath().getAsPathString());
//          System.out.println("localpreference:lower:"+routes.get(j).getLocalPreference().getLowerbound()+";upper:"+routes.get(j).getLocalPreference().getUpperbound());
//          System.out.println("as-pathSize:lower:"+routes.get(j).getAsPathSize().getLowerbound()+";upper:"+routes.get(j).getAsPathSize().getUpperbound());
//          System.out.println("dui ying de tuo pu tiao jian:");
//          routes.get(j).getTopologyCondition().printSet();
//        }
//        if(firstInternalPos==-1)
//        {
//          System.out.println("you yu wai bu lu you de cun zai,mei you nei bu lu you shi qi ke da!");
//        }else{
//          int lastInternalPos=secondExternalPos==-1?routes.size():secondExternalPos;
//          for(int j=firstInternalPos;j<lastInternalPos;j++)
//          {
//            System.out.println("---------------"+"ke da nei bu lu you:"+j+"---------------");
//            System.out.println("yuan jie dian:"+routes.get(j).getOriginatorIp());
//            System.out.println("as-path:"+routes.get(j).getAsPath().getAsPathString());
//            System.out.println("localpreference:lower:"+routes.get(j).getLocalPreference().getLowerbound()+";upper:"+routes.get(j).getLocalPreference().getUpperbound());
//            System.out.println("as-pathSize:lower:"+routes.get(j).getAsPathSize().getLowerbound()+";upper:"+routes.get(j).getAsPathSize().getUpperbound());
//            System.out.println("dui ying de tuo pu tiao jian:");
//            routes.get(j).getTopologyCondition().printSet();
//          }
//        }
//        if(secondExternalPos!=-1)
//        {
//          System.out.println("dang tuo po tiao jian wei ru xia qing kuang shi , reng hui dao xiang wai bu lu you:");
//          System.out.println("---------------"+"ke da nei bu lu you:"+secondExternalPos+"---------------");
//          System.out.println("yuan jie dian:"+routes.get(secondExternalPos).getOriginatorIp());
//          System.out.println("as-path:"+routes.get(secondExternalPos).getAsPath().getAsPathString());
//          System.out.println("localpreference:lower:"+routes.get(secondExternalPos).getLocalPreference().getLowerbound()+";upper:"+routes.get(secondExternalPos).getLocalPreference().getUpperbound());
//          System.out.println("as-pathSize:lower:"+routes.get(secondExternalPos).getAsPathSize().getLowerbound()+";upper:"+routes.get(secondExternalPos).getAsPathSize().getUpperbound());
//          System.out.println("dui ying de tuo pu tiao jian:");
//          routes.get(secondExternalPos).getTopologyCondition().printSet();
//
//        }
//      }else if(firstExternalPos != 0)
//      {
//        System.out.println("tong guo xia shu nei bu lu you ke da:");
//        for(int j=0;j<firstInternalPos;j++)
//        {
//          System.out.println("---------------"+"ke da nei bu lu you:"+j+"---------------");
//          System.out.println("yuan jie dian:"+routes.get(j).getOriginatorIp());
//          System.out.println("as-path:"+routes.get(j).getAsPath().getAsPathString());
//          System.out.println("localpreference:lower:"+routes.get(j).getLocalPreference().getLowerbound()+";upper:"+routes.get(j).getLocalPreference().getUpperbound());
//          System.out.println("as-pathSize:lower:"+routes.get(j).getAsPathSize().getLowerbound()+";upper:"+routes.get(j).getAsPathSize().getUpperbound());
//          System.out.println("dui ying de tuo pu tiao jian:");
//          routes.get(j).getTopologyCondition().printSet();
//        }
//
//        System.out.println("dang tuo po tiao jian wei ru xia qing kuang shi , reng hui dao xiang wai bu lu you:");
//        System.out.println("---------------"+"ke da nei bu lu you:"+firstExternalPos+"---------------");
//        System.out.println("yuan jie dian:"+routes.get(firstExternalPos).getOriginatorIp());
//        System.out.println("as-path:"+routes.get(firstExternalPos).getAsPath().getAsPathString());
//        System.out.println("localpreference:lower:"+routes.get(firstExternalPos).getLocalPreference().getLowerbound()+";upper:"+routes.get(firstExternalPos).getLocalPreference().getUpperbound());
//        System.out.println("as-pathSize:lower:"+routes.get(firstExternalPos).getAsPathSize().getLowerbound()+";upper:"+routes.get(firstExternalPos).getAsPathSize().getUpperbound());
//        System.out.println("dui ying de tuo pu tiao jian:");
//        routes.get(firstExternalPos).getTopologyCondition().printSet();
//      }
//    }
  }

  private DataPlane loadDataPlane(boolean compressed) {
    Cache<TestrigSettings, DataPlane> cache =
        compressed ? _cachedCompressedDataPlanes : _cachedDataPlanes;

    Path path =
        compressed
            ? _testrigSettings.getEnvironmentSettings().getCompressedDataPlanePath()
            : _testrigSettings.getEnvironmentSettings().getDataPlanePath();

    DataPlane dp = cache.getIfPresent(_testrigSettings);
    if (dp == null) {
      /*
       * Data plane should exist after loading answer element, as it triggers
       * repair if necessary. However, it might not be cached if it was not
       * repaired, so we still might need to load it from disk.
       */
      loadDataPlaneAnswerElement(compressed);
      dp = cache.getIfPresent(_testrigSettings);
      if (dp == null) {
        newBatch("Loading data plane from disk", 0);
        dp = deserializeObject(path, DataPlane.class);
        cache.put(_testrigSettings, dp);
      }
    }
    return dp;
  }

  private DataPlaneAnswerElement loadDataPlaneAnswerElement(boolean compressed) {
    return loadDataPlaneAnswerElement(compressed, true);
  }

  private DataPlaneAnswerElement loadDataPlaneAnswerElement(
      boolean compressed, boolean firstAttempt) {
    Path answerPath =
        compressed
            ? _testrigSettings.getEnvironmentSettings().getCompressedDataPlaneAnswerPath()
            : _testrigSettings.getEnvironmentSettings().getDataPlaneAnswerPath();

    DataPlaneAnswerElement bae = deserializeObject(answerPath, DataPlaneAnswerElement.class);
    if (!Version.isCompatibleVersion("Service", "Old data plane", bae.getVersion())) {
      if (firstAttempt) {
        repairDataPlane(compressed);
        return loadDataPlaneAnswerElement(compressed, false);
      } else {
        throw new BatfishException(
            "Version error repairing data plane for data plane answer element");
      }
    } else {
      return bae;
    }
  }

  @Override
  public SortedMap<String, BgpAdvertisementsByVrf> loadEnvironmentBgpTables() {
    EnvironmentSettings envSettings = _testrigSettings.getEnvironmentSettings();
    SortedMap<String, BgpAdvertisementsByVrf> environmentBgpTables =
        _cachedEnvironmentBgpTables.get(envSettings);
    if (environmentBgpTables == null) {
      ParseEnvironmentBgpTablesAnswerElement ae = loadParseEnvironmentBgpTablesAnswerElement();
      if (!Version.isCompatibleVersion(
          "Service", "Old processed environment BGP tables", ae.getVersion())) {
        repairEnvironmentBgpTables();
      }
      environmentBgpTables =
          deserializeEnvironmentBgpTables(envSettings.getSerializeEnvironmentBgpTablesPath());
      _cachedEnvironmentBgpTables.put(envSettings, environmentBgpTables);
    }
    return environmentBgpTables;
  }

  @Override
  public SortedMap<String, RoutesByVrf> loadEnvironmentRoutingTables() {
    EnvironmentSettings envSettings = _testrigSettings.getEnvironmentSettings();
    SortedMap<String, RoutesByVrf> environmentRoutingTables =
        _cachedEnvironmentRoutingTables.get(envSettings);
    if (environmentRoutingTables == null) {
      ParseEnvironmentRoutingTablesAnswerElement pertae =
          loadParseEnvironmentRoutingTablesAnswerElement();
      if (!Version.isCompatibleVersion(
          "Service", "Old processed environment routing tables", pertae.getVersion())) {
        repairEnvironmentRoutingTables();
      }
      environmentRoutingTables =
          deserializeEnvironmentRoutingTables(
              envSettings.getSerializeEnvironmentRoutingTablesPath());
      _cachedEnvironmentRoutingTables.put(envSettings, environmentRoutingTables);
    }
    return environmentRoutingTables;
  }

  private ForwardingAnalysis loadForwardingAnalysis(
      Map<String, Configuration> configurations, DataPlane dataPlane) {
    Topology topology = new Topology(dataPlane.getTopologyEdges());
    try {
      return _cachedForwardingAnalyses.get(
          _testrigSettings,
          () ->
              new ForwardingAnalysisImpl(
                  configurations, dataPlane.getRibs(), dataPlane.getFibs(), topology));
    } catch (ExecutionException e) {
      throw new BatfishException("error loading ForwardingAnalysis", e);
    }
  }

  @Override
  public ParseEnvironmentBgpTablesAnswerElement loadParseEnvironmentBgpTablesAnswerElement() {
    return loadParseEnvironmentBgpTablesAnswerElement(true);
  }

  private ParseEnvironmentBgpTablesAnswerElement loadParseEnvironmentBgpTablesAnswerElement(
      boolean firstAttempt) {
    Path answerPath =
        _testrigSettings.getEnvironmentSettings().getParseEnvironmentBgpTablesAnswerPath();
    if (!Files.exists(answerPath)) {
      repairEnvironmentBgpTables();
    }
    ParseEnvironmentBgpTablesAnswerElement ae =
        deserializeObject(answerPath, ParseEnvironmentBgpTablesAnswerElement.class);
    if (!Version.isCompatibleVersion(
        "Service", "Old processed environment BGP tables", ae.getVersion())) {
      if (firstAttempt) {
        repairEnvironmentRoutingTables();
        return loadParseEnvironmentBgpTablesAnswerElement(false);
      } else {
        throw new BatfishException(
            "Version error repairing environment BGP tables for parse environment BGP tables "
                + "answer element");
      }
    } else {
      return ae;
    }
  }

  @Override
  public ParseEnvironmentRoutingTablesAnswerElement
      loadParseEnvironmentRoutingTablesAnswerElement() {
    return loadParseEnvironmentRoutingTablesAnswerElement(true);
  }

  private ParseEnvironmentRoutingTablesAnswerElement loadParseEnvironmentRoutingTablesAnswerElement(
      boolean firstAttempt) {
    Path answerPath =
        _testrigSettings.getEnvironmentSettings().getParseEnvironmentRoutingTablesAnswerPath();
    if (!Files.exists(answerPath)) {
      repairEnvironmentRoutingTables();
    }
    ParseEnvironmentRoutingTablesAnswerElement pertae =
        deserializeObject(answerPath, ParseEnvironmentRoutingTablesAnswerElement.class);
    if (!Version.isCompatibleVersion(
        "Service", "Old processed environment routing tables", pertae.getVersion())) {
      if (firstAttempt) {
        repairEnvironmentRoutingTables();
        return loadParseEnvironmentRoutingTablesAnswerElement(false);
      } else {
        throw new BatfishException(
            "Version error repairing environment routing tables for parse environment routing "
                + "tables answer element");
      }
    } else {
      return pertae;
    }
  }

  @Override
  public ParseVendorConfigurationAnswerElement loadParseVendorConfigurationAnswerElement() {
    return loadParseVendorConfigurationAnswerElement(true);
  }

  private ParseVendorConfigurationAnswerElement loadParseVendorConfigurationAnswerElement(
      boolean firstAttempt) {
    if (Files.exists(_testrigSettings.getParseAnswerPath())) {
      ParseVendorConfigurationAnswerElement pvcae =
          deserializeObject(
              _testrigSettings.getParseAnswerPath(), ParseVendorConfigurationAnswerElement.class);
      if (Version.isCompatibleVersion(
          "Service", "Old processed configurations", pvcae.getVersion())) {
        return pvcae;
      }
    }
    if (firstAttempt) {
      repairVendorConfigurations();
      return loadParseVendorConfigurationAnswerElement(false);
    } else {
      throw new BatfishException(
          "Version error repairing vendor configurations for parse configuration answer element");
    }
  }

  private ValidateEnvironmentAnswerElement loadValidateEnvironmentAnswerElement() {
    return loadValidateEnvironmentAnswerElement(true);
  }

  private ValidateEnvironmentAnswerElement loadValidateEnvironmentAnswerElement(
      boolean firstAttempt) {
    Path answerPath = _testrigSettings.getEnvironmentSettings().getValidateEnvironmentAnswerPath();
    if (Files.exists(answerPath)) {
      ValidateEnvironmentAnswerElement veae =
          deserializeObject(answerPath, ValidateEnvironmentAnswerElement.class);
      if (Version.isCompatibleVersion("Service", "Old processed environment", veae.getVersion())) {
        return veae;
      }
    }
    if (firstAttempt) {
      parseConfigurationsAndApplyEnvironment();
      return loadValidateEnvironmentAnswerElement(false);
    } else {
      throw new BatfishException(
          "Version error repairing environment for validate environment answer element");
    }
  }

  private void mergeConvertAnswer(
      boolean summary, boolean verboseError, InitInfoAnswerElement answerElement) {
    ConvertConfigurationAnswerElement convertAnswer =
        loadConvertConfigurationAnswerElementOrReparse();
    mergeInitStepAnswer(answerElement, convertAnswer, summary, verboseError);
    for (String failed : convertAnswer.getFailed()) {
      answerElement.getParseStatus().put(failed, ParseStatus.FAILED);
    }
  }

  private void mergeInitStepAnswer(
      InitInfoAnswerElement initInfoAnswerElement,
      InitStepAnswerElement initStepAnswerElement,
      boolean summary,
      boolean verboseError) {
    if (!summary) {
      if (verboseError) {
        SortedMap<String, List<BatfishStackTrace>> errors = initInfoAnswerElement.getErrors();
        initStepAnswerElement
            .getErrors()
            .forEach(
                (hostname, initStepErrors) -> {
                  errors.computeIfAbsent(hostname, k -> new ArrayList<>()).add(initStepErrors);
                });
      }
      SortedMap<String, Warnings> warnings = initInfoAnswerElement.getWarnings();
      initStepAnswerElement
          .getWarnings()
          .forEach(
              (hostname, initStepWarnings) -> {
                Warnings combined =
                    warnings.computeIfAbsent(hostname, h -> buildWarnings(_settings));
                combined.getPedanticWarnings().addAll(initStepWarnings.getPedanticWarnings());
                combined.getRedFlagWarnings().addAll(initStepWarnings.getRedFlagWarnings());
                combined
                    .getUnimplementedWarnings()
                    .addAll(initStepWarnings.getUnimplementedWarnings());
              });
    }
  }

  private InitInfoAnswerElement mergeParseAnswer(
      boolean summary, boolean verboseError, ParseAnswerElement parseAnswer) {
    InitInfoAnswerElement answerElement = new InitInfoAnswerElement();
    mergeInitStepAnswer(answerElement, parseAnswer, summary, verboseError);
    answerElement.setParseStatus(parseAnswer.getParseStatus());
    answerElement.setParseTrees(parseAnswer.getParseTrees());
    return answerElement;
  }

  @Override
  public AnswerElement multipath(ReachabilitySettings reachabilitySettings) {
    return singleReachability(
        reachabilitySettings, MultipathInconsistencyQuerySynthesizer.builder());
  }

  @Override
  public AtomicInteger newBatch(String description, int jobs) {
    return Driver.newBatch(_settings, description, jobs);
  }

  private void outputAnswer(Answer answer) {
    outputAnswer(answer, /* log */ false);
  }

  void outputAnswerWithLog(Answer answer) {
    outputAnswer(answer, /* log */ true);
  }

  private void outputAnswer(Answer answer, boolean writeLog) {
    try {
      String answerString = BatfishObjectMapper.writePrettyString(answer) + '\n';
      _logger.debug(answerString);
      @Nullable String logString = writeLog ? answerString : null;
      writeJsonAnswerWithLog(logString, answerString);
    } catch (Exception e) {
      BatfishException be = new BatfishException("Error in sending answer", e);
      try {
        Answer failureAnswer = Answer.failureAnswer(e.toString(), answer.getQuestion());
        failureAnswer.addAnswerElement(be.getBatfishStackTrace());
        String answerString = BatfishObjectMapper.writePrettyString(failureAnswer) + '\n';
        _logger.error(answerString);
        @Nullable String logString = writeLog ? answerString : null;
        writeJsonAnswerWithLog(logString, answerString);
      } catch (Exception e1) {
        _logger.errorf("Could not serialize failure answer. %s", ExceptionUtils.getStackTrace(e1));
      }
      throw be;
    }
  }

  private ParserRuleContext parse(BatfishCombinedParser<?, ?> parser) {
    return parse(parser, _logger, _settings);
  }

  public ParserRuleContext parse(BatfishCombinedParser<?, ?> parser, String filename) {
    _logger.infof("Parsing: \"%s\"...", filename);
    return parse(parser);
  }

  @Override
  public AssertionAst parseAssertion(String text) {
    AssertionCombinedParser parser = new AssertionCombinedParser(text, _settings);
    AssertionContext tree = (AssertionContext) parse(parser);
    ParseTreeWalker walker = new ParseTreeWalker();
    AssertionExtractor extractor = new AssertionExtractor(text, parser.getParser());
    walker.walk(extractor, tree);
    AssertionAst ast = extractor.getAst();
    return ast;
  }

  private AwsConfiguration parseAwsConfigurations(Map<Path, String> configurationData) {
    AwsConfiguration config = new AwsConfiguration();
    for (Entry<Path, String> configFile : configurationData.entrySet()) {
      Path file = configFile.getKey();
      String fileText = configFile.getValue();
      String regionName = file.getName(file.getNameCount() - 2).toString(); // parent dir name

      // we stop classic link processing here because it interferes with VPC
      // processing
      if (file.toString().contains("classic-link")) {
        _logger.errorf("%s has classic link configuration\n", file);
        continue;
      }

      JSONObject jsonObj = null;
      try {
        jsonObj = new JSONObject(fileText);
      } catch (JSONException e) {
        _logger.errorf("%s does not have valid json\n", file);
      }

      if (jsonObj != null) {
        try {
          config.addConfigElement(regionName, jsonObj, _logger);
        } catch (JSONException e) {
          throw new BatfishException("Problems parsing JSON in " + file, e);
        }
      }
    }
    return config;
  }

  private SortedSet<Edge> parseEdgeBlacklist(Path edgeBlacklistPath) {
    String edgeBlacklistText = CommonUtil.readFile(edgeBlacklistPath);
    SortedSet<Edge> edges;
    try {
      edges =
          BatfishObjectMapper.mapper()
              .readValue(edgeBlacklistText, new TypeReference<SortedSet<Edge>>() {});
    } catch (IOException e) {
      throw new BatfishException("Failed to parse edge blacklist", e);
    }
    return edges;
  }

  private SortedMap<String, BgpAdvertisementsByVrf> parseEnvironmentBgpTables(
      SortedMap<Path, String> inputData, ParseEnvironmentBgpTablesAnswerElement answerElement) {
    _logger.info("\n*** PARSING ENVIRONMENT BGP TABLES ***\n");
    _logger.resetTimer();
    SortedMap<String, BgpAdvertisementsByVrf> bgpTables = new TreeMap<>();
    List<ParseEnvironmentBgpTableJob> jobs = new ArrayList<>();
    SortedMap<String, Configuration> configurations = loadConfigurations();
    for (Entry<Path, String> bgpFile : inputData.entrySet()) {
      Path currentFile = bgpFile.getKey();
      String fileText = bgpFile.getValue();

      String hostname = currentFile.getFileName().toString();
      String optionalSuffix = ".bgp";
      if (hostname.endsWith(optionalSuffix)) {
        hostname = hostname.substring(0, hostname.length() - optionalSuffix.length());
      }
      if (!configurations.containsKey(hostname)) {
        continue;
      }
      Warnings warnings = buildWarnings(_settings);
      ParseEnvironmentBgpTableJob job =
          new ParseEnvironmentBgpTableJob(
              _settings, fileText, hostname, currentFile, warnings, _bgpTablePlugins);
      jobs.add(job);
    }
    BatfishJobExecutor.runJobsInExecutor(
        _settings,
        _logger,
        jobs,
        bgpTables,
        answerElement,
        _settings.getHaltOnParseError(),
        "Parse environment BGP tables");
    _logger.printElapsedTime();
    return bgpTables;
  }

  private SortedMap<String, RoutesByVrf> parseEnvironmentRoutingTables(
      SortedMap<Path, String> inputData, ParseEnvironmentRoutingTablesAnswerElement answerElement) {
    _logger.info("\n*** PARSING ENVIRONMENT ROUTING TABLES ***\n");
    _logger.resetTimer();
    SortedMap<String, RoutesByVrf> routingTables = new TreeMap<>();
    List<ParseEnvironmentRoutingTableJob> jobs = new ArrayList<>();
    SortedMap<String, Configuration> configurations = loadConfigurations();
    for (Entry<Path, String> routingFile : inputData.entrySet()) {
      Path currentFile = routingFile.getKey();
      String fileText = routingFile.getValue();

      String hostname = currentFile.getFileName().toString();
      if (!configurations.containsKey(hostname)) {
        continue;
      }

      Warnings warnings = buildWarnings(_settings);
      ParseEnvironmentRoutingTableJob job =
          new ParseEnvironmentRoutingTableJob(_settings, fileText, currentFile, warnings, this);
      jobs.add(job);
    }
    BatfishJobExecutor.runJobsInExecutor(
        _settings,
        _logger,
        jobs,
        routingTables,
        answerElement,
        _settings.getHaltOnParseError(),
        "Parse environment routing tables");
    _logger.printElapsedTime();
    return routingTables;
  }

  private SortedSet<NodeInterfacePair> parseInterfaceBlacklist(Path interfaceBlacklistPath) {
    String interfaceBlacklistText = CommonUtil.readFile(interfaceBlacklistPath);
    SortedSet<NodeInterfacePair> ifaces;
    try {
      ifaces =
          BatfishObjectMapper.mapper()
              .readValue(
                  interfaceBlacklistText, new TypeReference<SortedSet<NodeInterfacePair>>() {});
    } catch (IOException e) {
      throw new BatfishException("Failed to parse interface blacklist", e);
    }
    return ifaces;
  }

  private SortedSet<String> parseNodeBlacklist(Path nodeBlacklistPath) {
    String nodeBlacklistText = CommonUtil.readFile(nodeBlacklistPath);
    SortedSet<String> nodes;
    try {
      nodes =
          BatfishObjectMapper.mapper()
              .readValue(nodeBlacklistText, new TypeReference<SortedSet<String>>() {});
    } catch (IOException e) {
      throw new BatfishException("Failed to parse node blacklist", e);
    }
    return nodes;
  }

  public Topology parseTopology(Path topologyFilePath) {
    _logger.info("*** PARSING TOPOLOGY ***\n");
    _logger.resetTimer();
    String topologyFileText = CommonUtil.readFile(topologyFilePath);
    _logger.infof("Parsing: \"%s\" ...", topologyFilePath.toAbsolutePath());
    Topology topology = null;
    if (topologyFileText.equals("")) {
      throw new BatfishException("ERROR: empty topology\n");
    } else if (topologyFileText.startsWith("autostart")) {
      BatfishCombinedParser<?, ?> parser = null;
      TopologyExtractor extractor = null;
      parser = new GNS3TopologyCombinedParser(topologyFileText, _settings);
      extractor = new GNS3TopologyExtractor();
      ParserRuleContext tree = parse(parser);
      ParseTreeWalker walker = new ParseTreeWalker();
      walker.walk(extractor, tree);
      topology = extractor.getTopology();
    } else {
      try {
        topology = BatfishObjectMapper.mapper().readValue(topologyFileText, Topology.class);
      } catch (IOException e) {
        _logger.fatal("...ERROR\n");
        throw new BatfishException("Topology format error", e);
      }
    }
    _logger.printElapsedTime();
    return topology;
  }

  private SortedMap<String, VendorConfiguration> parseVendorConfigurations(
      Map<Path, String> configurationData,
      ParseVendorConfigurationAnswerElement answerElement,
      ConfigurationFormat configurationFormat) {
    _logger.info("\n*** PARSING VENDOR CONFIGURATION FILES ***\n");
    _logger.resetTimer();
    SortedMap<String, VendorConfiguration> vendorConfigurations = new TreeMap<>();
    List<ParseVendorConfigurationJob> jobs = new ArrayList<>();//这里面的call函数才是真正执行解析的部分
    for (Entry<Path, String> vendorFile : configurationData.entrySet()) {
      Path currentFile = vendorFile.getKey();
      String fileText = vendorFile.getValue();

      Warnings warnings = buildWarnings(_settings);
      //filetext对应当前配置文件的内容
      //currentFile对应当前配置文件的路径
      ParseVendorConfigurationJob job =
          new ParseVendorConfigurationJob(
              _settings, fileText, currentFile, warnings, configurationFormat);
      jobs.add(job);//把解析的工作加入jobs队列
    }

    //batfish的job执行器,用来执行具体的解析任务
    //jobs表示需要执行的任务,vendorConfigurations表示将解析结果输出的地方
    //description表示对任务的描述
    BatfishJobExecutor.runJobsInExecutor(
        _settings,
        _logger,
        jobs,
        vendorConfigurations,
        answerElement,
        _settings.getHaltOnParseError(),
        "Parse configurations");
    _logger.printElapsedTime();

    return vendorConfigurations;

  }

  @Override
  public AnswerElement pathDiff(ReachabilitySettings reachabilitySettings) {
    Settings settings = getSettings();
    checkDifferentialDataPlaneQuestionDependencies();
    String tag = getDifferentialFlowTag();

    // load base configurations and generate base data plane
    pushBaseEnvironment();
    Map<String, Configuration> baseConfigurations = loadConfigurations();
    Synthesizer baseDataPlaneSynthesizer = synthesizeDataPlane();
    Topology baseTopology = getEnvironmentTopology();
    popEnvironment();

    // load diff configurations and generate diff data plane
    pushDeltaEnvironment();
    Map<String, Configuration> diffConfigurations = loadConfigurations();
    Synthesizer diffDataPlaneSynthesizer = synthesizeDataPlane();
    Topology diffTopology = getEnvironmentTopology();
    popEnvironment();

    pushDeltaEnvironment();
    SortedSet<String> blacklistNodes = getNodeBlacklist();
    Set<NodeInterfacePair> blacklistInterfaces = getInterfaceBlacklist();
    SortedSet<Edge> blacklistEdges = getEdgeBlacklist();
    popEnvironment();

    BlacklistDstIpQuerySynthesizer blacklistQuery =
        new BlacklistDstIpQuerySynthesizer(
            null, blacklistNodes, blacklistInterfaces, blacklistEdges, baseConfigurations);

    // compute composite program and flows
    List<Synthesizer> commonEdgeSynthesizers =
        ImmutableList.of(
            baseDataPlaneSynthesizer, diffDataPlaneSynthesizer, baseDataPlaneSynthesizer);

    List<CompositeNodJob> jobs = new ArrayList<>();

    // generate local edge reachability and black hole queries
    SortedSet<Edge> diffEdges = diffTopology.getEdges();
    for (Edge edge : diffEdges) {
      String ingressNode = edge.getNode1();
      String outInterface = edge.getInt1();
      String vrf =
          diffConfigurations.get(ingressNode).getInterfaces().get(outInterface).getVrf().getName();
      ReachEdgeQuerySynthesizer reachQuery =
          new ReachEdgeQuerySynthesizer(
              ingressNode, vrf, edge, true, reachabilitySettings.getHeaderSpace());
      ReachEdgeQuerySynthesizer noReachQuery =
          new ReachEdgeQuerySynthesizer(ingressNode, vrf, edge, true, new HeaderSpace());
      noReachQuery.setNegate(true);
      List<QuerySynthesizer> queries = ImmutableList.of(reachQuery, noReachQuery, blacklistQuery);
      SortedSet<IngressPoint> ingressPoints =
          ImmutableSortedSet.of(IngressPoint.ingressVrf(ingressNode, vrf));
      CompositeNodJob job =
          new CompositeNodJob(settings, commonEdgeSynthesizers, queries, ingressPoints, tag);
      jobs.add(job);
    }

    // we also need queries for nodes next to edges that are now missing,
    // in the case that those nodes still exist
    List<Synthesizer> missingEdgeSynthesizers =
        ImmutableList.of(baseDataPlaneSynthesizer, baseDataPlaneSynthesizer);
    SortedSet<Edge> baseEdges = baseTopology.getEdges();
    SortedSet<Edge> missingEdges = ImmutableSortedSet.copyOf(Sets.difference(baseEdges, diffEdges));
    for (Edge missingEdge : missingEdges) {
      String ingressNode = missingEdge.getNode1();
      String outInterface = missingEdge.getInt1();
      if (diffConfigurations.containsKey(ingressNode)
          && diffConfigurations.get(ingressNode).getInterfaces().containsKey(outInterface)) {
        String vrf =
            diffConfigurations
                .get(ingressNode)
                .getInterfaces()
                .get(outInterface)
                .getVrf()
                .getName();
        ReachEdgeQuerySynthesizer reachQuery =
            new ReachEdgeQuerySynthesizer(
                ingressNode, vrf, missingEdge, true, reachabilitySettings.getHeaderSpace());
        List<QuerySynthesizer> queries = ImmutableList.of(reachQuery, blacklistQuery);
        SortedSet<IngressPoint> ingressPoints =
            ImmutableSortedSet.of(IngressPoint.ingressVrf(ingressNode, vrf));
        CompositeNodJob job =
            new CompositeNodJob(settings, missingEdgeSynthesizers, queries, ingressPoints, tag);
        jobs.add(job);
      }
    }

    // TODO: maybe do something with nod answer element
    Set<Flow> flows = computeCompositeNodOutput(jobs, new NodAnswerElement());
    pushBaseEnvironment();
    getDataPlanePlugin().processFlows(flows, loadDataPlane(), false);
    popEnvironment();
    pushDeltaEnvironment();
    getDataPlanePlugin().processFlows(flows, loadDataPlane(), false);
    popEnvironment();

    AnswerElement answerElement = getHistory();
    return answerElement;
  }

  @Override
  public void popEnvironment() {
    int lastIndex = _testrigSettingsStack.size() - 1;
    _testrigSettings = _testrigSettingsStack.get(lastIndex);
    _testrigSettingsStack.remove(lastIndex);
  }

  private void populateFlowHistory(
      FlowHistory flowHistory, String envTag, Environment environment, String flowTag) {
    DataPlanePlugin dataPlanePlugin = getDataPlanePlugin();
    List<Flow> flows = dataPlanePlugin.getHistoryFlows(loadDataPlane());
    List<FlowTrace> flowTraces = dataPlanePlugin.getHistoryFlowTraces(loadDataPlane());
    int numEntries = flows.size();
    for (int i = 0; i < numEntries; i++) {
      Flow flow = flows.get(i);
      if (flow.getTag().equals(flowTag)) {
        FlowTrace flowTrace = flowTraces.get(i);
        flowHistory.addFlowTrace(flow, envTag, environment, flowTrace);
      }
    }
  }

  private void postProcessConfigurations(Collection<Configuration> configurations) {
    for (Configuration c : configurations) {
      //设置这个配置的类型
      // Set device type to host iff the configuration format is HOST
      if (c.getConfigurationFormat() == ConfigurationFormat.HOST) {
        c.setDeviceType(DeviceType.HOST);
      }
      for (Vrf vrf : c.getVrfs().values()) {
        // If vrf has BGP, OSPF, or RIP process and device isn't a host, set device type to router
        if (c.getDeviceType() == null
            && (vrf.getBgpProcess() != null
                || vrf.getOspfProcess() != null
                || vrf.getRipProcess() != null)) {
          c.setDeviceType(DeviceType.ROUTER);
        }
        // Compute OSPF interface costs where they are missing
        OspfProcess proc = vrf.getOspfProcess();
        if (proc != null) {
          proc.initInterfaceCosts(c);
        }
      }
      // If device was not a host or router, call it a switch
      if (c.getDeviceType() == null) {
        c.setDeviceType(DeviceType.SWITCH);
      }
    }
  }

  private void printSymmetricEdgePairs() {
    Map<String, Configuration> configs = loadConfigurations();
    SortedSet<Edge> edges = CommonUtil.synthesizeTopology(configs).getEdges();
    Set<Edge> symmetricEdgePairs = getSymmetricEdgePairs(edges);
    List<Edge> edgeList = new ArrayList<>();
    edgeList.addAll(symmetricEdgePairs);
    for (int i = 0; i < edgeList.size() / 2; i++) {
      Edge edge1 = edgeList.get(2 * i);
      Edge edge2 = edgeList.get(2 * i + 1);
      _logger.output(
          edge1.getNode1()
              + ":"
              + edge1.getInt1()
              + ","
              + edge1.getNode2()
              + ":"
              + edge1.getInt2()
              + " "
              + edge2.getNode1()
              + ":"
              + edge2.getInt1()
              + ","
              + edge2.getNode2()
              + ":"
              + edge2.getInt2()
              + "\n");
    }
    _logger.printElapsedTime();
  }

  @Override
  public Set<BgpAdvertisement> loadExternalBgpAnnouncements(
      Map<String, Configuration> configurations) {
    Set<BgpAdvertisement> advertSet = new LinkedHashSet<>();
    for (ExternalBgpAdvertisementPlugin plugin : _externalBgpAdvertisementPlugins) {
      Set<BgpAdvertisement> currentAdvertisements = plugin.loadExternalBgpAdvertisements();
      advertSet.addAll(currentAdvertisements);
    }
    return advertSet;
  }

  /**
   * Reads the external bgp announcement specified in the environment, and populates the
   * vendor-independent configurations with data about those announcements
   *
   * @param configurations The vendor-independent configurations to be modified
   */
  public Set<BgpAdvertisement> processExternalBgpAnnouncements(
      Map<String, Configuration> configurations, SortedSet<Long> allCommunities) {
    Set<BgpAdvertisement> advertSet = new LinkedHashSet<>();
    Path externalBgpAnnouncementsPath =
        _testrigSettings.getEnvironmentSettings().getExternalBgpAnnouncementsPath();
    if (Files.exists(externalBgpAnnouncementsPath)) {
      String externalBgpAnnouncementsFileContents =
          CommonUtil.readFile(externalBgpAnnouncementsPath);
      // Populate advertSet with BgpAdvertisements that
      // gets passed to populatePrecomputedBgpAdvertisements.
      // See populatePrecomputedBgpAdvertisements for the things that get
      // extracted from these advertisements.

      try {
        JSONObject jsonObj = new JSONObject(externalBgpAnnouncementsFileContents);

        JSONArray announcements = jsonObj.getJSONArray(BfConsts.PROP_BGP_ANNOUNCEMENTS);

        for (int index = 0; index < announcements.length(); index++) {
          JSONObject announcement = new JSONObject();
          announcement.put("@id", index);
          JSONObject announcementSrc = announcements.getJSONObject(index);
          for (Iterator<?> i = announcementSrc.keys(); i.hasNext(); ) {
            String key = (String) i.next();
            if (!key.equals("@id")) {
              announcement.put(key, announcementSrc.get(key));
            }
          }
          BgpAdvertisement bgpAdvertisement =
              BatfishObjectMapper.mapper()
                  .readValue(announcement.toString(), BgpAdvertisement.class);
          allCommunities.addAll(bgpAdvertisement.getCommunities());
          advertSet.add(bgpAdvertisement);
        }

      } catch (JSONException | IOException e) {
        throw new BatfishException("Problems parsing JSON in " + externalBgpAnnouncementsPath, e);
      }
    }
    return advertSet;
  }

  @Override
  public void processFlows(Set<Flow> flows, boolean ignoreAcls) {
    getDataPlanePlugin().processFlows(flows, loadDataPlane(), ignoreAcls);
  }

  /**
   * Helper function to disable a blacklisted interface and update the given {@link
   * ValidateEnvironmentAnswerElement} if the interface does not actually exist.
   */
  private static void blacklistInterface(
      Map<String, Configuration> configurations,
      ValidateEnvironmentAnswerElement veae,
      NodeInterfacePair iface) {
    String hostname = iface.getHostname();
    String ifaceName = iface.getInterface();
    @Nullable Configuration node = configurations.get(hostname);
    if (node == null) {
      veae.setValid(false);
      veae.getUndefinedInterfaceBlacklistNodes().add(hostname);
      return;
    }

    @Nullable Interface nodeIface = node.getInterfaces().get(ifaceName);
    if (nodeIface == null) {
      veae.setValid(false);
      veae.getUndefinedInterfaceBlacklistInterfaces()
          .computeIfAbsent(hostname, k -> new TreeSet<>())
          .add(ifaceName);
      return;
    }

    nodeIface.setActive(false);
    nodeIface.setBlacklisted(true);
  }

  private void processInterfaceBlacklist(
      Map<String, Configuration> configurations, ValidateEnvironmentAnswerElement veae) {
    Set<NodeInterfacePair> blacklistInterfaces = getInterfaceBlacklist();
    for (NodeInterfacePair p : blacklistInterfaces) {
      blacklistInterface(configurations, veae, p);
    }
  }

  private void processNodeBlacklist(
      Map<String, Configuration> configurations, ValidateEnvironmentAnswerElement veae) {
    SortedSet<String> blacklistNodes = getNodeBlacklist();
    for (String hostname : blacklistNodes) {
      Configuration node = configurations.get(hostname);
      if (node != null) {
        for (Interface iface : node.getInterfaces().values()) {
          iface.setActive(false);
          iface.setBlacklisted(true);
        }
      } else {
        veae.setValid(false);
        veae.getUndefinedNodeBlacklistNodes().add(hostname);
      }
    }
  }

  private Topology processTopologyFile(Path topologyFilePath) {
    Topology topology = parseTopology(topologyFilePath);
    return topology;
  }

  @Override
  public void pushBaseEnvironment() {
    _testrigSettingsStack.add(_testrigSettings);
    _testrigSettings = _baseTestrigSettings;
  }

  @Override
  public void pushDeltaEnvironment() {
    _testrigSettingsStack.add(_testrigSettings);
    _testrigSettings = _deltaTestrigSettings;
  }

  private SortedMap<Path, String> readConfigurationFiles(Path testRigPath, String configsType) {
    _logger.infof("\n*** READING %s FILES ***\n", configsType);
    _logger.resetTimer();
    SortedMap<Path, String> configurationData = new TreeMap<>();
    Path configsPath = testRigPath.resolve(configsType);
//    System.out.println("---------------back-readConfigueFiles-configsPath:----------------");
//    System.out.println(configsPath);
//    System.out.println("---------------------------End------------------------------------");
    List<Path> configFilePaths = listAllFiles(configsPath);
//    System.out.println("---------------back-readConfigueFiles-list-configsPath:----------------");
    AtomicInteger completed =
        newBatch("Reading network configuration files", configFilePaths.size());
    //这一部分主要是读取配置文件,然后把配置文件的内容保存到configurationData当中,configurationdata 是一个字典,用path对应configurations来保存配置文件
    //配置文件使用的字符串进行保存的
    for (Path file : configFilePaths) {
//      System.out.println(file);
      _logger.debugf("Reading: \"%s\"\n", file);
      String fileTextRaw = CommonUtil.readFile(file.toAbsolutePath());
      String fileText = fileTextRaw + ((fileTextRaw.length() != 0) ? "\n" : "");
      configurationData.put(file, fileText);
      completed.incrementAndGet();
    }
//    System.out.println("---------------------------End------------------------------------");
    _logger.printElapsedTime();
    return configurationData;//返回相应的配置文件
  }

  @Nullable
  @Override
  public String readExternalBgpAnnouncementsFile() {
    Path externalBgpAnnouncementsPath =
        _testrigSettings.getEnvironmentSettings().getExternalBgpAnnouncementsPath();
    if (Files.exists(externalBgpAnnouncementsPath)) {
      String externalBgpAnnouncementsFileContents =
          CommonUtil.readFile(externalBgpAnnouncementsPath);
      return externalBgpAnnouncementsFileContents;
    } else {
      return null;
    }
  }

  private SortedMap<Path, String> readFiles(Path directory, String description) {
    _logger.infof("\n*** READING FILES: %s ***\n", description);
    _logger.resetTimer();
    SortedMap<Path, String> fileData = new TreeMap<>();
    List<Path> filePaths;
    try (Stream<Path> paths = CommonUtil.list(directory)) {
      filePaths =
          paths
              .filter(path -> !path.getFileName().toString().startsWith("."))
              .sorted()
              .collect(Collectors.toList());
    }
    AtomicInteger completed = newBatch("Reading files: " + description, filePaths.size());
    for (Path file : filePaths) {
      _logger.debugf("Reading: \"%s\"\n", file);
      String fileTextRaw = CommonUtil.readFile(file.toAbsolutePath());
      String fileText = fileTextRaw + ((fileTextRaw.length() != 0) ? "\n" : "");
      fileData.put(file, fileText);
      completed.incrementAndGet();
    }
    _logger.printElapsedTime();
    return fileData;
  }

  /**
   * Read Iptable Files for each host in the keyset of {@code hostConfigurations}, and store the
   * contents in {@code iptablesDate}. Each task fails if the Iptable file specified by host is not
   * under {@code testRigPath} or does not exist.
   *
   * @throws BatfishException if there is a failed task and either {@link
   *     Settings#getExitOnFirstError()} or {@link Settings#getHaltOnParseError()} is set.
   */
  void readIptableFiles(
      Path testRigPath,
      SortedMap<String, VendorConfiguration> hostConfigurations,
      SortedMap<Path, String> iptablesData,
      ParseVendorConfigurationAnswerElement answerElement) {
    List<BatfishException> failureCauses = new ArrayList<>();
    for (VendorConfiguration vc : hostConfigurations.values()) {
      HostConfiguration hostConfig = (HostConfiguration) vc;
      if (hostConfig.getIptablesFile() != null) {
        Path path = Paths.get(testRigPath.toString(), hostConfig.getIptablesFile());

        // ensure that the iptables file is not taking us outside of the
        // testrig
        try {
          if (!path.toFile().getCanonicalPath().contains(testRigPath.toFile().getCanonicalPath())
              || !path.toFile().exists()) {
            String failureMessage =
                String.format(
                    "Iptables file %s for host %s is not contained within the testrig",
                    hostConfig.getIptablesFile(), hostConfig.getHostname());
            BatfishException bfc;
            if (answerElement.getErrors().containsKey(hostConfig.getHostname())) {
              bfc =
                  new BatfishException(
                      failureMessage,
                      answerElement.getErrors().get(hostConfig.getHostname()).getException());
              answerElement.getErrors().put(hostConfig.getHostname(), bfc.getBatfishStackTrace());
            } else {
              bfc = new BatfishException(failureMessage);
              if (_settings.getExitOnFirstError()) {
                throw bfc;
              } else {
                failureCauses.add(bfc);
                answerElement.getErrors().put(hostConfig.getHostname(), bfc.getBatfishStackTrace());
                answerElement.getParseStatus().put(hostConfig.getHostname(), ParseStatus.FAILED);
              }
            }
          } else {
            String fileText = CommonUtil.readFile(path);
            iptablesData.put(path, fileText);
          }
        } catch (IOException e) {
          throw new BatfishException("Could not get canonical path", e);
        }
      }
    }

    if (_settings.getHaltOnParseError() && !failureCauses.isEmpty()) {
      BatfishException e =
          new BatfishException(
              "Fatal exception due to at least one Iptables file is"
                  + " not contained within the testrig");
      failureCauses.forEach(e::addSuppressed);
      throw e;
    }
  }

  @Override
  public AnswerElement reducedReachability(ReachabilitySettings reachabilitySettings) {
    Settings settings = getSettings();
    checkDifferentialDataPlaneQuestionDependencies();
    String tag = getDifferentialFlowTag();

    Set<String> baseActiveIngressNodes = null;
    Set<String> diffActiveIngressNodes = null;

    // load base configurations and generate base data plane
    pushBaseEnvironment();
    Map<String, Configuration> baseConfigurations = loadConfigurations();
    try {
      baseActiveIngressNodes = reachabilitySettings.computeActiveIngressNodes(this);
    } catch (InvalidReachabilitySettingsException e) {
      return e.getInvalidSettingsAnswer();
    }
    Synthesizer baseDataPlaneSynthesizer = synthesizeDataPlane();
    popEnvironment();

    // load diff configurations and generate diff data plane
    pushDeltaEnvironment();
    try {
      diffActiveIngressNodes = reachabilitySettings.computeActiveIngressNodes(this);
    } catch (InvalidReachabilitySettingsException e) {
      return e.getInvalidSettingsAnswer();
    }
    Synthesizer diffDataPlaneSynthesizer = synthesizeDataPlane();
    popEnvironment();

    Set<String> ingressNodes;
    ingressNodes =
        ImmutableSet.copyOf(Sets.intersection(baseActiveIngressNodes, diffActiveIngressNodes));

    pushDeltaEnvironment();
    SortedSet<String> blacklistNodes = getNodeBlacklist();
    Set<NodeInterfacePair> blacklistInterfaces = getInterfaceBlacklist();
    SortedSet<Edge> blacklistEdges = getEdgeBlacklist();
    popEnvironment();

    BlacklistDstIpQuerySynthesizer blacklistQuery =
        new BlacklistDstIpQuerySynthesizer(
            null, blacklistNodes, blacklistInterfaces, blacklistEdges, baseConfigurations);

    // compute composite program and flows
    List<Synthesizer> synthesizers =
        ImmutableList.of(
            baseDataPlaneSynthesizer, diffDataPlaneSynthesizer, baseDataPlaneSynthesizer);

    // generate base reachability and diff blackhole and blacklist queries
    List<CompositeNodJob> jobs =
        ingressNodes
            .stream()
            .flatMap(
                node ->
                    baseConfigurations
                        .get(node)
                        .getVrfs()
                        .keySet()
                        .stream()
                        .map(
                            vrf -> {
                              Multimap<String, String> ingressNodeVrfs =
                                  ImmutableMultimap.of(node, vrf);
                              StandardReachabilityQuerySynthesizer acceptQuery =
                                  StandardReachabilityQuerySynthesizer.builder()
                                      .setActions(
                                          ImmutableSet.of(
                                              ForwardingAction.ACCEPT,
                                              ForwardingAction
                                                  .NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK))
                                      .setHeaderSpace(reachabilitySettings.getHeaderSpace())
                                      .setIngressNodeVrfs(ingressNodeVrfs)
                                      .setFinalNodes(ImmutableSet.of())
                                      .setTransitNodes(ImmutableSet.of())
                                      .setNonTransitNodes(ImmutableSet.of())
                                      .setSrcNatted(reachabilitySettings.getSrcNatted())
                                      .build();
                              StandardReachabilityQuerySynthesizer notAcceptQuery =
                                  StandardReachabilityQuerySynthesizer.builder()
                                      .setActions(
                                          ImmutableSet.of(
                                              ForwardingAction.ACCEPT,
                                              ForwardingAction
                                                  .NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK))
                                      .setHeaderSpace(new HeaderSpace())
                                      .setIngressNodeVrfs(ingressNodeVrfs)
                                      .setFinalNodes(ImmutableSet.of())
                                      .setTransitNodes(ImmutableSet.of())
                                      .setNonTransitNodes(ImmutableSet.of())
                                      .build();
                              notAcceptQuery.setNegate(true);
                              SortedSet<IngressPoint> ingressPoints =
                                  ImmutableSortedSet.of(IngressPoint.ingressVrf(node, vrf));
                              List<QuerySynthesizer> queries =
                                  ImmutableList.of(acceptQuery, notAcceptQuery, blacklistQuery);
                              return new CompositeNodJob(
                                  settings, synthesizers, queries, ingressPoints, tag);
                            }))
            .collect(Collectors.toList());

    // TODO: maybe do something with nod answer element
    Set<Flow> flows = computeCompositeNodOutput(jobs, new NodAnswerElement());
    pushBaseEnvironment();
    getDataPlanePlugin().processFlows(flows, loadDataPlane(), false);
    popEnvironment();
    pushDeltaEnvironment();
    getDataPlanePlugin().processFlows(flows, loadDataPlane(), false);
    popEnvironment();

    AnswerElement answerElement = getHistory();
    return answerElement;
  }

  @Override
  public void registerAnswerer(
      String questionName,
      String questionClassName,
      BiFunction<Question, IBatfish, Answerer> answererCreator) {
    _answererCreators.put(questionName, answererCreator);
  }

  @Override
  public void registerBgpTablePlugin(BgpTableFormat format, BgpTablePlugin bgpTablePlugin) {
    _bgpTablePlugins.put(format, bgpTablePlugin);
  }

  @Override
  public void registerExternalBgpAdvertisementPlugin(
      ExternalBgpAdvertisementPlugin externalBgpAdvertisementPlugin) {
    _externalBgpAdvertisementPlugins.add(externalBgpAdvertisementPlugin);
  }

  private void repairConfigurations() {
    ParseVendorConfigurationAnswerElement pvcae = loadParseVendorConfigurationAnswerElement();
    if (!Version.isCompatibleVersion("Service", "Old parsed configurations", pvcae.getVersion())) {
      repairVendorConfigurations();
    }
    Path inputPath = _testrigSettings.getSerializeVendorPath();
    serializeIndependentConfigs(inputPath);
  }

  private void repairDataPlane(boolean compressed) {
    Path dataPlanePath =
        compressed
            ? _testrigSettings.getEnvironmentSettings().getCompressedDataPlanePath()
            : _testrigSettings.getEnvironmentSettings().getDataPlanePath();

    Path dataPlaneAnswerPath =
        compressed
            ? _testrigSettings.getEnvironmentSettings().getCompressedDataPlaneAnswerPath()
            : _testrigSettings.getEnvironmentSettings().getDataPlaneAnswerPath();

    CommonUtil.deleteIfExists(dataPlanePath);
    CommonUtil.deleteIfExists(dataPlaneAnswerPath);

    if (compressed) {
      computeCompressedDataPlane();
    } else {
      computeDataPlane(false);
    }
  }

  /**
   * Applies the current environment to the specified configurations and updates the given {@link
   * ValidateEnvironmentAnswerElement}. Applying the environment includes:
   *
   * <ul>
   *   <li>Applying node and interface blacklists.
   *   <li>Applying node and interface blacklists.
   * </ul>
   */
  private void updateBlacklistedAndInactiveConfigs(
      Map<String, Configuration> configurations, ValidateEnvironmentAnswerElement veae) {
    processNodeBlacklist(configurations, veae);
    processInterfaceBlacklist(configurations, veae);
    // We do not process the edge blacklist here. Instead, we rely on these edges being explicitly
    // deleted from the Topology (aka list of edges) that is used along with configurations in
    // answering questions.
    disableUnusableVlanInterfaces(configurations);
    disableUnusableVpnInterfaces(configurations);
  }

  /**
   * Ensures that the current configurations for the current testrig+environment are up to date.
   * Among other things, this includes:
   *
   * <ul>
   *   <li>Invalidating cached configs if the in-memory copy has been changed by question
   *       processing.
   *   <li>Re-loading configurations from disk, including re-parsing if the configs were parsed on a
   *       previous version of Batfish.
   *   <li>Re-applying the environment to the configs, to ensure that blacklists are honored.
   * </ul>
   */
  private void applyEnvironment(Map<String, Configuration> configurationsWithoutEnvironment) {
    ValidateEnvironmentAnswerElement veae = new ValidateEnvironmentAnswerElement();
    updateBlacklistedAndInactiveConfigs(configurationsWithoutEnvironment, veae);

//    System.out.println("--------------------------------env-path:------------------------");
//    System.out.println(_testrigSettings.getEnvironmentSettings().getValidateEnvironmentAnswerPath());
//    System.out.println("----------------------------------End----------------------------");
    serializeObject(
        veae, _testrigSettings.getEnvironmentSettings().getValidateEnvironmentAnswerPath());
  }

  private void repairEnvironmentBgpTables() {
    EnvironmentSettings envSettings = _testrigSettings.getEnvironmentSettings();
    Path answerPath = envSettings.getParseEnvironmentBgpTablesAnswerPath();
    Path bgpTablesOutputPath = envSettings.getSerializeEnvironmentBgpTablesPath();
    CommonUtil.deleteIfExists(answerPath);
    CommonUtil.deleteDirectory(bgpTablesOutputPath);
    computeEnvironmentBgpTables();
  }

  private void repairEnvironmentRoutingTables() {
    EnvironmentSettings envSettings = _testrigSettings.getEnvironmentSettings();
    Path answerPath = envSettings.getParseEnvironmentRoutingTablesAnswerPath();
    Path rtOutputPath = envSettings.getSerializeEnvironmentRoutingTablesPath();
    CommonUtil.deleteIfExists(answerPath);
    CommonUtil.deleteDirectory(rtOutputPath);
    computeEnvironmentRoutingTables();
  }

  private void repairVendorConfigurations() {
    Path outputPath = _testrigSettings.getSerializeVendorPath();
    CommonUtil.deleteDirectory(outputPath);
    Path testRigPath = _testrigSettings.getTestRigPath();
    serializeVendorConfigs(testRigPath, outputPath);
  }

  private AnswerElement report() {
    ReportAnswerElement answerElement = new ReportAnswerElement();
    checkQuestionsDirExists();
    Path questionsDir =
        _settings.getActiveTestrigSettings().getBasePath().resolve(BfConsts.RELPATH_QUESTIONS_DIR);
    ConcurrentMap<Path, String> answers = new ConcurrentHashMap<>();
    try (DirectoryStream<Path> questions = Files.newDirectoryStream(questionsDir)) {
      questions.forEach(
          questionDirPath ->
              answers.put(
                  questionDirPath.resolve(BfConsts.RELPATH_ANSWER_JSON),
                  !questionDirPath.getFileName().startsWith(".")
                          && Files.exists(questionDirPath.resolve(BfConsts.RELPATH_ANSWER_JSON))
                      ? CommonUtil.readFile(questionDirPath.resolve(BfConsts.RELPATH_ANSWER_JSON))
                      : ""));
    } catch (IOException e1) {
      throw new BatfishException(
          "Could not create directory stream for '" + questionsDir + "'", e1);
    }
    ObjectMapper mapper = BatfishObjectMapper.mapper();
    for (Entry<Path, String> entry : answers.entrySet()) {
      Path answerPath = entry.getKey();
      String answerText = entry.getValue();
      if (!answerText.equals("")) {
        try {
          answerElement.getJsonAnswers().add(mapper.readTree(answerText));
        } catch (IOException e) {
          throw new BatfishException(
              "Error mapping JSON content of '" + answerPath + "' to object", e);
        }
      }
    }
    return answerElement;
  }

  public Answer run() {
    newBatch("Begin job", 0);
    loadPlugins();
    boolean action = false;
    Answer answer = new Answer();
    //判断这个batfish到底是用来干啥的
    if (_settings.getPrintSymmetricEdgePairs()) {
      printSymmetricEdgePairs();
      return answer;
    }

    if (_settings.getReport()) {
      answer.addAnswerElement(report());
      return answer;
    }

    if (_settings.getSynthesizeJsonTopology()) {
      writeJsonTopology();
      return answer;
    }

    if (_settings.getHistogram()) {
      histogram(_testrigSettings.getTestRigPath());
      return answer;
    }

    if (_settings.getFlatten()) {
      Path flattenSource = _testrigSettings.getTestRigPath();
      Path flattenDestination = _settings.getFlattenDestination();
      flatten(flattenSource, flattenDestination);
      return answer;
    }

    if (_settings.getGenerateStubs()) {
      String inputRole = _settings.getGenerateStubsInputRole();
      String interfaceDescriptionRegex = _settings.getGenerateStubsInterfaceDescriptionRegex();
      int stubAs = _settings.getGenerateStubsRemoteAs();
      generateStubs(inputRole, stubAs, interfaceDescriptionRegex);
      return answer;
    }

    // if (_settings.getZ3()) {
    // Map<String, Configuration> configurations = loadConfigurations();
    // String dataPlanePath = _envSettings.getDataPlanePath();
    // if (dataPlanePath == null) {
    // throw new BatfishException("Missing path to data plane");
    // }
    // File dataPlanePathAsFile = new File(dataPlanePath);
    // genZ3(configurations, dataPlanePathAsFile);
    // return answer;
    // }
    //
    // if (_settings.getRoleTransitQuery()) {
    // genRoleTransitQueries();
    // return answer;
    // }

    if (_settings.getSerializeVendor()) {//解析配置文件 一
      Path testRigPath = _testrigSettings.getTestRigPath();
      Path outputPath = _testrigSettings.getSerializeVendorPath();
      answer.append(serializeVendorConfigs(testRigPath, outputPath));
      action = true;
    }

    if (_settings.getSerializeIndependent()) {//这一部分是干什么的-获取拓扑结构? 二
      Path inputPath = _testrigSettings.getSerializeVendorPath();
      answer.append(serializeIndependentConfigs(inputPath));//获取拓扑结构,将配置解析为具体厂商无关的形式
      action = true;
    }

    if (_settings.getInitInfo()) {
      InitInfoAnswerElement initInfoAnswerElement = initInfo(true, false);
      // In this context we can remove parse trees because they will be returned in preceding answer
      // element. Note that parse trees are not removed when asking initInfo as its own question.
      initInfoAnswerElement.setParseTrees(Collections.emptySortedMap());
      answer.addAnswerElement(initInfoAnswerElement);
      action = true;
    }

    if (_settings.getCompileEnvironment()) {
      answer.append(compileEnvironmentConfigurations(_testrigSettings));
      action = true;
    }

    if (_settings.getAnswer()) {
      try (ActiveSpan questionSpan =
          GlobalTracer.get().buildSpan("Getting answer to question").startActive()) {
        assert questionSpan != null; // avoid unused warning
        answer.append(answer());
        action = true;
      }
    }

    if (_settings.getAnalyze()) {
      answer.append(analyze());
      action = true;
    }

    if (_settings.getDataPlane()) {
      answer.addAnswerElement(computeDataPlane(_settings.getDiffActive()));
      action = true;
    }

    if (_settings.getValidateEnvironment()) {
      answer.append(validateEnvironment());
      action = true;
    }

    if (!action) {
      throw new CleanBatfishException("No task performed! Run with -help flag to see usage\n");
    }
    return answer;
  }

  public static void serializeAsJson(Path outputPath, Object object, String objectName) {
    try {
      BatfishObjectMapper.prettyWriter().writeValue(outputPath.toFile(), object);
    } catch (IOException e) {
      throw new BatfishException("Could not serialize " + objectName + " ", e);
    }
  }

  private Answer serializeAwsConfigs(Path testRigPath, Path outputPath) {
    Answer answer = new Answer();
    Map<Path, String> configurationData =
        readConfigurationFiles(testRigPath, BfConsts.RELPATH_AWS_CONFIGS_DIR);
    AwsConfiguration config;
    try (ActiveSpan parseAwsConfigsSpan =
        GlobalTracer.get().buildSpan("Parse AWS configs").startActive()) {
      assert parseAwsConfigsSpan != null; // avoid unused warning
      config = parseAwsConfigurations(configurationData);
    }

    _logger.info("\n*** SERIALIZING AWS CONFIGURATION STRUCTURES ***\n");
    _logger.resetTimer();
    outputPath.toFile().mkdirs();
    Path currentOutputPath = outputPath.resolve(BfConsts.RELPATH_AWS_CONFIGS_FILE);
    _logger.debugf("Serializing AWS to \"%s\"...", currentOutputPath);
    serializeObject(config, currentOutputPath);
    _logger.debug("OK\n");
    _logger.printElapsedTime();
    return answer;
  }

  private Answer serializeEnvironmentBgpTables(Path inputPath, Path outputPath) {
    Answer answer = new Answer();
    ParseEnvironmentBgpTablesAnswerElement answerElement =
        new ParseEnvironmentBgpTablesAnswerElement();
    answerElement.setVersion(Version.getVersion());
    answer.addAnswerElement(answerElement);
    SortedMap<String, BgpAdvertisementsByVrf> bgpTables =
        getEnvironmentBgpTables(inputPath, answerElement);
    serializeEnvironmentBgpTables(bgpTables, outputPath);
    serializeObject(
        answerElement,
        _testrigSettings.getEnvironmentSettings().getParseEnvironmentBgpTablesAnswerPath());
    return answer;
  }

  private void serializeEnvironmentBgpTables(
      SortedMap<String, BgpAdvertisementsByVrf> bgpTables, Path outputPath) {
    if (bgpTables == null) {
      throw new BatfishException("Exiting due to parsing error(s)");
    }
    _logger.info("\n*** SERIALIZING ENVIRONMENT BGP TABLES ***\n");
    _logger.resetTimer();
    outputPath.toFile().mkdirs();
    SortedMap<Path, BgpAdvertisementsByVrf> output = new TreeMap<>();
    bgpTables.forEach(
        (name, rt) -> {
          Path currentOutputPath = outputPath.resolve(name);
          output.put(currentOutputPath, rt);
        });
    serializeObjects(output);
    _logger.printElapsedTime();
  }

  private Answer serializeEnvironmentRoutingTables(Path inputPath, Path outputPath) {
    Answer answer = new Answer();
    ParseEnvironmentRoutingTablesAnswerElement answerElement =
        new ParseEnvironmentRoutingTablesAnswerElement();
    answerElement.setVersion(Version.getVersion());
    answer.addAnswerElement(answerElement);
    SortedMap<String, RoutesByVrf> routingTables =
        getEnvironmentRoutingTables(inputPath, answerElement);
    serializeEnvironmentRoutingTables(routingTables, outputPath);
    serializeObject(
        answerElement,
        _testrigSettings.getEnvironmentSettings().getParseEnvironmentRoutingTablesAnswerPath());
    return answer;
  }

  private void serializeEnvironmentRoutingTables(
      SortedMap<String, RoutesByVrf> routingTables, Path outputPath) {
    if (routingTables == null) {
      throw new BatfishException("Exiting due to parsing error(s)");
    }
    _logger.info("\n*** SERIALIZING ENVIRONMENT ROUTING TABLES ***\n");
    _logger.resetTimer();
    outputPath.toFile().mkdirs();
    SortedMap<Path, RoutesByVrf> output = new TreeMap<>();
    routingTables.forEach(
        (name, rt) -> {
          Path currentOutputPath = outputPath.resolve(name);
          output.put(currentOutputPath, rt);
        });
    serializeObjects(output);
    _logger.printElapsedTime();
  }

  //序列化配置文件
  private SortedMap<String, VendorConfiguration> serializeHostConfigs(
      Path testRigPath, Path outputPath, ParseVendorConfigurationAnswerElement answerElement) {
    // 读取配置文件,将配置文件保存成String的形式
    SortedMap<Path, String> configurationData =
        readConfigurationFiles(testRigPath, BfConsts.RELPATH_HOST_CONFIGS_DIR);
    // read the host files
    SortedMap<String, VendorConfiguration> allHostConfigurations;
    //这一部分就是真正的解析配置文件了
    try (ActiveSpan parseHostConfigsSpan =
        GlobalTracer.get().buildSpan("Parse host configs").startActive()) {
      assert parseHostConfigsSpan != null; // avoid unused warning
      allHostConfigurations =
          parseVendorConfigurations(configurationData, answerElement, ConfigurationFormat.HOST);
    }
    if (allHostConfigurations == null) {
      throw new BatfishException("Exiting due to parser errors");
    }
    _logger.infof(
        "Testrig:%s in container:%s has total number of host configs:%d",
        getTestrigName(), getContainerName(), allHostConfigurations.size());

    // split into hostConfigurations and overlayConfigurations
    SortedMap<String, VendorConfiguration> overlayConfigurations =
        allHostConfigurations
            .entrySet()
            .stream()
            .filter(e -> ((HostConfiguration) e.getValue()).getOverlay())
            .collect(toMap(Entry::getKey, Entry::getValue, (v1, v2) -> v1, TreeMap::new));
    SortedMap<String, VendorConfiguration> nonOverlayHostConfigurations =
        allHostConfigurations
            .entrySet()
            .stream()
            .filter(e -> !((HostConfiguration) e.getValue()).getOverlay())
            .collect(toMap(Entry::getKey, Entry::getValue, (v1, v2) -> v1, TreeMap::new));

    // read and associate iptables files for specified hosts
    SortedMap<Path, String> iptablesData = new TreeMap<>();
    readIptableFiles(testRigPath, allHostConfigurations, iptablesData, answerElement);

    SortedMap<String, VendorConfiguration> iptablesConfigurations =
        parseVendorConfigurations(iptablesData, answerElement, ConfigurationFormat.IPTABLES);
    for (VendorConfiguration vc : allHostConfigurations.values()) {
      HostConfiguration hostConfig = (HostConfiguration) vc;
      if (hostConfig.getIptablesFile() != null) {
        Path path = Paths.get(testRigPath.toString(), hostConfig.getIptablesFile());
        String relativePathStr = _testrigSettings.getBasePath().relativize(path).toString();
        if (iptablesConfigurations.containsKey(relativePathStr)) {
          hostConfig.setIptablesVendorConfig(
              (IptablesVendorConfiguration) iptablesConfigurations.get(relativePathStr));
        }
      }
    }

    // now, serialize
    _logger.info("\n*** SERIALIZING VENDOR CONFIGURATION STRUCTURES ***\n");
    _logger.resetTimer();
    CommonUtil.createDirectories(outputPath);

    Map<Path, VendorConfiguration> output = new TreeMap<>();
    nonOverlayHostConfigurations.forEach(
        (name, vc) -> {
          Path currentOutputPath = outputPath.resolve(name);
          output.put(currentOutputPath, vc);
        });
    serializeObjects(output);
    // serialize warnings
    serializeObject(answerElement, _testrigSettings.getParseAnswerPath());
    _logger.printElapsedTime();
    return overlayConfigurations;
  }
  private Answer serializeIndependentConfigs(Path vendorConfigPath) {
    Answer answer = new Answer();
    ConvertConfigurationAnswerElement answerElement = new ConvertConfigurationAnswerElement();
    answerElement.setVersion(Version.getVersion());
    if (_settings.getVerboseParse()) {
      answer.addAnswerElement(answerElement);
    }
    Map<String, Configuration> configurations = getConfigurations(vendorConfigPath, answerElement);//将配置解析为与具体厂商无关的形式,并将其作为类来保存下来
    Topology testrigTopology =
        computeTestrigTopology(_testrigSettings.getTestRigPath(), configurations);//得到拓扑结构这一套后续也可以借鉴


    //序列化保存拓扑
    serializeAsJson(_testrigSettings.getTopologyPath(), testrigTopology, "testrig topology");
    checkTopology(configurations, testrigTopology);
    org.batfish.datamodel.pojo.Topology pojoTopology =
        org.batfish.datamodel.pojo.Topology.create(
            _testrigSettings.getName(), configurations, testrigTopology);
    serializeAsJson(_testrigSettings.getPojoTopologyPath(), pojoTopology, "testrig pojo topology");
    //计算的拓扑结构保存在临时文件的testrig_topology当中
    _storage.storeConfigurations(configurations, answerElement, _testrigSettings.getName());

    //writed by lxz,为每一条边编码,同时将解析完的边存储到特定路径
    SortedSet<Edge> topology_edge=testrigTopology.getEdges();
    Integer edge_num=0;
    HashMap<String,Integer> topology_edge_num=new HashMap<>();
    for(Edge temp:topology_edge)
    {
      String edge_name=temp.getNode1()+"-"+temp.getNode2();
      String edge_name_reverse=temp.getNode2()+"-"+temp.getNode1();
      if(topology_edge_num.get(edge_name)==null)
      {
        topology_edge_num.put(edge_name,edge_num);
        topology_edge_num.put(edge_name_reverse,edge_num);
        edge_num++;
      }
    }
//    System.out.println(topology_edge_num.toString());

    for(String nodeName : configurations.keySet())
    {
      Configuration conf=configurations.get(nodeName);
      conf.setEdgeEncode(topology_edge_num);
    }
    for(String nodeName : configurations.keySet())
    {
      Configuration conf=configurations.get(nodeName);
      HashMap<String,Integer> temp = conf.getEdgeEncode();
    }
    //获取考虑环境的配置文件
    applyEnvironment(configurations);
    Topology envTopology = computeEnvironmentTopology(configurations);
    serializeAsJson(
        _testrigSettings.getEnvironmentSettings().getSerializedTopologyPath(),
        envTopology,
        "environment topology");

    // Compute new auto role data and updates existing auto data with it
    SortedSet<NodeRoleDimension> autoRoles =
        new InferRoles(configurations.keySet(), configurations, this).call();
    Path nodeRoleDataPath = _settings.getContainerDir().resolve(BfConsts.RELPATH_NODE_ROLES_PATH);
    try {
      NodeRolesData.mergeNodeRoleDimensions(nodeRoleDataPath, autoRoles, true);
    } catch (IOException e) {
      _logger.warnf("Could not update node roles in %s: %s", nodeRoleDataPath, e);
    }

    return answer;
  }

  private void serializeNetworkConfigs(
      Path testRigPath,
      Path outputPath,
      ParseVendorConfigurationAnswerElement answerElement,
      SortedMap<String, VendorConfiguration> overlayHostConfigurations) {
    Map<Path, String> configurationData =
        readConfigurationFiles(testRigPath, BfConsts.RELPATH_CONFIGURATIONS_DIR);
    System.out.println("------------------------configuration:--------------------");
//    for(Path temp:configurationData.keySet())
//    {
//      System.out.println("-----------------Path:"+temp.toString());
//      System.out.println(configurationData.get(temp));
//    }
    Map<String, VendorConfiguration> vendorConfigurations;
    try (ActiveSpan parseNetworkConfigsSpan =
        GlobalTracer.get().buildSpan("Parse network configs").startActive()) {
      assert parseNetworkConfigsSpan != null; // avoid unused warning
      vendorConfigurations =
          parseVendorConfigurations(configurationData, answerElement, ConfigurationFormat.UNKNOWN);
      System.out.println("answerelement:"+answerElement.prettyPrint());
    }
    if (vendorConfigurations == null) {
      throw new BatfishException("Exiting due to parser errors");
    }
    _logger.infof(
        "Testrig:%s in container:%s has total number of network configs:%d",
        getTestrigName(), getContainerName(), vendorConfigurations.size());
    _logger.info("\n*** SERIALIZING VENDOR CONFIGURATION STRUCTURES ***\n");
    _logger.resetTimer();
    CommonUtil.createDirectories(outputPath);
    Map<Path, VendorConfiguration> output = new TreeMap<>();
    vendorConfigurations.forEach(
        (name, vc) -> {
          if (name.contains(File.separator)) {
            // iptables will get a hostname like configs/iptables-save if they
            // are not set up correctly using host files
            _logger.errorf("Cannot serialize configuration with hostname %s\n", name);
            answerElement.addRedFlagWarning(
                name,
                new Warning(
                    "Cannot serialize network config. Bad hostname " + name.replace("\\", "/"),
                    "MISCELLANEOUS"));
          } else {
            // apply overlay if it exists
            VendorConfiguration overlayConfig = overlayHostConfigurations.get(name);
            if (overlayConfig != null) {
              vc.setOverlayConfiguration(overlayConfig);
              overlayHostConfigurations.remove(name);
            }

            Path currentOutputPath = outputPath.resolve(name);
            output.put(currentOutputPath, vc);
          }
        });

    // warn about unused overlays
    overlayHostConfigurations.forEach(
        (name, overlay) -> {
          answerElement.getParseStatus().put(name, ParseStatus.ORPHANED);
        });

    serializeObjects(output);
    _logger.printElapsedTime();
  }

  public <S extends Serializable>  void serializeObjects(Map<Path, S> objectsByPath) {
    if (objectsByPath.isEmpty()) {
      return;
    }

    int size = objectsByPath.size();
    String className = objectsByPath.values().iterator().next().getClass().getName();
    AtomicInteger serializeCompleted =
        newBatch(String.format("Serializing '%s' instances to disk", className), size);
    objectsByPath
        .entrySet()
        .parallelStream()
        .forEach(
            entry -> {
              Path outputPath = entry.getKey();
              S object = entry.getValue();
              serializeObject(object, outputPath);
              //serializeCompleted.incrementAndGet();
            });
  }

  Answer serializeVendorConfigs(Path testRigPath, Path outputPath) {
    Answer answer = new Answer();
    boolean configsFound = false;

    // look for network configs
    Path networkConfigsPath = testRigPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR);
    ParseVendorConfigurationAnswerElement answerElement =
        new ParseVendorConfigurationAnswerElement();
    answerElement.setVersion(Version.getVersion());
//    if (_settings.getVerboseParse()) {
//      answer.addAnswerElement(answerElement);
//    }

    if (true) {
      answer.addAnswerElement(answerElement);
    }

    // look for host configs and overlay configs
    // 寻找配置文件的位置,并读取配置文件,然后序列化配置文件,分为两部分分别是读取主机的配置文件和网络路由器的配置文件
    SortedMap<String, VendorConfiguration> overlayHostConfigurations = new TreeMap<>();
    Path hostConfigsPath = testRigPath.resolve(BfConsts.RELPATH_HOST_CONFIGS_DIR);

//    System.out.println("----------------------back-batfish-hostConfigsPath:---------------------");
//    System.out.println(hostConfigsPath);
//    System.out.println("---------------------------------End-----------------------------------");
//
//    System.out.println("----------------------back-batfish-networkConfigsPath:---------------------");
//    System.out.println(networkConfigsPath);
//    System.out.println("---------------------------------End-----------------------------------");

    if (Files.exists(hostConfigsPath)) {
      overlayHostConfigurations = serializeHostConfigs(testRigPath, outputPath, answerElement);
      configsFound = true;
    }

    if (Files.exists(networkConfigsPath)) {
      serializeNetworkConfigs(testRigPath, outputPath, answerElement, overlayHostConfigurations);
      configsFound = true;
    }

    // look for AWS VPC configs
    Path awsVpcConfigsPath = testRigPath.resolve(BfConsts.RELPATH_AWS_CONFIGS_DIR);
    if (Files.exists(awsVpcConfigsPath)) {
      answer.append(serializeAwsConfigs(testRigPath, outputPath));
      configsFound = true;
    }

    if (!configsFound) {
      throw new BatfishException("No valid configurations found");
    }

    // serialize warnings
    serializeObject(answerElement, _testrigSettings.getParseAnswerPath());

    return answer;
  }

  @Override
  public void registerDataPlanePlugin(DataPlanePlugin plugin, String name) {
    _dataPlanePlugins.put(name, plugin);
  }

  public void setTerminatingExceptionMessage(String terminatingExceptionMessage) {
    _terminatingExceptionMessage = terminatingExceptionMessage;
  }

  private AnswerElement singleReachability(
      ReachabilitySettings reachabilitySettings,
      ReachabilityQuerySynthesizer.Builder<?, ?> builder) {
    Settings settings = getSettings();
    String tag = getFlowTag(_testrigSettings);
    Set<ForwardingAction> actions = reachabilitySettings.getActions();
    boolean useCompression = reachabilitySettings.getUseCompression();

    // specialized compression
    /*
    CompressDataPlaneResult compressionResult =
        useCompression ? computeCompressedDataPlane(headerSpace) : null;
    Map<String, Configuration> configurations =
        useCompression ? compressionResult._compressedConfigs : loadConfigurations();
    DataPlane dataPlane = useCompression ? compressionResult._compressedDataPlane : loadDataPlane();
    */

    // general compression
    Snapshot snapshot = getSnapshot();
    Map<String, Configuration> configurations =
        useCompression ? loadCompressedConfigurations(snapshot) : loadConfigurations(snapshot);
    DataPlane dataPlane = loadDataPlane(useCompression);

    if (configurations == null) {
      throw new BatfishException("error loading configurations");
    }

    if (dataPlane == null) {
      throw new BatfishException("error loading data plane");
    }

    Set<String> activeIngressNodes;
    Set<String> activeFinalNodes;
    HeaderSpace headerSpace;
    Set<String> transitNodes;
    Set<String> nonTransitNodes;
    int maxChunkSize;

    try {
      activeIngressNodes = reachabilitySettings.computeActiveIngressNodes(this);
      activeFinalNodes = reachabilitySettings.computeActiveFinalNodes(this);
      headerSpace = reachabilitySettings.getHeaderSpace();
      transitNodes = reachabilitySettings.computeActiveTransitNodes(this);
      nonTransitNodes = reachabilitySettings.computeActiveNonTransitNodes(this);
      maxChunkSize = reachabilitySettings.getMaxChunkSize();
      reachabilitySettings.validateTransitNodes(this);
    } catch (InvalidReachabilitySettingsException e) {
      return e.getInvalidSettingsAnswer();
    }

    List<IngressPoint> ingressPoints = new ArrayList<>();
    if (reachabilitySettings.getIngressInterfaces() != InterfacesSpecifier.NONE) {
      // originate from specified interfaces
      activeIngressNodes
          .stream()
          .flatMap(
              ingressNode ->
                  configurations
                      .get(ingressNode)
                      .getInterfaces()
                      .values()
                      .stream()
                      .filter(reachabilitySettings.getIngressInterfaces()::matches)
                      .map(iface -> IngressPoint.ingressInterface(ingressNode, iface.getName())))
          .forEach(ingressPoints::add);
    } else {
      // originate from VRFs
      activeIngressNodes
          .stream()
          .flatMap(
              ingressNode ->
                  configurations
                      .get(ingressNode)
                      .getVrfs()
                      .keySet()
                      .stream()
                      .map(ingressVrf -> IngressPoint.ingressVrf(ingressNode, ingressVrf)))
          .forEach(ingressPoints::add);
    }

    int chunkSize =
        Math.max(1, Math.min(maxChunkSize, ingressPoints.size() / _settings.getAvailableThreads()));

    // partition originateNodeVrfs into chunks
    List<List<IngressPoint>> ingressPointChunks = Lists.partition(ingressPoints, chunkSize);

    Synthesizer dataPlaneSynthesizer =
        synthesizeDataPlane(
            configurations,
            dataPlane,
            loadForwardingAnalysis(configurations, dataPlane),
            headerSpace,
            nonTransitNodes,
            reachabilitySettings.getSpecialize(),
            transitNodes);

    // build query jobs
    List<NodJob> jobs =
        ingressPointChunks
            .stream()
            .map(ImmutableSortedSet::copyOf)
            .map(
                chunkIngressPoints -> {
                  ImmutableMultimap.Builder<String, String> ingressNodeInterfacesBuilder =
                      ImmutableMultimap.builder();
                  ImmutableMultimap.Builder<String, String> ingressNodeVrfsBuilder =
                      ImmutableMultimap.builder();
                  chunkIngressPoints.forEach(
                      ingressPoint -> {
                        if (ingressPoint.isIngressInterface()) {
                          ingressNodeInterfacesBuilder.put(
                              ingressPoint.getNode(), ingressPoint.getInterface());
                        } else if (ingressPoint.isIngressVrf()) {
                          ingressNodeVrfsBuilder.put(ingressPoint.getNode(), ingressPoint.getVrf());
                        } else {
                          throw new BatfishException("Unexpected IngressPoint type");
                        }
                      });
                  Multimap<String, String> ingressNodeInterfaces =
                      ingressNodeInterfacesBuilder.build();
                  Multimap<String, String> ingressNodeVrfs = ingressNodeVrfsBuilder.build();

                  ReachabilityQuerySynthesizer query =
                      builder
                          .setActions(actions)
                          .setHeaderSpace(headerSpace)
                          .setFinalNodes(activeFinalNodes)
                          .setIngressNodeInterfaces(ingressNodeInterfaces)
                          .setIngressNodeVrfs(ingressNodeVrfs)
                          .setTransitNodes(transitNodes)
                          .setNonTransitNodes(nonTransitNodes)
                          .setSrcNatted(reachabilitySettings.getSrcNatted())
                          .build();

                  return new NodJob(
                      settings,
                      dataPlaneSynthesizer,
                      query,
                      chunkIngressPoints,
                      tag,
                      reachabilitySettings.getSpecialize());
                })
            .collect(Collectors.toList());

    // run jobs and get resulting flows
    Set<Flow> flows = computeNodOutput(jobs);

    getDataPlanePlugin().processFlows(flows, loadDataPlane(), false);

    AnswerElement answerElement = getHistory();
    return answerElement;
  }

  @Override
  public AnswerElement smtBlackhole(HeaderQuestion q) {
    PropertyChecker p = new PropertyChecker(this, _settings);
    return p.checkBlackHole(q);
  }

  @Override
  public AnswerElement smtBoundedLength(HeaderLocationQuestion q, Integer bound) {
    if (bound == null) {
      throw new BatfishException("Missing parameter length bound: (e.g., bound=3)");
    }
    PropertyChecker p = new PropertyChecker(this, _settings);
    return p.checkBoundedLength(q, bound);
  }

  @Override
  public AnswerElement smtDeterminism(HeaderQuestion q) {
    PropertyChecker p = new PropertyChecker(this, _settings);
    return p.checkDeterminism(q);
  }

  @Override
  public AnswerElement smtEqualLength(HeaderLocationQuestion q) {
    PropertyChecker p = new PropertyChecker(this, _settings);
    return p.checkEqualLength(q);
  }

  @Override
  public AnswerElement smtForwarding(HeaderQuestion q) {
    PropertyChecker p = new PropertyChecker(this, _settings);
    return p.checkForwarding(q);
  }

  @Override
  public AnswerElement smtLoadBalance(HeaderLocationQuestion q, int threshold) {
    PropertyChecker p = new PropertyChecker(this, _settings);
    return p.checkLoadBalancing(q, threshold);
  }

  @Override
  public AnswerElement smtLocalConsistency(Pattern routerRegex, boolean strict, boolean fullModel) {
    PropertyChecker p = new PropertyChecker(this, _settings);
    return p.checkLocalEquivalence(routerRegex, strict, fullModel);
  }

  @Override
  public AnswerElement smtMultipathConsistency(HeaderLocationQuestion q) {
    PropertyChecker p = new PropertyChecker(this, _settings);
    return p.checkMultipathConsistency(q);
  }

  @Override
  public AnswerElement smtReachability(HeaderLocationQuestion q) {
    PropertyChecker p = new PropertyChecker(this, _settings);
    return p.checkReachability(q);
  }

  @Override
  public AnswerElement smtRoles(RoleQuestion q) {
    Roles roles = Roles.create(this, q.getDstIps(), new NodesSpecifier(q.getNodeRegex()));
    return roles.asAnswer(q.getType());
  }

  @Override
  public AnswerElement smtRoutingLoop(HeaderQuestion q) {
    PropertyChecker p = new PropertyChecker(this, _settings);
    return p.checkRoutingLoop(q);
  }

  @Override
  public AnswerElement standard(ReachabilitySettings reachabilitySettings) {
    return singleReachability(reachabilitySettings, StandardReachabilityQuerySynthesizer.builder());
  }

  private Synthesizer synthesizeAcls(Map<String, Configuration> configurations) {
    _logger.info("\n*** GENERATING Z3 LOGIC ***\n");
    _logger.resetTimer();

    _logger.info("Synthesizing Z3 ACL logic...");
    Synthesizer s =
        new Synthesizer(
            SynthesizerInputImpl.builder()
                .setConfigurations(configurations)
                .setSimplify(_settings.getSimplify())
                .build());

    List<String> warnings = s.getWarnings();
    int numWarnings = warnings.size();
    if (numWarnings == 0) {
      _logger.info("OK\n");
    } else {
      for (String warning : warnings) {
        _logger.warn(warning);
      }
    }
    _logger.printElapsedTime();
    return s;
  }

  public Synthesizer synthesizeDataPlane() {
    SortedMap<String, Configuration> configurations = loadConfigurations();
    DataPlane dataPlane = loadDataPlane();
    ForwardingAnalysis forwardingAnalysis = loadForwardingAnalysis(configurations, dataPlane);
    return synthesizeDataPlane(
        configurations,
        dataPlane,
        forwardingAnalysis,
        new HeaderSpace(),
        ImmutableSet.of(),
        false,
        ImmutableSet.of());
  }

  @Nonnull
  public Synthesizer synthesizeDataPlane(
      Map<String, Configuration> configurations,
      DataPlane dataPlane,
      ForwardingAnalysis forwardingAnalysis,
      HeaderSpace headerSpace,
      Set<String> nonTransitNodes,
      boolean specialize,
      Set<String> transitNodes) {
    _logger.info("\n*** GENERATING Z3 LOGIC ***\n");
    _logger.resetTimer();

    _logger.info("Synthesizing Z3 logic...");

    Synthesizer s =
        new Synthesizer(
            computeSynthesizerInput(
                configurations,
                dataPlane,
                forwardingAnalysis,
                headerSpace,
                nonTransitNodes,
                _settings.getSimplify(),
                specialize,
                transitNodes));

    List<String> warnings = s.getWarnings();
    int numWarnings = warnings.size();
    if (numWarnings == 0) {
      _logger.info("OK\n");
    } else {
      for (String warning : warnings) {
        _logger.warn(warning);
      }
    }
    _logger.printElapsedTime();
    return s;
  }

  public static SynthesizerInputImpl computeSynthesizerInput(
      Map<String, Configuration> configurations,
      DataPlane dataPlane,
      ForwardingAnalysis forwardingAnalysis,
      HeaderSpace headerSpace,
      Set<String> nonTransitNodes,
      boolean simplify,
      boolean specialize,
      Set<String> transitNodes) {
    Topology topology = new Topology(dataPlane.getTopologyEdges());
    return SynthesizerInputImpl.builder()
        .setConfigurations(configurations)
        .setForwardingAnalysis(forwardingAnalysis)
        .setHeaderSpace(headerSpace)
        .setNonTransitNodes(nonTransitNodes)
        .setSimplify(simplify)
        .setSpecialize(specialize)
        .setTopology(topology)
        .setTransitNodes(transitNodes)
        .build();
  }

  private Answer validateEnvironment() {
    Answer answer = new Answer();
    ValidateEnvironmentAnswerElement ae = loadValidateEnvironmentAnswerElement();
    answer.addAnswerElement(ae);
    Topology envTopology = computeEnvironmentTopology(loadConfigurations());
    serializeAsJson(
        _testrigSettings.getEnvironmentSettings().getSerializedTopologyPath(),
        envTopology,
        "environment topology");
    return answer;
  }

  @Override
  public void writeDataPlane(DataPlane dp, DataPlaneAnswerElement ae) {
    _cachedDataPlanes.put(_testrigSettings, dp);
    serializeObject(dp, _testrigSettings.getEnvironmentSettings().getDataPlanePath());
    serializeObject(ae, _testrigSettings.getEnvironmentSettings().getDataPlaneAnswerPath());
  }

  private void writeJsonAnswer(String structuredAnswerString) {
    // TODO Reduce calls to _settings to deobfuscate this method's purpose and dependencies.
    // Purpose: to write answer json files for adhoc and analysis questions
    // Dependencies: Container, tr & env, (delta tr & env), question name, analysis name if present
    boolean diff = _settings.getDiffQuestion();
    String baseEnvName = _testrigSettings.getEnvironmentSettings().getName();
    Path answerDir;

    if (_settings.getQuestionName() != null) {
      // If settings has a question name, we're answering an adhoc question. Set up path accordingly
      Path testrigDir = _testrigSettings.getBasePath();
      answerDir =
          testrigDir.resolve(
              Paths.get(BfConsts.RELPATH_ANSWERS_DIR, _settings.getQuestionName(), baseEnvName));
      if (diff) {
        String deltaTestrigName = _deltaTestrigSettings.getName();
        String deltaEnvName = _deltaTestrigSettings.getEnvironmentSettings().getName();
        answerDir =
            answerDir.resolve(Paths.get(BfConsts.RELPATH_DIFF_DIR, deltaTestrigName, deltaEnvName));
      } else {
        answerDir = answerDir.resolve(Paths.get(BfConsts.RELPATH_STANDARD_DIR));
      }
    } else if (_settings.getAnalysisName() != null && _settings.getQuestionPath() != null) {
      // If settings has an analysis name and question path, we're answering an analysis question
      Path questionDir = _settings.getQuestionPath().getParent();
      answerDir = questionDir.resolve(Paths.get(BfConsts.RELPATH_ENVIRONMENTS_DIR, baseEnvName));
      if (diff) {
        answerDir =
            answerDir.resolve(
                Paths.get(
                    BfConsts.RELPATH_DELTA,
                    _deltaTestrigSettings.getName(),
                    _deltaTestrigSettings.getEnvironmentSettings().getName()));
      }
    } else {
      // If settings has neither a question nor an analysis configured, don't write a file
      return;
    }
    Path structuredAnswerPath = answerDir.resolve(BfConsts.RELPATH_ANSWER_JSON);
    answerDir.toFile().mkdirs();
    CommonUtil.writeFile(structuredAnswerPath, structuredAnswerString);
  }

  private void writeJsonAnswerWithLog(@Nullable String logString, String structuredAnswerString) {
    // Write log of WorkItem task to the configured path for logs
    Path jsonPath = _settings.getAnswerJsonPath();
    if (jsonPath != null && logString != null) {
      CommonUtil.writeFile(jsonPath, logString);
    }
    // Write answer.json and answer-pretty.json if WorkItem was answering a question
    writeJsonAnswer(structuredAnswerString);
  }

  private void writeJsonTopology() {
    try {
      Map<String, Configuration> configs = loadConfigurations();
      SortedSet<Edge> textEdges = CommonUtil.synthesizeTopology(configs).getEdges();
      JSONArray jEdges = new JSONArray();
      for (Edge textEdge : textEdges) {
        Configuration node1 = configs.get(textEdge.getNode1());
        Configuration node2 = configs.get(textEdge.getNode2());
        Interface interface1 = node1.getInterfaces().get(textEdge.getInt1());
        Interface interface2 = node2.getInterfaces().get(textEdge.getInt2());
        JSONObject jEdge = new JSONObject();
        jEdge.put("interface1", interface1.toJSONObject());
        jEdge.put("interface2", interface2.toJSONObject());
        jEdges.put(jEdge);
      }
      JSONObject master = new JSONObject();
      JSONObject topology = new JSONObject();
      topology.put("edges", jEdges);
      master.put("topology", topology);
      String text = master.toString(3);
      _logger.output(text);
    } catch (JSONException e) {
      throw new BatfishException("Failed to synthesize JSON topology", e);
    }
  }
}
