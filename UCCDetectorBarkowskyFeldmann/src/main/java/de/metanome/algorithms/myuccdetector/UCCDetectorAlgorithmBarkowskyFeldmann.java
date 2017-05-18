package de.metanome.algorithms.myuccdetector;

import de.metanome.algorithm_helper.data_structures.ColumnCombinationBitset;
import de.metanome.algorithm_helper.data_structures.PLIBuilder;
import de.metanome.algorithm_helper.data_structures.PositionListIndex;
import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.AlgorithmExecutionException;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.InputIterationException;
import de.metanome.algorithm_integration.input.RelationalInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metanome.algorithm_integration.result_receiver.ColumnNameMismatchException;
import de.metanome.algorithm_integration.result_receiver.CouldNotReceiveResultException;
import de.metanome.algorithm_integration.result_receiver.UniqueColumnCombinationResultReceiver;
import de.metanome.algorithm_integration.results.UniqueColumnCombination;
import javafx.util.Pair;

import java.util.*;

public class UCCDetectorAlgorithmBarkowskyFeldmann {

    protected RelationalInputGenerator inputGenerator = null;
    protected UniqueColumnCombinationResultReceiver resultReceiver = null;

    protected String relationName;
    protected List<String> columnNames;

    protected enum MODE {Debug, Performance}

    protected MODE mode = MODE.Debug;

    public void execute() throws AlgorithmExecutionException {

        ////////////////////////////////////////////
        // THE DISCOVERY ALGORITHM LIVES HERE :-) //
        ////////////////////////////////////////////
        // Initialize
        this.initialize();
        // Read input data
//        List<List<String>> records = this.readInput();
        RelationalInput records = this.readInput();
        List<PositionListIndex> indices = this.buildPLI(records);

        // Print what the algorithm read (to test that everything works)
        if (mode.equals(MODE.Debug)) this.prettyPrint(this.readInput());

        // Generate some results (usually, the algorithm should really calculate them on the data)
        List<UniqueColumnCombination> results = this.generateResults(indices);

        // To test if the algorithm outputs results
        this.emit(results);
        /////////////////////////////////////////////

    }

    protected void initialize() throws InputGenerationException, AlgorithmConfigurationException {
        RelationalInput input = this.inputGenerator.generateNewCopy();
        this.relationName = input.relationName();
        this.columnNames = input.columnNames();
    }

    protected RelationalInput readInput() throws InputGenerationException, AlgorithmConfigurationException, InputIterationException {
        List<List<String>> records = new ArrayList<>();
        return this.inputGenerator.generateNewCopy();
    }

    protected List<PositionListIndex> buildPLI(RelationalInput input) throws InputIterationException {
        return new PLIBuilder(input, false).getPLIList();
    }

    protected void print(List<List<String>> records) {
        // Print schema
        System.out.print(this.relationName + "( ");
        for (String columnName : this.columnNames)
            System.out.print(columnName + " ");
        System.out.println(")");

        // Print records
        for (List<String> record : records) {
            System.out.print("| ");
            for (String value : record)
                System.out.print(value + " | ");
            System.out.println();
        }
    }

    protected void prettyPrint(RelationalInput input) throws InputIterationException {
        // Print schema
        List<List<String>> records = new ArrayList<>();
        while (input.hasNext())
            records.add(input.next());

        System.out.println(this.relationName + ":");

        int[] maxCellLength = new int[records.get(0).size()];
        int i = 0;
        for (String columnName : this.columnNames) {
            maxCellLength[i] = columnName.length();
            i++;
        }
        for (List<String> record : records) {
            i = 0;
            for (String value : record) {
            	if(value == null){
            		maxCellLength[i] = Math.max(maxCellLength[i], "null".length());
            	}
            	else{
            		maxCellLength[i] = Math.max(maxCellLength[i], value.length());
            	}
                i++;
            }
        }

        // Print records
        i = 0;
        System.out.print("| ");
        for (String columnName : this.columnNames) {
            System.out.print(columnName);
            for (int k = 0; k < maxCellLength[i] - columnName.length(); k++)
                System.out.print(" ");

            System.out.print(" | ");
            i++;
        }
        System.out.println();

        for (List<String> record : records) {
            i = 0;
            System.out.print("| ");
            for (String value : record) {
                System.out.print(value);
                for (int k = 0; k < (value != null ? maxCellLength[i] - value.length() : maxCellLength[i] - "null".length()) ; k++)
                    System.out.print(" ");

                System.out.print(" | ");
                i++;
            }
            System.out.println();
        }
    }

    protected List<UniqueColumnCombination> generateResults(List<PositionListIndex> records) {
        List<UniqueColumnCombination> results = new ArrayList<>();
        List<Pair<ColumnCombinationBitset, PositionListIndex>> currentCandidates, negativeCandidates;
        BitsetPrefixTreeNode prefixTree = new BitsetPrefixTreeNode(0, columnNames.size());

        //create initial candidates
        currentCandidates = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            ColumnCombinationBitset candidate = new ColumnCombinationBitset();
            candidate.addColumn(i);
            currentCandidates.add(new Pair<>(candidate, records.get(i)));
        }

        //a priori algorithm
        while (!currentCandidates.isEmpty()) {
            System.out.print("candidates: ");
            printCandidateList(currentCandidates);

            negativeCandidates = new ArrayList<>();
            for (Pair<ColumnCombinationBitset, PositionListIndex> candidate : currentCandidates) {
                if (checkCCUniqueness(candidate.getValue())) {
                    UniqueColumnCombination ucc = new UniqueColumnCombination(candidate.getKey().createColumnCombination(relationName, columnNames));
                    results.add(ucc);
                } else {
                    negativeCandidates.add(candidate);
                }
            }

            prefixTree.addBitsets(negativeCandidates);

            System.out.print("negative candidates: ");
            printCandidateList(negativeCandidates);
//			System.out.print("prefix tree: ");
//			System.out.println(prefixTree);

            List<ColumnCombinationBitset> bits = createNextCandidates(negativeCandidates, prefixTree);
            currentCandidates = pruneCandidates(bits, prefixTree);
        }

        return results;
    }

    private void printCandidateList(List<Pair<ColumnCombinationBitset, PositionListIndex>> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            ColumnCombinationBitset candidate = candidates.get(i).getKey();
            System.out.print(candidate.toString().substring(24));
            if (i != candidates.size() - 1) {
                System.out.print(", ");
            }
        }
        System.out.println();
    }

    private List<ColumnCombinationBitset> createNextCandidates(List<Pair<ColumnCombinationBitset, PositionListIndex>> candidates, BitsetPrefixTreeNode prefixTree) {
        List<ColumnCombinationBitset> mergedCandidates = new ArrayList<>();
        for (Pair<ColumnCombinationBitset, PositionListIndex> candidate : candidates) {
            BitsetPrefixTreeNode parentNode = prefixTree.getContainingNode(candidate.getKey()).getParent();
            int startIndex = candidate.getKey().getSetBits().get(candidate.getKey().size() - 1) + 1;

            BitsetPrefixTreeNode[] childrenNodes = parentNode.getChildren();
            for (int i = startIndex; i < childrenNodes.length; i++) {
                if (childrenNodes[i] != null) {
                    mergedCandidates.add(mergeCandidates(candidate.getKey(), childrenNodes[i].getBitset()));
                }
            }
        }
        return mergedCandidates;
    }

    private ColumnCombinationBitset mergeCandidates(ColumnCombinationBitset candidate1, ColumnCombinationBitset candidate2) {
        return candidate1.union(candidate2);
    }

    private List<Pair<ColumnCombinationBitset, PositionListIndex>> pruneCandidates(List<ColumnCombinationBitset> currentCandidates, BitsetPrefixTreeNode prefixTree) {
        List<Pair<ColumnCombinationBitset, PositionListIndex>> pruned = new ArrayList<>();

        for (Iterator<ColumnCombinationBitset> it = currentCandidates.iterator(); it.hasNext(); ) {
            ColumnCombinationBitset candidate = it.next();
            boolean buildPLI = true;
            PositionListIndex smallest = null;
            PositionListIndex secondSmallest = null;
            for (ColumnCombinationBitset projection : candidate.getDirectSubsets()) {
                if (!prefixTree.containsBitset(projection)) {
                    System.out.println("pruning " + candidate.toString().substring(24));
                    it.remove();
                    buildPLI = false;
                    break;
                }

                PositionListIndex index = prefixTree.getContainingNode(projection).getIndex();
                if (smallest == null) {
                    smallest = index;
                } else if (secondSmallest == null) {
                    if (smallest.size() > index.size()) {
                        secondSmallest = smallest;
                        smallest = index;
                    } else secondSmallest = index;
                } else if (index.size() < smallest.size()) {
                    secondSmallest = smallest;
                    smallest = index;
                } else if (index.size() < secondSmallest.size()) {
                    secondSmallest = index;
                }
            }

            if (buildPLI)
                pruned.add(new Pair<ColumnCombinationBitset, PositionListIndex>(candidate, smallest.intersect(secondSmallest)));
        }

        return pruned;
    }

    protected boolean checkCCUniqueness(PositionListIndex index) {
        return index.isUnique();
    }

    private PositionListIndex getProjection(List<PositionListIndex> row, ColumnCombinationBitset columnNumbers) {
        PositionListIndex index = null;
        for (int i : columnNumbers.getSetBits()) {
            if (index == null) index = row.get(i);
            else index = index.intersect(row.get(i));
        }
        return index;
    }

    protected void emit(List<UniqueColumnCombination> results) throws CouldNotReceiveResultException, ColumnNameMismatchException {
        for (UniqueColumnCombination ucc : results)
            this.resultReceiver.receiveResult(ucc);
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
