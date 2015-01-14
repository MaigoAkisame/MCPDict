package maigosoft.mcpdict;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
// Use android.widget.CursorAdapter for API level >= 11
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.mobiRic.ui.widget.Boast;

public class CustomCursorAdapter extends CursorAdapter implements Masks {

    private Context context;
    private int layout;
    private LayoutInflater inflater;

    public CustomCursorAdapter(Context context, int layout, Cursor c) {
        super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
        this.context = context;
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
        StringBuilder sb;
        TextView textView;
        int tag = 0;

        // Unicode
        string = cursor.getString(cursor.getColumnIndex("unicode"));
        textView = (TextView) view.findViewById(R.id.text_unicode);
        textView.setText("U+" + string);
        tag |= MASK_UNICODE;

        // Chinese character
        c = (char)Integer.parseInt(string, 16);
        string = String.valueOf(c);
        textView = (TextView) view.findViewById(R.id.text_hz);
        textView.setText(string);
        tag |= MASK_HZ;

        // Variants
        string = cursor.getString(cursor.getColumnIndex("variants"));
        textView = (TextView) view.findViewById(R.id.text_variants);
        if (string == null) {
            textView.setVisibility(View.GONE);
        }
        else {
            sb = new StringBuilder();
            for (String s : string.split(" ")) {
                sb.append((char)Integer.parseInt(s, 16));
            }
            textView.setText("(" + sb.toString() + ")");
            textView.setVisibility(View.VISIBLE);
        }

        // Middle Chinese
        string = cursor.getString(cursor.getColumnIndex("mc"));
        textView = (TextView) view.findViewById(R.id.text_mc);
        textView.setText(middleChineseDisplayer.display(string));
        textView = (TextView) view.findViewById(R.id.text_mc_detail);
        if (string != null) {
            textView.setText(middleChineseDetailDisplayer.display(string));
            tag |= MASK_MC;
        }
        else {
            textView.setText("");
        }

        // Mandarin
        string = cursor.getString(cursor.getColumnIndex("pu"));
        textView = (TextView) view.findViewById(R.id.text_pu);
        textView.setText(mandarinDisplayer.display(string));
        if (string != null) tag |= MASK_PU;

        // Cantonese
        string = cursor.getString(cursor.getColumnIndex("ct"));
        textView = (TextView) view.findViewById(R.id.text_ct);
        textView.setText(cantoneseDisplayer.display(string));
        if (string != null) tag |= MASK_CT;

        // Korean
        string = cursor.getString(cursor.getColumnIndex("kr"));
        textView = (TextView) view.findViewById(R.id.text_kr);
        textView.setText(koreanDisplayer.display(string));
        if (string != null) tag |= MASK_KR;

        // Vietnamese
        string = cursor.getString(cursor.getColumnIndex("vn"));
        textView = (TextView) view.findViewById(R.id.text_vn);
        textView.setText(vietnameseDisplayer.display(string));
        if (string != null) tag |= MASK_VN;

        // Japanese go-on
        string = cursor.getString(cursor.getColumnIndex("jp_go"));
        textView = (TextView) view.findViewById(R.id.text_jp_go);
        setRichText(textView, japaneseDisplayer.display(string));
        if (string != null) tag |= MASK_JP_GO;

        // Japanese kan-on
        string = cursor.getString(cursor.getColumnIndex("jp_kan"));
        textView = (TextView) view.findViewById(R.id.text_jp_kan);
        setRichText(textView, japaneseDisplayer.display(string));
        if (string != null) tag |= MASK_JP_KAN;

        // Japanese extras
        ImageView[] imageViewJPExtras = {
                (ImageView) view.findViewById(R.id.image_jp_extra_1),
                (ImageView) view.findViewById(R.id.image_jp_extra_2),
                (ImageView) view.findViewById(R.id.image_jp_extra_3)
        };
        TextView[] textViewJPExtras = {
                (TextView) view.findViewById(R.id.text_jp_extra_1),
                (TextView) view.findViewById(R.id.text_jp_extra_2),
                (TextView) view.findViewById(R.id.text_jp_extra_3)
        };
        int i = 0;

        // Japanese tou-on
        string = cursor.getString(cursor.getColumnIndex("jp_tou"));
        if (string != null) {
            imageViewJPExtras[i].setImageResource(R.drawable.lang_jp_tou);
            setRichText(textViewJPExtras[i], japaneseDisplayer.display(string));
            i++;
            tag |= MASK_JP_TOU;
        }

        // Japanese kwan'you-on
        string = cursor.getString(cursor.getColumnIndex("jp_kwan"));
        if (string != null) {
            imageViewJPExtras[i].setImageResource(R.drawable.lang_jp_kwan);
            setRichText(textViewJPExtras[i], japaneseDisplayer.display(string));
            i++;
            tag |= MASK_JP_KWAN;
        }

        // Japanese other pronunciations
        string = cursor.getString(cursor.getColumnIndex("jp_other"));
        if (string != null) {
            imageViewJPExtras[i].setImageResource(R.drawable.lang_jp_other);
            setRichText(textViewJPExtras[i], japaneseDisplayer.display(string));
            i++;
            tag |= MASK_JP_OTHER;
        }

        for (int j = 0; j < i; j++) {
            imageViewJPExtras[j].setVisibility(View.VISIBLE);
            textViewJPExtras[j].setVisibility(View.VISIBLE);
        }
        for (int j = i; j < 3; j++) {
            imageViewJPExtras[j].setVisibility(View.GONE);
            textViewJPExtras[j].setVisibility(View.GONE);
        }

        // Favorite button
        boolean favorite = cursor.getInt(cursor.getColumnIndex("favorite")) == 1;
        final Button star = (Button) view.findViewById(R.id.button_favorite);
        star.setBackgroundResource(favorite ? R.drawable.ic_star_yellow : R.drawable.ic_star_white);
        star.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean favorite = UserDatabase.toggleFavorite(c);
                star.setBackgroundResource(favorite ? R.drawable.ic_star_yellow : R.drawable.ic_star_white);
                view.setTag((Integer) view.getTag() ^ MASK_FAVORITE);
                int messageId = favorite ? R.string.favorite_add_done : R.string.favorite_remove_done;
                Resources r = context.getResources();
                String message = r.getString(messageId).replace("X", String.valueOf(c));
                Boast.showText(context, message, Toast.LENGTH_SHORT);
            }
        });
        if (favorite) {
            tag |= MASK_FAVORITE;
        }

        // Set the view's tag to indicate which readings exist
        view.setTag(tag);
    }

    public void setRichText(TextView view, String richTextString) {
        StringBuilder sb = new StringBuilder();
        List<Integer> stars = new ArrayList<Integer>();
        List<Integer> slashes = new ArrayList<Integer>();

        for (int i = 0; i < richTextString.length(); i++) {
            char c = richTextString.charAt(i);
            switch (c) {
                case '*': stars.add(sb.length()); break;
                case '/': slashes.add(sb.length()); break;
                default : sb.append(c); break;
            }
        }

        view.setText(sb.toString(), BufferType.SPANNABLE);
        Spannable spannable = (Spannable) view.getText();
        for (int i = 1; i < stars.size(); i += 2) {
            spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), stars.get(i-1), stars.get(i), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        for (int i = 1; i < slashes.size(); i += 2) {
            spannable.setSpan(new ForegroundColorSpan(0xFF808080), slashes.get(i-1), slashes.get(i), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
    }

    private abstract static class Displayer {
        protected static final String NULL_STRING = "-";

        public String display(String s) {
            if (s == null) return NULL_STRING;
            s = lineBreak(s);
            // Find all regions of [a-z0-9]+ in s, and apply displayer to each of them
            StringBuilder sb = new StringBuilder();
            int L = s.length(), p = 0;
            while (p < L) {
                int q = p;
                while (q < L && Character.isLetterOrDigit(s.charAt(q))) q++;
                if (q > p) {
                    String t1 = s.substring(p, q);
                    String t2 = displayOne(t1);
                    sb.append(t2 == null ? t1 : t2);
                    p = q;
                }
                while (p < L && !Character.isLetterOrDigit(s.charAt(p))) p++;
                sb.append(s.substring(q, p));
            }
            // Add spaces as hints for line wrapping
            s = sb.toString().replace(",", ", ")
                             .replace("(", " (")
                             .replace("]", "] ")
                             .replaceAll(" +", " ");
            return s;
        }

        public String lineBreak(String s) {return s;}
        public abstract String displayOne(String s);
    }

    private final Displayer middleChineseDisplayer = new Displayer() {
        public String lineBreak(String s) {return s.replace(",", "\n");}
        public String displayOne(String s) {return Orthography.MiddleChinese.display(s);}
    };

    private final Displayer middleChineseDetailDisplayer = new Displayer() {
        public String lineBreak(String s) {return s.replace(",", "\n");}
        public String displayOne(String s) {return "(" + Orthography.MiddleChinese.detail(s) + ")";}
    };

    private final Displayer mandarinDisplayer = new Displayer() {
        public String displayOne(String s) {return Orthography.Mandarin.display(s);}
    };

    private final Displayer cantoneseDisplayer = new Displayer() {
        public String displayOne(String s) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            Resources r = context.getResources();
            int system = sp.getInt(r.getString(R.string.pref_key_cantonese_romanization), 0);
            return Orthography.Cantonese.display(s, system);
        }
    };

    private final Displayer koreanDisplayer = new Displayer() {
        public String displayOne(String s) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            Resources r = context.getResources();
            int style = sp.getInt(r.getString(R.string.pref_key_korean_display), 0);
            return Orthography.Korean.display(s, style);
        }
    };

    private final Displayer vietnameseDisplayer = new Displayer() {
        public String displayOne(String s) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            Resources r = context.getResources();
            int style = sp.getInt(r.getString(R.string.pref_key_vietnamese_tone_position), 0);
            return Orthography.Vietnamese.display(s, style);
        }
    };

    private final Displayer japaneseDisplayer = new Displayer() {
        public String lineBreak(String s) {
            if (s.charAt(0) == '[') {
                s = '[' + s.substring(1).replace("[", "\n[");
            }
            return s;
        }

        public String displayOne(String s) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            Resources r = context.getResources();
            int style = sp.getInt(r.getString(R.string.pref_key_japanese_display), 0);
            return Orthography.Japanese.display(s, style);
        }
    };
}
