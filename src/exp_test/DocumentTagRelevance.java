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
import java.util.Scanner;
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
public class DocumentTagRelevance {

	/**
	 * @param args
	 */
	protected static Logger logger = Logger.getRootLogger();
	public static double mu = 2500;
	public static String Model = "DirichletLM_TAG";


	// tri
	public static boolean ASC = true;
	public static boolean DESC = false;


	// index tags document 
	protected static Index  indexTag;	
	static int numberOfDoc = 0;
	static double numberOftokens = 0;


	// files 
	static FileWriter file_resut ;
	static org.jdom2.Document document;
	static Element racine;

	public static void main(String[] args) throws IOException {
		loadIndex();
		file_resut = new FileWriter("./Resultats/"+Model+"_res.txt");
		numberOfDoc = indexTag.getCollectionStatistics().getNumberOfDocuments();	
		numberOftokens = indexTag.getCollectionStatistics().getNumberOfTokens();
		// tags 
		HashMap<String, String> map_id_tag = new HashMap<String, String>();
		Scanner s = new Scanner(new File("./files/tags"));
		while (s.hasNextLine()) {
			String line = s.nextLine();
			String vect [] = line.split(" ");
			map_id_tag.put(vect[1], vect[0]);			
		}
		s.close();	
		
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
			// suprrimer 
			Scanner s2 = new Scanner(new File("./files/index_users.txt"));
			double tf_remove = 0;
			while (s2.hasNextLine()) {
				String string = (String) s2.nextLine();
				String [] vect = string.split(" ");
				if(vect[0].equals(courant.getChild("USER").getText())){
					for (int j = 1; j < vect.length; j++) {
						String [] term_fr = vect[j].split(":");
						if(term_fr[0].equals(terme_query)){
							tf_remove = Double.parseDouble(term_fr[1]);	
						}
					}					
				}				
			}
			s2.close();
			HashMap<String, Double> user_doc_tags = new HashMap<String, Double>();
			Scanner s1 = new Scanner(new File("./files/users_files/"+courant.getChild("USER").getText()+".txt"));
			while (s1.hasNextLine()) {
				String line = s1.nextLine();
				String  [] vect = line.split(" ");
				if(map_id_tag.containsKey(vect[3])){
					if(porterStemmer.stripAffixes(map_id_tag.get(vect[3])).equals(terme_query)){
						if(user_doc_tags.containsKey(vect[1])){
							user_doc_tags.put(vect[1], user_doc_tags.get(vect[1]) + 1);
						}
						else {
							user_doc_tags.put(vect[1], (double)1);
						}
					}
				}								
			}
			s1.close();			
			computeScore(courant.getChild("NUM").getText(),courant.getChild("USER").getText(), terme_query, user_doc_tags, tf_remove);
		}
		file_resut.close();
	}
	


	public static double dirichlet(double tf_t, double dl, double tf_c, double numberOfToken){
		return WeightingModelLibrary.log(1+(tf_t/(mu * (tf_c / numberOfToken)))) + WeightingModelLibrary.log(mu / (dl + mu));
	}


	public static void computeScore(String query_id, String user_id, String terme_query, HashMap<String, Double> user_doc_tags, double tf_remove) throws IOException {
		HashMap<String, Double> result_liste = new HashMap<String, Double>();
		MetaIndex meta = indexTag.getMetaIndex();
		@SuppressWarnings({ "resource" })
		DirectIndex di = new DirectIndex( (IndexOnDisk) indexTag, "direct");
		DocumentIndex doi = indexTag.getDocumentIndex();
		Lexicon<String> lex = indexTag.getLexicon();
		LexiconEntry le = lex.getLexiconEntry(terme_query);
		double tfu_remove=0;
		if (le != null){
			BitPostingIndex inv = (BitPostingIndex) indexTag.getInvertedIndex();
			IterablePosting postings = inv.getPostings((BitIndexPointer) le);		
			while (postings.next() != IterablePosting.EOL) {
				String docno = meta.getItem("docno", postings.getId());
				IterablePosting postings_doc = di.getPostings((BitIndexPointer)doi.getDocumentEntry(postings.getId()));
				if(user_doc_tags.containsKey(docno)){
					tfu_remove= user_doc_tags.get(docno);
				}
				result_liste.put(docno,dirichlet(postings.getFrequency()-tfu_remove,postings_doc.getDocumentLength(),le.getFrequency()-tf_remove,numberOftokens));
			}		
			printResults(query_id, user_id, result_liste);
		}
	}

	public static void printResults(String query_id,String user_id, HashMap<String, Double> liste_result) throws IOException{	
		Map<String, Double> result_liste = new HashMap<String, Double>();
		result_liste= sortByComparator(liste_result,DESC);
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
		indexTag = Index.createIndex("/home/ould/ould/collections/delicious_cogo/indexation/index_document_tags/terrier-4.0/var/index", "data");
		if(indexTag == null)
		{
			logger.fatal("Failed to load index tags. Perhaps index files are missing");
		}
		long endLoadingDoc = System.currentTimeMillis();
		if (logger.isInfoEnabled())
			logger.info("time to intialise index tag: " + ((endLoadingDoc-startLoadingDoc)/1000.0D));
	}




}
