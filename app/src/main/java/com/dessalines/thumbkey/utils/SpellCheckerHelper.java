package com.dessalines.thumbkey.utils;

import android.os.Parcel;
import android.view.textservice.SuggestionsInfo;
import java.lang.reflect.Method;

/**
 * SuggestionsInfo.getSuggestion(int) is absent from the API 35 compile-time stub but exists at
 * runtime. We try reflection first; if that fails we read suggestions via Parcel (which relies on
 * the stable Parcelable contract: writeInt(attrs) + writeStringArray(suggestions) + ...).
 */
public class SpellCheckerHelper {
    private static volatile Method sGetSuggestionMethod;
    private static volatile boolean sMethodSearched = false;

    private static Method findMethod() {
        if (sMethodSearched) return sGetSuggestionMethod;
        sMethodSearched = true;
        for (String name : new String[]{"getSuggestion", "getSuggestionAt"}) {
            try {
                Method m = SuggestionsInfo.class.getMethod(name, int.class);
                sGetSuggestionMethod = m;
                return m;
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    public static String[] getAllSuggestions(SuggestionsInfo info) {
        int count = info.getSuggestionsCount();
        if (count <= 0) return new String[0];

        // Try reflection (works at runtime even when the stub doesn't expose the method)
        Method m = findMethod();
        if (m != null) {
            try {
                String[] results = new String[count];
                for (int i = 0; i < count; i++) {
                    Object r = m.invoke(info, i);
                    results[i] = r != null ? r.toString() : "";
                }
                return results;
            } catch (Exception ignored) {}
        }

        // Fallback: SuggestionsInfo.writeToParcel writes attrs(int) then suggestions(String[])
        Parcel parcel = Parcel.obtain();
        try {
            info.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            parcel.readInt(); // mSuggestionsAttributes
            String[] suggestions = parcel.createStringArray();
            return suggestions != null ? suggestions : new String[0];
        } catch (Exception e) {
            return new String[0];
        } finally {
            parcel.recycle();
        }
    }
}
