package org.keycloak.representations.openid_federation;

import java.util.ArrayList;
import java.util.List;

public class TrustChainForExplicit {

//    private List<String> chain;

    private List<EntityStatement> parsedChain;

    private RPMetadataPolicy combinedPolicy;

    private String trustAnchorId;

    private String leafId;

    public TrustChainForExplicit() {
//        chain = new ArrayList<>();
        parsedChain = new ArrayList<>();
        combinedPolicy = new RPMetadataPolicy();
    }

//    public List<String> getChain() {
//        return chain;
//    }
//
//    public void setChain(List<String> chain) {
//        this.chain = chain;
//    }

    public List<EntityStatement> getParsedChain() {
        return parsedChain;
    }

    public void setParsedChain(List<EntityStatement> parsedChain) {
        this.parsedChain = parsedChain;
    }

    public RPMetadataPolicy getCombinedPolicy() {
        return combinedPolicy;
    }

    public void setCombinedPolicy(RPMetadataPolicy combinedPolicy) {
        this.combinedPolicy = combinedPolicy;
    }

    public String getTrustAnchorId() {
        return trustAnchorId;
    }

    public void setTrustAnchorId(String trustAnchorId) {
        this.trustAnchorId = trustAnchorId;
    }

    public String getLeafId() {
        return leafId;
    }

    public void setLeafId(String leafId) {
        this.leafId = leafId;
    }
}
