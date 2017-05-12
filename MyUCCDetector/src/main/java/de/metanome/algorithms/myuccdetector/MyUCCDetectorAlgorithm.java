package de.metanome.algorithms.myuccdetector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.AlgorithmExecutionException;
import de.metanome.algorithm_integration.ColumnCombination;
import de.metanome.algorithm_integration.ColumnIdentifier;
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
	
	public void execute() throws AlgorithmExecutionException {
		
		////////////////////////////////////////////
		// THE DISCOVERY ALGORITHM LIVES HERE :-) //
		////////////////////////////////////////////
		// Example: Initialize
		this.initialize();
		// Example: Read input data
		List<List<String>> records = this.readInput();
		// Example: Print what the algorithm read (to test that everything works)
		this.print(records);
		// Example: Generate some results (usually, the algorithm should really calculate them on the data)
		List<UniqueColumnCombination> results = this.generateResults(records);
		// Example: To test if the algorithm outputs results
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
	
	protected List<UniqueColumnCombination> generateResults(List<List<String>> records) {
		List<UniqueColumnCombination> results = new ArrayList<>();
		List<List<Integer>> currentCandidates, negativeCandidates;
		
		//create initial candidates
		currentCandidates = new ArrayList<>();
		for(int i = 0; i < columnNames.size(); i++){
			List<Integer> candidate = new ArrayList<>();
			candidate.add(i);
			currentCandidates.add(candidate);
		}
		
		//a priori algorithm
		while(!currentCandidates.isEmpty()){
			System.out.println("candidates: " + currentCandidates);
			negativeCandidates = new ArrayList<>();
			for(List<Integer> candidate:currentCandidates){
				if(checkCCUniqueness(records, candidate)){
					UniqueColumnCombination ucc = new UniqueColumnCombination(getColumnIdentifiers(candidate));
					results.add(ucc);
				}
				else{
					negativeCandidates.add(candidate);
				}
			}
			System.out.println("negative candidates: " + negativeCandidates);
			
			currentCandidates = createNextCandidates(negativeCandidates);
			pruneCandidates(currentCandidates, negativeCandidates);
		}
		
		
		
//		for(long combinationNumber = 0; combinationNumber < Math.pow(2, columnNames.size()); combinationNumber++){
//			List<Integer> columnCombination = new ArrayList<>();
//			for(int i = 0; i < columnNames.size(); i++){
//				if((combinationNumber & (1l << i)) != 0){
//					columnCombination.add(i);
//				}
//			}
//			
//			if(checkCCUniqueness(records, columnCombination)){
//				UniqueColumnCombination ucc = new UniqueColumnCombination(getColumnIdentifiers(columnCombination));
//				results.add(ucc);
//			}
//		}
		return results;
	}

	private List<List<Integer>> createNextCandidates(List<List<Integer>> currentCandidates) {
		Set<List<Integer>> mergedCandidates = new HashSet<>();
		for(int i = 0; i < currentCandidates.size(); i++){
			for(int j = i + 1; j < currentCandidates.size(); j++){
				List<Integer> mergedCandidate = mergeCandidates(currentCandidates.get(i), currentCandidates.get(j));
				if(mergedCandidate.size() == currentCandidates.get(i).size() + 1 ){
					mergedCandidates.add(mergedCandidate);
				}
			}
		}
		return new ArrayList<List<Integer>>(mergedCandidates);
	}

	private List<Integer> mergeCandidates(List<Integer> candidate1, List<Integer> candidate2) {
		List<Integer> mergedCandidate = new ArrayList<>();
		for(int i= 0, j = 0; i < candidate1.size() || j < candidate2.size();){
			if(i < candidate1.size() && (j >= candidate2.size() || candidate1.get(i) < candidate2.get(j))){
				mergedCandidate.add(candidate1.get(i));
				i++;
			}
			else if(j < candidate2.size() && (i >= candidate1.size() || candidate2.get(j) < candidate1.get(i))){
				mergedCandidate.add(candidate2.get(j));
				j++;
			}
			else{
				mergedCandidate.add(candidate1.get(i));
				i++;
				j++;
			}
		}
		System.out.println("candidate 1:\t\t" + candidate1);
		System.out.println("candidate 2:\t\t" + candidate2);
		System.out.println("merged candidate:\t" + mergedCandidate);
		return mergedCandidate;
	}
	
	private void pruneCandidates(List<List<Integer>> currentCandidates, List<List<Integer>> negativeCandidates) {
		Set<List<Integer>> negativeCandidateSet = new HashSet<>(negativeCandidates);
		for(Iterator<List<Integer>> it = currentCandidates.iterator(); it.hasNext();){
			List<Integer> candidate = it.next();
			for(int i = 0; i < candidate.size(); i++){
				List<Integer> projection = new ArrayList<>(candidate);
				projection.remove(i);
				if(!negativeCandidateSet.contains(projection)){
					System.out.println("pruning " + candidate);
					it.remove();
					break;
				}
			}
		}
	}

	private ColumnIdentifier[] getColumnIdentifiers(List<Integer> columnCombination) {
		ColumnIdentifier[] identifiers = new ColumnIdentifier[columnCombination.size()];
		for(int i = 0; i < columnCombination.size(); i++){
			identifiers[i] = new ColumnIdentifier(this.relationName, this.columnNames.get(columnCombination.get(i)));
		}
		return identifiers;
	}

	protected ColumnIdentifier getRandomColumn() {
		Random random = new Random(System.currentTimeMillis());
		return new ColumnIdentifier(this.relationName, this.columnNames.get(random.nextInt(this.columnNames.size())));
	}
	
	protected boolean checkCCUniqueness(List<List<String>> records, List<Integer> columnCombination){
		Set<List<String>> uniqueValues = new HashSet<>();
		
		for(List<String> row:records){
			List<String> projection = getProjection(row, columnCombination);
			if(!uniqueValues.add(projection)){
				return false;
			}
		}
		
		return true;
	}

	private List<String> getProjection(List<String> row, List<Integer> columnNumbers) {
		List<String> projection = new ArrayList<>();
		for(int column:columnNumbers){
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
