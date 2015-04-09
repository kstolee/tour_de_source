package analyze;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
// import org.rosuda.JRI.Rengine;
// import org.rosuda.JRI.REXP;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import metric.AlienFeatureException;
import metric.FeatureCount;

import org.apache.commons.io.FileUtils;
// c.execute('''CREATE TABLE RegexCitation (uniqueSourceID int, sourceJSON text,
// fileHash char(44), filePath text, pattern text, flags int, regexFunction
// int)''')
// c.execute('''CREATE TABLE FilesPerProject (nFiles int, frequency int)''')
import org.python.util.PythonInterpreter;

import pcre.PCRE;

public class PaperWriter {

	private static LinkedList<RegexCite> patternList = new LinkedList<RegexCite>();

	private static String N_PROJ_SCANNED = "nScanned";
	private static String N_PROJ_HAS_REGEX = "nProjectsWithRegex";
	private static String N_PYTHON_FILES = "nPythonFiles";
	private static String N_FILES_HAS_REGEX = "nFilesWithRegex";
	private static String FUNC_0 = "FUNC_0";
	private static String FUNC_1 = "FUNC_1";
	private static String FUNC_2 = "FUNC_2";
	private static String FUNC_3 = "FUNC_3";
	private static String FUNC_4 = "FUNC_4";
	private static String FUNC_5 = "FUNC_5";
	private static String FUNC_6 = "FUNC_6";
	private static String FUNC_7 = "FUNC_7";

	private static String FLAGS_0 = "FLAGS_0";
	private static String FLAGS_2 = "FLAGS_2";
	private static String FLAGS_4 = "FLAGS_4";
	private static String FLAGS_8 = "FLAGS_8";
	private static String FLAGS_16 = "FLAGS_16";
	private static String FLAGS_32 = "FLAGS_32";
	private static String FLAGS_64 = "FLAGS_64";
	private static String FLAGS_128 = "FLAGS_128";
	private static String FLAGS_255 = "FLAGS_128";

	private static String HAS_FLAGS = "HAS_FLAGS";
	private static String CANNOT_PARSE = "CANNOT_PARSE";
	private static String CAN_PARSE = "CAN_PARSE";
	
	private static String HAS_ALIEN = "HAS_ALIEN";
	private static String DISTINCT_PATTERN = "DISTINCT_PATTERN";
	private static String N_RC = "N_RC";

	public static void main(String[] args) throws ClassNotFoundException,
			SQLException, ValueMissingException, IOException,
			InterruptedException {
		System.out.println("begin paper writer");

		// initialize
		String homePath = "/Users/carlchapman/Documents/SoftwareProjects/tour_de_source/";
		String connectionString = "jdbc:sqlite:" + homePath +
			"tools/merged/merged_report.db";

		// populate the map with keys for the Latex database
		HashMap<String, Integer> databaseFileContent = new HashMap<String, Integer>();
		populateTexDatabase(databaseFileContent, connectionString);

		// create the R script content for image creation
		String rScriptContent = createImagesScript(connectionString, homePath, databaseFileContent);

		// createContent
		generateArtifacts(stringifyMap(databaseFileContent), rScriptContent, homePath);
		System.out.println("finished paper writer");
	}

	private static void generateArtifacts(String databaseFileContent,
			String rScriptContent, String homePath) throws IOException,
			InterruptedException {

		//
		// create the output file objects
		String scriptName = "analysis_script.r";
		String databaseName = "database.csv";
		String outputPath = homePath + "analysis/analysis_output/";
		File dbFile = new File(outputPath + databaseName);
		File rFile = new File(outputPath + scriptName);
		FileUtils.cleanDirectory(new File(outputPath));

		IOUtil.createAndWrite(dbFile, databaseFileContent);
		IOUtil.createAndWrite(rFile, rScriptContent);

		// run the R script
		Process p = Runtime.getRuntime().exec("/usr/local/bin/R CMD BATCH " +
			outputPath + scriptName);

		// add + " /dev/null" to the end to silence output
		p.waitFor();
	}

	//note this depends heavily on databaseFileContent containing various values
	private static String createImagesScript(String connectionString,
			String homePath, HashMap<String, Integer> databaseFileContent)
			throws ClassNotFoundException, SQLException {
		// mostly following:
		// http://stackoverflow.com/questions/5142842
		StringBuilder rScriptContent = new StringBuilder();
		rScriptContent.append("\n");

		// 0.P1 V1(total number of projects scanned) and V2 (number of projects
		// with at least one regex usage found):
		rScriptContent.append("setEPS()\n");
		rScriptContent.append("options(scipen=10)\n");
		rScriptContent.append("postscript(\"" + homePath +
			"analysis/analysis_output/percentProjHasRegex.eps\")\n");
		int nProjects = databaseFileContent.get(N_PROJ_SCANNED);
		int nProjectsHasRegex = databaseFileContent.get(N_PROJ_HAS_REGEX);
		rScriptContent.append("P0_1 = matrix(c(" +
			(nProjects - nProjectsHasRegex) + "," + nProjectsHasRegex +
			"),ncol=1,byrow=T)\n");
		rScriptContent.append("rownames(P0_1)=c(\"Projects Without Regex " +
			(nProjects - nProjectsHasRegex) + "\",\"Projects With Regex " +
			nProjectsHasRegex + "\")\n");
		rScriptContent.append("par(pin=c(4,2))\n");
		rScriptContent.append("barplot(P0_1, main=\"How Many Projects Use Regex?\",legend=rownames(P0_1),col=c(\"rosybrown1\",\"red\"),xlim=c(0,9),width=0.6,las=1)\n");
		rScriptContent.append("dev.off()\n");

		// 0.P2 V3 (total number of Python files scanned) and V4 (total number
		// of scanned Python files with at least one regex usage)
		rScriptContent.append("setEPS()\n");
		rScriptContent.append("postscript(\"" + homePath +
			"analysis/analysis_output/percentFileHasRegex.eps\")\n");
		int nPythonFiles = databaseFileContent.get(N_PYTHON_FILES);
		int nFilesWithRegex = databaseFileContent.get(N_FILES_HAS_REGEX);
		rScriptContent.append("P0_2 = matrix(c(" +
			(nPythonFiles - nFilesWithRegex) + "," + nFilesWithRegex +
			"),ncol=1,byrow=T)\n");
		rScriptContent.append("rownames(P0_2)=c(\"Files Without Regex " +
			(nPythonFiles - nFilesWithRegex) + "\",\"Files With Regex " +
			nFilesWithRegex + "\")\n");
		rScriptContent.append("par(pin=c(3.5,2))\n");
		rScriptContent.append("barplot(P0_2, main=\"How Many Files Use Regex?\",legend=rownames(P0_2),col=c(\"slategrey\",\"blue\"),xlim=c(0,9),width=0.6,las=1)\n");
		rScriptContent.append("dev.off()\n");

		// 0.H1 histogram characterizing log of # of unique python files in a
		// project's history
		rScriptContent.append(getFilesPerProjectVector(connectionString));
		rScriptContent.append("setEPS()\n");
		rScriptContent.append("postscript(\"" + homePath +
			"analysis/analysis_output/filesPerProject.eps\")\n");
		rScriptContent.append("boxplot(filesPerProjectVector, ylab =\"ln(nFilesPerProject)\")\n");
		rScriptContent.append("dev.off()\n");

		// 0.H2 histogram characterizing number of unique files in a project to
		// have at least one regex usage
		rScriptContent.append(getRegexFilesPerProjectVector(connectionString));
		rScriptContent.append("setEPS()\n");
		rScriptContent.append("postscript(\"" + homePath +
			"analysis/analysis_output/regexFilesPerProject.eps\")\n");
		rScriptContent.append("boxplot(regexFilesPerProjectVector, ylab =\"ln(nRegexFilesPerProject)\")\n");
		rScriptContent.append("dev.off()\n");

		// 0.H3 histogram characterizing number of regex usages per file
		rScriptContent.append(getRegexPerFileVector(connectionString));
		rScriptContent.append("setEPS()\n");
		rScriptContent.append("postscript(\"" + homePath +
			"analysis/analysis_output/regexPerFile.eps\")\n");
		rScriptContent.append("boxplot(regexPerFileVector, ylab =\"ln(nRegexPerFile)\")\n");
		rScriptContent.append("dev.off()\n");

		// 0.P3 how regexFunction partitions observed usages
		rScriptContent.append("setEPS()\n");
		rScriptContent.append("postscript(\"" + homePath +
			"analysis/analysis_output/percentFunctions.eps\")\n");
		int f0 = databaseFileContent.get(FUNC_0);
		int f1 = databaseFileContent.get(FUNC_1);
		int f2 = databaseFileContent.get(FUNC_2);
		int f3 = databaseFileContent.get(FUNC_3);
		int f4 = databaseFileContent.get(FUNC_4);
		int f5 = databaseFileContent.get(FUNC_5);
		int f6 = databaseFileContent.get(FUNC_6);
		int f7 = databaseFileContent.get(FUNC_7);
		rScriptContent.append("P0_3 = matrix(c(" + f0 + "," + f1 + "," + f2 +
			"," + f3 + "," + f4 + "," + f5 + "," + f6 + "," + f7 +
			"),ncol=1,byrow=T)\n");
		rScriptContent.append("rownames(P0_3)=c(\"re.compile " + f0 +
			"\",\"re.search " + f1 + "\",\"re.match " + f2 + "\",\"re.split " +
			f3 + "\",\"re.findall " + f4 + "\",\"re.finditer " + f5 +
			"\",\"re.sub " + f6 + "\",\"re.subn " + f7 + "\")\n");
		rScriptContent.append("par(pin=c(3.5,2))\n");
		rScriptContent.append("barplot(P0_3, main=\"re module Function Use\",legend=rownames(P0_3),col=c(\"limegreen\",\"chocolate\",\"blue2\",\"darkorchid3\",\"coral1\",\"darkgrey\",\"azure1\",\"burlywood1\"),xlim=c(0,9),width=0.6,las=1)\n");
		rScriptContent.append("dev.off()\n");

		// 0.P4 observed valid flags
		rScriptContent.append("setEPS()\n");
		rScriptContent.append("postscript(\"" + homePath +
			"analysis/analysis_output/percentFlags.eps\")\n");
		int flag0 = databaseFileContent.get(FLAGS_0);
		int flag2 = databaseFileContent.get(FLAGS_2);
		int flag4 = databaseFileContent.get(FLAGS_4);
		int flag8 = databaseFileContent.get(FLAGS_8);
		int flag16 = databaseFileContent.get(FLAGS_16);
		int flag32 = databaseFileContent.get(FLAGS_32);
		int flag64 = databaseFileContent.get(FLAGS_64);
		int flag128 = databaseFileContent.get(FLAGS_128);
		int flag255 = databaseFileContent.get(FLAGS_255);
		rScriptContent.append("P0_4 = matrix(c(" + flag2 + "," + flag4 + "," +
			flag8 + "," + flag16 + "," + flag32 + "," + flag64 + "," + flag128 +
			"," + flag255 + "),ncol=1,byrow=T)\n");
		rScriptContent.append("rownames(P0_4)=c(\"IGNORECASE " + flag2 +
			"\",\"LOCALE " + flag4 + "\",\"MULTILINE " + flag8 +
			"\",\"DOTALL " + flag16 + "\",\"UNICODE " + flag32 +
			"\",\"VERBOSE " + flag64 + "\",\"DEBUG " + flag128 +
			"\",\"(combo) " + flag255 + "\")\n");
		rScriptContent.append("par(pin=c(3.5,2))\n");
		rScriptContent.append("barplot(P0_4, main=\"non-default flag usage (DEFAULT: " +
			flag0 +
			")\",legend=rownames(P0_4),col=c(\"steelblue2\",\"lightcoral\",\"papayawhip\",\"thistle2\",\"grey66\",\"firebrick3\",\"deepskyblue4\",\"green3\"),xlim=c(0,9),width=0.6,las=1)\n");
		rScriptContent.append("dev.off()\n");

		// 0.P5 how the corpus is filtered
		rScriptContent.append("setEPS()\n");
		rScriptContent.append("postscript(\"" + homePath +
			"analysis/analysis_output/percentCorpusOrigin.eps\")\n");
		int nHasFlags = databaseFileContent.get(HAS_FLAGS);
		int nCannotParse = databaseFileContent.get(CANNOT_PARSE);
		int nCanParse = databaseFileContent.get(CAN_PARSE);
		rScriptContent.append("P0_5 = matrix(c(" + nHasFlags + "," +
			nCannotParse + "," + nCanParse + "),ncol=1,byrow=T)\n");
		rScriptContent.append("rownames(P0_5)=c(\"has flags " + nHasFlags +
			"\",\"cannot parse " + nCannotParse + "\",\"usable pattern " +
			nCanParse + "\")\n");
		rScriptContent.append("par(pin=c(3.5,2))\n");
		rScriptContent.append("barplot(P0_5, main=\"Corpus Origin\",legend=rownames(P0_5),col=c(\"palegreen1\",\"paleturquoise3\",\"cyan\"),xlim=c(0,9),width=0.6,las=1)\n");
		rScriptContent.append("dev.off()\n");

		
		int nDistinctPatterns = databaseFileContent.get(DISTINCT_PATTERN);
		int nHasAlien = databaseFileContent.get(HAS_ALIEN);
		int nRC = databaseFileContent.get(N_RC);
		
		
		// 1.P1 pie chart of the above three ints
		rScriptContent.append("setEPS()\n");
		rScriptContent.append("postscript(\"" + homePath +
			"analysis/analysis_output/percentPatternAlien.eps\")\n");
		rScriptContent.append("P1_1 = matrix(c(" + nHasAlien + "," +
			(nDistinctPatterns-nHasAlien-nRC) + "," + nRC + "),ncol=1,byrow=T)\n");
		rScriptContent.append("rownames(P1_1)=c(\"alien feature " + nHasAlien +
			"\",\"error " + (nDistinctPatterns-nHasAlien-nRC) + "\",\"pattern corpus " +
			nRC + "\")\n");
		rScriptContent.append("par(pin=c(3.5,2))\n");
		rScriptContent.append("barplot(P1_1, main=\"Preprocessing Distinct Patterns\",legend=rownames(P1_1),col=c(\"mediumblue\",\"lightskyblue1\",\"seagreen2\"),xlim=c(0,9),width=0.6,las=1)\n");
		rScriptContent.append("dev.off()\n");
		
		rScriptContent.append("\n");
		return rScriptContent.toString();
	}

	private static String getFilesPerProjectVector(String connectionString)
			throws ClassNotFoundException, SQLException {
		// first we get the db data into memory
		HashMap<Integer, Integer> filesPerProjectMap = new HashMap<Integer, Integer>();

		// prepare sql
		Connection c = null;
		Statement stmt = null;
		Class.forName("org.sqlite.JDBC");
		c = DriverManager.getConnection(connectionString);
		c.setAutoCommit(false);
		stmt = c.createStatement();

		// turn all the non-negative keys into pairs for R
		ResultSet rs = stmt.executeQuery("Select nFiles, frequency FROM FilesPerProjectMerged WHERE nFiles >= 0;");
		while (rs.next()) {
			filesPerProjectMap.put(rs.getInt("nFiles"), rs.getInt("frequency"));
		}

		// wind down sql
		rs.close();
		stmt.close();
		c.close();

		// now take your time looping through the data
		boolean firstNumber = true;
		StringBuilder sb = new StringBuilder();
		sb.append("filesPerProjectVector <- c(");
		for (Entry<Integer, Integer> entry : filesPerProjectMap.entrySet()) {
			int nFiles = entry.getKey();
			int frequency = entry.getValue();
			// maybe there is a better way - this feels crazy,
			// but I am new to R - this should create a boxplot
			for (int i = 0; i < frequency; i++) {
				int logNfiles = nFiles == 0 ? 0
						: (int) Math.ceil(Math.log(nFiles));
				if (firstNumber) {
					sb.append("" + logNfiles);
					firstNumber = false;
				} else {
					sb.append(", " + logNfiles);
				}
			}
		}
		sb.append(")\n");
		System.out.println(sb.toString());
		return sb.toString();
	}

	private static String getRegexFilesPerProjectVector(String connectionString)
			throws ClassNotFoundException, SQLException {
		// first we get the db data into memory
		List<Integer> nFilesList = new LinkedList<Integer>();

		// prepare sql
		Connection c = null;
		Statement stmt = null;
		Class.forName("org.sqlite.JDBC");
		c = DriverManager.getConnection(connectionString);
		c.setAutoCommit(false);
		stmt = c.createStatement();

		// turn all the non-negative keys into pairs for R
		ResultSet rs = stmt.executeQuery("select distinct uniqueSourceID, filePath, count(distinct filePath) as ct from RegexCitationMerged group by uniqueSourceID;");
		while (rs.next()) {
			nFilesList.add(rs.getInt("ct"));
		}

		// wind down sql
		rs.close();
		stmt.close();
		c.close();

		// now take your time looping through the data
		boolean firstNumber = true;
		StringBuilder sb = new StringBuilder();
		sb.append("regexFilesPerProjectVector <- c(");
		for (Integer ct : nFilesList) {
			int logNfiles = ct == 0 ? 0 : (int) Math.ceil(Math.log(ct));
			if (firstNumber) {
				sb.append("" + logNfiles);
				firstNumber = false;
			} else {
				sb.append(", " + logNfiles);
			}
		}
		sb.append(")\n");
		System.out.println(sb.toString());
		return sb.toString();
	}

	private static String getRegexPerFileVector(String connectionString)
			throws ClassNotFoundException, SQLException {
		// first we get the db data into memory
		List<Integer> nRegexPerFileList = new LinkedList<Integer>();

		// prepare sql
		Connection c = null;
		Statement stmt = null;
		Class.forName("org.sqlite.JDBC");
		c = DriverManager.getConnection(connectionString);
		c.setAutoCommit(false);
		stmt = c.createStatement();

		// turn all the non-negative keys into pairs for R
		ResultSet rs = stmt.executeQuery("select count(filePath) as ct from RegexCitationMerged group by uniqueSourceID, filePath;");
		while (rs.next()) {
			nRegexPerFileList.add(rs.getInt("ct"));
		}

		// wind down sql
		rs.close();
		stmt.close();
		c.close();

		// now take your time looping through the data
		boolean firstNumber = true;
		StringBuilder sb = new StringBuilder();
		sb.append("regexPerFileVector <- c(");
		for (Integer ct : nRegexPerFileList) {
			int logNfiles = ct == 0 ? 0 : (int) Math.ceil(Math.log(ct));
			if (firstNumber) {
				sb.append("" + logNfiles);
				firstNumber = false;
			} else {
				sb.append(", " + logNfiles);
			}
		}
		sb.append(")\n");
		System.out.println(sb.toString());
		return sb.toString();
	}

	private static void populateTexDatabase(
			HashMap<String, Integer> databaseFileContent,
			String connectionString) throws ClassNotFoundException,
			SQLException, ValueMissingException {
		//
		// first get the total number of projects observed
		String valueName = "nObserved";
		String query = "SELECT frequency as " + valueName +
			" FROM FilesPerProjectMerged WHERE nFiles = -1;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// get the total number of observed projects that could not be
		// classified as Python or not
		valueName = "nSkipped";
		query = "SELECT frequency as " + valueName +
			" FROM FilesPerProjectMerged WHERE nFiles = -2;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// get the total number of Python Projects that the scraper tried to
		// clone and scrape but failed on
		valueName = "nAborted";
		query = "SELECT frequency as " + valueName +
			" FROM FilesPerProjectMerged WHERE nFiles = -3;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// get the total number of projects scanned successfully
		valueName = N_PROJ_SCANNED;
		query = "SELECT SUM(frequency) as " +
			valueName +
			" FROM FilesPerProjectMerged WHERE nFiles != -1 AND nFiles != -2 AND nFiles != -3;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// get the total number projects where at least one Regex was found
		valueName = N_PROJ_HAS_REGEX;
		query = "SELECT COUNT(*) as " +
			valueName +
			" FROM (SELECT uniqueSourceID FROM RegexCitationMerged GROUP BY uniqueSourceID);";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// get the total number of python files scanned, regex or no
		valueName = N_PYTHON_FILES;
		query = "SELECT SUM(product) AS " +
			valueName +
			" FROM (SELECT nFiles, frequency, (nFiles * frequency) AS product FROM FilesPerProjectMerged);";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// get the total number python files with at least one regex usage found
		valueName = N_FILES_HAS_REGEX;
		query = "SELECT COUNT(*) as " + valueName +
			" FROM (SELECT DISTINCT uniqueSourceID, filePath FROM RegexCitationMerged);";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V5 total number of unique re.* usages observed
		valueName = "total_re_usesObserved";
		query = "SELECT COUNT(*) as " + valueName +
			" FROM RegexCitationMerged;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		// note: the regexFunction mapping uses indices from this array:
		// ["re.compile", "re.search", "re.match", "re.split", "re.findall",
		// "re.finditer", "re.sub", "re.subn"]

		//
		// 0.V6.0 - count of usages where regexFunction=0
		valueName = FUNC_0;
		query = "select count(*) as " + valueName +
			" from RegexCitationMerged where regexFunction=0;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V6.1 - count of usages where regexFunction=1
		valueName = FUNC_1;
		query = "select count(*) as " + valueName +
			" from RegexCitationMerged where regexFunction=1;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V6.2 - count of usages where regexFunction=2
		valueName = FUNC_2;
		query = "select count(*) as " + valueName +
			" from RegexCitationMerged where regexFunction=2;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V6.3 - count of usages where regexFunction=3
		valueName = FUNC_3;
		query = "select count(*) as " + valueName +
			" from RegexCitationMerged where regexFunction=3;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V6.4 - count of usages where regexFunction=4
		valueName = FUNC_4;
		query = "select count(*) as " + valueName +
			" from RegexCitationMerged where regexFunction=4;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V6.5 - count of usages where regexFunction=5
		valueName = FUNC_5;
		query = "select count(*) as " + valueName +
			" from RegexCitationMerged where regexFunction=5;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V6.6 - count of usages where regexFunction=6
		valueName = FUNC_6;
		query = "select count(*) as " + valueName +
			" from RegexCitationMerged where regexFunction=6;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V6.7 - count of usages where regexFunction=7
		valueName = FUNC_7;
		query = "select count(*) as " + valueName +
			" from RegexCitationMerged where regexFunction=7;";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V8.0 - count of usages where flags=0 or arg#, which will default to
		// 0
		valueName = FLAGS_0;
		query = "select count(*) as " + valueName +
			" from RegexCitationMerged where flags=0 or flags like 'arg%';";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V8.128 - count of usages where flags=128
		valueName = FLAGS_128;
		query = "select count(*) as " + valueName +
			" from RegexCitationMerged where flags=128 or flags='re.DEBUG';";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V9.2 - count of usages where flags=2
		valueName = FLAGS_2;
		query = "select count(*) as " +
			valueName +
			" from RegexCitationMerged where flags=2 or flags='re.I' or flags='re.IGNORECASE';";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V9.4 - count of usages where flags=4
		valueName = FLAGS_4;
		query = "select count(*) as " + valueName +
			" from RegexCitationMerged where flags=4 or flags='re.L' or flags='re.LOCALE';";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V9.8 - count of usages where flags=8
		valueName = FLAGS_8;
		query = "select count(*) as " +
			valueName +
			" from RegexCitationMerged where flags=8 or flags='re.M' or flags='re.MULTILINE';";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V9.16 - count of usages where flags=16
		valueName = FLAGS_16;
		query = "select count(*) as " +
			valueName +
			" from RegexCitationMerged where flags=16 or flags='re.S' or flags='re.DOTALL';";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V9.32 - count of usages where flags=32
		valueName = FLAGS_32;
		query = "select count(*) as " +
			valueName +
			" from RegexCitationMerged where flags=32 or flags='re.U' or flags='re.UNICODE';";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V9.64 - count of usages where flags=64
		valueName = FLAGS_64;
		query = "select count(*) as " +
			valueName +
			" from RegexCitationMerged where flags=64 or flags='re.X' or flags='re.VERBOSE';";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));

		//
		// 0.V9.255 - count of usages of composed flags - these are very rare or
		// non-existent but possible. Note that like does not have character
		// classes.
		valueName = FLAGS_255;
		query = "select count(*) as " +
			valueName +
			" from RegexCitationMerged where (flags like '0%' or flags like '1%' or flags like '2%' or flags like '3%' or flags like '4%' or flags like '5%' or flags like '6%' or flags like '7%' or flags like '8%' or flags like '9%') and (flags not in (0,2,4,8,16,32,64,128))";
		databaseFileContent.put(valueName, getIntFromQuery(connectionString, query, valueName));
		// note that there are other, possibly invalid entries in this column
		// like 'None' or a myConst.flag1 - we will ignore these and they are
		// rare.

		databaseFileContent.put(HAS_FLAGS, databaseFileContent.get(FLAGS_2) +
			databaseFileContent.get(FLAGS_4) +
			databaseFileContent.get(FLAGS_8) +
			databaseFileContent.get(FLAGS_16) +
			databaseFileContent.get(FLAGS_32) +
			databaseFileContent.get(FLAGS_64) +
			databaseFileContent.get(FLAGS_255));
		
		//this chunk is turning into spaghetti - refactor or separate more?
		//often doing two things at once.
		TreeSet<String> patternSet = new TreeSet<String>();
		int nCannotParse = getNCannotParse_popPatternSet(connectionString,patternSet);
		databaseFileContent.put(CANNOT_PARSE, nCannotParse);
		int nDefault = databaseFileContent.get(FLAGS_0);
		int nDebug = databaseFileContent.get(FLAGS_128);
		databaseFileContent.put(CAN_PARSE, (nDefault + nDebug) - nCannotParse);
		
		int nDistinctPatterns = patternSet.size();
		int nHasAlien = initializePatternList_retAlienCount(connectionString);
		int nRC = patternList.size();
		databaseFileContent.put(DISTINCT_PATTERN,nDistinctPatterns);
		databaseFileContent.put(HAS_ALIEN,nHasAlien);
		databaseFileContent.put(N_RC,nRC);
		
		
		//
		// 1.V1 raw number of distinct patterns in the corpus
		databaseFileContent.put("distinctPatterns", nDistinctPatterns);

	}

	private static int initializePatternList_retAlienCount(String connectionString)
			throws SQLException, ClassNotFoundException {
		int alienCount = 0;
		
		// prepare sql
		Connection c = null;
		Statement stmt = null;
		Class.forName("org.sqlite.JDBC");
		c = DriverManager.getConnection(connectionString);
		c.setAutoCommit(false);
		stmt = c.createStatement();

		// this proves that '\\s+' occurs in a certain number of projects:
		// select distinct uniqueSourceID from RegexCitationMerged where
		// (flags=0 or flags like 'arg%' or flags=128 or flags='re.DEBUG') and
		// pattern!='arg1' and pattern="'\\s+'"
		// I get the same number with the query below so it is probably working
		String query = "select pattern, count(distinct uniqueSourceID) as weight from RegexCitationMerged where (flags=0 or flags like 'arg%' or flags=128 or flags='re.DEBUG') and pattern!='arg1' group by pattern order by weight desc;";

		// these are all the distinct patterns with weight
		ResultSet rs = stmt.executeQuery(query);
		while (rs.next()) {
			String pattern = rs.getString("pattern");
			int weight = rs.getInt("weight");
			try {
				RegexCite r = new RegexCite(pattern, weight);
				patternList.add(r);
			} catch (AlienFeatureException e) {
				System.out.println(
					e.getMessage());
				alienCount++;
			} catch (Exception e) {
				System.out.println("Cannot parse " + pattern + " because: " +
					e.getMessage());
			}
		}

		// wind down sql: '^(XIE)$'
		rs.close();
		stmt.close();
		c.close();
		System.out.println("pl.size: " + patternList.size());
		return alienCount;
	}

	/**
	 * it's a little inefficient to do this just to get the nCannotParse, but
	 * this is straightforward and the sql for this and init pattern list is
	 * different
	 * 
	 * @param connectionString
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	private static int getNCannotParse_popPatternSet(String connectionString, TreeSet<String> patternSet)
			throws SQLException, ClassNotFoundException {
		int nCannotParse = 0;

		// prepare sql
		Connection c = null;
		Statement stmt = null;
		Class.forName("org.sqlite.JDBC");
		c = DriverManager.getConnection(connectionString);
		c.setAutoCommit(false);
		stmt = c.createStatement();
		String query = "select * from RegexCitationMerged where flags=0 or flags like 'arg%' or flags=128 or flags='re.DEBUG';";

		// these are all the regexes without behavioral flags
		ResultSet rs = stmt.executeQuery(query);
		while (rs.next()) {
			String pattern = rs.getString("pattern");
			patternSet.add(pattern);
			try {
				if (pattern == null) {
					throw new IllegalArgumentException("pattern cannot be null");
				} else if ("".equals(pattern)) {
					throw new IllegalArgumentException("pattern cannot be empty");
				} else {
					// make sure the pattern is a valid regex
					PythonInterpreter interpreter = new PythonInterpreter();
					interpreter.exec("import re");
					interpreter.exec("x = re.compile(" + pattern + ")");
				}
			} catch (Exception e) {
				nCannotParse++;
				System.out.println("Cannot parse " + pattern + " because: " +
					e.getMessage());
			}
		}

		// wind down sql
		rs.close();
		stmt.close();
		c.close();

		return nCannotParse;
	}

	private static String stringifyMap(HashMap<String, Integer> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("key,value\n");
		Set<Entry<String, Integer>> entrySet = map.entrySet();
		for (Entry<String, Integer> e : entrySet) {
			sb.append(e.getKey());
			sb.append(",");
			sb.append(e.getValue().toString() + "\n");
		}
		return sb.toString();
	}

	private static int getIntFromQuery(String connectionString, String query,
			String valueName) throws ClassNotFoundException, SQLException,
			ValueMissingException {
		int value = Integer.MIN_VALUE;

		// prepare sql
		Connection c = null;
		Statement stmt = null;
		Class.forName("org.sqlite.JDBC");
		c = DriverManager.getConnection(connectionString);
		c.setAutoCommit(false);
		stmt = c.createStatement();

		// execute query, look for the valueName
		ResultSet rs = stmt.executeQuery(query);
		if (rs.next()) {
			value = rs.getInt(valueName);
			System.out.println(valueName + ": " + value);
		} else {
			System.out.println("No value for: " + valueName);
		}

		// wind down sql
		rs.close();
		stmt.close();
		c.close();

		// fail fast for missing values
		if (value == Integer.MIN_VALUE) {
			throw new ValueMissingException();
		}
		return value;
	}
}

class ValueMissingException extends Exception {
	private static final long serialVersionUID = 12345L;
}
