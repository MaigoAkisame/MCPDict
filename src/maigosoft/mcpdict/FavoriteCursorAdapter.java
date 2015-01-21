package maigosoft.mcpdict;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mobiRic.ui.widget.Boast;

public class FavoriteCursorAdapter extends CursorAdapter {

    private FavoriteFragment fragment;
    private int layout;
    private LayoutInflater inflater;

    public FavoriteCursorAdapter(Context context, FavoriteFragment fragment, int layout, Cursor c) {
        super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
        this.fragment = fragment;
        this.layout = layout;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(layout, parent, false);
    }

    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {
        final char c;
        String string;
        TextView textView;

        // Chinese character
        string = cursor.getString(cursor.getColumnIndex("unicode"));
        c = (char)Integer.parseInt(string, 16);
        string = String.valueOf(c);
        textView = (TextView) view.findViewById(R.id.text_hz);
        textView.setText(string);

        // Timestamp
        string = cursor.getString(cursor.getColumnIndex("local_timestamp"));
        textView = (TextView) view.findViewById(R.id.text_timestamp);
        textView.setText(string);

        // Comment
        string = cursor.getString(cursor.getColumnIndex("comment"));
        textView = (TextView) view.findViewById(R.id.text_comment);
        textView.setText(string);

        // "Delete" button
        final Button button = (Button) view.findViewById(R.id.button_delete);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserDatabase.toggleFavorite(c);
                int messageId = R.string.favorite_delete_done;
                String message = context.getResources().getString(messageId).replace("X", String.valueOf(c));
                Boast.showText(context, message, Toast.LENGTH_SHORT);
                fragment.refresh(false);
            }
        });
    }
}
