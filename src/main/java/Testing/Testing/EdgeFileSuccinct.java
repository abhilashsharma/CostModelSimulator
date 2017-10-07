package Testing.Testing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class EdgeFileSuccinct {

	
	public static void main(String[] args) throws NumberFormatException, IOException {
		
		
		 FileReader fr = new FileReader(args[0]);//inputFile
		 String OutputDir=args[1];
         BufferedReader br = new BufferedReader(fr);

         String sCurrentLine;
         PrintWriter writer0 = new PrintWriter(OutputDir+"/part-0", "UTF-8");
         PrintWriter writer1 = new PrintWriter(OutputDir+"/part-1", "UTF-8");
         PrintWriter writer2 = new PrintWriter(OutputDir+"/part-2", "UTF-8");
         PrintWriter writer3 = new PrintWriter(OutputDir+"/part-3", "UTF-8");
         PrintWriter writer4 = new PrintWriter(OutputDir+"/part-4", "UTF-8");
         PrintWriter writer5 = new PrintWriter(OutputDir+"/part-5", "UTF-8");
         PrintWriter writer6 = new PrintWriter(OutputDir+"/part-6", "UTF-8");
         PrintWriter writer7 = new PrintWriter(OutputDir+"/part-7", "UTF-8");
//         PrintWriter writer8 = new PrintWriter(OutputDir+"/part-8", "UTF-8");
//         PrintWriter writer9 = new PrintWriter(OutputDir+"/part-9", "UTF-8");
//         PrintWriter writer10 = new PrintWriter(OutputDir+"/part-10", "UTF-8");
//         PrintWriter writer11 = new PrintWriter(OutputDir+"/part-11", "UTF-8");
         

         while ((sCurrentLine = br.readLine()) != null) {
             
      
		
		String str=sCurrentLine;
		String[] data = str.split("\\s+");
		String srcid=data[0];
		String blockid=data[1];
		String srcPid=data[2];
		ArrayList<String> localSinkList= new ArrayList<String>();
		ArrayList<String> remoteSinkList= new ArrayList<String>();
		for(int i=3;i<data.length;i=i+3) {
			if(data[i+2].equals(srcPid)) {
				localSinkList.add(data[i]);
			}
			else {
				remoteSinkList.add(data[i]);
			}
		}
	
		
		StringBuilder vertexAdj= new StringBuilder("");
		vertexAdj.append(blockid).append("#").append(srcid).append("@").append(localSinkList.size()).append("%");
		if(localSinkList.size()>0) {
			
			for(String localSink:localSinkList) {
				vertexAdj.append(localSink).append(":");
			}
			
		}
		
		if(remoteSinkList.size()>0) {
			for(String remoteSink:remoteSinkList) {
				vertexAdj.append(remoteSink).append(":");
			}
		}
		
		
		if(remoteSinkList.size()>0 || localSinkList.size()>0) {
			vertexAdj.setCharAt(vertexAdj.length()-1, '|');
			
		}else {
			vertexAdj.append("|");
		}

		int pid=Integer.parseInt(srcPid);
		switch(pid) {
		case 0:
			writer0.println(vertexAdj.toString());
			break;
		case 1:
			writer1.println(vertexAdj.toString());
			break;
		case 2:
			writer2.println(vertexAdj.toString());
			break;
		case 3:
			writer3.println(vertexAdj.toString());
			break;
		case 4:
			writer4.println(vertexAdj.toString());
			break;
		case 5:
			writer5.println(vertexAdj.toString());
			break;
		case 6:
			writer6.println(vertexAdj.toString());
			break;
		case 7:
			writer7.println(vertexAdj.toString());
			break;
//		case 8:
//			writer8.println(vertexAdj.toString());
//			break;
//		case 9:
//			writer9.println(vertexAdj.toString());
//			break;
//		case 10:
//			writer10.println(vertexAdj.toString());
//			break;
//		case 11:
//			writer11.println(vertexAdj.toString());
//			break;			
		default:
			System.out.println("PID not in range exception");
				break;
		}
		
//		context.write(new Text(srcPid), new Text(vertexAdj.toString()));
         }//while loop ends
         
         writer0.flush();
         writer0.close();
         writer1.flush();
         writer1.close();
         writer2.flush();
         writer2.close();
         writer3.flush();
         writer3.close();
         
         writer4.flush();
         writer4.close();
         writer5.flush();
         writer5.close();
         writer6.flush();
         writer6.close();
         writer7.flush();
         writer7.close();
         
//         writer8.flush();
//         writer8.close();
//         writer9.flush();
//         writer9.close();
//         writer10.flush();
//         writer10.close();
//         writer11.flush();
//         writer11.close();
	}
}
