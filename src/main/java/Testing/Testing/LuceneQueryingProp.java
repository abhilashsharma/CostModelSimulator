package Testing.Testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
import org.apache.lucene.util.Version;


public class LuceneQueryingProp {
	
	//Required for lucene 
		static File vertexIndexDir;
		static Directory vertexDirectory;
		static Analyzer analyzer;
		static IndexReader indexReader;
		static IndexSearcher indexSearcher;
		static BooleanQuery query;
		static ScoreDoc[] hits;
		static boolean initDone = false ;
		static boolean queryMade = false;
		private static final Object initLock = new Object();
		private static final Object queryLock = new Object();
		private static boolean queryStart=false;//later lock this when multithreaded
	    private static boolean queryEnd=false;//later lock this when multithreaded
	    private static boolean gcCalled=false;

	    /**
		 * Initialize lucene
		 * 
		 */
		private static void initLucene(long pseudoPid) throws InterruptedException, IOException{
			
			{
//			  long pseudoPid=getSubgraph().getSubgraphId().get() >> 32;
				initDone = true;
				vertexIndexDir = new File( "/home/abhilash/abhilash/index/Partition" + pseudoPid + "/vertexIndex");
				vertexDirectory = FSDirectory.open(vertexIndexDir);
				analyzer = new StandardAnalyzer(Version.LATEST);
				indexReader  = DirectoryReader.open(vertexDirectory);
				indexSearcher = new IndexSearcher(indexReader);
			}
			
		}
	
		
		private static void initLucene(long pseudoPid,String baseDir) throws InterruptedException, IOException{
			
			{
//			  long pseudoPid=getSubgraph().getSubgraphId().get() >> 32;
				initDone = true;
				vertexIndexDir = new File( baseDir+"Partition" + pseudoPid + "/vertexIndex");
				vertexDirectory = FSDirectory.open(vertexIndexDir);
				analyzer = new StandardAnalyzer(Version.LATEST);
				indexReader  = DirectoryReader.open(vertexDirectory);
				indexSearcher = new IndexSearcher(indexReader);
			}
			
		}
		/**
		 * 
		 * uses index to query vertices/Edges that contain a particular property with specified value
		 */
		private static void makeQuery(String prop,Object val) throws IOException{
			{
				queryMade = true;
				if(val.getClass()==String.class){
				query  = new BooleanQuery();
				query.add(new TermQuery(new Term(prop, (String)val)), BooleanClause.Occur.MUST);
				hits =  indexSearcher.search(query,40000).scoreDocs;
				}
				else if(val.getClass()==Integer.class)
				{
					Query q = NumericRangeQuery.newIntRange(prop,(Integer)val, (Integer)val, true, true);
					hits =  indexSearcher.search(q,40000).scoreDocs;
				}
				
			}
		}
		
		public static void clear() {
			hits=null;
		}
	public static void main(String[] args) throws InterruptedException, IOException {
		
		
		int partid=Integer.parseInt(args[2]);
		initLucene(partid,args[3]);
		String propertyName = args[0];
		long totalHits=0;
		try  {
				String q=args[1];
		    	makeQuery(propertyName, q.toString());
		    	System.out.println(q+","+ hits.length);
		    	clear();
		    
		   System.out.println("Number of Vertices:" + totalHits);
		}catch(Exception e) {
			
		}
//		
//		initLucene(1l);
////		String fileName=args[0];
////		String propertyName = args[1];
//		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
//		    String line;
//		    while ((line = br.readLine()) != null) {
//		    	long start=System.currentTimeMillis();
//		    	makeQuery(propertyName, line.trim());
//		    	System.out.println(line.trim()+","+ hits.length + "," + (System.currentTimeMillis()-start));
//		    	clear();
//		    }
//		}catch(Exception e) {
//			
//		}
//
//		initLucene(2l);
////		String fileName=args[0];
////		String propertyName = args[1];
//		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
//		    String line;
//		    while ((line = br.readLine()) != null) {
//		    	long start=System.currentTimeMillis();
//		    	makeQuery(propertyName, line.trim());
//		    	System.out.println(line.trim()+","+ hits.length + "," + (System.currentTimeMillis()-start));
//		    	clear();
//		    }
//		}catch(Exception e) {
//			
//		}
//		
//		initLucene(3l);
////		String fileName=args[0];
////		String propertyName = args[1];
//		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
//		    String line;
//		    while ((line = br.readLine()) != null) {
//		    	long start=System.currentTimeMillis();
//		    	makeQuery(propertyName, line.trim());
//		    	System.out.println(line.trim()+","+ hits.length + "," + (System.currentTimeMillis()-start));
//		    	clear();
//		    }
//		}catch(Exception e) {
//			
//		}
		
		
	}
}
