package com.example.peertutoring.utils;

import com.example.peertutoring.models.Tutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * US 06: Ranking Logic.
 * Implementation of the system requirement to rank tutors based on multiple factors:
 * - Subject Relevance (Match)
 * - Tutor Rating
 * - Responsiveness
 * - Availability Compatibility
 */
public class RankingEngine {

    /**
     * Ranks a list of tutors based on student preferences.
     * 
     * @param tutors List of tutors to rank.
     * @param studentSubjects List of subjects the student is interested in.
     * @param preferredHours Map of student's preferred hours (e.g., {"mon": [9, 10]})
     * @return Sorted list of tutors.
     */
    public static List<Tutor> rankTutors(List<Tutor> tutors, 
                                         List<String> studentSubjects, 
                                         Map<String, List<Integer>> preferredHours) {
        
        List<Tutor> rankedTutors = new ArrayList<>(tutors);

        Collections.sort(rankedTutors, new Comparator<Tutor>() {
            @Override
            public int compare(Tutor t1, Tutor t2) {
                double score1 = calculateScore(t1, studentSubjects, preferredHours);
                double score2 = calculateScore(t2, studentSubjects, preferredHours);
                return Double.compare(score2, score1); // Descending order
            }
        });

        return rankedTutors;
    }

    /**
     * Calculates a weighted score for a tutor.
     * Weights: Subject Match (40%), Rating (30%), Responsiveness (20%), Availability (10%)
     */
    private static double calculateScore(Tutor tutor, 
                                         List<String> studentSubjects, 
                                         Map<String, List<Integer>> preferredHours) {
        
        // 1. Subject Match Score (0.0 - 1.0)
        double subjectScore = 0;
        if (studentSubjects != null && !studentSubjects.isEmpty() && tutor.getSubjects() != null) {
            int matches = 0;
            for (String sub : studentSubjects) {
                if (tutor.getSubjects().contains(sub)) matches++;
            }
            subjectScore = (double) matches / studentSubjects.size();
        }

        // 2. Rating Score (0.0 - 1.0)
        double ratingScore = tutor.getRating() / 5.0;

        // 3. Responsiveness Score (0.0 - 1.0)
        double responsivenessScore = tutor.getResponsivenessScore();

        // 4. Availability Compatibility (0.0 - 1.0)
        double availabilityScore = 0;
        if (preferredHours != null && tutor.getAvailability() != null) {
            int totalPreferred = 0;
            int matches = 0;
            for (Map.Entry<String, List<Integer>> entry : preferredHours.entrySet()) {
                String day = entry.getKey();
                List<Integer> hours = entry.getValue();
                totalPreferred += hours.size();
                
                List<Integer> tutorHours = tutor.getAvailability().get(day);
                if (tutorHours != null) {
                    for (Integer h : hours) {
                        if (tutorHours.contains(h)) matches++;
                    }
                }
            }
            if (totalPreferred > 0) {
                availabilityScore = (double) matches / totalPreferred;
            }
        }

        // Weighted Average
        return (subjectScore * 0.4) + (ratingScore * 0.3) + 
               (responsivenessScore * 0.2) + (availabilityScore * 0.1);
    }
}
