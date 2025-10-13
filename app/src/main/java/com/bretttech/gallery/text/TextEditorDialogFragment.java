package com.bretttech.gallery.text;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.bretttech.gallery.R;

public class TextEditorDialogFragment extends DialogFragment {

    public static final String TAG = TextEditorDialogFragment.class.getSimpleName();
    public static final String EXTRA_INPUT_TEXT = "extra_input_text";
    public static final String EXTRA_COLOR_CODE = "extra_color_code";

    private EditText mAddTextEditText;
    private int mColorCode;
    private TextEditorListener mTextEditorListener;

    public interface TextEditorListener {
        void onDone(String inputText, int colorCode);
    }

    public static TextEditorDialogFragment show(@NonNull AppCompatActivity appCompatActivity, @NonNull String inputText, @ColorInt int colorCode) {
        Bundle args = new Bundle();
        args.putString(EXTRA_INPUT_TEXT, inputText);
        args.putInt(EXTRA_COLOR_CODE, colorCode);
        TextEditorDialogFragment fragment = new TextEditorDialogFragment();
        fragment.setArguments(args);
        fragment.show(appCompatActivity.getSupportFragmentManager(), TAG);
        return fragment;
    }

    public static TextEditorDialogFragment show(@NonNull AppCompatActivity appCompatActivity) {
        return show(appCompatActivity, "", ContextCompat.getColor(appCompatActivity, R.color.white));
    }

    public void setOnTextEditorListener(@NonNull TextEditorListener textEditorListener) {
        mTextEditorListener = textEditorListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_text_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAddTextEditText = view.findViewById(R.id.add_text_edit_text);
        TextView mAddTextDoneTextView = view.findViewById(R.id.add_text_done_tv);

        // Correctly get the InputMethodManager
        InputMethodManager inputMethodManager = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mAddTextEditText.requestFocus();
        if (inputMethodManager != null) {
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }

        mAddTextDoneTextView.setOnClickListener(v -> {
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            dismiss();
            String inputText = mAddTextEditText.getText().toString();
            if (!TextUtils.isEmpty(inputText) && mTextEditorListener != null) {
                mTextEditorListener.onDone(inputText, mColorCode);
            }
        });

        if (getArguments() != null) {
            mAddTextEditText.setText(getArguments().getString(EXTRA_INPUT_TEXT));
            mColorCode = getArguments().getInt(EXTRA_COLOR_CODE);
            mAddTextEditText.setTextColor(mColorCode);
        }
    }
}