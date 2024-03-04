package ir.ac.ut.fml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.checkerframework.checker.nullness.qual.Nullable;

import be.vibes.fexpression.Feature;
import be.vibes.fexpression.configuration.SimpleConfiguration;
import br.usp.icmc.labes.mealyInference.Infer_LearnLib;
import br.usp.icmc.labes.mealyInference.utils.ExperimentAndLearner;
import br.usp.icmc.labes.mealyInference.utils.LearnLibProperties;
import br.usp.icmc.labes.mealyInference.utils.MyObservationTable;
import br.usp.icmc.labes.mealyInference.utils.OTUtils;
import br.usp.icmc.labes.mealyInference.utils.Utils;
import br.usp.icmc.labes.mealyInference.utils.EquivEQOracle.RandomWMethodHypEQOracle;
import br.usp.icmc.labes.mealyInference.utils.EquivEQOracle.WMethodHypEQOracle;
import br.usp.icmc.labes.mealyInference.utils.EquivEQOracle.WpMethodHypEQOracle;
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandler;
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstar.closing.ClosingStrategies;
import de.learnlib.algorithms.lstar.closing.ClosingStrategy;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealy;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.api.SUL;
import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.oracle.MembershipOracle.MealyMembershipOracle;
import de.learnlib.api.statistic.StatisticSUL;
import de.learnlib.datastructure.observationtable.ObservationTable;
import de.learnlib.datastructure.observationtable.writer.ObservationTableASCIIWriter;
import de.learnlib.driver.util.MealySimulatorSUL;
import de.learnlib.filter.cache.sul.SULCache;
import de.learnlib.filter.statistic.sul.ResetCounterSUL;
import de.learnlib.filter.statistic.sul.SymbolCounterSUL;
import de.learnlib.oracle.equivalence.RandomWMethodEQOracle;
import de.learnlib.oracle.equivalence.RandomWordsEQOracle;
import de.learnlib.oracle.equivalence.WMethodEQOracle;
import de.learnlib.oracle.equivalence.WpMethodEQOracle;
import de.learnlib.oracle.equivalence.mealy.RandomWalkEQOracle;
import de.learnlib.oracle.membership.SULOracle;
import de.learnlib.util.ExperimentDebug.MealyExperiment;
import de.learnlib.util.statistics.SimpleProfiler;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.commons.util.Pair;
import net.automatalib.serialization.InputModelDeserializer;
import net.automatalib.serialization.dot.DOTParsers;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.visualization.VisualizationHelper.EdgeAttrs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.MapAlphabet;
import uk.le.ac.ffsm.FeaturedMealyUtils;
import uk.le.ac.fts.FtsUtils;

public class LearningOrderSampling {

	public static final String SOT = "sot";
	public static final String SOT2 = "sot2";
	public static final String HELP = "help";
	public static final String HELP_SHORT = "h";
	public static final String OT = "ot";
	public static final String CEXH = "cexh";
	public static final String CLOS = "clos";
	public static final String EQ = "eq";
	public static final String CACHE = "cache";
	public static final String SEED = "seed";
	public static final String OUT = "out";
	public static final String LEARN = "learn";
	public static final String INFO = "info";
	public static final String DIR = "dir";
	private static final String FM = "fm";

	public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	public static final String[] eqMethodsAvailable = { "rndWalk", "rndWords", "wp", "wphyp", "w", "whyp", "wrnd",
			"wrndhyp" };
	public static final String[] closingStrategiesAvailable = { "CloseFirst", "CloseShortest", "CloseRandom" };
	private static final String RIVEST_SCHAPIRE_ALLSUFFIXES = "RivestSchapireAllSuffixes";
	public static final String[] cexHandlersAvailable = { "ClassicLStar", "MalerPnueli", "RivestSchapire",
			RIVEST_SCHAPIRE_ALLSUFFIXES, "Shahbaz", "Suffix1by1" };
	public static final String[] learningMethodsAvailable = { "lstar", "l1", "adaptive", "dlstar_v2", "dlstar_v1",
			"dlstar_v0", "ttt" };

	public static final Function<Map<String, String>, Pair<@Nullable String, @Nullable Word<String>>> MEALY_EDGE_WORD_STR_PARSER = attr -> {
		final String label = attr.get(EdgeAttrs.LABEL);
		if (label == null) {
			return Pair.of(null, null);
		}

		final String[] tokens = label.split("/");

		if (tokens.length != 2) {
			return Pair.of(null, null);
		}

		Word<String> token2 = Word.epsilon();
		token2 = token2.append(tokens[1]);
		return Pair.of(tokens[0], token2);
	};

//	static long seed = System.currentTimeMillis();
//	static long seed = 1649657577795L;
	
//	Minepump SPL:
	static long seed = 1706945802091L;
	
//	BCS SPL:
//	static long seed = 1706863099383L;
	
	private static Random rand = new Random(seed);

	public static void enumeratePermutations(ArrayList<Integer> remaining, ArrayList<Integer> result) {
		if (remaining.size() == 0) {
			// process the permutation:
			if (rand.nextInt(100) < 60) {
				for (int n : result)
					System.out.print(n + " ");
				System.out.println();
			}
			return;
		}
		for (int i = 0; i < remaining.size(); i++) {
			int ithElement = remaining.get(i);
			result.add(ithElement);
			remaining.remove(i);
			enumeratePermutations(remaining, result);
			remaining.add(i, ithElement);
			result.remove(result.size() - 1);
		}
	}

	public static List<List<Integer>> generate(ArrayList<Integer> original, int count) {
		List<List<Integer>> perms = new ArrayList<>();
		while (perms.size() < count) {
			ArrayList<Integer> list = new ArrayList<>(original);
			Collections.shuffle(list, rand);
			if (!perms.contains(list))
				perms.add(list);
		}
		return perms;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out.println(seed);

		// create the command line parser
		CommandLineParser parser = new BasicParser();
		// create the Options
		Options options = createOptions();
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();

		int array_length = 7;

		String product_selection_method = "specified_order";

		try {

			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			if (line.hasOption(HELP)) {
				formatter.printHelp("ProductsAdaptiveLearning", options);
				System.exit(0);
			}

			if (!line.hasOption(DIR)) {
				throw new IllegalArgumentException("Must provide a Directory (containing SPL products)");
			}

			// if passed as argument, set OT path
			File obsTable = null;
			if (line.hasOption(OT)) {
				obsTable = new File(line.getOptionValue(OT));
			}

			// set output dir
			File out_dir = new File(line.getOptionValue(OUT));
			// create log
			System.setProperty("logdir", out_dir.getAbsolutePath());
			LearnLogger logger_1 = LearnLogger.getLogger(Infer_LearnLib.class);

			File products_dir = new File(line.getOptionValue(DIR));
			File[] filesList = products_dir.listFiles();

			int sample_num = 0;

			// Counting the number of products in the directory
			int products_num = 0;
			for (int i = 0; i < filesList.length; i++) {
				File pFile = filesList[i];
				String fileName = "";
				String fileCompleteName = "";
				fileCompleteName = pFile.getName();
				if (fileCompleteName.endsWith("_fsm.dot")) {
					products_num += 1;
				}
			}

			double[][] products_similarity = new double[products_num][products_num];

//			System.out.println("\n" + Arrays.deepToString(products_similarity));

			String learningAlgorithm = "lstar_r";
			String learningAlgorithm_2 = "SPL_adaptive_learning_v2_r";
			String learningAlgorithm_3 = "lstar_r";
			int samples_count = 200;
//			int samples_count = 1;

			ArrayList<Integer> list = new ArrayList<>();
			for (int i = 0; i < products_num; i++) {
				list.add(i + 1);
			}

//			// For testing some specified orders
//			List<List<Integer>> result = new ArrayList<>();
//			List<Integer> r = new ArrayList<>();
//			Collections.addAll(r, 8, 7, 6, 5, 4, 3, 2, 1);
//			result.add(r);
//			r = new ArrayList<>();
//			Collections.addAll(r, 4, 2, 6, 5, 3, 1, 8, 7);
//			result.add(r);
//			System.out.println(result.toString());

			// enumeratePermutations(list, new ArrayList<Integer>());
			List<List<Integer>> result = generate(list, samples_count);

			for (List<Integer> l : result) {

				int[] statistics_adaptive = new int[array_length];
				Arrays.fill(statistics_adaptive, 0);

				int[] statistics_nonadaptive = new int[array_length];
				Arrays.fill(statistics_nonadaptive, 0);

				ArrayList<ObservationTable> ot_list = new ArrayList<>();

				ArrayList<String[]> ordered_configs = new ArrayList<String[]>();

				int[] product_order = new int[products_num];
				int index = 0;
				for (Integer p : l) {
					product_order[index] = p;
					index += 1;
				}
				sample_num += 1;
				System.out.println(sample_num + ", Learning order:" + Arrays.toString(product_order));
				logger_1.logEvent("New learning order");
				logger_1.logEvent("\nLearning order: " + Arrays.toString(product_order));

				double[][] configs_properties = new double[products_num][2];
				SimpleConfiguration[] sampled_configs = new SimpleConfiguration[products_num];
				for (int i = 1; i < (products_num + 1); i++) {
					// Feature list of config_i
					String fixedLengthString_i = ConvertTofixedLengthString(i);
					String configFileName_i = fixedLengthString_i + ".config";
					File configFile_i = new File(products_dir, configFileName_i);
					String config_i_string = configFile_i.getPath();
					SimpleConfiguration config_i = FtsUtils.getInstance().loadConfiguration(config_i_string);
					sampled_configs[i - 1] = config_i;
					Feature[] config_i_features = config_i.getFeatures();
					List<String> features_i = new ArrayList<>();

					configs_properties[i - 1][0] = 0;
					configs_properties[i - 1][1] = config_i_features.length;

					for (Feature f : config_i_features) {
						features_i.add(f.toString());
					}

					for (int j = 1; j < (products_num + 1); j++) {
						// Feature list of config_j
						String fixedLengthString_j = ConvertTofixedLengthString(j);
						String configFileName_j = fixedLengthString_j + ".config";
						File configFile_j = new File(products_dir, configFileName_j);
						String config_j_string = configFile_j.getPath();
						SimpleConfiguration config_j = FtsUtils.getInstance().loadConfiguration(config_j_string);
						Feature[] config_j_features = config_j.getFeatures();
						List<String> features_j = new ArrayList<>();
						for (Feature f : config_j_features) {
							features_j.add(f.toString());
						}

					}
				}

				// start learning using this order
				List<String> covered_features = new ArrayList<>();

				int[] selected_products = new int[products_num];

				int previous_product = -1;

				List<Feature> selected_features = new ArrayList<>();
				List<Feature> product_features = new ArrayList<>();
				List<List<Feature>> ot_features_list = new ArrayList<>();

				// Set the first product to be learned
				int productIndex_1 = NextProductIndex(selected_products, product_selection_method, products_similarity,
						product_order, configs_properties);

//					System.out.println(Arrays.deepToString(configs_properties));

				selected_products[productIndex_1 - 1] = 1;

				configs_properties = UpdateProperties(configs_properties, productIndex_1, sampled_configs,
						selected_features);
//					System.out.println(Arrays.deepToString(configs_properties));
				selected_features = UpdateSelectedFeatures(selected_features, productIndex_1, products_dir);
//					System.out.println(selected_features.toString());

				product_features = UpdateProductFeatures(product_features, productIndex_1, products_dir);
//				System.out.println(product_features.toString());
				ot_features_list.add(product_features);
				String[] product_features_2 = new String[product_features.size()];
				int f_index = 0;
				for (Feature f : product_features) {
					product_features_2[f_index] = f.toString();
					f_index += 1;
				}
				ordered_configs.add(product_features_2);

				String fixedLengthString_1 = ConvertTofixedLengthString(productIndex_1);
				String productFileName_1 = fixedLengthString_1 + "_fsm.dot";
				File productFile_1 = new File(products_dir, productFileName_1);

				statistics_adaptive = LearnFSM(productFile_1, ot_list, learningAlgorithm, out_dir, line, seed,
						statistics_adaptive, 1, logger_1, ot_features_list);
				statistics_nonadaptive = LearnFSM(productFile_1, ot_list, learningAlgorithm, out_dir, line, seed,
						statistics_nonadaptive, 0, logger_1, ot_features_list);

//					System.out.println(ot_list.size());

				for (int i = 0; i < (selected_products.length - 1); i++) {
					int productIndex_2 = NextProductIndex(selected_products, product_selection_method,
							products_similarity, product_order, configs_properties);
					selected_products[productIndex_2 - 1] = 1;
//					System.out.println(Arrays.toString(selected_products));

					configs_properties = UpdateProperties(configs_properties, productIndex_2, sampled_configs,
							selected_features);
//						System.out.println(Arrays.deepToString(configs_properties));
					selected_features = UpdateSelectedFeatures(selected_features, productIndex_2, products_dir);
//						System.out.println(selected_features.toString());

					product_features = new ArrayList<>();
					product_features = UpdateProductFeatures(product_features, productIndex_2, products_dir);
//					System.out.println(product_features.toString());
					ot_features_list.add(product_features);
					product_features_2 = new String[product_features.size()];
					f_index = 0;
					for (Feature f : product_features) {
						product_features_2[f_index] = f.toString();
						f_index += 1;
					}
					ordered_configs.add(product_features_2);

					String fixedLengthString_2 = ConvertTofixedLengthString(productIndex_2);
					String productFileName_2 = fixedLengthString_2 + "_fsm.dot";
					File productFile_2 = new File(products_dir, productFileName_2);

					statistics_adaptive = LearnFSM(productFile_2, ot_list, learningAlgorithm_2, out_dir, line, seed,
							statistics_adaptive, 1, logger_1, ot_features_list);

					statistics_nonadaptive = LearnFSM(productFile_2, ot_list, learningAlgorithm_3, out_dir, line, seed,
							statistics_nonadaptive, 0, logger_1, ot_features_list);

					if (i == selected_products.length - 2) {
						System.out.println("Adaptive method:");
						System.out.println(Arrays.toString(statistics_adaptive));
						System.out.println(
								"[Rounds, MQ [Resets], MQ [Symbols], EQ [Resets], EQ [Symbols]], Total resets, Total Symbols\n");
						logger_1.logEvent("Adaptive method: " + Arrays.toString(statistics_adaptive));

						System.out.println("Non-adaptive method:");
						System.out.println(Arrays.toString(statistics_nonadaptive));
						System.out.println("[Rounds, MQ [Resets], MQ [Symbols], EQ [Resets], EQ [Symbols]]\n");
						logger_1.logEvent("Non-adaptive method: " + Arrays.toString(statistics_nonadaptive));
					}
				}

//				System.out.println("Product selection method: " + product_selection_method);
//				System.out.println("Adaptive learning algorithm: " + learningAlgorithm_2);

			}
		} catch (Exception exp) {
			// automatically generate the help statement
			formatter.printHelp("Optioms:", options);
			System.err.println("Unexpected Exception");
			exp.printStackTrace();
		}

		System.out.println("Finished");

	}

	private static List<int[]> computePermutations(List<int[]> primary_list) {
		// TODO Auto-generated method stub
		int p_num = primary_list.size();
		if (primary_list.isEmpty()) {
			List<int[]> result = new ArrayList<int[]>();
			result.add(new int[p_num]);
			return result;
		}
		return null;
	}

	private static List<Feature> UpdateProductFeatures(List<Feature> selected_features_list, int productIndex,
			File products_dir_1) {
		// TODO Auto-generated method stub
		String fixedLengthString = ConvertTofixedLengthString(productIndex);
		String configFileName = fixedLengthString + ".config";
		File configFile = new File(products_dir_1, configFileName);
		String config_string = configFile.getPath();
		try {
			SimpleConfiguration config = FtsUtils.getInstance().loadConfiguration(config_string);
			Feature[] configs_features = config.getFeatures();
			for (Feature f : configs_features) {
				selected_features_list.add(f);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return selected_features_list;
	}

	private static List<Feature> UpdateSelectedFeatures(List<Feature> selected_features_list, int productIndex,
			File products_dir_1) {
		// TODO Auto-generated method stub
		String fixedLengthString = ConvertTofixedLengthString(productIndex);
		String configFileName = fixedLengthString + ".config";
		File configFile = new File(products_dir_1, configFileName);
		String config_string = configFile.getPath();
		try {
			SimpleConfiguration config = FtsUtils.getInstance().loadConfiguration(config_string);
			Feature[] configs_features = config.getFeatures();
			for (Feature f : configs_features) {
				if (!selected_features_list.contains(f)) {
					selected_features_list.add(f);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return selected_features_list;
	}

	private static double[][] UpdateProperties(double[][] configs_properties_array, int productIndex,
			SimpleConfiguration[] sampled_configs_array, List<Feature> selected_features_list) {
		// TODO Auto-generated method stub
		int current_index = productIndex - 1;
		SimpleConfiguration current_config = sampled_configs_array[current_index];
		Feature[] added_features = current_config.getFeatures();
		for (int i = 0; i < sampled_configs_array.length; i++) {
			Feature[] features_i = sampled_configs_array[i].getFeatures();
			for (Feature f : added_features) {
				if (!selected_features_list.contains(f) && Arrays.asList(features_i).contains(f)) {
					configs_properties_array[i][0]++;
				}
			}
		}

		return configs_properties_array;
	}

	private static double ConfigurationSimilarity(List<String> features_i_1, List<String> features_j_1,
			List<String> all_features_1) {
		// TODO Auto-generated method stub
		List<String> intersection_i_j = Intersection(features_i_1, features_j_1);

		List<String> all_minus_i = Difference(all_features_1, features_i_1);

		List<String> all_minus_j = Difference(all_features_1, features_j_1);

		List<String> intersection_remained = Intersection(all_minus_i, all_minus_j);

		double similarity = (intersection_i_j.size() + intersection_remained.size()) / ((double) all_features_1.size());
//		System.out.println(similarity);

		return similarity;
	}

	private static List<String> Intersection(List<String> list_1, List<String> list_2) {
		List<String> intersection_list = new ArrayList<>();
		for (String f : list_1) {
			if (list_2.contains(f)) {
				intersection_list.add(f);
			}
		}
		return intersection_list;
	}

	private static List<String> Difference(List<String> list_1, List<String> list_2) {
		List<String> difference_list = new ArrayList<>();
		for (String f : list_1) {
			if (!list_2.contains(f)) {
				difference_list.add(f);
			}
		}
		return difference_list;
	}

	private static String ConvertTofixedLengthString(int productIndex) {
		// TODO Auto-generated method stub
		String productIndexString = Integer.toString(productIndex);
		String fixedLengthString = "00000".substring(productIndexString.length()) + productIndexString;
		return fixedLengthString;
	}

	// Important method
	private static int NextProductIndex(int[] selected_p_array, String method, double[][] p_similarity,
			int[] product_order_1, double[][] configs_properties_array) {
		ArrayList<Integer> return_list = new ArrayList<Integer>();
		int first_num = 0;
		int remained_p_count = 0;
		int p_num = selected_p_array.length;
		for (int k = 0; k < p_num; k++) {
			if (selected_p_array[k] == 0) {
				remained_p_count += 1;
			}
		}

		int selected_p_count = p_num - remained_p_count;

		if (method.equals("total_random")) {
			Random rand = new Random();
			int random_num = rand.nextInt(remained_p_count) + 1;
//			System.out.println(random_num);

			int num = 0;
			for (int k = 0; k < p_num; k++) {
//				System.out.println(selected_p_array[k]);
				if (selected_p_array[k] == 0) {
					num += 1;
				}
				if (num == random_num) {
//					System.out.println(k +1);
					first_num = k + 1;
//					System.out.println("first_num:" + first_num);
					break;
				}
			}
		} else {
			if (method.equals("specified_order")) {
				first_num = product_order_1[selected_p_count];
//				System.out.println("first_num:" + first_num);
			}
			// hello
			if (method.equals("feature_coverage")) {
				double max_coverage = 0;
				List<Integer> equal_max_index = new ArrayList();
				equal_max_index.add(0);
				for (int i = 0; i < p_num; i++) {
					if (selected_p_array[i] == 0) {
						double feature_coverage = configs_properties_array[i][0] / configs_properties_array[i][1];
						if (feature_coverage == max_coverage) {
							equal_max_index.add(i);
						}
						if (feature_coverage > max_coverage) {
							max_coverage = feature_coverage;
							equal_max_index = new ArrayList();
							equal_max_index.add(i);
						}
					}
				}
//				System.out.println(max_coverage);
//				System.out.println(equal_max_index.toString());

				int min_features_num_index = (int) equal_max_index.toArray()[0];
				int min_features_num = (int) configs_properties_array[min_features_num_index][1];

				for (Integer i : equal_max_index) {
					if (configs_properties_array[i][1] < min_features_num) {
						min_features_num = (int) configs_properties_array[i][1];
						min_features_num_index = i;
					}
				}
				first_num = min_features_num_index + 1;
//				System.out.println(first_num);
			}
		}

		return first_num;
	}

	private static ClosingStrategy<Object, Object> getClosingStrategy(String optionValue) {
		if (optionValue != null) {
			if (optionValue.equals(ClosingStrategies.CLOSE_FIRST.toString())) {
				return ClosingStrategies.CLOSE_FIRST;
			} else if (optionValue.equals(ClosingStrategies.CLOSE_SHORTEST.toString())) {
				return ClosingStrategies.CLOSE_SHORTEST;
			} else if (optionValue.equals(ClosingStrategies.CLOSE_RANDOM.toString())) {
				return ClosingStrategies.CLOSE_RANDOM;
			}
		}
		return ClosingStrategies.CLOSE_FIRST;
	}

	private static ObservationTableCEXHandler<Object, Object> getCEXHandler(String optionValue) {
		if (optionValue != null) {
			if (optionValue.equals(ObservationTableCEXHandlers.RIVEST_SCHAPIRE.toString())) {
				return ObservationTableCEXHandlers.RIVEST_SCHAPIRE;

			} else if (optionValue.equals(RIVEST_SCHAPIRE_ALLSUFFIXES)) {
				return ObservationTableCEXHandlers.RIVEST_SCHAPIRE_ALLSUFFIXES;
			} else if (optionValue.equals(ObservationTableCEXHandlers.SUFFIX1BY1.toString())) {
				return ObservationTableCEXHandlers.SUFFIX1BY1;
			} else if (optionValue.equals(ObservationTableCEXHandlers.CLASSIC_LSTAR.toString())) {
				return ObservationTableCEXHandlers.CLASSIC_LSTAR;
			} else if (optionValue.equals(ObservationTableCEXHandlers.MALER_PNUELI.toString())) {
				return ObservationTableCEXHandlers.MALER_PNUELI;
			} else if (optionValue.equals(ObservationTableCEXHandlers.SHAHBAZ.toString())) {
				return ObservationTableCEXHandlers.SHAHBAZ;
			}
		}
		return ObservationTableCEXHandlers.RIVEST_SCHAPIRE;
	}

	private static EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> buildEqOracle(
			Random rnd_seed, CommandLine line, LearnLogger logger, CompactMealy<String, Word<String>> mealyss,
			SUL<String, Word<String>> eq_sul) {
		MembershipOracle<String, Word<Word<String>>> oracleForEQoracle = new SULOracle<>(eq_sul);

		EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle;
		if (!line.hasOption(EQ)) {
//			logger.logEvent("EquivalenceOracle: WpMethodEQOracle(" + 2 + ")");
			return new WpMethodEQOracle<>(oracleForEQoracle, 2);
		}

		double restartProbability;
		int maxSteps, maxTests, maxLength, minLength, maxDepth, minimalSize, rndLength, bound;
		long rnd_long;
		boolean resetStepCount;

		LearnLibProperties learn_props = LearnLibProperties.getInstance();

		switch (line.getOptionValue(EQ)) {
		case "rndWalk":
			// create RandomWalkEQOracle
			restartProbability = learn_props.getRndWalk_restartProbability();
			maxSteps = learn_props.getRndWalk_maxSteps();
			resetStepCount = learn_props.getRndWalk_resetStepsCount();

			eqOracle = new RandomWalkEQOracle<String, Word<String>>(eq_sul, // sul
					restartProbability, // reset SUL w/ this probability before a step
					maxSteps, // max steps (overall)
					resetStepCount, // reset step count after counterexample
					rnd_seed // make results reproducible
			);
//			logger.logEvent("EquivalenceOracle: RandomWalkEQOracle(" + restartProbability + "," + maxSteps + ","
//					+ resetStepCount + ")");
			break;
		case "rndWords":
			// create RandomWordsEQOracle
			maxTests = learn_props.getRndWords_maxTests();
			maxLength = learn_props.getRndWords_maxLength();
			minLength = learn_props.getRndWords_minLength();
			rnd_long = rnd_seed.nextLong();
			rnd_seed.setSeed(rnd_long);

			eqOracle = new RandomWordsEQOracle<>(oracleForEQoracle, minLength, maxLength, maxTests, rnd_seed);
//			logger.logEvent("EquivalenceOracle: RandomWordsEQOracle(" + minLength + ", " + maxLength + ", " + maxTests
//					+ ", " + rnd_long + ")");
			break;
		case "wp":
			maxDepth = learn_props.getW_maxDepth();
			eqOracle = new WpMethodEQOracle<>(oracleForEQoracle, maxDepth);
//			logger.logEvent("EquivalenceOracle: WpMethodEQOracle(" + maxDepth + ")");
			break;
		case "wphyp":
			maxDepth = learn_props.getW_maxDepth();
			eqOracle = new WpMethodHypEQOracle((MealyMembershipOracle) oracleForEQoracle, maxDepth, mealyss);
//			logger.logEvent("EquivalenceOracle: WpMethodHypEQOracle(" + maxDepth + ")");
			break;
		case "w":
			maxDepth = learn_props.getW_maxDepth();
			eqOracle = new WMethodEQOracle<>(oracleForEQoracle, maxDepth);
//			logger.logEvent("EquivalenceOracle: WMethodQsizeEQOracle(" + maxDepth + ")");
			break;
		case "whyp":
			maxDepth = learn_props.getW_maxDepth();
			eqOracle = new WMethodHypEQOracle((MealyMembershipOracle) oracleForEQoracle, maxDepth, mealyss);
//			logger.logEvent("EquivalenceOracle: WMethodHypEQOracle(" + maxDepth + ")");
			break;
		case "wrnd":
			minimalSize = learn_props.getWhyp_minLen();
			rndLength = learn_props.getWhyp_rndLen();
			bound = learn_props.getWhyp_bound();
			rnd_long = rnd_seed.nextLong();
			rnd_seed.setSeed(rnd_long);

			eqOracle = new RandomWMethodEQOracle<>(oracleForEQoracle, minimalSize, rndLength, bound, rnd_seed, 1);
//			logger.logEvent("EquivalenceOracle: RandomWMethodEQOracle(" + minimalSize + "," + rndLength + "," + bound
//					+ "," + rnd_long + ")");
			break;
		case "wrndhyp":
			minimalSize = learn_props.getWhyp_minLen();
			rndLength = learn_props.getWhyp_rndLen();
			bound = learn_props.getWhyp_bound();
			rnd_long = rnd_seed.nextLong();
			rnd_seed.setSeed(rnd_long);

			eqOracle = new RandomWMethodHypEQOracle((MealyMembershipOracle) oracleForEQoracle, minimalSize, rndLength,
					bound, rnd_seed, 1, mealyss);
//			logger.logEvent("EquivalenceOracle: RandomWMethodHypEQOracle(" + minimalSize + "," + rndLength + "," + bound
//					+ "," + rnd_long + "," + 1 + ")");
			break;
		default:
			maxDepth = 2;
			eqOracle = new WMethodEQOracle<>(oracleForEQoracle, maxDepth);
//			logger.logEvent("EquivalenceOracle: WMethodEQOracle(" + maxDepth + ")");
			break;
		}
		return eqOracle;
	}

	private static ExperimentAndLearner learningLStarM(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<? super MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy) {
		List<Word<String>> initPrefixes = new ArrayList<>();
		initPrefixes.add(Word.epsilon());
		List<Word<String>> initSuffixes = new ArrayList<>();
		Word<String> word = Word.epsilon();
		for (String symbol : mealyss.getInputAlphabet()) {
			initSuffixes.add(word.append(symbol));
		}

		// construct standard L*M instance
		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(mealyss.getInputAlphabet());
		builder.setOracle(mqOracle);
		builder.setInitialPrefixes(initPrefixes);
		builder.setInitialSuffixes(initSuffixes);
		builder.setCexHandler(handler);
		builder.setClosingStrategy(strategy);

		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	private static ExperimentAndLearner learningLStarM_r(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<? super MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy) {

		List<Word<String>> initPrefixes = new ArrayList<>();
		initPrefixes.add(Word.epsilon());
		List<Word<String>> initSuffixes = new ArrayList<>();
		Word<String> word = Word.epsilon();

		Alphabet<String> alphabet = mealyss.getInputAlphabet();
//		System.out.println("Alphabet:\n" + alphabet);
		alphabet = shuffleAlphabet(alphabet);

		for (String symbol : alphabet) {
			initSuffixes.add(word.append(symbol));
		}

//		System.out.println("\ninitSuffixes1: " + initSuffixes);
		initPrefixes = shufflePrefixes(initPrefixes);
		Collections.shuffle(initSuffixes, rand);
//		System.out.println("initSuffixes2: " + initSuffixes);

		// construct standard L*M instance
		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(mealyss.getInputAlphabet());
		builder.setOracle(mqOracle);
		builder.setInitialPrefixes(initPrefixes);
		builder.setInitialSuffixes(initSuffixes);
		builder.setCexHandler(handler);
		builder.setClosingStrategy(strategy);

		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	protected static MyObservationTable loadObservationTable(CompactMealy<String, Word<String>> mealyss, File the_ot)
			throws IOException {
		// create log
		LearnLogger logger = LearnLogger.getLogger(Infer_LearnLib.class);
//		logger.logEvent("Reading OT: " + the_ot.getName());

		MyObservationTable my_ot = OTUtils.getInstance().readOT(the_ot, mealyss.getInputAlphabet());

		return my_ot;

	}

	private static int[] LearnFSM(File sul_file, ArrayList<ObservationTable> otList, String learningAlgorithm_1,
			File out_dir_file, CommandLine line_1, long seed_1, int[] statistics_array, int isAdaptiveMethod,
			LearnLogger logger, List<List<Feature>> ot_features_list_1) {
		try {

			Random random_1 = new Random(seed_1);

			// set closing strategy
			ClosingStrategy<Object, Object> strategy = getClosingStrategy(line_1.getOptionValue(CLOS));

			// set CE processing approach
			ObservationTableCEXHandler<Object, Object> handler = getCEXHandler(line_1.getOptionValue(CEXH));

			// load mealy machine
			CompactMealy<String, Word<String>> mealyss = LoadMealy(sul_file);
			logger.logEvent("\nSUL name: " + sul_file.getName());
//			logger.logEvent("SUL dir: " + sul_file.getAbsolutePath());
//			logger.logEvent("Output dir: " + out_dir_file);

			Utils.getInstance();
			// SUL simulator
			SUL<String, Word<String>> sulSim = new MealySimulatorSUL<>(mealyss, Utils.OMEGA_SYMBOL);

			//////////////////////////////////
			// Setup objects related to MQs //
			//////////////////////////////////

			// Counters for MQs
			StatisticSUL<String, Word<String>> mq_sym = new SymbolCounterSUL<>("MQ", sulSim);
			StatisticSUL<String, Word<String>> mq_rst = new ResetCounterSUL<>("MQ", mq_sym);

			// SUL for counting queries wraps sul
			SUL<String, Word<String>> mq_sul = mq_rst;

			// use caching to avoid duplicate queries
			if (line_1.hasOption(CACHE)) {
				// SULs for associating the IncrementalMealyBuilder 'mq_cbuilder' to MQs
				mq_sul = SULCache.createDAGCache(mealyss.getInputAlphabet(), mq_rst);
			}

			MembershipOracle<String, Word<Word<String>>> mqOracle = new SULOracle<String, Word<String>>(mq_sul);

//			logger.logEvent("Cache: " + (line_1.hasOption(CACHE) ? "Y" : "N"));

			//////////////////////////////////
			// Setup objects related to EQs //
			//////////////////////////////////

//			logger.logEvent("ClosingStrategy: " + strategy.toString());
//			logger.logEvent("ObservationTableCEXHandler: " + line_1.getOptionValue(CEXH));

			// Counters for EQs
			StatisticSUL<String, Word<String>> eq_sym = new SymbolCounterSUL<>("EQ", sulSim);
			StatisticSUL<String, Word<String>> eq_rst = new ResetCounterSUL<>("EQ", eq_sym);

			// SUL for counting queries wraps sul
			SUL<String, Word<String>> eq_sul = eq_rst;

			// use caching to avoid duplicate queries
			if (line_1.hasOption(CACHE)) {
				// SULs for associating the IncrementalMealyBuilder 'cbuilder' to EQs
				eq_sul = SULCache.createDAGCache(mealyss.getInputAlphabet(), eq_rst);
			}

			EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle = null;
			eqOracle = buildEqOracle(random_1, line_1, logger, mealyss, eq_sul);

			ExperimentAndLearner experiment_pair = null;

			switch (learningAlgorithm_1) {
			case "lstar":
//				logger.logConfig("Method: L*M");
				experiment_pair = learningLStarM(mealyss, mqOracle, eqOracle, handler, strategy);
				break;
			case "lstar_r":
//				logger.logConfig("Method: L*M_r");
				experiment_pair = learningLStarM_r(mealyss, mqOracle, eqOracle, handler, strategy);
				break;
			case "SPL_adaptive_learning_v1":
				if (handler == ObservationTableCEXHandlers.CLASSIC_LSTAR)
					throw new Exception("DL*M requires " + ObservationTableCEXHandlers.RIVEST_SCHAPIRE + " CexH");
//				logger.logConfig("Method: SPL_AdaptiveLearning_v1");
				experiment_pair = SPL_AdaptiveLearning_v1(mealyss, mqOracle, eqOracle, handler, strategy, otList);
				break;
			case "SPL_adaptive_learning_v2":
				if (handler == ObservationTableCEXHandlers.CLASSIC_LSTAR)
					throw new Exception("DL*M requires " + ObservationTableCEXHandlers.RIVEST_SCHAPIRE + " CexH");
//				logger.logConfig("Method: SPL_AdaptiveLearning_v2");
				experiment_pair = SPL_AdaptiveLearning_v2(mealyss, mqOracle, eqOracle, handler, strategy, otList);
				break;
			case "SPL_adaptive_learning_v2_r":
				if (handler == ObservationTableCEXHandlers.CLASSIC_LSTAR)
					throw new Exception("DL*M requires " + ObservationTableCEXHandlers.RIVEST_SCHAPIRE + " CexH");
//				logger.logConfig("Method: SPL_AdaptiveLearning_v2_r");
				experiment_pair = SPL_AdaptiveLearning_v2_r(mealyss, mqOracle, eqOracle, handler, strategy, otList);
				break;
			case "SPL_adaptive_learning_v4":
				if (handler == ObservationTableCEXHandlers.CLASSIC_LSTAR)
					throw new Exception("DL*M requires " + ObservationTableCEXHandlers.RIVEST_SCHAPIRE + " CexH");
//				logger.logConfig("Method: SPL_AdaptiveLearning_v4");
				experiment_pair = SPL_AdaptiveLearning_v4(mealyss, mqOracle, eqOracle, handler, strategy, otList,
						ot_features_list_1);
				break;
			default:
				throw new Exception("Invalid learning method selected: " + learningAlgorithm_1);
			}

			MealyExperiment experiment = experiment_pair.getExperiment();

			// turn on time profiling
			experiment.setProfile(true);

			// uncomment one of the following lines
//			experiment.setLogModels(true);
//			experiment.setLogOT(true);

			// run experiment
			experiment.run();

			// statistics array
			statistics_array[0] += experiment.getRounds().getCount();
			statistics_array[1] += ExtractValue(mq_rst.getStatisticalData().getSummary());
			statistics_array[2] += ExtractValue(mq_sym.getStatisticalData().getSummary());
			statistics_array[3] += ExtractValue(eq_rst.getStatisticalData().getSummary());
			statistics_array[4] += ExtractValue(eq_sym.getStatisticalData().getSummary());
			statistics_array[5] += ExtractValue(mq_rst.getStatisticalData().getSummary())
					+ ExtractValue(eq_rst.getStatisticalData().getSummary());
			statistics_array[6] += ExtractValue(mq_sym.getStatisticalData().getSummary())
					+ ExtractValue(eq_sym.getStatisticalData().getSummary());

			// learning statistics
//			logger.logConfig("Rounds: " + experiment.getRounds().getCount());
//			logger.logStatistic(mq_rst.getStatisticalData());
//			logger.logStatistic(mq_sym.getStatisticalData());
//			logger.logStatistic(eq_rst.getStatisticalData());
//			logger.logStatistic(eq_sym.getStatisticalData());

			// profiling
			SimpleProfiler.logResults();

			MealyMachine finalHyp = (MealyMachine) experiment.getFinalHypothesis();

//			logger.logConfig("Qsize: " + mealyss.getStates().size());
//			logger.logConfig("Isize: " + mealyss.getInputAlphabet().size());

			boolean isEquiv = mealyss.getStates().size() == finalHyp.getStates().size();
			if (isEquiv) {
				logger.logConfig("Equivalent: OK");
			} else {
				logger.logConfig("Equivalent: NOK");
			}

			if (line_1.hasOption(SOT) && experiment_pair.getLearner() != null) {
				StringBuffer sb = new StringBuffer();
				sb.append("\n");
				new ObservationTableASCIIWriter<>().write(experiment_pair.getLearner().getObservationTable(), sb);
//				logger.logEvent(sb.toString());
			}

			File ot_out_dir = null;
			if (line_1.hasOption(SOT2) && experiment_pair.getLearner() != null) {
				ot_out_dir = new File(line_1.getOptionValue(SOT2));
				OTUtils otUtils_1 = new OTUtils();

				String fileCompleteName = sul_file.getName();
				String fileName = "";
				int j = fileCompleteName.lastIndexOf('.');
				if (j >= 0) {
					fileName = fileCompleteName.substring(0, j);
				}

				if (isAdaptiveMethod == 1) {
					// save observation table
					String ot_name = "";
					if (fileName.endsWith("_text")) {
						ot_name = fileName.replace("_text", "_ot");
					} else if (fileName.endsWith("_fsm")) {
						ot_name = fileName.replace("_fsm", "_ot");
					} else {
						ot_name = fileName + "_ot";
					}
					File ot_file_2 = new File(ot_out_dir, ot_name);
					otUtils_1.writeOT(experiment_pair.getLearner().getObservationTable(), ot_file_2);
//					System.out.println("The observation table and the learned FSM are saved to the disk.");

					// save FSM as a .txt file
					String learned_fsm_name = "";
					if (fileName.endsWith("_text")) {
						learned_fsm_name = fileName.replace("_text", "_learnedFsm") + ".txt";
					} else if (fileName.endsWith("_fsm")) {
						learned_fsm_name = fileName.replace("_fsm", "_learnedFsm") + ".txt";
					} else {
						learned_fsm_name = fileName + "_learnedFsm.txt";
					}
					File learned_fsm_file = new File(ot_out_dir, learned_fsm_name);
					String header = "";
					FeaturedMealyUtils.getInstance().saveFSM_kiss(mealyss, learned_fsm_file, header);

					// save FSM as a dot file
					String learned_fsm_dot_name = "";
					if (fileName.endsWith("_text")) {
						learned_fsm_dot_name = ot_out_dir.toString() + "\\"
								+ fileName.replaceFirst("_text", "_learnedFsmDot.dot");
					} else if (fileName.endsWith("_fsm")) {
						learned_fsm_dot_name = ot_out_dir.toString() + "\\"
								+ fileName.replaceFirst("_fsm", "_learnedFsmDot.dot");
					} else {
						learned_fsm_dot_name = ot_out_dir.toString() + "\\" + fileName + "_learnedFsmDot.dot";
					}
					BufferedWriter bw = new BufferedWriter(new FileWriter(learned_fsm_dot_name));
					GraphDOT.write(mealyss, bw);
					bw.close();

					otList.add(experiment_pair.getLearner().getObservationTable());

				}

			}

		} catch (Exception exp) {
			System.err.println("Unexpected Exception");
			exp.printStackTrace();
		}
		return statistics_array;

	}

	// Prefixes and suffixes (which are compatible with the new product alphabet)
	// from all previously learned models are added
	private static ExperimentAndLearner SPL_AdaptiveLearning_v1(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy,
			ArrayList<ObservationTable> otList) {
		// TODO Auto-generated method stub

		List<Word<String>> initPrefixes = new ArrayList<>();
		List<Word<String>> initSuffixes = new ArrayList<>();

		Alphabet<String> alphabet = mealyss.getInputAlphabet();
//		System.out.println("Alphabet:\n" + alphabet);

		initPrefixes.add(Word.epsilon());
		for (ObservationTable ot : otList) {
			List ot_prefixes = (List) ot.getShortPrefixes();
//			System.out.println("Prefixes:\n" + ot_prefixes);
			for (Object prefix : ot_prefixes) {
				String prefix_string = prefix.toString();
				List<String> prefix_symbols = Arrays.asList(prefix_string.split(" "));
//				System.out.println(prefix_symbols);
				int include = 1;
				for (String symbol : prefix_symbols) {
					if (!alphabet.toString().contains(symbol)) {
						include = 0;
						break;
					}
				}
				if (include == 1) {
//					System.out.println("Included:" + prefix);
					if (!initPrefixes.contains(prefix)) {
						initPrefixes.add((Word<String>) prefix);
					}
				}
			}
//			System.out.println("Prefixes result:\n" + initPrefixes + "\n");

			List ot_suffixes = ot.getSuffixes();
//			System.out.println("Suffixes:\n" + ot_suffixes);
			for (Object suffix : ot_suffixes) {
				String suffix_string = suffix.toString();
				List<String> suffix_symbols = Arrays.asList(suffix_string.split(" "));
//				System.out.println(suffix_symbols);
				int include = 1;
				for (String symbol : suffix_symbols) {
					if (!alphabet.toString().contains(symbol)) {
						include = 0;
						break;
					}
				}
				if (include == 1) {
//					System.out.println("Included:" + suffix);
					if (!initSuffixes.contains(suffix)) {
						initSuffixes.add((Word<String>) suffix);
					}
				}
			}
//			System.out.println("Suffixes result:\n" + initSuffixes + "\n");
		}

//		System.out.println("Final prefixes result:\n" + initPrefixes + "\n");
//		System.out.println("Final suffixes result:\n" + initSuffixes + "\n");

		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(mealyss.getInputAlphabet());
		builder.setOracle(mqOracle);
		builder.setInitialPrefixes(initPrefixes);
		builder.setInitialSuffixes(initSuffixes);
		builder.setCexHandler(handler);
		builder.setClosingStrategy(strategy);
		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	// Prefixes and suffixes (which are compatible with the new product alphabet)
	// from all previously learned models are added
	// The input alphabet of the new product is also added to the initial set of
	// prefixes
	private static ExperimentAndLearner SPL_AdaptiveLearning_v2(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy,
			ArrayList<ObservationTable> otList) {
		// TODO Auto-generated method stub

		List<Word<String>> initPrefixes = new ArrayList<>();
		List<Word<String>> initSuffixes = new ArrayList<>();

		Alphabet<String> alphabet = mealyss.getInputAlphabet();
//		System.out.println("Alphabet:\n" + alphabet);

		initPrefixes.add(Word.epsilon());
		for (ObservationTable ot : otList) {
			List ot_prefixes = (List) ot.getShortPrefixes();
//			System.out.println("Prefixes:\n" + ot_prefixes);
			for (Object prefix : ot_prefixes) {
				String prefix_string = prefix.toString();
				List<String> prefix_symbols = Arrays.asList(prefix_string.split(" "));
//				System.out.println(prefix_symbols);
				int include = 1;
				for (String symbol : prefix_symbols) {
					if (!alphabet.toString().contains(symbol)) {
						include = 0;
						break;
					}
				}
				if (include == 1) {
//					System.out.println("Included:" + prefix);
					if (!initPrefixes.contains(prefix)) {
						initPrefixes.add((Word<String>) prefix);
					}
				}
			}
//			System.out.println("Prefixes result:\n" + initPrefixes + "\n");

			List ot_suffixes = ot.getSuffixes();
//			System.out.println("Suffixes:\n" + ot_suffixes);
			for (Object suffix : ot_suffixes) {
				String suffix_string = suffix.toString();
				List<String> suffix_symbols = Arrays.asList(suffix_string.split(" "));
//				System.out.println(suffix_symbols);
				int include = 1;
				for (String symbol : suffix_symbols) {
					if (!alphabet.toString().contains(symbol)) {
						include = 0;
						break;
					}
				}
				if (include == 1) {
//					System.out.println("Included:" + suffix);
					if (!initSuffixes.contains(suffix)) {
						initSuffixes.add((Word<String>) suffix);
					}
				}
			}
//			System.out.println("Suffixes result:\n" + initSuffixes + "\n");
		}

//		System.out.println("Final prefixes result:\n" + initPrefixes + "\n");
//		System.out.println("Final suffixes result:\n" + initSuffixes + "\n");

		List<String> initSuffixesString = new ArrayList<>();
		for (Word<String> w : initSuffixes) {
			String wString = w.toString();
			initSuffixesString.add(wString);
		}

		for (String a1 : alphabet) {
			if (!initSuffixesString.contains(a1)) {
				Word<String> a2 = Word.fromLetter(a1);
				initSuffixes.add(a2);
			}
		}

//		System.out.println("Final_2 suffixes result:\n" + initSuffixes + "\n");

		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(mealyss.getInputAlphabet());
		builder.setOracle(mqOracle);
		builder.setInitialPrefixes(initPrefixes);
		builder.setInitialSuffixes(initSuffixes);
		builder.setCexHandler(handler);
		builder.setClosingStrategy(strategy);
		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	private static ExperimentAndLearner SPL_AdaptiveLearning_v2_r(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy,
			ArrayList<ObservationTable> otList) {
		// TODO Auto-generated method stub

		List<Word<String>> initPrefixes = new ArrayList<>();
		List<Word<String>> initSuffixes = new ArrayList<>();

		Alphabet<String> alphabet = mealyss.getInputAlphabet();
//		System.out.println("Alphabet:\n" + alphabet);
		alphabet = shuffleAlphabet(alphabet);

		initPrefixes.add(Word.epsilon());
		for (ObservationTable ot : otList) {
			List ot_prefixes = (List) ot.getShortPrefixes();
//			System.out.println("Prefixes:\n" + ot_prefixes);
			for (Object prefix : ot_prefixes) {
				String prefix_string = prefix.toString();
				List<String> prefix_symbols = Arrays.asList(prefix_string.split(" "));
//				System.out.println(prefix_symbols);
				int include = 1;
				for (String symbol : prefix_symbols) {
					if (!alphabet.toString().contains(symbol)) {
						include = 0;
						break;
					}
				}
				if (include == 1) {
//					System.out.println("Included:" + prefix);
					if (!initPrefixes.contains(prefix)) {
						initPrefixes.add((Word<String>) prefix);
					}
				}
			}
//			System.out.println("Prefixes result:\n" + initPrefixes + "\n");

			List ot_suffixes = ot.getSuffixes();
//			System.out.println("Suffixes:\n" + ot_suffixes);
			for (Object suffix : ot_suffixes) {
				String suffix_string = suffix.toString();
				List<String> suffix_symbols = Arrays.asList(suffix_string.split(" "));
//				System.out.println(suffix_symbols);
				int include = 1;
				for (String symbol : suffix_symbols) {
					if (!alphabet.toString().contains(symbol)) {
						include = 0;
						break;
					}
				}
				if (include == 1) {
//					System.out.println("Included:" + suffix);
					if (!initSuffixes.contains(suffix)) {
						initSuffixes.add((Word<String>) suffix);
					}
				}
			}
//			System.out.println("Suffixes result:\n" + initSuffixes + "\n");
		}

//		System.out.println("Final prefixes result:\n" + initPrefixes + "\n");
//		System.out.println("Final suffixes result:\n" + initSuffixes + "\n");

		List<String> initSuffixesString = new ArrayList<>();
		for (Word<String> w : initSuffixes) {
			String wString = w.toString();
			initSuffixesString.add(wString);
		}

		for (String a1 : alphabet) {
			if (!initSuffixesString.contains(a1)) {
				Word<String> a2 = Word.fromLetter(a1);
				initSuffixes.add(a2);
			}
		}

//		System.out.println("Final_2 suffixes result:\n" + initSuffixes + "\n");

		initPrefixes = shufflePrefixes(initPrefixes);
		Collections.shuffle(initSuffixes, rand);

		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(mealyss.getInputAlphabet());
		builder.setOracle(mqOracle);
		builder.setInitialPrefixes(initPrefixes);
		builder.setInitialSuffixes(initSuffixes);
		builder.setCexHandler(handler);
		builder.setClosingStrategy(strategy);
		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	private static List<Word<String>> shufflePrefixes(List<Word<String>> prefixes) {
		// TODO Auto-generated method stub
		Word<String> prefixes_1 = prefixes.get(0);
		List<Word<String>> prefixes_2 = prefixes.subList(1, prefixes.size());
		Collections.shuffle(prefixes_2, rand);
		prefixes_2.add(0, prefixes_1);
		return prefixes_2;
	}

	private static Alphabet<String> shuffleAlphabet(Alphabet<String> a_1) {
		// TODO Auto-generated method stub
		List<String> a_list = new ArrayList<String>();
		for (String a : a_1) {
			a_list.add(a);
		}
		Collections.shuffle(a_list, rand);
		Alphabet<String> a_2 = new MapAlphabet(a_list);
		return a_2;
	}

	private static ExperimentAndLearner SPL_AdaptiveLearning_v4(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy,
			ArrayList<ObservationTable> otList, List<List<Feature>> ot_features_list_1) {
		// TODO Auto-generated method stub

		List<Word<String>> initPrefixes = new ArrayList<>();
		List<Word<String>> initSuffixes = new ArrayList<>();

		Alphabet<String> alphabet = mealyss.getInputAlphabet();
//		System.out.println("Alphabet:\n" + alphabet);

		initPrefixes.add(Word.epsilon());
		for (ObservationTable ot : otList) {
			List ot_prefixes = (List) ot.getShortPrefixes();
//			System.out.println("Prefixes:\n" + ot_prefixes);
			for (Object prefix : ot_prefixes) {
				String prefix_string = prefix.toString();
				List<String> prefix_symbols = Arrays.asList(prefix_string.split(" "));
//				System.out.println(prefix_symbols);
				int include = 1;
				for (String symbol : prefix_symbols) {
					if (!alphabet.toString().contains(symbol)) {
						include = 0;
						break;
					}
				}
				if (include == 1) {
//					System.out.println("Included:" + prefix);
					if (!initPrefixes.contains(prefix)) {
						initPrefixes.add((Word<String>) prefix);
					}
				}
			}
//			System.out.println("Prefixes result:\n" + initPrefixes + "\n");

			List ot_suffixes = ot.getSuffixes();
//			System.out.println("Suffixes:\n" + ot_suffixes);
			for (Object suffix : ot_suffixes) {
				String suffix_string = suffix.toString();
				List<String> suffix_symbols = Arrays.asList(suffix_string.split(" "));
//				System.out.println(suffix_symbols);
				int include = 1;
				for (String symbol : suffix_symbols) {
					if (!alphabet.toString().contains(symbol)) {
						include = 0;
						break;
					}
				}
				if (include == 1) {
//					System.out.println("Included:" + suffix);
					if (!initSuffixes.contains(suffix)) {
						initSuffixes.add((Word<String>) suffix);
					}
				}
			}
//			System.out.println("Suffixes result:\n" + initSuffixes + "\n");
		}

//		System.out.println("Final prefixes result:\n" + initPrefixes + "\n");
//		System.out.println("Final suffixes result:\n" + initSuffixes + "\n");

		List<String> initSuffixesString = new ArrayList<>();
		for (Word<String> w : initSuffixes) {
			String wString = w.toString();
			initSuffixesString.add(wString);
		}

		for (String a1 : alphabet) {
			if (!initSuffixesString.contains(a1)) {
				Word<String> a2 = Word.fromLetter(a1);
				initSuffixes.add(a2);
			}
		}

//		System.out.println("Final_2 suffixes result:\n" + initSuffixes + "\n");

		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(mealyss.getInputAlphabet());
		builder.setOracle(mqOracle);
		builder.setInitialPrefixes(initPrefixes);
		builder.setInitialSuffixes(initSuffixes);
		builder.setCexHandler(handler);
		builder.setClosingStrategy(strategy);
		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	private static int ExtractValue(String string_1) {
		// TODO Auto-generated method stub
		int value_1 = 0;
		int j = string_1.lastIndexOf(" ");
		String string_2 = "";
		if (j >= 0) {
			string_2 = string_1.substring(j + 1);
		}
		value_1 = Integer.parseInt(string_2);
		return value_1;
	}

	private static CompactMealy<String, Word<String>> LoadMealy(File fsm_file) {
		// TODO Auto-generated method stub
		InputModelDeserializer<String, CompactMealy<String, Word<String>>> parser_1 = DOTParsers
				.mealy(MEALY_EDGE_WORD_STR_PARSER);
		CompactMealy<String, Word<String>> mealy = null;
		String file_name = fsm_file.getName();
		if (file_name.endsWith("txt")) {
			try {
				mealy = Utils.getInstance().loadMealyMachine(fsm_file);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return mealy;
		} else if (file_name.endsWith("dot")) {
			try {
				mealy = parser_1.readModel(fsm_file).model;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return mealy;
		}

		return null;
	}

// generatePerm method is from stackoverflow.com:
	public static List<List<Integer>> generatePerm(List<Integer> original) {
		if (original.isEmpty()) {
			List<List<Integer>> result = new ArrayList<>();
			result.add(new ArrayList<>());
			return result;
		}
		Integer firstElement = original.remove(0);
		List<List<Integer>> returnValue = new ArrayList<>();
		List<List<Integer>> permutations = generatePerm(original);
		for (List<Integer> smallerPermutated : permutations) {
			for (int index = 0; index <= smallerPermutated.size(); index++) {
				List<Integer> temp = new ArrayList<>(smallerPermutated);
				temp.add(index, firstElement);
				returnValue.add(temp);
			}
		}
		return returnValue;
	}

	private static Options createOptions() {
		// create the Options
		Options options = new Options();
		options.addOption(SOT, false, "Save observation table (OT)");
		options.addOption(SOT2, true, "Save observation table (OT) to the disk");
		options.addOption(HELP, false, "Shows help");
		options.addOption(OT, true, "Load observation table (OT)");
		options.addOption(OUT, true, "The output directory");
		options.addOption(CLOS, true,
				"Set closing strategy." + "\nOptions: {" + String.join(", ", closingStrategiesAvailable) + "}");
		options.addOption(EQ, true,
				"Set equivalence query generator." + "\nOptions: {" + String.join(", ", eqMethodsAvailable) + "}");
		options.addOption(CEXH, true, "Set counter example (CE) processing method." + "\nOptions: {"
				+ String.join(", ", cexHandlersAvailable) + "}");
		options.addOption(CACHE, false, "Use caching.");
		options.addOption(LEARN, true,
				"Model learning algorithm." + "\nOptions: {" + String.join(", ", learningMethodsAvailable) + "}");
		options.addOption(SEED, true, "Seed used by the random generator");
		options.addOption(INFO, true, "Add extra information as string");
		options.addOption(DIR, true, "Directory of the SPL products");
		options.addOption(FM, true, "Feature model");
		return options;
	}

}
