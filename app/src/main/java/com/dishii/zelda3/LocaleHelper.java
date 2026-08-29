package com.dishii.zelda3;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class LocaleHelper {

    public static String getEffectiveLanguage(Context context, String configLang) {
        if (configLang != null && !configLang.trim().isEmpty()) {
            return configLang.trim();
        }
        // Fallback to system locale
        Locale sysLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sysLocale = Resources.getSystem().getConfiguration().getLocales().get(0);
        } else {
            sysLocale = Resources.getSystem().getConfiguration().locale;
        }
        if (sysLocale != null && sysLocale.getLanguage().toLowerCase().startsWith("pt")) {
            return "pt";
        }
        return "us";
    }

    public static Context applyLocale(Context context, String langCode) {
        String eff = getEffectiveLanguage(context, langCode);
        Locale locale = getLocaleForCode(eff);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }

    public static void updateResources(Context context, String langCode) {
        String eff = getEffectiveLanguage(context, langCode);
        Locale locale = getLocaleForCode(eff);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
        } else {
            config.locale = locale;
        }
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    public static Locale getLocaleForCode(String langCode) {
        if (langCode == null || langCode.isEmpty()) {
            return Locale.ENGLISH;
        }
        if (langCode.equalsIgnoreCase("us") || langCode.equalsIgnoreCase("en")) {
            return Locale.ENGLISH;
        } else if (langCode.equalsIgnoreCase("pt") || langCode.equalsIgnoreCase("pt-br") || langCode.equalsIgnoreCase("pt_br")) {
            return new Locale("pt", "BR");
        }
        return new Locale(langCode);
    }
}
