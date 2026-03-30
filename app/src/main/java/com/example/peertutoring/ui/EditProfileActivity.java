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

import com.example.peertutoring.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User Story 3
 */
public class EditProfileActivity extends AppCompatActivity {

    // ── Firebase setup
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // ── Header views
    private TextView tvAvatarInitials, tvUserName, tvUserRole;

    // ── Tab views
    private Button tabEditProfile, tabPrivacy;
    private LinearLayout panelEditProfile, panelPrivacy;

    // ── Role toggle
    private Button btnRoleStudent, btnRoleTutor;
    private String currentRole = "student"; // tracks which role tab is active in UI

    // ── Edit Profile fields
    private TextInputEditText etFirstName, etLastName, etEmail,
            etInstitution, etBio, etRate;
    private TextView tvCharCount;
    private LinearLayout layoutInstitution, layoutBio, layoutRate;
    private ChipGroup chipGroupSubjects;

    // ── Privacy fields ────────────────────────────────────────
    private SwitchMaterial switchShowName, switchShowInstitution,
            switchShowSubjects, switchShowRate, switchProfileVisible;
    private LinearLayout rowShowInstitution, rowShowRate;

    // ── Firestore doc role
    private String savedRole = "student";

    // ── All subject chip IDs in order ────────────────────────
    private static final int[] CHIP_IDS = {
            R.id.chipMathematics, R.id.chipPhysics, R.id.chipChemistry,
            R.id.chipBiology, R.id.chipComputerScience, R.id.chipEnglish,
            R.id.chipHistory, R.id.chipEconomics
    };
    private static final String[] CHIP_NAMES = {
            "Mathematics", "Physics", "Chemistry",
            "Biology", "Computer Science", "English",
            "History", "Economics"
    };

    // ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        bindViews();
        setupTabSwitcher();
        setupRoleToggle();
        setupChips();
        setupBioCharCount();
        setupSaveButtons();

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Load existing data from Firestore
        if (currentUser != null) {
            loadUserProfile(currentUser.getUid());
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // ── Bind all views

    private void bindViews() {
        // Header
        tvAvatarInitials  = findViewById(R.id.tvAvatarInitials);
        tvUserName        = findViewById(R.id.tvUserName);
        tvUserRole        = findViewById(R.id.tvUserRole);

        // Tabs
        tabEditProfile    = findViewById(R.id.tabEditProfile);
        tabPrivacy        = findViewById(R.id.tabPrivacy);
        panelEditProfile  = findViewById(R.id.panelEditProfile);
        panelPrivacy      = findViewById(R.id.panelPrivacy);

        // Role toggle
        btnRoleStudent    = findViewById(R.id.btnRoleStudent);
        btnRoleTutor      = findViewById(R.id.btnRoleTutor);

        // Fields
        etFirstName       = findViewById(R.id.editTextFirstName);
        etLastName        = findViewById(R.id.editTextLastName);
        etEmail           = findViewById(R.id.editTextEmail);
        etInstitution     = findViewById(R.id.editTextInstitution);
        etBio             = findViewById(R.id.editTextBio);
        etRate            = findViewById(R.id.editTextRate);
        tvCharCount       = findViewById(R.id.tvCharCount);
        layoutInstitution = findViewById(R.id.layoutInstitution);
        layoutBio         = findViewById(R.id.layoutBio);
        layoutRate        = findViewById(R.id.layoutRate);
        chipGroupSubjects = findViewById(R.id.chipGroupSubjects);

        // Privacy switches
        switchShowName        = findViewById(R.id.switchShowName);
        switchShowInstitution = findViewById(R.id.switchShowInstitution);
        switchShowSubjects    = findViewById(R.id.switchShowSubjects);
        switchShowRate        = findViewById(R.id.switchShowRate);
        switchProfileVisible  = findViewById(R.id.switchProfileVisible);
        rowShowInstitution    = findViewById(R.id.rowShowInstitution);
        rowShowRate           = findViewById(R.id.rowShowRate);
    }

    // ── Tab switcher ──────────────────────────────────────────

    private void setupTabSwitcher() {
        tabEditProfile.setOnClickListener(v -> showTab(true));
        tabPrivacy.setOnClickListener(v -> showTab(false));
    }

    private void showTab(boolean editProfile) {
        if (editProfile) {
            panelEditProfile.setVisibility(View.VISIBLE);
            panelPrivacy.setVisibility(View.GONE);
            // Active style
            tabEditProfile.setBackground(getDrawable(R.drawable.bg_button_gradient));
            tabEditProfile.setTextColor(Color.WHITE);
            // Inactive style
            tabPrivacy.setBackground(null);
            tabPrivacy.setTextColor(Color.parseColor("#4B5D7A"));
        } else {
            panelEditProfile.setVisibility(View.GONE);
            panelPrivacy.setVisibility(View.VISIBLE);
            // Active style
            tabPrivacy.setBackground(getDrawable(R.drawable.bg_button_gradient));
            tabPrivacy.setTextColor(Color.WHITE);
            // Inactive style
            tabEditProfile.setBackground(null);
            tabEditProfile.setTextColor(Color.parseColor("#4B5D7A"));
        }
    }

    // ── Role toggle (Student / Tutor) ─────────────────────────

    private void setupRoleToggle() {
        btnRoleStudent.setOnClickListener(v -> applyRoleUI("student"));
        btnRoleTutor.setOnClickListener(v -> applyRoleUI("tutor"));
    }

    private void applyRoleUI(String role) {
        currentRole = role;

        if ("student".equals(role)) {
            // Student active
            btnRoleStudent.setBackground(getDrawable(R.drawable.bg_button_gradient));
            btnRoleStudent.setTextColor(Color.WHITE);
            // Tutor inactive
            btnRoleTutor.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnRoleTutor.setTextColor(Color.parseColor("#4B5D7A"));
            // Show/hide fields
            layoutInstitution.setVisibility(View.VISIBLE);
            layoutBio.setVisibility(View.GONE);
            layoutRate.setVisibility(View.GONE);
            // Privacy rows
            rowShowInstitution.setVisibility(View.VISIBLE);
            rowShowRate.setVisibility(View.GONE);
        } else {
            // Tutor active
            btnRoleTutor.setBackground(getDrawable(R.drawable.bg_button_gradient));
            btnRoleTutor.setTextColor(Color.WHITE);
            // Student inactive
            btnRoleStudent.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnRoleStudent.setTextColor(Color.parseColor("#4B5D7A"));
            // Show/hide fields
            layoutInstitution.setVisibility(View.GONE);
            layoutBio.setVisibility(View.VISIBLE);
            layoutRate.setVisibility(View.VISIBLE);
            // Privacy rows
            rowShowInstitution.setVisibility(View.GONE);
            rowShowRate.setVisibility(View.VISIBLE);
        }
    }

    // ── Chips

    private void setupChips() {
        for (int i = 0; i < CHIP_IDS.length; i++) {
            Chip chip = findViewById(CHIP_IDS[i]);
            if (chip == null) continue;
            String subject = CHIP_NAMES[i];
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    chip.setChipBackgroundColor(
                            ColorStateList.valueOf(Color.parseColor("#089A3C")));
                    chip.setTextColor(Color.WHITE);
                } else {
                    chip.setChipBackgroundColor(
                            ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                    chip.setTextColor(Color.parseColor("#33476A"));
                }
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

    // ── Bio char count

    private void setupBioCharCount() {
        etBio.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                tvCharCount.setText(s.length() + "/500 characters");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ── Save buttons

    private void setupSaveButtons() {
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnSavePrivacy).setOnClickListener(v -> savePrivacy());
    }

    // ── Load from Firestore

    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        populateUI(doc);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @SuppressWarnings("unchecked")
    private void populateUI(DocumentSnapshot doc) {
        // Role
        savedRole = doc.getString("role") != null ? doc.getString("role") : "student";
        currentRole = savedRole;
        applyRoleUI(currentRole);

        // Name
        String firstName = doc.getString("firstName");
        String lastName  = doc.getString("lastName");
        String fullName  = doc.getString("fullName");

        if (firstName != null) etFirstName.setText(firstName);
        if (lastName != null)  etLastName.setText(lastName);

        // Avatar initials
        String initials = "";
        if (firstName != null && !firstName.isEmpty()) initials += firstName.charAt(0);
        if (lastName  != null && !lastName.isEmpty())  initials += lastName.charAt(0);
        tvAvatarInitials.setText(initials.toUpperCase());

        // Header name + role
        tvUserName.setText(fullName != null ? fullName : "");
        tvUserRole.setText(Character.toUpperCase(savedRole.charAt(0)) + savedRole.substring(1));

        // Email (from FirebaseAuth, read-only feel)
        if (currentUser != null && currentUser.getEmail() != null) {
            etEmail.setText(currentUser.getEmail());
        }

        // Student fields
        String institution = doc.getString("institution");
        if (institution != null) etInstitution.setText(institution);

        // Tutor fields
        String bio = doc.getString("bio");
        if (bio != null) etBio.setText(bio);

        Long rate = doc.getLong("rate");
        if (rate != null) etRate.setText(String.valueOf(rate));

        // Subjects
        List<String> subjects = (List<String>) doc.get("subjects");
        preSelectChips(subjects);

        // Privacy / visibility
        Boolean showName    = doc.getBoolean("showName");
        Boolean showInst    = doc.getBoolean("showInstitution");
        Boolean showSubj    = doc.getBoolean("showSubjects");
        Boolean showRate    = doc.getBoolean("showRate");
        Boolean profileVis  = doc.getBoolean("profileVisible");

        if (showName   != null) switchShowName.setChecked(showName);
        if (showInst   != null) switchShowInstitution.setChecked(showInst);
        if (showSubj   != null) switchShowSubjects.setChecked(showSubj);
        if (showRate   != null) switchShowRate.setChecked(showRate);
        if (profileVis != null) switchProfileVisible.setChecked(profileVis);
    }

    // ── Save profile to Firestore

    private void saveProfile() {
        String firstName = etFirstName.getText() != null
                ? etFirstName.getText().toString().trim() : "";
        String lastName  = etLastName.getText() != null
                ? etLastName.getText().toString().trim() : "";

        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("First name is required");
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Last name is required");
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

        if ("student".equals(currentRole)) {
            String institution = etInstitution.getText() != null
                    ? etInstitution.getText().toString().trim() : "";
            if (TextUtils.isEmpty(institution)) {
                etInstitution.setError("Institution is required");
                return;
            }
            updates.put("institution", institution);
        } else {
            String bio = etBio.getText() != null
                    ? etBio.getText().toString().trim() : "";
            String rateStr = etRate.getText() != null
                    ? etRate.getText().toString().trim() : "";
            if (TextUtils.isEmpty(bio)) {
                etBio.setError("Bio is required");
                return;
            }
            if (TextUtils.isEmpty(rateStr)) {
                etRate.setError("Rate is required");
                return;
            }
            updates.put("bio",  bio);
            updates.put("rate", Integer.parseInt(rateStr));
        }

        db.collection("users")
                .document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "✅ Profile saved!", Toast.LENGTH_SHORT).show();
                    // Refresh header
                    tvUserName.setText(firstName + " " + lastName);
                    String initials = "" + firstName.charAt(0) + lastName.charAt(0);
                    tvAvatarInitials.setText(initials.toUpperCase());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ── Save privacy settings to Firestore ────────────────────

    private void savePrivacy() {
        Map<String, Object> privacy = new HashMap<>();
        privacy.put("showName",        switchShowName.isChecked());
        privacy.put("showInstitution", switchShowInstitution.isChecked());
        privacy.put("showSubjects",    switchShowSubjects.isChecked());
        privacy.put("showRate",        switchShowRate.isChecked());
        privacy.put("profileVisible",  switchProfileVisible.isChecked());

        db.collection("users")
                .document(currentUser.getUid())
                .update(privacy)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "🔒 Privacy settings saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}