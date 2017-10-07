package in.dream_lab.goffish.godb;
import org.apache.cassandra.thrift.Cassandra.AsyncProcessor.system_add_column_family;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class IndexMounter {

	static //configuration object takes care of absolute directory
	String IndexHDFSDirectory="abhilash/index";
    
    
    
	
    static String NAMENODE_URI="hdfs://orion-00:19000/";
	static void mountIndexToMemory()
	{
		
		try
		{
		Configuration conf = new Configuration();
		conf.set("fs.defaultFS", NAMENODE_URI);
		FileSystem hdfsFileSystem = FileSystem.get(conf);
		
		hdfsFileSystem.copyToLocalFile(new Path(IndexHDFSDirectory), new Path("/tmp"));
		
		}
		catch(Exception e){
			System.out.println("Exception while Mounting:" + e.getMessage());
		}
		
	}
	
	public static void main(String[] args){
//		System.out.println("Test");
//		mountIndexToMemory();
		String[] str={"123","345","457"};
		System.out.println(str.toString());
	}
}
