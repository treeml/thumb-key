package com.dessalines.thumbkey.utils;

import android.view.textservice.SuggestionsInfo;
import java.lang.reflect.Method;

/**
 * SuggestionsInfo.getSuggestion(int) is missing from the API 35 compile-time
 * stub but is present at runtime. Access it via reflection.
 */
public class SpellCheckerHelper {
    private static volatile Method sGetSuggestion;

    private static Method findMethod() {
        if (sGetSuggestion != null) return sGetSuggestion;
        for (String name : new String[]{"getSuggestion", "getSuggestionAt"}) {
            try {
                Method m = SuggestionsInfo.class.getMethod(name, int.class);
                sGetSuggestion = m;
                return m;
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    public static String getFirstSuggestion(SuggestionsInfo info) {
        if (info.getSuggestionsCount() <= 0) return null;
        Method m = findMethod();
        if (m == null) return null;
        try {
            return (String) m.invoke(info, 0);
        } catch (Exception e) {
            return null;
        }
    }
}
