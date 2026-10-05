package com.aiinterview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the AI-Based Interview and Coding Assessment System backend.
 *
 * Modules exposed via REST (see controller package), mirroring the system
 * architecture in Section III of the accompanying paper:
 *   A. Resume Analyzer
 *   B. Personalized Question Generation
 *   C. MetaHuman Interviewer (delivery/orchestration endpoints; the actual
 *      3D avatar rendering happens client-side / in Unreal Engine)
 *   D. Speech-to-Text and Answer Analysis
 *   E. Adaptive Difficulty Controller
 *   F. Coding Question Generator and Code Editor
 *   G. Performance Report Generation
 */
@SpringBootApplication
public class AiInterviewApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiInterviewApplication.class, args);
    }
}
