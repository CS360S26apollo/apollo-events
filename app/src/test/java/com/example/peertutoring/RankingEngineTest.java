package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        List<Tutor> ranked = RankingEngine.rankTutors(tutors, studentSubjects, null);

        int bobIndex  = indexOf(ranked, "Bob Wilson");
        int johnIndex = indexOf(ranked, "John Doe");
        assertTrue("Full subject match (John) should rank above no match (Bob)",
                johnIndex < bobIndex);
    }

    @Test
    public void testEmptyTutorListReturnsEmpty() {
        List<Tutor> ranked = RankingEngine.rankTutors(new ArrayList<>(), studentSubjects, studentAvailability);
        assertTrue("Empty tutor list should return empty ranked list", ranked.isEmpty());
    }

    @Test
    public void testNullStudentSubjectsDoesNotCrash() {
        // Should not throw; subject score defaults to 0
        List<Tutor> ranked = RankingEngine.rankTutors(tutors, null, studentAvailability);
        assertEquals("All tutors should still be returned when subjects is null",
                tutors.size(), ranked.size());
    }

    @Test
    public void testNullAvailabilityDoesNotCrash() {
        List<Tutor> ranked = RankingEngine.rankTutors(tutors, studentSubjects, null);
        assertEquals("All tutors should still be returned when preferredHours is null",
                tutors.size(), ranked.size());
    }

    @Test
    public void testSingleTutorRanksFirst() {
        List<Tutor> single = new ArrayList<>();
        single.add(tutors.get(0));
        List<Tutor> ranked = RankingEngine.rankTutors(single, studentSubjects, studentAvailability);
        assertEquals(1, ranked.size());
        assertEquals("John Doe", ranked.get(0).getFullName());
    }

    @Test
    public void testTutorWithZeroRatingRanksBelow() {
        Tutor lowRating = new Tutor("T4", "t4@example.com", "Low", "Rater",
                "Bio", "Junior", 20, Arrays.asList("Math", "Physics"));
        lowRating.setRating(0.0);
        lowRating.setResponsivenessScore(0.0);

        List<Tutor> withLow = new ArrayList<>(tutors);
        withLow.add(lowRating);

        List<Tutor> ranked = RankingEngine.rankTutors(withLow, studentSubjects, studentAvailability);

        int lowIndex = indexOf(ranked, "Low Rater");
        // Low Rater has 100% subject match but 0 rating and 0 responsiveness
        // John Doe has 100% subject match, 3.5 rating, 0.8 responsiveness
        // John Doe should rank above Low Rater
        int johnIndex = indexOf(ranked, "John Doe");
        assertTrue("John Doe (rating 3.5, resp 0.8) should rank above Low Rater (rating 0, resp 0)",
                johnIndex < lowIndex);
    }

    @Test
    public void testRankingPreservesAllTutors() {
        List<Tutor> ranked = RankingEngine.rankTutors(tutors, studentSubjects, studentAvailability);
        assertEquals("Ranking should not drop any tutors", tutors.size(), ranked.size());
    }

    @Test
    public void testRankingWithEmptyStudentSubjectsScoresZeroSubjectMatch() {
        List<String> noSubjects = new ArrayList<>();
        List<Tutor> ranked = RankingEngine.rankTutors(tutors, noSubjects, null);
        // All tutors get 0 subject score; order determined by rating + responsiveness
        // Bob Wilson: rating 5.0 (1.0 * 0.3) + responsiveness 1.0 (1.0 * 0.2) = 0.5
        // Jane Smith: rating 4.8 (0.96 * 0.3) + responsiveness 0.9 (0.9 * 0.2) = 0.468
        // John Doe:   rating 3.5 (0.7 * 0.3) + responsiveness 0.8 (0.8 * 0.2) = 0.37
        int bobIndex  = indexOf(ranked, "Bob Wilson");
        int johnIndex = indexOf(ranked, "John Doe");
        assertTrue("With no subject preference, highest rated tutor should rank first",
                bobIndex < johnIndex);
    }

    // ── Helper ────────────────────────────────────────────────

    private int indexOf(List<Tutor> list, String fullName) {
        for (int i = 0; i < list.size(); i++) {
            if (fullName.equals(list.get(i).getFullName())) return i;
        }
        return -1;
    }
}
