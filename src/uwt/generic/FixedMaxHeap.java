package uwt.generic;

import java.util.Arrays;

public class FixedMaxHeap<E extends Comparable<E>> {
    Comparable[] list;
    int maxIndex = -1;
    int count = 0;
    public FixedMaxHeap(int capacity)
    {
	list = new Comparable[capacity];
    }
    
    public void add(E e)
    {
	
	if(count == 0)
	{
	    list[0] = e;
	    maxIndex = 0;
	}
	else
	{
	    swap(e,maxIndex);
	}
	if(count<list.length)
	{
	    maxIndex = count;
	    count++;
	}
    }
    
    public E swap(E e, int i)
    {
	int c = 0;
	E returnVal = null;
	int leftIndex = -1, rightIndex = -1;
	c = e.compareTo((E) list[i]);
	E temp = null;
	if (c < 0) {
	    returnVal = (E) list[i];
	    leftIndex = 2 * i + 1;
	    if(leftIndex<count)
	    {
        	    temp = swap(e,leftIndex);
        	    if(temp == null)
        	    {
        		rightIndex =  2 * i + 2;
        		if(rightIndex<count)
        		{
        		    temp = swap(e,rightIndex);
        		    list[i] = temp;
        		}
        	    }
	    }
	    else
		list[i] = e;
	    if(temp!=null)
		list[i] = temp;
	    else
		list[i] = e;
	} 
	return returnVal;
    }
    
    public String toString()
    {
	return Arrays.toString(list);
    }

}
