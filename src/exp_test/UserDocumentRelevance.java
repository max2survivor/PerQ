/**
 * 
 */
package exp_test;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.collections4.ListUtils;
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
 *
 */
@SuppressWarnings("deprecation")
public class UserDocumentRelevance {

	/**
	 * @param args
	 */



	protected static Logger logger = Logger.getRootLogger();
	public static double mu = 2500;
	public static String Model = "DirichletLM_DOC+TAG";
	static double alpha = 0.8;


	// trie
	public static boolean ASC = true;
	public static boolean DESC = false;


	// index  document 
	protected static Index  indexDoc;	
	static int numberOfDoc = 0;
	static double numberOftokens = 0;


	// index tags document 
	protected static Index  indexTag;	
	static int numberOfDocTag = 0;
	static double numberOftokensTag = 0;

	// files 
	static FileWriter file_profil_general_weight,file_profil_general;
	static FileWriter file_snq_gen_weight, file_snq_gen;
	static FileWriter file_snq_sel_weight, file_snq_sel;
	static FileWriter statistique_SNQ ;
	static org.jdom2.Document document;
	static Element racine;
	
	static HashMap<String, String> map_id_tag = new HashMap<String, String>();
	public static void main(String[] args) throws IOException {
		statistique_SNQ = new FileWriter("./Resultats/SNQ/statistique_Snq.xml");
		statistique_SNQ.write("user" + " " + "size_PU_G"+ " "+ "size_SNQ_G" + " "+ "size_SNQ_S \n");
		Scanner s1 = new Scanner(new File("./files/tags"));
		while (s1.hasNextLine()) {
			String line = s1.nextLine();
			String vect [] = line.split(" ");
			map_id_tag.put(vect[1], vect[0]);			
		}
		s1.close();	

		loadIndex();
		file_profil_general = new FileWriter("./Resultats/SNQ/"+Model+"_profil_general_res.txt");
		file_profil_general_weight = new FileWriter("./Resultats/SNQ/"+Model+"_profil_general_weight_res.txt");
		file_snq_gen_weight = new FileWriter("./Resultats/SNQ/"+Model+"_snq_gen_weight_res.txt");
		file_snq_gen = new FileWriter("./Resultats/SNQ/"+Model+"_snq_gen_res.txt");
		file_snq_sel_weight = new FileWriter("./Resultats/SNQ/"+Model+"_snq_sel_weight_res.txt");
		file_snq_sel = new FileWriter("./Resultats/SNQ/"+Model+"_snq_sel_res.txt");
		
		numberOfDocTag = indexTag.getCollectionStatistics().getNumberOfDocuments();	
		numberOftokensTag = indexTag.getCollectionStatistics().getNumberOfTokens();
		numberOfDoc = indexDoc.getCollectionStatistics().getNumberOfDocuments();	
		numberOftokens = indexDoc.getCollectionStatistics().getNumberOfTokens();
		// tags 
		HashMap<String, String> map_id_tag = new HashMap<String, String>();
		Scanner st = new Scanner(new File("./files/tags"));
		while (st.hasNextLine()) {
			String line = st.nextLine();
			String vect [] = line.split(" ");
			map_id_tag.put(vect[1], vect[0]);			
		}
		st.close();	

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

			// profil general de l'utilisateur 
			HashMap<String, Double> map_profil_general = new HashMap<String, Double>();
			ArrayList<String> liste_terme_profil_general = new ArrayList<String>();

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
						liste_terme_profil_general.add(term_fr[0]);
						map_profil_general.put(term_fr[0], Double.valueOf(term_fr[2])); // tf_idf 
						if(term_fr[0].equals(terme_query)){
							tf_remove = Double.parseDouble(term_fr[1]);	
						}
					}					
				}				
			}
			s2.close();
			HashMap<String, Double> user_doc_tags = new HashMap<String, Double>();
			Scanner s11 = new Scanner(new File("./files/users_files/"+courant.getChild("USER").getText()+".txt"));
			while (s11.hasNextLine()) {
				String line = s11.nextLine();
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
			s11.close();	


			/*
			 *   calculs 
			 */
			/*
			 *     -- 1 -- profil general de l'utilisateur 
			 */

			//---------- calcul score Dirichlet pour le profil general de l'utilisateur --------//
			
	        computeScoreListeTerme(courant.getChild("NUM").getText(),courant.getChild("USER").getText(), map_profil_general, user_doc_tags, file_profil_general);
			computeScoreListeTermeWeight(courant.getChild("NUM").getText(),courant.getChild("USER").getText(), map_profil_general, user_doc_tags, file_profil_general_weight);

			/*
			 *   -- 2 --  profil SNQ : tout les utiliateurs qui ont utilié le terme de la requete 
			 */
					/*
					 *  SNQ General 
					 */
			
			List<String> profil_general_SNQ = new ArrayList<String>();				
			profil_general_SNQ = getProfilGeneralSNQ(liste_terme_profil_general, terme_query, courant.getChild("USER").getText());
			HashMap<String, Double> map_profil_general_snq = new HashMap<String,Double>();
			
			for (int j = 0; j < profil_general_SNQ.size(); j++) {
				map_profil_general_snq.put(profil_general_SNQ.get(j), map_profil_general.get(profil_general_SNQ.get(j)));				
			}
			computeScoreListeTerme(courant.getChild("NUM").getText(),courant.getChild("USER").getText(), map_profil_general_snq, user_doc_tags, file_snq_gen);
			computeScoreListeTermeWeight(courant.getChild("NUM").getText(),courant.getChild("USER").getText(), map_profil_general_snq, user_doc_tags, file_snq_gen_weight);

					/*
					 * SNQ Selection
					 */
			
			List<String> profil_selection_SNQ = new ArrayList<String>();				
			profil_selection_SNQ = getProfilSelectionSNQ(liste_terme_profil_general, terme_query, courant.getChild("USER").getText());
			HashMap<String, Double> map_profil_selection_snq = new HashMap<String,Double>();
			for (int j = 0; j < profil_selection_SNQ.size(); j++) {
				map_profil_selection_snq.put(profil_selection_SNQ.get(j), map_profil_general.get(profil_selection_SNQ.get(j)));				
			}
			computeScoreListeTerme(courant.getChild("NUM").getText(),courant.getChild("USER").getText(), map_profil_selection_snq, user_doc_tags, file_snq_sel);
			computeScoreListeTermeWeight(courant.getChild("NUM").getText(),courant.getChild("USER").getText(), map_profil_selection_snq, user_doc_tags, file_snq_sel_weight);

				
					/*
					 * Statistiques
					 */
			statistique_SNQ.write(courant.getChild("USER").getText()+ " " + map_profil_general.size()+ " " + profil_general_SNQ.size() + " " + map_profil_selection_snq.size() +"\n");
			statistique_SNQ.flush();
		}
		file_profil_general.close();
		file_profil_general_weight.close();
		file_snq_gen.close();
		file_snq_gen_weight.close();
		file_snq_sel.close();
		file_snq_sel_weight.close();
		statistique_SNQ.close();
	}
	
	public static List<String> getProfilSelectionSNQ(ArrayList<String> user_profil_general, String terme_query, String user_id) throws IOException{
		ArrayList<String> tags = new ArrayList<>();
		ArrayList<String> liste_document = new ArrayList<String>();
		liste_document = getDocument(terme_query);

		Scanner s = new Scanner(new File("./files/users.txt"));
		ArrayList<String> users = new ArrayList<String>();
		while (s.hasNextLine()) {
			users.add(s.nextLine());	
		}
		s.close();


		if(users.contains(user_id)) users.remove(user_id);

		StemmingTerm porterStemmer = new StemmingTerm();
		for (int i = 0; i < users.size(); i++) {
			Scanner s2 = new Scanner(new File("./files/users_files/"+users.get(i) +".txt"));
			while (s2.hasNextLine()) {
				String [] line = s2.nextLine().split(" ");
				if(liste_document.contains(line[1])){
					if(map_id_tag.containsKey(line[3]))
					tags.add(porterStemmer.stripAffixes(map_id_tag.get(line[3])));
				}				
			}
			s2.close();
		}

		Set<String> set = new HashSet<String>() ;
		set.addAll(tags) ;
		ArrayList<String> liste = new ArrayList<String>(set) ;		
		List<String> profil_selection_snq = new ArrayList<String>();
		profil_selection_snq = ListUtils.intersection(user_profil_general, liste);		
		return profil_selection_snq;
	}

	public static ArrayList<String> getDocument(String terme_query) throws IOException{		
		// on cherche les documents qui contiennent le terme de la requete comme tag
		ArrayList<String> liste_document = new ArrayList<String>();
		BitPostingIndex invTag =(BitPostingIndex) indexTag.getInvertedIndex();
		Lexicon<String> lexTag = indexTag.getLexicon();
		MetaIndex metaTag = indexTag.getMetaIndex();
		LexiconEntry leTag = lexTag.getLexiconEntry(terme_query);
		IterablePosting postings = invTag.getPostings((BitIndexPointer) leTag);
		if (leTag != null){	
			while (postings.next() != IterablePosting.EOL) {
				String docno = metaTag.getItem("docno", postings.getId());				
				liste_document.add(docno);
			}
		}
		return liste_document;
	}


	public static List<String> getProfilGeneralSNQ(ArrayList<String> user_profil_general, String terme_query, String user_id) throws IOException{
		ArrayList<String> users_select = new ArrayList<>();
		ArrayList<String> liste_document = new ArrayList<String>();
		// on cherche tous les documents qui ont été tagué avec le terme de la requete
		liste_document = getDocument(terme_query);

		// on charge les utilisateurs 
		Scanner s = new Scanner(new File( "./files/users.txt" ));
		ArrayList<String> users = new ArrayList<String>();
		while (s.hasNextLine()) {
			users.add(s.nextLine());			
		}
		s.close();

		// on selectionne les utilisateur ayant tagué les documents selectionné dans l'étape precedante
		for (int i = 0; i < users.size(); i++) {
			Scanner s2 = new Scanner(new File("./files//users_files/"+users.get(i) +".txt"));
			while (s2.hasNextLine()) {
				String [] line = s2.nextLine().split(" ");
				if(liste_document.contains(line[1])){
					users_select.add(line[2]);					
					break;
				}		
			}
			s2.close();
		}
		
		if(users_select.contains(user_id))
			users_select.remove(user_id);

		// on selectionne tout les tags des utilisateurs (selectionné )mis pour les document qui ont été tagé avec le terme de la requete 
		ArrayList<String>  tags = new ArrayList<>();	
		Scanner s2 = new Scanner(new File("./files/index_users.txt"));
		while (s2.hasNextLine()) {
			String [] line = s2.nextLine().split(" ");		
			if(users_select.contains(line[0])){
				for (int i = 1; i < line.length; i++) {
					String [] vect = line[i].split(":");	
					tags.add(vect[0]);
				}			
			}
		}
		s2.close();

		// on fais l'intersection avec le profil general de l'utilisateur
		Set<String> set = new HashSet<String>() ;
		set.addAll(tags) ;
		ArrayList<String> liste = new ArrayList<String>(set) ;	
		List<String> profil_General_SNQ= new ArrayList<String>();
		profil_General_SNQ = ListUtils.intersection(user_profil_general, liste);		
		return profil_General_SNQ;
	}


	public static double dirichlet(double tf_t, double dl, double tf_c, double numberOfToken){
		return WeightingModelLibrary.log(1+(tf_t/(mu * (tf_c / numberOfToken)))) + WeightingModelLibrary.log(mu / (dl + mu));
	}



	public static void computeScoreListeTerme(String query_id, String user_id, HashMap<String, Double> profil_user, HashMap<String, Double> user_doc_tags, FileWriter file_resut_liste_terme) throws IOException {
		HashMap<String, Double> result_liste_doc = new HashMap<String, Double>();
		MetaIndex meta = indexDoc.getMetaIndex();
		@SuppressWarnings({ "resource" })
		DirectIndex di = new DirectIndex( (IndexOnDisk) indexDoc, "direct");
		DocumentIndex doi = indexDoc.getDocumentIndex();
		Lexicon<String> lex = indexDoc.getLexicon();
		for(Entry<String, Double> entry : profil_user.entrySet()) {
			String terme = entry.getKey();
			//Double weight = entry.getValue();	
			LexiconEntry le = lex.getLexiconEntry( terme );
			if (le != null){
				BitPostingIndex inv = (BitPostingIndex) indexDoc.getInvertedIndex();
				IterablePosting postings = inv.getPostings((BitIndexPointer) le);		
				while (postings.next() != IterablePosting.EOL) {
					String docno = meta.getItem("docno", postings.getId());
					IterablePosting postings_doc = di.getPostings((BitIndexPointer)doi.getDocumentEntry(postings.getId()));
					if(result_liste_doc.containsKey(docno)){
						result_liste_doc.put(docno,result_liste_doc.get(docno) + 
								dirichlet(postings.getFrequency(),postings_doc.getDocumentLength(),le.getFrequency(),numberOftokens));
					}
					else {
						result_liste_doc.put(docno,dirichlet(postings.getFrequency(),postings_doc.getDocumentLength(),le.getFrequency(),numberOftokens));
					}
				}		
			}
		}

		// partie tag

		HashMap<String, Double> result_liste_tag = new HashMap<String, Double>();
		MetaIndex metaTag = indexTag.getMetaIndex();
		@SuppressWarnings({ "resource" })
		DirectIndex diTag = new DirectIndex( (IndexOnDisk) indexTag, "direct");
		DocumentIndex doiTag = indexTag.getDocumentIndex();
		Lexicon<String> lexTag = indexTag.getLexicon();		
		for(Entry<String, Double> entry : profil_user.entrySet()) {
			String terme = entry.getKey();
			LexiconEntry letag= lexTag.getLexiconEntry(terme);
			if (letag != null){
				BitPostingIndex inv = (BitPostingIndex) indexTag.getInvertedIndex();
				IterablePosting postings = inv.getPostings((BitIndexPointer) letag);		
				while (postings.next() != IterablePosting.EOL) {
					String docno = metaTag.getItem("docno", postings.getId());
					IterablePosting postings_doc = diTag.getPostings((BitIndexPointer)doiTag.getDocumentEntry(postings.getId()));

					if (result_liste_tag.containsKey(docno)) {
						result_liste_tag.put(docno,result_liste_tag.get(docno) 
								+ dirichlet(postings.getFrequency(),postings_doc.getDocumentLength(),letag.getFrequency(),numberOftokensTag));
					}
					else {
						result_liste_tag.put(docno,dirichlet(postings.getFrequency(),postings_doc.getDocumentLength(),letag.getFrequency(),numberOftokensTag));
					}
				}				
			}
		}

		// Fusion 
		HashMap<String, Double> result_liste = new HashMap<String, Double>();
		result_liste.putAll(result_liste_tag);
		result_liste.putAll(result_liste_doc);
		for(Entry<String, Double> entry : result_liste.entrySet()) {
			String cle = entry.getKey();
			if(result_liste_doc.containsKey(cle) && result_liste_tag.containsKey(cle)){
				result_liste.put(cle, alpha * result_liste_doc.get(cle) + (1-alpha) * result_liste_tag.get(cle));
			}
			else {
				if(!result_liste_doc.containsKey(cle) && result_liste_tag.containsKey(cle))
					result_liste.put(cle, (1-alpha) * result_liste_tag.get(cle));
				else {
					result_liste.put(cle, alpha * result_liste_doc.get(cle));

				}
			}
		}
		printResults(query_id, user_id, result_liste, file_resut_liste_terme);
	}

	public static void computeScoreListeTermeWeight(String query_id, String user_id, HashMap<String, Double> profil_user, HashMap<String, Double> user_doc_tag, FileWriter file_resut_liste_terme_weight) throws IOException {
		HashMap<String, Double> result_liste_doc = new HashMap<String, Double>();
		MetaIndex meta = indexDoc.getMetaIndex();
		@SuppressWarnings({ "resource" })
		DirectIndex di = new DirectIndex( (IndexOnDisk) indexDoc, "direct");
		DocumentIndex doi = indexDoc.getDocumentIndex();
		Lexicon<String> lex = indexDoc.getLexicon();
		for(Entry<String, Double> entry : profil_user.entrySet()) {
			String terme = entry.getKey();
			Double weight = entry.getValue();	
			LexiconEntry le = lex.getLexiconEntry( terme );
			if (le != null){
				BitPostingIndex inv = (BitPostingIndex) indexDoc.getInvertedIndex();
				IterablePosting postings = inv.getPostings((BitIndexPointer) le);		
				while (postings.next() != IterablePosting.EOL) {
					String docno = meta.getItem("docno", postings.getId());
					IterablePosting postings_doc = di.getPostings((BitIndexPointer)doi.getDocumentEntry(postings.getId()));
					if(result_liste_doc.containsKey(docno)){
						result_liste_doc.put(docno,result_liste_doc.get(docno)
								+ weight * dirichlet(postings.getFrequency(),postings_doc.getDocumentLength(),le.getFrequency(),numberOftokens));
					}
					else {
						result_liste_doc.put(docno, weight * dirichlet(postings.getFrequency(),postings_doc.getDocumentLength(),le.getFrequency(),numberOftokens));
					}
				}		
			}
		}

		// partie tag

		HashMap<String, Double> result_liste_tag = new HashMap<String, Double>();
		MetaIndex metaTag = indexTag.getMetaIndex();
		@SuppressWarnings({ "resource" })
		DirectIndex diTag = new DirectIndex( (IndexOnDisk) indexTag, "direct");
		DocumentIndex doiTag = indexTag.getDocumentIndex();
		Lexicon<String> lexTag = indexTag.getLexicon();		
		for(Entry<String, Double> entry : profil_user.entrySet()) {
			String terme = entry.getKey();
			Double weight = entry.getValue();	
			LexiconEntry letag= lexTag.getLexiconEntry(terme);
			if (letag != null){
				BitPostingIndex inv = (BitPostingIndex) indexTag.getInvertedIndex();
				IterablePosting postings = inv.getPostings((BitIndexPointer) letag);		
				while (postings.next() != IterablePosting.EOL) {
					String docno = metaTag.getItem("docno", postings.getId());
					IterablePosting postings_doc = diTag.getPostings((BitIndexPointer)doiTag.getDocumentEntry(postings.getId()));

					if (result_liste_tag.containsKey(docno)) {
						result_liste_tag.put(docno,result_liste_tag.get(docno) 
								+  weight * dirichlet(postings.getFrequency(),postings_doc.getDocumentLength(),letag.getFrequency(),numberOftokensTag));
					}
					else {
						result_liste_tag.put(docno, weight * dirichlet(postings.getFrequency(),postings_doc.getDocumentLength(),letag.getFrequency(),numberOftokensTag));
					}
				}				
			}
		}

		// Fusion 
		HashMap<String, Double> result_liste = new HashMap<String, Double>();
		result_liste.putAll(result_liste_tag);
		result_liste.putAll(result_liste_doc);

		for(Entry<String, Double> entry : result_liste.entrySet()) {
			String cle = entry.getKey();
			if(result_liste_doc.containsKey(cle) && result_liste_tag.containsKey(cle))
				result_liste.put(cle, alpha * result_liste_doc.get(cle) + (1-alpha) * result_liste_tag.get(cle));
			else {
				if(!result_liste_doc.containsKey(cle) && result_liste_tag.containsKey(cle))
					result_liste.put(cle, (1-alpha) * result_liste_tag.get(cle));
				else {
					result_liste.put(cle, alpha * result_liste_doc.get(cle));
				}
			}
		}

		printResults(query_id, user_id, result_liste, file_resut_liste_terme_weight);
	}

	public static void computeScoreTerme(String query_id, String user_id, String terme_query, HashMap<String, Double> user_doc_tags, double tf_remove, FileWriter file_resut_liste_terme) throws IOException {
		HashMap<String, Double> result_liste_doc = new HashMap<String, Double>();
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
				result_liste_doc.put(docno,dirichlet(postings.getFrequency(),postings_doc.getDocumentLength(),le.getFrequency(),numberOftokens));
			}		
		}

		// partie tag

		HashMap<String, Double> result_liste_tag = new HashMap<String, Double>();
		MetaIndex metaTag = indexTag.getMetaIndex();
		@SuppressWarnings({ "resource" })
		DirectIndex diTag = new DirectIndex( (IndexOnDisk) indexTag, "direct");
		DocumentIndex doiTag = indexTag.getDocumentIndex();
		Lexicon<String> lexTag = indexTag.getLexicon();
		LexiconEntry letag= lexTag.getLexiconEntry(terme_query);
		double tfu_remove=0;
		if (letag != null){
			BitPostingIndex inv = (BitPostingIndex) indexTag.getInvertedIndex();
			IterablePosting postings = inv.getPostings((BitIndexPointer) letag);		
			while (postings.next() != IterablePosting.EOL) {
				String docno = metaTag.getItem("docno", postings.getId());
				IterablePosting postings_doc = diTag.getPostings((BitIndexPointer)doiTag.getDocumentEntry(postings.getId()));
				if(user_doc_tags.containsKey(docno)){
					tfu_remove= user_doc_tags.get(docno);
				}
				result_liste_tag.put(docno,dirichlet(postings.getFrequency()-tfu_remove,postings_doc.getDocumentLength(),letag.getFrequency()-tf_remove,numberOftokensTag));
			}				
		}

		// Fusion 
		HashMap<String, Double> result_liste = new HashMap<String, Double>();
		result_liste.putAll(result_liste_tag);
		result_liste.putAll(result_liste_doc);


		for(Entry<String, Double> entry : result_liste.entrySet()) {
			String cle = entry.getKey();
			if(result_liste_doc.containsKey(cle) && result_liste_tag.containsKey(cle))
				result_liste.put(cle, alpha * result_liste_doc.get(cle) + (1-alpha) * result_liste_tag.get(cle));
			else {
				if(!result_liste_doc.containsKey(cle) && result_liste_tag.containsKey(cle))
					result_liste.put(cle, (1-alpha) * result_liste_tag.get(cle));
				else {
					result_liste.put(cle, alpha * result_liste_doc.get(cle));
				}
			}
		}

		printResults(query_id, user_id, result_liste, file_resut_liste_terme);


	}

	public static void printResults(String query_id,String user_id, HashMap<String, Double> liste_result, FileWriter file) throws IOException{	
		Map<String, Double> result_liste = new HashMap<String, Double>();
		result_liste= sortByComparator(liste_result,DESC);
		int rank = 1; 	
		for (String mapKey : result_liste.keySet()) {
			if(rank <=1000){
				file.write(query_id +  " " + user_id + " " + mapKey + " " + rank + " " + result_liste.get(mapKey) + " " + Model + "\n");
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

	public static void writeFile(List<String> liste, String user_id, String id_query, FileWriter file_name) throws IOException{		
		String title = "";
		for (int i = 0; i < liste.size(); i++) {
			title = title + liste.get(i) + " ";
		}
		file_name.write("<TOP> \n");
		file_name.write("<NUM>"+id_query+"</NUM>\n");
		file_name.write("<TITLE>"+ title + "</TITLE>\n");
		file_name.write("<USER>"+ user_id +"</USER>\n");
		file_name.write("</TOP>\n");
	}


	/**
	 * chargement de l'index des documents et des tags des documents 
	 */
	public static void loadIndex(){
		// chargement de l'index des tags  des documents 
		long startLoadingDocTag = System.currentTimeMillis();
		indexTag = Index.createIndex("/home/ould/ould/collections/delicious_cogo/indexation/index_document_tags/terrier-4.0/var/index", "data");
		if(indexTag == null)
		{
			logger.fatal("Failed to load index tags. Perhaps index files are missing");
		}
		long endLoadingDocTag = System.currentTimeMillis();
		if (logger.isInfoEnabled())
			logger.info("time to intialise index tag: " + ((endLoadingDocTag-startLoadingDocTag)/1000.0D));



		// chargement de l'index des documents 
		long startLoadingDoc = System.currentTimeMillis();
		indexDoc = Index.createIndex("/home/ould/ould/collections/delicious_cogo/indexation/index_documents/terrier-4.0/var/index", "data");
		if(indexDoc == null)
		{
			logger.fatal("Failed to load index tags. Perhaps index files are missing");
		}
		long endLoadingDoc = System.currentTimeMillis();
		if (logger.isInfoEnabled())
			logger.info("time to intialise index tag: " + ((endLoadingDoc-startLoadingDoc)/1000.0D));
	}

}

