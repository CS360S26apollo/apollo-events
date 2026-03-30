# Project Documentation


- [Team Information](#team-information)

- [Meeting Minutes](#meeting-minutes)
  - [Meeting – Feb 23, 2026](#meeting--feb-23-2026)
  - [Meeting – Mar 2, 2026](#meeting--mar-2-2026)
  - [Meeting – Mar 8, 2026](#meeting--mar-8-2026)

- [UML Diagrams](#uml-diagrams)

- [Object-Oriented Analysis (CRC Cards)](#object-oriented-analysis)

- [Product Backlog](#product-backlog)
  - [Product Backlog – Project Part 1](#product-backlog--project-part-1)
  - [Product Backlog – Project Part 2](#product-backlog--project-part-2)
  - [Product Backlog – Project Part 3](#product-backlog--project-part-3)

- [Wireframes](#wireframes)
  - [Wireframes – Project Part 1](#wireframes--project-part-1)
  - [Wireframes – Project Part 2 (Figma)](#wireframes--project-part-2) 
  - [Wireframes – Project Part 3](#wireframes--project-part-3)

---

## Team Information <a name="team-information"></a>
- Team Name: apollo

| Name                  | Roll Number | GitHub ID            |
|-----------------------|-------------|----------------------|
| Ali Iqbal             | 27100129    | aliiqbal07           |
| Abdullah Khaliq       | 27100400    | abdullahkhaliq12     |
| M. Abdullah Iqbal     | 27100457    | abdullahiqbal27100457|
| Hassan Fayyaz         | 27100397    | HassanFyyz           |
| M. Zain ul Abideen    | 27100180    | Zain100796           |

---

## Meeting Minutes

### Meeting – Feb 23, 2026

#### Date
Monday, February 23, 2026

#### Attendance
- Ali Iqbal  
- Abdullah Khaliq  
- M. Abdullah Iqbal  
- Hassan Fayyaz  
- M. Zain ul Abideen  

---

#### Key Takeaways
- Initial discussion held with the TA regarding project progress and setup.
- Figma screens were discussed.
- The team was instructed to add the professor and TA to the GitHub repository.
- The repository was to be made private.
- Relevant links for wireframes and product backlog were shared.

---

#### Discussion Points
- Figma screen designs
- GitHub repository access and privacy settings
- Wireframe-related resources
- Product backlog-related resources

---

#### Action Items
- [ ] Finalize and improve Figma screens  
- [ ] Add professor and TA to the GitHub repository  
- [ ] Make the GitHub repository private  
- [ ] Review shared links for wireframes  
- [ ] Review shared links for product backlog  

---

### Meeting – Mar 2, 2026

#### Date
Monday, March 2, 2026

#### Attendance
- Ali Iqbal  
- Abdullah Khaliq  
- M. Abdullah Iqbal  
- Hassan Fayyaz  
- M. Zain ul Abideen  

---

#### Key Takeaways
- A general overview discussion of the project progress was conducted.
- The TA reviewed the current progress of the team.
- Clarifications were provided regarding the Figma screens.

---

#### Discussion Points
- Current progress overview
- Feedback on ongoing work
- Clarifications on Figma screens

---

#### Action Items
- [ ] Incorporate TA feedback into Figma screens  
- [ ] Continue progress on the assigned project tasks  
- [ ] Update documentation as the project moves forward  

---

### Meeting – Mar 8, 2026

#### Date
Sunday, March 8, 2026

#### Attendance
- Ali Iqbal  
- Abdullah Khaliq  
- M. Abdullah Iqbal  
- Hassan Fayyaz  
- M. Zain ul Abideen  

---

#### Key Takeaways
- Completed the Object Oriented Analysis by consolidating team ideas into a 9 class CRC card structure.
- Updated the Phase 2 Product Backlog to include mandatory columns for Story Points, Risk, and Checkpoint tracking.
- Clarifications were provided regarding the Figma screens.

---

#### Discussion Points
- Defining the boundaries between the Wallet and EscrowManager to handle token security.
- Categorizing the RankingEngine and Payment logic as "High Risk" due to technical complexity.
- Feedback on ongoing work
- Ensuring all diagrams are embedded as images directly in the README

---

#### Action Items
- [ ] Finalize CRC card image and upload to /doc folder
- [ ] Map 25 user stories to class rationales for requirement coverage  
- [ ] Populate the Part 2 Product Backlog table with Priority and Points
- [ ] Embed Figma UI URLS into the Wireframes section with description

---

### Meeting – Mar 7, 2026

#### Date
Tuesday, March 7, 2026

#### Attendance
Ali Iqbal  
Abdullah Khaliq  
M. Abdullah Iqbal  
Hassan Fayyaz  
M. Zain ul Abideen  

---

#### Key Takeaways
A detailed demo was presented to the TA showcasing the work completed in Project Phase 2. Each team member presented their respective contributions: Abdullah Khaliq acted as the general presenter, providing an overall walkthrough of the project; M. Abdullah Iqbal presented the CRC cards; Hassan Fayyaz demonstrated the Figma screens; and Ali Iqbal and M. Zain ul Abideen provided a detailed explanation of the user stories, including value-added features such as the token-based system.

---

#### Discussion Points
- Demonstration of Project Phase 2 progress  

---

#### Action Items
- Continue working on the next phase of the project  

---

### Meeting – Mar 27, 2026

#### Date
Friday, March 27, 2026

#### Attendance
Ali Iqbal  
Abdullah Khaliq  
M. Abdullah Iqbal  
Hassan Fayyaz  
M. Zain ul Abideen  

---

#### Key Takeaways
The team discussed the approach for Project Phase 3 and clarified the overall direction with the TA. Feedback was requested regarding the team’s strengths and weaknesses from Phase 2 to better plan improvements for Phase 3. Confusions were addressed regarding whether to structure the work epic-wise or user story-wise, and guidance was provided to ensure a more organized and effective execution strategy.

---

#### Discussion Points
- Planning approach for Phase 3  
- Feedback on strengths and weaknesses from Phase 2  
- Clarification on working structure (epic-wise vs user story-wise)  

---

#### Action Items
- Start a sprint for Phase 3

---


## UML Diagrams
_Add UML diagrams here or link images from the repository._

---

## Object-Oriented Analysis
![CRC Cards](./doc/crc_cards.png)

### Class Rationales
  - Account: Serves as the security and identity hub, centralizing authentication logic and the "Verified" badge system to ensure platform trust (US 01, 04).
  - Student: Encapsulates learner-specific behaviors, focusing on setting educational goals, preferences, and the booking lifecycle (US 05, 07, 18).
  - Tutor: Manages professional provider attributes, including subject expertise, pricing models, and post-session pedagogical feedback (US 02, 17, 25).
  - Session: Acts as the central state machine and primary controller, managing the transition from request to completion while enforcing system-wide rules (US 08, 16).
  - AvailabilityManager: Decouples complex scheduling logic from user profiles to prevent double booking and automatically generate bookable slots (US 12, 13, 14).
  - RankingEngine: Implements the recommendation logic by matching student preferences against tutor performance metrics like rating and responsiveness (US 05, 06).
  - Wallet: Handles the financial data layer, managing real time token balances, purchase transactions, and tutor withdrawal conversions (US 23, 25).
  - EscrowManager: Mitigates financial risk by holding tokens in a protected "Pending" state, releasing them only upon verified session completion (US 24).
  - Review: Maintains community quality standards by linking feedback directly to verified sessions and providing a reporting mechanism for inappropriate content (US 19, 21).

---

## Product Backlog

### Product Backlog – Project Part 1
| ID | User Story | Priority | Status |
|----|------------|----------|--------|

### Product Backlog – Project Part 2
| ID | User Story | Priority | Status | Risk | Points | Checkpoint |
|:---|:---|:---|:---|:---|:---|:---|
| **US 01** | Student Signup: Create account with goals/subjects | High | Open | Low | 3 | Half |
| **US 02** | Tutor Signup: Create profile with bio/rates | High | Open | Low | 3 | Half |
| **US 03** | Edit Profile: Manage visibility and accuracy | Medium | Open | Low | 2 | Half |
| **US 08** | Request a Session: Student sends request with goals | High | Open | Medium | 5 | Half |
| **US 09** | Tutor Response: Accept, decline, or counter-offer | High | Open | Medium | 5 | Half |
| **US 10** | Track Request Status: View pending/accepted status | Medium | Open | Low | 3 | Half |
| **US 13** | Tutor Availability: Set weekly hours/breaks | High | Open | Medium | 5 | Half |
| **US 16** | Session Lifecycle: Track from request to completion | High | Open | Medium | 5 | Half |
| **US 04** | Verification Badge: Upload ID for "Verified" status | Medium | Open | Medium | 5 | Final |
| **US 05** | Recommended Tutors: View tutors based on needs | High | Open | High | 8 | Final |
| **US 06** | Ranking Logic: System ranks by rating/responsiveness | High | Open | High | 13 | Final |
| **US 07** | Matching Preferences: Filter by budget/level/type | Medium | Open | Medium | 5 | Final |
| **US 11** | Auto-Expire: System clears old pending requests | Low | Open | Medium | 3 | Final |
| **US 12** | Instant Book: Book slots without tutor approval | Medium | Open | Medium | 5 | Final |
| **US 14** | Prevent Double-Booking: Detect scheduling conflicts | High | Open | High | 8 | Final |
| **US 15** | Reschedule/Cancel: Change bookings within rules | Medium | Open | Medium | 5 | Final |
| **US 17** | Session Notes: Tutor adds outcomes/action items | Low | Open | Low | 3 | Final |
| **US 18** | Progress Tracking: View milestones/feedback trends | Medium | Open | Medium | 5 | Final |
| **US 19** | Rate & Review: Student provides feedback post-session | Low | Open | Low | 3 | Final |
| **US 20** | Verified Reviews: One review per completed session | Medium | Open | Medium | 5 | Final |
| **US 21** | Report Reviews: Flag inappropriate/suspicious content | Low | Open | Low | 3 | Final |
| **US 22** | Review Sorting: Sort by helpfulness/quality | Low | Open | Low | 3 | Final |
| **US 23** | Buy Tokens: Purchase and load in-app wallet | High | Open | High | 8 | Final |
| **US 24** | Escrow Hold: Tokens held until session completion | High | Open | High | 13 | Final |
| **US 25** | Token Withdrawal: Tutor requests payout of earnings | High | Open | High | 8 | Final |

### Product Backlog – Project Part 3
| ID | User Story | Priority | Status |
|----|------------|----------|--------|

---


## Wireframes

### Wireframes – Project Part 1
_Add screenshots or links to wireframe images._

### Wireframes – Project Part 2

#### **Phase 1: User Onboarding & Identity**
* **User Story 01: Student Registration**
  * [US 01 - figma screen](https://droop-area-07497312.figma.site/)
  * **Description:** This screen handles the initial student signup, goal setting, and subject selection.
* **User Story 02: Tutor Profile Creation**
  * [US 02 - figma screen](https://upbeat-type-80148988.figma.site/)
  * **Description:** This screen allows tutors to set their rates, bio, and expertise levels.
* **User Story 03: Profile Visibility Management**
  * [US 03 - figma screen](https://disc-nebula-91631195.figma.site/)
  * **Description:** Interface for users to toggle profile privacy and update account information.
* **User Story 04: Identity Verification Badge**
  * [US 04 - figma screen](https://wool-tempo-72920640.figma.site/)
  * **Description:** Upload portal for official ID documents to earn the "Verified" badge for trust.
  * #### **Phase 2: Discovery & Matching Logic**
* **User Story 05: Tutor Recommendations**
  * [US 05 - figma screen](https://frame-theme-83662306.figma.site/)
  * **Description:** A personalized dashboard showing tutors that match the student's specific learning goals.
* **User Story 06: Ranking Engine Display**
  * [US 06 - figma screen](https://type-azalea-96297080.figma.site/)
  * **Description:** Dynamic list of tutors sorted by rating, responsiveness, and subject relevance.
* **User Story 07: Advanced Matching Filters**
  * [US 07 - figma screen](https://cell-apron-07825627.figma.site/)
  * **Description:** Detailed search interface to filter tutors by budget, language, and session level.
#### **Phase 3: Scheduling & The Booking Lifecycle**
* **User Story 08: Session Request Submission**
  * [US 08 - figma screen](https://retina-step-47133644.figma.site/)
  * **Description:** Form for students to send session requests with specific topics and desired times.
* **User Story 09: Tutor Request Management**
  * [US 09 - figma screen](https://snow-party-78133997.figma.site/)
  * **Description:** Tutor side dashboard to accept, decline, or suggest counter offers for session requests.
* **User Story 10: Request Status Tracking**
  * [US 10 - figma screen](https://glass-ruler-66720644.figma.site/)
  * **Description:** Real time tracking of request states (Pending, Accepted, or Declined) for both users.
* **User Story 11: Request Auto-Expiration**
  * [US 11 - figma screen](https://maroon-action-89075264.figma.site/)
  * **Description:** Visual feedback/notification screen for requests that have expired due to inactivity.
* **User Story 12: Instant Booking Portal**
  * [US 12 - figma screen](https://whirl-mop-53947330.figma.site/)
  * **Description:** Interface allowing students to book available slots immediately without tutor manual approval.
* **User Story 13: Availability Calendar Manager**
  * [US 13 - figma screen](https://upload-self-32625465.figma.site/)
  * **Description:** Calendar tool for tutors to set their working hours, breaks, and unavailable dates.
* **User Story 14: Double-Booking Prevention**
  * [US 14 - figma screen](https://zebra-mentor-49868617.figma.site/)
  * **Description:** Error state and conflict detection alerts that prevent scheduling sessions during busy slots.
* **User Story 15: Cancellation & Rescheduling**
  * [US 15 - figma screen](https://pry-beige-43915897.figma.site/)
  * **Description:** Management screen for modifying or cancelling bookings according to platform rules.
* **User Story 16: Active Session Lifecycle**
  * [US 16 - figma screen](https://veggie-oats-68762995.figma.site/)
  * **Description:** Monitoring screen for ongoing sessions, showing time remaining and status updates.
#### **Phase 4: Feedback & Progress Monitoring**
* **User Story 17: Post-Session Tutor Notes**
  * [US 17 - figma screen](https://import-done-88623223.figma.site/)
  * **Description:** Form for tutors to record session outcomes, homework, and student feedback.
* **User Story 18: Student Progress Dashboard**
  * [US 18 - figma screen](https://wise-taupe-57553558.figma.site/)
  * **Description:** Visual representation of student milestones, hours completed, and learning trends.
* **User Story 19: Student Rating & Review**
  * [US 19 - figma screen](https://koala-fix-35034417.figma.site/)
  * **Description:** Interactive rating system for students to provide qualitative feedback on tutors.
* **User Story 20: Verified Review Verification**
  * [US 20 - figma screen](https://modify-kite-56382271.figma.site/)
  * **Description:** Badge system ensuring only students who completed a session can post a review.
* **User Story 21: Content Moderation & Reporting**
  * [US 21 - figma screen](https://tower-bass-85856634.figma.site/)
  * **Description:** Reporting tool to flag inappropriate reviews or suspicious profiles for admin review.
* **User Story 22: Review Filtering & Sorting**
  * [US 22 - figma screen](https://jazz-os-97737612.figma.site/)
  * **Description:** Utility to sort reviews by most recent, highest rating, or community helpfulness.
#### **Phase 5: Financial Lifecycle & Token Security**
* **User Story 23: Token Purchase & Wallet Balance**
  * [US 23 - figma screen](https://photo-goat-19100088.figma.site/)
  * **Description:** Secure payment gateway for students to buy platform tokens and view balances.
* **User Story 24: Escrow Hold & Payment Security**
  * [US 24 - figma screen](https://cerise-base-50475017.figma.site/)
  * **Description:** Notification screen showing tokens being held securely until session completion.
* **User Story 25: Tutor Token Withdrawal**
  * [US 25 - figma screen](https://grain-verify-90660343.figma.site/)
  * **Description:** Professional portal for tutors to request payouts and convert tokens to currency.
    

### Wireframes – Project Part 3
_Add screenshots or links to wireframe images._
