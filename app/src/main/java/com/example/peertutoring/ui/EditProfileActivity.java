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
 * Account Settings activity.
 *
 * New features:
 * 1. Profile picture — tap the avatar circle to pick a photo from gallery or camera.
 *    Uploaded to Firebase Storage at profilePictures/{uid}.jpg, URL saved to Firestore.
 * 2. Document upload — "Tap to select document" opens the phone's file browser.
 *    Uploaded to Firebase Storage at idDocuments/{uid}, URL saved to Firestore.
 *    Submit button enables only after a document is selected.
 * 3. Logout button with confirmation dialog.
 */
public class EditProfileActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private StorageReference storageRef;

    // Header views
    private TextView tvAvatarInitials, tvUserName, tvUserRole;
    private ImageView ivProfilePicture;
    private View avatarContainer;

    // Tab panels
    private Button tabEditProfile, tabPrivacy, tabVerification;
    private LinearLayout panelEditProfile, panelPrivacy, panelVerification;

    // Role toggle
    private Button btnRoleStudent, btnRoleTutor;
    private String currentRole = "student";

    // Edit profile fields
    private TextInputEditText etFirstName, etLastName, etEmail,
            etInstitution, etBio, etRate;
    private TextView tvCharCount;
    private LinearLayout layoutInstitution, layoutBio, layoutRate;
    private Button btnManageAvailability;

    // Privacy
    private SwitchMaterial switchShowName, switchShowInstitution,
            switchShowSubjects, switchShowRate, switchProfileVisible;
    private LinearLayout rowShowInstitution, rowShowRate;

    // Verification
    private TextView tvUploadLabel;
    private Button btnSubmitVerification;
    private Uri selectedDocumentUri = null;

    // Subject chips
    private static final int[] CHIP_IDS = {
            R.id.chipMathematics, R.id.chipPhysics, R.id.chipChemistry,
            R.id.chipBiology, R.id.chipComputerScience, R.id.chipEnglish,
            R.id.chipHistory, R.id.chipEconomics
    };
    private static final String[] CHIP_NAMES = {
            "Mathematics", "Physics", "Chemistry", "Biology",
            "Computer Science", "English", "History", "Economics"
    };

    // ── Activity Result Launchers ─────────────────────────────

    /** Gallery picker for profile picture */
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) uploadProfilePicture(uri);
                }
            });

    /** Camera capture for profile picture */
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                    if (bitmap != null && ivProfilePicture != null) {
                        ivProfilePicture.setImageBitmap(bitmap);
                        ivProfilePicture.setVisibility(View.VISIBLE);
                        if (tvAvatarInitials != null) tvAvatarInitials.setVisibility(View.GONE);
                    }
                }
            });

    /** File browser for ID document */
    private final ActivityResultLauncher<Intent> documentLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedDocumentUri = result.getData().getData();
                    if (selectedDocumentUri != null) {
                        String name = getFileNameFromUri(selectedDocumentUri);
                        if (tvUploadLabel != null)
                            tvUploadLabel.setText("✅ " + name);
                        if (btnSubmitVerification != null)
                            btnSubmitVerification.setEnabled(true);
                    }
                }
            });

    // ─────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        storageRef  = FirebaseStorage.getInstance().getReference();

        bindViews();
        setupTabSwitcher();
        setupRoleToggle();
        setupChips();
        setupBioCharCount();
        setupSaveButtons();
        setupLogoutButton();
        setupAvatarTap();
        setupVerificationPanel();

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (currentUser != null) {
            loadUserProfile(currentUser.getUid());
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void bindViews() {
        tvAvatarInitials      = findViewById(R.id.tvAvatarInitials);
        ivProfilePicture      = findViewById(R.id.ivProfilePicture);
        avatarContainer       = findViewById(R.id.avatarContainer);
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
        tvUploadLabel         = findViewById(R.id.tvUploadLabel);
        btnSubmitVerification = findViewById(R.id.btnSubmitVerification);
    }

    // ── Avatar / Profile Picture ──────────────────────────────

    /**
     * Tapping the avatar circle shows a dialog to choose Camera or Gallery.
     */
    private void setupAvatarTap() {
        // We use avatarContainer (the FrameLayout wrapping the circle + badge)
        // If it's null, fall back to the MaterialCardView holding the initials
        View tappable = avatarContainer != null ? avatarContainer
                : (tvAvatarInitials != null ? (View) tvAvatarInitials.getParent() : null);

        if (tappable != null) {
            tappable.setOnClickListener(v -> showPhotoSourceDialog());
        }
    }

    private void showPhotoSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Change Profile Picture")
                .setItems(new String[]{"Choose from Gallery", "Take a Photo"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/*");
                        galleryLauncher.launch(intent);
                    } else {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraLauncher.launch(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Uploads the selected image to Firebase Storage and saves the download URL
     * to the user's Firestore document under "profilePictureUrl".
     */
    private void uploadProfilePicture(Uri uri) {
        if (currentUser == null) return;

        Toast.makeText(this, "Uploading picture...", Toast.LENGTH_SHORT).show();

        // Show immediately in UI
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            if (ivProfilePicture != null) {
                ivProfilePicture.setImageBitmap(bitmap);
                ivProfilePicture.setVisibility(View.VISIBLE);
            }
            if (tvAvatarInitials != null) tvAvatarInitials.setVisibility(View.GONE);
        } catch (IOException ignored) {}

        StorageReference ref = storageRef
                .child("profilePictures/" + currentUser.getUid() + ".jpg");

        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            String url = downloadUri.toString();
                            db.collection("users").document(currentUser.getUid())
                                    .update("profilePictureUrl", url)
                                    .addOnSuccessListener(u ->
                                            Toast.makeText(this, "✅ Profile picture updated!",
                                                    Toast.LENGTH_SHORT).show());
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ── Verification Panel ────────────────────────────────────

    /**
     * Wires up the document picker and submit button in the Verification tab.
     */
    private void setupVerificationPanel() {
        View btnUploadID = findViewById(R.id.btnUploadID);
        if (btnUploadID != null) {
            btnUploadID.setOnClickListener(v -> openFilePicker());
        }

        if (btnSubmitVerification != null) {
            btnSubmitVerification.setEnabled(false); // disabled until file selected
            btnSubmitVerification.setOnClickListener(v -> submitVerification());
        }
    }

    /** Opens the Android file browser to pick any document (PDF, image, etc.). */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Allow PDFs, images, Word docs
        String[] mimeTypes = {"application/pdf", "image/*",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        documentLauncher.launch(Intent.createChooser(intent, "Select ID Document"));
    }

    /**
     * Uploads the selected document to Firebase Storage under idDocuments/{uid}
     * and saves the download URL to Firestore, then marks verification as pending.
     */
    private void submitVerification() {
        if (currentUser == null || selectedDocumentUri == null) return;

        if (btnSubmitVerification != null) {
            btnSubmitVerification.setEnabled(false);
            btnSubmitVerification.setText("Uploading...");
        }

        String fileName = getFileNameFromUri(selectedDocumentUri);
        StorageReference ref = storageRef
                .child("idDocuments/" + currentUser.getUid() + "/" + fileName);

        ref.putFile(selectedDocumentUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            String url = downloadUri.toString();

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("idDocumentUrl",       url);
                            updates.put("verificationStatus",  "pending");

                            db.collection("users").document(currentUser.getUid())
                                    .set(updates, SetOptions.merge())
                                    .addOnSuccessListener(u -> {
                                        Toast.makeText(this,
                                                "✅ Document submitted! We'll review it shortly.",
                                                Toast.LENGTH_LONG).show();
                                        if (tvUploadLabel != null)
                                            tvUploadLabel.setText("📄 Document submitted for review");
                                        if (btnSubmitVerification != null) {
                                            btnSubmitVerification.setText("Submitted ✓");
                                            btnSubmitVerification.setEnabled(false);
                                        }
                                    });
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Upload failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    if (btnSubmitVerification != null) {
                        btnSubmitVerification.setEnabled(true);
                        btnSubmitVerification.setText("Submit for Review");
                    }
                });
    }

    /** Extracts a human-readable file name from a content URI. */
    private String getFileNameFromUri(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name == null) name = "document";
        // Strip path prefix if present
        if (name.contains("/")) name = name.substring(name.lastIndexOf('/') + 1);
        return name;
    }

    // ── Tabs ──────────────────────────────────────────────────

    private void setupTabSwitcher() {
        if (tabEditProfile != null)  tabEditProfile.setOnClickListener(v -> showTab(0));
        if (tabPrivacy != null)      tabPrivacy.setOnClickListener(v -> showTab(1));
        if (tabVerification != null) tabVerification.setOnClickListener(v -> showTab(2));
    }

    private void showTab(int index) {
        if (panelEditProfile != null)  panelEditProfile.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        if (panelPrivacy != null)      panelPrivacy.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        if (panelVerification != null) panelVerification.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        updateTabStyle(tabEditProfile, index == 0);
        updateTabStyle(tabPrivacy, index == 1);
        updateTabStyle(tabVerification, index == 2);
    }

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

    // ── Role Toggle ───────────────────────────────────────────

    private void setupRoleToggle() {
        if (btnRoleStudent != null) btnRoleStudent.setOnClickListener(v -> applyRoleUI("student"));
        if (btnRoleTutor   != null) btnRoleTutor.setOnClickListener(v -> applyRoleUI("tutor"));
    }

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
        styleRoleButton(btnRoleStudent, role.equals("student"));
        styleRoleButton(btnRoleTutor,   role.equals("tutor"));
    }

    private void styleRoleButton(Button btn, boolean active) {
        if (btn == null) return;
        if (active) {
            btn.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_button_gradient));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btn.setTextColor(Color.parseColor("#4B5D7A"));
        }
    }

    // ── Chips ─────────────────────────────────────────────────

    private void setupChips() {
        for (int id : CHIP_IDS) {
            Chip chip = findViewById(id);
            if (chip == null) continue;
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                chip.setChipBackgroundColor(ColorStateList.valueOf(
                        isChecked ? Color.parseColor("#089A3C") : Color.parseColor("#E0E0E0")));
                chip.setTextColor(isChecked ? Color.WHITE : Color.parseColor("#33476A"));
            });
        }
    }

    private void preSelectChips(List<String> subjects) {
        if (subjects == null) return;
        for (int i = 0; i < CHIP_IDS.length; i++) {
            Chip chip = findViewById(CHIP_IDS[i]);
            if (chip != null && subjects.contains(CHIP_NAMES[i])) chip.setChecked(true);
        }
    }

    private List<String> getSelectedSubjects() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < CHIP_IDS.length; i++) {
            Chip chip = findViewById(CHIP_IDS[i]);
            if (chip != null && chip.isChecked()) selected.add(CHIP_NAMES[i]);
        }
        return selected;
    }

    // ── Bio char count ────────────────────────────────────────

    private void setupBioCharCount() {
        if (etBio == null || tvCharCount == null) return;
        etBio.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                tvCharCount.setText(s.length() + "/500 characters");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ── Save Buttons ──────────────────────────────────────────

    private void setupSaveButtons() {
        View btnSaveProfile = findViewById(R.id.btnSaveProfile);
        View btnSavePrivacy = findViewById(R.id.btnSavePrivacy);
        if (btnSaveProfile != null) btnSaveProfile.setOnClickListener(v -> saveProfile());
        if (btnSavePrivacy != null) btnSavePrivacy.setOnClickListener(v -> savePrivacy());
    }

    // ── Logout ────────────────────────────────────────────────

    private void setupLogoutButton() {
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle("Log Out")
                            .setMessage("Are you sure you want to log out?")
                            .setPositiveButton("Log Out", (d, w) -> {
                                FirebaseAuth.getInstance().signOut();
                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show());
        }
    }

    // ── Load Profile ──────────────────────────────────────────

    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(this::populateUI)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    @SuppressWarnings("unchecked")
    private void populateUI(DocumentSnapshot doc) {
        if (!doc.exists()) return;

        String savedRole = doc.getString("role");
        if (savedRole != null) currentRole = savedRole;
        applyRoleUI(currentRole);

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
        if (tvUserName != null) tvUserName.setText(fullName != null ? fullName : "");
        if (tvUserRole != null && savedRole != null && !savedRole.isEmpty()) {
            tvUserRole.setText(Character.toUpperCase(savedRole.charAt(0)) + savedRole.substring(1));
        }

        // Load profile picture if exists
        String picUrl = doc.getString("profilePictureUrl");
        if (picUrl != null && !picUrl.isEmpty() && ivProfilePicture != null) {
            loadImageFromUrl(picUrl);
        }

        if (etEmail != null && currentUser != null)
            etEmail.setText(currentUser.getEmail());

        String institution = doc.getString("institution");
        if (etInstitution != null && institution != null) etInstitution.setText(institution);

        String bio = doc.getString("bio");
        if (etBio != null && bio != null) etBio.setText(bio);

        Long rate = doc.getLong("rate");
        if (etRate != null && rate != null) etRate.setText(String.valueOf(rate));

        preSelectChips((List<String>) doc.get("subjects"));

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

        // Check if doc already submitted
        String verStatus = doc.getString("verificationStatus");
        if ("pending".equals(verStatus) && tvUploadLabel != null) {
            tvUploadLabel.setText("📄 Document submitted for review");
            if (btnSubmitVerification != null) {
                btnSubmitVerification.setText("Submitted ✓");
                btnSubmitVerification.setEnabled(false);
            }
        }
    }

    /**
     * Loads a profile picture URL into the ImageView using just the Android framework
     * (no Glide/Picasso dependency needed for a basic implementation).
     */
    private void loadImageFromUrl(String url) {
        new Thread(() -> {
            try {
                java.net.URL imgUrl = new java.net.URL(url);
                java.net.HttpURLConnection connection =
                        (java.net.HttpURLConnection) imgUrl.openConnection();
                connection.setDoInput(true);
                connection.connect();
                java.io.InputStream input = connection.getInputStream();
                Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);
                runOnUiThread(() -> {
                    if (ivProfilePicture != null && bitmap != null) {
                        ivProfilePicture.setImageBitmap(bitmap);
                        ivProfilePicture.setVisibility(View.VISIBLE);
                        if (tvAvatarInitials != null) tvAvatarInitials.setVisibility(View.GONE);
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    // ── Save Profile ──────────────────────────────────────────

    private void saveProfile() {
        if (currentUser == null) return;

        String firstName = etFirstName != null && etFirstName.getText() != null
                ? etFirstName.getText().toString().trim() : "";
        String lastName = etLastName != null && etLastName.getText() != null
                ? etLastName.getText().toString().trim() : "";

        if (TextUtils.isEmpty(firstName)) {
            if (etFirstName != null) etFirstName.setError("Required"); return;
        }
        if (TextUtils.isEmpty(lastName)) {
            if (etLastName != null) etLastName.setError("Required"); return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName",  lastName);
        updates.put("fullName",  firstName + " " + lastName);
        updates.put("subjects",  getSelectedSubjects());
        updates.put("role",      currentRole);

        if (currentRole.equals("student")) {
            String inst = etInstitution != null && etInstitution.getText() != null
                    ? etInstitution.getText().toString().trim() : "";
            if (!TextUtils.isEmpty(inst)) updates.put("institution", inst);
        } else {
            String bio = etBio != null && etBio.getText() != null
                    ? etBio.getText().toString().trim() : "";
            String rateStr = etRate != null && etRate.getText() != null
                    ? etRate.getText().toString().trim() : "";
            if (!TextUtils.isEmpty(bio)) updates.put("bio", bio);
            if (!TextUtils.isEmpty(rateStr)) {
                try { updates.put("rate", Integer.parseInt(rateStr)); }
                catch (NumberFormatException ignored) {}
            }
        }

        db.collection("users").document(currentUser.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(u ->
                        Toast.makeText(this, "✅ Profile saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void savePrivacy() {
        if (currentUser == null) return;
        Map<String, Object> privacy = new HashMap<>();
        if (switchShowName        != null) privacy.put("showName",        switchShowName.isChecked());
        if (switchShowInstitution != null) privacy.put("showInstitution", switchShowInstitution.isChecked());
        if (switchShowSubjects    != null) privacy.put("showSubjects",    switchShowSubjects.isChecked());
        if (switchShowRate        != null) privacy.put("showRate",        switchShowRate.isChecked());
        if (switchProfileVisible  != null) privacy.put("profileVisible",  switchProfileVisible.isChecked());

        db.collection("users").document(currentUser.getUid())
                .set(privacy, SetOptions.merge())
                .addOnSuccessListener(u ->
                        Toast.makeText(this, "✅ Privacy saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}