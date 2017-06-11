package de.metanome.algorithms.myfddetector;

import de.metanome.algorithm_helper.data_structures.ColumnCombinationBitset;
import de.metanome.algorithm_helper.data_structures.PLIBuilder;
import de.metanome.algorithm_helper.data_structures.PositionListIndex;
import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.AlgorithmExecutionException;
import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.InputIterationException;
import de.metanome.algorithm_integration.input.RelationalInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metanome.algorithm_integration.result_receiver.ColumnNameMismatchException;
import de.metanome.algorithm_integration.result_receiver.CouldNotReceiveResultException;
import de.metanome.algorithm_integration.result_receiver.FunctionalDependencyResultReceiver;
import de.metanome.algorithm_integration.results.FunctionalDependency;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FDDetectorAlgorithmBarkowskyFeldmann {

    protected RelationalInputGenerator inputGenerator = null;
    protected FunctionalDependencyResultReceiver resultReceiver = null;

    protected String relationName;
    protected List<String> columnNames;

    protected enum MODE {Debug, Performance}

    protected MODE mode = MODE.Debug;

    public void execute() throws AlgorithmExecutionException {
        ////////////////////////////////////////////
        // THE DISCOVERY ALGORITHM LIVES HERE :-) //
        ////////////////////////////////////////////
        initialize();
        List<List<String>> records = this.readInput();
        List<PositionListIndex> indices = this.buildPLI(inputGenerator.generateNewCopy());
        List<FunctionalDependency> results = this.generateResults(records.size(), indices);
        emit(results);
    }

    protected void initialize() throws InputGenerationException, AlgorithmConfigurationException {
        RelationalInput input = this.inputGenerator.generateNewCopy();
        this.relationName = input.relationName();
        this.columnNames = input.columnNames();
    }

    protected List<List<String>> readInput() throws InputGenerationException, AlgorithmConfigurationException, InputIterationException {
    	 List<List<String>> records = new ArrayList<>();
        RelationalInput input = inputGenerator.generateNewCopy();
        while(input.hasNext()){
        	records.add(input.next());
        }
        return records;
    }

    protected List<PositionListIndex> buildPLI(RelationalInput input) throws InputIterationException {
        return new PLIBuilder(input, false).getPLIList();
    }

    protected List<FunctionalDependency> generateResults(int recordCount, List<PositionListIndex> plis) {
        List<FunctionalDependency> results = new ArrayList<>();
        List<Tuple<ColumnCombinationBitset, PositionListIndex>> currentCandidates, negativeCandidates;
        
        BitsetPrefixTreeNode prefixTree = new BitsetPrefixTreeNode(0, columnNames.size());
        prefixTree.setBitsetIndex(new Tuple<ColumnCombinationBitset, PositionListIndex>(new ColumnCombinationBitset(), null));
        ColumnCombinationBitset fullCandidateSet = new ColumnCombinationBitset();
        fullCandidateSet.setAllBits(columnNames.size());
        prefixTree.setCandidateSet(fullCandidateSet);
        
        //create initial candidates
        currentCandidates = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            ColumnCombinationBitset candidate = new ColumnCombinationBitset();
            candidate.addColumn(i);
            currentCandidates.add(new Tuple<>(candidate, plis.get(i)));
        }

        //a priori algorithm
        while (!currentCandidates.isEmpty()) {
        	negativeCandidates = new ArrayList<>();
        	
            for (Tuple<ColumnCombinationBitset, PositionListIndex> candidate : currentCandidates) {
            	ColumnCombinationBitset candidateSet = null;
            	for(ColumnCombinationBitset subset:candidate.e1.getDirectSubsets()){
            		ColumnCombinationBitset subsetCandidateSet = prefixTree.getContainingNode(subset).getCandidateSet();
            		if(candidateSet == null){
            			candidateSet = subsetCandidateSet;
            		}
            		else{
            			candidateSet = candidateSet.intersect(subsetCandidateSet);
            		}
            	}
            	
            	ColumnCombinationBitset candidateRhs = candidate.e1.intersect(candidateSet);
            	
            	for(int rhs:candidateRhs.getSetBits()){
            		ColumnCombinationBitset lhs = new ColumnCombinationBitset(candidate.e1);
            		lhs.removeColumn(rhs);
            		
                	if (checkFD(lhs, rhs, prefixTree, recordCount, plis)) {
                		FunctionalDependency fd = new FunctionalDependency(lhs.createColumnCombination(relationName, columnNames), new ColumnIdentifier(relationName, columnNames.get(rhs)));
                        results.add(fd);
                        
                		candidateSet.removeColumn(rhs);
                		candidateSet = candidateSet.minus(fullCandidateSet.minus(candidate.e1));
                    }
                }
            	
            	if(!candidateSet.isEmpty()){
            		negativeCandidates.add(candidate);
                	BitsetPrefixTreeNode containingNode = prefixTree.addBitset(candidate);
                	containingNode.setCandidateSet(candidateSet);
            	}
            }

            List<ColumnCombinationBitset> bits = createNextCandidates(negativeCandidates, prefixTree);
            currentCandidates = pruneCandidates(bits, prefixTree);
        }

        return results;
    }

    private List<ColumnCombinationBitset> createNextCandidates(List<Tuple<ColumnCombinationBitset, PositionListIndex>> candidates, BitsetPrefixTreeNode prefixTree) {
        List<ColumnCombinationBitset> mergedCandidates = new ArrayList<>();
        for (Tuple<ColumnCombinationBitset, PositionListIndex> candidate : candidates) {
            BitsetPrefixTreeNode parentNode = prefixTree.getContainingNode(candidate.e1).getParent();
            int startIndex = candidate.e1.getSetBits().get(candidate.e1.size() - 1) + 1;

            BitsetPrefixTreeNode[] childrenNodes = parentNode.getChildren();
            for (int i = startIndex; i < childrenNodes.length; i++) {
                if (childrenNodes[i] != null) {
                    mergedCandidates.add((candidate.e1.union(childrenNodes[i].getBitset())));
                }
            }
        }
        return mergedCandidates;
    }

    private List<Tuple<ColumnCombinationBitset, PositionListIndex>> pruneCandidates(List<ColumnCombinationBitset> currentCandidates, BitsetPrefixTreeNode prefixTree) {
        List<Tuple<ColumnCombinationBitset, PositionListIndex>> pruned = new ArrayList<>();

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
                pruned.add(new Tuple<>(candidate, smallest.intersect(secondSmallest)));
        }

        return pruned;
    }

    protected boolean checkFD(ColumnCombinationBitset lhs, int rhs, BitsetPrefixTreeNode prefixTree, int recordCount, List<PositionListIndex> plis) {
    	if(lhs.size() < 1){
    		return false;
    	}
    	
    	PositionListIndex pli = prefixTree.getContainingNode(lhs).getIndex();

        return keyError(pli, recordCount) == keyError(plis.get(rhs).intersect(pli), recordCount);
    }

    protected float keyError(PositionListIndex index, int r){
        return index.getRawKeyError() / (float) r;
    }

    protected void emit(List<FunctionalDependency> results) throws CouldNotReceiveResultException, ColumnNameMismatchException {
        for (FunctionalDependency fd : results)
            this.resultReceiver.receiveResult(fd);
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
                if (value == null) {
                    maxCellLength[i] = Math.max(maxCellLength[i], "null".length());
                } else {
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
                for (int k = 0; k < (value != null ? maxCellLength[i] - value.length() : maxCellLength[i] - "null".length()); k++)
                    System.out.print(" ");

                System.out.print(" | ");
                i++;
            }
            System.out.println();
        }
    }

    @SuppressWarnings("unused")
	private void printCandidateList(List<Tuple<ColumnCombinationBitset, PositionListIndex>> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            ColumnCombinationBitset candidate = candidates.get(i).e1;
            System.out.print(candidate.toString().substring(24));
            if (i != candidates.size() - 1) {
                System.out.print(", ");
            }
        }
        System.out.println();
    }
    
    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
