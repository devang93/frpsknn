package uwt.generic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.event.ListDataEvent;

import com.google.common.primitives.Doubles;

import uwt.frs.ApproxRow;
import uwt.frs.ClassVectorsGenerator;
import uwt.frs.FuzzySimilarityFunction;
import uwt.frs.quality.QualityRow;
import uwt.knn.DistanceFunction;
import uwt.knn.KnnRow;
import uwt.knn.NearestNeighbor;
import uwt.knn.predictors.ErrorAccumulation;
import uwt.knn.predictors.KnnCostPredictor;
import uwt.knn.predictors.KnnPredictor;
import uwt.knn.predictors.PredictedValue;

public class Utility implements Serializable {

    public static double getJaccardSim(long set1, long set2) {
	long intersection = set1 & set2;
	long union = set1 | set2;
	if (union == 0) // if both sets are empty, return 0
	    return 1;
	double jacardSim = Long.bitCount(intersection) / Long.bitCount(union);

	return jacardSim;
    }

    public static double getDistance(Row row1, Row row2) {
   
	double[] numAttr1 = row1.getNumericAttributes();
	double[] numAttr2 = row2.getNumericAttributes();
	//System.out.println(numAttr1+ " "+ numAttr2);
	long[] boolAttr1 = row1.getBooleanAttributes();
	long[] boolAttr2 = row2.getBooleanAttributes();

	String[] strAttr1 = row1.getStringAttributes();
	String[] strAttr2 = row2.getStringAttributes();
	double temp=0;
	double numDistance = 0;
	for (int i = 0; i < numAttr1.length; i++) {
	    if(!(Double.isFinite(numAttr1[i])) || !(Double.isFinite(numAttr2[i])))
	    {
	    	
	    	//System.out.println(numAttr1[i]+"  "+numAttr2[i]);
	    }
	    else
	    {
	    	
	    	numDistance += Math.abs(numAttr1[i] - numAttr2[i]);
	    }
	    //System.out.println(numAttr1[i]+"-"+numAttr2[i]+"="+numDistance);
	}

	double boolDistance = 0;
	for (int i = 0; i < boolAttr1.length; i++) {
	    boolDistance += getJaccardSim(boolAttr1[i], boolAttr2[i]);
	}
	boolDistance = boolAttr1.length - boolDistance;

	double strDistance = 0;
	for (int i = 0; i < strAttr1.length; i++) {
	    strDistance += strAttr1[i].equals(strAttr2[i]) ? 0 : 1;
	}

	return (numDistance + boolDistance + strDistance);
    }

    public static Properties readParameters(String path) {
	Properties prop = new Properties();
	InputStream input = null;

	try {
	    input = new FileInputStream(path);
	    prop.load(input);

	} catch (IOException ex) {
	    ex.printStackTrace();
	} finally {
	    if (input != null) {
		try {
		    input.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}
	return prop;
    }

    public static void setKnn(int k, KnnRow testRow, KnnRow[] trainingSet, DistanceFunction dFunction) {
	NearestNeighbor neighbour;
	double distance;

	for (KnnRow trainingRow : trainingSet) {
	    if (testRow.getFoldNo() != trainingRow.getFoldNo()) {
		distance = dFunction.getDistance(testRow, trainingRow);
		neighbour = new NearestNeighbor();
		neighbour.setId(trainingRow.getId());
		neighbour.setDistance(distance);
		neighbour.setLabel(trainingRow.getLabel());
		testRow.addNearestNeighbor(neighbour);
	    }
	}

    }

    public static void setKnn(int k, KnnRow testRow, KnnRow[] trainingSet, DistanceFunction dFunction, boolean isFoldNotApplicable) {
	NearestNeighbor neighbour;
	double distance;
	for (KnnRow trainingRow : trainingSet) {
	    if (isFoldNotApplicable || testRow.getFoldNo() != trainingRow.getFoldNo()) {
		distance = dFunction.getDistance(testRow, trainingRow);
		neighbour = new NearestNeighbor();
		neighbour.setId(trainingRow.getId());
		neighbour.setDistance(distance);
		neighbour.setLabel(trainingRow.getLabel());
		testRow.addNearestNeighbor(neighbour);
	    }
	}

    }

    public static List<Row> loadRows(String path, RowsDescriptor rowFormat) throws IOException {
	BufferedReader reader = new BufferedReader(new FileReader(new File(path)));
	String line = reader.readLine();
	List<Row> rows = new ArrayList<Row>();
	while (line != null) {
	    rows.add(new Row(line, rowFormat));
	    line = reader.readLine();
	}
	reader.close();
	return rows;
    }

    public static List<KnnRow> loadKnnRows(String path, RowsDescriptor rowFormat, KnnPredictor predictor, int k) throws IOException {
	BufferedReader reader = new BufferedReader(new FileReader(new File(path)));
	String line = reader.readLine();
	List<KnnRow> rows = new ArrayList<KnnRow>();
	while (line != null) {
	    rows.add(new KnnRow(line, rowFormat, k, predictor));
	    line = reader.readLine();
	}

	return rows;
    }

    /**
     * Uses the prototype set in protoTypePath to predict the outcome of each instance in the test set under testDataPath.
     * @param protoTypePath
     * @param testDataPath
     * @param numberOfNN
     * @param predictor This object defines the function to apply on the kNN (such as weighted avg) to predict the outcome.
     * @param rowFormat Object used to parse a row in a file
     * @param dFunction Distance function
     * @return output message listing test rows' IDs, their actual outcomes and their predicted outcomes
     * @throws IOException
     */
    public static String predict(final String protoTypePath, final String testDataPath, final int numberOfNN, final KnnPredictor predictor, final RowsDescriptor rowFormat,
	    final DistanceFunction dFunction) throws IOException {
	List<KnnRow> testRows = loadKnnRows(testDataPath, rowFormat, predictor, numberOfNN);
	List<KnnRow> trainingRows = loadKnnRows(protoTypePath, rowFormat, predictor, numberOfNN);
	KnnRow[] trainingSetRows = trainingRows.toArray(new KnnRow[trainingRows.size()]);

	for (KnnRow testRow : testRows) {
	    setKnn(numberOfNN, testRow, trainingSetRows, dFunction, true);
	    System.out.println("testrow: "+testRow.id+"knnset: "+testRow.getKnnList());
	}

	PredictedValue pr;
	String output="Row ID, Actual Outcome, Predicted Outcome\n";
	for(KnnRow r:testRows)
	{
	    pr = new PredictedValue(r);
	    output+=r.getId()+","+pr.getActualValue()+","+pr.getPredictedValue()+"\n";
	}
	return output;
    }

    public static double knnTenFold(final String filePath, int k, String outputPath, final RowsDescriptor rowFormat, DistanceFunction dFunction) throws IOException {
	long startTime = System.currentTimeMillis() / 1000;

	// Properties properties = null;
	// properties = Utility.readParameters(paramsPath);
	/*
	 * final String filePath = properties.getProperty("data_path"); int k =
	 * Integer.parseInt(properties.getProperty("k")); String outputPath =
	 * properties.getProperty("output_path"); final RowsDescriptor rowFormat
	 * = new RowsDescriptor(properties.getProperty("attr_types"));
	 */

	BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
	String line = reader.readLine();
	List<KnnRow> rows = new ArrayList<KnnRow>();
	KnnCostPredictor predictor = new KnnCostPredictor();
	while (line != null) {
	    rows.add(new KnnRow(line, rowFormat, k, predictor));
	    line = reader.readLine();
	}
	int size = rows.size();
	System.out.println(size);
	KnnRow[] inMemoryRows = new KnnRow[rows.size()];
	inMemoryRows = rows.toArray(inMemoryRows);
	double[] error = new double[10];
	double[] counts = new double[10];
	int foldNo = -1;
	int counter=0;
	PredictedValue predVal;
	String output = "";
	for (KnnRow testRow : rows) {
	    output += testRow.getId() + ",";
	    Utility.setKnn(k, testRow, inMemoryRows, dFunction);
	    predVal = new PredictedValue(testRow);
	    output += predVal.getPredictedValue() + ",";
	    output += testRow.getKnnString() + "\n";
	    foldNo = testRow.getFoldNo();
	   // System.out.println(foldNo);
	    if(predVal.getActualValue()==predVal.getPredictedValue())
	    {
	    	counter= counter+1;
	    }
	    error[foldNo] += predVal.getSquaredError();
	    counts[foldNo] += 1;
	}
	System.out.println("counter:" +counter);
	System.out.println("rows:"+ rows.size());
	float r = (float)counter/rows.size();
	System.out.println(counter / rows.size() + r);
	/*double rmse = 0;
	for (int i = 0; i < 10; i++) {
	    rmse += Math.sqrt(error[i] / counts[i]);
	}
	rmse = rmse / 10;*/
	double acc =r;
	//double acc= ((float)(counter/rows.size()));
	long stopTime = System.currentTimeMillis() / 1000;
	output += "\nTime elapsed: " + (stopTime - startTime) + " seconds\n";

	//System.out.println(output);
	System.out.println("Accuaracy:" +acc);
	return acc;

    }

    /**
     * Apply kNN 10-Fold CV on a data set given a prototype set and the original data set which is used to generate test sets.
     * @param prototypeSet A list of rows in the prototype set
     * @param originalSet A list of rows in all data set. This gets virtually split into 10 groups (folds) and RMSE is computed for each group.
     * @param k number of NN
     * @return RMSE
     * @throws IOException
     */
    public static double knnTenFold(List<Row> prototypeSet, List<Row> originalSet, int k, DistanceFunction dFunction) throws IOException {
	long startTime = System.currentTimeMillis() / 1000;

	List<KnnRow> rows = new ArrayList<KnnRow>();
	KnnCostPredictor predictor = new KnnCostPredictor();
	/*prepare the prototype set. Note that each KnnRow object gets assigned a fold number. When computing
	 * kNN for an instance i, only instances j with different fold number are considered.
	 */
	for (Row r : prototypeSet) {
	    rows.add(new KnnRow(r, k, predictor));
	}
	
	/*prepare the test rows. Note that each KnnRow object gets assigned a fold number. When computing
	 * kNN for an instance i, only instances j with different fold number are considered.
	 */
	List<KnnRow> testRows = new ArrayList<KnnRow>();
	for (Row r : originalSet) {
	    testRows.add(new KnnRow(r, k, predictor));
	}

	KnnRow[] inMemoryRows = new KnnRow[rows.size()];
	inMemoryRows = rows.toArray(inMemoryRows);
	
	double[] error = new double[10]; //an element for each fold number (we have 10 fold numbers)
	double[] counts = new double[10]; //an element for each fold number (we have 10 fold numbers)
	int foldNo = -1;
	int counter=0;
	PredictedValue predVal;
	String output = "";
	for (KnnRow testRow : testRows) {
	    output += testRow.getId() + ",";
	    Utility.setKnn(k, testRow, inMemoryRows, dFunction); //compute kNN for testRow from inMemoryRows (ignore instances with equal fold number)
	    predVal = new PredictedValue(testRow); //compute weighted average of kNN of testRow 
	    //System.out.println("Row: "+testRow.getId()+"Predicted value:"+predVal.getPredictedValue());
	    output += predVal.getPredictedValue() + ",";
	    //System.out.println("Row:" +testRow.getId()+" "+testRow.getKnnString());
	    foldNo = testRow.getFoldNo();
	    if(predVal.getActualValue()==predVal.getPredictedValue())
	    {
	    	error[foldNo] += 1;
	    	
	    }
	    //error[foldNo] += predVal.getSquaredError(); //accumulate the squared error for each fold
	    counts[foldNo] += 1; //count the number of elements in each fold
	  //System.out.println("Counter at "+foldNo+" with acc" + error[foldNo] );
		//System.out.println("rows:"+ testRows.size());
	}
	
	//System.out.println("Counter at "+ );
	//System.out.println("rows:"+ testRows.size());
	//float r = (float)counter/testRows.size();
	//System.out.println(counter / testRows.size() + r);
	/*for(int h=0; h<10 ; h++)
	{
		System.out.println("Error"+error[h]);
	}*/
	//double rmse = 0;
	double accuracy=0;
	for (int i = 0; i < 10; i++) {
	    if(counts[i]!=0)
	    	//System.out.println("Counter at "+i+" with acc" + error[i]+ " "+ counts[i] );
	    accuracy += (float)(error[i] / counts[i]);
	    //System.out.println(accuracy);//compute RMSE for fold and sum them all together
	}
	
	accuracy = accuracy/10;
	/*double rmse = 0;
	for (int i = 0; i < 10; i++) {
	    rmse += Math.sqrt(error[i] / counts[i]);
	}
	rmse = rmse / 10;*/

	//double acc= ((float)(counter/rows.size()));
	long stopTime = System.currentTimeMillis() / 1000;
	output += "\nTime elapsed: " + (stopTime - startTime) + " seconds\n";

	//System.out.println(output);
	//System.out.println("Accuaracy:" +accuracy);
	return accuracy;
	//rmse = rmse / 10; //take the average of all RMSEs of every fold. The result is the final RMSE
	//long stopTime = System.currentTimeMillis() / 1000;
	//output += "\nTime elapsed: " + (stopTime - startTime) + " seconds\n";

	//System.out.println(output);
	//System.out.println(rmse);
	//return rmse;

    }
    
    

    public static double[] getRanges(List<Row> rows) {
	int numOfAttrs = rows.get(0).getNumericAttributes().length;
	double[] ranges = new double[numOfAttrs];
	double[] min = new double[numOfAttrs];
	for (int i = 0; i < min.length; i++)
	    min[i] = Double.MAX_VALUE;

	double[] max = new double[numOfAttrs];
	for (int i = 0; i < max.length; i++)
	    max[i] = Double.NEGATIVE_INFINITY;

	double[] attrs;
	for (Row row : rows) {
	    attrs = row.getNumericAttributes();
	    for (int i = 0; i < attrs.length; i++) {
		min[i] = Math.min(min[i], attrs[i]);
		max[i] = Math.max(max[i], attrs[i]);
	    }

	}

	for (int i = 0; i < ranges.length; i++)
	    ranges[i] = max[i] - min[i];
	return ranges;

    }
    
    /**
     * Computes the max of every attribute in the data set
     * @param rows the rows of the data set
     * @return an array containing the maximum value of every attribute in the data set
     */
    public static double[] getMax(List<Row> rows) {
	int numOfAttrs = rows.get(0).getNumericAttributes().length;

	double[] max = new double[numOfAttrs+1];
	for (int i = 0; i < max.length; i++)
	    max[i] = Double.NEGATIVE_INFINITY;

	double[] attrs;
	for (Row row : rows) {
	    attrs = row.getNumericAttributes();
	    for (int i = 0; i < attrs.length; i++) {
		max[i] = Math.max(max[i], attrs[i]);
	    }
	    max[max.length-1] = Math.max(max[max.length-1], row.getOutcome());
	}
	return max;
    }
    
    public static double[] getMin(List<Row> rows) {
	int numOfAttrs = rows.get(0).getNumericAttributes().length;

	double[] min = new double[numOfAttrs+1];
	for (int i = 0; i < min.length; i++)
	    min[i] = Double.MAX_VALUE;

	double[] attrs;
	for (Row row : rows) {
	    attrs = row.getNumericAttributes();
	    for (int i = 0; i < attrs.length; i++) {
		min[i] = Math.min(min[i], attrs[i]);
	    }
	    min[min.length-1] = Math.min(min[min.length-1], row.getOutcome());
	}
	return min;
    }

    /**
     * Uses the HML approach to computes the quality of every row in the data set located under filePath 
     * @param filePath A path to a data set
     * @param cvGen This object is used to generate values for high mid and low the class vectors.
     * @param rowFormat This object is used to parse each row in the data set file.
     * @param simFunction This is the similarity function applied when computing similarities between instances
     * @return A list of Row objects where each Row object represents a row in the data set and each of these rows has an HML quality value associated with it.
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<Row> computeHML(final String filePath, final ClassVectorsGenerator cvGen, final RowsDescriptor rowFormat, final FuzzySimilarityFunction simFunction)
	    throws IOException, InterruptedException {
	//long startTime = System.currentTimeMillis() / 1000;
	List<Row> rows = loadRows(filePath, rowFormat);
	cvGen.init(rows);
	double[] ranges = getRanges(rows);

	List<ApproxRow> approxRows = new ArrayList<ApproxRow>();

	ApproxRow newRow;

	for (Row r : rows) {
	    /*
	     * Convert Row to ApproxRow which causes generating class vector 
	     * values at this instance and also divides the attributes' values by the ranges
	     */
	    newRow = new ApproxRow(r, cvGen, ranges, false); 
	    approxRows.add(newRow);
	}
	
	double simVal = 0;
	double[] classVectors;
	double[] lowerApproxValues;
	double implicator;
	double quality = Double.NEGATIVE_INFINITY;
	for (ApproxRow row : approxRows) { //outter loop 
	    quality = Double.NEGATIVE_INFINITY;
	    lowerApproxValues = row.getLowerApproxValues();
	    for (ApproxRow row2 : approxRows) { // inner loop
		simVal = simFunction.getSimilarity(row, row2);
		classVectors = row2.getClassVectors();

		for (int i = 0; i < classVectors.length; i++) {
		    implicator = Math.max((1 - simVal), classVectors[i]); //compute the implicator
		    lowerApproxValues[i] = Math.min(implicator, lowerApproxValues[i]); //accumulate the lower approximation values
		}

	    }

	    for (int i = 0; i < lowerApproxValues.length; i++)
		quality = Math.max(quality, lowerApproxValues[i]); //apply fuzzy union on all of the high, mid and low class vectors
	    row.setQuality(quality);
	}

	List<Row> result = new ArrayList<Row>();
	result.addAll(approxRows);
	return result;
    }

    public static String saveToDisk(String result, String outputPath) {
	return "";
    }

    
    /**
     * Uses the POWA/OWA approach to computes the quality of every row in the data set located under filePath. When partition=1 then OWA is computed. Otherwise, OWA is approximated with POWA.
     * @param filePath filePath A path to a data set
     * @param rowFormat This object is used to parse each row in the data set file.
     * @param simFunction This is the similarity function applied when computing similarities between instances
     * @param numOfPartitions This controls the degree of approximated OWA using POWA. 1 means compute OWA and more than 1 means approximate OWA using POWA.
     * @return A list of Row objects where each Row object represents a row in the data set and each of these rows has an POWA quality value associated with it.
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<Row> computePOWA(final String filePath, final RowsDescriptor rowFormat, final FuzzySimilarityFunction simFunction, int numOfPartitions) throws IOException,
	    InterruptedException {
	long startTime = System.currentTimeMillis() / 1000;
	List<Row> rows = loadRows(filePath, rowFormat);
	//double[] ranges = getRanges(rows);
	double[] maxVals = getMax(rows);
	double[] minVals = getMin(rows);

	List<QualityRow> qualityRows = new ArrayList<QualityRow>();

	QualityRow newRow;

	for (Row r : rows) {
	    /*
	     * Convert Row to ApproxRow which divides the attributes' values by the ranges
	     */
	    newRow = new QualityRow(r, maxVals,minVals);
	    qualityRows.add(newRow);
	}

	double simVal = 0;
	double implicator;
	double d1, d2, rd;
	//Double[] qualities = new Double[rows.size()];
	List<Double> qualities = new ArrayList<Double>();
	
	double quality = 0;
	Double[] w ;
	//int index = 0;
	List<Double> owaVals = new ArrayList<Double>();
	
	List<List<QualityRow>> partitions = Utility.partitionList(qualityRows, numOfPartitions);
	
	for (QualityRow row : qualityRows) {//outter loop
	    d1 = row.getNormalizedOutcome();
	    quality = 1;
	    for(List<QualityRow> partition:partitions)
	    {
		    for (QualityRow row2 : partition) { //inner loop
			if (row2.getNormalizedOutcome()!= row.getNormalizedOutcome()) {
				//System.out.println(row+" "+row2);
			    simVal = simFunction.getSimilarity(row, row2);
			    //System.out.println(row.getId()+" "+row2.getId());
			    //compute attributes' similarity
			    d2 = row2.getNormalizedOutcome();
			    rd = 1 - Math.abs(d1 - d2);
			    //compute outcome similarity
			   
			    //it is used only for the regression part.
			    /* if(simVal>rd)
			    {
				implicator = 1 - simVal + rd;
			
				qualities.add(implicator);
				
			    }*/
			   implicator = 1 - simVal;
			   
			   qualities.add(implicator);
			   
			} 
		   }
		    w = generateWeights(qualities.size());
		    quality = owa(qualities, w);
		    owaVals.add(quality);
		    //System.out.println("Row :"+row.getId()+" Quality: "+ quality);
		    qualities.clear();
	    }
	   // System.out.println("Row :"+row.getId()+" Quality: "+ quality);
	    /*w = generateWeights(numOfPartitions);
	    quality = owa(owaVals, w);
	    owaVals.clear();
	    row.setQuality(quality);*/
	    
	 quality = avgowa(owaVals);
	    owaVals.clear();
	     //System.out.println(quality);
	    row.setQuality(quality);
	    	    
	    
	}

	List<Row> result = new ArrayList<Row>();
	result.addAll(qualityRows);
	return result;

    }
    
    public static <T> List<List<T>> partitionList(List<T> list, int numOfPartitions) {
	
	List<List<T>> partitions = new ArrayList<List<T>>(numOfPartitions);
	for(int i=0;i<numOfPartitions;i++)
	    partitions.add(new ArrayList<T>());
	
	for(int i=0,j=0;i<list.size();i++)
	{
	    partitions.get(j).add(list.get(i));
	    if(j==numOfPartitions-1)
		j=0;
	    else
		j++;
	}
	return partitions;
    }
    
    public static Double[] listDoubleToArray(List<Double> list)
    {
	Double[] arr = new Double[list.size()];
	for(int i=0;i<list.size();i++)
	{
	    arr[i] = list.get(i);
	}
	return arr;
    }
    
    
    public static double avgowa(List<Double> v) {
    	
    	double result = 0;
    	for (int i = 0; i < v.size(); i++)
    	    result += v.get(i);
    	result= result/v.size();
    	return result;
        }
    
    

    public static double owa(Double[] v, Double[] w) {
	Arrays.sort(v, Collections.reverseOrder());
	double result = 0;
	for (int i = 0; i < v.length; i++)
	    result += v[i] * w[i];
	return result;
    }
    
    public static double owa(List<Double> v, Double[] w) {
	//Arrays.sort(v, Collections.reverseOrder());
	Collections.sort(v, Collections.reverseOrder());
	double result = 0;
	int size = v.size();
	for (int i = 0; i < size; i++)
	    result += v.get(i) * w[i];
	return result;
    }

    public static Double[] generateWeights(int numOfRows) {
	Double[] w = new Double[numOfRows];
	double d = 0;
	for (int i = 1; i <= numOfRows; i++) {
	    d += (1.0 / i);
	}

	for (int i = 0, j = numOfRows; i < numOfRows; i++, j--) {
	    w[i] = 1 / (d * j);
	}
	return w;
    }

    public static List<Row> filter(List<Row> rows, double quality) {
	List<Row> result = new ArrayList<Row>();

	for (Row r : rows) {
	    if (r.getQuality() >= quality)
		result.add(r);
	}
	return result;
    }

    /**
     * This method computes the highest quality value that produce the least RMSE. It basically run kNN 10-fold on the data set ten times and in each 
     * time it filters out some rows based on their qualities.
     * @param rows The rows of the data set having their qualities computed
     * @param k number of NN
     * @return output message
     * @throws IOException
     */
    public static String computeBestRMSE(List<Row> rows, int k, DistanceFunction dFunction) throws IOException {
	double maxQuality = 0, minQuality = 1;
	

	for (Row r : rows) {
	    maxQuality = Math.max(maxQuality, r.getQuality());
	    minQuality = Math.min(minQuality, r.getQuality());
	}
	
	
	double qualityDecrement = (maxQuality - minQuality) / 100;
	double quality = maxQuality-qualityDecrement;
	//quality -= qualityDecrement;
	double rmse = Double.MAX_VALUE;
	double newRMSE = Double.MAX_VALUE;
	double acc=0;
	DecimalFormat df = new DecimalFormat("###.###");
	long startTime, stopTime;
	String outputMsg = null;
	double bestQuality = 0;
	double lowestRMSE = Double.MAX_VALUE;
	double accuracy=0;

	List<Row> prototypeSet;
	outputMsg+="\nRMSE,Quality\n";
	String newOutput = "quality,rmse,size,time\n";
	while (quality >= minQuality) {
	    prototypeSet = Utility.filter(rows, quality);
	  
	    //System.out.println("Size:"+prototypeSet.size());
	    //System.out.println("Quality:"+ quality);
	    startTime = System.currentTimeMillis() / 1000;

	    newRMSE = Utility.knnTenFold(prototypeSet,rows, k, dFunction);
	    stopTime = System.currentTimeMillis() / 1000;
	    newOutput += df.format(quality)+","+newRMSE+","+prototypeSet.size()+","+(stopTime - startTime)+"\n";
	    //outputMsg+= "Done Computing RMSE = "+newRMSE+" for quality= "+df.format(quality)+" - Time elapsed: " + (stopTime - startTime) + " seconds\n";
	    acc = newRMSE;
	    if (acc > accuracy) {
		accuracy = newRMSE;
		bestQuality = quality;
	    }
	    quality -= qualityDecrement;
	}

	final double finalQuality = bestQuality;
	newOutput += "Best Quality Threshold =" + finalQuality + "\n";
	return newOutput;
    }
    
    public static void printQualities(List<Row> rows)
    {
	Collections.sort(rows);
	for(Row r:rows)
	    System.out.println(r.id+","+r.quality);
    }

}
