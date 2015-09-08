package uwt.knn;

import uwt.generic.MinMaxPriorityQueue;

import uwt.generic.FixedMaxHeap;

public class TestHeap {

    public static void main(String[] args) {
	// TODO Auto-generated method stub
	/*FixedMaxHeap<Integer> l = new FixedMaxHeap<Integer>(4);
	l.add(1);
	l.add(3);
	l.add(2);
	l.add(5);
	l.add(4);*/
	
	MinMaxPriorityQueue<Double> l = MinMaxPriorityQueue.maximumSize(100).create();
	l.add(20.0);
	System.out.println(l);
	l.add(4.0);
	System.out.println(l);
	l.add(5.0);
	System.out.println(l);
	l.add(8.0);
	System.out.println(l);
	l.add(10.0);
	System.out.println(l);
	l.add(11.0);
	System.out.println(l);
	l.add(13.0);
	System.out.println(l);
	l.add(100.0);
	System.out.println(l);
	/*l.add(60.0);
	System.out.println(l);
	l.add(23.0);
	System.out.println(l);
	l.add(30.0);
	System.out.println(l);
	l.add(40.0);
	System.out.println(l);
	l.add(6.0);
	System.out.println(l);
	l.add(9.0);
	System.out.println(l);
	l.add(50.0);
	System.out.println(l);
	l.add(1.0);
	System.out.println(l);*/



    }

}
