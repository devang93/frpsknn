package uwt.frs.quality;

import java.util.Arrays;
import java.util.Vector;

import uwt.frs.ClassVectorsGenerator;
import uwt.generic.Row;
import uwt.generic.RowsDescriptor;

public class QualityRow extends Row {

	double normalizedOutcome;

	public QualityRow(double q)
	{
		quality = q;
	}
	
	public QualityRow(String line, RowsDescriptor rowFormat, double[] maxVals) {
		super(line, rowFormat);
		scale(maxVals);
	}
	
	public QualityRow(Row row, double[] maxVals, double[] minVals) {
		this.line = row.getLine();
		this.id = row.getId();
		this.numericAttributes = row.getNumericAttributes();
		this.booleanAttributes = row.getBooleanAttributes();
		this.stringAttributes = row.getStringAttributes();
		this.numOfAttributes = row.getNumOfAttributes();
		this.label = row.getLabel();
		this.setOutcome(row.getOutcome());
		scale(maxVals,minVals);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public String toString() {
		return id+","+quality;
	}

	public double[] getNumericAttributes() {
		return numericAttributes;
	}
	
	protected void scale(double[] maxVals, double[] minVals) {
		// TODO Auto-generated method stub
		//super.scale(maxVals);
		for(int i=0; i<numericAttributes.length;i++)
		{
			numericAttributes[i] = (numericAttributes[i] - minVals[i])/ (maxVals[i]- minVals[i]);
		}
		normalizedOutcome = (this.getOutcome()-minVals[minVals.length-1])/(maxVals[maxVals.length-1]-minVals[minVals.length-1]);
		//System.out.println("1NormalizedOutcome: "+ normalizedOutcome);
	}

	public double getNormalizedOutcome() {
		return normalizedOutcome;
	}

	public void setNormalizedOutcome(double normalizedOutcome) {
		this.normalizedOutcome = normalizedOutcome;
	}
	
	
	
	
	
	
}
