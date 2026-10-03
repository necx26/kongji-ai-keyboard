package com.kongji.aikeyboard;

/** Shared ranking policy for local word memory and context pairs. */
final class LearningWeights {
    static final long HALF_LIFE_MS = 30L * 24 * 60 * 60 * 1000;
    private static final int MAX_EFFECTIVE_HITS = 64;

    private LearningWeights() {}

    static double strength(int hits, long updated, long now) {
        if (hits <= 0) return 0;
        // Old habits remain available, but recent choices can catch up.
        long age = updated <= 0 ? 0 : Math.max(0, now - updated);
        double freshness = .25 + .75 * Math.pow(.5, (double) age / HALF_LIFE_MS);
        return Math.log1p(Math.min(hits, MAX_EFFECTIVE_HITS) * freshness);
    }
}
