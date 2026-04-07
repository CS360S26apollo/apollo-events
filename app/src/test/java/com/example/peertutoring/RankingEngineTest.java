package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.peertutoring.models.Tutor;
import com.example.peertutoring.utils.RankingEngine;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for US 06: Ranking Logic.
 * Verifies that tutors are ranked correctly based on subject match, rating,
 * responsiveness, and availability.
 */
public class RankingEngineTest {

    private List<Tutor> tutors;
    private List<String> studentSubjects;
    private Map<String, List<Integer>> studentAvailability;

    @Before
    public void setUp() {
        tutors = new ArrayList<>();

        // Tutor 1: Perfect subject match, lower rating
        Tutor t1 = new Tutor("T1", "t1@example.com", "John", "Doe", "Bio", "Junior", 30, Arrays.asList("Math", "Physics"));
        t1.setRating(3.5);
        t1.setResponsivenessScore(0.8);
        Map<String, List<Integer>> avail1 = new HashMap<>();
        avail1.put("mon", Arrays.asList(9, 10));
        t1.setAvailability(avail1);
        tutors.add(t1);

        // Tutor 2: Partial subject match, higher rating, good availability
        Tutor t2 = new Tutor("T2", "t2@example.com", "Jane", "Smith", "Bio", "Senior", 40, Arrays.asList("Math", "Chemistry"));
        t2.setRating(4.8);
        t2.setResponsivenessScore(0.9);
        Map<String, List<Integer>> avail2 = new HashMap<>();
        avail2.put("mon", Arrays.asList(9, 10, 11));
        t2.setAvailability(avail2);
        tutors.add(t2);

        // Tutor 3: No subject match, perfect rating
        Tutor t3 = new Tutor("T3", "t3@example.com", "Bob", "Wilson", "Bio", "Expert", 50, Arrays.asList("History", "Art"));
        t3.setRating(5.0);
        t3.setResponsivenessScore(1.0);
        tutors.add(t3);

        studentSubjects = Arrays.asList("Math", "Physics");
        studentAvailability = new HashMap<>();
        studentAvailability.put("mon", Arrays.asList(9, 10));
    }

    @Test
    public void testRankingOrder() {
        List<Tutor> ranked = RankingEngine.rankTutors(tutors, studentSubjects, studentAvailability);

        // Expected: Tutor 1 should be high due to perfect subject match (40%) and availability match (10%).
        // Tutor 2 has partial subject match but better rating (30%) and responsiveness (20%).
        
        // Let's verify that the ranking returns the list in the correct order.
        // John Doe (T1) should be first because of the heavy weight on subject match.
        assertEquals("John Doe", ranked.get(0).getFullName());
        assertEquals("Jane Smith", ranked.get(1).getFullName());
        assertEquals("Bob Wilson", ranked.get(2).getFullName());
    }

    @Test
    public void testSubjectMatchWeight() {
        // Even with a lower rating, a tutor with a better subject match should rank higher
        // than a tutor with no match but a perfect rating.
        List<Tutor> ranked = RankingEngine.rankTutors(tutors, studentSubjects, null);
        
        boolean foundMatch = false;
        for (Tutor t : ranked) {
            if (t.getFullName().equals("John Doe")) {
                foundMatch = true;
                break;
            }
        }
        assertTrue(foundMatch);
        // Bob Wilson has 0% subject match, John Doe has 100%. 
        // 1.0 * 0.4 vs 0.0 * 0.4.
        int bobIndex = -1;
        int johnIndex = -1;
        for (int i = 0; i < ranked.size(); i++) {
            if (ranked.get(i).getFullName().equals("Bob Wilson")) bobIndex = i;
            if (ranked.get(i).getFullName().equals("John Doe")) johnIndex = i;
        }
        assertTrue(johnIndex < bobIndex);
    }
}
