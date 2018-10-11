package in.dream_lab.goffish.godb;




import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.Iterator;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;







public class CreatingHeuristics{ 
        
        
        
public static final Log LOG = LogFactory.getLog(CreatingHeuristics.class);
        

        
public static void main(String[] args) throws IOException{

	String inputFile=args[0];
	long numVertices=Long.valueOf(args[1]);
	long numEdges=Long.valueOf(args[2]);
	long numRemoteVertices=Long.valueOf(args[3]);
	String outputFileName=args[4];
	Hueristics hueristics=new Hueristics();
	HashMap<String,String> propertyMapping = new HashMap<String,String>();
	String heuristicsBasePath="/scratch/abhilash/";
 	BufferedReader br = null;
	FileReader fr = null;
 	FileOutputStream fos;
	ObjectOutputStream oos;
	//"employer", "school"," major","places_lived"
	propertyMapping.put("0","vid");
	propertyMapping.put("1","language");
	propertyMapping.put("2","industry");
	propertyMapping.put("3","contr");
	propertyMapping.put("4","ispublic");
	propertyMapping.put("5","follow");
	
	
	try {
		
		hueristics.numVertices=(double) numVertices;
		hueristics.numEdges=(double) numEdges;
		hueristics.numRemoteVertices=(double) numRemoteVertices;
		fr = new FileReader(inputFile);
		br = new BufferedReader(fr);
		
		String line;

		while ((line = br.readLine()) != null) {
//			System.out.println(line);
			String[] data=line.split("\\s+");
			String[] propData=data[0].split(",");
			String propVal="";
			if(propData.length>1) {
				propVal=propData[1];
			}
			String propName=propertyMapping.get(propData[0]);
			if(propName.equals("vid")) {
				continue;
			}
			String value=propVal;
			String[] statsData=data[1].split(",");
			long vertexCount=Long.valueOf(statsData[0]);
			long localOutEdgeCount=Long.valueOf(statsData[1]);
			long remoteOutEdgeCount=Long.valueOf(statsData[2]);
			long localInEdgeCount=Long.valueOf(statsData[3]);
			long remoteInEdgeCount=Long.valueOf(statsData[4]);
			vertexPredicateStats vertexStats=new vertexPredicateStats();
			vertexStats.numOutDegree=(double) localOutEdgeCount;
			vertexStats.numInDegree=(double) localInEdgeCount;
			vertexStats.numRemoteOutDegree=(double) remoteOutEdgeCount;
			vertexStats.numRemoteInDegree=(double) remoteInEdgeCount;
			vertexStats.numberMatchingPredicate=(double) vertexCount;
			vertexStats.avgOutDegree=((double)localOutEdgeCount)/vertexCount;
			vertexStats.avgInDegree=((double)localInEdgeCount)/vertexCount;
			vertexStats.avgRemoteOutDegree=((double)remoteOutEdgeCount)/vertexCount;
			vertexStats.avgRemoteInDegree=((double)remoteInEdgeCount)/vertexCount;
			vertexStats.probability=((double)vertexCount)/numVertices;

			HashMap<String, vertexPredicateStats> valueMap=hueristics.vertexPredicateMap.get(propName);
			if(valueMap==null) {
				valueMap=new HashMap<String,vertexPredicateStats>();
				hueristics.vertexPredicateMap.put(propName,valueMap);
			}
			
			valueMap.put(value, vertexStats);
			
			
		}

	} catch (IOException e) {

		e.printStackTrace();

	}
	
	System.out.println("Writing heuristic object:" + heuristicsBasePath+String.valueOf(outputFileName)+".ser");
	//store in file
	File file = new File(heuristicsBasePath+String.valueOf(outputFileName)+".ser");
	if ( !file.exists() ) {
		
		file.getParentFile().mkdirs();
		file.createNewFile();
		
	}
	fos = new FileOutputStream(file); 
	oos = new ObjectOutputStream(fos);
	oos.writeObject(hueristics);
	oos.close();
 
	System.out.println("Complete");
}
       








        
        
        
        
        
        
        
    
        
        
        






       




        
        
    
    
}

