package com.varuncjain.behancemuzei.ui.hhmmpicker;


import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.varuncjain.behancemuzei.R;

import java.util.Vector;

/**
 * Dialog to set alarm time.
 */
public class HHmsPickerDialogFragment extends DialogFragment {

    private static final String REFERENCE_KEY = "HHmsPickerDialogFragment_ReferenceKey";
    private static final String THEME_RES_ID_KEY = "HHmsPickerDialogFragment_ThemeResIdKey";

    private Button mSet, mCancel;
    private HHmsPicker mPicker;

    private int mReference = -1;
    private int mTheme = -1;
    private View mDividerOne, mDividerTwo;
    private int mDividerColor;
    private ColorStateList mTextColor;
    private int mButtonBackgroundResId;
    private int mDialogBackgroundResId;
    private Vector<HHmsPickerDialogHandler> mHHmsPickerDialogHandlers = new Vector<HHmsPickerDialogHandler>();

    /**
     * Create an instance of the Picker (used internally)
     *
     * @param reference an (optional) user-defined reference, helpful when tracking multiple Pickers
     * @param themeResId the style resource ID for theming
     * @return a Picker!
     */
    public static HHmsPickerDialogFragment newInstance(int reference, int themeResId) {
        final HHmsPickerDialogFragment frag = new HHmsPickerDialogFragment();
        Bundle args = new Bundle();
        args.putInt(REFERENCE_KEY, reference);
        args.putInt(THEME_RES_ID_KEY, themeResId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null && args.containsKey(REFERENCE_KEY)) {
            mReference = args.getInt(REFERENCE_KEY);
        }
        if (args != null && args.containsKey(THEME_RES_ID_KEY)) {
            mTheme = args.getInt(THEME_RES_ID_KEY);
        }

        setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        // Init defaults
        mTextColor = getResources().getColorStateList(R.color.dialog_text_color_holo_dark);
        mButtonBackgroundResId = R.drawable.button_background_dark;
        mDividerColor = getResources().getColor(R.color.default_divider_color_dark);
        mDialogBackgroundResId = R.drawable.dialog_full_holo_dark;

        if (mTheme != -1) {
            TypedArray a = getActivity().getApplicationContext()
                    .obtainStyledAttributes(mTheme, R.styleable.BetterPickersDialogFragment);

            mTextColor = a.getColorStateList(R.styleable.BetterPickersDialogFragment_bpTextColor);
            mButtonBackgroundResId = a.getResourceId(R.styleable.BetterPickersDialogFragment_bpButtonBackground,
                    mButtonBackgroundResId);
            mDividerColor = a.getColor(R.styleable.BetterPickersDialogFragment_bpDividerColor, mDividerColor);
            mDialogBackgroundResId = a
                    .getResourceId(R.styleable.BetterPickersDialogFragment_bpDialogBackground, mDialogBackgroundResId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.hhms_picker_dialog, null);
        mSet = (Button) v.findViewById(R.id.set_button);
        mCancel = (Button) v.findViewById(R.id.cancel_button);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        mPicker = (HHmsPicker) v.findViewById(R.id.hms_picker);
        mPicker.setSetButton(mSet);
        mSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (HHmsPickerDialogHandler handler : mHHmsPickerDialogHandlers) {
                    handler.onDialogHmsSet(mReference, mPicker.getHours(), mPicker.getMinutes(), mPicker.getSeconds());
                }
                final Activity activity = getActivity();
                final Fragment fragment = getTargetFragment();
                if (activity instanceof HHmsPickerDialogHandler) {
                    final HHmsPickerDialogHandler act =
                            (HHmsPickerDialogHandler) activity;
                    act.onDialogHmsSet(mReference, mPicker.getHours(), mPicker.getMinutes(), mPicker.getSeconds());
                } else if (fragment instanceof HHmsPickerDialogHandler) {
                    final HHmsPickerDialogHandler frag =
                            (HHmsPickerDialogHandler) fragment;
                    frag.onDialogHmsSet(mReference, mPicker.getHours(), mPicker.getMinutes(), mPicker.getSeconds());
                }
                dismiss();
            }
        });

        mDividerOne = v.findViewById(R.id.divider_1);
        mDividerTwo = v.findViewById(R.id.divider_2);
        mDividerOne.setBackgroundColor(mDividerColor);
        mDividerTwo.setBackgroundColor(mDividerColor);
        mSet.setTextColor(mTextColor);
        mSet.setBackgroundResource(mButtonBackgroundResId);
        mCancel.setTextColor(mTextColor);
        mCancel.setBackgroundResource(mButtonBackgroundResId);
        mPicker.setTheme(mTheme);
        getDialog().getWindow().setBackgroundDrawableResource(mDialogBackgroundResId);

        return v;
    }

    /**
     * This interface allows objects to register for the Picker's set action.
     */
    public interface HHmsPickerDialogHandler {

        void onDialogHmsSet(int reference, int hours, int minutes, int seconds);
    }

    /**
     * Attach a Vector of handlers to be notified in addition to the Fragment's Activity and target Fragment.
     *
     * @param handlers a Vector of handlers
     */
    public void setHHmsPickerDialogHandlers(Vector<HHmsPickerDialogHandler> handlers) {
        mHHmsPickerDialogHandlers = handlers;
    }
}