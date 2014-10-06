package com.varuncjain.behancemuzei;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.varuncjain.behancemuzei.ui.hhmmpicker.HHmsPickerBuilder;
import com.varuncjain.behancemuzei.ui.hhmmpicker.HHmsPickerDialogFragment;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.OnDismissCallback;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.SwipeDismissAdapter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsActivity extends FragmentActivity implements OnDismissCallback, HHmsPickerDialogFragment.HHmsPickerDialogHandler {

    private static final String TAG = "SettingsActivity";

    private ListView mList;
    private EditText mUserName;
    private View mEmpty;
    private TextView mConfigConnection;
    private TextView mConfigFreq;
    private TextView mConfigPopular;
    private View mAdd;

    private UserNameAdapter mUserNameAdapter;
    private String mLastUserName;

    private static final Pattern pattern = Pattern.compile("\\s");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceHelper.limitConfigFreq(this);

        setContentView(R.layout.activity_settings);

        setupActionBar();

        mList = (ListView) findViewById(R.id.list);
        mUserName = (EditText) findViewById(R.id.userName);
        mEmpty = findViewById(R.id.empty);
        mConfigConnection = (TextView) findViewById(R.id.config_connection);
        mConfigFreq = (TextView) findViewById(R.id.config_freq);
        mConfigPopular = (TextView) findViewById(R.id.config_popular);
        mAdd = findViewById(R.id.add);

        setupList();
        setupUserNameEditText();
        setupConfig();

        mEmpty.setOnClickListener(mOnEmptyClickListener);

        mAdd.setOnClickListener(mOnAddClickListener);
    }

    private final View.OnClickListener mOnAddClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            addUserName();
        }
    };

    private void setupConfig() {
        mConfigFreq.setOnClickListener(mOnConfigFreqClickListener);
        mConfigConnection.setOnClickListener(mOnConfigConnectionClickListener);
        mConfigPopular.setOnClickListener(mOnConfigPopularClickListener);

        updateConfigFreq();
        updateConfigConnection();
        updateConfigPopular();
    }

    private void updateConfigFreq() {
        int configFreq = PreferenceHelper.getConfigFreq(this);
        mConfigFreq.setText(getString(R.string.config_every, Utils.convertDurationToString(configFreq)));
        // Send an intent to communicate the update with the service
        Intent intent = new Intent(this, BehanceMuzeiSource.class);
        intent.putExtra("configFreq", configFreq);
        startService(intent);
    }

    private void updateConfigConnection() {
        switch (PreferenceHelper.getConfigConnection(this)) {
            case PreferenceHelper.CONNECTION_ALL:
                mConfigConnection.setText(R.string.config_connection_all);
                mConfigConnection.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_config_connection_all, 0, 0, 0);
                break;
            case PreferenceHelper.CONNECTION_WIFI:
                mConfigConnection.setText(R.string.config_connection_wifi);
                mConfigConnection.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_config_connection_wifi, 0, 0, 0);
                break;
        }
    }

    private void updateConfigPopular() {
        switch (PreferenceHelper.getConfigPopular(this)) {
            case PreferenceHelper.USERS_POPULAR_OFF:
                mConfigPopular.setText(R.string.config_popular_off);
                mConfigPopular.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_cancel, 0, 0, 0);
                break;
            case PreferenceHelper.USERS_POPULAR_ON:
                mConfigPopular.setText(R.string.config_popular_on);
                mConfigPopular.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_done, 0, 0, 0);
                break;
        }
    }

    private View.OnClickListener mOnConfigFreqClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            HHmsPickerBuilder hpb = new HHmsPickerBuilder()
                    .setFragmentManager(getSupportFragmentManager())
                    .setStyleResId(R.style.BetterPickersDialogFragment_Light);
            hpb.show();
        }
    };

    @Override
    public void onDialogHmsSet(int reference, int hours, int minutes, int seconds) {
        int duration = hours * 3600000 + minutes * 60000 + seconds * 1000;
        if(duration < PreferenceHelper.MIN_FREQ_MILLIS) {
            Toast.makeText(this, R.string.config_min, Toast.LENGTH_LONG).show();
            duration = PreferenceHelper.MIN_FREQ_MILLIS;
        }
        PreferenceHelper.setConfigFreq(this, duration);
        updateConfigFreq();
    }

    private View.OnClickListener mOnConfigConnectionClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int connection = PreferenceHelper.getConfigConnection(SettingsActivity.this);
            if(connection == PreferenceHelper.CONNECTION_WIFI) {
                mConfigConnection.setText(R.string.config_connection_all);
                PreferenceHelper.setConfigConnection(SettingsActivity.this, PreferenceHelper.CONNECTION_ALL);
            } else if(connection == PreferenceHelper.CONNECTION_ALL) {
                mConfigConnection.setText(R.string.config_connection_wifi);
                PreferenceHelper.setConfigConnection(SettingsActivity.this, PreferenceHelper.CONNECTION_WIFI);
            }
            updateConfigConnection();
        }
    };

    private View.OnClickListener mOnConfigPopularClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int popular = PreferenceHelper.getConfigPopular(SettingsActivity.this);
            if(popular == PreferenceHelper.USERS_POPULAR_ON) {
                mConfigPopular.setText(R.string.config_popular_off);
                PreferenceHelper.setConfigPopular(SettingsActivity.this, PreferenceHelper.USERS_POPULAR_OFF);
            } else if(popular == PreferenceHelper.USERS_POPULAR_OFF) {
                mConfigPopular.setText(R.string.config_popular_on);
                PreferenceHelper.setConfigPopular(SettingsActivity.this, PreferenceHelper.USERS_POPULAR_ON);
            }
            updateConfigPopular();
        }
    };

    private void setupList() {
        mUserNameAdapter = new UserNameAdapter();
        SwipeDismissAdapter swipeDismissAdapter = new SwipeDismissAdapter(mUserNameAdapter, this);
        swipeDismissAdapter.setAbsListView(mList);
        mList.setAdapter(swipeDismissAdapter);
        mList.setEmptyView(mEmpty);
        mList.setOnItemClickListener(mOnUserIdClickListener);
    }

    private AdapterView.OnItemClickListener mOnUserIdClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String userName = mUserNameAdapter.getItem(position);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.behance.net/" + userName)));
        }
    };

    private final View.OnClickListener mOnEmptyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mUserName.requestFocus()) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mUserName, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    };

    private void setupActionBar() {
        final LayoutInflater inflater = getLayoutInflater();
        View actionBarView = inflater.inflate(R.layout.ab_activity_settings, null);
        actionBarView.findViewById(R.id.actionbar_done).setOnClickListener(mOnActionBarDoneClickListener);
        getActionBar().setCustomView(actionBarView);
    }

    private View.OnClickListener mOnActionBarDoneClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private void setupUserNameEditText() {
        mUserName.setImeActionLabel(getString(R.string.add), EditorInfo.IME_ACTION_DONE);
        mUserName.setOnEditorActionListener(mOnEditorActionListener);
    }

    private TextView.OnEditorActionListener mOnEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addUserName();
            }
            return false;
        }
    };

    private void addUserName() {
        String userName = mUserName.getText().toString();
        Matcher matcher = pattern.matcher(userName);

        if(TextUtils.isEmpty(userName) || matcher.find()) {
            Toast.makeText(SettingsActivity.this, R.string.no_user_add, Toast.LENGTH_SHORT).show();
            return;
        }
        mLastUserName = userName;
        mUserNameAdapter.add(userName);
        mUserName.setText(null);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void animatedAdd(final ViewGroup view) {
        final TextView overlay = (TextView) view.findViewById(R.id.overlay);

        ViewGroup hostView = (ViewGroup) findViewById(android.R.id.content);
        final ViewGroupOverlay viewGroupOverlay = hostView.getOverlay();
        viewGroupOverlay.add(overlay);
        overlay.offsetTopAndBottom(mList.getTop());

        float width = overlay.getPaint().measureText(mLastUserName);

        overlay.setPivotX(width);
        overlay.setPivotY(0.0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(overlay, "scaleX", 1, 3f),
                ObjectAnimator.ofFloat(overlay, "scaleY", 1, 3f),
                ObjectAnimator.ofFloat(overlay, "alpha", 1, 0.0f)
        );
        set.start();
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                viewGroupOverlay.remove(overlay);
                view.addView(overlay);
            }
        });
    }

    @Override
    public void onDismiss(ViewGroup viewGroup, int[] reverseSortedPositions) {
        for (int position : reverseSortedPositions) {
            mUserNameAdapter.remove(position);
        }
    }

    private class UserNameAdapter extends BaseAdapter {

        private List<String> mUserNames;

        private UserNameAdapter() {
            mUserNames = PreferenceHelper.userNamesFromPref(SettingsActivity.this);
        }

        void add(String projectId) {
            if(mUserNames.contains(projectId)) {
                return;
            }
            mUserNames.add(0, projectId);
            updateUsersInPref();
            notifyDataSetChanged();
        }

        void remove(int position) {
            mUserNames.remove(position);
            updateUsersInPref();
            notifyDataSetChanged();
        }

        private void updateUsersInPref() {
            PreferenceHelper.userNamesToPref(SettingsActivity.this, mUserNames);
        }

        @Override
        public int getCount() {
            return mUserNames.size();
        }

        @Override
        public String getItem(int position) {
            return mUserNames.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.listitem_username, parent, false);
            }

            TextView projectId = (TextView) convertView.findViewById(R.id.userName);
            TextView overlay = (TextView) convertView.findViewById(R.id.overlay);

            final String id = getItem(position);
            projectId.setText(id);
            if(overlay != null && id.equals(mLastUserName) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                overlay.setText(id);
                animatedAdd((ViewGroup) convertView);
                mLastUserName = null;
            }

            return convertView;
        }
    }
}
