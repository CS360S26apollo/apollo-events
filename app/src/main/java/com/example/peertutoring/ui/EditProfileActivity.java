package com.example.peertutoring.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import com.example.peertutoring.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User Story 3 — Edit Profile & Privacy Settings
 * User Story 4 — Tutor Verification
 */
public class EditProfileActivity extends AppCompatActivity {

    // ── Firebase
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // ── Header views
    private TextView tvAvatarInitials, tvUserName, tvUserRole;

    // ── Tab views
    private Button tabEditProfile, tabPrivacy, tabVerification;
    private LinearLayout panelEditProfile, panelPrivacy, panelVerification;

    // ── Role toggle
    private Button btnRoleStudent, btnRoleTutor;
    private String currentRole = "student";

    // ── Edit Profile fields
    private TextInputEditText etFirstName, etLastName, etEmail,
            etInstitution, etBio, etRate;
    private TextView tvCharCount;
    private LinearLayout layoutInstitution, layoutBio, layoutRate;
    private ChipGroup chipGroupSubjects;

    // ── Privacy switches
    private SwitchMaterial switchShowName, switchShowInstitution,
            switchShowSubjects, switchShowRate, switchProfileVisible;
    private LinearLayout rowShowInstitution, rowShowRate;

    // ── Role from Firestore
    private String savedRole = "student";

    // ── Subject chip IDs and names (must match activity_edit_profile.xml)
    private static final int[] CHIP_IDS = {
            R.id.chipMathematics, R.id.chipPhysics, R.id.chipChemistry,
            R.id.chipBiology, R.id.chipComputerScience, R.id.chipEnglish,
            R.id.chipHistory, R.id.chipEconomics
    };
    private static final String[] CHIP_NAMES = {
            "Mathematics", "Physics", "Chemistry",
            "Biology", "Computer Science", "English",
            "History", "Economics","Accounting"
    };

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        bindViews();
        setupTabSwitcher();
        setupRoleToggle();
        setupChips();
        setupBioCharCount();
        setupSaveButtons();

        // Back button — null-safe
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Load profile or exit if not logged in
        if (currentUser != null) {
            loadUserProfile(currentUser.getUid());
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // ── Bind all views (all null-safe) ────────────────────────────────────────

    private void bindViews() {
        tvAvatarInitials      = findViewById(R.id.tvAvatarInitials);
        tvUserName            = findViewById(R.id.tvUserName);
        tvUserRole            = findViewById(R.id.tvUserRole);

        tabEditProfile        = findViewById(R.id.tabEditProfile);
        tabPrivacy            = findViewById(R.id.tabPrivacy);
        tabVerification       = findViewById(R.id.tabVerification);
        
        panelEditProfile      = findViewById(R.id.panelEditProfile);
        panelPrivacy          = findViewById(R.id.panelPrivacy);
        panelVerification     = findViewById(R.id.panelVerification);

        btnRoleStudent        = findViewById(R.id.btnRoleStudent);
        btnRoleTutor          = findViewById(R.id.btnRoleTutor);

        etFirstName           = findViewById(R.id.editTextFirstName);
        etLastName            = findViewById(R.id.editTextLastName);
        etEmail               = findViewById(R.id.editTextEmail);
        etInstitution         = findViewById(R.id.editTextInstitution);
        etBio                 = findViewById(R.id.editTextBio);
        etRate                = findViewById(R.id.editTextRate);
        tvCharCount           = findViewById(R.id.tvCharCount);
        layoutInstitution     = findViewById(R.id.layoutInstitution);
        layoutBio             = findViewById(R.id.layoutBio);
        layoutRate            = findViewById(R.id.layoutRate);
        chipGroupSubjects     = findViewById(R.id.chipGroupSubjects);

        switchShowName        = findViewById(R.id.switchShowName);
        switchShowInstitution = findViewById(R.id.switchShowInstitution);
        switchShowSubjects    = findViewById(R.id.switchShowSubjects);
        switchShowRate        = findViewById(R.id.switchShowRate);
        switchProfileVisible  = findViewById(R.id.switchProfileVisible);
        rowShowInstitution    = findViewById(R.id.rowShowInstitution);
        rowShowRate           = findViewById(R.id.rowShowRate);
    }

    // ── Tab switcher ──────────────────────────────────────────────────────────

    private void setupTabSwitcher() {
        if (tabEditProfile != null)  tabEditProfile.setOnClickListener(v -> showTab(0));
        if (tabPrivacy != null)      tabPrivacy.setOnClickListener(v -> showTab(1));
        if (tabVerification != null) tabVerification.setOnClickListener(v -> showTab(2));
    }

    private void showTab(int index) {
        // Panels visibility
        if (panelEditProfile != null)  panelEditProfile.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        if (panelPrivacy != null)      panelPrivacy.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        if (panelVerification != null) panelVerification.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        // Tab styles
        updateTabStyle(tabEditProfile, index == 0);
        updateTabStyle(tabPrivacy, index == 1);
        updateTabStyle(tabVerification, index == 2);
    }

    private void updateTabStyle(Button btn, boolean selected) {
        if (btn == null) return;
        if (selected) {
            btn.setBackground(getDrawable(R.drawable.bg_button_gradient));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackground(null);
            btn.setTextColor(Color.parseColor("#4B5D7A"));
        }
    }

    // ── Role toggle ───────────────────────────────────────────────────────────

    private void setupRoleToggle() {
        if (btnRoleStudent != null) btnRoleStudent.setOnClickListener(v -> applyRoleUI("student"));
        if (btnRoleTutor != null)   btnRoleTutor.setOnClickListener(v -> applyRoleUI("tutor"));
    }

    private void applyRoleUI(String role) {
        currentRole = role;

        // Show/hide fields
        if (layoutInstitution != null)
            layoutInstitution.setVisibility(role.equals("student") ? View.VISIBLE : View.GONE);
        if (layoutBio != null)
            layoutBio.setVisibility(role.equals("tutor") ? View.VISIBLE : View.GONE);
        if (layoutRate != null)
            layoutRate.setVisibility(role.equals("tutor") ? View.VISIBLE : View.GONE);

        // Show/hide privacy rows
        if (rowShowInstitution != null)
            rowShowInstitution.setVisibility(role.equals("student") ? View.VISIBLE : View.GONE);
        if (rowShowRate != null)
            rowShowRate.setVisibility(role.equals("tutor") ? View.VISIBLE : View.GONE);

        // Role button styles
        if (btnRoleStudent != null) {
            if (role.equals("student")) {
                btnRoleStudent.setBackground(getDrawable(R.drawable.bg_button_gradient));
                btnRoleStudent.setTextColor(Color.WHITE);
            } else {
                btnRoleStudent.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                btnRoleStudent.setTextColor(Color.parseColor("#4B5D7A"));
            }
        }
        if (btnRoleTutor != null) {
            if (role.equals("tutor")) {
                btnRoleTutor.setBackground(getDrawable(R.drawable.bg_button_gradient));
                btnRoleTutor.setTextColor(Color.WHITE);
            } else {
                btnRoleTutor.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                btnRoleTutor.setTextColor(Color.parseColor("#4B5D7A"));
            }
        }
    }

    // ── Chips ─────────────────────────────────────────────────────────────────

    private void setupChips() {
        for (int i = 0; i < CHIP_IDS.length; i++) {
            Chip chip = findViewById(CHIP_IDS[i]);
            if (chip == null) continue;
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                chip.setChipBackgroundColor(ColorStateList.valueOf(
                        isChecked
                                ? Color.parseColor("#089A3C")
                                : Color.parseColor("#E0E0E0")
                ));
                chip.setTextColor(isChecked ? Color.WHITE : Color.parseColor("#33476A"));
            });
        }
    }

    private void preSelectChips(List<String> subjects) {
        if (subjects == null) return;
        for (int i = 0; i < CHIP_IDS.length; i++) {
            Chip chip = findViewById(CHIP_IDS[i]);
            if (chip != null && subjects.contains(CHIP_NAMES[i])) {
                chip.setChecked(true);
            }
        }
    }

    private List<String> getSelectedSubjects() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < CHIP_IDS.length; i++) {
            Chip chip = findViewById(CHIP_IDS[i]);
            if (chip != null && chip.isChecked()) {
                selected.add(CHIP_NAMES[i]);
            }
        }
        return selected;
    }

    // ── Bio character count ───────────────────────────────────────────────────

    private void setupBioCharCount() {
        // Guard: etBio or tvCharCount may be null if IDs don't match layout
        if (etBio == null || tvCharCount == null) return;
        etBio.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                tvCharCount.setText(s.length() + "/500 characters");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ── Save buttons ──────────────────────────────────────────────────────────

    private void setupSaveButtons() {
        View btnSaveProfile = findViewById(R.id.btnSaveProfile);
        View btnSavePrivacy = findViewById(R.id.btnSavePrivacy);
        if (btnSaveProfile != null) btnSaveProfile.setOnClickListener(v -> saveProfile());
        if (btnSavePrivacy != null) btnSavePrivacy.setOnClickListener(v -> savePrivacy());
    }

    // ── Load from Firestore ───────────────────────────────────────────────────

    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        populateUI(doc);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @SuppressWarnings("unchecked")
    private void populateUI(DocumentSnapshot doc) {
        // Role
        savedRole   = doc.getString("role") != null ? doc.getString("role") : "student";
        currentRole = savedRole;
        applyRoleUI(currentRole);

        // Name
        String firstName = doc.getString("firstName");
        String lastName  = doc.getString("lastName");
        String fullName  = doc.getString("fullName");

        if (etFirstName != null && firstName != null) etFirstName.setText(firstName);
        if (etLastName  != null && lastName  != null) etLastName.setText(lastName);

        // Avatar initials
        String initials = "";
        if (firstName != null && !firstName.isEmpty()) initials += firstName.charAt(0);
        if (lastName  != null && !lastName.isEmpty())  initials += lastName.charAt(0);
        if (tvAvatarInitials != null) tvAvatarInitials.setText(initials.toUpperCase());

        // Header name + role label
        if (tvUserName != null) tvUserName.setText(fullName != null ? fullName : "");
        if (tvUserRole != null && savedRole.length() > 0) {
            tvUserRole.setText(
                    Character.toUpperCase(savedRole.charAt(0)) + savedRole.substring(1)
            );
        }

        // Email (read-only display)
        if (etEmail != null && currentUser != null && currentUser.getEmail() != null) {
            etEmail.setText(currentUser.getEmail());
        }

        // Student fields
        String institution = doc.getString("institution");
        if (etInstitution != null && institution != null) etInstitution.setText(institution);

        // Tutor fields
        String bio = doc.getString("bio");
        if (etBio != null && bio != null) etBio.setText(bio);

        Long rate = doc.getLong("rate");
        if (etRate != null && rate != null) etRate.setText(String.valueOf(rate));

        // Subjects
        List<String> subjects = (List<String>) doc.get("subjects");
        preSelectChips(subjects);

        // Privacy switches
        Boolean showName   = doc.getBoolean("showName");
        Boolean showInst   = doc.getBoolean("showInstitution");
        Boolean showSubj   = doc.getBoolean("showSubjects");
        Boolean showRate   = doc.getBoolean("showRate");
        Boolean profileVis = doc.getBoolean("profileVisible");

        if (switchShowName        != null && showName   != null) switchShowName.setChecked(showName);
        if (switchShowInstitution != null && showInst   != null) switchShowInstitution.setChecked(showInst);
        if (switchShowSubjects    != null && showSubj   != null) switchShowSubjects.setChecked(showSubj);
        if (switchShowRate        != null && showRate   != null) switchShowRate.setChecked(showRate);
        if (switchProfileVisible  != null && profileVis != null) switchProfileVisible.setChecked(profileVis);
    }

    // ── Save profile to Firestore ─────────────────────────────────────────────

    private void saveProfile() {
        // Guard: must be logged in
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate name
        String firstName = etFirstName != null && etFirstName.getText() != null
                ? etFirstName.getText().toString().trim() : "";
        String lastName  = etLastName  != null && etLastName.getText()  != null
                ? etLastName.getText().toString().trim()  : "";

        if (TextUtils.isEmpty(firstName)) {
            if (etFirstName != null) etFirstName.setError("First name is required");
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            if (etLastName != null) etLastName.setError("Last name is required");
            return;
        }
        if (getSelectedSubjects().isEmpty()) {
            Toast.makeText(this, "Please select at least one subject", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName",  lastName);
        updates.put("fullName",  firstName + " " + lastName);
        updates.put("subjects",  getSelectedSubjects());
        updates.put("role",      currentRole);

        if (currentRole.equals("student")) {
            String institution = etInstitution != null && etInstitution.getText() != null
                    ? etInstitution.getText().toString().trim() : "";
            if (TextUtils.isEmpty(institution)) {
                if (etInstitution != null) etInstitution.setError("Institution is required");
                return;
            }
            updates.put("institution", institution);

        } else {
            String bio     = etBio  != null && etBio.getText()  != null
                    ? etBio.getText().toString().trim()  : "";
            String rateStr = etRate != null && etRate.getText() != null
                    ? etRate.getText().toString().trim() : "";

            if (TextUtils.isEmpty(bio)) {
                if (etBio != null) etBio.setError("Bio is required");
                return;
            }
            if (TextUtils.isEmpty(rateStr)) {
                if (etRate != null) etRate.setError("Rate is required");
                return;
            }
            updates.put("bio",  bio);
            updates.put("rate", Integer.parseInt(rateStr));
        }

        // Use set + merge so it works even if the document is new
        db.collection("users")
                .document(currentUser.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "✅ Profile saved!", Toast.LENGTH_SHORT).show();
                    // Refresh header
                    if (tvUserName != null) tvUserName.setText(firstName + " " + lastName);
                    if (tvAvatarInitials != null && !firstName.isEmpty() && !lastName.isEmpty()) {
                        tvAvatarInitials.setText(
                                ("" + firstName.charAt(0) + lastName.charAt(0)).toUpperCase()
                        );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ── Save privacy settings to Firestore ────────────────────────────────────

    private void savePrivacy() {
        // Guard: must be logged in
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> privacy = new HashMap<>();
        if (switchShowName        != null) privacy.put("showName",        switchShowName.isChecked());
        if (switchShowInstitution != null) privacy.put("showInstitution", switchShowInstitution.isChecked());
        if (switchShowSubjects    != null) privacy.put("showSubjects",    switchShowSubjects.isChecked());
        if (switchShowRate        != null) privacy.put("showRate",        switchShowRate.isChecked());
        if (switchProfileVisible  != null) privacy.put("profileVisible",  switchProfileVisible.isChecked());

        if (privacy.isEmpty()) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use set + merge so it works even if the document is new
        db.collection("users")
                .document(currentUser.getUid())
                .set(privacy, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, " Privacy settings saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}