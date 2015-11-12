package exp_test;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;
import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.MetaIndex;
import org.terrier.structures.bit.BitPostingIndex;
import org.terrier.structures.postings.IterablePosting;

import utils.StemmingTerm;

/**
 * @author ould
 *
 */
public class SNQ {
	static FileWriter statistique_Snq;
	static FileWriter file_user_profil_general ;
	static FileWriter file_profil_selection_snq ;
	static FileWriter file_profil_general_snq ;
	static org.jdom2.Document document;
	static Element racine;


	// index tags document 



	// index des tags des documents 	
	protected static Index  indexTag;
	static int numberOfDocTags = 0;
	static double numberOftokensTags = 0;
	protected static Logger logger = Logger.getRootLogger();

	// index des users 

	protected static Index  indexUser;	
	static int numberOfUser = 0;
	static HashMap<String, String> map_id_tag = new HashMap<String, String>();

	public SNQ() {
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		loadIndex();
		statistique_Snq  = new FileWriter("./Resultats/SNQ/statistique_Snq.xml");
		file_profil_selection_snq  = new FileWriter("./Resultats/SNQ/file_profil_selection_snq.xml");
		file_user_profil_general = new FileWriter("./Resultats/SNQ/file_user_profil_general.xml");
		file_profil_general_snq = new FileWriter("./Resultats/SNQ/file_profil_general_snq.xml");
		
		// tags 

		Scanner s1 = new Scanner(new File("./files/tags"));
		while (s1.hasNextLine()) {
			String line = s1.nextLine();
			String vect [] = line.split(" ");
			map_id_tag.put(vect[1], vect[0]);			
		}
		s1.close();	

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
			ArrayList<String> user_profil_general = new ArrayList<String>();
			Element courant = i.next();

			StemmingTerm porterStemmer = new StemmingTerm();
			String terme_query = porterStemmer.stripAffixes(courant.getChild("TITLE").getText());
			user_profil_general =  getUserGeneralProfil(courant.getChild("USER").getText(), terme_query);

			// création profils 

			// 1 --- profil snq 
			List<String> profil_selection_snq = new ArrayList<String>();				
			profil_selection_snq = getProfilSelectionSnq(user_profil_general, terme_query);
			//writeFile(profil_selection_snq, courant.getChild("USER").getText(), courant.getChild("NUM").getText(), file_profil_selection_snq);

			// 2 - user profil general 				
			///writeFile(user_profil_general,  courant.getChild("USER").getText(), courant.getChild("NUM").getText(), file_user_profil_general);				


			//  3 -- profil snq general 
			List<String> profil_general_snq = new ArrayList<String>();				
			profil_general_snq = getProfilGeneralSnq(user_profil_general, terme_query);
			//writeFile(profil_general_snq,  courant.getChild("USER").getText(), courant.getChild("NUM").getText(), file_profil_general_snq);			


			statistique_Snq.write(courant.getChild("NUM").getText() + " " + courant.getChild("USER").getText() + " " + user_profil_general.size()+ " " + profil_general_snq.size() + " " + profil_selection_snq.size() + "\n");
			statistique_Snq.flush();

		}

		file_profil_selection_snq.close();
		file_profil_general_snq.close();
		file_user_profil_general.close();
		statistique_Snq.close();

	}


	public static List<String> getProfilGeneralSnq(ArrayList<String> user_profil_general, String terme_query) throws IOException{
		ArrayList<String> users_select = new ArrayList<>();
		ArrayList<String> liste_document = new ArrayList<String>();
		liste_document = getDocument(terme_query);

		Scanner s = new Scanner(new File( "./files/users.txt" ));
		ArrayList<String> users = new ArrayList<String>();
		while (s.hasNextLine()) {
			users.add(s.nextLine());			
		}
		s.close();

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


		ArrayList<String>  tags = new ArrayList<>();	
		Scanner s2 = new Scanner(new File("./files/index_users.txt"));
		while (s2.hasNextLine()) {
			String [] line = s2.nextLine().split(" ");		
			if(users_select.contains(line[0])){
				for (int i = 1; i < line.length; i++) {
					tags.add(line[i]);
				}			
			}
		}
		s2.close();

		Set<String> set = new HashSet<String>() ;
		set.addAll(tags) ;
		ArrayList<String> liste = new ArrayList<String>(set) ;		
		List<String> profil_Selection_snq = new ArrayList<String>();
		profil_Selection_snq = ListUtils.intersection(user_profil_general, liste);		
		return profil_Selection_snq;

	}
	public static List<String> getProfilSelectionSnq(ArrayList<String> user_profil_general, String terme_query) throws IOException{
		ArrayList<String> tags = new ArrayList<>();
		ArrayList<String> liste_document = new ArrayList<String>();
		liste_document = getDocument(terme_query);

		Scanner s = new Scanner(new File("./files/users.txt"));
		ArrayList<String> users = new ArrayList<String>();
		while (s.hasNextLine()) {
			users.add(s.nextLine());	
		}
		s.close();



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
		List<String> profil_snq = new ArrayList<String>();
		profil_snq = ListUtils.intersection(user_profil_general, liste);		
		return profil_snq;
	}

	public static ArrayList<String> getUserGeneralProfil(String user_id, String terme_query) throws IOException{
		Scanner s = new Scanner(new File("./files/index_users.txt"));
		ArrayList<String> user_profil = new ArrayList<String>();
		boolean trouve = false;
		while (s.hasNextLine() && trouve == false) {
			String [] line = s.nextLine().split(" ");		
			if(line[0].equals(user_id)){
				trouve = true;
				for (int k = 1; k < line.length; k++) {
					String [] vect = line[k].split(":");
					user_profil.add(vect[0]);					
				}
			}
		}
		s.close();
		if(user_profil.contains(terme_query)) user_profil.remove(terme_query);
		return user_profil;
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
	}


}
