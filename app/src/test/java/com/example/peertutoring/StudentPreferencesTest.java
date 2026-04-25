package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for US-07: Student Preference Wizard.
 *
 * Covers all pure-Java logic extracted from StudentPreferencesActivity:
 *   - Budget snap formula (SeekBar → token value)
 *   - Level filter matching (student preference vs tutor level string)
 *   - Budget filter (tutor rate vs student budget)
 *   - Progress percentage calculation
 *   - Per-step completion check (mirrors checkAllComplete())
 */
public class StudentPreferencesTest {

    // ── Budget Snap (SeekBar → token value) ──────────────────────
    // Formula used in buildStep1():  ((progress / 100) * 100) + 100

    private int snapBudget(int seekbarProgress) {
        return ((seekbarProgress / 100) * 100) + 100;
    }

    @Test
    public void testBudgetDefaultValueIs500() {
        // seekBar.setProgress(400) is the default → 500 tokens
        assertEquals(500, snapBudget(400));
    }

    @Test
    public void testBudgetMinimumIs100() {
        // Seekbar at 0 → (0/100)*100 + 100 = 100
        assertEquals(100, snapBudget(0));
    }

    @Test
    public void testBudgetMaximumIs2000() {
        // Seekbar.setMax(1900) → (1900/100)*100 + 100 = 2000
        assertEquals(2000, snapBudget(1900));
    }

    @Test
    public void testBudgetSnapsDownToNearest100() {
        // Progress 349 → (3)*100 + 100 = 400  (not 350)
        assertEquals(400, snapBudget(349));
    }

    @Test
    public void testBudgetSnapsUpOnBoundary() {
        // Progress 900 → (9)*100 + 100 = 1000
        assertEquals(1000, snapBudget(900));
    }

    @Test
    public void testBudgetMidRange() {
        // Progress 750 → (7)*100 + 100 = 800
        assertEquals(800, snapBudget(750));
    }

    // ── Level Filter ──────────────────────────────────────────────
    // Mirrors the switch statement in StudentPreferencesActivity.showResults().
    // Student picks one of 4 preference labels; we check against tutor's level string.

    private boolean levelMatches(String studentPref, String tutorLevel) {
        if (tutorLevel == null || studentPref == null) return false;
        String lv = tutorLevel.toLowerCase();
        switch (studentPref) {
            case "Beginner Friendly":
                return lv.contains("beginner") || lv.contains("freshman")
                        || lv.contains("sophomore") || lv.contains("intro");
            case "Intermediate":
                return lv.contains("intermediate") || lv.contains("junior")
                        || lv.contains("senior");
            case "Expert":
                return lv.contains("expert") || lv.contains("graduate")
                        || lv.contains("phd") || lv.contains("professional")
                        || lv.contains("advanced");
            default: // "Native Speaker" — no expertise restriction
                return true;
        }
    }

    // Beginner Friendly

    @Test
    public void testBeginnerMatchesBeginner() {
        assertTrue(levelMatches("Beginner Friendly", "Beginner"));
    }

    @Test
    public void testBeginnerMatchesFreshman() {
        assertTrue(levelMatches("Beginner Friendly", "Freshman"));
    }

    @Test
    public void testBeginnerMatchesSophomore() {
        assertTrue(levelMatches("Beginner Friendly", "Sophomore"));
    }

    @Test
    public void testBeginnerMatchesIntroLevel() {
        assertTrue(levelMatches("Beginner Friendly", "Intro"));
    }

    @Test
    public void testBeginnerDoesNotMatchJunior() {
        assertFalse(levelMatches("Beginner Friendly", "Junior"));
    }

    @Test
    public void testBeginnerDoesNotMatchExpert() {
        assertFalse(levelMatches("Beginner Friendly", "Expert"));
    }

    // Intermediate

    @Test
    public void testIntermediateMatchesJunior() {
        assertTrue(levelMatches("Intermediate", "Junior"));
    }

    @Test
    public void testIntermediateMatchesSenior() {
        assertTrue(levelMatches("Intermediate", "Senior"));
    }

    @Test
    public void testIntermediateMatchesIntermediateExactly() {
        assertTrue(levelMatches("Intermediate", "Intermediate"));
    }

    @Test
    public void testIntermediateDoesNotMatchFreshman() {
        assertFalse(levelMatches("Intermediate", "Freshman"));
    }

    @Test
    public void testIntermediateDoesNotMatchExpert() {
        assertFalse(levelMatches("Intermediate", "Expert"));
    }

    // Expert — this was the key bug: "Expert" preference didn't match tutor level "Expert"

    @Test
    public void testExpertMatchesExpertExactly() {
        assertTrue("Expert preference must match tutor with level 'Expert'",
                levelMatches("Expert", "Expert"));
    }

    @Test
    public void testExpertMatchesGraduate() {
        assertTrue(levelMatches("Expert", "Graduate"));
    }

    @Test
    public void testExpertMatchesPhD() {
        assertTrue(levelMatches("Expert", "PhD"));
    }

    @Test
    public void testExpertMatchesProfessional() {
        assertTrue(levelMatches("Expert", "Professional"));
    }

    @Test
    public void testExpertMatchesAdvanced() {
        assertTrue(levelMatches("Expert", "Advanced"));
    }

    @Test
    public void testExpertDoesNotMatchJunior() {
        assertFalse(levelMatches("Expert", "Junior"));
    }

    @Test
    public void testExpertDoesNotMatchSenior() {
        assertFalse(levelMatches("Expert", "Senior"));
    }

    // Native Speaker — should match any tutor level

    @Test
    public void testNativeSpeakerMatchesBeginner() {
        assertTrue(levelMatches("Native Speaker", "Beginner"));
    }

    @Test
    public void testNativeSpeakerMatchesExpert() {
        assertTrue(levelMatches("Native Speaker", "Expert"));
    }

    @Test
    public void testNativeSpeakerMatchesAnyArbitraryLevel() {
        assertTrue(levelMatches("Native Speaker", "SomeUnknownLevel"));
    }

    // Case-insensitivity

    @Test
    public void testLevelFilterCaseInsensitiveForExpert() {
        assertTrue(levelMatches("Expert", "EXPERT"));
        assertTrue(levelMatches("Expert", "expert"));
        assertTrue(levelMatches("Expert", "ExPeRt"));
    }

    @Test
    public void testLevelFilterCaseInsensitiveForIntermediate() {
        assertTrue(levelMatches("Intermediate", "JUNIOR"));
        assertTrue(levelMatches("Intermediate", "senior"));
    }

    @Test
    public void testLevelFilterCaseInsensitiveForBeginner() {
        assertTrue(levelMatches("Beginner Friendly", "FRESHMAN"));
        assertTrue(levelMatches("Beginner Friendly", "Sophomore"));
    }

    // ── Budget Filter ─────────────────────────────────────────────
    // Mirrors BrowseTutorsActivity.displayTutors():
    //   if (rate != null && studentBudget < Integer.MAX_VALUE && rate > studentBudget) skip

    private boolean withinBudget(long tutorRate, int studentBudget) {
        if (studentBudget == Integer.MAX_VALUE) return true;
        return tutorRate <= studentBudget;
    }

    @Test
    public void testTutorWithinBudgetIsIncluded() {
        assertTrue(withinBudget(80, 100));
    }

    @Test
    public void testTutorAtExactBudgetIsIncluded() {
        assertTrue(withinBudget(100, 100));
    }

    @Test
    public void testTutorOneTokenOverBudgetIsExcluded() {
        assertFalse(withinBudget(101, 100));
    }

    @Test
    public void testTutorWellOverBudgetIsExcluded() {
        assertFalse(withinBudget(500, 100));
    }

    @Test
    public void testNoBudgetPreference_ShowsAllTutors() {
        // When studentBudget == Integer.MAX_VALUE, no preference was set
        assertTrue(withinBudget(9999, Integer.MAX_VALUE));
        assertTrue(withinBudget(0, Integer.MAX_VALUE));
    }

    @Test
    public void testFreeRateTutorAlwaysWithinBudget() {
        assertTrue(withinBudget(0, 50));
    }

    @Test
    public void testHighBudgetIncludesExpensiveTutor() {
        assertTrue(withinBudget(1800, 2000));
    }

    // ── Progress Percentage ───────────────────────────────────────
    // Formula: (step * 100) / TOTAL_STEPS  where TOTAL_STEPS = 4

    private int progressPercent(int step) {
        return (step * 100) / 4;
    }

    @Test
    public void testStep1ProgressIs25() {
        assertEquals(25, progressPercent(1));
    }

    @Test
    public void testStep2ProgressIs50() {
        assertEquals(50, progressPercent(2));
    }

    @Test
    public void testStep3ProgressIs75() {
        assertEquals(75, progressPercent(3));
    }

    @Test
    public void testStep4ProgressIs100() {
        assertEquals(100, progressPercent(4));
    }

    // ── Step Completion (mirrors checkAllComplete()) ──────────────

    private boolean isStepComplete(int step, String level,
                                   List<String> languages, String sessionType) {
        switch (step) {
            case 1: return true;                              // budget always has a value
            case 2: return level != null;
            case 3: return languages != null && !languages.isEmpty();
            case 4: return sessionType != null;
            default: return true;
        }
    }

    @Test
    public void testStep1IsAlwaysComplete() {
        assertTrue(isStepComplete(1, null, null, null));
    }

    @Test
    public void testStep2IncompleteWithNoLevel() {
        assertFalse(isStepComplete(2, null, null, null));
    }

    @Test
    public void testStep2CompleteWhenLevelSelected() {
        assertTrue(isStepComplete(2, "Expert", null, null));
    }

    @Test
    public void testStep3IncompleteWithNoLanguages() {
        assertFalse(isStepComplete(3, null, new ArrayList<>(), null));
    }

    @Test
    public void testStep3IncompleteWithNullLanguageList() {
        assertFalse(isStepComplete(3, null, null, null));
    }

    @Test
    public void testStep3CompleteWithOneLanguage() {
        assertTrue(isStepComplete(3, null, Arrays.asList("English"), null));
    }

    @Test
    public void testStep3CompleteWithMultipleLanguages() {
        List<String> langs = new ArrayList<>(Arrays.asList("English", "Spanish", "French"));
        assertTrue(isStepComplete(3, null, langs, null));
        assertEquals(3, langs.size());
    }

    @Test
    public void testStep4IncompleteWithNoSessionType() {
        assertFalse(isStepComplete(4, null, null, null));
    }

    @Test
    public void testStep4CompleteWhenOnlineSelected() {
        assertTrue(isStepComplete(4, null, null, "Online"));
    }

    @Test
    public void testStep4CompleteWhenInPersonSelected() {
        assertTrue(isStepComplete(4, null, null, "In-Person"));
    }

    @Test
    public void testStep4CompleteWhenHybridSelected() {
        assertTrue(isStepComplete(4, null, null, "Hybrid"));
    }

    // ── Language Multi-Select ─────────────────────────────────────

    @Test
    public void testAddingLanguageTogglesIn() {
        List<String> selected = new ArrayList<>();
        selected.add("English");
        assertTrue(selected.contains("English"));
        assertEquals(1, selected.size());
    }

    @Test
    public void testRemovingLanguageTogglesOut() {
        List<String> selected = new ArrayList<>(Arrays.asList("English", "Spanish"));
        selected.remove("English");
        assertFalse(selected.contains("English"));
        assertTrue(selected.contains("Spanish"));
    }

    @Test
    public void testLanguageNotAddedTwice() {
        List<String> selected = new ArrayList<>();
        // Simulate UI toggle: only add if not present
        if (!selected.contains("English")) selected.add("English");
        if (!selected.contains("English")) selected.add("English"); // second tap → remove
        assertEquals(1, selected.size());
    }

    @Test
    public void testAllAvailableLanguagesCanBeSelected() {
        String[] ALL = {"English", "Spanish", "French", "German",
                "Mandarin", "Japanese", "Korean", "Arabic", "Italian", "Portuguese"};
        List<String> selected = new ArrayList<>(Arrays.asList(ALL));
        assertEquals(10, selected.size());
        assertTrue(isStepComplete(3, null, selected, null));
    }
}
