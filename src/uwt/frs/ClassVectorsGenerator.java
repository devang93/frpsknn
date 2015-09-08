package uwt.frs;

import java.io.Serializable;
import java.util.List;

import uwt.generic.Row;

public interface ClassVectorsGenerator extends Serializable {
	public abstract void init(List<Row> rows);
	public abstract double[] generateClassVectors(String label);
}
