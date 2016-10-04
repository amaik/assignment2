package lsde10.suspicious.outage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.storage.StorageLevel;

import dk.tbsalling.aismessages.ais.messages.AISMessage;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.SparkConf;


public class SuspiciousOutageApp {
	
	private SparkConf sparkConf;
	private JavaSparkContext javaSparkContext;
	
	private void init(){
		sparkConf = new SparkConf().setAppName("SuspiciousOutageApp").setMaster("yarn-cluster");
		javaSparkContext = new JavaSparkContext(sparkConf);
		
	}
	private static SuspiciousOutageApp instance = null;

	private SuspiciousOutageApp() {
		this.init();
	}


	public static SuspiciousOutageApp getInstance() {
		if (instance == null) {
			instance = new SuspiciousOutageApp();
		}
		return instance;
	}
	
	
	public SparkConf getSparkConf() {
		return sparkConf;
	}
	public void setSparkConf(SparkConf sparkConf) {
		this.sparkConf = sparkConf;
	}
	public JavaSparkContext getJavaSparkContext() {
		return javaSparkContext;
	}
	public void setJavaSparkContext(JavaSparkContext javaSparkContext) {
		this.javaSparkContext = javaSparkContext;
	}


	
	

	public static void main( String[] args )
    {
		
		SuspiciousOutageApp app = SuspiciousOutageApp.getInstance();
		JavaSparkContext sc = app.getJavaSparkContext();
		Processor processor = Processor.getInstance();
		
		
		JavaRDD<String> distFile = sc.textFile("\\user\\hannesm\\lsde\\ais\\10\\01\\00-00.txt");
		distFile.map(s -> s.length()).reduce((a, b) -> a + b);
		
		//TODO get rid of the time information inside the files, but keep all lines
		JavaRDD<String> cleanAIS = distFile.map(s -> processor.cleanAISMsg(s));
		
		//TODO decode the lines to AISMessages
		JavaRDD<AISMessage> decoded = cleanAIS.map(s -> processor.decodeAISMessage(s));
		decoded.persist(StorageLevel.MEMORY_ONLY());
		
		//TODO read the Messages and train a grid-like World-map
		decoded.foreach(m -> processor.trainGridMap(m)); //  NOT SURE ABOUT THIS
		
		
		//TODO find ships that have suspicious outage time
		
		//TODO check if the found ships are in a area where other ships are able to send
		//signals
		
		//TODO make a final ranking by taking the type of the ship into account
		
		
		/*
		 * Code from the local version 
		 * String path = System.getProperty("user.dir");
		
		List<InputStream> iss = null;
		try {
			iss = Files.list(Paths.get(path + "//data//06//"))
			        .filter(Files::isRegularFile)
			        .map(f -> {
			            try {
			                return new FileInputStream(f.toString());
			            } catch (Exception e) {
			                throw new RuntimeException(e);
			            }
			        }).collect(Collectors.toList());
		} catch (IOException e1) {
			
			e1.printStackTrace();
		} 

		SequenceInputStream stream = new SequenceInputStream(Collections.enumeration(iss));

		
		File dir = new File(path + "//data//06//");
		int c = 0;
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
		    	if(child.getName().startsWith("!") && (c < 240)){
		    		AisTracker.readCsv(child.getAbsolutePath());
		    		c++;
		    	}
			}
		}*/

		AisTracker.printOutages(30);
		//AisTracker.plotOnMap(180);
    }

}
