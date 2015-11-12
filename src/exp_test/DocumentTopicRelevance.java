/**
 * 
 */
package exp_test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.MetaIndex;
import org.terrier.structures.bit.BitPostingIndex;
import org.terrier.structures.bit.DirectIndex;
import org.terrier.structures.postings.IterablePosting;

import utils.StemmingTerm;





/**
 * @author ould
 * cette classe calcule le RSV(Q,D) 
 */
@SuppressWarnings("deprecation")
public class DocumentTopicRelevance {

	/**
	 * @param args
	 */


	protected static Logger logger = Logger.getRootLogger();
	public static double mu = 2500;
	public static String Model = "DirichletLM";


	// trie
	public static boolean ASC = true;
	public static boolean DESC = false;


	// index  document 
	protected static Index  indexDoc;	
	static int numberOfDoc = 0;
	static double numberOftokens = 0;


	// files 
	static FileWriter file_resut ;

	static org.jdom2.Document document;
	static Element racine;

	public static void main(String[] args) throws IOException {
		loadIndex();
		file_resut = new FileWriter("./Resultats/"+Model+"_res.txt");
		numberOfDoc = indexDoc.getCollectionStatistics().getNumberOfDocuments();	
		numberOftokens = indexDoc.getCollectionStatistics().getNumberOfTokens();		
		SAXBuilder sxb = new SAXBuilder();
		try
		{
			document = sxb.build(new File("./topics/topics_delicious_cogo.xml"));
		}
		catch(Exception e){}
		racine = document.getRootElement();
		List<Element> listQuery = racine.getChildren("TOP");
		Iterator<Element> i = listQuery.iterator();
		while(i.hasNext())
		{
			Element courant = (Element)i.next();
			StemmingTerm porterStemmer = new StemmingTerm();
			String terme_query = porterStemmer.stripAffixes(courant.getChild("TITLE").getText()); // requete
			computeScore(courant.getChild("NUM").getText(),courant.getChild("USER").getText(), terme_query );
		}
		file_resut.close();
	}
	


	public static double dirichlet(double tf_t, double dl, double tf_c, double numberOfToken){
		return WeightingModelLibrary.log(1+(tf_t/(mu * (tf_c / numberOfToken)))) + WeightingModelLibrary.log(mu / (dl + mu));
	}


	public static void computeScore(String query_id, String user_id, String terme_query) throws IOException {
		HashMap<String, Double> result_liste = new HashMap<String, Double>();
		MetaIndex meta = indexDoc.getMetaIndex();
		@SuppressWarnings({ "resource" })
		DirectIndex di = new DirectIndex( (IndexOnDisk) indexDoc, "direct");
		DocumentIndex doi = indexDoc.getDocumentIndex();
		Lexicon<String> lex = indexDoc.getLexicon();
		LexiconEntry le = lex.getLexiconEntry( terme_query );
		if (le != null){
			BitPostingIndex inv = (BitPostingIndex) indexDoc.getInvertedIndex();
			IterablePosting postings = inv.getPostings((BitIndexPointer) le);		
			while (postings.next() != IterablePosting.EOL) {
				String docno = meta.getItem("docno", postings.getId());
				IterablePosting postings_doc = di.getPostings((BitIndexPointer)doi.getDocumentEntry(postings.getId()));
				result_liste.put(docno,dirichlet(postings.getFrequency(),postings_doc.getDocumentLength(),le.getFrequency(),numberOftokens));
			}		
			printResults(query_id, user_id, result_liste);
		}
	}

	public static void printResults(String query_id,String user_id, HashMap<String, Double> liste_result) throws IOException{	
		Map<String, Double> result_liste = new HashMap<String, Double>();
		result_liste= sortByComparator(liste_result,DESC );
		int rank = 1; 	
		for (String mapKey : result_liste.keySet()) {
			if(rank <=1000){
				file_resut.write(query_id +  " " + user_id + " " + mapKey + " " + rank + " " + result_liste.get(mapKey) + " " + Model + "\n");
				rank++;
			}
		}
	}
	
	private static Map<String, Double> sortByComparator(Map<String, Double> unsortMap, final boolean order)
	{
		List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(unsortMap.entrySet());
		Collections.sort(list, new Comparator<Entry<String, Double>>(){
			public int compare(Entry<String, Double> o1,Entry<String, Double> o2)
			{if (order){return o1.getValue().compareTo(o2.getValue());}
			else
			{return o2.getValue().compareTo(o1.getValue());	}
			}
		});

		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : list)
		{sortedMap.put(entry.getKey(), entry.getValue());}

		return sortedMap;
	}




	/**
	 * chargement de l'index des documents et des tags des documents 
	 */
	public static void loadIndex(){
		// chargement de l'index des documents 
		long startLoadingDoc = System.currentTimeMillis();
		indexDoc = IndexOnDisk.createIndex();
		if(indexDoc == null)
		{
			logger.fatal("Failed to load index tags. Perhaps index files are missing");
		}
		long endLoadingDoc = System.currentTimeMillis();
		if (logger.isInfoEnabled())
			logger.info("time to intialise index tag: " + ((endLoadingDoc-startLoadingDoc)/1000.0D));


	}




}
