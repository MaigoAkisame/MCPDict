package maigosoft.mcpdict;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import maigosoft.mcpdict.R;

public class CustomSearchView extends RelativeLayout {

    private EditText editText;
    private Button clearButton;
    private Button searchButton;

    public CustomSearchView(Context context) {
        this(context, null);
    }

    public CustomSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater)
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.custom_search_view, this, true);

        editText = (EditText) findViewById(R.id.search_query_text);
        clearButton = (Button) findViewById(R.id.search_clear_button);
        searchButton = (Button) findViewById(R.id.search_go_button);

        // Toggle the clear button when user edits text
        editText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                clearButton.setVisibility((s.length() == 0) ? View.GONE : View.VISIBLE);
            }
        });

        // Invoke the search button when user hits Enter
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                searchButton.performClick();
                return true;
            }
        });

        clearButton.setVisibility(View.GONE);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editText.setText("");
            }
        });
    }

    public void setHint(String hint) {
        editText.setHint(hint);
    }

    public void setSearchButtonOnClickListener(final View.OnClickListener listener) {
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Hide the keyboard before performing the search
                editText.clearFocus();
                InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                listener.onClick(v);
            }
        });
    }

    public void clickSearchButton() {
        searchButton.performClick();
    }

    public String getQuery() {
        return editText.getText().toString();
    }
}
