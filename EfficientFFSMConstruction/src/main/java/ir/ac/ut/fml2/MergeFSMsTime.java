package ir.ac.ut.fml2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import uk.le.ac.ffsm.FeaturedMealy;
import uk.le.ac.ffsm.FeaturedMealyUtils;
import uk.le.ac.ffsm.FfsmDiffUtils;
import uk.le.ac.ffsm.IConfigurableFSM;
import uk.le.ac.ffsm.ProductMealy;
import uk.le.ac.ffsm.SimplifiedTransition;

public class MergeFSMsTime {

	private static final String HELP = "h";
	private static final String FM = "fm";
	public static final String DIR0 = "dir0";
	public static final String DIR1 = "dir1";
	public static final String DIR2 = "dir2";

	// 1: global similarity metric
	// 2, 3: our methods
	// 4: random method
	// 5: Walkinshaw local similarity metric, comments have been deleted
	// 6: our method, comments have been deleted
	// 7: Walkinshaw local similarity metric, merged loop version
	// 8: our method, comments have been deleted, different initialization of arrays
	public static int algorithm_version = 5;

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// sample (Minepump SPL)
//		int[] product_order = { 1, 14, 9, 10, 12, 13, 15, 2, 4, 5, 7, 6, 8, 11, 3 };

		// all products (Minepump SPL)
//		int[] product_order = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
//				25, 26, 27, 28, 29, 30, 31, 32 };

		// sample (BCS SPL)
		int[] product_order = { 9, 1, 3, 5, 12, 15, 8, 16, 7, 13, 14, 2, 11, 4, 6, 10 };

		// all products (BCS SPL)
//		int[] product_order = { 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 13, 14, 15, 16, 18, 19, 20, 21, 9, 17, 23, 24, 12, 22 };

//		int[] product_order = { 1, 3 };

		// Minepump SPL
		double K_value = 0.50;
		double T_value = 0.50;
		double R_value = 1.40;

		int transitions_count_products = 0;
		int transitions_count_learned = 0;

		List<Long> measured_time = new ArrayList<>();

		try {
			// create the command line parser
			CommandLineParser parser = new BasicParser();

			// create the Options
			Options options = createOptions();

			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();

			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			if (line.hasOption(HELP) || !line.hasOption(FM) || !line.hasOption(DIR1) || !line.hasOption(DIR2)) {
				formatter.printHelp("CreateFSMs", options);
				System.exit(0);
			}

			File feature_model_file = new File(line.getOptionValue(FM));
			IFeatureModel feature_model = FeatureModelManager.load(feature_model_file.toPath()).getObject();

			File ffsm_dir_input = new File(line.getOptionValue(DIR0));
			File fsm_dir = new File(line.getOptionValue(DIR1));
			File ffsm_dir = new File(line.getOptionValue(DIR2));

			System.out.println("Algorithm version:" + algorithm_version);
			System.out.println("T:" + T_value + ", R:" + R_value);

			String type = "MREF";

			int index = 0;
			System.out.println("index: " + index + ", product " + product_order[index] + " :");
			int productIndex_1 = product_order[index];
			File fsm_1_file = LoadFsmFile(productIndex_1, fsm_dir);
			IConfigurableFSM<String, Word<String>> fsm_1 = FeaturedMealyUtils.getInstance()
					.loadProductMachine(fsm_1_file, feature_model);
			System.out.println("input fsm_1 size: " + fsm_1.getStateIDs().size());
			int transitions_count_1 = allTransitionsCount(fsm_1);
			System.out.println("transitions count 1: " + transitions_count_1);
			transitions_count_products += transitions_count_1;

			index += 1;
			System.out.println("index: " + index + ", product " + product_order[index] + " :");
			int productIndex_2 = product_order[index];
			File fsm_2_file = LoadFsmFile(productIndex_2, fsm_dir);
			IConfigurableFSM<String, Word<String>> fsm_2 = FeaturedMealyUtils.getInstance()
					.loadProductMachine(fsm_2_file, feature_model);
			System.out.println("input fsm_2 size: " + fsm_2.getStateIDs().size());
			int transitions_count_2 = allTransitionsCount(fsm_2);
			System.out.println("transitions count 2: " + transitions_count_2);
			transitions_count_products += transitions_count_2;

			FeaturedMealy<String, Word<String>> ffsm_1 = null;

			long start_time_1 = System.currentTimeMillis();
//			System.out.println(2 + ", start t:" + start_time_1);
			ffsm_1 = ConstructFFSM(fsm_1, fsm_2, feature_model, K_value, T_value, R_value, type);
			long end_time_1 = System.currentTimeMillis();
//			System.out.println(2 + ", end t:" + end_time_1);
			long duration_1 = end_time_1 - start_time_1;
			System.out.println(2 + ", time:" + duration_1);
			measured_time.add(duration_1);

			System.out.println("FFSM size:" + ffsm_1.size());
			int transitions_count_ffsm = allTransitionsCount(ffsm_1);
			System.out.println("transitions count (ffsm): " + transitions_count_ffsm);

			FeaturedMealyUtils.getInstance().saveFFSM_kiss(ffsm_1, new File(ffsm_dir, "ffsm_2.txt"));
			FeaturedMealyUtils.getInstance().saveFFSM(ffsm_1, new File(ffsm_dir, "ffsm_2.dot"));

			transitions_count_learned = transitions_count_ffsm;
			System.out.println("transitions count (products):" + transitions_count_products);
			System.out.println("transitions count (learned ffsm):" + transitions_count_learned);
			float ratio = (float) transitions_count_learned
					/ ((float) transitions_count_1 + (float) transitions_count_2);
			System.out.println("ratio:" + ratio);

			for (int i = 2; i < product_order.length; i++) {
				System.out.println("\nNew round started:");
				String ffsm_file_name = "ffsm_" + i + ".txt";
				File ffsm_file = new File(ffsm_dir_input, ffsm_file_name);
				IConfigurableFSM<String, Word<String>> ffsm_n = FeaturedMealyUtils.getInstance()
						.loadFeaturedMealy(ffsm_file, feature_model);
				System.out.println("input ffsm size: " + ffsm_n.getStateIDs().size());
				transitions_count_1 = allTransitionsCount(ffsm_n);
				System.out.println("transitions count 1: " + transitions_count_1);

				System.out.println("index: " + i + ", product " + product_order[i] + " :");
				int productIndex_n = product_order[i];
				File fsm_n_file = LoadFsmFile(productIndex_n, fsm_dir);
				IConfigurableFSM<String, Word<String>> fsm_n = FeaturedMealyUtils.getInstance()
						.loadProductMachine(fsm_n_file, feature_model);
				System.out.println("input fsm size: " + fsm_n.getStateIDs().size());
				transitions_count_2 = allTransitionsCount(fsm_n);
				System.out.println("transitions count 2: " + transitions_count_2);
				transitions_count_products += transitions_count_2;

				type = "FREF";
				FeaturedMealy<String, Word<String>> ffsm_2 = null;

				long start_time_2 = System.currentTimeMillis();
//				System.out.println((i + 1) + ", start t:" + start_time_2);
				ffsm_2 = ConstructFFSM(ffsm_n, fsm_n, feature_model, K_value, T_value, R_value, type);
				long end_time_2 = System.currentTimeMillis();
//				System.out.println((i + 1) + ", end t:" + end_time_2);
				long duration_2 = end_time_2 - start_time_2;
				System.out.println((i + 1) + ", time:" + duration_2);
				measured_time.add(duration_2);

				System.out.println("FFSM size:" + ffsm_2.size());
				transitions_count_ffsm = allTransitionsCount(ffsm_2);
				System.out.println("transitions count (ffsm): " + transitions_count_ffsm);

				String file_name = "ffsm_" + (i + 1);
				FeaturedMealyUtils.getInstance().saveFFSM_kiss(ffsm_2, new File(ffsm_dir, file_name + ".txt"));
				FeaturedMealyUtils.getInstance().saveFFSM(ffsm_2, new File(ffsm_dir, file_name + ".dot"));

				transitions_count_learned = transitions_count_ffsm;
				System.out.println("transitions count (products):" + transitions_count_products);
				System.out.println("transitions count (learned ffsm):" + transitions_count_learned);
				ratio = (float) transitions_count_learned / ((float) transitions_count_1 + (float) transitions_count_2);
				System.out.println("ratio:" + ratio);
			}
			System.out.println("\nmeasured time:" + measured_time.toString());
		}

		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("algorithm version:" + algorithm_version);
		System.out.println("T:" + T_value + ", R:" + R_value);
		System.out.println("Finished!");

	}

	private static int allTransitionsCount(IConfigurableFSM<String, Word<String>> m) {
		// TODO Auto-generated method stub
		int transitions_count = 0;
		List<Integer> state_list = new ArrayList<>(m.getStateIDs());
		int n = state_list.size();
		List<SimplifiedTransition<String, Word<String>>> t_list = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions = m
					.getSimplifiedTransitions(state_list.get(i));
			for (Entry<String, List<SimplifiedTransition<String, Word<String>>>> t : transitions.entrySet()) {
				List<SimplifiedTransition<String, Word<String>>> list_1 = t.getValue();
				for (SimplifiedTransition<String, Word<String>> element : list_1) {
					t_list.add(element);

				}
			}
		}
		transitions_count = t_list.size();
		return transitions_count;
	}

	private static FeaturedMealy<String, Word<String>> ConstructFFSM(IConfigurableFSM<String, Word<String>> ref,
			IConfigurableFSM<String, Word<String>> updt, IFeatureModel feature_model_1, double K, double T, double R,
			String type_1) {
		// TODO Auto-generated method stub
		Set<List<Integer>> kPairs = ffsmAlgorithm(ref, updt, K, T, R);

		System.out.print("Common states found:");
		kPairs.forEach(pair -> System.out.print("\t" + pair.get(0) + "," + pair.get(1)));
		System.out.println();

		FeaturedMealy<String, Word<String>> ffsm = null;
		if (type_1.equals("MREF")) {
			System.out.println("MREF");
			ffsm = FfsmDiffUtils.getInstance().makeFFSM((ProductMealy<String, Word<String>>) ref,
					(ProductMealy<String, Word<String>>) updt, kPairs, feature_model_1);
		} else {
			System.out.println("FREF");
			ffsm = FfsmDiffUtils.getInstance().makeFFSM((FeaturedMealy<String, Word<String>>) ref,
					(ProductMealy<String, Word<String>>) updt, kPairs, feature_model_1);
		}

		return ffsm;
	}

	public static Set<List<Integer>> ffsmAlgorithm(IConfigurableFSM<String, Word<String>> ref,
			IConfigurableFSM<String, Word<String>> updt, double K, double T, double R) {
		// See https://doi.org/10.1145/2430545.2430549 (Algorithm 1)

		// Line 1 @ Algorithm 1
		RealVector pairsToScore = null;

		switch (algorithm_version) {
		case 1:
			pairsToScore = computeScores(ref, updt, K);
			break;
		case 2:
			pairsToScore = computeScores2(ref, updt);
			break;
		case 3:
			pairsToScore = computeScores3(ref, updt);
			break;
		case 4:
			pairsToScore = computeScores4(ref, updt);
			break;
		case 5:
			pairsToScore = computeScores5(ref, updt);
			break;
		case 6:
			pairsToScore = computeScores6(ref, updt);
			break;
		case 7:
			pairsToScore = computeScores7(ref, updt);
			break;
		case 8:
			pairsToScore = computeScores8(ref, updt);
			break;
		}

		System.out.print("States pair scores:\t");
		System.out.println(pairsToScore);

		// Line 2-5 @ Algorithm 1
		Set<List<Integer>> kPairs = FfsmDiffUtils.getInstance().identifyLandmaks(pairsToScore, ref, updt, T, R);
		System.out.print("Landmarks found:\t");
		kPairs.forEach(pair -> System.out.print("\t" + pair.get(0) + "," + pair.get(1)));
		System.out.println();

		// Line 6 @ Algorithm 1
		Set<List<Integer>> nPairs = FfsmDiffUtils.getInstance().surr(kPairs, ref, updt);
		Map<Integer, Set<Integer>> checked = new HashMap<>();
		checked.put(0, new LinkedHashSet<>());
		checked.put(1, new LinkedHashSet<>());
		for (List<Integer> list : kPairs) {
			checked.get(0).add(list.get(0));
			checked.get(1).add(list.get(1));
		}
		FfsmDiffUtils.getInstance().removeConflicts(nPairs, checked);

		// Line 7-14 @ Algorithm 1
		while (!nPairs.isEmpty()) {
			while (!nPairs.isEmpty()) {
				// Line 9 @ Algorithm 1
				List<Integer> A_B = pickHighest(nPairs, pairsToScore, ref, updt);
				System.out.print("Highest state pair found:");
				System.out.println("\t" + A_B.get(0) + "," + A_B.get(1));

				// Line 10 @ Algorithm 1
				kPairs.add(A_B);
				checked.get(0).add(A_B.get(0));
				checked.get(1).add(A_B.get(1));

				// Line 11 @ Algorithm 1
				FfsmDiffUtils.getInstance().removeConflicts(nPairs, checked);
			}
			// Line 13 @ Algorithm 1
			nPairs = FfsmDiffUtils.getInstance().surr(kPairs, ref, updt);
			FfsmDiffUtils.getInstance().removeConflicts(nPairs, checked);
		}

		return kPairs;
	}

	public static List<Integer> pickHighest(Set<List<Integer>> nPairs, RealVector solution,
			IConfigurableFSM<String, Word<String>> fsm1, IConfigurableFSM<String, Word<String>> fsm2) {
		List<Integer> lst_B = new ArrayList<>(fsm2.getStateIDs());

		List<Integer> max = new ArrayList<>();
		max.add(null);
		max.add(null);
		double maxSim = Double.MIN_VALUE;
		for (List<Integer> pair : nPairs) {
			Integer A = pair.get(0);
			Integer B = pair.get(1);
			int coordIdx = (A) * lst_B.size() + (B);
			// higher priority to fix point pairs, i.e., (x,x)
			if (maxSim == solution.getEntry(coordIdx) && A == B) {
				max.set(0, A);
				max.set(1, B);
			} else if (maxSim < solution.getEntry(coordIdx)) {
				max.set(0, A);
				max.set(1, B);
				maxSim = solution.getEntry(coordIdx);
			}
			// Added
			else {
				max.set(0, A);
				max.set(1, B);
				maxSim = solution.getEntry(coordIdx);
			}

		}
		return max;

	}

	public static RealVector computeScores(IConfigurableFSM<String, Word<String>> fsm1,
			IConfigurableFSM<String, Word<String>> fsm2, double K) {

		List<Integer> lst_nfa1 = new ArrayList<>(fsm1.getStateIDs());
		List<Integer> lst_nfa2 = new ArrayList<>(fsm2.getStateIDs());

		//////////////////////////////////////////////////////
		// Solving linear equation for outgoing transitions //
		//////////////////////////////////////////////////////
		RealMatrix coeffOut = new Array2DRowRealMatrix(lst_nfa1.size() * lst_nfa2.size(),
				lst_nfa1.size() * lst_nfa2.size());
		RealVector constOut = new ArrayRealVector(lst_nfa1.size() * lst_nfa2.size());
		for (int i1 = 0; i1 < lst_nfa1.size(); i1++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> trsIn1 = fsm1
					.getSimplifiedTransitions(lst_nfa1.get(i1));
			Map<String, List<SimplifiedTransition<String, Word<String>>>> sigma_i1 = new LinkedHashMap<>();
			trsIn1.values().forEach(a_lst -> a_lst.forEach(
					sympTr -> sigma_i1.putIfAbsent(sympTr.getIn() + "\t/\t" + sympTr.getOut(), new ArrayList<>())));
			trsIn1.values().forEach(a_lst -> a_lst
					.forEach(sympTr -> sigma_i1.get(sympTr.getIn() + "\t/\t" + sympTr.getOut()).add(sympTr)));

			for (int i2 = 0; i2 < lst_nfa2.size(); i2++) {
				int rowIdx = lst_nfa1.get(i1) * lst_nfa2.size() + lst_nfa2.get(i2);

				Map<String, List<SimplifiedTransition<String, Word<String>>>> sigma_i2 = new LinkedHashMap<>();
				Map<String, List<SimplifiedTransition<String, Word<String>>>> trsIn2 = fsm2
						.getSimplifiedTransitions(lst_nfa2.get(i2));
				trsIn2.values().forEach(a_lst -> a_lst.forEach(
						sympTr -> sigma_i2.putIfAbsent(sympTr.getIn() + "\t/\t" + sympTr.getOut(), new ArrayList<>())));
				trsIn2.values().forEach(a_lst -> a_lst
						.forEach(sympTr -> sigma_i2.get(sympTr.getIn() + "\t/\t" + sympTr.getOut()).add(sympTr)));

				Set<String> sigma_i1_min_i2 = new LinkedHashSet<>(sigma_i1.keySet());
				sigma_i1_min_i2.removeAll(sigma_i2.keySet());
				Set<String> sigma_i2_min_i1 = new LinkedHashSet<>(sigma_i2.keySet());
				sigma_i2_min_i1.removeAll(sigma_i1.keySet());
				Set<String> sigma_intersec = new LinkedHashSet<>(sigma_i1.keySet());
				sigma_intersec.retainAll(sigma_i2.keySet());

				int succ_i1_i2 = 0;
				for (String inputSymbol : sigma_intersec) {
					for (SimplifiedTransition<String, Word<String>> dState1 : sigma_i1.get(inputSymbol)) {
						for (SimplifiedTransition<String, Word<String>> dState2 : sigma_i2.get(inputSymbol)) {
							int colIdx = lst_nfa1.indexOf(dState1.getSj()) * lst_nfa2.size()
									+ lst_nfa2.indexOf(dState2.getSj());
							coeffOut.setEntry(rowIdx, colIdx, coeffOut.getEntry(rowIdx, colIdx) - K);
							constOut.setEntry(rowIdx, constOut.getEntry(rowIdx) + 1);
						}
					}

					succ_i1_i2 += sigma_i1.get(inputSymbol).size() * sigma_i2.get(inputSymbol).size();
				}

				double sG = 2 * (sigma_i1_min_i2.size() + sigma_i2_min_i1.size() + succ_i1_i2);
				sG = (sG == 0) ? 1 : sG;
				int colIdx = lst_nfa1.get(i1) * lst_nfa2.size() + lst_nfa2.get(i2);
				coeffOut.setEntry(rowIdx, colIdx, coeffOut.getEntry(rowIdx, colIdx) + sG);

			}
		}

		DecompositionSolver solverOut = new LUDecomposition(coeffOut).getSolver();
		RealVector solutionOut = solverOut.solve(constOut);

		//////////////////////////////////////////////////////
		// Solving linear equation for incoming transitions //
		//////////////////////////////////////////////////////
		RealMatrix coeffIn = new Array2DRowRealMatrix(lst_nfa1.size() * lst_nfa2.size(),
				lst_nfa1.size() * lst_nfa2.size());
		RealVector constIn = new ArrayRealVector(lst_nfa1.size() * lst_nfa2.size());
		for (int i1 = 0; i1 < lst_nfa1.size(); i1++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> trsIn1 = fsm1
					.getSimplifiedTransitionsIn(lst_nfa1.get(i1));
			Map<String, List<SimplifiedTransition<String, Word<String>>>> sigma_i1 = new LinkedHashMap<>();
			trsIn1.values().forEach(a_lst -> a_lst.forEach(
					sympTr -> sigma_i1.putIfAbsent(sympTr.getIn() + "\t/\t" + sympTr.getOut(), new ArrayList<>())));
			trsIn1.values().forEach(a_lst -> a_lst
					.forEach(sympTr -> sigma_i1.get(sympTr.getIn() + "\t/\t" + sympTr.getOut()).add(sympTr)));
			for (int i2 = 0; i2 < lst_nfa2.size(); i2++) {
				int rowIdx = lst_nfa1.get(i1) * lst_nfa2.size() + lst_nfa2.get(i2);

				Map<String, List<SimplifiedTransition<String, Word<String>>>> sigma_i2 = new LinkedHashMap<>();
				Map<String, List<SimplifiedTransition<String, Word<String>>>> trsIn2 = fsm2
						.getSimplifiedTransitionsIn(lst_nfa2.get(i2));
				trsIn2.values().forEach(a_lst -> a_lst.forEach(
						sympTr -> sigma_i2.putIfAbsent(sympTr.getIn() + "\t/\t" + sympTr.getOut(), new ArrayList<>())));
				trsIn2.values().forEach(a_lst -> a_lst
						.forEach(sympTr -> sigma_i2.get(sympTr.getIn() + "\t/\t" + sympTr.getOut()).add(sympTr)));

				Set<String> sigma_i1_min_i2 = new LinkedHashSet<>(sigma_i1.keySet());
				sigma_i1_min_i2.removeAll(sigma_i2.keySet());
				Set<String> sigma_i2_min_i1 = new LinkedHashSet<>(sigma_i2.keySet());
				sigma_i2_min_i1.removeAll(sigma_i1.keySet());
				Set<String> sigma_intersec = new LinkedHashSet<>(sigma_i1.keySet());
				sigma_intersec.retainAll(sigma_i2.keySet());

				int succ_i1_i2 = 0;
				for (String inputSymbol : sigma_intersec) {
					for (SimplifiedTransition<String, Word<String>> dState1 : sigma_i1.get(inputSymbol)) {
						for (SimplifiedTransition<String, Word<String>> dState2 : sigma_i2.get(inputSymbol)) {
							int colIdx = lst_nfa1.indexOf(dState1.getSj()) * lst_nfa2.size()
									+ lst_nfa2.indexOf(dState2.getSj());
							coeffIn.setEntry(rowIdx, colIdx, coeffIn.getEntry(rowIdx, colIdx) - K);
							constIn.setEntry(rowIdx, constIn.getEntry(rowIdx) + 1);
						}
					}

					succ_i1_i2 += sigma_i1.get(inputSymbol).size() * sigma_i2.get(inputSymbol).size();
				}

				double sG = 2 * (sigma_i1_min_i2.size() + sigma_i2_min_i1.size() + succ_i1_i2);
				sG = (sG == 0) ? 1 : sG;
				int colIdx = lst_nfa1.get(i1) * lst_nfa2.size() + lst_nfa2.get(i2);
				coeffIn.setEntry(rowIdx, colIdx, coeffIn.getEntry(rowIdx, colIdx) + sG);

			}
		}

		DecompositionSolver solverIn = new LUDecomposition(coeffIn).getSolver();
		RealVector solutionIn = solverIn.solve(constIn);

		RealVector solutionJoined = solutionOut.copy().add(solutionIn);
		solutionJoined.mapDivideToSelf(2);
		return solutionJoined;
	}

	public static RealVector computeScores2(IConfigurableFSM<String, Word<String>> ref_1,
			IConfigurableFSM<String, Word<String>> updt_1) {

		List<Integer> lst_nfa1 = new ArrayList<>(ref_1.getStateIDs());
		List<Integer> lst_nfa2 = new ArrayList<>(updt_1.getStateIDs());
		int n1 = lst_nfa1.size();
		int n2 = lst_nfa2.size();
		System.out.println(n1);
		System.out.println(n2);

		Alphabet<String> alphabet_1 = ref_1.getInputAlphabet();
		Alphabet<String> alphabet_2 = updt_1.getInputAlphabet();
		System.out.println("alphabet 1: " + alphabet_1.toString());
		System.out.println("alphabet 2: " + alphabet_2.toString());

		int[] t_count_outward_1 = new int[n1];
		int[] t_count_outward_2 = new int[n2];
		int[] t_count_inward_1 = new int[n1];
		int[] t_count_inward_2 = new int[n2];

		for (int i = 0; i < n1; i++) {
			t_count_outward_1[i] = -1;
			t_count_inward_1[i] = -1;
		}

		for (int j = 0; j < n2; j++) {
			t_count_outward_2[j] = -1;
			t_count_inward_2[j] = -1;
		}

		// Outward transitions
		float[][] matching_transitions_outward = new float[n1][n2];
		for (int i = 0; i < n1; i++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_outward_1 = ref_1
					.getSimplifiedTransitions(lst_nfa1.get(i));
			for (int j = 0; j < n2; j++) {
				Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_outward_2 = updt_1
						.getSimplifiedTransitions(lst_nfa2.get(j));
				for (String input_1 : alphabet_1) {
					if (transitions_outward_1.containsKey(input_1) && transitions_outward_2.containsKey(input_1)) {
						List<SimplifiedTransition<String, Word<String>>> t_outward_1 = transitions_outward_1
								.get(input_1);
						List<SimplifiedTransition<String, Word<String>>> t_outward_2 = transitions_outward_2
								.get(input_1);
//						System.out.println(input_1.toString());
//						System.out.println("fsm_1" + t_outward_1);
//						System.out.println("fsm_2" + t_outward_2);
						for (SimplifiedTransition<String, Word<String>> t_1 : t_outward_1) {
							for (SimplifiedTransition<String, Word<String>> t_2 : t_outward_2) {
								Word<String> output_outward_1 = t_1.getOut();
								String output_outward_1_string = output_outward_1.toString();
								Word<String> output_outward_2 = t_2.getOut();
								String output_outward_2_string = output_outward_2.toString();
								if (!output_outward_2_string.equals("1")
										&& output_outward_1_string.equals(output_outward_2_string)) {
									matching_transitions_outward[i][j] += 1;
								}
							}
						}
					}
				}
				if (t_count_outward_2[j] == -1) {
					t_count_outward_2[j] = effectiveTransitionsCount(transitions_outward_2);
				}
			}
			if (t_count_outward_1[i] == -1) {
				t_count_outward_1[i] = effectiveTransitionsCount(transitions_outward_1);
			}
		}

//		System.out.println(Arrays.deepToString(matching_transitions_outward));

		// Inward transitions
		float[][] matching_transitions_inward = new float[n1][n2];
		for (int i = 0; i < n1; i++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_inward_1 = ref_1
					.getSimplifiedTransitionsIn(lst_nfa1.get(i));
			for (int j = 0; j < n2; j++) {
				Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_inward_2 = updt_1
						.getSimplifiedTransitionsIn(lst_nfa2.get(j));
				for (String input_1 : alphabet_1) {
					if (transitions_inward_1.containsKey(input_1) && transitions_inward_2.containsKey(input_1)) {
						List<SimplifiedTransition<String, Word<String>>> t_inward_1 = transitions_inward_1.get(input_1);
						List<SimplifiedTransition<String, Word<String>>> t_inward_2 = transitions_inward_2.get(input_1);
//						System.out.println(input_1.toString());
//						System.out.println("fsm_1" + t_inward_1);
//						System.out.println("fsm_2" + t_inward_2);
						for (SimplifiedTransition<String, Word<String>> t_1 : t_inward_1) {
							for (SimplifiedTransition<String, Word<String>> t_2 : t_inward_2) {
								Word<String> output_inward_1 = t_1.getOut();
								String output_inward_1_string = output_inward_1.toString();
								Word<String> output_inward_2 = t_2.getOut();
								String output_inward_2_string = output_inward_2.toString();
								if (!output_inward_2_string.equals("1")
										&& output_inward_1_string.equals(output_inward_2_string)) {
									matching_transitions_inward[i][j] += 1;
								}
							}
						}
					}
				}
				if (t_count_inward_2[j] == -1) {
					t_count_inward_2[j] = effectiveTransitionsCount(transitions_inward_2);
				}
			}
			if (t_count_inward_1[i] == -1) {
				t_count_inward_1[i] = effectiveTransitionsCount(transitions_inward_1);
			}
		}
//		System.out.println(Arrays.deepToString(matching_transitions_inward));

		float[][] matching_transitions = new float[n1][n2];
		RealVector result = new ArrayRealVector();
		for (int i = 0; i < n1; i++) {
			for (int j = 0; j < n2; j++) {
				// method 1:
//				matching_transitions[i][j] = (matching_transitions_outward[i][j] + matching_transitions_inward[i][j])
//						/ (Math.max(t_count_outward_1[i], t_count_outward_2[j])
//								+ Math.max(t_count_inward_1[i], t_count_inward_2[j]));

				// method 2:
//				matching_transitions[i][j] = (matching_transitions_outward[i][j] + matching_transitions_inward[i][j])
//						/ (alphabet_1.size() + alphabet_2.size());

				// method 3:
				matching_transitions[i][j] = (matching_transitions_outward[i][j]
						/ Math.max(t_count_outward_1[i], t_count_outward_2[j])
						+ matching_transitions_inward[i][j] / Math.max(t_count_inward_1[i], t_count_inward_2[j])) / 2;

				float value = matching_transitions[i][j];
				result = result.append(value);
			}
		}

		System.out.println(Arrays.toString(t_count_outward_1));
		System.out.println(result.toString());
		return result;
	}

	// If each one of s_i and s_j has two transitions of the form a/b, it counts
	// them as 2
	public static RealVector computeScores3(IConfigurableFSM<String, Word<String>> ref_1,
			IConfigurableFSM<String, Word<String>> updt_1) {

		List<Integer> lst_nfa1 = new ArrayList<>(ref_1.getStateIDs());
		List<Integer> lst_nfa2 = new ArrayList<>(updt_1.getStateIDs());
		int n1 = lst_nfa1.size();
		int n2 = lst_nfa2.size();
//		System.out.println(n1);
//		System.out.println(n2);

		Alphabet<String> alphabet_1 = ref_1.getInputAlphabet();
		Alphabet<String> alphabet_2 = updt_1.getInputAlphabet();
//		System.out.println("alphabet 1: " + alphabet_1.toString());
//		System.out.println("alphabet 2: " + alphabet_2.toString());

		int[] t_count_outward_1 = new int[n1];
		int[] t_count_outward_2 = new int[n2];
		int[] t_count_inward_1 = new int[n1];
		int[] t_count_inward_2 = new int[n2];

		for (int i = 0; i < n1; i++) {
			t_count_outward_1[i] = -1;
			t_count_inward_1[i] = -1;
		}

		for (int j = 0; j < n2; j++) {
			t_count_outward_2[j] = -1;
			t_count_inward_2[j] = -1;
		}

		// Outward transitions
		float[][] matching_transitions_outward = new float[n1][n2];
		for (int i = 0; i < n1; i++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_outward_1 = ref_1
					.getSimplifiedTransitions(lst_nfa1.get(i));
			for (int j = 0; j < n2; j++) {
				Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_outward_2 = updt_1
						.getSimplifiedTransitions(lst_nfa2.get(j));
				for (String input_1 : alphabet_1) {
					if (transitions_outward_1.containsKey(input_1) && transitions_outward_2.containsKey(input_1)) {
						List<SimplifiedTransition<String, Word<String>>> t_outward_1 = transitions_outward_1
								.get(input_1);
						List<SimplifiedTransition<String, Word<String>>> t_outward_2 = transitions_outward_2
								.get(input_1);
//						System.out.println(input_1.toString());
//						System.out.println("fsm_1" + t_outward_1);
//						System.out.println("fsm_2" + t_outward_2);

						int size_outward_1 = t_outward_1.size();
						int size_outward_2 = t_outward_2.size();
						int[] matched_outward_1 = new int[size_outward_1];
						int[] matched_outward_2 = new int[size_outward_2];

						for (int i_1 = 0; i_1 < size_outward_1; i_1++) {
							SimplifiedTransition<String, Word<String>> t_1 = t_outward_1.get(i_1);
							for (int i_2 = 0; i_2 < size_outward_2; i_2++) {
								SimplifiedTransition<String, Word<String>> t_2 = t_outward_2.get(i_2);

								Word<String> output_outward_1 = t_1.getOut();
								String output_outward_1_string = output_outward_1.toString();
								Word<String> output_outward_2 = t_2.getOut();
								String output_outward_2_string = output_outward_2.toString();
								if (!output_outward_2_string.equals("1")
										&& output_outward_1_string.equals(output_outward_2_string)
										&& matched_outward_1[i_1] != 1 && matched_outward_2[i_2] != 1) {
									matching_transitions_outward[i][j] += 1;
									matched_outward_1[i_1] = 1;
									matched_outward_2[i_2] = 1;
								}
							}
						}
					}
				}
				if (t_count_outward_2[j] == -1) {
					t_count_outward_2[j] = effectiveTransitionsCount(transitions_outward_2);
				}
			}
			if (t_count_outward_1[i] == -1) {
				t_count_outward_1[i] = effectiveTransitionsCount(transitions_outward_1);
			}
		}

//		System.out.println(Arrays.deepToString(matching_transitions_outward));

		// Inward transitions
		float[][] matching_transitions_inward = new float[n1][n2];
		for (int i = 0; i < n1; i++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_inward_1 = ref_1
					.getSimplifiedTransitionsIn(lst_nfa1.get(i));
			for (int j = 0; j < n2; j++) {
				Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_inward_2 = updt_1
						.getSimplifiedTransitionsIn(lst_nfa2.get(j));
				for (String input_1 : alphabet_1) {
					if (transitions_inward_1.containsKey(input_1) && transitions_inward_2.containsKey(input_1)) {
						List<SimplifiedTransition<String, Word<String>>> t_inward_1 = transitions_inward_1.get(input_1);
						List<SimplifiedTransition<String, Word<String>>> t_inward_2 = transitions_inward_2.get(input_1);
//						System.out.println(input_1.toString());
//						System.out.println("fsm_1" + t_inward_1);
//						System.out.println("fsm_2" + t_inward_2);

						int size_inward_1 = t_inward_1.size();
						int size_inward_2 = t_inward_2.size();
						int[] matched_inward_1 = new int[size_inward_1];
						int[] matched_inward_2 = new int[size_inward_2];

						for (int i_1 = 0; i_1 < size_inward_1; i_1++) {
							SimplifiedTransition<String, Word<String>> t_1 = t_inward_1.get(i_1);
							for (int i_2 = 0; i_2 < size_inward_2; i_2++) {
								SimplifiedTransition<String, Word<String>> t_2 = t_inward_2.get(i_2);

								Word<String> output_inward_1 = t_1.getOut();
								String output_inward_1_string = output_inward_1.toString();
								Word<String> output_inward_2 = t_2.getOut();
								String output_inward_2_string = output_inward_2.toString();
								if (!output_inward_2_string.equals("1")
										&& output_inward_1_string.equals(output_inward_2_string)
										&& matched_inward_1[i_1] != 1 && matched_inward_2[i_2] != 1) {
									matching_transitions_inward[i][j] += 1;
									matched_inward_1[i_1] = 1;
									matched_inward_2[i_2] = 1;
								}
							}
						}
					}
				}
				if (t_count_inward_2[j] == -1) {
					t_count_inward_2[j] = effectiveTransitionsCount(transitions_inward_2);
				}
			}
			if (t_count_inward_1[i] == -1) {
				t_count_inward_1[i] = effectiveTransitionsCount(transitions_inward_1);
			}
		}
//		System.out.println(Arrays.deepToString(matching_transitions_inward));

		int[] t_count_total_1 = new int[n1];
		int[] t_count_total_2 = new int[n2];

		int m_1 = 0;
		for (int i = 0; i < n1; i++) {
			t_count_total_1[i] = t_count_outward_1[i] + t_count_inward_1[i];
			if (t_count_total_1[i] > m_1) {
				m_1 = t_count_total_1[i];
			}
		}

		int m_2 = 0;
		for (int i = 0; i < n2; i++) {
			t_count_total_2[i] = t_count_outward_2[i] + t_count_inward_2[i];
			if (t_count_total_2[i] > m_2) {
				m_2 = t_count_total_2[i];
			}
		}
//		System.out.println("m_1:" + m_1 + ", m_2:" + m_2);

		float[][] matching_transitions = new float[n1][n2];
		RealVector result = new ArrayRealVector();
		for (int i = 0; i < n1; i++) {
			for (int j = 0; j < n2; j++) {
				// method 1:
//				matching_transitions[i][j] = (matching_transitions_outward[i][j] + matching_transitions_inward[i][j])
//						/ (Math.max(t_count_outward_1[i], t_count_outward_2[j])
//								+ Math.max(t_count_inward_1[i], t_count_inward_2[j]));

				// method 2:
//				matching_transitions[i][j] = (matching_transitions_outward[i][j] + matching_transitions_inward[i][j])
//						/ (alphabet_1.size() + alphabet_2.size());

				// method 3:
//				matching_transitions[i][j] = (matching_transitions_outward[i][j]
//						/ Math.min(t_count_outward_1[i], t_count_outward_2[j])
//						+ matching_transitions_inward[i][j] / Math.min(t_count_inward_1[i], t_count_inward_2[j])) / 2;

				// method 4:
//				matching_transitions[i][j] = matching_transitions_outward[i][j]
//						/ Math.max(t_count_outward_1[i], t_count_outward_2[j])
//						+ matching_transitions_inward[i][j] / Math.max(t_count_inward_1[i], t_count_inward_2[j]);

				// method 5:
				matching_transitions[i][j] = (matching_transitions_outward[i][j] + matching_transitions_inward[i][j])
						/ Math.max(m_1, m_2);

//				if (i == 0 && j == 0) {
//					System.out.println("out:" + matching_transitions_outward[i][j]);
//					System.out.println("out:" + t_count_outward_1[i]);
//					System.out.println("out:" + t_count_outward_2[j]);
//					System.out.println("in:" + matching_transitions_inward[i][j]);
//					System.out.println("in:" + t_count_inward_1[i]);
//					System.out.println("in:" + t_count_inward_2[j]);
//				}

				float value = matching_transitions[i][j];
				result = result.append(value);
			}
		}

//		System.out.println(Arrays.toString(t_count_outward_1));
//		System.out.println("a1:" + alphabet_1.size() + ", a2:" + alphabet_2.size());
//		System.out.println(result);
		return result;
	}

	// our method, comments have been deleted
	public static RealVector computeScores6(IConfigurableFSM<String, Word<String>> ref_1,
			IConfigurableFSM<String, Word<String>> updt_1) {
		List<Integer> lst_nfa1 = new ArrayList<>(ref_1.getStateIDs());
		List<Integer> lst_nfa2 = new ArrayList<>(updt_1.getStateIDs());
		int n1 = lst_nfa1.size();
		int n2 = lst_nfa2.size();
		Alphabet<String> alphabet_1 = ref_1.getInputAlphabet();
		int[] t_count_outward_1 = new int[n1];
		int[] t_count_outward_2 = new int[n2];
		int[] t_count_inward_1 = new int[n1];
		int[] t_count_inward_2 = new int[n2];
		for (int i = 0; i < n1; i++) {
			t_count_outward_1[i] = -1;
			t_count_inward_1[i] = -1;
		}
		for (int j = 0; j < n2; j++) {
			t_count_outward_2[j] = -1;
			t_count_inward_2[j] = -1;
		}

		// Outward transitions
		float[][] matching_transitions_outward = new float[n1][n2];
		for (int i = 0; i < n1; i++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_outward_1 = ref_1
					.getSimplifiedTransitions(lst_nfa1.get(i));
			for (int j = 0; j < n2; j++) {
				Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_outward_2 = updt_1
						.getSimplifiedTransitions(lst_nfa2.get(j));
				for (String input_1 : alphabet_1) {
					if (transitions_outward_1.containsKey(input_1) && transitions_outward_2.containsKey(input_1)) {
						List<SimplifiedTransition<String, Word<String>>> t_outward_1 = transitions_outward_1
								.get(input_1);
						List<SimplifiedTransition<String, Word<String>>> t_outward_2 = transitions_outward_2
								.get(input_1);
						int size_outward_1 = t_outward_1.size();
						int size_outward_2 = t_outward_2.size();
						int[] matched_outward_1 = new int[size_outward_1];
						int[] matched_outward_2 = new int[size_outward_2];
						for (int i_1 = 0; i_1 < size_outward_1; i_1++) {
							SimplifiedTransition<String, Word<String>> t_1 = t_outward_1.get(i_1);
							for (int i_2 = 0; i_2 < size_outward_2; i_2++) {
								SimplifiedTransition<String, Word<String>> t_2 = t_outward_2.get(i_2);
								Word<String> output_outward_1 = t_1.getOut();
								String output_outward_1_string = output_outward_1.toString();
								Word<String> output_outward_2 = t_2.getOut();
								String output_outward_2_string = output_outward_2.toString();
								if (!output_outward_2_string.equals("1")
										&& output_outward_1_string.equals(output_outward_2_string)
										&& matched_outward_1[i_1] != 1 && matched_outward_2[i_2] != 1) {
									matching_transitions_outward[i][j] += 1;
									matched_outward_1[i_1] = 1;
									matched_outward_2[i_2] = 1;
								}
							}
						}
					}
				}
				if (t_count_outward_2[j] == -1) {
					t_count_outward_2[j] = effectiveTransitionsCount(transitions_outward_2);
				}
			}
			if (t_count_outward_1[i] == -1) {
				t_count_outward_1[i] = effectiveTransitionsCount(transitions_outward_1);
			}
		}

		// Inward transitions
		float[][] matching_transitions_inward = new float[n1][n2];
		for (int i = 0; i < n1; i++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_inward_1 = ref_1
					.getSimplifiedTransitionsIn(lst_nfa1.get(i));
			for (int j = 0; j < n2; j++) {
				Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_inward_2 = updt_1
						.getSimplifiedTransitionsIn(lst_nfa2.get(j));
				for (String input_1 : alphabet_1) {
					if (transitions_inward_1.containsKey(input_1) && transitions_inward_2.containsKey(input_1)) {
						List<SimplifiedTransition<String, Word<String>>> t_inward_1 = transitions_inward_1.get(input_1);
						List<SimplifiedTransition<String, Word<String>>> t_inward_2 = transitions_inward_2.get(input_1);
						int size_inward_1 = t_inward_1.size();
						int size_inward_2 = t_inward_2.size();
						int[] matched_inward_1 = new int[size_inward_1];
						int[] matched_inward_2 = new int[size_inward_2];
						for (int i_1 = 0; i_1 < size_inward_1; i_1++) {
							SimplifiedTransition<String, Word<String>> t_1 = t_inward_1.get(i_1);
							for (int i_2 = 0; i_2 < size_inward_2; i_2++) {
								SimplifiedTransition<String, Word<String>> t_2 = t_inward_2.get(i_2);
								Word<String> output_inward_1 = t_1.getOut();
								String output_inward_1_string = output_inward_1.toString();
								Word<String> output_inward_2 = t_2.getOut();
								String output_inward_2_string = output_inward_2.toString();
								if (!output_inward_2_string.equals("1")
										&& output_inward_1_string.equals(output_inward_2_string)
										&& matched_inward_1[i_1] != 1 && matched_inward_2[i_2] != 1) {
									matching_transitions_inward[i][j] += 1;
									matched_inward_1[i_1] = 1;
									matched_inward_2[i_2] = 1;
								}
							}
						}
					}
				}
				if (t_count_inward_2[j] == -1) {
					t_count_inward_2[j] = effectiveTransitionsCount(transitions_inward_2);
				}
			}
			if (t_count_inward_1[i] == -1) {
				t_count_inward_1[i] = effectiveTransitionsCount(transitions_inward_1);
			}
		}

		int[] t_count_total_1 = new int[n1];
		int[] t_count_total_2 = new int[n2];
		int m_1 = 0;
		for (int i = 0; i < n1; i++) {
			t_count_total_1[i] = t_count_outward_1[i] + t_count_inward_1[i];
			if (t_count_total_1[i] > m_1) {
				m_1 = t_count_total_1[i];
			}
		}
		int m_2 = 0;
		for (int i = 0; i < n2; i++) {
			t_count_total_2[i] = t_count_outward_2[i] + t_count_inward_2[i];
			if (t_count_total_2[i] > m_2) {
				m_2 = t_count_total_2[i];
			}
		}
		float[][] matching_transitions = new float[n1][n2];
		RealVector result = new ArrayRealVector();
		for (int i = 0; i < n1; i++) {
			for (int j = 0; j < n2; j++) {
				matching_transitions[i][j] = (matching_transitions_outward[i][j] + matching_transitions_inward[i][j])
						/ Math.max(m_1, m_2);
				float value = matching_transitions[i][j];
				result = result.append(value);
			}
		}
		return result;
	}

	// our method, comments have been deleted, different initialization of arrays
	public static RealVector computeScores8(IConfigurableFSM<String, Word<String>> ref_1,
			IConfigurableFSM<String, Word<String>> updt_1) {

		List<Integer> lst_nfa1 = new ArrayList<>(ref_1.getStateIDs());
		List<Integer> lst_nfa2 = new ArrayList<>(updt_1.getStateIDs());
		int n1 = lst_nfa1.size();
		int n2 = lst_nfa2.size();
		Alphabet<String> alphabet_1 = ref_1.getInputAlphabet();
		int[] t_count_outward_1 = new int[n1];
		int[] t_count_outward_2 = new int[n2];
		int[] t_count_inward_1 = new int[n1];
		int[] t_count_inward_2 = new int[n2];

		// Outward transitions
		float[][] matching_transitions_outward = new float[n1][n2];
		for (int i = 0; i < n1; i++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_outward_1 = ref_1
					.getSimplifiedTransitions(lst_nfa1.get(i));
			for (int j = 0; j < n2; j++) {
				Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_outward_2 = updt_1
						.getSimplifiedTransitions(lst_nfa2.get(j));
				for (String input_1 : alphabet_1) {
					if (transitions_outward_1.containsKey(input_1) && transitions_outward_2.containsKey(input_1)) {
						List<SimplifiedTransition<String, Word<String>>> t_outward_1 = transitions_outward_1
								.get(input_1);
						List<SimplifiedTransition<String, Word<String>>> t_outward_2 = transitions_outward_2
								.get(input_1);
						int size_outward_1 = t_outward_1.size();
						int size_outward_2 = t_outward_2.size();
						int[] matched_outward_1 = new int[size_outward_1];
						int[] matched_outward_2 = new int[size_outward_2];
						for (int i_1 = 0; i_1 < size_outward_1; i_1++) {
							SimplifiedTransition<String, Word<String>> t_1 = t_outward_1.get(i_1);
							for (int i_2 = 0; i_2 < size_outward_2; i_2++) {
								SimplifiedTransition<String, Word<String>> t_2 = t_outward_2.get(i_2);
								Word<String> output_outward_1 = t_1.getOut();
								String output_outward_1_string = output_outward_1.toString();
								Word<String> output_outward_2 = t_2.getOut();
								String output_outward_2_string = output_outward_2.toString();
								if (!output_outward_2_string.equals("1")
										&& output_outward_1_string.equals(output_outward_2_string)
										&& matched_outward_1[i_1] != 1 && matched_outward_2[i_2] != 1) {
									matching_transitions_outward[i][j] += 1;
									matched_outward_1[i_1] = 1;
									matched_outward_2[i_2] = 1;
								}
							}
						}
					}
				}
				if (t_count_outward_2[j] == 0) {
					t_count_outward_2[j] = effectiveTransitionsCount(transitions_outward_2);
				}
			}
			if (t_count_outward_1[i] == 0) {
				t_count_outward_1[i] = effectiveTransitionsCount(transitions_outward_1);
			}
		}

		// Inward transitions
		float[][] matching_transitions_inward = new float[n1][n2];
		for (int i = 0; i < n1; i++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_inward_1 = ref_1
					.getSimplifiedTransitionsIn(lst_nfa1.get(i));
			for (int j = 0; j < n2; j++) {
				Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_inward_2 = updt_1
						.getSimplifiedTransitionsIn(lst_nfa2.get(j));
				for (String input_1 : alphabet_1) {
					if (transitions_inward_1.containsKey(input_1) && transitions_inward_2.containsKey(input_1)) {
						List<SimplifiedTransition<String, Word<String>>> t_inward_1 = transitions_inward_1.get(input_1);
						List<SimplifiedTransition<String, Word<String>>> t_inward_2 = transitions_inward_2.get(input_1);
						int size_inward_1 = t_inward_1.size();
						int size_inward_2 = t_inward_2.size();
						int[] matched_inward_1 = new int[size_inward_1];
						int[] matched_inward_2 = new int[size_inward_2];
						for (int i_1 = 0; i_1 < size_inward_1; i_1++) {
							SimplifiedTransition<String, Word<String>> t_1 = t_inward_1.get(i_1);
							for (int i_2 = 0; i_2 < size_inward_2; i_2++) {
								SimplifiedTransition<String, Word<String>> t_2 = t_inward_2.get(i_2);
								Word<String> output_inward_1 = t_1.getOut();
								String output_inward_1_string = output_inward_1.toString();
								Word<String> output_inward_2 = t_2.getOut();
								String output_inward_2_string = output_inward_2.toString();
								if (!output_inward_2_string.equals("1")
										&& output_inward_1_string.equals(output_inward_2_string)
										&& matched_inward_1[i_1] != 1 && matched_inward_2[i_2] != 1) {
									matching_transitions_inward[i][j] += 1;
									matched_inward_1[i_1] = 1;
									matched_inward_2[i_2] = 1;
								}
							}
						}
					}
				}
				if (t_count_inward_2[j] == 0) {
					t_count_inward_2[j] = effectiveTransitionsCount(transitions_inward_2);
				}
			}
			if (t_count_inward_1[i] == 0) {
				t_count_inward_1[i] = effectiveTransitionsCount(transitions_inward_1);
			}
		}

		int[] t_count_total_1 = new int[n1];
		int[] t_count_total_2 = new int[n2];
		int m_1 = 0;
		for (int i = 0; i < n1; i++) {
			t_count_total_1[i] = t_count_outward_1[i] + t_count_inward_1[i];
			if (t_count_total_1[i] > m_1) {
				m_1 = t_count_total_1[i];
			}
		}
		int m_2 = 0;
		for (int i = 0; i < n2; i++) {
			t_count_total_2[i] = t_count_outward_2[i] + t_count_inward_2[i];
			if (t_count_total_2[i] > m_2) {
				m_2 = t_count_total_2[i];
			}
		}
		float[][] matching_transitions = new float[n1][n2];
		RealVector result = new ArrayRealVector();
		for (int i = 0; i < n1; i++) {
			for (int j = 0; j < n2; j++) {
				matching_transitions[i][j] = (matching_transitions_outward[i][j] + matching_transitions_inward[i][j])
						/ Math.max(m_1, m_2);
				float value = matching_transitions[i][j];
				result = result.append(value);
			}
		}
		return result;

	}

	// Random method
	public static RealVector computeScores4(IConfigurableFSM<String, Word<String>> ref_1,
			IConfigurableFSM<String, Word<String>> updt_1) {

		List<Integer> lst_nfa1 = new ArrayList<>(ref_1.getStateIDs());
		List<Integer> lst_nfa2 = new ArrayList<>(updt_1.getStateIDs());
		int n1 = lst_nfa1.size();
		int n2 = lst_nfa2.size();
		System.out.println(n1);
		System.out.println(n2);

		Alphabet<String> alphabet_1 = ref_1.getInputAlphabet();
		Alphabet<String> alphabet_2 = updt_1.getInputAlphabet();
		System.out.println("alphabet 1: " + alphabet_1.toString());
		System.out.println("alphabet 2: " + alphabet_2.toString());

		RealVector result = new ArrayRealVector();
		Random r = new Random();

		for (int i = 0; i < n1; i++) {
			for (int j = 0; j < n2; j++) {
				float value = r.nextFloat();
				result = result.append(value);
			}
		}

		System.out.println(result.toString());
		return result;
	}

	// Walkinshaw local similarity metric, comments have been deleted
	public static RealVector computeScores5(IConfigurableFSM<String, Word<String>> ref_1,
			IConfigurableFSM<String, Word<String>> updt_1) {
		List<Integer> lst_nfa1 = new ArrayList<>(ref_1.getStateIDs());
		List<Integer> lst_nfa2 = new ArrayList<>(updt_1.getStateIDs());
		int n1 = lst_nfa1.size();
		int n2 = lst_nfa2.size();
		Alphabet<String> alphabet_1 = ref_1.getInputAlphabet();
		Alphabet<String> alphabet_2 = updt_1.getInputAlphabet();

		// Outward transitions
		float[][] similarity_score_outward = new float[n1][n2];
		for (int i = 0; i < n1; i++) {
			List<TransitionLabel> t_list_outward_1 = new ArrayList<>();
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_outward_1 = ref_1
					.getSimplifiedTransitions(lst_nfa1.get(i));
			for (String input_1 : alphabet_1) {
				List<SimplifiedTransition<String, Word<String>>> t_outward_1 = transitions_outward_1.get(input_1);
				if (t_outward_1 != null) {
					for (SimplifiedTransition<String, Word<String>> t_outward_1_1 : t_outward_1) {
						Word<String> output_outward_1 = t_outward_1_1.getOut();
						if (!output_outward_1.toString().equals("1")) {
							TransitionLabel l_1 = new TransitionLabel(input_1, output_outward_1);
							t_list_outward_1.add(l_1);
						}
					}
				}
			}
			for (int j = 0; j < n2; j++) {
				List<TransitionLabel> t_list_outward_2 = new ArrayList<>();
				Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_outward_2 = updt_1
						.getSimplifiedTransitions(lst_nfa2.get(j));
				for (String input_2 : alphabet_2) {
					List<SimplifiedTransition<String, Word<String>>> t_outward_2 = transitions_outward_2.get(input_2);
					if (t_outward_2 != null) {
						for (SimplifiedTransition<String, Word<String>> t_outward_2_2 : t_outward_2) {
							Word<String> output_outward_2 = t_outward_2_2.getOut();
							if (!output_outward_2.toString().equals("1")) {
								TransitionLabel l_2 = new TransitionLabel(input_2, output_outward_2);
								t_list_outward_2.add(l_2);
							}
						}
					}
				}
				similarity_score_outward[i][j] = localSimilarityScore_W(t_list_outward_1, t_list_outward_2);
			}
		}

		// Inward transitions
		float[][] similarity_score_inward = new float[n1][n2];
		for (int i = 0; i < n1; i++) {
			List<TransitionLabel> t_list_inward_1 = new ArrayList<>();
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_inward_1 = ref_1
					.getSimplifiedTransitionsIn(lst_nfa1.get(i));
			for (String input_1 : alphabet_1) {
				List<SimplifiedTransition<String, Word<String>>> t_inward_1 = transitions_inward_1.get(input_1);
				if (t_inward_1 != null) {
					for (SimplifiedTransition<String, Word<String>> t_inward_1_1 : t_inward_1) {
						Word<String> output_inward_1 = t_inward_1_1.getOut();
						if (!output_inward_1.toString().equals("1")) {
							TransitionLabel l_1 = new TransitionLabel(input_1, output_inward_1);
							t_list_inward_1.add(l_1);
						}
					}
				}
			}
			for (int j = 0; j < n2; j++) {
				List<TransitionLabel> t_list_inward_2 = new ArrayList<>();
				Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_inward_2 = updt_1
						.getSimplifiedTransitionsIn(lst_nfa2.get(j));
				for (String input_2 : alphabet_2) {
					List<SimplifiedTransition<String, Word<String>>> t_inward_2 = transitions_inward_2.get(input_2);
					if (t_inward_2 != null) {
						for (SimplifiedTransition<String, Word<String>> t_inward_2_2 : t_inward_2) {
							Word<String> output_inward_2 = t_inward_2_2.getOut();
							if (!output_inward_2.toString().equals("1")) {
								TransitionLabel l_2 = new TransitionLabel(input_2, output_inward_2);
								t_list_inward_2.add(l_2);
							}
						}
					}
				}
				similarity_score_inward[i][j] = localSimilarityScore_W(t_list_inward_1, t_list_inward_2);
			}
		}

		RealVector result = new ArrayRealVector();
		for (int i = 0; i < n1; i++) {
			for (int j = 0; j < n2; j++) {
				float value = (similarity_score_outward[i][j] + similarity_score_inward[i][j]) / 2;
				result = result.append(value);
			}
		}
		return result;
	}

	// Walkinshaw local similarity metric, merged loop version
	public static RealVector computeScores7(IConfigurableFSM<String, Word<String>> ref_1,
			IConfigurableFSM<String, Word<String>> updt_1) {

		List<Integer> lst_nfa1 = new ArrayList<>(ref_1.getStateIDs());
		List<Integer> lst_nfa2 = new ArrayList<>(updt_1.getStateIDs());
		int n1 = lst_nfa1.size();
		int n2 = lst_nfa2.size();
		Alphabet<String> alphabet_1 = ref_1.getInputAlphabet();
		Alphabet<String> alphabet_2 = updt_1.getInputAlphabet();

		// Outward and inward transitions
		float[][] similarity_score_outward = new float[n1][n2];
		float[][] similarity_score_inward = new float[n1][n2];
		RealVector result = new ArrayRealVector();
		for (int i = 0; i < n1; i++) {
			List<TransitionLabel> t_list_outward_1 = new ArrayList<>();
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_outward_1 = ref_1
					.getSimplifiedTransitions(lst_nfa1.get(i));
			List<TransitionLabel> t_list_inward_1 = new ArrayList<>();
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_inward_1 = ref_1
					.getSimplifiedTransitionsIn(lst_nfa1.get(i));
			for (String input_1 : alphabet_1) {
				List<SimplifiedTransition<String, Word<String>>> t_outward_1 = transitions_outward_1.get(input_1);
				if (t_outward_1 != null) {
					for (SimplifiedTransition<String, Word<String>> t_outward_1_1 : t_outward_1) {
						Word<String> output_outward_1 = t_outward_1_1.getOut();
						if (!output_outward_1.toString().equals("1")) {
							TransitionLabel l_1 = new TransitionLabel(input_1, output_outward_1);
							t_list_outward_1.add(l_1);
						}
					}
				}
				List<SimplifiedTransition<String, Word<String>>> t_inward_1 = transitions_inward_1.get(input_1);
				if (t_inward_1 != null) {
					for (SimplifiedTransition<String, Word<String>> t_inward_1_1 : t_inward_1) {
						Word<String> output_inward_1 = t_inward_1_1.getOut();
						if (!output_inward_1.toString().equals("1")) {
							TransitionLabel l_1 = new TransitionLabel(input_1, output_inward_1);
							t_list_inward_1.add(l_1);
						}
					}
				}
			}
			for (int j = 0; j < n2; j++) {
				List<TransitionLabel> t_list_outward_2 = new ArrayList<>();
				Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_outward_2 = updt_1
						.getSimplifiedTransitions(lst_nfa2.get(j));
				List<TransitionLabel> t_list_inward_2 = new ArrayList<>();
				Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions_inward_2 = updt_1
						.getSimplifiedTransitionsIn(lst_nfa2.get(j));
				for (String input_2 : alphabet_2) {
					List<SimplifiedTransition<String, Word<String>>> t_outward_2 = transitions_outward_2.get(input_2);
					if (t_outward_2 != null) {
						for (SimplifiedTransition<String, Word<String>> t_outward_2_2 : t_outward_2) {
							Word<String> output_outward_2 = t_outward_2_2.getOut();
							if (!output_outward_2.toString().equals("1")) {
								TransitionLabel l_2 = new TransitionLabel(input_2, output_outward_2);
								t_list_outward_2.add(l_2);
							}
						}
					}
					List<SimplifiedTransition<String, Word<String>>> t_inward_2 = transitions_inward_2.get(input_2);
					if (t_inward_2 != null) {
						for (SimplifiedTransition<String, Word<String>> t_inward_2_2 : t_inward_2) {
							Word<String> output_inward_2 = t_inward_2_2.getOut();
							if (!output_inward_2.toString().equals("1")) {
								TransitionLabel l_2 = new TransitionLabel(input_2, output_inward_2);
								t_list_inward_2.add(l_2);
							}
						}
					}
				}
				similarity_score_outward[i][j] = localSimilarityScore_W(t_list_outward_1, t_list_outward_2);
				similarity_score_inward[i][j] = localSimilarityScore_W(t_list_inward_1, t_list_inward_2);
				float value = (similarity_score_outward[i][j] + similarity_score_inward[i][j]) / 2;
				result = result.append(value);
			}
		}
		return result;
	}

	private static float localSimilarityScore_W(List<TransitionLabel> t_list_1, List<TransitionLabel> t_list_2) {
		Set<TransitionLabel> t_set_1 = new HashSet<TransitionLabel>(t_list_1);
		Set<TransitionLabel> t_set_2 = new HashSet<TransitionLabel>(t_list_2);
		Set<TransitionLabel> common_transitions = Intersection(t_set_1, t_set_2);
		Set<TransitionLabel> difference_transitions_1_2 = Difference(t_set_1, t_set_2);
		Set<TransitionLabel> difference_transitions_2_1 = Difference(t_set_2, t_set_1);
		int count_1 = 0;
		int count_2 = 0;
		int sum_1 = 0;
		for (TransitionLabel t : common_transitions) {
			count_1 = countTransitions(t, t_list_1);
			count_2 = countTransitions(t, t_list_2);
			sum_1 += count_1 * count_2;
		}
		float similarity_score;
		similarity_score = (float) sum_1
				/ (float) (sum_1 + difference_transitions_1_2.size() + difference_transitions_2_1.size());
		return similarity_score;
	}

//	 A version containing print statements (comments)
	private static float localSimilarityScore_W_2(List<TransitionLabel> t_list_1, List<TransitionLabel> t_list_2) {
		// TODO Auto-generated method stub
//		System.out.println("\nt1 list");
//		printList(t_list_1);
//		System.out.println("t2 list");
//		printList(t_list_2);

		Set<TransitionLabel> t_set_1 = new HashSet<TransitionLabel>(t_list_1);
		Set<TransitionLabel> t_set_2 = new HashSet<TransitionLabel>(t_list_2);

//		System.out.println("\nt1:");
//		printSet(t_set_1);
//		System.out.println("t2:");
//		printSet(t_set_2);

		Set<TransitionLabel> common_transitions = Intersection(t_set_1, t_set_2);
//		System.out.println("\nCommon transitions:");
//		printSet(common_transitions);

		Set<TransitionLabel> difference_transitions_1_2 = Difference(t_set_1, t_set_2);
//		System.out.println("\nt1 - t2:");
//		printSet(difference_transitions_1_2);

		Set<TransitionLabel> difference_transitions_2_1 = Difference(t_set_2, t_set_1);
//		System.out.println("\nt2 - t1:");
//		printSet(difference_transitions_2_1);

		// Calculating the number of matching transitions
		int count_1 = 0;
		int count_2 = 0;
		int sum_1 = 0;
		for (TransitionLabel t : common_transitions) {
			count_1 = countTransitions(t, t_list_1);
			count_2 = countTransitions(t, t_list_2);
			sum_1 += count_1 * count_2;
		}

		float similarity_score;
		similarity_score = (float) sum_1
				/ (float) (sum_1 + difference_transitions_1_2.size() + difference_transitions_2_1.size());
		return similarity_score;
	}

	private static int countTransitions(TransitionLabel transition, List<TransitionLabel> transition_list_1) {
		// TODO Auto-generated method stub
		int count = 0;
		for (TransitionLabel t : transition_list_1) {
			if (t.equals(transition)) {
				count += 1;
			}
		}
		return count;
	}

	private static void printSet(Set<TransitionLabel> t_set_1) {
		// TODO Auto-generated method stub
		if (t_set_1.size() == 0)
			System.out.println("empty set");
		else {
			for (TransitionLabel t : t_set_1) {
				t.printTransition();
			}
		}
	}

	private static void printList(List<TransitionLabel> t_list_outward_1) {
		// TODO Auto-generated method stub
		if (t_list_outward_1.size() == 0)
			System.out.println("empty list");
		else {
			for (TransitionLabel t : t_list_outward_1) {
				t.printTransition();
			}
		}
	}

	private static Set<TransitionLabel> Intersection2(Set<TransitionLabel> set_1, Set<TransitionLabel> set_2) {
		Set<TransitionLabel> intersection_set = new HashSet<TransitionLabel>();
		for (TransitionLabel t : set_1) {
			if (set_2.contains(t)) {
				intersection_set.add(t);
			}
		}
		return intersection_set;
	}

	private static Set<TransitionLabel> Intersection(Set<TransitionLabel> set_1, Set<TransitionLabel> set_2) {
		Set<TransitionLabel> intersection_set = new HashSet<TransitionLabel>();
		for (TransitionLabel t_1 : set_1) {
			int contains = 0;
			for (TransitionLabel t_2 : set_2) {
				if (t_1.equals(t_2)) {
					contains = 1;
					break;
				}
			}
			if (contains == 1) {
				intersection_set.add(t_1);
			}
		}
		return intersection_set;
	}

	private static Set<TransitionLabel> Difference(Set<TransitionLabel> set_1, Set<TransitionLabel> set_2) {
		Set<TransitionLabel> dif_set = new HashSet<TransitionLabel>();
		for (TransitionLabel t_1 : set_1) {
			int contains = 0;
			for (TransitionLabel t_2 : set_2) {
				if (t_1.equals(t_2)) {
					contains = 1;
					break;
				}
			}
			if (contains == 0) {
				dif_set.add(t_1);
			}
		}
		return dif_set;
	}

	@SuppressWarnings("null")
	private static Map<String, List<SimplifiedTransition<String, Word<String>>>> distinctTransitions(
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions) {
		// TODO Auto-generated method stub
		Map<String, List<SimplifiedTransition<String, Word<String>>>> distinct_transitions = null;
		for (Entry<String, List<SimplifiedTransition<String, Word<String>>>> t : transitions.entrySet()) {
			List<SimplifiedTransition<String, Word<String>>> list_1 = t.getValue();
			List<SimplifiedTransition<String, Word<String>>> distinct_list_1 = new ArrayList<>();
			List<Word<String>> outputs = new ArrayList<>();
			for (SimplifiedTransition<String, Word<String>> element : list_1) {
				Word<String> output = element.getOut();
				if (!outputs.contains(output)) {
					distinct_list_1.add(element);
					outputs.add(output);
				}
			}
			String key_1 = t.getKey();
			distinct_transitions.put(key_1, distinct_list_1);
		}
		return distinct_transitions;
	}

	private static int effectiveTransitionsCount(
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions) {
		// TODO Auto-generated method stub
		int count = 0;
		for (Entry<String, List<SimplifiedTransition<String, Word<String>>>> t : transitions.entrySet()) {
			List<SimplifiedTransition<String, Word<String>>> list_1 = t.getValue();
			for (SimplifiedTransition<String, Word<String>> element : list_1) {
				if (!element.getOut().toString().equals("1")) {
					count += 1;
				}
			}
		}
		return count;
	}

	private static File LoadFsmFile(int productIndex, File fsm_directory) {
		// TODO Auto-generated method stub
		String fixedLengthString = ConvertTofixedLengthString(productIndex);

		// For learned FSMs (samples)
		String productFileName = fixedLengthString + "_learnedFsm.txt";

		// For all products
//		String productFileName = fixedLengthString + "_fsm.txt";

		File productFile = new File(fsm_directory, productFileName);
		return productFile;
	}

	private static String ConvertTofixedLengthString(int productIndex) {
		// TODO Auto-generated method stub
		String productIndexString = Integer.toString(productIndex);
		String fixedLengthString = "00000".substring(productIndexString.length()) + productIndexString;
		return fixedLengthString;
	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption(HELP, false, "Help menu");
		options.addOption(FM, true, "Feature model");
		options.addOption(DIR0, true, "Directory of the input FFSM files");
		options.addOption(DIR1, true, "Directory of the input FSM files");
		options.addOption(DIR2, true, "Directory for the constructed FFSM files");
		return options;
	}
}