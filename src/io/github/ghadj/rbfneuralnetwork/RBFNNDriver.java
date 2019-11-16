package io.github.ghadj.rbfneuralnetwork;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Driver of the neural network. Takes as input from the arguments the path to
 * the file, containing the parameters of the neural network. Traings the neural
 * network based on the given parameters and exports the error per epoch and
 * clustering.
 * 
 * Assume that the parameter file has the following format:
 * numHiddenLayerNeurons (???) numInputNeurons (???) numOutputNeurons 1
 * learningRate (???) sigmas (???) maxIterations (???) centresFile
 * ../Parameters/centreVectors.txt trainFile ../Parameters/training.csv testFile
 * ../Parameters/test.csv
 * 
 * Compile from RBF-Neural-Network/ directory javac -d ./bin
 * ./src/io/github/ghadj/rbfneuralnetwork/*.java
 * 
 * Run from RBF-Neural-Network/ directory java -cp ./bin
 * io.github.ghadj.rbfneuralnetwork.RBFNNDriver <path to parameters.txt>
 * 
 * @author Georgios Hadjiantonis
 * @since 15-11-2019
 */
public class RBFNNDriver {
    private static final String errorFilename = "errors.txt";
    private static final String weightsFilename = "weights.txt";
    private static final int dataDimension = 53; // dimensions of the input data

    /**
     * Reads the parameters of the neural network from the given file.
     * 
     * @param filename path to the file containing the parameters
     * @return a String array containing the parameters.
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InvalidParameterException
     */
    public static String[] readParameters(String filename)
            throws FileNotFoundException, IOException, InvalidParameterException {
        File file = new File(filename);
        BufferedReader br;
        String[] parameters = new String[7];
        int i = 0;

        br = new BufferedReader(new FileReader(file));
        String st;
        while ((st = br.readLine()) != null)
            parameters[i++] = st.split(" ")[1];
        br.close();

        if (i != 9)
            throw new InvalidParameterException("Invalid parameters given.");
        return parameters;
    }

    /**
     * Reads data from the given file. Returns a map in the form of <input list,
     * output list>.
     * 
     * @param filename name of file to be read.
     * @return a map in the form of <input list, output list>.
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InvalidParameterException in case the data of the given file is
     *                                   inconsistent.
     */
    public static Map<List<Double>, Double> readData(String filename)
            throws FileNotFoundException, IOException, InvalidParameterException {
        Map<List<Double>, Double> data = new HashMap<List<Double>, Double>();
        File file = new File(filename);
        BufferedReader br;
        br = new BufferedReader(new FileReader(file));
        String st;
        while ((st = br.readLine()) != null) {
            String[] line = st.split(",");
            // molecule name + biological activity + molecule characteristics
            if (line.length != 1 + 1 + dataDimension) {
                br.close();
                throw new InvalidParameterException("Inconsistent data given in file " + filename);
            }

            List<Double> input = new ArrayList<>();
            int i = 1;
            // int i = 0; String moleculeName = line[i++];
            double output = Double.parseDouble(line[i++]);
            for (int j = 0; j < dataDimension; j++)
                input.add(Double.parseDouble(line[i++]));

            data.put(input, output);
        }
        br.close();
        return data;
    }

    public static List<List<Double>> readCentreVectors(String filename)
            throws FileNotFoundException, IOException, InvalidParameterException {
        File file = new File(filename);
        BufferedReader br;
        br = new BufferedReader(new FileReader(file));
        String st;
        List<List<Double>> centres = new ArrayList<>();
        while ((st = br.readLine()) != null) {
            String[] line = st.split(",");

            if (line.length != dataDimension) {
                br.close();
                throw new InvalidParameterException("Inconsistent data given in file " + filename);
            }

            List<Double> centre = new ArrayList<>();
            for (int j = 0; j < dataDimension; j++)
                centre.add(Double.parseDouble(line[j]));

            centres.add(centre);
        }
        br.close();
        return centres;
    }

    /**
     * Runs the NN based on the parameters, training and testing given data. Writes
     * the square error and clusters(labels per neuron) generated to two separate
     * files at the end of all the iterations.
     * 
     * @param parameters   array containing the given parameters.
     * @param trainingData training data.
     * @param testData     test data.
     * @throws IOException
     */
    public static void run(String[] parameters, List<List<Double>> centreVectors,
            Map<List<Double>, Double> trainingData, Map<List<Double>, Double> testData) throws IOException {

        RBFNN nn = new RBFNN(Integer.parseInt(parameters[0]), Integer.parseInt(parameters[1]),
                Integer.parseInt(parameters[2]), Double.parseDouble(parameters[3]), Double.parseDouble(parameters[4]),
                Integer.parseInt(parameters[5]), centreVectors);
        nn.run(trainingData, testData);
        List<Double> trainError = nn.getTrainErrorList();
        List<Double> testError = nn.getTestErrorList();
        writeResults(trainError, testError, errorFilename);
        writeWeights(nn.getWeights(), weightsFilename);
    }

    /**
     * Writes the results in csv format to the file given.
     * 
     * @param trainResults training squared error.
     * @param testResults  test squared error.
     * @param filename.
     * @throws IOException
     */
    public static void writeResults(List<Double> trainResults, List<Double> testResults, String filename)
            throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"));
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < trainResults.size() && i < testResults.size(); i++)
            str.append((i + 1) + "," + trainResults.get(i) + "," + testResults.get(i) + "\n");
        writer.write(str.toString());
        writer.close();
    }

    /**
     * Writes the weights of the centres to the filename given.
     * 
     * @param weights   array containing the corresponding weight per centre.
     * @param filename.
     * @throws IOException
     */
    public static void writeWeights(List<Double> weights, String filename) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"));
        StringBuilder str = new StringBuilder();

        writer.write(str.toString());
        writer.close();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Error: Enter the path to the parameters.txt as an argument to the program.");
            return;
        }
        Map<List<Double>, Double> trainingData, testData;
        List<List<Double>> centreVectors;
        String[] parameters;
        try {
            parameters = readParameters(args[0]);
            centreVectors = readCentreVectors(parameters[6]);
            trainingData = readData(parameters[7]);
            testData = readData(parameters[8]);

            run(parameters, centreVectors, trainingData, testData);
        } catch (InvalidParameterException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }

    }
}