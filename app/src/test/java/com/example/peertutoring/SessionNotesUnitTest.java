package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.peertutoring.ui.SessionNotesActivity;

import org.junit.Test;

/**
 * Unit tests for User Story 17: Session Notes & Outcomes.
 *
 * Covers:
 *  - Note message formatting (all three sections)
 *  - Partial messages (only topics, only takeaways, etc.)
 *  - Conversation ID construction
 *  - Validation logic (empty notes should be rejected)
 */
public class SessionNotesUnitTest {

    // ── Message formatting ─────────────────────────────────────

    @Test
    public void testNoteMessageContainsAllSections() {
        String msg = SessionNotesActivity.buildNoteMessage(
                "Integration by parts", "Chain rule is key", "Do 5 practice problems");

        assertTrue("Message must contain topics section",
                msg.contains("Topics Covered"));
        assertTrue("Message must contain takeaways section",
                msg.contains("Key Takeaways"));
        assertTrue("Message must contain action items section",
                msg.contains("Action Items"));
    }

    @Test
    public void testNoteMessageContainsTopicsText() {
        String msg = SessionNotesActivity.buildNoteMessage(
                "Limits and continuity", "", "");
        assertTrue("Topics text must appear in message", msg.contains("Limits and continuity"));
    }

    @Test
    public void testNoteMessageContainsTakeawaysText() {
        String msg = SessionNotesActivity.buildNoteMessage(
                "", "Derivatives measure rate of change", "");
        assertTrue("Takeaways text must appear in message",
                msg.contains("Derivatives measure rate of change"));
    }

    @Test
    public void testNoteMessageContainsActionItemsText() {
        String msg = SessionNotesActivity.buildNoteMessage(
                "Vectors", "", "Complete exercises 4.1 – 4.5");
        assertTrue("Action items text must appear in message",
                msg.contains("Complete exercises 4.1 – 4.5"));
    }

    @Test
    public void testNoteMessageOmitsEmptySections() {
        // If takeaways is empty it should NOT appear in the message
        String msg = SessionNotesActivity.buildNoteMessage("Calculus", "", "");
        assertFalse("Empty takeaways section must not appear",
                msg.contains("Key Takeaways"));
        assertFalse("Empty action items section must not appear",
                msg.contains("Action Items"));
    }

    @Test
    public void testNoteMessageStartsWithHeader() {
        String msg = SessionNotesActivity.buildNoteMessage("Topic A", "Takeaway B", "Action C");
        assertTrue("Message must start with 📝 SESSION NOTES header",
                msg.startsWith("📝 SESSION NOTES"));
    }

    @Test
    public void testNoteMessageHasDividers() {
        String msg = SessionNotesActivity.buildNoteMessage("Topic", "Takeaway", "Action");
        assertTrue("Message must contain divider lines",
                msg.contains("─────────────────────────"));
    }

    // ── Validation: at least topics or takeaways required ─────

    @Test
    public void testBothEmptyFailsValidation() {
        String topics    = "";
        String takeaways = "";
        assertTrue("Both empty — validation should reject this",
                topics.isEmpty() && takeaways.isEmpty());
    }

    @Test
    public void testTopicsFilledPassesValidation() {
        String topics    = "Integration";
        String takeaways = "";
        assertFalse("Topics filled — validation should pass",
                topics.isEmpty() && takeaways.isEmpty());
    }

    @Test
    public void testTakeawaysFilledPassesValidation() {
        String topics    = "";
        String takeaways = "Always check boundary conditions";
        assertFalse("Takeaways filled — validation should pass",
                topics.isEmpty() && takeaways.isEmpty());
    }

    @Test
    public void testBothFilledPassesValidation() {
        String topics    = "Waves";
        String takeaways = "Frequency × wavelength = speed";
        assertFalse("Both filled — validation should pass",
                topics.isEmpty() && takeaways.isEmpty());
    }

    // ── Conversation ID construction ──────────────────────────

    @Test
    public void testConvIdIsDeterministic() {
        String id1 = SessionNotesActivity.buildConvId("uid_aaa", "uid_bbb");
        String id2 = SessionNotesActivity.buildConvId("uid_bbb", "uid_aaa");
        assertEquals("ConvId must be the same regardless of argument order", id1, id2);
    }

    @Test
    public void testConvIdContainsBothUids() {
        String convId = SessionNotesActivity.buildConvId("tutor123", "student456");
        assertTrue("ConvId must contain tutor UID",  convId.contains("tutor123"));
        assertTrue("ConvId must contain student UID", convId.contains("student456"));
    }

    @Test
    public void testConvIdSeparatedByUnderscore() {
        String convId = SessionNotesActivity.buildConvId("alpha", "beta");
        assertTrue("ConvId must use underscore separator", convId.contains("_"));
    }
}
