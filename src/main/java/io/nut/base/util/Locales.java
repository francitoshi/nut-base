/*
 * Locales.java
 *
 * Copyright (c) 2012-2024 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//singleton holder (Efective Java 2nd Ed - Joshua Bloch)
class Country
{
    final String iso2;
    final String iso3;
    final String nameDefault;
    final String nameEnglish;

    public Country(String iso2, String iso3, String nameDefault, String nameEnglish)
    {
        this.iso2 = iso2;
        this.iso3 = iso3;
        this.nameDefault = nameDefault;
        this.nameEnglish = nameEnglish;
    }
}
//singleton holder (Efectife Java 2nd Ed - Joshua Bloch)
enum CountryHolder
{
    INSTANCE;
    
    final HashMap<String,Country> map = new HashMap();
    final Locale defaultLocale = Locale.getDefault();
    
    CountryHolder()
    {
        for(String code : Locale.getISOCountries())
        {
            Locale locale = new Locale("", code, "");
            
            String iso2  = locale.getCountry();
            String iso3  = locale.getISO3Country();
            String nameDefault = locale.getDisplayCountry(defaultLocale);
            String nameEnglish = locale.getDisplayCountry(Locale.US);
            
            Country country = new Country(iso2, iso3, nameDefault, nameEnglish);
            
            map.put(code.toLowerCase(),country);
            map.put(iso2.toLowerCase(),country);
            map.put(iso3.toLowerCase(),country);
            map.put(nameDefault.toLowerCase(),country);
            if(!nameEnglish.equalsIgnoreCase(nameDefault))
            {
                map.put(nameEnglish.toLowerCase(),country);
            }
        }
        for(Locale locale : Locale.getAvailableLocales())
        {
            String nameNative  = locale.getDisplayCountry(locale).toLowerCase();
            if(!nameNative.isEmpty() && !map.containsKey(nameNative))
            {
                String code  = locale.getCountry().toLowerCase();
                Country country = map.get(code);
                if(country!=null)
                {
                    map.put(nameNative, country);
                }
            }
        }
    }
    boolean populate(String code)
    {
        return false;
    }
    
    public String iso2(String val)
    {
        Country country = val!=null?map.get(val.toLowerCase()):null;
        return country!=null?country.iso2:null;
    }
    public String iso3(String val)
    {
        Country country = val!=null?map.get(val.toLowerCase()):null;
        return country!=null?country.iso3:null;
    }
    public String name(String val,Locale locale)
    {
        Country country = val!=null?map.get(val.toLowerCase()):null;
        String name = null;
        if(country!=null)
        {
            if(locale==defaultLocale)
                name = country.nameDefault;
            else if(locale==Locale.US)
                name = country.nameEnglish;
            else 
                name = new Locale("",country.iso2,"").getDisplayCountry(locale);
        }
        return name;
    }
    public String name(String val)
    {
        return name(val,Locale.getDefault());
    }
    
}
class Language
{
    final String iso2;
    final String iso3;
    final String nameDefault;
    final String nameNative;
    final String nameEnglish;

    public Language(String iso2, String iso3, String nameDefault, String nameNative, String nameEnglish)
    {
        this.iso2 = iso2;
        this.iso3 = iso3;
        this.nameDefault = nameDefault;
        this.nameNative  = nameNative;
        this.nameEnglish = nameEnglish;
    }
}
    //singleton holder (Efectife Java 2nd Ed - Joshua Bloch) addapted copy from CountryHolder
    enum LanguageHolder
    {
        INSTANCE;

        final HashMap<String,Language> map = new HashMap();
        final Locale defaultLocale = Locale.getDefault();

        LanguageHolder()
        {
            for(String code : Locale.getISOLanguages())
            {
                populate(code, map, defaultLocale);
            }
        }

        static Language populate(String code, Map<String,Language> map, Locale defaultLocale)
        {
            try
            {
                Locale locale = new Locale(code, "", "");

                String iso2  = locale.getLanguage();
                String iso3  = locale.getISO3Language();
                String nameDefault = locale.getDisplayLanguage(defaultLocale);
                String nameNative  = locale.getDisplayLanguage(locale);
                String nameEnglish = locale.getDisplayLanguage(Locale.US);

                //spanish and interligue uses interligua for ia and ie
                if(iso2.equals("ie"))
                {
                    nameDefault=nameNative=nameEnglish;
                }

                Language language = new Language(iso2, iso3, nameDefault, nameNative, nameEnglish);

                map.put(code.toLowerCase(),language);
                map.put(iso3.toLowerCase(),language);
                map.put(nameDefault.toLowerCase(),language);
                boolean nativeEqualsDefault = nameNative.equalsIgnoreCase(nameDefault);
                boolean englishEqualsNative = nameEnglish.equalsIgnoreCase(nameNative);
                boolean englishEqualsDefault = (englishEqualsNative&&nativeEqualsDefault) || nameEnglish.equalsIgnoreCase(nameDefault);
                if(!nativeEqualsDefault)
                {
                    map.put(nameNative.toLowerCase(),language);
                }
                if(!englishEqualsDefault)
                {
                    map.put(nameEnglish.toLowerCase(),language);
                }
                return language;
            }
            catch(Exception ex)
            {
                Logger.getLogger(LanguageHolder.class.getName()).log(Level.CONFIG,"error populating language",ex);
                return null;
            }
        }
        private static final HashMap<String,String> alt = new HashMap()
        {
            {
                //alternative names, java names
                put("he","iw");
                put("yi","ji");
                put("id","in");
                put("alb", "sqi");
                put("arm", "hye");
                put("baq", "eus");
                put("bur", "mya");
                put("chi", "zho");
                put("cze", "ces");
                put("dut", "nld");
                put("fre", "fra");
                put("ger", "deu");
                put("geo", "kat");
                put("gre", "ell");
                put("ice", "isl");
                put("mac", "mkd");
                put("mao", "mri");
                put("may", "msa");
                put("per", "fas");
                put("rum", "ron");
                put("slo", "slk");
                put("tib", "bod");
                put("wel", "cym");
                put("dhivehi", "divehi");
                put("limburgan","limburgish");
                put("romansh","raeto-romance");
                put("sinhala","sinhalese");
            }
        };

        private final HashMap<String,String> macro = new HashMap()
        {
            {
                put("nb","no");
                put("nn","no");
                put("nob","nor");
                put("nno", "nor");
            }
        };
        private final HashMap<String,String[]> macroComponents = new HashMap()
        {
            {
                String[] no = {"nb","nn"};
                String[] nor= {"nob","nno"};
                put("no", no);
                put("nor", nor);
            }
        };

        private String altToISO(String key)
        {
            String val = alt.get(key);
            return val!=null?val:key;
        }

        private Language getLanguage(String code)
        {
            if(code!=null)
            {
                String iso = altToISO(code.toLowerCase());
                Language lang = map.get(iso);
                return lang!=null? lang : populate(iso,map,defaultLocale);
            }
            return null;
        }

        public String iso2(String val)
        {
            Language language = getLanguage(val);
            return language!=null?language.iso2:null;
        }
        public String iso3(String val)
        {
            Language language = getLanguage(val);
            return language!=null?language.iso3:null;
        }
        public String name(String val,Locale locale)
        {
            Language language = getLanguage(val);
            String name = null;
            if(language!=null)
            {
                if(locale==defaultLocale)
                    name = language.nameDefault;
                else if(locale==Locale.US)
                    name = language.nameEnglish;
                else if(language.iso2.equals(locale.getLanguage()))
                    name = language.nameNative;
                else
                    name = new Locale(language.iso2,"","").getDisplayLanguage(locale);
            }
            return name;
        }
        public String name(String val)
        {
            return name(val,Locale.getDefault());
        }
        public String getMacro(String value)
        {
            String ret = macro.get(value);
            return ret!=null?ret:value;
        }
        public boolean isMacro(String value)
        {
            return macroComponents.containsKey(value);
        }
        public String[] getMacroComponents(String value)
        {
            return macroComponents.get(value);
        }
        public Map<String,String> getMacroMap()
        {
            return (Map<String, String>) macro.clone();
        }
    }

/**
 *
 * @author franci
 */
public class Locales
{
    public static final String zz = "zz";
    public static final String ZZ = "ZZ";
    public static final Locale zz_ZZ = new Locale(zz,ZZ);

    public static String getISO2Country(String val)
    {
        return CountryHolder.INSTANCE.iso2(val);
    }
    public static String getISO2Language(String val)
    {
        return LanguageHolder.INSTANCE.iso2(val);
    }
    public static String getISO3Country(String val)
    {
        return CountryHolder.INSTANCE.iso3(val);
    }
    public static String getISO3Language(String val)
    {
        return LanguageHolder.INSTANCE.iso3(val);
    }
    public static String getDisplayCountry(String val)
    {
        return CountryHolder.INSTANCE.name(val);
    }
    public static String getDisplayCountry(String val, Locale locale)
    {
        return CountryHolder.INSTANCE.name(val, locale);
    }
    public static String getDisplayCountry(String val, String language)
    {
        String iso2 = getISO2Language(language);
        return iso2!=null?getDisplayCountry(val,new Locale(iso2,"","")):null;
    }
    public static String getDisplayLanguage(String val)
    {
        return LanguageHolder.INSTANCE.name(val);
    }
    public static String[] getDisplayLanguage(String[] val)
    {
        String[] langs = new String[val.length];
        for(int i=0;i<langs.length;i++)
        {
            langs[i] = getDisplayLanguage(val[i]);
        }
        return langs;
    }
    public static String getDisplayLanguage(String val, Locale locale)
    {
        return LanguageHolder.INSTANCE.name(val, locale);
    }
    public static String[] getDisplayLanguage(String[] val, Locale locale)
    {
        String[] langs = new String[val.length];
        for(int i=0;i<langs.length;i++)
        {
            langs[i] = getDisplayLanguage(val[i], locale);
        }
        return langs;
    }
    public static String getDisplayLanguage(String val, String language)
    {
        String iso2 = getISO2Language(language);
        return iso2!=null?getDisplayLanguage(val,new Locale(iso2,"","")):null;
    }
    public static String[] getDisplayLanguage(String[] val, String language)
    {
        String[] langs = new String[val.length];
        for(int i=0;i<langs.length;i++)
        {
            langs[i] = getDisplayLanguage(val[i], language);
        }
        return langs;
    }
    public static String getDisplayLanguageNative(String val)
    {
        return getDisplayLanguage(val,val);
    }
    public static String[] getDisplayLanguageNative(String[] val)
    {
        String[] langs = new String[val.length];
        for(int i=0;i<langs.length;i++)
        {
            langs[i] = getDisplayLanguageNative(val[i]);
        }
        return langs;
    }

    public static String[] getDisplayLanguageCountry(Locale[] val)
    {
        return getDisplayLanguageCountry(val, null);
    }
    public static String[] getDisplayLanguageCountry(Locale[] val, Locale locale)
    {
        String[] display = new String[val.length];
        for(int i=0;i<display.length;i++)
        {
            display[i] = getDisplayLanguageCountry(val[i], locale);
        }
        return display;
    }
    public static String getDisplayLanguageCountry(Locale val)
    {
        return getDisplayLanguageCountry(val, null);
    }
    public static String getDisplayLanguageCountry(Locale val, Locale locale)
    {
        if(val==null)
        {
            return "";
        }
        if(locale==null)
        {
            locale = val;
        }
        String lang = val.getDisplayLanguage(locale);
        String country = val.getDisplayCountry(locale);
        
        if(lang.length()>0 && country.length()>0)
        {
            return lang + " (" + country +")";
        }
        if(lang.length()>0)
        {
            return lang;
        }
        return "("+country+")";
    }
    public static String getMacroLanguage(String value)
    {
        return LanguageHolder.INSTANCE.getMacro(value);
    }
    public static boolean isMacroLanguage(String value)
    {
        return LanguageHolder.INSTANCE.isMacro(value);
    }
    public static String[] getMacroLanguageComponents(String value)
    {
        return LanguageHolder.INSTANCE.getMacroComponents(value);
    }

    public static Map<String, String> getMacroMap()
    {
        return LanguageHolder.INSTANCE.getMacroMap();
    }
    
    private enum DefaultHolder
    {
        INSTANCE;
        private final Locale initial = Locale.getDefault();
        Locale setDefault(Locale value)
        {
            value = value==null?initial:value;
            Locale.setDefault(value);
            return value;
        }
    }
    // sets a locale or the original one if null is passed as parameter
    public static Locale setDefault(Locale value)
    {
        return DefaultHolder.INSTANCE.setDefault(value);
    }

    private static Locale build(String... tokens)
    {
        if(tokens!=null)
        {
            String lan = (tokens.length>0) ? tokens[0] : "";
            String cou = (tokens.length>1) ? tokens[1] : "";
            String var = (tokens.length>2) ? tokens[2] : "";

            lan = lan!=null? lan : "";
            cou = cou!=null? cou : "";
            var = var!=null? var : "";
            return new Locale(lan, cou, var);
        }   
        return new Locale("", "", "");
        
    }
    
    private static Locale buildISO2(String... tokens)
    {
        if(tokens!=null)
        {
            if(tokens.length>0)
            {
                tokens[0] = (tokens[0]!=null) ? Strings.firstNonEmpty(getISO2Language(tokens[0]), tokens[0]) : null ;
            }
            if(tokens.length>1)
            {
                tokens[1] = (tokens[1]!=null) ? Strings.firstNonEmpty(getISO2Country(tokens[1]), tokens[1]) : null ;
            }
        }
        return build(tokens);
    }
    private static Locale buildISO3(String... tokens)
    {
        if(tokens!=null)
        {
            if(tokens.length>0)
            {
                tokens[0] = (tokens[0]!=null) ? Strings.firstNonEmpty(getISO3Language(tokens[0]), tokens[0]) : null ;
            }
            if(tokens.length>1)
            {
                tokens[1] = (tokens[1]!=null) ? Strings.firstNonEmpty(getISO3Country(tokens[1]), tokens[1]) : null ;
            }
        }
        return build(tokens);
    }
    private static final String LOCALE_SEPARATOR = "_";
    public static Locale parse(String s)
    {
        return (s!=null) ? build(s.split(LOCALE_SEPARATOR)) : null;
    }
    public static Locale parseISO2(String s)
    {
        return (s!=null) ? buildISO2(s.split(LOCALE_SEPARATOR)) : null;
    }
    public static Locale parseISO3(String s)
    {
        return (s!=null) ? buildISO3(s.split(LOCALE_SEPARATOR)) : null;
    }
    public static Locale getISO2(Locale locale)
    {
        return parseISO2(locale.toString());
    }
    public static Locale getISO3(Locale locale)
    {
        return parseISO3(locale.toString());
    }
    
    public static boolean contains(Locale container, Locale contained)
    {
        try
        {
            String containerLang3;
            if(container==null || container.equals(contained) || (containerLang3=container.getISO3Language()).length()==0)
            {
                return true;
            }
            String containedLang3;
            if(contained==null || (containedLang3=contained.getISO3Language()).length()==0)
            {
                return false;
            }
            if(containerLang3.equals(containedLang3)==false)
            {
                return false;
            }

            String containerCountry3;
            if((containerCountry3=container.getISO3Country()).length()==0)
            {
                return true;
            }
            String containedCountry3;
            if((containedCountry3=contained.getISO3Country()).length()==0)
            {
                return false;
            }
            if(containerCountry3.equals(containedCountry3)==false)
            {
                return false;
            }

            String containerVariant;
            if((containerVariant=container.getVariant()).length()==0)
            {
                return true;
            }
            String containedVariant;
            if((containedVariant=contained.getVariant()).length()==0)
            {
                return false;
            }
            return containerVariant.equals(containedVariant);
        }
        catch(MissingResourceException ex)
        {
            return containsIso2(container, contained);
        }
    }
    private static boolean containsIso2(Locale container, Locale contained)
    {
        String containerLang2;
        if(container==null || container.equals(contained) || (containerLang2=container.getLanguage()).length()==0)
        {
            return true;
        }
        String containedLang2;
        if(contained==null || (containedLang2=contained.getLanguage()).length()==0)
        {
            return false;
        }
        if(containerLang2.equals(containedLang2)==false)
        {
            return false;
        }

        String containerCountry2;
        if((containerCountry2=container.getCountry()).length()==0)
        {
            return true;
        }
        String containedCountry2;
        if((containedCountry2=contained.getCountry()).length()==0)
        {
            return false;
        }
        if(containerCountry2.equals(containedCountry2)==false)
        {
            return false;
        }

        String containerVariant;
        if((containerVariant=container.getVariant()).length()==0)
        {
            return true;
        }
        String containedVariant;
        if((containedVariant=contained.getVariant()).length()==0)
        {
            return false;
        }
        return containerVariant.equals(containedVariant);
    }
    
    private static final WeakHashMap<Locale, Locale[]> hierarchyMap = new WeakHashMap<>();
    public static Locale[] getHierarchy(Locale locale)
    {
        if(locale==null)
        {
            return null;
        }
        
        Locale[] hierarchy = hierarchyMap.get(locale);
        if(hierarchy==null)
        {
            ArrayList<Locale> list = new ArrayList<>();
            String lang    = locale.getLanguage();
            String country = locale.getCountry();
            String variant = locale.getVariant();
            list.add(locale);
            if(variant.length()>0)
            {
                list.add(new Locale(lang,country,""));
            }
            if(country.length()>0)
            {
                list.add(new Locale(lang,"",""));
            }
            if(lang.length()>0)
            {
                list.add(new Locale("","",""));
            }
            hierarchy = list.toArray(new Locale[0]);
            hierarchyMap.put(locale, hierarchy);
        }
        return hierarchy;
    }
    private static final WeakHashMap<Locale, Locale[]> parentsMap = new WeakHashMap<>();
    public static Locale[] getParents(Locale locale)
    {
        if(locale==null)
        {
            return null;
        }
        
        Locale[] hierarchy = parentsMap.get(locale);
        if(hierarchy==null)
        {
            ArrayList<Locale> list = new ArrayList<>();
            String lang    = locale.getLanguage();
            String country = locale.getCountry();
            String variant = locale.getVariant();
            if(variant.length()>0)
            {
                list.add(new Locale(lang,country,""));
            }
            if(country.length()>0)
            {
                list.add(new Locale(lang,"",""));
            }
            if(lang.length()>0)
            {
                list.add(new Locale("","",""));
            }
            hierarchy = list.toArray(new Locale[0]);
            parentsMap.put(locale, hierarchy);
        }
        return hierarchy;
    }
    public static String getISO3Language(Locale locale)
    {
        try
        {
            return locale!=null ? locale.getISO3Language() : null;
        }
        catch(Exception ex)
        {
            Logger.getLogger(Locales.class.getName()).log(Level.WARNING, null,ex);
            return  locale!=null ? (zz.equals(locale.getLanguage()) ? "zzz" : "") : "";
        }
    }

    //languages
    public static String SPANISH_ISO2  = "es";
    public static String CATALAN_ISO2  = "ca";
    public static String BASQUE_ISO2   = "eu";
    public static String GALICIAN_ISO2 = "gl";


    public static String SPANISH_ISO3  = "spa";
    public static String CATALAN_ISO3  = "cat";
    public static String BASQUE_ISO3   = "eus";
    public static String GALICIAN_ISO3 = "glg";

    //countries
    public static String SPAIN_ISO2 = "ES";
    public static String SPAIN_ISO3 = "ESP";


}
