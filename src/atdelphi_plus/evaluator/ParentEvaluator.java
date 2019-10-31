package atdelphi_plus.evaluator;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ParentEvaluator {
    private String _inputFolder;
    private String _outputFolder;
    
    public ParentEvaluator(String inputFolder, String outputFolder) {
	this._inputFolder = inputFolder;
	this._outputFolder = outputFolder;
    }
    
    public void writeChromosomes(String[] chromosomes) throws FileNotFoundException, UnsupportedEncodingException {
	for(int i=0; i<chromosomes.length; i++) {
	    PrintWriter writer = new PrintWriter(this._inputFolder + i + ".txt", "UTF-8");
	    writer.print(chromosomes[i]);
	    writer.close();
	}
    }
    
    public boolean checkChromosomes(int size) {
	for(int i=0; i<size; i++) {
	    File f = new File(this._outputFolder + i + ".txt");
	    if(!f.exists()) {
		return false;
	    }
	}
	return true;
    }
    
    public String[] assignChromosomes(int size) throws IOException {
	String[] results = new String[size];
	for(int i=0; i<size; i++) {
	    results[i] = Files.readAllLines(Paths.get(this._outputFolder, i + ".txt")).get(0);
	}
	return results;
    }
    
    public void clearOutputFiles(int size) {
	for(int i=0; i<size; i++) {
	    File f = new File(this._outputFolder + i + ".txt");
	    f.delete();
	}
    }
}