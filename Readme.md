# Project Documentation


- [Team Information]
| Name                  | Roll Number | GitHub ID            |
|-----------------------|-------------|----------------------|
| Ali Iqbal             | 27100129    | aliiqbal07           |
| Abdullah Khaliq       | 27100400    | abdullahkhaliq12     |
| M. Abdullah Iqbal     | 27100457    | abdullahiqbal27100457|
| Hassan Fayyaz         | 27100397    | HassanFyyz           |
| M. Zain ul Abideen    | 27100180    | Zain100796           |

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
  - [Wireframes – Project Part 2](#wireframes--project-part-2)
  - [Wireframes – Project Part 3](#wireframes--project-part-3)

---

## Team Information
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
- [ ] Embed Figma UI screenshots into the Wireframes section

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
_Add screenshots or links to wireframe images._

#### **Phase 1: User Onboarding & Identity**
* **User Story 01: Student Registration**
  * [View Interactive Prototype](https://droop-area-07497312.figma.site/)
  * **Description:** This screen handles the initial student signup, goal setting, and subject selection.
* **User Story 02: Tutor Profile Creation**
  * [View Interactive Prototype](https://upbeat-type-80148988.figma.site/)
  * **Description:** This screen allows tutors to set their rates, bio, and expertise levels.

### Wireframes – Project Part 3
_Add screenshots or links to wireframe images._
