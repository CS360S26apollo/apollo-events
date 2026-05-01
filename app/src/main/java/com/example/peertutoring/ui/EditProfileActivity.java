package com.example.peertutoring.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.example.peertutoring.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for users to view and modify their profile information and privacy settings.
 * Role: View component for User Story 3 (Edit Profile & Privacy) and User Story 4 (Verification).
 */
public class EditProfileActivity extends AppCompatActivity {

    // -- Firebase
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // -- Header views
    private TextView tvAvatarInitials, tvUserName, tvUserRole;

    // -- Tab views
    private Button tabEditProfile, tabPrivacy, tabVerification;
    private LinearLayout panelEditProfile, panelPrivacy, panelVerification;

    // -- Role toggle
    private Button btnRoleStudent, btnRoleTutor;
    private String currentRole = "student";

    // -- Edit Profile fields
    private TextInputEditText etFirstName, etLastName, etEmail,
            etInstitution, etBio, etRate;
    private TextView tvCharCount;
    private LinearLayout layoutInstitution, layoutBio, layoutRate;
    private Button btnManageAvailability;

    // -- Privacy switches
    private SwitchMaterial switchShowName, switchShowInstitution,
            switchShowSubjects, switchShowRate, switchProfileVisible;
    private LinearLayout rowShowInstitution, rowShowRate;

    // -- Profile photo
    private ImageView ivProfilePicture;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> documentLauncher;
    private Uri selectedDocumentUri = null;
    private FirebaseStorage storage;

    // -- Subject chip IDs and names
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

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        storage     = FirebaseStorage.getInstance();

        setupLaunchers();
        bindViews();
        setupTabSwitcher();
        setupRoleToggle();
        setupChips();
        setupBioCharCount();
        setupSaveButtons();
        setupLogoutButton();

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (currentUser != null) {
            loadUserProfile(currentUser.getUid());
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /** Binds all layout views to their respective member variables. */
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
        btnManageAvailability = findViewById(R.id.btnManageAvailability);
        switchShowName        = findViewById(R.id.switchShowName);
        switchShowInstitution = findViewById(R.id.switchShowInstitution);
        switchShowSubjects    = findViewById(R.id.switchShowSubjects);
        switchShowRate        = findViewById(R.id.switchShowRate);
        switchProfileVisible  = findViewById(R.id.switchProfileVisible);
        rowShowInstitution    = findViewById(R.id.rowShowInstitution);
        rowShowRate           = findViewById(R.id.rowShowRate);
        ivProfilePicture      = findViewById(R.id.ivProfilePicture);

        // Profile picture click → choose source
        View avatarContainer = findViewById(R.id.avatarContainer);
        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v -> showPhotoSourceDialog());
        }
        if (ivProfilePicture != null) {
            ivProfilePicture.setOnClickListener(v -> showPhotoSourceDialog());
        }

        // Verification panel
        View btnUploadID = findViewById(R.id.btnUploadID);
        if (btnUploadID != null) {
            btnUploadID.setOnClickListener(v -> openDocumentPicker());
        }
        Button btnSubmit = findViewById(R.id.btnSubmitVerification);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> submitVerificationDocument());
        }
    }

    /** Configures the tab selection listeners. */
    private void setupTabSwitcher() {
        if (tabEditProfile != null)  tabEditProfile.setOnClickListener(v -> showTab(0));
        if (tabPrivacy != null)      tabPrivacy.setOnClickListener(v -> showTab(1));
        if (tabVerification != null) tabVerification.setOnClickListener(v -> showTab(2));
    }

    /** Toggles visibility between Edit, Privacy, and Verification panels. */
    private void showTab(int index) {
        if (panelEditProfile != null)  panelEditProfile.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        if (panelPrivacy != null)      panelPrivacy.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        if (panelVerification != null) panelVerification.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        updateTabStyle(tabEditProfile, index == 0);
        updateTabStyle(tabPrivacy, index == 1);
        updateTabStyle(tabVerification, index == 2);
    }

    /** Updates the visual style of tabs based on selection. */
    private void updateTabStyle(Button btn, boolean selected) {
        if (btn == null) return;
        if (selected) {
            btn.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_button_gradient));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackground(null);
            btn.setTextColor(Color.parseColor("#4B5D7A"));
        }
    }

    /**
     * Role is fixed at signup — buttons are display-only indicators.
     * Clicking them shows a message explaining the role cannot be changed.
     */
    private void setupRoleToggle() {
        View.OnClickListener locked = v ->
                Toast.makeText(this,
                        "Your role is fixed at signup. Create a new account to switch roles.",
                        Toast.LENGTH_LONG).show();
        if (btnRoleStudent != null) btnRoleStudent.setOnClickListener(locked);
        if (btnRoleTutor   != null) btnRoleTutor.setOnClickListener(locked);
    }

    /** Adjusts the visibility of role-specific input fields. */
    private void applyRoleUI(String role) {
        currentRole = role;
        if (layoutInstitution != null)
            layoutInstitution.setVisibility(role.equals("student") ? View.VISIBLE : View.GONE);
        if (layoutBio != null)
            layoutBio.setVisibility(role.equals("tutor") ? View.VISIBLE : View.GONE);
        if (layoutRate != null)
            layoutRate.setVisibility(role.equals("tutor") ? View.VISIBLE : View.GONE);
        if (rowShowInstitution != null)
            rowShowInstitution.setVisibility(role.equals("student") ? View.VISIBLE : View.GONE);
        if (rowShowRate != null)
            rowShowRate.setVisibility(role.equals("tutor") ? View.VISIBLE : View.GONE);

        if (btnManageAvailability != null) {
            if (role.equals("tutor")) {
                btnManageAvailability.setVisibility(View.VISIBLE);
                btnManageAvailability.setOnClickListener(v ->
                        startActivity(new Intent(this, AvailabilityDashboardActivity.class)));
            } else {
                btnManageAvailability.setVisibility(View.GONE);
            }
        }

        if (btnRoleStudent != null) {
            if (role.equals("student")) {
                btnRoleStudent.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_button_gradient));
                btnRoleStudent.setTextColor(Color.WHITE);
            } else {
                btnRoleStudent.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                btnRoleStudent.setTextColor(Color.parseColor("#4B5D7A"));
            }
        }
        if (btnRoleTutor != null) {
            if (role.equals("tutor")) {
                btnRoleTutor.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_button_gradient));
                btnRoleTutor.setTextColor(Color.WHITE);
            } else {
                btnRoleTutor.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                btnRoleTutor.setTextColor(Color.parseColor("#4B5D7A"));
            }
        }
    }

    /** Sets up selection listeners for the subject chips. */
    private void setupChips() {
        for (int id : CHIP_IDS) {
            Chip chip = findViewById(id);
            if (chip == null) continue;
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                chip.setChipBackgroundColor(ColorStateList.valueOf(
                        isChecked ? Color.parseColor("#089A3C") : Color.parseColor("#E0E0E0")
                ));
                chip.setTextColor(isChecked ? Color.WHITE : Color.parseColor("#33476A"));
            });
        }
    }

    /** Pre-selects chips based on data loaded from the database. */
    private void preSelectChips(List<String> subjects) {
        if (subjects == null) return;
        for (int i = 0; i < CHIP_IDS.length; i++) {
            Chip chip = findViewById(CHIP_IDS[i]);
            if (chip != null && subjects.contains(CHIP_NAMES[i])) {
                chip.setChecked(true);
            }
        }
    }

    /** @return List of strings representing the currently selected subjects. */
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

    /** Monitors the bio field and updates the character counter. */
    private void setupBioCharCount() {
        if (etBio == null || tvCharCount == null) return;
        etBio.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                String count = s.length() + "/500 characters";
                tvCharCount.setText(count);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    /** Initializes the Save buttons for Profile and Privacy panels. */
    private void setupSaveButtons() {
        View btnSaveProfile = findViewById(R.id.btnSaveProfile);
        View btnSavePrivacy = findViewById(R.id.btnSavePrivacy);
        if (btnSaveProfile != null) btnSaveProfile.setOnClickListener(v -> saveProfile());
        if (btnSavePrivacy != null) btnSavePrivacy.setOnClickListener(v -> savePrivacy());
    }

    /** Fetches the user's profile document from Firestore. */
    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        populateUI(doc);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(EditProfileActivity.this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** Populates the UI widgets with data from a Firestore DocumentSnapshot. */
    @SuppressWarnings("unchecked")
    private void populateUI(DocumentSnapshot doc) {
        String savedRole = doc.getString("role") != null ? doc.getString("role") : "student";
        currentRole = savedRole;
        applyRoleUI(currentRole);

        String firstName = doc.getString("firstName");
        String lastName  = doc.getString("lastName");
        String fullName  = doc.getString("fullName");

        if (etFirstName != null && firstName != null) etFirstName.setText(firstName);
        if (etLastName  != null && lastName  != null) etLastName.setText(lastName);

        String initials = "";
        if (firstName != null && !firstName.isEmpty()) initials += firstName.charAt(0);
        if (lastName  != null && !lastName.isEmpty())  initials += lastName.charAt(0);
        if (tvAvatarInitials != null) tvAvatarInitials.setText(initials.toUpperCase());

        if (tvUserName != null) tvUserName.setText(fullName != null ? fullName : "");
        if (tvUserRole != null && !savedRole.isEmpty()) {
            String roleText = Character.toUpperCase(savedRole.charAt(0)) + savedRole.substring(1);
            tvUserRole.setText(roleText);
        }

        if (etEmail != null && currentUser != null && currentUser.getEmail() != null) {
            etEmail.setText(currentUser.getEmail());
        }

        String institution = doc.getString("institution");
        if (etInstitution != null && institution != null) etInstitution.setText(institution);

        String bio = doc.getString("bio");
        if (etBio != null && bio != null) etBio.setText(bio);

        Long rate = doc.getLong("rate");
        if (etRate != null && rate != null) etRate.setText(String.valueOf(rate));

        List<String> subjects = (List<String>) doc.get("subjects");
        preSelectChips(subjects);

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

        // Load profile picture if stored
        String photoUrl = doc.getString("profilePhotoUrl");
        if (photoUrl != null && !photoUrl.isEmpty() && ivProfilePicture != null) {
            ivProfilePicture.setVisibility(View.VISIBLE);
            if (tvAvatarInitials != null) tvAvatarInitials.setVisibility(View.GONE);
            Glide.with(this).load(photoUrl).circleCrop().into(ivProfilePicture);
        }
    }

    /** Validates and saves the general profile information back to Firestore. */
    private void saveProfile() {
        if (currentUser == null) return;

        String firstName = etFirstName != null && etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        String lastName  = etLastName  != null && etLastName.getText()  != null ? etLastName.getText().toString().trim()  : "";

        if (TextUtils.isEmpty(firstName)) {
            if (etFirstName != null) etFirstName.setError("First name is required");
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            if (etLastName != null) etLastName.setError("Last name is required");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName",  lastName);
        updates.put("fullName",  firstName + " " + lastName);
        updates.put("subjects",  getSelectedSubjects());
        // Role is NOT saved from UI — it was set at signup and never changes
        // updates.put("role", currentRole);  // intentionally removed

        if (currentRole.equals("student")) {
            String institution = etInstitution != null && etInstitution.getText() != null ? etInstitution.getText().toString().trim() : "";
            if (!TextUtils.isEmpty(institution)) updates.put("institution", institution);
        } else {
            String bio     = etBio  != null && etBio.getText()  != null ? etBio.getText().toString().trim()  : "";
            String rateStr = etRate != null && etRate.getText() != null ? etRate.getText().toString().trim() : "";
            if (!TextUtils.isEmpty(bio)) updates.put("bio",  bio);
            if (!TextUtils.isEmpty(rateStr)) {
                try {
                    updates.put("rate", Integer.parseInt(rateStr));
                } catch (NumberFormatException ignored) {}
            }
        }

        db.collection("users").document(currentUser.getUid()).set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> Toast.makeText(EditProfileActivity.this, "✅ Profile saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(EditProfileActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /** Saves only the privacy/visibility settings back to Firestore. */
    private void savePrivacy() {
        if (currentUser == null) return;

        Map<String, Object> privacy = new HashMap<>();
        if (switchShowName        != null) privacy.put("showName",        switchShowName.isChecked());
        if (switchShowInstitution != null) privacy.put("showInstitution", switchShowInstitution.isChecked());
        if (switchShowSubjects    != null) privacy.put("showSubjects",    switchShowSubjects.isChecked());
        if (switchShowRate        != null) privacy.put("showRate",        switchShowRate.isChecked());
        if (switchProfileVisible  != null) privacy.put("profileVisible",  switchProfileVisible.isChecked());

        db.collection("users").document(currentUser.getUid()).set(privacy, SetOptions.merge())
                .addOnSuccessListener(unused -> Toast.makeText(EditProfileActivity.this, " Privacy settings saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(EditProfileActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
    // ── Activity result launchers ─────────────────────────────────────────

    private Uri cameraImageUri = null; // URI for full-res camera photo

    private void setupLaunchers() {

        // ── Camera: uses FileProvider for full-res photo ──────────────────
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                        try {
                            android.graphics.BitmapFactory.Options opts =
                                    new android.graphics.BitmapFactory.Options();
                            opts.inSampleSize = 2; // reduce memory usage
                            java.io.InputStream is =
                                    getContentResolver().openInputStream(cameraImageUri);
                            Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is, null, opts);
                            if (is != null) is.close();
                            if (bitmap != null) uploadProfilePhoto(bitmap);
                        } catch (IOException e) {
                            Toast.makeText(this,
                                    "Could not read photo: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // ── Gallery: ACTION_GET_CONTENT works on all Android versions ─────
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri == null) return;
                        try {
                            // Take persistent permission so we can read the URI
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignored) {}
                        try {
                            android.graphics.BitmapFactory.Options opts =
                                    new android.graphics.BitmapFactory.Options();
                            opts.inSampleSize = 2;
                            java.io.InputStream is = getContentResolver().openInputStream(uri);
                            Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is, null, opts);
                            if (is != null) is.close();
                            if (bitmap != null) {
                                uploadProfilePhoto(bitmap);
                            } else {
                                Toast.makeText(this, "Could not decode image.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            Toast.makeText(this,
                                    "Failed to read image: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // ── Document (PDF only) ───────────────────────────────────────────
        documentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri == null) return;

                        // Take persistent permission
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignored) {}

                        // Validate MIME type — PDF only
                        String type = getContentResolver().getType(uri);
                        if (type == null || !type.equals("application/pdf")) {
                            Toast.makeText(this,
                                    "Only PDF files are accepted for verification.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        selectedDocumentUri = uri;
                        TextView tvLabel = findViewById(R.id.tvUploadLabel);
                        if (tvLabel != null) {
                            String name = uri.getLastPathSegment();
                            if (name != null && name.contains("/"))
                                name = name.substring(name.lastIndexOf("/") + 1);
                            tvLabel.setText("✅  " + name);
                            tvLabel.setTextColor(0xFF00C853);
                        }
                        Button btnSubmit = findViewById(R.id.btnSubmitVerification);
                        if (btnSubmit != null) btnSubmit.setEnabled(true);
                    }
                });
    }

    // ── Profile photo ─────────────────────────────────────────────────────

    private void showPhotoSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Update Profile Photo")
                .setItems(new String[]{"Take Photo", "Choose from Gallery", "Cancel"},
                        (dialog, which) -> {
                            if (which == 0) {
                                launchCamera();
                            } else if (which == 1) {
                                // ACTION_GET_CONTENT works on all Android versions incl. 13+
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("image/*");
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                galleryLauncher.launch(intent);
                            }
                        })
                .show();
    }

    private void launchCamera() {
        try {
            // Create a temp file in app cache — no FileProvider needed for cache dir
            java.io.File photoFile = java.io.File.createTempFile(
                    "profile_", ".jpg", getCacheDir());
            cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            // Grant URI permission to camera app
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(this, "Cannot open camera: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadProfilePhoto(Bitmap bitmap) {
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Scale to 512px max — reduce upload size
        Bitmap scaled = scaleBitmap(bitmap, 512);

        // Show immediately in UI
        if (ivProfilePicture != null) {
            ivProfilePicture.setVisibility(View.VISIBLE);
            ivProfilePicture.setImageBitmap(scaled);
        }
        if (tvAvatarInitials != null) tvAvatarInitials.setVisibility(View.GONE);

        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();

        // Compress to JPEG
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] data = baos.toByteArray();

        // Build metadata
        com.google.firebase.storage.StorageMetadata metadata =
                new com.google.firebase.storage.StorageMetadata.Builder()
                        .setContentType("image/jpeg")
                        .build();

        // Upload to: profilePhotos/{uid}.jpg
        // NOTE: This path must match your Firebase Storage Rules
        StorageReference ref = storage.getReference()
                .child("profilePhotos")
                .child(currentUser.getUid() + ".jpg");

        ref.putBytes(data, metadata)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    String url = uri.toString();
                                    // Save to Firestore
                                    Map<String, Object> update = new HashMap<>();
                                    update.put("profilePhotoUrl", url);
                                    db.collection("users").document(currentUser.getUid())
                                            .set(update, SetOptions.merge())
                                            .addOnSuccessListener(u ->
                                                    Toast.makeText(this,
                                                            "✅ Profile photo updated!",
                                                            Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this,
                                                            "Photo uploaded but URL not saved: "
                                                                    + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show());
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Upload done but URL fetch failed: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    // Friendly error messages
                    if (msg.contains("User does not have permission")
                            || msg.contains("unauthorized")
                            || msg.contains("403")) {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Storage Permission Denied")
                                .setMessage("Firebase Storage is blocking the upload.\n\n"
                                        + "Ask your friend to update Firebase Storage Rules to:\n\n"
                                        + "rules_version = '2';\n"
                                        + "service firebase.storage {\n"
                                        + "  match /b/{bucket}/o {\n"
                                        + "    match /{allPaths=**} {\n"
                                        + "      allow read, write: if request.auth != null;\n"
                                        + "    }\n"
                                        + "  }\n"
                                        + "}")
                                .setPositiveButton("OK", null)
                                .show();
                    } else if (msg.contains("Object does not exist")
                            || msg.contains("404")) {
                        Toast.makeText(this,
                                "Storage path error. Check Firebase Storage is enabled.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Upload failed: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Scales bitmap down so largest dimension == maxPx. Preserves aspect ratio. */
    private Bitmap scaleBitmap(Bitmap src, int maxPx) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxPx && h <= maxPx) return src;
        float scale = maxPx / (float) Math.max(w, h);
        return Bitmap.createScaledBitmap(src, (int)(w * scale), (int)(h * scale), true);
    }

    // ── Verification document (PDF only) ─────────────────────────────────

    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf"); // PDF only
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        documentLauncher.launch(intent);
    }

    private void submitVerificationDocument() {
        if (selectedDocumentUri == null) {
            Toast.makeText(this, "Please select a PDF document first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) return;

        Button btnSubmit = findViewById(R.id.btnSubmitVerification);
        if (btnSubmit != null) { btnSubmit.setEnabled(false); btnSubmit.setText("Uploading..."); }
        TextView tvLabel = findViewById(R.id.tvUploadLabel);

        // Upload to: verifications/{uid}/document.pdf
        StorageReference ref = storage.getReference()
                .child("verifications")
                .child(currentUser.getUid())
                .child("document.pdf");

        // Read PDF bytes from URI — avoids "Object does not exist" path errors
        byte[] pdfBytes = null;
        try {
            java.io.InputStream is = getContentResolver().openInputStream(selectedDocumentUri);
            if (is != null) {
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int n;
                while ((n = is.read(chunk)) != -1) buffer.write(chunk, 0, n);
                is.close();
                pdfBytes = buffer.toByteArray();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Cannot read PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (btnSubmit != null) { btnSubmit.setEnabled(true); btnSubmit.setText("Submit for Review"); }
            return;
        }

        if (pdfBytes == null || pdfBytes.length == 0) {
            Toast.makeText(this, "PDF file is empty.", Toast.LENGTH_SHORT).show();
            if (btnSubmit != null) { btnSubmit.setEnabled(true); btnSubmit.setText("Submit for Review"); }
            return;
        }

        com.google.firebase.storage.StorageMetadata pdfMeta =
                new com.google.firebase.storage.StorageMetadata.Builder()
                        .setContentType("application/pdf")
                        .build();

        final byte[] finalBytes = pdfBytes;
        ref.putBytes(finalBytes, pdfMeta)
                .addOnSuccessListener(snap ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            Map<String, Object> verif = new HashMap<>();
                            verif.put("verificationDocUrl", uri.toString());
                            verif.put("verificationStatus", "pending");
                            verif.put("submittedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

                            db.collection("users").document(currentUser.getUid())
                                    .set(verif, SetOptions.merge())
                                    .addOnSuccessListener(u -> {
                                        Toast.makeText(this,
                                                "✅ Document submitted! Under review.",
                                                Toast.LENGTH_LONG).show();
                                        if (tvLabel != null) tvLabel.setText("✅ Submitted — Under Review");
                                        if (btnSubmit != null) {
                                            btnSubmit.setText("Submitted");
                                            btnSubmit.setEnabled(false);
                                        }
                                    });
                        }))
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    Toast.makeText(this, "Upload failed: " + msg, Toast.LENGTH_LONG).show();
                    if (btnSubmit != null) { btnSubmit.setEnabled(true); btnSubmit.setText("Submit for Review"); }
                });
    }


    // ── Logout ────────────────────────────────────────────────

    private void setupLogoutButton() {
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


}