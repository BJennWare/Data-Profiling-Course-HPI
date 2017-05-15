package de.metanome.algorithms.myuccdetector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.metanome.algorithm_helper.data_structures.ColumnCombinationBitset;
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

public class MyUCCDetectorAlgorithm {

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
        List<List<String>> records = this.readInput();

        // Print what the algorithm read (to test that everything works)
        if (mode.equals(MODE.Debug)) this.prettyPrint(records);

        // Generate some results (usually, the algorithm should really calculate them on the data)
        List<UniqueColumnCombination> results = this.generateResults(records);

        // To test if the algorithm outputs results
        this.emit(results);
        /////////////////////////////////////////////

    }

    protected void initialize() throws InputGenerationException, AlgorithmConfigurationException {
        RelationalInput input = this.inputGenerator.generateNewCopy();
        this.relationName = input.relationName();
        this.columnNames = input.columnNames();
    }

    protected List<List<String>> readInput() throws InputGenerationException, AlgorithmConfigurationException, InputIterationException {
        List<List<String>> records = new ArrayList<>();
        RelationalInput input = this.inputGenerator.generateNewCopy();
        while (input.hasNext())
            records.add(input.next());
        return records;
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

    protected void prettyPrint(List<List<String>> records) {
        // Print schema
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
                maxCellLength[i] = Math.max(maxCellLength[i], value.length());
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
                for (int k = 0; k < maxCellLength[i] - value.length(); k++)
                    System.out.print(" ");

                System.out.print(" | ");
                i++;
            }
            System.out.println();
        }
    }

    protected List<UniqueColumnCombination> generateResults(List<List<String>> records) {
        List<UniqueColumnCombination> results = new ArrayList<>();
        List<ColumnCombinationBitset> currentCandidates, negativeCandidates;
        BitsetPrefixTreeNode prefixTree = new BitsetPrefixTreeNode(0, columnNames.size());

        //create initial candidates
        currentCandidates = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            ColumnCombinationBitset candidate = new ColumnCombinationBitset();
            candidate.addColumn(i);
            currentCandidates.add(candidate);
        }

        //a priori algorithm
        while (!currentCandidates.isEmpty()) {
            System.out.print("candidates: ");
            printCandidateList(currentCandidates);

            negativeCandidates = new ArrayList<>();
            for (ColumnCombinationBitset candidate : currentCandidates) {
                if (checkCCUniqueness(records, candidate)) {
                    UniqueColumnCombination ucc = new UniqueColumnCombination(candidate.createColumnCombination(relationName, columnNames));
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

            currentCandidates = createNextCandidates(negativeCandidates, prefixTree);
            pruneCandidates(currentCandidates, prefixTree);
        }

        return results;
    }

    private void printCandidateList(List<ColumnCombinationBitset> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            ColumnCombinationBitset candidate = candidates.get(i);
            System.out.print(candidate.toString().substring(24));
            if (i != candidates.size() - 1) {
                System.out.print(", ");
            }
        }
        System.out.println();
    }

    private List<ColumnCombinationBitset> createNextCandidates(List<ColumnCombinationBitset> candidates, BitsetPrefixTreeNode prefixTree) {
        List<ColumnCombinationBitset> mergedCandidates = new ArrayList<>();
        for (ColumnCombinationBitset candidate : candidates) {
            BitsetPrefixTreeNode parentNode = prefixTree.getContainingNode(candidate).getParent();
            int startIndex = candidate.getSetBits().get(candidate.size() - 1) + 1;

            BitsetPrefixTreeNode[] childrenNodes = parentNode.getChildren();
            for (int i = startIndex; i < childrenNodes.length; i++) {
                if (childrenNodes[i] != null) {
                    mergedCandidates.add(mergeCandidates(candidate, childrenNodes[i].getValue()));
                }
            }
        }
        return mergedCandidates;
    }

    private ColumnCombinationBitset mergeCandidates(ColumnCombinationBitset candidate1, ColumnCombinationBitset candidate2) {
        return candidate1.union(candidate2);
    }

    private void pruneCandidates(List<ColumnCombinationBitset> currentCandidates, BitsetPrefixTreeNode prefixTree) {
        for (Iterator<ColumnCombinationBitset> it = currentCandidates.iterator(); it.hasNext(); ) {
            ColumnCombinationBitset candidate = it.next();
            for (ColumnCombinationBitset projection : candidate.getDirectSubsets()) {
                if (!prefixTree.containsBitset(projection)) {
                    System.out.println("pruning " + candidate.toString().substring(24));
                    it.remove();
                    break;
                }
            }
        }
    }

    protected boolean checkCCUniqueness(List<List<String>> records, ColumnCombinationBitset columnCombination) {
        Set<List<String>> uniqueValues = new HashSet<>();

        for (List<String> row : records) {
            List<String> projection = getProjection(row, columnCombination);
            if (!uniqueValues.add(projection)) {
                return false;
            }
        }

        return true;
    }

    private List<String> getProjection(List<String> row, ColumnCombinationBitset columnNumbers) {
        List<String> projection = new ArrayList<>();
        for (int column : columnNumbers.getSetBits()) {
            projection.add(row.get(column));
        }
        return projection;
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
