package de.metanome.algorithms.myinddetector;

import de.metanome.algorithm_helper.data_structures.ColumnCombinationBitset;
import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.AlgorithmExecutionException;
import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metanome.algorithm_integration.ColumnPermutation;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.InputIterationException;
import de.metanome.algorithm_integration.input.RelationalInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metanome.algorithm_integration.result_receiver.ColumnNameMismatchException;
import de.metanome.algorithm_integration.result_receiver.CouldNotReceiveResultException;
import de.metanome.algorithm_integration.result_receiver.InclusionDependencyResultReceiver;
import de.metanome.algorithm_integration.results.InclusionDependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class INDDetectorAlgorithmBarkowskyFeldmann {

    protected RelationalInputGenerator[] inputGenerators = null;
    protected InclusionDependencyResultReceiver resultReceiver = null;

    protected RelationalInput[] inputs = null;
    protected List<ColumnIdentifier> columnIdentifiers = null;

    public void execute() throws AlgorithmExecutionException {
        ////////////////////////////////////////////
        // THE DISCOVERY ALGORITHM LIVES HERE :-) //
        ////////////////////////////////////////////
        initialize();
        List<InclusionDependency> results = this.generateResults();
        emit(results);
    }

    protected void initialize() throws InputGenerationException, AlgorithmConfigurationException {
    	inputs = new RelationalInput[inputGenerators.length];
    	columnIdentifiers = new ArrayList<>();
    	for(int i = 0; i < inputGenerators.length; i++){
    		RelationalInputGenerator generator = inputGenerators[i];
    		inputs[i] = generator.generateNewCopy();
    		String name = inputs[i].relationName();
    		List<String> columnNames = inputs[i].columnNames();
    		for(String columnName:columnNames){
    			columnIdentifiers.add(new ColumnIdentifier(name, columnName));
    		}
    	}
    }

    protected List<InclusionDependency> generateResults() throws InputGenerationException, AlgorithmConfigurationException, InputIterationException {
        List<InclusionDependency> results = new ArrayList<>();
        ColumnCombinationBitset[] candidates = new ColumnCombinationBitset[columnIdentifiers.size()];
        for(int i = 0; i < columnIdentifiers.size(); i++){
        	candidates[i] = new ColumnCombinationBitset();
        	candidates[i].setAllBits(columnIdentifiers.size());
        }
        
        Map<String, ColumnCombinationBitset> values = createValueMap();
        for(ColumnCombinationBitset attributes:values.values()){
        	for(Integer attribute:attributes.getSetBits()){
        		candidates[attribute] = candidates[attribute].intersect(attributes);
        	}
        }
        
        for(int lhsIndex = 0; lhsIndex < columnIdentifiers.size(); lhsIndex++){
        	ColumnIdentifier lhsIdentifier = columnIdentifiers.get(lhsIndex);
        	ColumnPermutation lhs = new ColumnPermutation(lhsIdentifier);
        	for(Integer rhsIndex:candidates[lhsIndex].getSetBits()){
        		if(rhsIndex == lhsIndex){
        			continue;
        		}
        		
        		results.add(new InclusionDependency(lhs, new ColumnPermutation(columnIdentifiers.get(rhsIndex))));
        	}
        }
        
        return results;
    }

    private Map<String, ColumnCombinationBitset> createValueMap() throws InputGenerationException, AlgorithmConfigurationException, InputIterationException {
    	Map<String, ColumnCombinationBitset> values = new HashMap<>();
    	int offset = 0;
    	for(RelationalInput input:this.inputs){
    		int columns = input.numberOfColumns();
    		
    		while(input.hasNext()){
    			List<String> row = input.next();
    			for(int i = 0; i < columns; i++){
    				String value = row.get(i);
    				if(value == null){
    					continue;
    				}
    				if(!values.containsKey(value)){
    					values.put(value, new ColumnCombinationBitset());
    				}
    				values.get(value).addColumn(i + offset);
    			}
    		}
    		
    		offset += columns;
    	}
    	
		return values;
	}

	protected void emit(List<InclusionDependency> results) throws CouldNotReceiveResultException, ColumnNameMismatchException {
        for (InclusionDependency fd : results)
            this.resultReceiver.receiveResult(fd);
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
