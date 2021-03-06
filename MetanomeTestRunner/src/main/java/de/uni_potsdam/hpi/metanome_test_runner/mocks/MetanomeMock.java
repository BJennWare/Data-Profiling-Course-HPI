package de.uni_potsdam.hpi.metanome_test_runner.mocks;

import de.metanome.algorithm_integration.Algorithm;
import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.AlgorithmExecutionException;
import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metanome.algorithm_integration.configuration.ConfigurationSettingFileInput;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.RelationalInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metanome.algorithm_integration.results.Result;
import de.metanome.algorithm_integration.results.UniqueColumnCombination;
import de.metanome.algorithms.myfddetector.FDDetectorBarkowskyFeldmann;
import de.metanome.algorithms.myinddetector.INDDetectorBarkowskyFeldmann;
import de.metanome.algorithms.myuccdetector.UCCDetectorBarkowskyFeldmann;
import de.metanome.backend.input.file.DefaultFileInputGenerator;
import de.metanome.backend.result_receiver.ResultCache;
import de.uni_potsdam.hpi.metanome_test_runner.config.Config;
import de.uni_potsdam.hpi.metanome_test_runner.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MetanomeMock {

    public static void execute(Config conf) {
        try {
            RelationalInputGenerator inputGenerator = new DefaultFileInputGenerator(new ConfigurationSettingFileInput(
                    conf.inputFolderPath + conf.inputDatasetName + conf.inputFileEnding, true,
                    conf.inputFileSeparator, conf.inputFileQuotechar, conf.inputFileEscape, conf.inputFileStrictQuotes,
                    conf.inputFileIgnoreLeadingWhiteSpace, conf.inputFileSkipLines, conf.inputFileHasHeader,
                    conf.inputFileSkipDifferingLines, conf.inputFileNullString));

            ResultCache resultReceiver = new ResultCache("MetanomeMock", getAcceptedColumns(inputGenerator));

            Algorithm algorithm = null;
            switch (conf.algorithm) {
                case MYUCCDETECTOR:
                    algorithm = new UCCDetectorBarkowskyFeldmann();
                    ((UCCDetectorBarkowskyFeldmann) algorithm).setRelationalInputConfigurationValue(UCCDetectorBarkowskyFeldmann.Identifier.INPUT_GENERATOR.name(), inputGenerator);
                    ((UCCDetectorBarkowskyFeldmann) algorithm).setResultReceiver(resultReceiver);
                    break;
                case MYFDDETECTOR:
                    algorithm = new FDDetectorBarkowskyFeldmann();
                    ((FDDetectorBarkowskyFeldmann) algorithm).setRelationalInputConfigurationValue(FDDetectorBarkowskyFeldmann.Identifier.INPUT_GENERATOR.name(), inputGenerator);
                    ((FDDetectorBarkowskyFeldmann) algorithm).setResultReceiver(resultReceiver);
                    break;
                case MYINDDETECTOR:
                    algorithm = new INDDetectorBarkowskyFeldmann();
                    ((INDDetectorBarkowskyFeldmann) algorithm).setRelationalInputConfigurationValue(INDDetectorBarkowskyFeldmann.Identifier.INPUT_GENERATOR.name(), inputGenerator);
                    ((INDDetectorBarkowskyFeldmann) algorithm).setResultReceiver(resultReceiver);
                    break;
                default:
                    throw new IllegalArgumentException("wrong algorithm");
            }

            long runtime = System.currentTimeMillis();
            algorithm.execute();
            runtime = System.currentTimeMillis() - runtime;

            writeResults(conf, resultReceiver, algorithm, runtime);
        } catch (AlgorithmExecutionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<ColumnIdentifier> getAcceptedColumns(RelationalInputGenerator relationalInputGenerator) throws InputGenerationException, AlgorithmConfigurationException {
        List<ColumnIdentifier> acceptedColumns = new ArrayList<>();
        RelationalInput relationalInput = relationalInputGenerator.generateNewCopy();
        String tableName = relationalInput.relationName();
        for (String columnName : relationalInput.columnNames())
            acceptedColumns.add(new ColumnIdentifier(tableName, columnName));
        return acceptedColumns;
    }

    private static void writeResults(Config conf, ResultCache resultReceiver, Object algorithm, long runtime) throws IOException {
        if (conf.writeResults) {
            String outputPath = conf.measurementsFolderPath + conf.inputDatasetName + "_" + algorithm.getClass().getSimpleName() + File.separator;
            List<Result> results = resultReceiver.fetchNewResults();

            FileUtils.writeToFile(
                    algorithm.toString() + "\r\n\r\n" + conf.toString() + "\r\n\r\n" + "Runtime: " + runtime + "\r\n\r\n" + "Results: " + results.size(),
                    outputPath + conf.statisticsFileName);
            FileUtils.writeToFile(format(results), outputPath + conf.resultFileName);
        }
    }

    private static String format(List<Result> results) {
        StringBuilder builder = new StringBuilder();
        for (Result result : results) {
            builder.append(result.toString() + "\r\n");
        }
        return builder.toString();
    }
}
