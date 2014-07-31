package maigosoft.mcpdict;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.res.Resources;

public class Orthography {

    // One static inner class for each language.
    // All inner classes (except for Character) have the two following methods:
    //   public static String canonicalize(String s);
    //   public static String display(String s);
    // However, some may require an additional argument (int or boolean) specifying the format.
    // Inner classes for tonal languages also have the following method:
    //   public static List<String> getAllTones(String s);
    // which returns the given *canonicalized* syllable in all possible tones.
    // All methods return null on failure.

    public static class Hanzi {
        public static final char FIRST_HANZI = 0x4E00;
        public static final char LAST_HANZI = 0x9FA5;

        private static char[][] variants = new char[LAST_HANZI - FIRST_HANZI + 1][];

        public static boolean isHanzi(char unicode) {
            return unicode >= FIRST_HANZI && unicode <= LAST_HANZI;
        }

        public static char[] getVariants(char c) {
            char[] var = variants[c - FIRST_HANZI];
            if (var != null) {
                return var;
            }
            else {
                return new char[] {c};
            }
        }
    }

    public static class MiddleChinese {
        private static Map<String, String> mapInitials = new HashMap<String, String>();
        private static Map<String, String> mapFinals = new HashMap<String, String>();
        private static Map<String, String> mapSjep = new HashMap<String, String>(); // 攝
        private static Map<String, String> mapTongx = new HashMap<String, String>();    // 等
        private static Map<String, String> mapHo = new HashMap<String, String>();   // 呼

        public static String canonicalize(String s) {
            // Replace apostrophes with zeros to make SQLite FTS happy
            return s.replace('\'', '0');
        }

        public static String display(String s) {
            // Restore apostrophes
            return s.replace('0', '\'');
        }

        public static String detail(String s) {
            // Get tone first
            int tone = 0;
            switch (s.charAt(s.length() - 1)) {
                case 'x': tone = 1; s = s.substring(0, s.length() - 1); break;
                case 'h': tone = 2; s = s.substring(0, s.length() - 1); break;
                case 'd': tone = 2; break;
                case 'p': tone = 3; s = s.substring(0, s.length() - 1) + "m"; break;
                case 't': tone = 3; s = s.substring(0, s.length() - 1) + "n"; break;
                case 'k': tone = 3; s = s.substring(0, s.length() - 1) + "ng"; break;
            }

            // Split initial and final
            String init = null, fin = null;
            boolean extraJ = false;
            int p = s.indexOf('0');
            if (p >= 0) {               // Abnormal syllables containing apostrophes
                init = s.substring(0, p);
                fin = s.substring(p + 1);
                if (init.equals("i")) init = "";
                if (!mapInitials.containsKey(init)) return null;    // Fail
                if (!mapFinals.containsKey(fin)) return null;       // Fail
            }
            else {
                for (int i = 3; i >= 0; i--) {
                    if (i <= s.length() && mapInitials.containsKey(s.substring(0, i))) {
                        init = s.substring(0, i);
                        fin = s.substring(i);
                        break;
                    }
                }
                if (fin.equals("")) return null;        // Fail

                // Extract extra "j" in syllables that look like 重紐A類
                if (fin.charAt(0) == 'j') {
                    if (fin.length() < 2) return null;  // Fail
                    extraJ = true;
                    if (fin.charAt(1) == 'i' || fin.charAt(1) == 'y') {
                        fin = fin.substring(1);
                    }
                    else {
                        fin = "i" + fin.substring(1);
                    }
                }

                // Recover omitted glide in final
                if (init.endsWith("r")) {       // 只能拼二等或三等韻，二等韻省略介音r
                    if (fin.charAt(0) != 'i' && fin.charAt(0) != 'y') {
                        fin = "r" + fin;
                    }
                }
                else if (init.endsWith("j")) {  // 只能拼三等韻，省略介音i
                    if (fin.charAt(0) != 'i' && fin.charAt(0) != 'y') {
                        fin = "i" + fin;
                    }
                }
            }
            if (!mapFinals.containsKey(fin)) return null;   // Fail

            // Distinguish 重韻
            if (fin.equals("ia")) {         // 牙音声母爲戈韻，其餘爲麻韻
                if (Arrays.asList("k", "kh", "g", "ng").contains(init)) {
                    fin = "Ia";
                }
            }
            else if (fin.equals("ieng") || fin.equals("yeng")) {
                                            // 脣牙喉音声母直接接-ieng,-yeng者及莊組爲庚韻，其餘爲清韻
                if (Arrays.asList("p", "ph", "b", "m",
                                  "k", "kh", "g", "ng",
                                  "h", "gh", "q", "",
                                  "cr", "chr", "zr", "sr", "zsr").contains(init) && !extraJ) {
                    fin = (fin.equals("ieng")) ? "Ieng" : "Yeng";
                }
            }
            else if (fin.equals("in")) {    // 莊組声母爲臻韻，其餘爲眞韻
                if (Arrays.asList("cr", "chr", "zr", "sr", "zsr").contains(init)) {
                    fin = "In";
                }
            }
            else if (fin.equals("yn")) {    // 脣牙喉音声母直接接-yn者爲眞韻，其餘爲諄韻
                if (Arrays.asList("p", "ph", "b", "m",
                          "k", "kh", "g", "ng",
                          "h", "gh", "q", "").contains(init) && !extraJ) {
                    fin = "Yn";
                }
            }

            // Resolve 重紐
            String dryungNriux = "";
            if ("支脂祭眞仙宵侵鹽".indexOf(mapFinals.get(fin).charAt(0)) >= 0 &&
                    Arrays.asList("p", "ph", "b", "m",
                                  "k", "kh", "g", "ng",
                                  "h", "gh", "q", "", "j").contains(init)) {
                dryungNriux = (extraJ || init.equals("j")) ? "A" : "B";
            }

            // Render details
            String mux = mapInitials.get(init);
            String sjep = mapSjep.get(fin);
            char yonh = mapFinals.get(fin).charAt(fin.endsWith("d") ? 0 : tone);
            String tongx = mapTongx.get(fin);
            String ho = mapHo.get(fin);
            char sjeng = "平上去入".charAt(tone);

            return mux + sjep + yonh + dryungNriux + tongx + ho + sjeng;
            // return mux + "母 " + sjep + "攝 " + yonh + dryungNriux + "韻 " + tongx + "等 " + ho + "口 " + sjeng + "声";
        }

        public static List<String> getAllTones(String s) {
            if (s == null || s.equals("")) return null;                 // Fail
            String base = s.substring(0, s.length() - 1);
            if (base.equals("")) return null;                           // Fail
            switch (s.charAt(s.length() - 1)) {
                case 'x': return Arrays.asList(s, base, base + "h");    // 上 -> 上,平,去
                case 'h': return Arrays.asList(s, base, base + "x");    // 去 -> 去,平,上
                case 'd': case 'p': case 't': case 'k':
                          return Arrays.asList(s);                      // 次入、入 -> self
                default:  return Arrays.asList(s, s + "x", s + "h");    // 平 -> 平,上,去
            }
        }
    }

    public static class Mandarin {
        private static Map<String, String> map = new HashMap<String, String>();
        private static char[] vowels = {'a', 'o', 'e', 'i', 'u', 'v', 'n', 'm'};

        public static String canonicalize(String s) {
            StringBuilder sb = new StringBuilder();
            char tone = '_';
            for (int i = 0; i < s.length(); i++) {
                String key = s.substring(i, i + 1);
                if (map.containsKey(key)) {
                    String value = map.get(key);
                    char base = value.charAt(0);
                    if (base != '_') sb.append(base);
                    tone = value.charAt(1);
                }
                else {
                    sb.append(s.charAt(i));
                }
            }
            return sb.toString() + (tone == '_' ? "" : tone);
        }

        public static String display(String s) {
            // Get tone
            char tone = s.charAt(s.length() - 1);
            if (tone >= '1' && tone <= '4') {
                s = s.substring(0, s.length() - 1);
            }
            else {
                tone = '_';
            }
            // Find letter to carry the tone
            int pos = -1;
            if (s.endsWith("iu")) {     // In the combination "iu", "u" gets the tone
                pos = s.length() - 1;
            }
            else {                      // Find letter in this order: a,o,e,i,u,v,n,m
                for (char c : vowels) {
                    pos = s.indexOf(c);
                    if (pos >= 0) break;
                }
            }
            if (pos == -1) return null; // Fail
            // Add tone to letter
            String key = String.valueOf(s.charAt(pos)) + tone;
            if (map.containsKey(key)) {
                return s.substring(0, pos) + map.get(key) + s.substring(pos + 1, s.length());
            }
            else if (tone == '_') {
                return s;
            }
            else {
                return s.substring(0, pos + 1) + map.get("_" + tone) + s.substring(pos + 1, s.length());
            }
        }

        public static List<String> getAllTones(String s) {
            if (s == null || s.equals("")) return null;     // Fail
            char tone = s.charAt(s.length() - 1);
            String base = s;
            if (tone >= '1' && tone <= '4') {
                base = s.substring(0, s.length() - 1);
            }
            else {
                tone = '_';
            }
            if (base.equals("")) return null;               // Fail

            List<String> result = new ArrayList<String>();
            result.add(s);
            for (char c = '1'; c <= '4'; c++) {
                if (c != tone) {
                    result.add(base + c);
                }
            }
            if (tone != '_') {
                result.add(base);
            }
            return result;
        }
    }

    public static class Cantonese {
        public static final int JYUTPING = 0;   // This is the database representation
        public static final int CANTONESE_PINYIN = 1;
        public static final int YALE = 2;
        public static final int SIDNEY_LAU = 3;
        // References:
        // http://en.wikipedia.org/wiki/Jyutping
        // http://en.wikipedia.org/wiki/Cantonese_Pinyin
        // http://en.wikipedia.org/wiki/Yale_romanization_of_Cantonese
        // http://en.wikipedia.org/wiki/Sidney_Lau
        // http://humanum.arts.cuhk.edu.hk/Lexis/lexi-can/

        private static final Map<String, String> mapInitialsJ2C = new HashMap<String, String>();
        private static final Map<String, String> mapInitialsJ2Y = new HashMap<String, String>();
        private static final Map<String, String> mapInitialsJ2L = new HashMap<String, String>();
        private static final Map<String, String> mapInitialsC2J = new HashMap<String, String>();
        private static final Map<String, String> mapInitialsY2J = new HashMap<String, String>();
        private static final Map<String, String> mapInitialsL2J = new HashMap<String, String>();
        private static final Map<String, String> mapFinalsJ2C = new HashMap<String, String>();
        private static final Map<String, String> mapFinalsJ2Y = new HashMap<String, String>();
        private static final Map<String, String> mapFinalsJ2L = new HashMap<String, String>();
        private static final Map<String, String> mapFinalsC2J = new HashMap<String, String>();
        private static final Map<String, String> mapFinalsY2J = new HashMap<String, String>();
        private static final Map<String, String> mapFinalsL2J = new HashMap<String, String>();

        public static String canonicalize(String s, int system) {
            // Convert from given system to Jyutping

            // Choose map first
            Map<String, String> mapInitials = null, mapFinals = null;
            switch (system) {
                case JYUTPING:          return s;
                case CANTONESE_PINYIN:  mapInitials = mapInitialsC2J; mapFinals = mapFinalsC2J; break;
                case YALE:              mapInitials = mapInitialsY2J; mapFinals = mapFinalsY2J; break;
                case SIDNEY_LAU:        mapInitials = mapInitialsL2J; mapFinals = mapFinalsL2J; break;
            }

            // Get tone
            char tone = s.charAt(s.length() - 1);
            if (tone >= '1' && tone <= '9') {
                s = s.substring(0, s.length() - 1);
            }
            else {
                tone = '_';
            }

            // Get final
            int p = 0;
            while (p < s.length() && !mapFinals.containsKey(s.substring(p))) p++;
            if (p == s.length()) return null;   // Fail
            String fin = mapFinals.get(s.substring(p));

            // Get initial
            String init = s.substring(0, p);
            if (p > 0) {
                if (!mapInitials.containsKey(s.substring(0, p))) return null;
                init = mapInitials.get(s.substring(0, p));
            }

            // In Cantonese Pinyin, tones 7,8,9 are used for entering tones
            // They need to be replaced by with 1,3,6 in Jyutping
            switch (tone) {
                case '7': tone = '1'; break;
                case '8': tone = '3'; break;
                case '9': tone = '6'; break;
            }

            // In Yale, initial "y" is omitted if final begins with "yu"
            // If that happens, we need to put the initial "j" back in Jyutping
            if (system == YALE && init.equals("") && fin.startsWith("yu")) init = "j";

            return init + fin + (tone == '_' ? "" : tone);
        }

        public static String display(String s, int system) {
            // Convert from Jyutping to given system

            // Choose map first
            Map<String, String> mapInitials = null, mapFinals = null;
            switch (system) {
                case JYUTPING:          return s;
                case CANTONESE_PINYIN:  mapInitials = mapInitialsJ2C; mapFinals = mapFinalsJ2C; break;
                case YALE:              mapInitials = mapInitialsJ2Y; mapFinals = mapFinalsJ2Y; break;
                case SIDNEY_LAU:        mapInitials = mapInitialsJ2L; mapFinals = mapFinalsJ2L; break;
            }

            // Get tone
            char tone = s.charAt(s.length() - 1);
            if (tone >= '1' && tone <= '6') {
                s = s.substring(0, s.length() - 1);
            }
            else {
                tone = '_';
            }

            // Get final
            int p = 0;
            while (p < s.length() && !mapFinals.containsKey(s.substring(p))) p++;
            if (p == s.length()) return null;   // Fail
            String fin = mapFinals.get(s.substring(p));

            // Get initial
            String init = s.substring(0, p);
            if (p > 0) {
                if (!mapInitials.containsKey(s.substring(0, p))) return null;
                init = mapInitials.get(s.substring(0, p));
            }

            // In Cantonese Pinyin, tones 7,8,9 are used for entering tones
            if (system == CANTONESE_PINYIN && "ptk".indexOf(fin.charAt(fin.length() - 1)) >= 0) {
                switch (tone) {
                    case '1': tone = '7'; break;
                    case '3': tone = '8'; break;
                    case '6': tone = '9'; break;
                }
            }

            // In Yale, initial "y" is omitted if final begins with "yu"
            if (system == YALE && init.equals("y") && fin.startsWith("yu")) init = "";

            return init + fin + (tone == '_' ? "" : tone);
        }

        public static List<String> getAllTones(String s) {
            if (s == null || s.equals("")) return null;     // Fail
            char tone = s.charAt(s.length() - 1);
            String base = s;
            if (tone >= '1' && tone <= '6') {
                base = s.substring(0, s.length() - 1);
            }
            else {
                tone = '_';
            }
            if (base.equals("")) return null;               // Fail

            boolean isEnteringTone = "ptk".indexOf(base.charAt(base.length() - 1)) >= 0;
            List<String> result = new ArrayList<String>();
            result.add(s);
            if (tone != '1')                    result.add(base + '1');
            if (tone != '2' && !isEnteringTone) result.add(base + '2');
            if (tone != '3')                    result.add(base + '3');
            if (tone != '4' && !isEnteringTone) result.add(base + '4');
            if (tone != '5' && !isEnteringTone) result.add(base + '5');
            if (tone != '6')                    result.add(base + '6');
            return result;
        }
    }

    public static class Korean {
        public static final int HANGUL = 0;
        public static final int ROMANIZATION = 1;   // This is the database representation

        public static final char FIRST_HANGUL = 0xAC00;
        public static final char LAST_HANGUL = 0xD7A3;

        // Arrays: index to spelling
        private static final String[] initials = {"g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h"};
        private static final String[] vowels = {"a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i"};
        private static final String[] finals = {"", "k", "kk0", "ks0", "n", "nj0", "nh0", "d0", "l", "lg0", "lm0", "lb0", "ls0", "lt0", "lp0", "lh0", "m", "p", "bs0", "s0", "ss0", "ng", "j0", "ch0", "k0", "t0", "p0", "h0"};
            // Finals with "0" are not valid pronunciations of Chinese characters
            // And they won't match anything in the database
        // Maps: spelling to index
        private static final Map<String, Integer> mapInitials = new HashMap<String, Integer>();
        private static final Map<String, Integer> mapVowels = new HashMap<String, Integer>();
        private static final Map<String, Integer> mapFinals = new HashMap<String, Integer>();

        public static boolean isHangul(char c) {
            return c >= FIRST_HANGUL && c <= LAST_HANGUL;
        }

        public static String canonicalize(String s) {
            // Input can be either a hangul, or non-canonicalized romanization
            if (s.length() == 0) return s;
            char unicode = s.charAt(0);
            if (isHangul(unicode)) {    // Hangul
                unicode -= FIRST_HANGUL;
                int z = unicode % finals.length;
                int x = unicode / finals.length;
                int y = x % vowels.length;
                x /= vowels.length;
                return initials[x] + vowels[y] + finals[z];
            }
            else {      // Romanization, do some obvious corrections
                if (s.startsWith("l")) s = "r" + s.substring(1);
                    else if (s.startsWith("gg")) s = "kk" + s.substring(2);
                    else if (s.startsWith("dd")) s = "tt" + s.substring(2);
                    else if (s.startsWith("bb")) s = "pp" + s.substring(2);
                s = s.replace("weo", "wo").replace("oi", "oe").replace("eui", "ui");
                if (s.endsWith("r")) s = s.substring(0, s.length() - 1) + "l";
                    else if (s.endsWith("g") && !s.endsWith("ng")) s = s.substring(0, s.length() - 1) + "k";
                    else if (s.endsWith("d")) s = s.substring(0, s.length() - 1) + "t";
                    else if (s.endsWith("b")) s = s.substring(0, s.length() - 1) + "p";
                return s;
            }
        }

        public static String display(String s, int style) {
            if (style == ROMANIZATION) return s;

            int L = s.length();
            int x, y, z, p, q;
            // Initial
            p = 0;
            for (int i = 2; i > 0; i--) {
                if (i <= L && mapInitials.containsKey(s.substring(0, i))) {
                    p = i; break;
                }
            }
            x = mapInitials.get(s.substring(0, p));
            // Final
            q = L;
            for (int i = L - 2; i < L; i++) {
                if (i >= p && mapFinals.containsKey(s.substring(i))) {
                    q = i; break;
                }
            }
            z = mapFinals.get(s.substring(q));
            // Vowel
            if (!mapVowels.containsKey(s.substring(p, q))) return null; // Fail
            y = mapVowels.get(s.substring(p, q));
            return String.valueOf((char) (FIRST_HANGUL + (x * vowels.length + y) * finals.length + z));
        }
    }

    public static class Vietnamese {
        public static final int OLD_STYLE = 0;
        public static final int NEW_STYLE = 1;

        private static Map<String, String> map = new HashMap<String, String>();

        public static String canonicalize(String s) {
            // Input can be either with diacritics, or non-canonicalized Telex string
            char tone = '_';
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (i > 0 && "zrsfxj".indexOf(c) >= 0 && !(i == 1 && s.startsWith("tr"))) { // c is tone
                    if (c != 'z') tone = c;
                    continue;
                }
                String key = s.substring(i, i + 1);
                if (map.containsKey(key)) {
                    String value = map.get(key);
                    String base = value.substring(0, value.length() - 1);
                    sb.append(base);
                    c = value.charAt(value.length() - 1);
                    if (c != '_') tone = c;
                }
                else {
                    sb.append(s.charAt(i));
                }
            }

            // Canonicalizing "y" and "i":
            // At the beginning of a word, use "y" if it's the only letter, or if it's followed by "e"
            // At other places, both "y" and "i" can occur after "a" or "u", but only "i" can occur after other letters
            for (int i = 0; i < sb.length(); i++) {
                if (sb.charAt(i) == 'y' || sb.charAt(i) == 'i') {
                    if (i == 0) {
                        sb.setCharAt(0, (sb.length() == 1 || sb.charAt(1) == 'e') ? 'y' : 'i');
                    }
                    else {
                        if (sb.charAt(i-1) != 'a' && sb.charAt(i-1) != 'u') sb.setCharAt(i, 'i');
                    }
                }
            }
            return sb.toString() + (tone == '_' ? "" : tone);
        }

        // Rules for placing the tone marker follows this page in Vietnamese Wikipedia:
        // Quy tắc đặt dấu thanh trong chữ quốc ngữ
        public static String display(String s, int style) {
            // Get tone
            char tone = s.charAt(s.length() - 1);
            if ("rsfxj".indexOf(tone) >= 0) {
                s = s.substring(0, s.length() - 1);
            }
            else {
                tone = '_';
            }

            // If any vowel carries quality marker, put tone marker there, too
            // In the combination "ươ", "ơ" gets the tone marker
            StringBuilder sb = new StringBuilder();
            int p = 0;
            while (p < s.length()) {
                if (p == s.length() - 1) {
                    sb.append(s.charAt(p));
                    break;
                }
                String key = s.substring(p, p+2);
                if (map.containsKey(key + "_")) {
                    if (key.equals("dd") || p+4 <= s.length() && s.substring(p, p+4).equals("uwow")) {
                        sb.append(map.get(key + "_"));      // Tone marker doesn't go here
                    }
                    else {
                        sb.append(map.get(key + tone));     // Tone marker goes here
                        tone = '_';
                    }
                    p += 2;
                }
                else {
                    sb.append(s.charAt(p++));
                }
            }
            if (tone == '_') return sb.toString();          // No tone marker to place

            // Place tone marker
            // Find first and last vowel
            p = 0; while (p < sb.length() && "aeiouy".indexOf(sb.charAt(p)) < 0) p++;
            if (p == sb.length()) return null;              // There has to be a vowel, otherwise fail
            int q = p + 1;  while (q < sb.length() && "aeiouy".indexOf(sb.charAt(q)) >= 0) q++;
            // Decide which vowel to get the tone marker
            if (q - p == 3 ||
                q - p == 2 && (q < sb.length() ||
                               s.startsWith("gi") ||
                               s.startsWith("qu") ||
                               (style == NEW_STYLE) && (sb.substring(p, q).equals("oa") ||
                                                        sb.substring(p, q).equals("oe") ||
                                                        sb.substring(p, q).equals("uy")))) p++;
            // Place the tone marker
            sb.setCharAt(p, map.get(String.valueOf(sb.charAt(p)) + tone).charAt(0));
            return sb.toString();
        }

        public static List<String> getAllTones(String s) {
            if (s == null || s.equals("")) return null;     // Fail
            char tone = s.charAt(s.length() - 1);
            String base = s;
            if ("rsfxj".indexOf(tone) >= 0) {
                base = s.substring(0, s.length() - 1);
            }
            else {
                tone = '_';
            }
            if (base.equals("")) return null;               // Fail

            boolean isEnteringTone = "ptc".indexOf(base.charAt(base.length() - 1)) >= 0 ||
                                     base.endsWith("ch");
            List<String> result = new ArrayList<String>();
            result.add(s);
            if (tone != '_' && !isEnteringTone) result.add(base);
            if (tone != 'r' && !isEnteringTone) result.add(base + 'r');
            if (tone != 's')                    result.add(base + 's');
            if (tone != 'f' && !isEnteringTone) result.add(base + 'f');
            if (tone != 'x' && !isEnteringTone) result.add(base + 'x');
            if (tone != 'j')                    result.add(base + 'j');
            return result;
        }
    }

    public static class Japanese {
        public static final int HIRAGANA = 0;
        public static final int KATAKANA = 1;
        public static final int NIPPON = 2;     // This is the database representation
        public static final int HEPBURN = 3;
        // Reference: Japanese Wikipedia ローマ字

        private static final Map<String, String> mapHiragana = new HashMap<String, String>();
        private static final Map<String, String> mapKatakana = new HashMap<String, String>();
        private static final Map<String, String> mapNippon = new HashMap<String, String>();
        private static final Map<String, String> mapHepburn = new HashMap<String, String>();

        public static String convertTo(String s, int system) {
            // Choose map
            Map<String, String> map = null;
            switch (system) {
                case HIRAGANA:  map = mapHiragana;  break;
                case KATAKANA:  map = mapKatakana;  break;
                case NIPPON:    map = mapNippon;    break;
                case HEPBURN:   map = mapHepburn;   break;
            }

            StringBuilder sb = new StringBuilder();
            int p = 0;
            while (p < s.length()) {
                int q = p;
                for (int i = 4; i > 0; i--) {
                    if (p + i <= s.length() && map.containsKey(s.substring(p, p + i))) {
                        q = p + i;
                        sb.append(map.get(s.substring(p, q)));
                        break;
                    }
                }
                if (q == p) return null;
                p = q;
            }

            return sb.toString();
        }

        public static String canonicalize(String s) {
            return convertTo(s, NIPPON);
        }

        public static String display(String s, int system) {
            return (system == NIPPON) ? s : convertTo(s, system);
        }
    }

    // Initialization code
    public static void initialize(Resources resources) {
        if (initialized) return;

        InputStream inputStream;
        BufferedReader reader;
        String line;
        String[] fields;

        try {
            // Character variants
            inputStream = resources.openRawResource(R.raw.orthography_hz_variants);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = reader.readLine()) != null) {
                char c = line.charAt(0);
                Hanzi.variants[c - Hanzi.FIRST_HANZI] = line.toCharArray();
            }
            reader.close();

            // Middle Chinese
            inputStream = resources.openRawResource(R.raw.orthography_mc_initials);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = reader.readLine()) != null) {
                if (line.equals("") || line.charAt(0) == '#') continue;
                fields = line.split("\\s+");
                if (fields[0].equals("_")) fields[0] = "";
                MiddleChinese.mapInitials.put(fields[0], fields[1]);
            }
            reader.close();

            inputStream = resources.openRawResource(R.raw.orthography_mc_finals);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = reader.readLine()) != null) {
                if (line.equals("") || line.charAt(0) == '#') continue;
                fields = line.split("\\s+");
                MiddleChinese.mapSjep.put(fields[0], fields[1]);
                MiddleChinese.mapTongx.put(fields[0], fields[2]);
                MiddleChinese.mapHo.put(fields[0], fields[3]);
                MiddleChinese.mapFinals.put(fields[0], fields[4]);
            }
            reader.close();

            // Mandarin
            inputStream = resources.openRawResource(R.raw.orthography_pu);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = reader.readLine()) != null) {
                if (line.equals("") || line.charAt(0) == '#') continue;
                fields = line.split("\\s+");
                Mandarin.map.put(fields[0], fields[1] + fields[2]);
                Mandarin.map.put(fields[1] + fields[2], fields[0]);
            }
            reader.close();

            // Cantonese
            inputStream = resources.openRawResource(R.raw.orthography_ct_initials);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = reader.readLine()) != null) {
                if (line.equals("") || line.charAt(0) == '#') continue;
                fields = line.split("\\s+");
                Cantonese.mapInitialsJ2C.put(fields[0], fields[1]);
                Cantonese.mapInitialsJ2Y.put(fields[0], fields[2]);
                Cantonese.mapInitialsJ2L.put(fields[0], fields[3]);
                Cantonese.mapInitialsC2J.put(fields[1], fields[0]);
                Cantonese.mapInitialsY2J.put(fields[2], fields[0]);
                Cantonese.mapInitialsL2J.put(fields[3], fields[0]);
            }
            reader.close();

            inputStream = resources.openRawResource(R.raw.orthography_ct_finals);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = reader.readLine()) != null) {
                if (line.equals("") || line.charAt(0) == '#') continue;
                fields = line.split("\\s+");
                Cantonese.mapFinalsJ2C.put(fields[0], fields[1]);
                Cantonese.mapFinalsJ2Y.put(fields[0], fields[2]);
                Cantonese.mapFinalsJ2L.put(fields[0], fields[3]);
                Cantonese.mapFinalsC2J.put(fields[1], fields[0]);
                Cantonese.mapFinalsY2J.put(fields[2], fields[0]);
                Cantonese.mapFinalsL2J.put(fields[3], fields[0]);
            }
            reader.close();

            // Korean
            for (int i = 0; i < Korean.initials.length; i++) {
                Korean.mapInitials.put(Korean.initials[i], i);
            }
            for (int i = 0; i < Korean.vowels.length; i++) {
                Korean.mapVowels.put(Korean.vowels[i], i);
            }
            for (int i = 0; i < Korean.finals.length; i++) {
                Korean.mapFinals.put(Korean.finals[i], i);
            }

            // Vietnamese
            inputStream = resources.openRawResource(R.raw.orthography_vn);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = reader.readLine()) != null) {
                if (line.equals("") || line.charAt(0) == '#') continue;
                fields = line.split("\\s+");
                Vietnamese.map.put(fields[0], fields[1] + fields[2]);
                Vietnamese.map.put(fields[1] + fields[2], fields[0]);
            }
            reader.close();

            // Japanese
            inputStream = resources.openRawResource(R.raw.orthography_jp);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = reader.readLine()) != null) {
                if (line.equals("") || line.charAt(0) == '#') continue;
                fields = line.split("\\s+");
                for (int i = 0; i < 4; i++) {
                    Japanese.mapHiragana.put(fields[i], fields[0]);
                    Japanese.mapKatakana.put(fields[i], fields[1]);
                    Japanese.mapNippon.put(fields[i], fields[2]);
                    Japanese.mapHepburn.put(fields[i], fields[3]);
                }
            }
            reader.close();
        }
        catch (IOException e) {}

        initialized = true;
    }

    private static boolean initialized = false;
}
