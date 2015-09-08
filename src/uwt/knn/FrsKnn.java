package uwt.knn;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import uwt.frs.ApproxRow;
import uwt.frs.BucketUtil;
import uwt.frs.ClassVectorsGenerator;
import uwt.frs.FuzzySimilarityFunction;
import uwt.frs.MultiTypesSimilarity;
import uwt.generic.Row;
import uwt.generic.RowsDescriptor;
import uwt.generic.Utility;
import uwt.knn.predictors.ErrorAccumulation;
import uwt.knn.predictors.KnnCostPredictor;
import uwt.knn.predictors.KnnPredictor;
import uwt.knn.predictors.PredictedValue;

public class FrsKnn {

	public static void main(String[] args) throws IOException {
	    myMain("fuzzyparam.properties");
	}
	
    public static void myMain(String paramsPath) throws IOException {
	Properties properties = null;
	properties = Utility.readParameters(paramsPath);
	String command = properties.getProperty("command");
	String outputPath = properties.getProperty("output_path");
	int numOfPartitions = Integer.parseInt(properties.getProperty("partitions"));
	final RowsDescriptor rowFormat = new RowsDescriptor(properties.getProperty("attr_types"));

	String trainingSetPath = properties.getProperty("training_set");
	String testDataPath = properties.getProperty("test_data");
	final int numberOfNN = Integer.parseInt(properties.getProperty("knn"));
	String lowerApproxPath = properties.getProperty("lowerapprox");

	long startTime = System.currentTimeMillis() / 1000;
	long stopTime = 0;
	String outputMsg = "";

	ClassVectorsGenerator cvGen = new BucketUtil();
	FuzzySimilarityFunction simFunction = new MultiTypesSimilarity();
	DistanceFunction dfunction = new MultiTypesDistance();
	/*
	 * ClassVectorsGenerator cvGen = new ClassVectorsGenerator() {
	 * 
	 * @Override public void init(JavaRDD<Row> rowsRdd) { // TODO
	 * Auto-generated method stub
	 * 
	 * }
	 * 
	 * @Override public double[] generateClassVectors(String label) {
	 * double[] cValue = new double[1]; cValue[0] =
	 * Double.parseDouble(label); return cValue; } };
	 */

	final KnnCostPredictor predictor = new KnnCostPredictor();

	if (command.equals("p")) {
	    String output = Utility.predict(trainingSetPath, testDataPath, numberOfNN, predictor, rowFormat, dfunction);
	    //stopTime = System.currentTimeMillis() / 1000;
	    //outputMsg += "Done Predicting - Time elapsed: " + (stopTime - startTime) + " seconds\n";
	    //outputMsg += predPath;
	    System.out.println(output);
	} else if (command.equals("rmse")) {

	    startTime = System.currentTimeMillis() / 1000;
	    double newRMSE = Utility.knnTenFold(trainingSetPath, numberOfNN, outputPath, rowFormat, dfunction);
	    stopTime = System.currentTimeMillis() / 1000;
	    System.out.println("RMSE= " + newRMSE + " Time elapsed: " + (stopTime - startTime) + " seconds\n");

	} else {
	    List<Row> rows = null;
	    if (command.contains("l")) {
		try {
		    rows = Utility.computeHML(trainingSetPath, cvGen, rowFormat, simFunction);
		    System.out.println(rows);
		    System.out.println("Done Computing HML Qualities Below");
		} catch (IOException e) {
		    e.printStackTrace();
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
		stopTime = System.currentTimeMillis() / 1000;
		//outputMsg += "Done Computing Lower Approximation - Time elapsed: " + (stopTime - startTime) + " seconds\n";
		//outputMsg += lowerApproxPath + "\n";
	    } else if (command.contains("q")) {
		try {
		    rows = Utility.computePOWA(trainingSetPath, rowFormat, simFunction,numOfPartitions);
		    System.out.println("Done Computing POWA Qualities Below");
		    for(int h=0 ; h<rows.size(); h++)
		    	System.out.println(rows.get(h));
		    } 
		catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
		stopTime = System.currentTimeMillis() / 1000;
		//outputMsg += "Done Computing Lower Approximation - Time elapsed: " + (stopTime - startTime) + " seconds\n";
		//System.out.println("Done Computing Lower Approximation - Time elapsed: " + (stopTime - startTime) + " seconds\n");
		//outputMsg += lowerApproxPath + "\n";

	    }

	    if (command.contains("r")) {
		outputMsg += Utility.computeBestRMSE(rows, numberOfNN, dfunction);
		//System.out.println(outputMsg);
	    }
	}
	System.out.println(outputMsg);
    }

}
