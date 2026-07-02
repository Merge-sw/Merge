package com.merge.backend.identity.service;

public class PromotionNotEligibleException extends RuntimeException {

    private final int missingXp;
    private final int missingBuildScore;

    public PromotionNotEligibleException(int missingXp, int missingBuildScore) {
        super("Not eligible for promotion");
        this.missingXp = missingXp;
        this.missingBuildScore = missingBuildScore;
    }

    public int getMissingXp() { return missingXp; }
    public int getMissingBuildScore() { return missingBuildScore; }
}
