package uwt.knn.predictors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import uwt.knn.KnnRow;
import uwt.knn.NearestNeighbor;

public class KnnCostPredictor implements KnnPredictor {

	@Override
	public double predict(KnnRow row) {
		// TODO Auto-generated method stub
		return getWeightedclasslabel(row);
		//return avg(row);
	}
	
	public double knnclass(KnnRow row)
	{
		HashMap<String, Integer> h = new HashMap<String, Integer>();
		List<NearestNeighbor> knnList = row.getKnnList();
		double classvalue=0;
		int majority=0;
		for (NearestNeighbor nn: knnList)
		{
			if(h.containsKey(nn.getLabel()))
			{
				h.put(nn.getLabel(), h.get(nn.getLabel())+1);
			}else
			{
				h.put(nn.getLabel(), 1 );
			}
		}
		Iterator it = h.entrySet().iterator();
	    while (it.hasNext()) {
	    	HashMap.Entry pair = (HashMap.Entry)it.next();
	    	int v = (int) pair.getValue();
	    	if((int) pair.getValue()>=majority)
	    	{
	    		classvalue=Double.parseDouble((String)pair.getKey());
	    		majority= (int) pair.getValue();
	    	}
	       
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    return classvalue;
	}
	
	public double avg(KnnRow row) //for regression.
	{
		double predicted=0;
		List<NearestNeighbor> knnList = row.getKnnList();
		double weightedCost = 0;
		for(NearestNeighbor nn: knnList)
		{
			weightedCost += Double.parseDouble(nn.getLabel());
		}
		predicted = weightedCost/knnList.size();
		return weightedCost;
	}
	
	public double getWeightedAvgCost(KnnRow row) //for regression
	{
		double predicted=0;
		List<NearestNeighbor> knnList = row.getKnnList();
		if(knnList.isEmpty())
			{
			predicted = Double.parseDouble(row.getLabel());}
		else
		{
		
		    	Collections.sort(knnList, Collections.reverseOrder());
			double distanceRange = knnList.get(knnList.size()-1).getDistance() - knnList.get(0).getDistance();
			double largestDistance = knnList.get(knnList.size()-1).getDistance();
			double weightedCost = 0;
			double weight;
			double totalWeight = 0;
			
			if(distanceRange == 0)
			{
				for(NearestNeighbor nn: knnList)
				{
					weightedCost += Double.parseDouble(nn.getLabel());
				}
				predicted = weightedCost/knnList.size();
				
			}
			else
			{
				for(NearestNeighbor nn: knnList)
				{
					if(distanceRange == 0)
						weight = 1;
					else
					weight = (largestDistance - nn.getDistance())/distanceRange;
					totalWeight += weight;
					weightedCost += weight * Double.parseDouble(nn.getLabel());
				}
				predicted = weightedCost/totalWeight;
			}
		}
		return predicted;
	}

	public double getWeightedclasslabel(KnnRow row)
	{
		System.out.println("New iteration starts Row: "+row.getId());
		double classlabel=0;
		double majority=0;
		HashMap<String, Double> h = new HashMap<String, Double>();
		HashMap<String, Double> w = new HashMap<String, Double>();
		List<NearestNeighbor> knnList = row.getKnnList();
		
		if(knnList.isEmpty())
		{System.out.println("if the knn is empty");
			classlabel = Double.parseDouble(row.getLabel());}
		else
		{
			System.out.println("inside the else");
			Collections.sort(knnList, Collections.reverseOrder());
			double distanceRange = knnList.get(knnList.size()-1).getDistance() - knnList.get(0).getDistance();
			double largestDistance = knnList.get(knnList.size()-1).getDistance();
			double weightedCost = 0;
			double weight;
			double totalWeight = 0;
			
			if(distanceRange == 0)
			{
				for (NearestNeighbor nn: knnList)
				{
				
					if(h.containsKey(nn.getLabel()))
					{
						h.put(nn.getLabel(), h.get(nn.getLabel())+1);
					}else
					{
						h.put(nn.getLabel(), 1.0 );
					}
				}
				Iterator it = h.entrySet().iterator();
			    while (it.hasNext()) {
			    	HashMap.Entry pair = (HashMap.Entry)it.next();
			    	//int v = (int) pair.getValue();
			    	if((double) pair.getValue()>=majority)
			    	{
			    		classlabel=Double.parseDouble((String)pair.getKey());
			    		majority=(double) pair.getValue();
			    	}
			       
			        it.remove(); // avoids a ConcurrentModificationException
			    }
				
			}
			else
			{
				System.out.println("Second else");
				for (NearestNeighbor nn: knnList)
				{
					
				
					if(h.containsKey(nn.getLabel()))
					{
						h.put(nn.getLabel(), h.get(nn.getLabel())+1);
					}else
					{
						h.put(nn.getLabel(), 1.0 );
					}
				}
				Iterator it = h.entrySet().iterator();
				
			    while (it.hasNext()) {
			    	totalWeight = 0.0;
			    	HashMap.Entry pair = (HashMap.Entry)it.next();
			    	String c=  (String) pair.getKey();
			    	System.out.println("class itrating" + c);
			    	for(NearestNeighbor nn: knnList)
			    	{
			    		System.out.println("Nearest neighbor: "+nn.getId());
			    		if(c.equals(nn.getLabel()))
			    		{
			    			System.out.println(nn.getLabel());
			    			System.out.println("if the classes match");
			    			weight = (largestDistance - nn.getDistance())/distanceRange;
			    			System.out.println("weight: "+weight);
							totalWeight += weight;
							System.out.println("total weight :"+totalWeight);
			    		}
			    	}
			    	it.remove();
			    	w.put(c, totalWeight);
			    	
			      }
			    	Iterator i = w.entrySet().iterator();
			    	while(i.hasNext())
			    	{
			    		HashMap.Entry set = (HashMap.Entry)i.next();
			    		System.out.println("calss :"+set.getKey());
			    		System.out.println("value :"+set.getValue());
			    		if((double) set.getValue()>=majority)
				    	{
				    		classlabel=Double.parseDouble((String)set.getKey());
				    		majority=(double) set.getValue();
				    	}
			    	  i.remove();
			    	}
			   }
				
			}
		System.out.println("decided class: "+classlabel);
		return classlabel;
	}
	@Override
	public void setClassValue(KnnRow row) {
		// TODO Auto-generated method stub
		row.setClassValue(Double.parseDouble(row.getLabel()));
	}

}
