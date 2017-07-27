package in.dream_lab.goffish.godb;


public class RecursivePathMaintainedTest {

	static class RecursivePathMaintained{
		Long startVertex;
		Integer startStep;
		String path;
		int direction;
		
		public int hashCode(){
			return (int)(startStep+direction+startVertex + path.hashCode());
		}
		
		public boolean equals(Object obj){
			RecursivePathMaintained other=(RecursivePathMaintained)obj;
			return (this.direction==other.direction && this.startStep.intValue()==other.startStep.intValue() && this.startVertex.longValue()==other.startVertex.longValue() && this.path.equals(other.path));
		}
		
		
		public RecursivePathMaintained(Long _startVertex,Integer _startStep, String _path, int _direction){
			this.startVertex=_startVertex;
			this.startStep=_startStep;
			this.path=_path;
			this.direction = _direction;
		}
	}
	
	static class OutputPathKey{
		long queryID;
		int step;
		String direction;
		long startVertex;
		
		public int hashCode() {
			return (int) (queryID + step + startVertex);
		}
		
		public boolean equals(Object obj){
			OutputPathKey other=(OutputPathKey)obj;
			return (this.queryID==other.queryID && this.step==other.step && this.direction.equals(direction) && this.startVertex==other.startVertex);
		}
		
		public OutputPathKey(long _queryID,int _step,String _direction,long _startVertex){
			this.queryID=_queryID;
			this.step=_step;
			this.direction=_direction;
			this.startVertex=_startVertex;
			
		}
		
	}
	
	public static void main(String[] args){
		RecursivePathMaintained r1= new RecursivePathMaintained(124123l, 2, "a->b->c", 0);
		RecursivePathMaintained r2= new RecursivePathMaintained(124123l, 2, "a->b->c", 0);
		System.out.println(r1.equals(r2));
		
		OutputPathKey o1=new OutputPathKey(1, 2, "false", 12321l);
		OutputPathKey o2=new OutputPathKey(1, 2, "false", 12321l);
		System.out.println(o1.equals(o2));
	}
}
