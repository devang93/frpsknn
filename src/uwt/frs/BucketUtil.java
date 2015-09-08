package uwt.frs;

import java.util.List;

import uwt.generic.Row;

public class BucketUtil implements ClassVectorsGenerator {
	double a,b,c;
	
	private void init(double a, double b, double c, double minCost, double maxCost) {
		double costRange = maxCost - minCost;
		this.a = a * costRange + minCost;
		this.b = b * costRange + minCost;
		this.c = c * costRange + minCost;
	}
	
	public BucketUtil()
	{
	}
	
	public double getLow(double cost)
	{
		if(cost<=a)
			return 1;
		else if(cost>a && cost<b)
			return (cost - b)/(a-b);
		else
			return 0;
		
	}
	
	public double getMid(double cost)
	{
		if(cost>a && cost<=b)
			return (cost - a)/(b-a);
		else if(cost>b && cost<=c)
			return (cost - c)/(b-c);
		else
			return 0;
		
	}

	public double getHigh(double cost)
	{
		if(cost>=c)
			return 1;
		else if(cost>b && cost<c)
			return (cost - b)/(c-b);
		else
			return 0;
	}


	@Override
	public void init(List<Row> rows) {

		double min = Double.MAX_VALUE,max=Double.NEGATIVE_INFINITY;
		double temp = 0;
		Row r = null;
		for(int i=0;i<rows.size();i++)
		{
		    r= rows.get(i);
		    temp = Double.parseDouble(r.getLabel());
		    max = Math.max(max, temp);
		    min = Math.min(min, temp);
		}

		init(0.25, 0.5, 0.75,min,max);
		
	}

	@Override
	public double[] generateClassVectors(String label) {
		double cost = Double.parseDouble(label);
		double[] bucket = new double[3];
		bucket[0] = getLow(cost);
		bucket[1] = getMid(cost);
		bucket[2] = getHigh(cost);
		return bucket;
	}

}
