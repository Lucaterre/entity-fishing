package com.scienceminer.nerd.kb.db;

import com.scienceminer.nerd.exceptions.NerdResourceException;
import com.scienceminer.nerd.kb.Statement;
import org.apache.hadoop.record.CsvRecordInput;
import org.lmdbjava.CursorIterator;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.nio.ByteBuffer.allocateDirect;

public class TaxonDatabase extends StringRecordDatabase<List<String>> {
    private static final Logger logger = LoggerFactory.getLogger(TaxonDatabase.class);

    public TaxonDatabase(KBEnvironment env) {
        super(env, DatabaseType.taxon);
    }

    @Override
    public KBEntry<String, List<String>> deserialiseCsvRecord(
            CsvRecordInput record) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Load the taxon hierarchy index
     */
    public void fillTaxonDbs(ConceptDatabase conceptDb, StatementDatabase statementDb,
                             boolean overwrite) throws Exception {
        if (isLoaded && !overwrite)
            return;
        System.out.println("Loading " + name + " database");

        if (conceptDb == null)
            throw new NerdResourceException("conceptDb not found");

        // iterate through concepts
        KBIterator iter = new KBIterator(conceptDb);
        Txn<ByteBuffer> tx = environment.txnWrite();
        try {
            int nbToAdd = 0;
            int n = 0; // total entities
            int nbTaxon = 0; // total doi found
            while (iter.hasNext()) {
                if (nbToAdd > 10000) {
                    tx.commit();
                    tx.close();
                    tx = environment.txnWrite();
                    nbToAdd = 0;
                }
                CursorIterator.KeyVal<ByteBuffer> entry = iter.next();
                ByteBuffer keyData = entry.key();
                ByteBuffer valueData = entry.val();
                //Page p = null;
                try {
                    String entityId = (String) KBEnvironment.deserialize(keyData);
                    // check the statements for a property P31 (instanceOf) with
                    // value Q16521 (taxon)
                    String superType = null;

                    List<Statement> statements = statementDb.retrieve(entityId);
					/*if ( (statements != null) && (statements.size() > 0) ) {
						for(Statement statement : statements) {
							if (statement.getPropertyId().equals("P31")) {
								superType = statement.getValue();
								//System.out.println("found DOI: " + doi);
							}
						}
					}*/

                    // check the statements for a property P171 (parent taxon)

                    //if ((superType != null) && (superType.equals("Q16521")) )
                    //{
                    List<String> parentTaxons = null;

                    // check the statements for a property P171 (parent taxon)
                    for (Statement statement : statements) {
                        if (statement.getPropertyId().equals("P171")) {
                            String parentTaxon = statement.getValue();
                            //System.out.println("found DOI: " + doi);
                            if (parentTaxons == null)
                                parentTaxons = new ArrayList<String>();
                            if (!parentTaxons.contains(parentTaxon))
                                parentTaxons.add(parentTaxon);
                        }
                    }

                    if (parentTaxons != null) {
                        // we have a taxon
                        nbTaxon++;
                        // store the parent information

                        final ByteBuffer keyBuffer = allocateDirect(environment.getMaxKeySize());
                        keyBuffer.put(KBEnvironment.serialize(entityId)).flip();
                        final byte[] serializedValue = KBEnvironment.serialize(parentTaxons);
                        final ByteBuffer valBuffer = allocateDirect(serializedValue.length);
                        valBuffer.put(serializedValue).flip();
                        db.put(tx, keyBuffer, valBuffer);

                        nbToAdd++;
                    }
                    //}
                } catch (Exception e) {
                    logger.error("fail to write entity description", e);
                }
                n++;
            }
            logger.info("total nb entities visited: " + n);
            logger.info("total nb taxon found: " + nbTaxon);
        } catch (Exception e) {
            logger.error("Error when filling taxon databases", e);
        } finally {
            if (iter != null)
                iter.close();
            tx.commit();
            tx.close();
            isLoaded = true;
        }
    }
}
