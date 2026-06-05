package com.dessalines.thumbkey.utils;

import android.os.Parcel;
import android.view.textservice.SuggestionsInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SpellCheckerHelper {

    public static String[] getAllSuggestions(SuggestionsInfo info) {
        int count = info.getSuggestionsCount();
        if (count <= 0) return new String[0];

        // Approach 1: getSuggestions() zero-arg method returns String[] directly.
        // Present since API 14, @Deprecated since API 31 but still in the runtime.
        try {
            Method m = SuggestionsInfo.class.getDeclaredMethod("getSuggestions");
            m.setAccessible(true);
            Object r = m.invoke(info);
            if (r instanceof String[]) {
                String[] arr = (String[]) r;
                if (arr.length > 0) return arr;
            }
        } catch (Exception ignored) {}

        // Approach 2: getSuggestion(int) or getSuggestionAt(int) individually.
        for (String name : new String[]{"getSuggestion", "getSuggestionAt"}) {
            try {
                Method m = SuggestionsInfo.class.getDeclaredMethod(name, int.class);
                m.setAccessible(true);
                String[] results = new String[count];
                for (int i = 0; i < count; i++) {
                    Object r = m.invoke(info, i);
                    results[i] = r != null ? r.toString() : "";
                }
                return results;
            } catch (Exception ignored) {}
        }

        // Approach 3: direct field access on mSuggestions.
        try {
            Field f = SuggestionsInfo.class.getDeclaredField("mSuggestions");
            f.setAccessible(true);
            Object r = f.get(info);
            if (r instanceof String[]) {
                String[] arr = (String[]) r;
                if (arr.length > 0) return arr;
            }
        } catch (Exception ignored) {}

        // Approach 4: Parcel — writeToParcel contract: int(attrs) + String[](suggestions) + ...
        Parcel parcel = Parcel.obtain();
        try {
            info.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            parcel.readInt(); // mSuggestionsAttributes
            String[] suggestions = parcel.createStringArray();
            if (suggestions != null && suggestions.length > 0) return suggestions;
        } catch (Exception ignored) {
        } finally {
            parcel.recycle();
        }

        return new String[0];
    }
}
