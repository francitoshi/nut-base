/*
 * LocalesTest.java
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

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class LocalesTest
{
    
    public LocalesTest()
    {
    }
    
    @BeforeAll
    public static void setUpClass()
    {
    }
    
    @AfterAll
    public static void tearDownClass()
    {
    }
    
    @BeforeEach
    public void setUp()
    {
    }
    
    @AfterEach
    public void tearDown()
    {
    }

    /**
     * Test of getISO2Country method, of class Locales.
     */
    @Test
    public void testGetISO2Country()
    {
        assertNull(Locales.getISO2Country(null));
        assertNull(Locales.getISO2Country("garbaje not a country"));
        
        assertEquals("ES", Locales.getISO2Country("es"));
        assertEquals("ES", Locales.getISO2Country("ES"));
        assertEquals("ES", Locales.getISO2Country("esp"));
        assertEquals("ES", Locales.getISO2Country("ESP"));
        assertEquals("ES", Locales.getISO2Country("spain"));
        assertEquals("ES", Locales.getISO2Country("Spain"));
        assertEquals("ES", Locales.getISO2Country("SPAIN"));
        
        //Esto sólo funciona si el locale es español
        if(Locale.getDefault().getLanguage().equalsIgnoreCase("es"))
        {
            assertEquals("ES", Locales.getISO2Country("españa"));
            assertEquals("ES", Locales.getISO2Country("España"));
            assertEquals("ES", Locales.getISO2Country("ESPAÑA"));
        }

        Locale es = new Locale("es");
        
        for(int i=0;i<COUNTRIES.length;i++)
        {
            String name = COUNTRIES[i][0];
            String iso2 = COUNTRIES[i][1];
            String iso3 = COUNTRIES[i][2];
            String num3 = COUNTRIES[i][3];
            
            assertEquals(iso2, Locales.getISO2Country(iso2));
            assertEquals(iso2, Locales.getISO2Country(iso3));
            assertEquals(iso2, Locales.getISO2Country(name));
            assertEquals(iso2, Locales.getISO2Country(name.toLowerCase()));
            assertEquals(iso2, Locales.getISO2Country(name.toUpperCase()));
        }
    }

    /**
     * Test of getISO2Language method, of class Locales.
     */
    @Test
    public void testGetISO2Language()
    {
        assertNull(Locales.getISO2Language(null), "@1");
        assertNull(Locales.getISO2Language("garbaje not a language"),"@2");
       
        assertEquals("es", Locales.getISO2Language("es"),"@3");
        assertEquals("es", Locales.getISO2Language("spa"),"@4");
        assertEquals("es", Locales.getISO2Language("spanish"),"@5");
        assertEquals("es", Locales.getISO2Language("Spanish"),"@6");
        assertEquals("es", Locales.getISO2Language("SPANISH"),"@7");

        assertEquals("bo", Locales.getISO2Language("bo"),"@8");
        assertEquals("bo", Locales.getISO2Language("tib"),"@9");
        assertEquals("bo", Locales.getISO2Language("bod"),"@10");
        assertEquals("bo", Locales.getISO2Language("tibetan"),"v11");
        assertEquals("bo", Locales.getISO2Language("Tibetan"),"@12");
        assertEquals("bo", Locales.getISO2Language("TIBETAN"),"@13");
        
        //Esto sólo funciona si el locale es español
        if(Locale.getDefault().getLanguage().equalsIgnoreCase("es"))
        {
            assertEquals("es", Locales.getISO2Language("Español"),"@14");
            assertEquals("bo", Locales.getISO2Language("Tibetano"),"@15");
        }        
        
        for(int i=0;i<ISO_369.length;i++)
        {
            String iso2 = ISO_369[i][0];
            String old2 = ISO_369[i][1];
            String old3 = ISO_369[i][2];
            String iso3 = ISO_369[i][3];
            String name = ISO_369[i][4];
            
            assertEquals(iso2, Locales.getISO2Language(iso2), name+"@16");
            assertEquals(iso2, Locales.getISO2Language(iso3), name+"@17");
            if(!old2.equals(iso2))
            {
                assertEquals(iso2, Locales.getISO2Language(old2), name+"@18");
            }
            if(!old3.equals(iso3))
            {
                assertEquals(iso2, Locales.getISO2Language(old3), name+"@19");
            }
            
            assertEquals(iso2, Locales.getISO2Language(name), name+"@20");
            assertEquals(iso2, Locales.getISO2Language(name.toLowerCase()), name+"@21");
            assertEquals(iso2, Locales.getISO2Language(name.toUpperCase()), name+"@22");
        }
    }

    /**
     * Test of getISO3Country method, of class Locales.
     */
    @Test
    public void testGetISO3Country()
    {
        assertNull(Locales.getISO3Country(null));
        assertNull(Locales.getISO3Country("garbaje not a country"));
        
        assertEquals("ESP", Locales.getISO3Country("ES"));
        assertEquals("ESP", Locales.getISO3Country("es"));
        assertEquals("ESP", Locales.getISO3Country("spain"));
        assertEquals("ESP", Locales.getISO3Country("Spain"));
        assertEquals("ESP", Locales.getISO3Country("SPAIN"));
        
        for(int i=0;i<COUNTRIES.length;i++)
        {
            String name = COUNTRIES[i][0].toUpperCase();
            String iso2 = COUNTRIES[i][1].toUpperCase();
            String iso3 = COUNTRIES[i][2].toUpperCase();
            String num3 = COUNTRIES[i][3];
            
            assertEquals(iso3, Locales.getISO3Country(iso2),i+"@1");
            assertEquals(iso3, Locales.getISO3Country(iso3),i+"@2");
            assertEquals(iso3, Locales.getISO3Country(name),i+"@3");
            assertEquals(iso3, Locales.getISO3Country(name.toLowerCase()),i+"@4");
            assertEquals(iso3, Locales.getISO3Country(name.toUpperCase()),i+"@5");
        }
        
    }

    /**
     * Test of getISO3Language method, of class Locales.
     */
    @Test
    public void testGetISO3Language()
    {
        assertNull(Locales.getISO3Language((String)null));
        assertNull(Locales.getISO3Language("garbaje not a language"));
        
        assertEquals("spa", Locales.getISO3Language("es"));
        assertEquals("spa", Locales.getISO3Language("spa"));
        assertEquals("spa", Locales.getISO3Language("spanish"));
        assertEquals("spa", Locales.getISO3Language("Spanish"));

        assertEquals("bod", Locales.getISO3Language("bo"));
        assertEquals("bod", Locales.getISO3Language("tib"));
        assertEquals("bod", Locales.getISO3Language("bod"));
        assertEquals("bod", Locales.getISO3Language("Tibetan"));
        assertEquals("bod", Locales.getISO3Language("tibetan"));

        for(int i=0;i<ISO_369.length;i++)
        {
            String old2 = ISO_369[i][0];
            String iso2 = ISO_369[i][1];
            String old3 = ISO_369[i][3];
            String iso3 = ISO_369[i][3];
            String name = ISO_369[i][0];
            
            assertEquals(iso3, Locales.getISO3Language(old2),name+"@1");
            assertEquals(iso3, Locales.getISO3Language(iso2),name+"@2");
            assertEquals(iso3, Locales.getISO3Language(iso3),name+"@3");
            assertEquals(iso3, Locales.getISO3Language(old3),name+"@4");
            assertEquals(iso3, Locales.getISO3Language(name),name+"@5");
            assertEquals(iso3, Locales.getISO3Language(name.toLowerCase()),name+"@6");
            assertEquals(iso3, Locales.getISO3Language(name.toUpperCase()),name+"@7");
        }
    }

    /**
     * Test of getDisplayCountry method, of class Locales.
     */
    @Test
    public void testGetDisplayCountry_String_Locale()
    {
        assertNull(Locales.getDisplayCountry(null,Locale.US));
        assertNull(Locales.getDisplayCountry("garbaje not a country",Locale.US));
        
        Locale loc = new Locale("es","ES","");
        assertEquals("España", Locales.getDisplayCountry("es",loc),"@1");
        assertEquals("España", Locales.getDisplayCountry("ES",loc),"@2");
        assertEquals("España", Locales.getDisplayCountry("spain",loc),"@3");
        assertEquals("España", Locales.getDisplayCountry("Spain",loc),"@4");
        assertEquals("España", Locales.getDisplayCountry("SPAIN",loc),"@5");
        assertEquals("España", Locales.getDisplayCountry("españa",loc),"@6");
        assertEquals("España", Locales.getDisplayCountry("España",loc),"@7");
        assertEquals("España", Locales.getDisplayCountry("ESPAÑA",loc),"@8");
        loc = Locale.US;
        assertEquals("Spain", Locales.getDisplayCountry("es",loc),"@9");
        assertEquals("Spain", Locales.getDisplayCountry("ES",loc),"@10");
        assertEquals("Spain", Locales.getDisplayCountry("spain",loc),"@11");
        assertEquals("Spain", Locales.getDisplayCountry("Spain",loc),"@12");
        assertEquals("Spain", Locales.getDisplayCountry("SPAIN",loc),"@13");
        assertEquals("Spain", Locales.getDisplayCountry("españa",loc),"@14");
        assertEquals("Spain", Locales.getDisplayCountry("España",loc),"@15");
        assertEquals("Spain", Locales.getDisplayCountry("ESPAÑA",loc),"@16");
        loc = Locale.ITALY;
        assertEquals("Spagna", Locales.getDisplayCountry("es",loc),"@17");
        assertEquals("Spagna", Locales.getDisplayCountry("ES",loc),"@18");
        assertEquals("Spagna", Locales.getDisplayCountry("spain",loc),"@19");
        assertEquals("Spagna", Locales.getDisplayCountry("Spain",loc),"@20");
        assertEquals("Spagna", Locales.getDisplayCountry("SPAIN",loc),"@21");
        assertEquals("Spagna", Locales.getDisplayCountry("españa",loc),"@22");
        assertEquals("Spagna", Locales.getDisplayCountry("España",loc),"@23");
        assertEquals("Spagna", Locales.getDisplayCountry("ESPAÑA",loc),"@24");
        loc = Locale.SIMPLIFIED_CHINESE;
        assertEquals("西班牙", Locales.getDisplayCountry("es",loc),"@25");
        assertEquals("西班牙", Locales.getDisplayCountry("ES",loc),"@26");
        assertEquals("西班牙", Locales.getDisplayCountry("spain",loc),"@27");
        assertEquals("西班牙", Locales.getDisplayCountry("Spain",loc),"@28");
        assertEquals("西班牙", Locales.getDisplayCountry("SPAIN",loc),"@29");
        assertEquals("西班牙", Locales.getDisplayCountry("españa",loc),"@30");
        assertEquals("西班牙", Locales.getDisplayCountry("España",loc),"@31");
        assertEquals("西班牙", Locales.getDisplayCountry("ESPAÑA",loc),"@32");
    }

    /**
     * Test of getDisplayCountry method, of class Locales.
     */
    @Test
    public void testGetDisplayCountry_String_String()
    {
        String loc = "es";
        assertEquals("España", Locales.getDisplayCountry("es",loc));
        assertEquals("España", Locales.getDisplayCountry("ES",loc));
        assertEquals("España", Locales.getDisplayCountry("spain",loc));
        assertEquals("España", Locales.getDisplayCountry("Spain",loc));
        assertEquals("España", Locales.getDisplayCountry("SPAIN",loc));
        assertEquals("España", Locales.getDisplayCountry("españa",loc));
        assertEquals("España", Locales.getDisplayCountry("España",loc));
        assertEquals("España", Locales.getDisplayCountry("ESPAÑA",loc));
        loc = Locale.US.getLanguage();
        assertEquals("Spain", Locales.getDisplayCountry("es",loc));
        assertEquals("Spain", Locales.getDisplayCountry("ES",loc));
        assertEquals("Spain", Locales.getDisplayCountry("spain",loc));
        assertEquals("Spain", Locales.getDisplayCountry("Spain",loc));
        assertEquals("Spain", Locales.getDisplayCountry("SPAIN",loc));
        assertEquals("Spain", Locales.getDisplayCountry("españa",loc));
        assertEquals("Spain", Locales.getDisplayCountry("España",loc));
        assertEquals("Spain", Locales.getDisplayCountry("ESPAÑA",loc));
        loc = Locale.ITALY.getLanguage();
        assertEquals("Spagna", Locales.getDisplayCountry("es",loc));
        assertEquals("Spagna", Locales.getDisplayCountry("ES",loc));
        assertEquals("Spagna", Locales.getDisplayCountry("spain",loc));
        assertEquals("Spagna", Locales.getDisplayCountry("Spain",loc));
        assertEquals("Spagna", Locales.getDisplayCountry("SPAIN",loc));
        assertEquals("Spagna", Locales.getDisplayCountry("españa",loc));
        assertEquals("Spagna", Locales.getDisplayCountry("España",loc));
        assertEquals("Spagna", Locales.getDisplayCountry("ESPAÑA",loc));
        loc = Locale.SIMPLIFIED_CHINESE.getLanguage();
        assertEquals("西班牙", Locales.getDisplayCountry("es",loc));
        assertEquals("西班牙", Locales.getDisplayCountry("ES",loc));
        assertEquals("西班牙", Locales.getDisplayCountry("spain",loc));
        assertEquals("西班牙", Locales.getDisplayCountry("Spain",loc));
        assertEquals("西班牙", Locales.getDisplayCountry("SPAIN",loc));
        assertEquals("西班牙", Locales.getDisplayCountry("españa",loc));
        assertEquals("西班牙", Locales.getDisplayCountry("España",loc));
        assertEquals("西班牙", Locales.getDisplayCountry("ESPAÑA",loc));
    }

    /**
     * Test of getDisplayLanguage method, of class Locales.
     */
    @Test
    public void testGetDisplayLanguage_String_Locale()
    {
        assertNull(Locales.getDisplayLanguage((String)null,Locale.US));
        assertNull(Locales.getDisplayLanguage("garbaje not a country",Locale.US));
        
        Locale loc = new Locale("ES","","");
        assertEquals("español", Locales.getDisplayLanguage("es",loc).toLowerCase(),"1");
        assertEquals("español", Locales.getDisplayLanguage("ES",loc).toLowerCase(),"2");
        assertEquals("español", Locales.getDisplayLanguage("spanish",loc).toLowerCase(),"3");
        assertEquals("español", Locales.getDisplayLanguage("Spanish",loc).toLowerCase(),"4");
        assertEquals("español", Locales.getDisplayLanguage("SPANISH",loc).toLowerCase(),"5");
        assertEquals("español", Locales.getDisplayLanguage("español",loc).toLowerCase(),"6");
        assertEquals("español", Locales.getDisplayLanguage("Español",loc).toLowerCase(),"7");
        assertEquals("español", Locales.getDisplayLanguage("ESPAÑOL",loc).toLowerCase(),"8");
        loc = Locale.US;
        assertEquals("spanish", Locales.getDisplayLanguage("es",loc).toLowerCase(),"9");
        assertEquals("spanish", Locales.getDisplayLanguage("ES",loc).toLowerCase(),"10");
        assertEquals("spanish", Locales.getDisplayLanguage("spanish",loc).toLowerCase(),"11");
        assertEquals("spanish", Locales.getDisplayLanguage("Spanish",loc).toLowerCase(),"12");
        assertEquals("spanish", Locales.getDisplayLanguage("SPANISH",loc).toLowerCase(),"13");
        assertEquals("spanish", Locales.getDisplayLanguage("español",loc).toLowerCase(),"14");
        assertEquals("spanish", Locales.getDisplayLanguage("Español",loc).toLowerCase(),"15");
        assertEquals("spanish", Locales.getDisplayLanguage("ESPAÑOL",loc).toLowerCase(),"16");
        loc = Locale.ITALY;
        assertEquals("spagnolo", Locales.getDisplayLanguage("es",loc).toLowerCase(),"17");
        assertEquals("spagnolo", Locales.getDisplayLanguage("ES",loc).toLowerCase(),"18");
        assertEquals("spagnolo", Locales.getDisplayLanguage("spanish",loc).toLowerCase(),"19");
        assertEquals("spagnolo", Locales.getDisplayLanguage("Spanish",loc).toLowerCase(),"20");
        assertEquals("spagnolo", Locales.getDisplayLanguage("SPANISH",loc).toLowerCase(),"21");
        assertEquals("spagnolo", Locales.getDisplayLanguage("español",loc).toLowerCase(),"22");
        assertEquals("spagnolo", Locales.getDisplayLanguage("Español",loc).toLowerCase(),"23");
        assertEquals("spagnolo", Locales.getDisplayLanguage("ESPAÑOL",loc).toLowerCase(),"24");
    }

    /**
     * Test of getDisplayLanguage method, of class Locales.
     */
    @Test
    public void testGetDisplayLanguage_String_String()
    {
        Locale loc = new Locale("es","ES","");
        assertEquals("español", Locales.getDisplayLanguage("es",loc).toLowerCase());
        assertEquals("español", Locales.getDisplayLanguage("ES",loc).toLowerCase());
        assertEquals("español", Locales.getDisplayLanguage("spanish",loc).toLowerCase());
        assertEquals("español", Locales.getDisplayLanguage("Spanish",loc).toLowerCase());
        assertEquals("español", Locales.getDisplayLanguage("SPANISH",loc).toLowerCase());
        assertEquals("español", Locales.getDisplayLanguage("español",loc).toLowerCase());
        assertEquals("español", Locales.getDisplayLanguage("Español",loc).toLowerCase());
        assertEquals("español", Locales.getDisplayLanguage("ESPAÑOL",loc).toLowerCase());
        loc = Locale.US;
        assertEquals("spanish", Locales.getDisplayLanguage("es",loc).toLowerCase());
        assertEquals("spanish", Locales.getDisplayLanguage("ES",loc).toLowerCase());
        assertEquals("spanish", Locales.getDisplayLanguage("spanish",loc).toLowerCase());
        assertEquals("spanish", Locales.getDisplayLanguage("Spanish",loc).toLowerCase());
        assertEquals("spanish", Locales.getDisplayLanguage("SPANISH",loc).toLowerCase());
        assertEquals("spanish", Locales.getDisplayLanguage("español",loc).toLowerCase());
        assertEquals("spanish", Locales.getDisplayLanguage("Español",loc).toLowerCase());
        assertEquals("spanish", Locales.getDisplayLanguage("ESPAÑOL",loc).toLowerCase());
        loc = Locale.ITALY;
        assertEquals("spagnolo", Locales.getDisplayLanguage("es",loc).toLowerCase());
        assertEquals("spagnolo", Locales.getDisplayLanguage("ES",loc).toLowerCase());
        assertEquals("spagnolo", Locales.getDisplayLanguage("spanish",loc).toLowerCase());
        assertEquals("spagnolo", Locales.getDisplayLanguage("Spanish",loc).toLowerCase());
        assertEquals("spagnolo", Locales.getDisplayLanguage("SPANISH",loc).toLowerCase());
        assertEquals("spagnolo", Locales.getDisplayLanguage("español",loc).toLowerCase());
        assertEquals("spagnolo", Locales.getDisplayLanguage("Español",loc).toLowerCase());
        assertEquals("spagnolo", Locales.getDisplayLanguage("ESPAÑOL",loc).toLowerCase());
    }

    static final String[][] COUNTRIES=
    {
        //only non problematic countries
        //Country, A2, A3, Number
        {"AFGHANISTAN","AF","AFG","004"},
        {"ALBANIA","AL", "ALB", "008"},
        {"ALGERIA","DZ", "DZA", "012"},
        {"AMERICAN SAMOA", "AS", "ASM", "016"},
        {"ANDORRA","AD", "AND", "020"},
        {"ANGOLA","AO", "AGO", "024"},
        {"ANGUILLA","AI", "AIA", "660"},
        {"ANTARCTICA","AQ", "ATA", "010"},

        {"ARGENTINA","AR", "ARG", "032"},
        {"ARMENIA","AM", "ARM", "051 "},
        {"ARUBA","AW", "ABW", "533"},
        {"AUSTRALIA","AU", "AUS", "036"},
        {"AUSTRIA","AT", "AUT", "040"},
        {"AZERBAIJAN","AZ", "AZE", "031 "},
        {"BAHAMAS","BS", "BHS", "044"},
        {"BAHRAIN","BH", "BHR", "048"},
        {"BANGLADESH","BD", "BGD", "050"},
        {"BARBADOS","BB", "BRB", "052"},
        {"BELARUS","BY", "BLR", "112 "},
        {"BELGIUM","BE", "BEL", "056"},
        {"BELIZE","BZ", "BLZ", "084"},
        {"BENIN","BJ", "BEN", "204"},
        {"BERMUDA","BM", "BMU", "060"},
        {"BHUTAN","BT", "BTN", "064"},
        {"BOLIVIA","BO", "BOL", "068"},
        {"BOTSWANA","BW", "BWA", "072"},
        {"BOUVET ISLAND","BV", "BVT", "074"},
        {"BRAZIL","BR", "BRA", "076"},
        {"BRITISH INDIAN OCEAN TERRITORY","IO", "IOT", "086"},
        {"BULGARIA","BG", "BGR", "100"},
        {"BURKINA FASO","BF", "BFA", "854"},
        {"BURUNDI","BI", "BDI", "108"},
        {"CAMBODIA","KH", "KHM", "116"},
        {"CAMEROON","CM", "CMR", "120"},
        {"CANADA","CA", "CAN", "124"},
        {"CAPE VERDE","CV", "CPV", "132"},
        {"CAYMAN ISLANDS","KY", "CYM", "136"},
        {"CENTRAL AFRICAN REPUBLIC","CF", "CAF", "140"},
        {"CHAD","TD", "TCD", "148"},
        {"CHILE","CL", "CHL", "152"},
        {"CHINA","CN", "CHN", "156"},
        {"CHRISTMAS ISLAND","CX", "CXR", "162"},
        {"COLOMBIA","CO", "COL", "170"},
        {"COMOROS","KM", "COM", "174"},

        {"COOK ISLANDS","CK", "COK", "184"},
        {"COSTA RICA","CR", "CRI", "188"},
        {"CUBA","CU", "CUB", "192"},
        {"CYPRUS","CY", "CYP", "196"},

        {"DENMARK","DK", "DNK", "208"},
        {"DJIBOUTI","DJ", "DJI", "262"},
        {"DOMINICA","DM", "DMA", "212"},
        {"DOMINICAN REPUBLIC","DO", "DOM", "214"},
        {"ECUADOR","EC", "ECU", "218"},
        {"EGYPT","EG", "EGY", "818"},
        {"EL SALVADOR","SV", "SLV", "222"},
        {"EQUATORIAL GUINEA","GQ", "GNQ", "226"},
        {"ERITREA","ER", "ERI", "232"},
        {"ESTONIA","EE", "EST", "233 "},
        {"ETHIOPIA","ET", "ETH", "231"},
        {"FAROE ISLANDS","FO", "FRO", "234"},
        {"FIJI","FJ", "FJI", "242"},
        {"FINLAND","FI", "FIN", "246"},
        {"FRANCE","FR", "FRA", "250"},
        {"FRENCH GUIANA","GF", "GUF", "254"},
        {"FRENCH POLYNESIA","PF", "PYF", "258"},
        {"FRENCH SOUTHERN TERRITORIES","TF", "ATF", "260"},
        {"GABON","GA", "GAB", "266"},
        {"GAMBIA","GM", "GMB", "270"},
        {"GEORGIA","GE", "GEO", "268 "},
        {"GERMANY","DE", "DEU", "276"},
        {"GHANA","GH", "GHA", "288"},
        {"GIBRALTAR","GI", "GIB", "292"},
        {"GREECE","GR", "GRC", "300"},
        {"GREENLAND","GL", "GRL", "304"},
        {"GRENADA","GD", "GRD", "308"},
        {"GUADELOUPE","GP", "GLP", "312"},
        {"GUAM","GU", "GUM", "316"},
        {"GUATEMALA","GT", "GTM", "320"},
        {"GUINEA","GN", "GIN", "324"},
        {"GUINEA-BISSAU","GW", "GNB", "624"},
        {"GUYANA","GY", "GUY", "328"},
        {"HAITI","HT", "HTI", "332"},
        {"HONDURAS","HN", "HND", "340"},

        {"HUNGARY","HU", "HUN", "348"},
        {"ICELAND","IS", "ISL", "352"},
        {"INDIA","IN", "IND", "356"},
        {"INDONESIA","ID", "IDN", "360"},
        {"IRAQ","IQ", "IRQ", "368"},
        {"IRELAND","IE", "IRL", "372"},
        {"ISRAEL","IL", "ISR", "376"},
        {"ITALY","IT", "ITA", "380"},
        {"JAMAICA","JM", "JAM", "388"},
        {"JAPAN","JP", "JPN", "392"},
        {"JORDAN","JO", "JOR", "400"},
        {"KAZAKHSTAN","KZ", "KAZ", "398  "},
        {"KENYA","KE", "KEN", "404"},
        {"KIRIBATI","KI", "KIR", "296"},
        {"KUWAIT","KW", "KWT", "414"},
        {"KYRGYZSTAN","KG", "KGZ", "417 "},
        {"LATVIA","LV", "LVA", "428 "},
        {"LEBANON","LB", "LBN", "422"},
        {"LESOTHO","LS", "LSO", "426"},
        {"LIBERIA","LR", "LBR", "430"},
        {"LIECHTENSTEIN","LI", "LIE", "438"},
        {"LITHUANIA","LT", "LTU", "440 "},
        {"MADAGASCAR","MG", "MDG", "450"},
        {"MALAWI","MW", "MWI", "454"},
        {"MALAYSIA","MY", "MYS", "458"},
        {"MALDIVES","MV", "MDV", "462"},
        {"MALI","ML", "MLI", "466"},
        {"MALTA","MT", "MLT", "470"},
        {"MARSHALL ISLANDS","MH", "MHL", "584"},
        {"MARTINIQUE","MQ", "MTQ", "474"},
        {"MAURITANIA","MR", "MRT", "478"},
        {"MAURITIUS","MU", "MUS", "480"},
        {"MAYOTTE","YT", "MYT", "175 "},
        {"MEXICO","MX", "MEX", "484"},
        {"MONACO","MC", "MCO", "492"},
        {"MONGOLIA","MN", "MNG", "496"},
        {"MONTSERRAT","MS", "MSR", "500"},
        {"MOROCCO","MA", "MAR", "504"},
        {"MOZAMBIQUE","MZ", "MOZ", "508"},

        {"NAMIBIA","NA", "NAM", "516"},
        {"NAURU","NR", "NRU", "520"},
        {"NEPAL","NP", "NPL", "524"},
        {"NETHERLANDS","NL", "NLD", "528"},

        {"NEW CALEDONIA","NC", "NCL", "540"},
        {"NEW ZEALAND","NZ", "NZL", "554"},
        {"NICARAGUA","NI", "NIC", "558"},
        {"NIGER","NE", "NER", "562"},
        {"NIGERIA","NG", "NGA", "566"},
        {"NIUE","NU", "NIU", "570"},
        {"NORFOLK ISLAND","NF", "NFK", "574"},
        {"NORTHERN MARIANA ISLANDS","MP", "MNP", "580"},
        {"NORWAY","NO", "NOR", "578"},
        {"OMAN","OM", "OMN", "512"},
        {"PAKISTAN","PK", "PAK", "586"},
        {"PALAU","PW", "PLW", "585"},
        {"PANAMA","PA", "PAN", "591"},
        {"PAPUA NEW GUINEA","PG", "PNG", "598"},
        {"PARAGUAY","PY", "PRY", "600"},
        {"PERU","PE", "PER", "604"},
        {"PHILIPPINES","PH", "PHL", "608"},

        {"POLAND","PL", "POL", "616"},
        {"PORTUGAL","PT", "PRT", "620"},
        {"PUERTO RICO","PR", "PRI", "630"},
        {"QATAR","QA", "QAT", "634"},

        {"RWANDA","RW", "RWA", "646"},

        {"SAMOA","WS", "WSM", "882"},
        {"SAN MARINO","SM", "SMR", "674"},

        {"SAUDI ARABIA","SA", "SAU", "682"},
        {"SENEGAL","SN", "SEN", "686"},
        {"SEYCHELLES","SC", "SYC", "690"},
        {"SIERRA LEONE","SL", "SLE", "694"},
        {"SINGAPORE","SG", "SGP", "702"},
        {"SLOVENIA","SI", "SVN", "705 "},
        {"SOLOMON ISLANDS","SB", "SLB", "090"},
        {"SOMALIA","SO", "SOM", "706"},
        {"SOUTH AFRICA","ZA", "ZAF", "710"},

        {"SPAIN","ES", "ESP", "724"},
        {"SRI LANKA","LK", "LKA", "144"},
        {"SUDAN","SD", "SDN", "736"},
        {"SURINAME","SR", "SUR", "740"},

        {"SWEDEN","SE", "SWE", "752"},
        {"SWITZERLAND","CH", "CHE", "756"},
        {"TAJIKISTAN","TJ", "TJK", "762 "},
        {"THAILAND","TH", "THA", "764"},
        {"TOGO","TG", "TGO", "768"},
        {"TOKELAU","TK", "TKL", "772"},
        {"TONGA","TO", "TON", "776"},

        {"TUNISIA","TN", "TUN", "788"},
        {"TURKEY","TR", "TUR", "792"},
        {"TURKMENISTAN","TM", "TKM", "795 "},

        {"TUVALU","TV", "TUV", "798"},
        {"UGANDA","UG", "UGA", "800"},
        {"UKRAINE","UA", "UKR", "804"},
        {"UNITED ARAB EMIRATES","AE", "ARE", "784"},
        {"UNITED KINGDOM","GB", "GBR", "826"},
        {"UNITED STATES","US", "USA", "840"},

        {"URUGUAY","UY", "URY", "858"},
        {"UZBEKISTAN","UZ", "UZB", "860 "},
        {"VANUATU","VU", "VUT", "548"},
        {"VENEZUELA","VE", "VEN", "862"},
        {"WESTERN SAHARA","EH", "ESH", "732"},
        {"YEMEN","YE", "YEM", "887"},
        {"ZAMBIA","ZM", "ZMB", "894"},
        {"ZIMBABWE","ZW", "ZWE", "716"},
    };
    static final String[][] ISO_369=
    {
        //639-1,old639-1,639-2B,639-3, Language Name (English), [java version]
        {"aa", "aa", "aar", "aar", "Afar"},
        {"ab", "ab", "abk", "abk", "Abkhazian"},        
        {"ae", "ae", "ave", "ave", "Avestan"},        
        {"af", "af", "afr", "afr", "Afrikaans"},        
        {"ak", "ak", "aka", "aka", "Akan"},        
        {"am", "am", "amh", "amh", "Amharic"},        
        {"an", "an", "arg", "arg", "Aragonese"},        
        {"ar", "ar", "ara", "ara", "Arabic"},        
        {"as", "as", "asm", "asm", "Assamese"},        
        {"av", "av", "ava", "ava", "Avaric"},        
        {"ay", "ay", "aym", "aym", "Aymara"},        
        {"az", "az", "aze", "aze", "Azerbaijani"},        
        {"ba", "ba", "bak", "bak", "Bashkir"},        
        {"be", "be", "bel", "bel", "Belarusian"},        
        {"bg", "bg", "bul", "bul", "Bulgarian"},        
        {"bi", "bi", "bis", "bis", "Bislama"},
        {"bm", "bm", "bam", "bam", "Bambara"},
        {"bn", "bn", "ben", "ben", "Bangla"},
        {"bo", "bo", "tib", "bod", "Tibetan"},
        {"br", "br", "bre", "bre", "Breton"},
        {"bs", "bs", "bos", "bos", "Bosnian"},
        {"ca", "ca", "cat", "cat", "Catalan"},
        {"ce", "ce", "che", "che", "Chechen"},
        {"ch", "ch", "cha", "cha", "Chamorro"},
        {"co", "co", "cos", "cos", "Corsican"},
        {"cr", "cr", "cre", "cre", "Cree"},
        {"cs", "cs", "cze", "ces", "Czech"},
        {"cu", "cu", "chu", "chu", "Church Slavic"},
        {"cv", "cv", "chv", "chv", "Chuvash"},
        {"cy", "cy", "wel", "cym", "Welsh"},
        {"da", "da", "dan", "dan", "Danish"},
        {"de", "de", "ger", "deu", "German"},
        {"dv", "dv", "div", "div", "Divehi"},
        {"dv", "dv", "div", "div", "Dhivehi"},
        {"dz", "dz", "dzo", "dzo", "Dzongkha"},
        {"ee", "ee", "ewe", "ewe", "Ewe"},
        {"el", "el", "gre", "ell", "Greek"},// (1453-)
        {"en", "en", "eng", "eng", "English"},
        {"eo", "eo", "epo", "epo", "Esperanto"},
        {"es", "es", "spa", "spa", "Spanish"},
        {"et", "et", "est", "est", "Estonian"},
        {"eu", "eu", "baq", "eus", "Basque"},
        {"fa", "fa", "per", "fas", "Persian"},
        {"ff", "ff", "ful", "ful", "Fulah"},
        {"fi", "fi", "fin", "fin", "Finnish"},
        {"fj", "fj", "fij", "fij", "Fijian"},
        {"fo", "fo", "fao", "fao", "Faroese"},
        {"fr", "fr", "fre", "fra", "French"},
        {"fr", "fr", "fre", "fra", "français"},
        
        {"ga", "ga", "gle", "gle", "Irish"},
        {"gd", "gd", "gla", "gla", "Scottish Gaelic"},
        
        {"gn", "gn", "grn", "grn", "Guarani"},
        {"gu", "gu", "guj", "guj", "Gujarati"},
        {"gv", "gv", "glv", "glv", "Manx"},
        {"ha", "ha", "hau", "hau", "Hausa"},

        {"hi", "hi", "hin", "hin", "Hindi"},
        {"ho", "ho", "hmo", "hmo", "Hiri Motu"},
        {"hr", "hr", "hrv", "hrv", "Croatian"},
        
        {"hu", "hu", "hun", "hun", "Hungarian"},
        {"hy", "hy", "arm", "hye", "Armenian"},
        {"hz", "hz", "her", "her", "Herero"},
        {"ia", "ia", "ina", "ina", "Interlingua"},
        
        {"ie", "ie", "ile", "ile", "Interlingue"},
        {"ig", "ig", "ibo", "ibo", "Igbo"},
        {"ii", "ii", "iii", "iii", "Sichuan Yi"},
        {"ik", "ik", "ipk", "ipk", "Inupiaq"},
        {"io", "io", "ido", "ido", "Ido"},
        {"is", "is", "ice", "isl", "Icelandic"},
        {"it", "it", "ita", "ita", "Italian"},
        {"iu", "iu", "iku", "iku", "Inuktitut"},
        {"ja", "ja", "jpn", "jpn", "Japanese"},
        {"jv", "jv", "jav", "jav", "Javanese"},
        {"ka", "ka", "geo", "kat", "Georgian"},
        {"kg", "kg", "kon", "kon", "Kongo"},
        {"ki", "ki", "kik", "kik", "Kikuyu"},
        
        {"kk", "kk", "kaz", "kaz", "Kazakh"},
        {"kl", "kl", "kal", "kal", "Kalaallisut"},
        {"km", "km", "khm", "khm", "Khmer"},
        {"kn", "kn", "kan", "kan", "Kannada"},
        {"ko", "ko", "kor", "kor", "Korean"},
        {"kr", "kr", "kau", "kau", "Kanuri"},
        {"ks", "ks", "kas", "kas", "Kashmiri"},
        {"ku", "ku", "kur", "kur", "Kurdish"},
        {"kv", "kv", "kom", "kom", "Komi"},
        {"kw", "kw", "cor", "cor", "Cornish"},

        {"la", "la", "lat", "lat", "Latin"},
        {"lb", "lb", "ltz", "ltz", "Luxembourgish"},
        {"lg", "lg", "lug", "lug", "Ganda"},
        {"li", "li", "lim", "lim", "Limburgan"},
        {"ln", "ln", "lin", "lin", "Lingala"},
        {"lo", "lo", "lao", "lao", "Lao"},
        {"lt", "lt", "lit", "lit", "Lithuanian"},
        {"lu", "lu", "lub", "lub", "Luba-Katanga"},
        {"lv", "lv", "lav", "lav", "Latvian"},
        {"mg", "mg", "mlg", "mlg", "Malagasy"},
        {"mh", "mh", "mah", "mah", "Marshallese"},
        {"mi", "mi", "mao", "mri", "Maori"},
        {"mk", "mk", "mac", "mkd", "Macedonian"},
        {"ml", "ml", "mal", "mal", "Malayalam"},
        {"mn", "mn", "mon", "mon", "Mongolian"},
        {"mr", "mr", "mar", "mar", "Marathi"},
        {"ms", "ms", "may", "msa", "Malay"},
        {"mt", "mt", "mlt", "mlt", "Maltese"},
        {"my", "my", "bur", "mya", "Burmese"},
        {"na", "na", "nau", "nau", "Nauru"},
        {"nb", "nb", "nob", "nob", "Norwegian Bokmål"},
        {"nd", "nd", "nde", "nde", "North Ndebele"},
        {"ne", "ne", "nep", "nep", "Nepali"},
        {"ng", "ng", "ndo", "ndo", "Ndonga"},
        {"nl", "nl", "dut", "nld", "Dutch"},
        {"nn", "nn", "nno", "nno", "Norwegian Nynorsk"},
        {"no", "no", "nor", "nor", "Norwegian"},
        {"nr", "nr", "nbl", "nbl", "South Ndebele"},
        {"nv", "nv", "nav", "nav", "Navajo"},
        {"ny", "ny", "nya", "nya", "Nyanja"},
        {"oc", "oc", "oci", "oci", "Occitan"},// (post 1500)
        {"oj", "oj", "oji", "oji", "Ojibwa"},
        {"om", "om", "orm", "orm", "Oromo"},
        
        {"pi", "pi", "pli", "pli", "Pali"},
        {"pl", "pl", "pol", "pol", "Polish"},

        {"pt", "pt", "por", "por", "Portuguese"},
        {"qu", "qu", "que", "que", "Quechua"},

        {"rn", "rn", "run", "run", "Rundi"},
        {"ro", "ro", "rum", "ron", "Romanian"},
        {"ru", "ru", "rus", "rus", "Russian"},
        {"rw", "rw", "kin", "kin", "Kinyarwanda"},
        {"sa", "sa", "san", "san", "Sanskrit"},
        {"sc", "sc", "srd", "srd", "Sardinian"},
        {"sd", "sd", "snd", "snd", "Sindhi"},
        {"se", "se", "sme", "sme", "Northern Sami"},
        {"sg", "sg", "sag", "sag", "Sango"},

        {"sk", "sk", "slo", "slk", "Slovak"},
        {"sl", "sl", "slv", "slv", "Slovenian"},
        {"sm", "sm", "smo", "smo", "Samoan"},
        {"sn", "sn", "sna", "sna", "Shona"},
        {"so", "so", "som", "som", "Somali"},
        {"sq", "sq", "alb", "sqi", "Albanian"},
        {"sr", "sr", "srp", "srp", "Serbian"},
        {"ss", "ss", "ssw", "ssw", "Swati"},
        {"st", "st", "sot", "sot", "Southern Sotho"},
        {"su", "su", "sun", "sun", "Sundanese"},
        {"sv", "sv", "swe", "swe", "Swedish"},
        {"sw", "sw", "swa", "swa", "Swahili"},
        {"ta", "ta", "tam", "tam", "Tamil"},
        {"te", "te", "tel", "tel", "Telugu"},
        {"tg", "tg", "tgk", "tgk", "Tajik"},
        {"th", "th", "tha", "tha", "Thai"},
        {"ti", "ti", "tir", "tir", "Tigrinya"},
        {"tk", "tk", "tuk", "tuk", "Turkmen"},
        {"tl", "tl", "tgl", "tgl", "Tagalog"},
        {"tn", "tn", "tsn", "tsn", "Tswana"},

        {"tr", "tr", "tur", "tur", "Turkish"},
        {"ts", "ts", "tso", "tso", "Tsonga"},
        {"tt", "tt", "tat", "tat", "Tatar"},
        {"tw", "tw", "twi", "twi", "Twi"},
        {"ty", "ty", "tah", "tah", "Tahitian"},

        {"uk", "uk", "ukr", "ukr", "Ukrainian"},
        {"ur", "ur", "urd", "urd", "Urdu"},
        {"uz", "uz", "uzb", "uzb", "Uzbek"},
        {"ve", "ve", "ven", "ven", "Venda"},
        {"vi", "vi", "vie", "vie", "Vietnamese"},
        {"vo", "vo", "vol", "vol", "Volapük"},
        {"wa", "wa", "wln", "wln", "Walloon"},
        {"wo", "wo", "wol", "wol", "Wolof"},
        {"xh", "xh", "xho", "xho", "Xhosa"},
            
        {"yo", "yo", "yor", "yor", "Yoruba"},
        {"za", "za", "zha", "zha", "Zhuang"},
        {"zh", "zh", "chi", "zho", "Chinese"},
        {"zu", "zu", "zul", "zul", "Zulu"}
    };

    /**
     * Test of getDisplayLanguageCountry method, of class Locales.
     */
    @Test
    public void testGetDisplayLanguageCountry_Locale_Locale()
    {
        Locale es = new Locale("es");
        Locale en = Locale.ENGLISH;

        Locale es_ES = new Locale("es","ES");
        Locale es_AR = new Locale("es","AR");
        Locale en_US = Locale.US;
        Locale en_GB = Locale.UK;

        assertEquals("español", Locales.getDisplayLanguageCountry(es, null).toLowerCase());
        assertEquals("español", Locales.getDisplayLanguageCountry(es, es).toLowerCase());
        assertEquals("spanish", Locales.getDisplayLanguageCountry(es, en).toLowerCase());

        assertEquals("english", Locales.getDisplayLanguageCountry(en, null).toLowerCase());
        assertEquals("english", Locales.getDisplayLanguageCountry(en, en).toLowerCase());
        assertEquals("inglés", Locales.getDisplayLanguageCountry(en, es).toLowerCase());
        
        assertEquals("español (españa)", Locales.getDisplayLanguageCountry(es_ES, null).toLowerCase());
        assertEquals("español (españa)", Locales.getDisplayLanguageCountry(es_ES, es).toLowerCase());
        assertEquals("spanish (spain)", Locales.getDisplayLanguageCountry(es_ES, en).toLowerCase());
        
        assertEquals("español (argentina)", Locales.getDisplayLanguageCountry(es_AR, null).toLowerCase());
        assertEquals("español (argentina)", Locales.getDisplayLanguageCountry(es_AR, es).toLowerCase());
        assertEquals("spanish (argentina)", Locales.getDisplayLanguageCountry(es_AR, en).toLowerCase());
        
        assertEquals("english (united states)", Locales.getDisplayLanguageCountry(en_US, null).toLowerCase());
        assertEquals("english (united states)", Locales.getDisplayLanguageCountry(en_US, en).toLowerCase());
        assertEquals("inglés (estados unidos)", Locales.getDisplayLanguageCountry(en_US, es).toLowerCase());
        
        assertEquals("english (united kingdom)", Locales.getDisplayLanguageCountry(en_GB, null).toLowerCase());
        assertEquals("english (united kingdom)", Locales.getDisplayLanguageCountry(en_GB, en).toLowerCase());
        assertEquals("inglés (reino unido)", Locales.getDisplayLanguageCountry(en_GB, es).toLowerCase());
    }

    /**
     * Test of getDisplayLanguageCountry method, of class Locales.
     */
    @Test
    public void testGetDisplayLanguageCountry_LocaleArr()
    {
        Locale[] locales = { new Locale("es"), Locale.ENGLISH, new Locale("es","ES"), new Locale("es","AR"), Locale.US, Locale.UK};
        String[] expected= {"español", "english", "español (españa)", "español (argentina)", "english (united states)", "english (united kingdom)"};
        String[] results = Locales.getDisplayLanguageCountry(locales);
        
        assertEquals(expected.length,results.length);
        
        for(int i=0;i<expected.length;i++)
        {
            assertEquals(expected[i], results[i].toLowerCase());
        }
    }

    /**
     * Test of getDisplayLanguageCountry method, of class Locales.
     */
    @Test
    public void testGetDisplayLanguageCountry_LocaleArr_Locale()
    {
        Locale[] locales = { new Locale("es"), Locale.ENGLISH, new Locale("es","ES"), new Locale("es","AR"), Locale.US, Locale.UK};
        String[] expected= {"spanish", "english", "spanish (spain)", "spanish (argentina)", "english (united states)", "english (united kingdom)"};
        String[] results = Locales.getDisplayLanguageCountry(locales, Locale.US);
        
        assertEquals(expected.length,results.length);
        
        for(int i=0;i<expected.length;i++)
        {
            assertEquals(expected[i], results[i].toLowerCase());
        }
    }

    /**
     * Test of getDisplayLanguageCountry method, of class Locales.
     */
    @Test
    public void testGetDisplayLanguageCountry_Locale()
    {
        Locale es = new Locale("es");
        Locale en = Locale.ENGLISH;

        Locale es_ES = new Locale("es","ES");
        Locale es_AR = new Locale("es","AR");
        Locale en_US = Locale.US;
        Locale en_GB = Locale.UK;

        assertEquals("español", Locales.getDisplayLanguageCountry(es).toLowerCase());
        assertEquals("english", Locales.getDisplayLanguageCountry(en).toLowerCase());
        assertEquals("español (españa)", Locales.getDisplayLanguageCountry(es_ES).toLowerCase());
        assertEquals("español (argentina)", Locales.getDisplayLanguageCountry(es_AR).toLowerCase());
        assertEquals("english (united states)", Locales.getDisplayLanguageCountry(en_US).toLowerCase());
        assertEquals("english (united kingdom)", Locales.getDisplayLanguageCountry(en_GB).toLowerCase());
    }

    /**
     * Test of setDefault method, of class Locales.
     */
    @Test
    public void testSetDefault()
    {
        final Locale original = Locale.getDefault();
        final Locale tested1  = original==Locale.US?Locale.UK:Locale.US;
        final Locale tested2  = original==Locale.FRANCE?Locale.UK:Locale.FRANCE;
        
        Locales.setDefault(tested1);
        assertEquals(tested1, Locale.getDefault());

        Locales.setDefault(tested2);
        assertEquals(tested2, Locale.getDefault());

        Locales.setDefault(null);
        assertEquals(original, Locale.getDefault());
        
    }

    /**
     * Test of getDisplayLanguageNative method, of class Locales.
     */
    @Test
    public void testGetDisplayLanguageNative_String()
    {
        String[] iso2 = Locale.getISOLanguages();
        for(String code : iso2)
        {
            String expected = Locales.getDisplayLanguage(code,code);
            String result = Locales.getDisplayLanguageNative(code);
            assertEquals(expected, result);    
        }
    }

    /**
     * Test of getDisplayLanguageNative method, of class Locales.
     */
    @Test
    public void testGetDisplayLanguageNative_StringArr()
    {
        String[] iso2 = Locale.getISOLanguages();
        String[] expected = new String[iso2.length];
        for(int i=0;i<iso2.length;i++)
        {
            expected[i] = Locales.getDisplayLanguageNative(iso2[i]);
        }
        String[] result = Locales.getDisplayLanguageNative(iso2);
        assertArrayEquals(expected, result);
    }

    /**
     * Test of getMacroLanguage method, of class Locales.
     */
    @Test
    public void testGetMacroLanguage()
    {
        assertNull(Locales.getMacroLanguage(null));
        assertEquals("en", Locales.getMacroLanguage("en"));
        assertEquals("es", Locales.getMacroLanguage("es"));
        assertEquals("no", Locales.getMacroLanguage("no"));
        assertEquals("no", Locales.getMacroLanguage("nb"));
        assertEquals("no", Locales.getMacroLanguage("nn"));

        assertEquals("eng", Locales.getMacroLanguage("eng"));
        assertEquals("spa", Locales.getMacroLanguage("spa"));
        assertEquals("nor", Locales.getMacroLanguage("nor"));
        assertEquals("nor", Locales.getMacroLanguage("nob"));
        assertEquals("nor", Locales.getMacroLanguage("nno"));
    }

    /**
     * Test of parse method, of class Locales.
     */
    @Test
    public void testParse()
    {
        assertEquals(Locale.ENGLISH, Locales.parse(Locale.ENGLISH.toString()));
        assertEquals(Locale.FRENCH, Locales.parse(Locale.FRENCH.toString()));
        assertEquals(Locale.US, Locales.parse(Locale.US.toString()));
        assertEquals(Locale.UK, Locales.parse(Locale.UK.toString()));

        assertEquals(new Locale("en"), Locales.parse("en"));
        assertEquals(new Locale("en"), Locales.parse("en_"));
        assertEquals(new Locale("en"), Locales.parse("en__"));
        
        assertEquals(new Locale("eng"), Locales.parse("eng"));
        assertEquals(new Locale("eng"), Locales.parse("eng_"));
        assertEquals(new Locale("eng"), Locales.parse("eng__"));

        assertEquals(new Locale("en","US"), Locales.parse("en_US"));
        assertEquals(new Locale("en","US"), Locales.parse("en_US_"));
        
        assertEquals(new Locale("eng","US"), Locales.parse("eng_US"));
        assertEquals(new Locale("eng","US"), Locales.parse("eng_US_"));
        
        assertEquals(new Locale("","US"), Locales.parse("_US"));
        assertEquals(new Locale("","US"), Locales.parse("_US_"));
        assertEquals(new Locale(""), Locales.parse(""));
        
        assertEquals(new Locale("ast"), Locales.parse("ast"));
    }
    /**
     * Test of parse method, of class Locales.
     */
    @Test
    public void testParseISO2()
    {
        assertEquals(Locale.ENGLISH, Locales.parseISO2(Locale.ENGLISH.toString()));
        assertEquals(Locale.FRENCH, Locales.parseISO2(Locale.FRENCH.toString()));
        assertEquals(Locale.US, Locales.parseISO2(Locale.US.toString()));
        assertEquals(Locale.UK, Locales.parseISO2(Locale.UK.toString()));

        assertEquals(new Locale("en"), Locales.parseISO2("en"));
        assertEquals(new Locale("en"), Locales.parseISO2("en_"));
        assertEquals(new Locale("en"), Locales.parseISO2("en__"));
        
        assertEquals(new Locale("en"), Locales.parseISO2("eng"));
        assertEquals(new Locale("en"), Locales.parseISO2("eng_"));
        assertEquals(new Locale("en"), Locales.parseISO2("eng__"));

        assertEquals(new Locale("en","US"), Locales.parseISO2("en_US"));
        assertEquals(new Locale("en","US"), Locales.parseISO2("en_US_"));
        
        assertEquals(new Locale("en","US"), Locales.parseISO2("eng_US"));
        assertEquals(new Locale("en","US"), Locales.parseISO2("eng_US_"));
        
        assertEquals(new Locale("","US"), Locales.parseISO2("_US"));
        assertEquals(new Locale("","US"), Locales.parseISO2("_US_"));
        assertEquals(new Locale(""), Locales.parseISO2(""));
        
        assertEquals(new Locale("ast"), Locales.parseISO2("ast"));
        
    }
    /**
     * Test of parse method, of class Locales.
     */
    @Test
    public void testParseISO3()
    {
        assertEquals(new Locale("eng"), Locales.parseISO3(Locale.ENGLISH.toString()));
        assertEquals(new Locale("fra"), Locales.parseISO3(Locale.FRENCH.toString()));
        assertEquals(new Locale("eng","USA"), Locales.parseISO3(Locale.US.toString()));
        assertEquals(new Locale("eng","GBR"), Locales.parseISO3(Locale.UK.toString()));

        assertEquals(new Locale("eng"), Locales.parseISO3("en"));
        assertEquals(new Locale("eng"), Locales.parseISO3("en_"));
        assertEquals(new Locale("eng"), Locales.parseISO3("en__"));
        
        assertEquals(new Locale("eng"), Locales.parseISO3("eng"));
        assertEquals(new Locale("eng"), Locales.parseISO3("eng_"));
        assertEquals(new Locale("eng"), Locales.parseISO3("eng__"));

        assertEquals(new Locale("eng","USA"), Locales.parseISO3("en_US"));
        assertEquals(new Locale("eng","USA"), Locales.parseISO3("en_US_"));
        
        assertEquals(new Locale("eng","USA"), Locales.parseISO3("eng_US"));
        assertEquals(new Locale("eng","USA"), Locales.parseISO3("eng_US_"));
        
        assertEquals(new Locale("","USA"), Locales.parseISO3("_US"));
        assertEquals(new Locale("","USA"), Locales.parseISO3("_US_"));
        assertEquals(new Locale(""), Locales.parseISO3(""));
        
        assertEquals(new Locale("ast"), Locales.parseISO3("ast"));
        
    }

    /**
     * Test of getISO2 method, of class Locales.
     */
    @Test
    public void testGetISO2()
    {
        assertEquals(new Locale("en","US"), Locales.getISO2(new Locale("en","US")));
        assertEquals(new Locale("en","US"), Locales.getISO2(new Locale("eng","USA")));
    }

    /**
     * Test of getISO3 method, of class Locales.
     */
    @Test
    public void testGetISO3()
    {
        assertEquals(new Locale("eng","USA"), Locales.getISO3(new Locale("en","US")));
        assertEquals(new Locale("eng","USA"), Locales.getISO3(new Locale("eng","USA")));
    }

    /**
     * Test of contains method, of class Locales.
     */
    @Test
    public void testContains()
    {
        Locale empty = new Locale("");
        Locale es = new Locale("es");
        Locale en = new Locale("en");
        Locale es_ES = new Locale("es","ES");
        Locale en_GB = new Locale("en","GB");
        Locale en_US = new Locale("en","US");
        Locale es_ES_male = new Locale("es","ES","male");
        Locale en_GB_male = new Locale("en","GB","male");
        Locale en_US_male = new Locale("en","US","male");
        
        Locale es_ES_female = new Locale("es","ES","female");
        Locale en_GB_female = new Locale("en","GB","female");
        Locale en_US_female = new Locale("en","US","female");
        
        Locale eng = new Locale("eng");
        Locale spa = new Locale("spa");

        Locale en_ZG = new Locale("en","ZG");
        
        assertTrue(Locales.contains(null, null));
        assertTrue(Locales.contains(empty, empty));
        assertTrue(Locales.contains(es, es));
        assertTrue(Locales.contains(es_ES, es_ES));
        assertTrue(Locales.contains(es_ES_male, es_ES_male));

        assertTrue(Locales.contains(null, empty));
        assertTrue(Locales.contains(empty, null));

        assertTrue(Locales.contains(null, es));
        assertTrue(Locales.contains(null, es_ES));
        assertTrue(Locales.contains(null, es_ES_male));
        assertFalse(Locales.contains(es, null));
        assertFalse(Locales.contains(es_ES, null));
        assertFalse(Locales.contains(es_ES_male, null));

        assertTrue(Locales.contains(empty, es));
        assertTrue(Locales.contains(empty, es_ES));
        assertTrue(Locales.contains(empty, es_ES_male));
        assertFalse(Locales.contains(es, empty));
        assertFalse(Locales.contains(es_ES, empty));
        assertFalse(Locales.contains(es_ES_male, empty));
        
        assertTrue(Locales.contains(es, es_ES));
        assertTrue(Locales.contains(es, es_ES_male));
        assertFalse(Locales.contains(es_ES, es));
        assertFalse(Locales.contains(es_ES_male, es));

        assertTrue(Locales.contains(es_ES, es_ES_male));
        assertFalse(Locales.contains(es_ES_male, es_ES));
        
        assertFalse(Locales.contains(en_GB, en_US));
        assertFalse(Locales.contains(en_GB_male, en_US_male));
        assertFalse(Locales.contains(en_GB_male, en_GB_female));

        assertTrue(Locales.contains(en, en_US));
        assertTrue(Locales.contains(en, en_US_male));
        assertTrue(Locales.contains(en, en_GB_female));
        assertTrue(Locales.contains(eng, en_US));
        assertTrue(Locales.contains(eng, en_US_male));
        assertTrue(Locales.contains(eng, en_GB_female));
        
        assertTrue(Locales.contains(spa, es_ES));
        assertTrue(Locales.contains(spa, es_ES_male));
        assertTrue(Locales.contains(spa, es));
        assertTrue(Locales.contains(spa, es));
        
        assertTrue(Locales.contains(en, en_ZG));
        assertFalse(Locales.contains(es, en_ZG));
        assertFalse(Locales.contains(en_ZG, en));
        assertFalse(Locales.contains(en_ZG, es));
        
    }

    /**
     * Test of getHierarchy method, of class Locales.
     */
    @Test
    public void testGetHierarchy()
    {
        Locale[] es_ES_MALE = {new Locale("es","ES","MALE"),new Locale("es","ES"),new Locale("es"),new Locale("")};
        Locale[] es_ES      = {new Locale("es","ES"),new Locale("es"),new Locale("")};
        Locale[] es         = {new Locale("es"),new Locale("")};
        Locale[] root       = {new Locale("")};
        
        assertNull(Locales.getHierarchy(null));
        assertArrayEquals(root, Locales.getHierarchy(new Locale("")));
        assertArrayEquals(es, Locales.getHierarchy(new Locale("es")));
        assertArrayEquals(es_ES, Locales.getHierarchy(new Locale("es","ES")));
        assertArrayEquals(es_ES_MALE, Locales.getHierarchy(new Locale("es","ES","MALE")));

        //try two times to test cache
        assertNull(Locales.getHierarchy(null));
        assertArrayEquals(root, Locales.getHierarchy(new Locale("")));
        assertArrayEquals(es, Locales.getHierarchy(new Locale("es")));
        assertArrayEquals(es_ES, Locales.getHierarchy(new Locale("es","ES")));
        assertArrayEquals(es_ES_MALE, Locales.getHierarchy(new Locale("es","ES","MALE")));
    }

    /**
     * Test of getParents method, of class Locales.
     */
    @Test
    public void testGetParents()
    {
        Locale[] es_ES_MALE = {new Locale("es","ES"),new Locale("es"),new Locale("")};
        Locale[] es_ES      = {new Locale("es"),new Locale("")};
        Locale[] es         = {new Locale("")};
        Locale[] root       = {};
        
        assertNull(Locales.getParents(null));
        assertArrayEquals(root, Locales.getParents(new Locale("")));
        assertArrayEquals(es, Locales.getParents(new Locale("es")));
        assertArrayEquals(es_ES, Locales.getParents(new Locale("es","ES")));
        assertArrayEquals(es_ES_MALE, Locales.getParents(new Locale("es","ES","MALE")));

        //try two times to test cache
        assertNull(Locales.getParents(null));
        assertArrayEquals(root, Locales.getParents(new Locale("")));
        assertArrayEquals(es, Locales.getParents(new Locale("es")));
        assertArrayEquals(es_ES, Locales.getParents(new Locale("es","ES")));
        assertArrayEquals(es_ES_MALE, Locales.getParents(new Locale("es","ES","MALE")));
    }

}
