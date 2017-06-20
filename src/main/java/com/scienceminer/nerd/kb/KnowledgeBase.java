package com.scienceminer.nerd.kb;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scienceminer.nerd.kb.db.*;
import com.scienceminer.nerd.kb.db.KBEnvironment.StatisticName;
import com.scienceminer.nerd.utilities.NerdConfig;
import com.scienceminer.nerd.kb.model.Page.PageType;

/**
 * The class offering an access to the (N)ERD Knowledge Base, concepts and all the language indenpendent
 * semantic information, and access to language-specific data level.
 * 
 */
public class KnowledgeBase {
	protected static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeBase.class);

	private KBUpperEnvironment env = null;
	private long conceptCount = -1;

	/**
	 * Initialises a newly created Wikipedia according to the given configuration. 
	 *  
	 * @param conf a Nerd configuration 
	 */
	public KnowledgeBase(NerdConfig conf) {
		this.env = new KBUpperEnvironment(conf);
		try {
			this.env.buildEnvironment(conf, false);
		} catch(Exception e) {
			e.printStackTrace();
		} 
	}

	public long getEntityCount() {
		if (conceptCount == -1)
			conceptCount = env.getDbConcepts().getDatabaseSize();
		return conceptCount;
	}

	/**
	 * Return the concept object corresponding to a given wikidata ID
	 */
	public Concept getConcept(String wikidataId) {
		if (env.getDbConcepts().retrieve(wikidataId) == null) 
			return null;
		else
			return new Concept(env, wikidataId);
	}

	/**
	 * Return the page id corresponding to a given concept id and a target lang
	 */
	public Integer getPageIdByLang(String wikidataId, String lang) {
		if (env.getDbConcepts().retrieve(wikidataId) == null) 
			return null;
		else {
			Concept concept = new Concept(env, wikidataId);
			return concept.getPageIdByLang(lang);
		}
	}

	/**
	 * Return the defintiion of a property 
	 */
	public Property getProperty(String propertyId) {
		return env.getDbProperties().retrieve(propertyId);
	}

	/**
	 * Return the list of relations associated to a given concept id
	 */
	public List<Statement> getStatements(String wikidataId) {
		System.out.println("get statements for: " + wikidataId);
		List<Statement> statements = env.getDbStatements().retrieve(wikidataId);
		System.out.println(statements.size() + " statements: ");

		return env.getDbStatements().retrieve(wikidataId);
	}
}