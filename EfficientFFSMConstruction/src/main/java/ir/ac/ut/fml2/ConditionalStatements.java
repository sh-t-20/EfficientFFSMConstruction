package ir.ac.ut.fml2;

import java.io.File;
import java.util.Collection;
import java.util.List;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.prop4j.Literal;
import org.prop4j.Node;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import uk.le.ac.ffsm.ConditionalState;
import uk.le.ac.ffsm.ConditionalTransition;
import uk.le.ac.ffsm.FeaturedMealy;
import uk.le.ac.ffsm.FeaturedMealyUtils;

public class ConditionalStatements {

	private static final String FM = "fm";
	private static final String FFSM = "ffsm";
	private static final String HELP = "h";

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try {

			// create the command line parser
			CommandLineParser parser = new BasicParser();
			// create the Options
			Options options = createOptions();
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();

			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			File f_fm = new File(line.getOptionValue(FM));
			IFeatureModel fm = FeatureModelManager.load(f_fm.toPath()).getObject();

			File ffsm_file = null;
			FeaturedMealy<String, Word<String>> ffsm_1 = null;

			if (line.hasOption(FFSM)) {
				ffsm_file = new File(line.getOptionValue(FFSM));
				ffsm_1 = FeaturedMealyUtils.getInstance().loadFeaturedMealy(ffsm_file, fm);
			}

			Alphabet<String> alphabet_1 = ffsm_1.getInputAlphabet();

			Collection<ConditionalState<ConditionalTransition<String, Word<String>>>> states_1 = ffsm_1.getStates();
			Node c1 = null;
			Node c2 = null;
			for (ConditionalState<ConditionalTransition<String, Word<String>>> s : states_1) {
				if (s.getId() == 1) {
					c1 = s.getCondition();
				}
				if (s.getId() == 2) {
					c2 = s.getCondition();
				}
			}

			System.out.println("c1:" + c1);
//			System.out.println(c1.);
			System.out.println("c2:" + c2);
			
			
			Node[] ch = c1.getChildren();
			if (ch[0].getLiterals().size() == 1) {
				System.out.println("c1 contains one product.");
			}
			for (Node c : ch) {
				System.out.println("Hi:\n" + c);
				if (c.equals(c2)) {
					System.out.println("equal:" + c);
				}
			}

			Node[] ch2 = c2.getChildren();
			if (ch2[0].getLiterals().size() == 1) {
				System.out.println("c2 contains one product.");
			}
			for (Node c : ch2) {
				System.out.println("ch2:" + c);
			}
			
			System.out.println("literals:");
			List<Literal> literals = c1.getLiterals();
			for (Literal l : literals) {
				System.out.println(l);
			}
			
			System.out.println("variables:");
			List<Object> vars = c1.getVariables();
			for (Object v : vars) {
				System.out.println(v);
			}

			System.out.println("Finished.");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption(FM, true, "Feature model");
		options.addOption(FFSM, true, "FFSM reference");
		options.addOption(HELP, false, "Help menu");
		return options;
	}
}
