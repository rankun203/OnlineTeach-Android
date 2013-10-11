package org.rankun.onlineteach.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Pattern;


/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class Login extends Activity {
    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"};

    /**
     * The default email to populate the email field with.
     */
    public static final String EXTRA_USERNAME = "org.rankun.onlineteach.android.Login.EXTRA_USERNAME";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // Values for email and password at the time of the login attempt.
    private String mUserRole;
    private String mUsername;
    private String mPassword;
    private boolean mAutoLogin;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;
    private RadioGroup mLoginRoleRadio;
    private RadioButton mLoginStudentRadioBtn;
    private RadioButton mLoginTeacherRadioBtn;
    private CheckBox mAutoLoginCheckbox;
    private TextView mMainTips2;

    //ERROR msg if login failed.
    private String httpQueryStatus;
    private String httpQueryMsg;

    //SavedPreferences.
    private SharedPreferences userInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        //init UI References.
        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mAutoLoginCheckbox = (CheckBox) findViewById(R.id.login_auto_login);
        mLoginRoleRadio = (RadioGroup) findViewById(R.id.login_user_role);
        mMainTips2 = (TextView) findViewById(R.id.login_tips2);
        mLoginStudentRadioBtn = (RadioButton) findViewById(R.id.role_student);
        mLoginTeacherRadioBtn = (RadioButton) findViewById(R.id.role_teacher);

        //Get saved user info & set up login form.
        userInfo = this.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        String savedUserRole = userInfo.getString("userRole", "student");
        mUserRole = savedUserRole;
        if (!savedUserRole.equals("")) {
            if (savedUserRole.equals("teacher")) {
                mLoginRoleRadio.check(R.id.role_teacher);
                mUsernameView.setHint(R.string.prompt_username);
            } else {
                mLoginRoleRadio.check(R.id.role_student);
                mUsernameView.setHint(R.string.prompt_student_id);
            }
        }
        String savedUserName = userInfo.getString("userName", "");
        mUsernameView.setText(savedUserName);
        String savedPassWord = userInfo.getString("passWord", "");
        mPasswordView.setText(savedPassWord);
        mAutoLoginCheckbox.setChecked(false);

        //Switch Hint of EditText
        mLoginStudentRadioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRole();
            }
        });
        mLoginTeacherRadioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRole();
            }
        });

        //Attempt login after password is given.
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });


        findViewById(R.id.sign_in_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        attemptLogin();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mUsername = mUsernameView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username.
        // TODO Distinguish Student No. & Teacher Username.
        if (TextUtils.isEmpty(mUsername)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (mUserRole.equals("student")) {
            boolean invalidStudentId = !Pattern.matches("^\\d*$", mUsername);
            if (invalidStudentId) mUsernameView.setError(getString(R.string.error_student_id));
            focusView = mUsernameView;
            cancel = invalidStudentId;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            mAuthTask = new UserLoginTask();
            mAuthTask.execute((Void) null);
        }

    }

    /**
     * Change hint to clicked role.
     */
    private void toggleRole() {
        mMainTips2.setTextColor(Color.BLACK);
        mMainTips2.setError(null);
        if (mLoginRoleRadio.getCheckedRadioButtonId() == R.id.role_student) {
            mMainTips2.setText(R.string.main_tip_student_);
            mUsernameView.setHint(R.string.prompt_student_id);
            mUserRole = "student";
        } else {
            mMainTips2.setText(R.string.main_tip_teacher_);
            mUsernameView.setHint(R.string.prompt_username);
            mUserRole = "teacher";
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(
                    android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate().setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE
                                    : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            AndroidApplication application = (AndroidApplication) getApplication();
            String queryUrl = application.getSERVER_URL();
            if (mUserRole.equals("student")) {
                queryUrl += "android/findStudent?username=" + mUsername + "&password=" + mPassword;
            } else if (mUserRole.equals("teacher")) {
                queryUrl += "android/findTeacher?username=" + mUsername + "&password=" + mPassword;
            }
Log.d("OT", "queryUrl: " + queryUrl);
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet request = new HttpGet(queryUrl);
                request.addHeader("Accept", "application/json");
                HttpResponse response = httpClient.execute(request);
                HttpEntity entity = response.getEntity();
                String json = EntityUtils.toString(entity, "utf-8");
                if (json != null && !json.equals("")) {
Log.d("OT", "Login return: " + json);
                    JSONObject jsonObject = new JSONObject(json);
                    String status = jsonObject.getString("status");
                    if (status != null && !TextUtils.isEmpty(status)) {
                        if (TextUtils.equals(status, "ok")) {
                            return true;
                        } else if(TextUtils.equals(status, "no-user")) {
                            httpQueryStatus = status;
                            httpQueryMsg = jsonObject.getString("msg");
                            return false;
                        }
                    }
                } else {
                    return false;
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                Log.e("OT", "json数据有误");
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                //Store username and password for auto Login
                if (mAutoLogin) {
                    
                }
                finish();
            } else {
                if (!TextUtils.isEmpty(httpQueryStatus) && TextUtils.equals(httpQueryStatus, "no-user")) {
                    mMainTips2.setTextColor(Color.RED);
                    mMainTips2.setText(httpQueryMsg);
                    mMainTips2.setError("");

//                    mPasswordView.setError(getString(R.string.error_incorrect_password));
//                    mPasswordView.requestFocus();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
