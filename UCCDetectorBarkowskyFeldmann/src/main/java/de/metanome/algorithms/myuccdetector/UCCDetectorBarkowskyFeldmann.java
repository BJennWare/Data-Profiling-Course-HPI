package de.metanome.algorithms.myuccdetector;

import java.util.ArrayList;

import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.AlgorithmExecutionException;
import de.metanome.algorithm_integration.algorithm_types.RelationalInputParameterAlgorithm;
import de.metanome.algorithm_integration.algorithm_types.UniqueColumnCombinationsAlgorithm;
import de.metanome.algorithm_integration.configuration.ConfigurationRequirement;
import de.metanome.algorithm_integration.configuration.ConfigurationRequirementRelationalInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metanome.algorithm_integration.result_receiver.UniqueColumnCombinationResultReceiver;

public class UCCDetectorBarkowskyFeldmann extends UCCDetectorAlgorithmBarkowskyFeldmann 				// Separating the algorithm implementation and the Metanome interface implementation is good practice
						  implements UniqueColumnCombinationsAlgorithm, // Defines the type of the algorithm, i.e., the result type, for instance, FunctionalDependencyAlgorithm or InclusionDependencyAlgorithm; implementing multiple types is possible
						  			 RelationalInputParameterAlgorithm {	// Defines the input type of the algorithm; relational input is any relational input from files or databases; more specific input specifications are possible
						  			 
	public enum Identifier {
		INPUT_GENERATOR
	};

	@Override
	public String getAuthors() {
		return "Matthias Barkowsky, Benjamin Feldmann"; // A string listing the author(s) of this algorithm
	}

	@Override
	public String getDescription() {
		return "a priori ucc algorithm"; // A string briefly describing what this algorithm does
	}
	
	@Override
	public ArrayList<ConfigurationRequirement<?>> getConfigurationRequirements() { // Tells Metanome which and how many parameters the algorithm needs
		ArrayList<ConfigurationRequirement<?>> conf = new ArrayList<>();
		conf.add(new ConfigurationRequirementRelationalInput(UCCDetectorBarkowskyFeldmann.Identifier.INPUT_GENERATOR.name()));
		return conf;
	}

	@Override
	public void execute() throws AlgorithmExecutionException {
//		mode = MODE.Performance;
		mode = MODE.Debug;
		super.execute();
	}

	private void handleUnknownConfiguration(String identifier, Object[] values) throws AlgorithmConfigurationException {
		throw new AlgorithmConfigurationException("Unknown configuration: " + identifier + " -> [" + concat(values, ",") + "]");
	}
	
	private static String concat(Object[] objects, String separator) {
		if (objects == null)
			return "";
		
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < objects.length; i++) {
			buffer.append(objects[i].toString());
			if ((i + 1) < objects.length)
				buffer.append(separator);
		}
		return buffer.toString();
	}

	////////////////////////////////////////////////
	// input and receiver implementations
	////////////////////////////////////////////////

	@Override
	public void setRelationalInputConfigurationValue(String identifier, RelationalInputGenerator... values) throws AlgorithmConfigurationException {
		if (!UCCDetectorBarkowskyFeldmann.Identifier.INPUT_GENERATOR.name().equals(identifier))
			this.handleUnknownConfiguration(identifier, values);
		this.inputGenerator = values[0];
	}

	@Override
	public void setResultReceiver(UniqueColumnCombinationResultReceiver resultReceiver) {
		this.resultReceiver = resultReceiver;
	}
}
