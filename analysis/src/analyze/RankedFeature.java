package analyze;

public class RankedFeature implements RankableContent{
	
	private final String featureText;
	private final double frequency;
	private final int index;
	
	public RankedFeature(int index, String featureText,double frequency){
		this.index = index;
		this.featureText = featureText;
		this.frequency = frequency;
	}
	public int getIndex(){
		return index;
	}

	@Override
	public int getRankableValue() {
		return new Double(frequency).intValue();
	}

	@Override
	public String getContent() {
		return featureText;
	}
	
	public double getFrequency(){
		return frequency;
	}
	

	public String dump(int rank) {
		return rank + ": " + featureText + " | "+frequency + "\n";
	}

	@Override
	public int compareTo(RankableContent o) {
		if(this.getClass()!= o.getClass()){
			throw new RuntimeException("cannot compare: "+this.getClass()+ " and: "+o.getClass());
		}
		RankedFeature other = (RankedFeature)o;
		if(this.frequency > other.frequency){
			return -1;
		}else if(this.frequency < other.frequency){
			return 1;
		}else{
			return this.featureText.compareTo(other.featureText);
		}
	}

}
