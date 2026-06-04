package com.dessalines.thumbkey.utils;

import android.view.textservice.SuggestionsInfo;

public class SpellCheckerHelper {
    public static String getFirstSuggestion(SuggestionsInfo info) {
        if (info.getSuggestionsCount() > 0) {
            return info.getSuggestion(0);
        }
        return null;
    }
}
