# Efficient Construction of Family-Based Behavioral Models from Adaptively Learned Models

In this repository, the implementation of the experiments of the paper "Efficient Construction of Family-Based Behavioral Models from Adaptively Learned Models" is briefly explained so that they can be replicated.
For each subject system, using FeatureIDE [1], sampling is performed using 3-wise method [2] and the Chvatal algorithm [3].
At the end of sampling, a number of configurations from each subject system are determined as sample products, which are stored as ".config" files.
We conducted the experiments using the samples used in [4], whose FSMs are provided in the same paper.
The files required to run the experiments in this paper are available in the ``experiments`` directory.
In this directory, there is a separate directory for each subject system.
The directory of each subject system contains the following items.

* The "xml" file of the feature model
* A directory named ``products_3wise`` containing the configuration files and the FSMs of a sample of products (a 3-wise sample)
* A directory named ``learned_FSMs`` containing the files of the FSMs learned using PL*

For the BCS SPL, there is also a directory named ``ffsm_dir`` that contains the FFSMs constructed using the local similarity metric v1.
These FFSMs are used as input files in the experiment evaluating the efficiency of FFSM construction.

## The Effect of Learning Order on Efficiency

In this experiment the effect of learning order on learning cost metrics (including the total number of resets and the total number of input symbols) is evaluated.
For 200 learning orders, model learning of the sample products is performed using the PL* algorithm and the learning cost metrics are measured.
Also, for each of these learning orders, the parameters *D* and *D'* are calculated.
Learning the product models using the PL* algorithm and also the calculation of the parameter *D* is done using the implementations provided in [4].

To replicate the experiment for calculating the parameter *D'*, the ``CalculateOrderMetric2`` class must be run with the following parameters:

* -dir: The directory containing the FSMs and configuration files of products (these files are available in the ``products_3wise`` directory)
* -out: The output directory for storing the log file
* -name: SPL name

Using the ``CalculateOrderMetric2`` class, the values of the parameter *D'* are calculated for all the tested learning orders, and the results are stored in a log file.
Using the ``ConvertToExcelFile3`` class provided in [4], the results are stored as a ".csv" file.
The results of these experiments are available  in the ``results/results_1`` directory.


## The Effect of the Similarity Metric on FFSM Construction

In this section, the experiments performed to evaluate the effect of the type of similarity metric on the conciseness of the constructed FFSM and also on the efficiency are explained.
In the following experiments, the implementation of the FFSM<sub>Diff</sub> algorithm provided in [5,6] is used to construct the FFSMs.

### Conciseness of the Final FFSM

To replicate this experiment, the ``MergeFSMsSize`` class must be run with the following parameters.

* -fm: The ".xml" file of the feature model (available in ``experiments`` directory)
* -dir1: The directory containing the input FSM files (the ".txt" files of the FSMs learned using the PL* algorithm which are available in the ``learned_FSMs`` directory)
* -dir2: The directory for storing the constructed FFSM files

The type of similarity metric used is determined by the ``algorithm_version`` integer variable.
For the similarity metrics used in these experiments, the ``algorithm_version`` variable is set as follows.

* 1: The global similarity metric
* 5: The local similarity metric v1
* 6: The local similarity metric v2

To run this experiment, the order of merging the product models must be specified.
For this purpose, the products are sorted in ascending order of the number of states.
According to this merging order, the indexes of products are stored in the "product_order" array.
For each subject system, the lines of the source code where ``product_order`` of that SPL is set must be uncommented.

The values of the parameters *t*, *r*, and *k* (which are used in the FFSM<sub>Diff</sub> algorithm) are specified by the variables ``T_value``, ``R_value``, and ``K_value``, respectively.
In these experiments ``R_value`` is set to 1.4 and ``K_value`` is set to 0.5.
Considering the numbers in the range of 0 and 1 with intervals of 0.05 for the variable ``T_value``, FFSM construction is performed for the similarity metrics mentioned above.

In each experiment, the FSM files are merged according to the order specified in ``product_order`` using the FFSM<sub>Diff</sub> algorithm.
Therefore, in each step, the FFSM constructed in the previous step is merged with an FSM to construct a new FFSM, and the number of states and transitions of the constructed FFSM is printed.
The ``.csv'' files containing the results of these experiments are available in the ``results/results_2_1`` directory.

### Efficiency of FFSM Construction

To replicate this experiment, the ``MergeFSMsTime`` class must be run using the following parameters.

* -fm: The ".xml" file of the feature model
* -dir0: The directory containing the input FFSM files (these files are available in the ``ffsm_dir`` directory)
* -dir1: The directory containing the input FSM files (the learned FSM files are available in the ``learned_FSMs`` directory)
* -dir2: The directory for storing the constructed FFSM files

The type of the similarity metric is determined by the variable ``algorithm_version``, and the order of merging the product models is determined by the array ``product_order``.
In these experiments, ``T_value`` is set to 0.5, ``R_value`` is set to 1.4, and ``K_value`` is set to 0.5.

In this experiment, in each step, two input models with specified sizes are merged with each other and an FFSM is constructed, and the time required to complete this process is measured.
For the BCS SPL, for each similarity metric, this experiment is repeated 10 times.
The ".csv" files containing the results of these experiments are available in the ``results/results_2_2`` directory.

## Licensing
The artifacts are all available under GNU public License 3.0.
It makes use of the following three repositories, which are also available under the same license, and are properly attributed in the artifact:

https://github.com/sh-t-20/artifacts [4]

https://github.com/damascenodiego/learningFFSM [5,6]

https://github.com/damascenodiego/DynamicLstarM [7]

## References:

[1] T. Thum, C. Kastner, F. Benduhn, J. Meinicke, G. Saake, and T. Leich, "FeatureIDE: An extensible framework for feature-oriented software development", Science of Computer Programming, vol. 79, pp. 70-85, 2014.

[2] M. F. Johansen, Ø. Haugen, and F. Fleurey, "Properties of realistic feature models make combinatorial testing of product lines feasible", in Model Driven Engineering Languages and Systems, 14th International Conference, MODELS 2011, Proceedings, ser. Lecture Notes in Computer Science, vol. 6981. Springer, 2011, pp. 638-652.

[3] V. Chvatal, "A greedy heuristic for the set-covering problem", Mathematics of Operations Research, vol. 4, no. 3, pp. 233-235, 1979.

[4] S. Tavassoli, C. D. N. Damasceno, R. Khosravi, and M. R. Mousavi, "Adaptive behavioral model learning for software product lines", in SPLC'22: 26th ACM International Systems and Software Product Line Conference, Volume A. ACM, 2022, pp. 142-153.

[5] C. D. N. Damasceno, M. R. Mousavi, and A. da Silva Simao, "Learning by sampling: learning behavioral family models from software product lines", Empirical Software Engineering, vol. 26, no. 1, p. 4, 2021.

[6] C. D. N. Damasceno, M. R. Mousavi, and A. da Silva Simao, "Learning from difference: an automated approach for learning family models from software product lines", in proceedings of the 23rd International Systems and Software Product Line Conference, SPLC 2019, Volume A. ACM, 2019, pp. 10:1-10:12.

[7] C. D. N. Damasceno, M. R. Mousavi, and A. da Silva Simao, "Learning to reuse: Adaptive model learning for evolving systems", in Integrated Formal Methods - 15th International Conference, IFM 2019, Proceedings, ser. Lecture Notes in Computer Science, vol. 11918. Springer, 2019, pp. 138-156.
