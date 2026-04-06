package com.example.peertutoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.peertutoring.models.SessionRequest;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * Unit tests for User Story 16: Session Lifecycle Tracking.
 */
public class SessionLifecycleTest {

    @Test
    public void testInitialStatusIsRequested() {
        SessionRequest request = new SessionRequest("student1", "John Doe", "Math", "Algebra", "Learn basics", 60);
        assertEquals(SessionRequest.STATUS_REQUESTED, request.getStatus());
    }

    @Test
    public void testStatusTransitions() {
        SessionRequest request = new SessionRequest();
        
        // requested -> booked
        request.setStatus(SessionRequest.STATUS_BOOKED);
        assertEquals(SessionRequest.STATUS_BOOKED, request.getStatus());
        
        // booked -> completed
        request.setStatus(SessionRequest.STATUS_COMPLETED);
        assertEquals(SessionRequest.STATUS_COMPLETED, request.getStatus());
        
        // Reset and test cancelled
        request.setStatus(SessionRequest.STATUS_CANCELLED);
        assertEquals(SessionRequest.STATUS_CANCELLED, request.getStatus());
        
        // Reset and test no-show
        request.setStatus(SessionRequest.STATUS_NO_SHOW);
        assertEquals(SessionRequest.STATUS_NO_SHOW, request.getStatus());
    }

    @Test
    public void testUpcomingSessionLogic() {
        SessionRequest request = new SessionRequest();
        request.setStatus(SessionRequest.STATUS_BOOKED);
        
        // Set date to tomorrow
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        request.setScheduledDate(cal.getTime());
        
        assertTrue("Session tomorrow should be upcoming", request.isUpcoming());
        
        // Set date to yesterday
        cal.add(Calendar.DAY_OF_YEAR, -2);
        request.setScheduledDate(cal.getTime());
        
        assertFalse("Session yesterday should not be upcoming", request.isUpcoming());
        
        // Change status to completed
        cal.add(Calendar.DAY_OF_YEAR, 2);
        request.setScheduledDate(cal.getTime());
        request.setStatus(SessionRequest.STATUS_COMPLETED);
        
        assertFalse("Completed session should not be upcoming", request.isUpcoming());
    }
}