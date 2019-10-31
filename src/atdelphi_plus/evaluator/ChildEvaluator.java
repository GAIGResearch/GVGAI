package atdelphi_plus.evaluator;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ChildEvaluator {
    private int _id;
    private int _size;
    private String _inputFolder;
    private String _outputFolder;
    
    public ChildEvaluator(int id, int size, String inputFolder, String outputFolder) {
	this._id = id;					//unique id of the child 
	this._size = size;				//how many levels this  child will handle
	this._inputFolder = inputFolder;
	this._outputFolder = outputFolder;
    }
    
    public boolean checkChromosomes() {
	int startIndex = this._id * this._size;
	for(int i=0; i<this._size; i++) {
	    File file = new File(this._inputFolder + (startIndex + i) + ".txt");
	    if(!file.exists()) {
		return false;
	    }
	}
	return true;
    }
    
    public String[] readChromosomes() throws IOException {
	String[] result = new String[this._size];
	int startIndex = this._id * this._size;
	for(int i=0; i<this._size; i++) {
	    result[i] = Files.readAllLines(Paths.get(this._inputFolder, (startIndex + i) + ".txt")).get(0);
	}
	return result;
    }
    
    public void writeResults(String[] values) throws FileNotFoundException, UnsupportedEncodingException {
	int startIndex = this._id * this._size;
	for(int i=0; i<values.length; i++) {
	    PrintWriter writer = new PrintWriter(this._outputFolder + (startIndex + i) + ".txt", "UTF-8");
	    writer.print(values[i]);
	    writer.close();
	}
    }
    
    public void clearInputFiles() {
	int startIndex = this._id * this._size;
	for(int i=0; i<this._size; i++) {
	    File f = new File(this._inputFolder + (startIndex + i) + ".txt");
	    f.delete();
	}
    }
}
