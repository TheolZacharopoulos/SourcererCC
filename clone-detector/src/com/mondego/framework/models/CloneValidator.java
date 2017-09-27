package com.mondego.framework.models;

import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mondego.application.handlers.SearchHandler;
import com.mondego.framework.controllers.MainController;
import com.mondego.utility.Util;

public class CloneValidator implements Runnable {
    private CandidatePair candidatePair;
    private static final Logger logger = LogManager.getLogger(CloneValidator.class);

    public CloneValidator(CandidatePair candidatePair) {
        // TODO Auto-generated constructor stub
        this.candidatePair = candidatePair;
    }

    @Override
    public void run() {
        try {
            this.process();
        } catch (NoSuchElementException e) {
            logger.error("EXCEPTION CAUGHT::", e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            logger.error("EXCEPTION CAUGHT::", e);
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            logger.error("EXCEPTION CAUGHT::", e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            logger.error("EXCEPTION CAUGHT::", e);
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            logger.error("EXCEPTION CAUGHT::", e);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            logger.error("EXCEPTION CAUGHT::", e);
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            logger.error("EXCEPTION CAUGHT::", e);
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            logger.error("EXCEPTION CAUGHT::", e);
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("EXCEPTION CAUGHT::", e);
        }
    }

    private void process()
            throws InterruptedException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        long startTime = System.nanoTime();
        if (this.candidatePair.simInfo.doc.tokenFrequencies!= null && this.candidatePair.simInfo.doc.tokenFrequencies.size() > 0) {
            int similarity = this.updateSimilarity(this.candidatePair.queryBlock,
                    this.candidatePair.computedThreshold, this.candidatePair.candidateSize, this.candidatePair.simInfo);
            if (similarity > 0) {
                ClonePair cp = new ClonePair(this.candidatePair.queryBlock.getFunctionId(), this.candidatePair.queryBlock.getId(),
                        this.candidatePair.functionIdCandidate, this.candidatePair.candidateId);
                long estimatedTime = System.nanoTime() - startTime;
                logger.debug(MainController.NODE_PREFIX + " CloneValidator, QueryBlock " + this.candidatePair + " in "
                        + estimatedTime / 1000 + " micros");
                SearchHandler.reportCloneQueue.send(cp);
            }
        } else {
            logger.debug("tokens not found for document");
        }
    }

    private int updateSimilarity(QueryBlock queryBlock,
            int computedThreshold, int candidateSize, CandidateSimInfo simInfo) {
        int tokensSeenInCandidate = 0;
        int similarity = simInfo.similarity;
        TokenInfo tokenInfo = null;
        boolean matchFound = false;
        for (TokenFrequency tf : simInfo.doc.tokenFrequencies) {
            if (Util.isSatisfyPosFilter(similarity, queryBlock.getSize(), simInfo.queryMatchPosition, candidateSize,
                    simInfo.candidateMatchPosition, computedThreshold)) {
                // System.out.println("sim: "+ similarity);
                tokensSeenInCandidate += tf.getFrequency();
                // logger.debug("cttseen: "+ tokensSeenInCandidate);
                if (tokensSeenInCandidate > simInfo.candidateMatchPosition) {
                    simInfo.candidateMatchPosition = tokensSeenInCandidate;
                    matchFound = false;
                    if (simInfo.queryMatchPosition < queryBlock.getPrefixMapSize()) {
                        // check in prefix
                        if (queryBlock.getPrefixMap().containsKey(tf.getToken().getValue())) {
                            matchFound = true;
                            tokenInfo = queryBlock.getPrefixMap().get(tf.getToken().getValue());
                            similarity = updateSimilarityHelper(simInfo, tokenInfo, similarity, tf.getFrequency(),
                                    tf.getToken().getValue());
                        }
                    }
                    // check in suffix
                    if (!matchFound && queryBlock.getSuffixMap().containsKey(tf.getToken().getValue())) {
                        tokenInfo = queryBlock.getSuffixMap().get(tf.getToken().getValue());
                        similarity = updateSimilarityHelper(simInfo, tokenInfo, similarity, tf.getFrequency(),
                                tf.getToken().getValue());
                    }
                    if (similarity >= computedThreshold) {
                        return similarity;
                    }
                }
            } else {
                break;
            }
        }
        return -1;
    }

    private int updateSimilarityHelper(CandidateSimInfo simInfo, TokenInfo tokenInfo, int similarity,
            int candidatesTokenFreq, String token) {
        simInfo.queryMatchPosition = tokenInfo.getPosition();
        similarity += Math.min(tokenInfo.getFrequency(), candidatesTokenFreq);
        return similarity;
    }
}
