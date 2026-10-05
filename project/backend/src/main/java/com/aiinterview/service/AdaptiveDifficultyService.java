package com.aiinterview.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Section III-E: Adaptive Difficulty Controller.
 *
 * Implements Eq. 3:
 *
 *     d_(t+1) = clip( d_t + eta * (p_t - theta), d_min, d_max )
 *
 * where d_t is the current difficulty, p_t is the candidate's performance
 * score on the current question, theta is the target-performance threshold,
 * eta is the step-size / learning-rate, clipped to [d_min, d_max].
 */
@Service
public class AdaptiveDifficultyService {

    @Value("${adaptive.difficulty.min:1}")
    private int dMin;

    @Value("${adaptive.difficulty.max:5}")
    private int dMax;

    @Value("${adaptive.difficulty.target-threshold:0.6}")
    private double theta;

    @Value("${adaptive.difficulty.learning-rate:2.0}")
    private double eta;

    /** Eq. 3: computes the next difficulty level given current level and performance p_t in [0,1]. */
    public int nextDifficulty(int currentDifficulty, double performanceScore) {
        double raw = currentDifficulty + eta * (performanceScore - theta);
        int next = (int) Math.round(raw);
        return Math.max(dMin, Math.min(dMax, next));
    }

    public int getMin() { return dMin; }
    public int getMax() { return dMax; }
}
