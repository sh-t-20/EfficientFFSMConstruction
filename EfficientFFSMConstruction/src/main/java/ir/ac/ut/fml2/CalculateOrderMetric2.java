package ir.ac.ut.fml2;

import java.io.File;
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
import br.usp.icmc.labes.mealyInference.utils.MyObservationTable;
import br.usp.icmc.labes.mealyInference.utils.OTUtils;
import br.usp.icmc.labes.mealyInference.utils.Utils;
import de.learnlib.api.logging.LearnLogger;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.commons.util.Pair;
import net.automatalib.serialization.InputModelDeserializer;
import net.automatalib.serialization.dot.DOTParsers;
import net.automatalib.visualization.VisualizationHelper.EdgeAttrs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import uk.le.ac.fts.FtsUtils;

public class CalculateOrderMetric2 {

	public static final String SOT = "sot";
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
	private static final String NAME = "name";

	public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	public static final String[] eqMethodsAvailable = { "rndWalk", "rndWords", "wp", "wphyp", "w", "whyp", "wrnd",
			"wrndhyp" };
	public static final String[] closingStrategiesAvailable = { "CloseFirst", "CloseShortest", "CloseRandom" };
	private static final String RIVEST_SCHAPIRE_ALLSUFFIXES = "RivestSchapireAllSuffixes";
	public static final String[] cexHandlersAvailable = { "ClassicLStar", "MalerPnueli", "RivestSchapire",
			RIVEST_SCHAPIRE_ALLSUFFIXES, "Shahbaz", "Suffix1by1" };
	public static final String[] learningMethodsAvailable = { "lstar", "l1", "adaptive", "dlstar_v2", "dlstar_v1",
			"dlstar_v0", "ttt" };

	public static int nonmandatory_features_count = 0;
	public static String[] all_features = null;
	public static int[] is_mandatory = null;
	public static int[] height = null;
	public static List<String> mandatory_features = null;
	public static int[][] similar_features;
	public static List<Integer> mandatory_features_index = null;

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

//	Minepump SPL
//	static long seed = 1649657577795L;
	static long seed = 1706945802091L;

//	BCS SPL
//	static long seed = 1649405545460L;
//	static long seed = 1706863099383L;

	// method 1: the parameter D (added features)
	// method 2: the parameter D' (added alphabet)
	static String method = "2";

//	static long seed = 1234;
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

			if (line.hasOption(OT)) {
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

			String sul_name = line.getOptionValue(NAME);
			switch (sul_name) {
			case "Minepump":
				System.out.println("aaaa");
				all_features = new String[] { "MinePumpSys", "Command", "MethaneDetect", "WaterRegulation", "Start",
						"Stop", "Low", "Normal", "High" };
				mandatory_features = Arrays.asList("MinePumpSys", "WaterRegulation", "High");
				nonmandatory_features_count = 6;
				break;

			case "BCS":
				System.out.println("bbbb");
				all_features = new String[] { "Door_System", "Power_Window", "Finger_Protection", "Window",
						"Manual_Power_Window", "Automatic_Power_Window", "Status_LED", "LED_Power_Window",
						"Central_Locking_System", "Human_Machine_Interface" };
				mandatory_features = Arrays.asList("Door_System", "Power_Window", "Finger_Protection", "Window",
						"Human_Machine_Interface");
				nonmandatory_features_count = 6;
				break;
			}

			System.out.println(Arrays.toString(is_mandatory));
			System.out.println(mandatory_features.toString());

			double[][] products_similarity = new double[products_num][products_num];

			int samples_count = 200;
//			int samples_count = 1;

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

			// finding common alphabet
			ArrayList<Alphabet<String>> total_alphabet = new ArrayList<>();
			for (int i = 1; i < (products_num + 1); i++) {
				String fixedLengthString = ConvertTofixedLengthString(i);
				String productFileName = fixedLengthString + "_fsm.dot";
				File productFile = new File(products_dir, productFileName);
				CompactMealy<String, Word<String>> mealy = LoadMealy(productFile);
				Alphabet<String> alphabet = mealy.getInputAlphabet();
//				System.out.println(alphabet);
				total_alphabet.add(alphabet);
			}

			ArrayList<String> common_alphabet = new ArrayList<>();
			for (int i = 0; i < products_num; i++) {
				Alphabet<String> a_1 = total_alphabet.get(i);
//				System.out.println(a_1);
				for (String input_1 : a_1) {
//					System.out.println(input_1);
					if (!common_alphabet.contains(input_1)) {
						int is_common_alphabet = 1;
						for (int j = 0; j < products_num; j++) {
							if (i != j) {
								int input_1_found = 0;
								Alphabet<String> a_2 = total_alphabet.get(j);
								for (String input_2 : a_2) {
									if (input_1.equals(input_2)) {
										input_1_found = 1;
									}
								}
								if (input_1_found == 0) {
									is_common_alphabet = 0;
								}
							}
						}
						if (is_common_alphabet == 1) {
							common_alphabet.add(input_1);
						}
					}
				}
			}
			System.out.println("common alphabet:" + common_alphabet);

			int orders_sum = 0;
			for (int a = 1; a <= products_num; a++) {
				orders_sum += a;
			}
			System.out.println(orders_sum);

			ArrayList<Integer> list = new ArrayList<>();
			for (int i = 0; i < products_num; i++) {
				list.add(i + 1);
			}

			// enumeratePermutations(list, new ArrayList<Integer>());
			List<List<Integer>> result = generate(list, samples_count);

			for (List<Integer> l : result) {

				float order_metric = 0;

				ArrayList<String[]> ordered_configs = new ArrayList<String[]>();

				int[] product_order = new int[products_num];
				int index = 0;
				for (Integer p : l) {
					product_order[index] = p;
					index += 1;
				}
				sample_num += 1;
				System.out.println("\n" + sample_num + ", Learning order:" + Arrays.toString(product_order));
				logger_1.logEvent("New learning orderr");
				logger_1.logEvent("\nLearning order: " + Arrays.toString(product_order));

				int[] selected_products = new int[products_num];

				List<Feature> selected_features_0 = new ArrayList<>();
				List<Feature> selected_features = new ArrayList<>();
				List<Feature> product_features = new ArrayList<>();

				// Set the first product to be learned
				int productIndex_1 = NextProductIndex(selected_products, product_selection_method, products_similarity,
						product_order, configs_properties);

				selected_products[productIndex_1 - 1] = 1;

				configs_properties = UpdateProperties(configs_properties, productIndex_1, sampled_configs,
						selected_features);

				selected_features_0 = new ArrayList<>(selected_features);

				selected_features = UpdateSelectedFeatures(selected_features, productIndex_1, products_dir);
//				System.out.println(productIndex_1 + ": Selected features:" + selected_features.toString());

				product_features = UpdateProductFeatures(product_features, productIndex_1, products_dir);
//				System.out.println(productIndex_1 + ": Product features:" + product_features.toString());

				String fixedLengthString_1 = ConvertTofixedLengthString(productIndex_1);
				String productFileName_1 = fixedLengthString_1 + "_fsm.dot";
				File productFile_1 = new File(products_dir, productFileName_1);
				CompactMealy<String, Word<String>> mealy_1 = LoadMealy(productFile_1);
				Alphabet<String> alphabet_1 = mealy_1.getInputAlphabet();
				System.out.println("alphabet:" + alphabet_1.toString());

				float product_order_metric = calculateMetric(product_features, selected_features_0, selected_features);
//				float weight = 0.6f;

				List<String> learned_alphabet = new ArrayList<>();
				List<Float> new_alphabet_inputs_count = new ArrayList<>();

				switch (method) {
				case "1":
					if (product_order_metric > 0) {

						order_metric += (float) 1 / product_order_metric;

					} else {

						order_metric += 0;
					}
					System.out.println(product_order_metric);
					break;

				case "2":
//					System.out.println(alphabet_1);
					List<String> alphabet_list_1 = new ArrayList<>();
					for (String a : alphabet_1) {
						alphabet_list_1.add(a);
					}
					List<String> new_alphabet_1 = Difference(alphabet_list_1, common_alphabet);
//					System.out.println("new alphabet:" + new_alphabet_1);
					learned_alphabet = alphabet_list_1;
//					System.out.println("learned alphabet:" + learned_alphabet);
					float new_alphabet_size_1 = (float) new_alphabet_1.size();
					if (new_alphabet_size_1 != 0) {
						new_alphabet_inputs_count.add(new_alphabet_size_1);
//						order_metric += (float) 1 / new_alphabet_size_1;
						order_metric += -(new_alphabet_size_1 * new_alphabet_size_1);
					}
					break;
				}

				String[] product_features_2 = new String[product_features.size()];
				int f_index = 0;
				for (Feature f : product_features) {
					product_features_2[f_index] = f.toString();
					f_index += 1;
				}
				ordered_configs.add(product_features_2);

//					System.out.println(ot_list.size());

				for (int i = 0; i < (selected_products.length - 1); i++) {
					int productIndex_2 = NextProductIndex(selected_products, product_selection_method,
							products_similarity, product_order, configs_properties);
					selected_products[productIndex_2 - 1] = 1;
//					System.out.println(Arrays.toString(selected_products));

					selected_features_0 = new ArrayList<>(selected_features);

					configs_properties = UpdateProperties(configs_properties, productIndex_2, sampled_configs,
							selected_features);
//						System.out.println(Arrays.deepToString(configs_properties));
					selected_features = UpdateSelectedFeatures(selected_features, productIndex_2, products_dir);
//					System.out.println(productIndex_2 + ": Selected features:" + selected_features.toString());

					product_features = new ArrayList<>();
					product_features = UpdateProductFeatures(product_features, productIndex_2, products_dir);
//					System.out.println(productIndex_2 + ": Product features:" + product_features.toString());

					String fixedLengthString_2 = ConvertTofixedLengthString(productIndex_2);
					String productFileName_2 = fixedLengthString_2 + "_fsm.dot";
					File productFile_2 = new File(products_dir, productFileName_2);
					CompactMealy<String, Word<String>> mealy_2 = LoadMealy(productFile_2);
					Alphabet<String> alphabet_2 = mealy_2.getInputAlphabet();
					System.out.println("alphabet:" + alphabet_2.toString());

					switch (method) {
					case "1":
						product_order_metric = calculateMetric(product_features, selected_features_0,
								selected_features);
						if (product_order_metric > 0) {

							order_metric += (float) 1 / product_order_metric;

						} else {

							order_metric += 0;
						}
						System.out.println(product_order_metric);
						break;

					case "2":
//						System.out.println("\n" + alphabet_2);
						List<String> alphabet_list_2 = new ArrayList<>();
						for (String a : alphabet_2) {
							alphabet_list_2.add(a);
						}
						List<String> new_alphabet_2 = Difference(alphabet_list_2, learned_alphabet);
//						System.out.println("new alphabet:" + new_alphabet_2);
						learned_alphabet = AggregateLists(learned_alphabet, alphabet_list_2);
//						System.out.println("learned alphabet:" + learned_alphabet);
						float new_alphabet_size_2 = (float) new_alphabet_2.size();
						if (new_alphabet_size_2 != 0) {
							new_alphabet_inputs_count.add(new_alphabet_size_2);
//							order_metric += (float) 1 / new_alphabet_size_2;
							order_metric += -(new_alphabet_size_2 * new_alphabet_size_2);
						}
						break;
					}

					product_features_2 = new String[product_features.size()];
					f_index = 0;
					for (Feature f : product_features) {
						product_features_2[f_index] = f.toString();
						f_index += 1;
					}
					ordered_configs.add(product_features_2);

				}
				
				System.out.println("new input alphabet count:" + new_alphabet_inputs_count);
				
//				order_metric = Variance(new_features_count);
				
				System.out.println("order metric:" + order_metric);
				logger_1.logEvent("\nCalculated metric: " + order_metric + "endline;");
			}
		} catch (Exception exp) {
			// automatically generate the help statement
			formatter.printHelp("Optioms:", options);
			System.err.println("Unexpected Exception");
			exp.printStackTrace();
		}

		System.out.println("Finished");

	}

	private static float Variance(List<Float> list_1) {
		// TODO Auto-generated method stub
		float variance_1 = 0;
		float avg = Average(list_1);
		System.out.println("average:" + avg);
		if (list_1.size() != 0) {
			for (Float num : list_1) {
				variance_1 += Math.pow((num-avg), 2);
			}	
			variance_1 /= list_1.size();
			System.out.println("variance:" + variance_1);
		}
		return variance_1;
	}

	private static float Average(List<Float> list_1) {
		// TODO Auto-generated method stub
		float average_1 = 0;
		if (list_1.size() != 0) {
			float sum = 0;
			for (Float num : list_1) {
				sum += num;
			}
			average_1 = (float) sum / list_1.size();
		}
		return average_1;
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

	private static float calculateMetric(List<Feature> p_features, List<Feature> s_features_0,
			List<Feature> s_features) {
		// TODO Auto-generated method stub

		List<Feature> new_features = new ArrayList<>();
		for (Feature f : p_features) {
			if (!s_features_0.contains(f)) {
				new_features.add(f);
			}
		}
//		System.out.println("New features:" + new_features.toString());
		float metric = 0;
		metric += new_features.size();

//		int all_features_length = all_features.length;
//		List<Integer> new_feature_index = new ArrayList<>();
//		for (Feature f : new_features) {
//			for (int index = 0; index < all_features_length; index++) {
//				if (f.toString().equals(all_features[index])) {
////					System.out.println(f.toString() + ", " + index);
//					new_feature_index.add(index);
//				}
//			}
//		}
//		
//		int[] new_feature_index_array = new_feature_index.stream().mapToInt(Integer::intValue).toArray();
////		System.out.println(Arrays.toString(new_feature_index_array));
//		for (int i_1: new_feature_index_array) {
//			for (int j = 0; j < all_features_length; j++) {
//				if (similar_features[i_1][j] != 0) {
//					if (mandatory_features_index.contains(j)) {
////						System.out.println("Common features (mandatory):" +i_1 + ", " + j);
//						metric += 1 * similar_features[i_1][j];
//					}
//					if (new_feature_index.contains(j)) {
////						System.out.println("Common features (non-mandatory):" +i_1 + ", " + j);
//						metric += 0.5 * similar_features[i_1][j];
//					}
//				}
//			}
//		}

		return metric;
	}

	private static List<Feature> UpdateProductFeatures(List<Feature> product_features_list, int productIndex,
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
				if (!mandatory_features.contains(f.toString())) {
					product_features_list.add(f);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return product_features_list;
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
					if (!mandatory_features.contains(f.toString())) {
						selected_features_list.add(f);
					}
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

	private static List<String> AggregateLists(List<String> list_1, List<String> list_2) {
		List<String> result_list = list_2;
		for (String f : list_1) {
			if (!list_2.contains(f)) {
				result_list.add(f);
			}
		}
		return result_list;
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

	protected static MyObservationTable loadObservationTable(CompactMealy<String, Word<String>> mealyss, File the_ot)
			throws IOException {
		// create log
		LearnLogger logger = LearnLogger.getLogger(Infer_LearnLib.class);
//		logger.logEvent("Reading OT: " + the_ot.getName());

		MyObservationTable my_ot = OTUtils.getInstance().readOT(the_ot, mealyss.getInputAlphabet());

		return my_ot;

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
		options.addOption(HELP, false, "Shows help");
		options.addOption(OT, true, "Load observation table (OT)");
		options.addOption(OUT, true, "Set output directory");
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
		options.addOption(NAME, true, "SUL name");
		return options;
	}

}
