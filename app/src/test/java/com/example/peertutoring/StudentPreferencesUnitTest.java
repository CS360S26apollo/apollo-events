package com.example.peertutoring;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit Tests for US-07: Student Preference Wizard
 *
 * Tests cover:
 * - Budget calculation and boundary values
 * - Level mapping logic
 * - Language multi-selection
 * - Session type selection
 * - Completion gate logic (all steps must be done before proceeding)
 * - Tutor filtering based on preferences
 */
public class StudentPreferencesUnitTest {

    // ── Simulated preference model ────────────────────────────

    private int budget;
    private String selectedLevel;
    private List<String> selectedLanguages;
    private String selectedSessionType;

    @Before
    public void setUp() {
        budget = 500;
        selectedLevel = null;
        selectedLanguages = new ArrayList<>();
        selectedSessionType = null;
    }

    // ── Budget Tests ──────────────────────────────────────────

    @Test
    public void testDefaultBudgetIs500() {
        assertEquals(500, budget);
    }

    @Test
    public void testBudgetSnapsToNearestHundred() {
        // Simulate SeekBar progress snapping
        int rawProgress = 437; // should snap to 500
        int snapped = ((rawProgress / 100) * 100) + 100;
        assertEquals(500, snapped);
    }

    @Test
    public void testBudgetMinimumIs100() {
        int rawProgress = 0;
        int snapped = ((rawProgress / 100) * 100) + 100;
        assertEquals(100, snapped);
    }

    @Test
    public void testBudgetMaximumIs2000() {
        int rawProgress = 1900;
        int snapped = ((rawProgress / 100) * 100) + 100;
        assertEquals(2000, snapped);
    }

    @Test
    public void testBudget1500SnapsCorrectly() {
        int rawProgress = 1450;
        int snapped = ((rawProgress / 100) * 100) + 100;
        assertEquals(1500, snapped);
    }

    // ── Level Selection Tests ─────────────────────────────────

    @Test
    public void testLevelSelectionStoresSingleValue() {
        selectedLevel = "Beginner Friendly";
        assertEquals("Beginner Friendly", selectedLevel);
    }

    @Test
    public void testLevelSelectionOverridesPreviousSelection() {
        selectedLevel = "Beginner Friendly";
        selectedLevel = "Expert"; // user changes their mind
        assertEquals("Expert", selectedLevel);
        assertNotEquals("Beginner Friendly", selectedLevel);
    }

    @Test
    public void testAllFourLevelsAreValid() {
        String[] validLevels = {"Beginner Friendly", "Intermediate", "Expert", "Native Speaker"};
        for (String level : validLevels) {
            selectedLevel = level;
            assertNotNull(selectedLevel);
            assertTrue(Arrays.asList(validLevels).contains(selectedLevel));
        }
    }

    @Test
    public void testLevelMappingBeginnerFriendly() {
        // Beginner Friendly maps to Freshman/Sophomore tutors
        String tutorLevel = "Undergraduate - Freshman";
        boolean matches = matchesLevel("Beginner Friendly", tutorLevel);
        assertTrue(matches);
    }

    @Test
    public void testLevelMappingIntermediate() {
        String tutorLevel = "Undergraduate - Senior";
        boolean matches = matchesLevel("Intermediate", tutorLevel);
        assertTrue(matches);
    }

    @Test
    public void testLevelMappingExpert() {
        String tutorLevel = "PhD Candidate";
        boolean matches = matchesLevel("Expert", tutorLevel);
        assertTrue(matches);
    }

    @Test
    public void testLevelMappingNoMismatch() {
        String tutorLevel = "Undergraduate - Freshman";
        boolean matches = matchesLevel("Expert", tutorLevel);
        assertFalse(matches);
    }

    // ── Language Tests ────────────────────────────────────────

    @Test
    public void testCanSelectMultipleLanguages() {
        selectedLanguages.add("English");
        selectedLanguages.add("Arabic");
        selectedLanguages.add("Mandarin");
        assertEquals(3, selectedLanguages.size());
    }

    @Test
    public void testDeselectingLanguageRemovesIt() {
        selectedLanguages.add("English");
        selectedLanguages.add("Spanish");
        selectedLanguages.remove("English");
        assertFalse(selectedLanguages.contains("English"));
        assertTrue(selectedLanguages.contains("Spanish"));
        assertEquals(1, selectedLanguages.size());
    }

    @Test
    public void testCannotAddDuplicateLanguage() {
        // Simulate the UI guard
        String lang = "French";
        if (!selectedLanguages.contains(lang)) selectedLanguages.add(lang);
        if (!selectedLanguages.contains(lang)) selectedLanguages.add(lang);
        assertEquals(1, selectedLanguages.size());
    }

    @Test
    public void testEmptyLanguagesBlocksCompletion() {
        // Step 3 is complete only when at least one language selected
        assertFalse(isStep3Complete());
        selectedLanguages.add("English");
        assertTrue(isStep3Complete());
    }

    // ── Session Type Tests ────────────────────────────────────

    @Test
    public void testSessionTypeOnlineIsValid() {
        selectedSessionType = "Online";
        assertEquals("Online", selectedSessionType);
    }

    @Test
    public void testSessionTypeInPersonIsValid() {
        selectedSessionType = "In-Person";
        assertEquals("In-Person", selectedSessionType);
    }

    @Test
    public void testSessionTypeHybridIsValid() {
        selectedSessionType = "Hybrid";
        assertEquals("Hybrid", selectedSessionType);
    }

    @Test
    public void testSessionTypeOverridesPrevious() {
        selectedSessionType = "Online";
        selectedSessionType = "Hybrid";
        assertEquals("Hybrid", selectedSessionType);
        assertNotEquals("Online", selectedSessionType);
    }

    // ── Completion Gate Tests ─────────────────────────────────

    @Test
    public void testAllStepsRequiredBeforeProceeding() {
        assertFalse(allStepsComplete());

        budget = 800;                           // step 1 always set
        selectedLevel = "Expert";               // step 2
        selectedLanguages.add("English");       // step 3
        selectedSessionType = "Online";         // step 4

        assertTrue(allStepsComplete());
    }

    @Test
    public void testMissingLevelBlocksCompletion() {
        budget = 800;
        selectedLanguages.add("English");
        selectedSessionType = "Online";
        // selectedLevel still null
        assertFalse(allStepsComplete());
    }

    @Test
    public void testMissingSessionTypeBlocksCompletion() {
        budget = 800;
        selectedLevel = "Expert";
        selectedLanguages.add("English");
        // selectedSessionType still null
        assertFalse(allStepsComplete());
    }

    // ── Tutor Filtering Tests ─────────────────────────────────

    @Test
    public void testTutorWithRateAboveBudgetIsExcluded() {
        budget = 500;
        int tutorRate = 600;
        assertFalse(tutorWithinBudget(tutorRate));
    }

    @Test
    public void testTutorWithRateBelowBudgetIsIncluded() {
        budget = 500;
        int tutorRate = 400;
        assertTrue(tutorWithinBudget(tutorRate));
    }

    @Test
    public void testTutorWithRateEqualToBudgetIsIncluded() {
        budget = 500;
        int tutorRate = 500;
        assertTrue(tutorWithinBudget(tutorRate));
    }

    @Test
    public void testTutorFilterReturnsOnlyMatchingBudget() {
        budget = 300;
        int[] tutorRates = {200, 300, 400, 500, 100};
        List<Integer> matched = new ArrayList<>();
        for (int rate : tutorRates) {
            if (tutorWithinBudget(rate)) matched.add(rate);
        }
        assertEquals(3, matched.size()); // 200, 300, 100
        assertFalse(matched.contains(400));
        assertFalse(matched.contains(500));
    }

    // ── Helper methods (mirror activity logic) ─────────────────

    private boolean allStepsComplete() {
        return selectedLevel != null
                && !selectedLanguages.isEmpty()
                && selectedSessionType != null;
    }

    private boolean isStep3Complete() {
        return !selectedLanguages.isEmpty();
    }

    private boolean tutorWithinBudget(int tutorRate) {
        return tutorRate <= budget;
    }

    private boolean matchesLevel(String studentLevel, String tutorLevel) {
        if (studentLevel == null || tutorLevel == null) return false;
        switch (studentLevel) {
            case "Beginner Friendly":
                return tutorLevel.contains("Freshman") || tutorLevel.contains("Sophomore");
            case "Intermediate":
                return tutorLevel.contains("Junior") || tutorLevel.contains("Senior");
            case "Expert":
                return tutorLevel.contains("Graduate") || tutorLevel.contains("PhD")
                        || tutorLevel.contains("Professional");
            case "Native Speaker":
                return true; // show all tutors
            default:
                return false;
        }
    }
}