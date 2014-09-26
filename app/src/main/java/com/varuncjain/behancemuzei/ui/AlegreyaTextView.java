package com.varuncjain.behancemuzei.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class AlegreyaTextView extends TextView {

    public AlegreyaTextView(Context context) {
        super(context);
        setTypeFace(context);
    }

    public AlegreyaTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTypeFace(context);
    }

    public AlegreyaTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setTypeFace(context);
    }

    private void setTypeFace(Context context) {
        if(isInEditMode()) {
            return;
        }
        Typeface tf = TypefaceUtil.getAndCache(context, "Alegreya-BlackItalic.ttf");
        setTypeface(tf);
    }
}
