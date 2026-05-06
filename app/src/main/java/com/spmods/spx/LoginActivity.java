package com.spmods.spx;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends Activity {

    // ── SharedPreferences keys ───────────────────────────────────────────────
    private static final String PREFS_NAME   = "SPMODS_AUTH";
    private static final String KEY_UID      = "uid";
    private static final String KEY_EMAIL    = "email";
    private static final String KEY_NAME     = "name";
    private static final String KEY_LOGGED   = "logged_in";

    // ── Firebase Database URL ────────────────────────────────────────────────
    private static final String DB_URL =
            "https://spwhatsapp-5103f-default-rtdb.asia-southeast1.firebasedatabase.app/";

    // ── Firebase ─────────────────────────────────────────────────────────────
    private FirebaseAuth      mAuth;
    private DatabaseReference mDbRef;

    // ── UI ───────────────────────────────────────────────────────────────────
    private LinearLayout loginPanel, signupPanel;

    private EditText etLoginEmail, etLoginPass;
    private EditText etSignupName, etSignupEmail, etSignupPass, etSignupConfirm;

    private Button   btnLogin, btnSignup;
    private TextView tvSwitchToSignup, tvSwitchToLogin;
    private ProgressBar progressBar;

    private ImageView ivToggleLoginPass, ivToggleSignupPass, ivToggleSignupConfirm;

    private boolean loginPassVisible  = false;
    private boolean signupPassVisible = false;
    private boolean signupConfVisible = false;

    // ── Lifecycle ────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ① Internal storage check — no network, instant
        if (isLoggedInLocally()) {
            goToMain();
            return;
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_login);

        // ② Init Firebase with specific DB URL
        mAuth  = FirebaseAuth.getInstance();
        mDbRef = FirebaseDatabase.getInstance(DB_URL).getReference("users");

        // ③ Firebase session check — only if local storage empty
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchAndSaveUser(currentUser.getUid(), currentUser.getEmail());
            return;
        }

        bindViews();
        setupListeners();
        showLogin();
    }

    // ── Internal Storage ─────────────────────────────────────────────────────

    private boolean isLoggedInLocally() {
        SharedPreferences p = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return p.getBoolean(KEY_LOGGED, false)
                && !TextUtils.isEmpty(p.getString(KEY_UID, ""));
    }

    private void saveUserLocally(String uid, String email, String name) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean(KEY_LOGGED, true)
                .putString(KEY_UID,    uid)
                .putString(KEY_EMAIL,  email)
                .putString(KEY_NAME,   name)
                .apply();
    }

    /** Logout helper — call from anywhere */
    public static void logout(Activity activity) {
        activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
        FirebaseAuth.getInstance().signOut();
        Intent i = new Intent(activity, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(i);
    }

    // ── Firebase: fetch user name then save locally ───────────────────────────

    private void fetchAndSaveUser(String uid, String email) {
        showLoading(true);
        mDbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                showLoading(false);
                String name = "User";
                if (snap.exists() && snap.child("name").getValue(String.class) != null) {
                    name = snap.child("name").getValue(String.class);
                } else if (email != null) {
                    name = email.split("@")[0];
                }
                saveUserLocally(uid, email != null ? email : "", name);
                goToMain();
            }
            @Override
            public void onCancelled(DatabaseError e) {
                showLoading(false);
                // Save with minimal info and proceed
                saveUserLocally(uid, email != null ? email : "", "User");
                goToMain();
            }
        });
    }

    // ── View Binding ─────────────────────────────────────────────────────────

    private void bindViews() {
        loginPanel  = findViewById(R.id.login_panel);
        signupPanel = findViewById(R.id.signup_panel);

        etLoginEmail    = findViewById(R.id.et_login_email);
        etLoginPass     = findViewById(R.id.et_login_pass);

        etSignupName    = findViewById(R.id.et_signup_name);
        etSignupEmail   = findViewById(R.id.et_signup_email);
        etSignupPass    = findViewById(R.id.et_signup_pass);
        etSignupConfirm = findViewById(R.id.et_signup_confirm);

        btnLogin        = findViewById(R.id.btn_login);
        btnSignup       = findViewById(R.id.btn_signup);

        tvSwitchToSignup = findViewById(R.id.tv_switch_signup);
        tvSwitchToLogin  = findViewById(R.id.tv_switch_login);

        progressBar           = findViewById(R.id.progress_bar);
        ivToggleLoginPass     = findViewById(R.id.iv_toggle_login_pass);
        ivToggleSignupPass    = findViewById(R.id.iv_toggle_signup_pass);
        ivToggleSignupConfirm = findViewById(R.id.iv_toggle_signup_confirm);
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void setupListeners() {
        btnLogin.setOnClickListener(v  -> doLogin());
        btnSignup.setOnClickListener(v -> doSignup());

        tvSwitchToSignup.setOnClickListener(v -> showSignup());
        tvSwitchToLogin.setOnClickListener(v  -> showLogin());

        ivToggleLoginPass.setOnClickListener(v -> {
            loginPassVisible = !loginPassVisible;
            togglePass(etLoginPass, ivToggleLoginPass, loginPassVisible);
        });
        ivToggleSignupPass.setOnClickListener(v -> {
            signupPassVisible = !signupPassVisible;
            togglePass(etSignupPass, ivToggleSignupPass, signupPassVisible);
        });
        ivToggleSignupConfirm.setOnClickListener(v -> {
            signupConfVisible = !signupConfVisible;
            togglePass(etSignupConfirm, ivToggleSignupConfirm, signupConfVisible);
        });
    }

    private void togglePass(EditText et, ImageView iv, boolean show) {
        et.setTransformationMethod(show
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
        iv.setImageResource(show ? R.drawable.ic_eye_off : R.drawable.ic_eye);
        et.setSelection(et.getText().length());
    }

    // ── Panel Switch ─────────────────────────────────────────────────────────

    private void showLogin() {
        loginPanel.setVisibility(View.VISIBLE);
        signupPanel.setVisibility(View.GONE);
    }

    private void showSignup() {
        loginPanel.setVisibility(View.GONE);
        signupPanel.setVisibility(View.VISIBLE);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    private void doLogin() {
        String email = etLoginEmail.getText().toString().trim();
        String pass  = etLoginPass.getText().toString().trim();

        if (TextUtils.isEmpty(email)) { etLoginEmail.setError("Email ඇතුළු කරන්න"); return; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.setError("Valid email එකක් දෙන්න"); return;
        }
        if (TextUtils.isEmpty(pass)) { etLoginPass.setError("Password ඇතුළු කරන්න"); return; }

        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> {
                    FirebaseUser u = r.getUser();
                    if (u != null) fetchAndSaveUser(u.getUid(), u.getEmail());
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("password")) {
                        toast("Password වැරදියි");
                    } else if (msg != null && msg.contains("no user")) {
                        toast("Account හොයාගත නොහැක");
                    } else {
                        toast("Login වීමට නොහැකි විය");
                    }
                });
    }

    // ── Signup ────────────────────────────────────────────────────────────────

    private void doSignup() {
        String name    = etSignupName.getText().toString().trim();
        String email   = etSignupEmail.getText().toString().trim();
        String pass    = etSignupPass.getText().toString().trim();
        String confirm = etSignupConfirm.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name))    { etSignupName.setError("නම ඇතුළු කරන්න"); return; }
        if (name.length() < 2)          { etSignupName.setError("නම characters 2කට වඩා"); return; }
        if (TextUtils.isEmpty(email))   { etSignupEmail.setError("Email ඇතුළු කරන්න"); return; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etSignupEmail.setError("Valid email එකක් දෙන්න"); return;
        }
        if (pass.length() < 6)          { etSignupPass.setError("අවම characters 6ක්"); return; }
        if (!pass.equals(confirm))      { etSignupConfirm.setError("Passwords match නැහැ"); return; }

        showLoading(true);
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> {
                    FirebaseUser u = r.getUser();
                    if (u == null) { showLoading(false); return; }

                    // Save to Firebase Realtime Database
                    UserModel user = new UserModel(name, email);
                    mDbRef.child(u.getUid()).setValue(user)
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                saveUserLocally(u.getUid(), email, name);
                                goToMain();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                // Auth created but DB write failed — still proceed
                                saveUserLocally(u.getUid(), email, name);
                                goToMain();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("already in use")) {
                        toast("Email එක දැනටමත් registered");
                        showLogin();
                    } else {
                        toast("Account සෑදීම අසාර්ථකයි");
                    }
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnSignup.setEnabled(!show);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // ── Firebase User Model ───────────────────────────────────────────────────

    public static class UserModel {
        public String name;
        public String email;
        public long   createdAt;

        public UserModel() {}

        public UserModel(String name, String email) {
            this.name      = name;
            this.email     = email;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
