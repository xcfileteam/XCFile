package commonapplet;

import java.io.*;

public class Pair<A,B> implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public A first;
	public B second;
	
	public Pair(A first,B second){
		this.first = first;
		this.second = second;
	}
}
