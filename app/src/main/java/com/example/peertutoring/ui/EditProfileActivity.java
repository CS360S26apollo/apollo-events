package com.example.peertutoring.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.peertutoring.R;
import com.example.peertutoring.data.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User Story 3 & 4
 */
public class EditProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private UserRepository userRepository;

    private TextView tvAvatarInitials, tvUserName, tvUserRole;
    private MaterialCardView badgeVerified;

    private Button tabEditProfile, tabPrivacy, tabVerification;
    private LinearLayout panelEditProfile, panelPrivacy, panelVerification;

    private TextInputEditText etFirstName, etLastName, etEmail,
            etInstitution, etBio, etRate;
    private TextView tvCharCount;
    private LinearLayout layoutInstitution, layoutBio, layoutRate;

    // US 3 Privacy switches
    private SwitchMaterial switchShowName, switchProfileVisible,
            switchShowInstitution, switchShowSubjects, switchShowRate;

    // US 4 Verification views
    private ImageView imgVerificationStatus;
    private TextView tvVerificationTitle, tvVerificationDesc, tvUploadLabel;
    private MaterialCardView btnUploadID;
    private Button btnSubmitVerification;

    private String savedRole = "student";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userRepository = new UserRepository();

        bindViews();
        setupTabSwitcher();
        setupChips();
        setupBioCharCount();
        setupSaveButtons();
        setupVerificationLogic();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (currentUser != null) {
            loadUserProfile(currentUser.getUid());
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void bindViews() {
        tvAvatarInitials  = findViewById(R.id.tvAvatarInitials);
        tvUserName        = findViewById(R.id.tvUserName);
        tvUserRole        = findViewById(R.id.tvUserRole);
        badgeVerified     = findViewById(R.id.badgeVerified);

        tabEditProfile    = findViewById(R.id.tabEditProfile);
        tabPrivacy        = findViewById(R.id.tabPrivacy);
        tabVerification   = findViewById(R.id.tabVerification);
        panelEditProfile  = findViewById(R.id.panelEditProfile);
        panelPrivacy      = findViewById(R.id.panelPrivacy);
        panelVerification = findViewById(R.id.panelVerification);

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

        switchShowName          = findViewById(R.id.switchShowName);
        switchProfileVisible    = findViewById(R.id.switchProfileVisible);
        switchShowInstitution    = findViewById(R.id.switchShowInstitution);
        switchShowSubjects       = findViewById(R.id.switchShowSubjects);
        switchShowRate          = findViewById(R.id.switchShowRate);

        imgVerificationStatus = findViewById(R.id.imgVerificationStatus);
        tvVerificationTitle   = findViewById(R.id.tvVerificationTitle);
        tvVerificationDesc    = findViewById(R.id.tvVerificationDesc);
        tvUploadLabel         = findViewById(R.id.tvUploadLabel);
        btnUploadID           = findViewById(R.id.btnUploadID);
        btnSubmitVerification = findViewById(R.id.btnSubmitVerification);
    }

    private void setupTabSwitcher() {
        tabEditProfile.setOnClickListener(v -> showTab("profile"));
        tabPrivacy.setOnClickListener(v -> showTab("privacy"));
        tabVerification.setOnClickListener(v -> showTab("verify"));
    }

    private void showTab(String tab) {
        panelEditProfile.setVisibility(View.GONE);
        panelPrivacy.setVisibility(View.GONE);
        panelVerification.setVisibility(View.GONE);

        tabEditProfile.setBackground(null);
        tabEditProfile.setTextColor(Color.parseColor("#4B5D7A"));
        tabPrivacy.setBackground(null);
        tabPrivacy.setTextColor(Color.parseColor("#4B5D7A"));
        tabVerification.setBackground(null);
        tabVerification.setTextColor(Color.parseColor("#4B5D7A"));

        switch (tab) {
            case "profile":
                panelEditProfile.setVisibility(View.VISIBLE);
                tabEditProfile.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_button_gradient));
                tabEditProfile.setTextColor(Color.WHITE);
                break;
            case "privacy":
                panelPrivacy.setVisibility(View.VISIBLE);
                tabPrivacy.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_button_gradient));
                tabPrivacy.setTextColor(Color.WHITE);
                break;
            case "verify":
                panelVerification.setVisibility(View.VISIBLE);
                tabVerification.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_button_gradient));
                tabVerification.setTextColor(Color.WHITE);
                break;
        }
    }

    private void setupChips() {
        for (int id : CHIP_IDS) {
            Chip chip = findViewById(id);
            if (chip == null) continue;
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#089A3C")));
                    chip.setTextColor(Color.WHITE);
                } else {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                    chip.setTextColor(Color.parseColor("#33476A"));
                }
            });
        }
    }

    private void setupBioCharCount() {
        if (etBio == null) return;
        etBio.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (tvCharCount != null) {
                    String countText = s.length() + "/500";
                    tvCharCount.setText(countText);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSaveButtons() {
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnSavePrivacy).setOnClickListener(v -> savePrivacy());
    }

    private void setupVerificationLogic() {
        btnUploadID.setOnClickListener(v -> {
            tvUploadLabel.setText("✅ ID Attached");
            btnSubmitVerification.setEnabled(true);
        });

        btnSubmitVerification.setOnClickListener(v -> {
            if (currentUser == null) return;
            userRepository.submitVerificationId(currentUser.getUid(), "mock_id_url", new UserRepository.SaveCallback() {
                @Override
                public void onSuccess() {
                    updateVerificationUI(false, true);
                    Toast.makeText(EditProfileActivity.this, "Submitted!", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFailure(String error) { Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show(); }
            });
        });
    }

    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) populateUI(doc);
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void populateUI(DocumentSnapshot doc) {
        savedRole = doc.getString("role") != null ? doc.getString("role") : "student";
        tvUserRole.setText(formatRole(savedRole));

        if ("tutor".equals(savedRole)) {
            layoutBio.setVisibility(View.VISIBLE);
            layoutRate.setVisibility(View.VISIBLE);
            layoutInstitution.setVisibility(View.GONE);
            switchShowRate.setVisibility(View.VISIBLE);
        } else {
            layoutBio.setVisibility(View.GONE);
            layoutRate.setVisibility(View.GONE);
            layoutInstitution.setVisibility(View.VISIBLE);
            switchShowRate.setVisibility(View.GONE);
        }

        String fName = doc.getString("firstName");
        String lName = doc.getString("lastName");
        etFirstName.setText(fName);
        etLastName.setText(lName);
        tvUserName.setText(doc.getString("fullName"));

        if (currentUser != null) {
            etEmail.setText(currentUser.getEmail());
        }

        String initials = "";
        if (fName != null && !fName.isEmpty()) initials += fName.charAt(0);
        if (lName != null && !lName.isEmpty()) initials += lName.charAt(0);
        tvAvatarInitials.setText(initials.toUpperCase());

        etInstitution.setText(doc.getString("institution"));
        etBio.setText(doc.getString("bio"));
        if (doc.getLong("rate") != null) etRate.setText(String.valueOf(doc.getLong("rate")));

        List<String> subjects = (List<String>) doc.get("subjects");
        if (subjects != null) {
            for (int i = 0; i < CHIP_IDS.length; i++) {
                Chip chip = findViewById(CHIP_IDS[i]);
                if (chip != null && subjects.contains(CHIP_NAMES[i])) {
                    chip.setChecked(true);
                }
            }
        }

        if (doc.getBoolean("showName") != null) {
            switchShowName.setChecked(Boolean.TRUE.equals(doc.getBoolean("showName")));
        }
        if (doc.getBoolean("profileVisible") != null) {
            switchProfileVisible.setChecked(Boolean.TRUE.equals(doc.getBoolean("profileVisible")));
        }
        if (doc.getBoolean("showInstitution") != null) {
            switchShowInstitution.setChecked(Boolean.TRUE.equals(doc.getBoolean("showInstitution")));
        }
        if (doc.getBoolean("showSubjects") != null) {
            switchShowSubjects.setChecked(Boolean.TRUE.equals(doc.getBoolean("showSubjects")));
        }
        if (doc.getBoolean("showRate") != null) {
            switchShowRate.setChecked(Boolean.TRUE.equals(doc.getBoolean("showRate")));
        }

        boolean verified = Boolean.TRUE.equals(doc.getBoolean("verified"));
        boolean pending = doc.getString("idDocumentUrl") != null && !verified;
        updateVerificationUI(verified, pending);
    }

    private String formatRole(String role) {
        if (role == null || role.isEmpty()) return "";
        return Character.toUpperCase(role.charAt(0)) + role.substring(1);
    }

    private void updateVerificationUI(boolean verified, boolean pending) {
        if (verified) {
            badgeVerified.setVisibility(View.VISIBLE);
            tvVerificationTitle.setText("Verified");
            tvVerificationDesc.setText("Your identity has been confirmed.");
            imgVerificationStatus.setImageResource(android.R.drawable.ic_dialog_info);
            imgVerificationStatus.setColorFilter(Color.parseColor("#089A3C"));
            btnUploadID.setVisibility(View.GONE);
            btnSubmitVerification.setVisibility(View.GONE);
        } else if (pending) {
            badgeVerified.setVisibility(View.GONE);
            tvVerificationTitle.setText("Pending Review");
            tvVerificationDesc.setText("We are currently reviewing your documents.");
            imgVerificationStatus.setImageResource(android.R.drawable.ic_menu_recent_history);
            imgVerificationStatus.setColorFilter(Color.parseColor("#E07B00"));
            btnUploadID.setVisibility(View.GONE);
            btnSubmitVerification.setEnabled(false);
            btnSubmitVerification.setText("Reviewing...");
        } else {
            badgeVerified.setVisibility(View.GONE);
            tvVerificationTitle.setText("Verify Your Identity");
            tvVerificationDesc.setText("Upload a document to earn your trust badge.");
            imgVerificationStatus.setImageResource(android.R.drawable.ic_lock_idle_lock);
            imgVerificationStatus.setColorFilter(Color.parseColor("#8B97A8"));
            btnUploadID.setVisibility(View.VISIBLE);
            btnSubmitVerification.setVisibility(View.VISIBLE);
            btnSubmitVerification.setEnabled(false);
            btnSubmitVerification.setText("Submit for Review");
        }
    }

    private void saveProfile() {
        if (etFirstName.getText() == null || etLastName.getText() == null) return;
        
        String fName = etFirstName.getText().toString();
        String lName = etLastName.getText().toString();
        
        if (TextUtils.isEmpty(fName) || TextUtils.isEmpty(lName)) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", fName);
        updates.put("lastName", lName);
        updates.put("fullName", fName + " " + lName);

        List<String> selected = new ArrayList<>();
        for (int id : CHIP_IDS) {
            Chip chip = findViewById(id);
            if (chip != null && chip.isChecked()) {
                selected.add(chip.getText().toString());
            }
        }
        updates.put("subjects", selected);

        if ("student".equals(savedRole)) {
            if (etInstitution.getText() != null) updates.put("institution", etInstitution.getText().toString());
        } else {
            if (etBio.getText() != null) updates.put("bio", etBio.getText().toString());
            if (etRate.getText() != null && !TextUtils.isEmpty(etRate.getText())) {
                try {
                    updates.put("rate", Integer.parseInt(etRate.getText().toString()));
                } catch (NumberFormatException ignored) {}
            }
        }

        userRepository.updateProfile(currentUser.getUid(), updates, new UserRepository.SaveCallback() {
            @Override public void onSuccess() { 
                Toast.makeText(EditProfileActivity.this, "Saved!", Toast.LENGTH_SHORT).show();
                tvUserName.setText(fName + " " + lName);
            }
            @Override public void onFailure(String e) { Toast.makeText(EditProfileActivity.this, e, Toast.LENGTH_SHORT).show(); }
        });
    }

    private void savePrivacy() {
        Map<String, Object> privacy = new HashMap<>();
        privacy.put("showName", switchShowName.isChecked());
        privacy.put("profileVisible", switchProfileVisible.isChecked());
        privacy.put("showInstitution", switchShowInstitution.isChecked());
        privacy.put("showSubjects", switchShowSubjects.isChecked());
        privacy.put("showRate", switchShowRate.isChecked());

        userRepository.updatePrivacy(currentUser.getUid(), privacy, new UserRepository.SaveCallback() {
            @Override public void onSuccess() { Toast.makeText(EditProfileActivity.this, "Privacy Saved!", Toast.LENGTH_SHORT).show(); }
            @Override public void onFailure(String e) { Toast.makeText(EditProfileActivity.this, e, Toast.LENGTH_SHORT).show(); }
        });
    }
}