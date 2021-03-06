package analyze;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import analyze.exceptions.PythonParsingException;
import analyze.exceptions.QuoteRuleException;
import metric.FeatureDictionary;

@SuppressWarnings("serial")
public class Cluster extends TreeSet<WeightRankedRegex> implements
		RankableContent {
	private static int nextClusterID = 0;
	public final int thisClusterID = nextClusterID++;

	private int topN;
	private TreeSet<Integer> projectIDs;
	private FeaturePile featurePile = null;

	public Cluster(int topN) {
		super();
		this.topN = topN;
		projectIDs = new TreeSet<Integer>();
	}
	
	public String getLatex(){
		StringBuilder sb = new StringBuilder();
		sb.append("\\item ");
		sb.append("["+getDescription()+"] ");
		sb.append("");
		
		return sb.toString();
	}

	private String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	public void initialzeStats() throws ClassNotFoundException, SQLException {
		this.projectIDs = computeProjectIDs();
		this.featurePile = new FeaturePile(this);
	}

	@Override
	public boolean add(WeightRankedRegex x) {
		boolean addSuccess = super.add(x);
		return addSuccess;
	}

	@Override
	public int getRankableValue() {
		return projectIDs.size();
	}

	public TreeSet<Integer> getProjectIDs() {
		return projectIDs;
	}

	public FeaturePile getFeaturePile() {
		return featurePile;
	}

	public boolean containsAnyFeatures(int[] featureGroup) {
		FeaturePile fp = this.getFeaturePile();
		for (int featureIndex : featureGroup) {
			if (fp.containsFeatureIndex(featureIndex)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsAllFeatures(int[] featureGroup) {
		FeaturePile fp = this.getFeaturePile();

		// protect against false positives with error code
		if (featureGroup.length == 0) {
			return false;
		}
		boolean containsAll = true;
		for (int featureIndex : featureGroup) {
			containsAll = containsAll && fp.containsFeatureIndex(featureIndex);
		}
		return containsAll;
	}

	public TreeSet<Integer> computeProjectIDs() throws SQLException,
			ClassNotFoundException {
		if(this.size() > 512){
			return getProjectIDsSlowly();
		}else{
			return getProjectIDsBatch();
		}
	}

	private TreeSet<Integer> getProjectIDsBatch() throws SQLException, ClassNotFoundException {
		StringBuilder querySB = new StringBuilder();

		// wow this is crazy, but should be faster than joining after the fact
		querySB.append("select distinct uniqueSourceID as ID from RegexCitationMerged where pattern=? ");
		for (int i = 1; i < this.size(); i++) {
			querySB.append("or pattern=? ");
		}
		querySB.append(";");

		TreeSet<Integer> projectIDList = new TreeSet<Integer>();

		// prepare sql
		Connection c = null;
		Statement stmt = null;
		Class.forName("org.sqlite.JDBC");
		c = DriverManager.getConnection(PaperWriter.connectionString);
		c.setAutoCommit(false);

		PreparedStatement ps = c.prepareStatement(querySB.toString());
		int indexVal = 1;
		Iterator<WeightRankedRegex> it = this.iterator();
		while (it.hasNext()) {
			WeightRankedRegex wrr = it.next();
			ps.setString(indexVal++, wrr.getContent());
		}

		// the query needs to return a relation,
		// the first string is a key, second the pattern
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			int ID = rs.getInt("ID");
			projectIDList.add(ID);
		}

		// wind down sql
		rs.close();
		c.close();
		return projectIDList;
	}

	private TreeSet<Integer> getProjectIDsSlowly() throws ClassNotFoundException, SQLException {
		TreeSet<Integer> allIDs = new TreeSet<Integer>();
		Iterator<WeightRankedRegex> it = iterator();
		while(it.hasNext()){
			WeightRankedRegex wrr = it.next();
			ArrayList<Integer> IDs = IOUtil.getProjectIDsHavingPattern(wrr.getContent());
			allIDs.addAll(IDs);
		}
		return allIDs;
	}

	@Override
	public String getContent() {
		StringBuilder sb = new StringBuilder();
		Iterator<WeightRankedRegex> it = this.iterator();
		int i = 0;
		if (it.hasNext()) {
			WeightRankedRegex x = it.next();
			sb.append(x.getContent() + "::" + x.getRankableValue());
			i++;
		}
		while (it.hasNext() && i < topN) {
			WeightRankedRegex x = it.next();
			sb.append(",");
			sb.append(x.getContent() + "::" + x.getRankableValue());
			i++;
		}
		return sb.toString();
	}

	public String getShortest() {
		Iterator<WeightRankedRegex> it = this.iterator();
		String smallest = null;
		if (it.hasNext()) {

			// should always get here - no empty clusters
			smallest = it.next().getUnescapedPattern();
		}
		while (it.hasNext()) {
			String smaller = it.next().getUnescapedPattern();
			if (smaller.length() < smallest.length()) {
				smallest = smaller;
			}
		}
		return smallest;
	}

	public String getShortestWithFeature(int[] featureIndices) {
		Iterator<WeightRankedRegex> it = this.iterator();
		String smallest = "Cluster does not contain any of " +
			Arrays.toString(featureIndices) +
			" it does not contain them and so there cannot be any smallest feature containing at least one of them, also this string is really long so that it is very unlikely that the smallest regex containing one of those features would be larger than this one, although that is probably the most likely reason that you would see this string, as we shouldn't be calling this function without knowing ahead of time that the cluster does contain at least one of those features";
		while (it.hasNext()) {
			WeightRankedRegex wrr = it.next();
			boolean hasFeature = false;
			for(int fix : featureIndices){
				hasFeature = hasFeature || wrr.hasFeature(fix);
			}
			String pattern = wrr.getContent();
			if (hasFeature && pattern.length() < smallest.length()) {
				smallest = pattern;
			}
		}
		return smallest;
	}

	@Override
	public int compareTo(RankableContent other) {
		if (other.getClass() != this.getClass()) {
			System.err.println("class mismatch");
			return 1;
		}
		Cluster cOther = (Cluster) other;
		int nProjectsThis = this.getRankableValue();
		int nProjectsOther = cOther.getRankableValue();
		// higher weight is earlier
		if (nProjectsThis > nProjectsOther) {
			return -1;
		} else if (nProjectsThis < nProjectsOther) {
			return 1;
		} else {
			if(this.size() > cOther.size()){
				return -1;
			}else if(this.size() < cOther.size()){
				return 1;
			}else{
				Iterator<WeightRankedRegex> it1 = this.iterator();
				Iterator<WeightRankedRegex> it2 = cOther.iterator();
				while(it1.hasNext()){
					WeightRankedRegex wrr1 = it1.next();
					WeightRankedRegex wrr2 = it2.next();
					int ct = wrr1.compareTo(wrr2);
					if(ct!=0){
						return ct;
					}
				}
				return 0;
			}
		}
	}
	
	//needed to count the number of projects supported by Rex, why not here?
//	public static void main(String[] args) throws IllegalArgumentException, QuoteRuleException, PythonParsingException, ClassNotFoundException, SQLException{
//		String filtered_corpus_path = PaperWriter.homePath +
//				"csharp/filteredCorpus.txt";
//		ArrayList<WeightRankedRegex> corpus = IOUtil.importFilteredCorpus(filtered_corpus_path);
//		System.out.println("corpus size: "+corpus.size());
//		TreeSet<Integer> allProjectIDs = new TreeSet<Integer>();
//		for(WeightRankedRegex wrr : corpus){
//			allProjectIDs.addAll(IOUtil.getProjectIDsHavingPattern(wrr.getContent()));
//		}
//		System.out.println("total Projects Supported By Rex: "+allProjectIDs.size());
//
//	}
}
