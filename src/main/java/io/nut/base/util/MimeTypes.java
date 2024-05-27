/*
 *  MimeTypes.java
 *
 *  Copyright (C) 2009-2023 francitoshi@gmail.com
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author franci
 */
public class MimeTypes
{
    public static final String APPLICATION_JAVASCRIPT       = "application/javascript";
    public static final String APPLICATION_MSWORD           = "application/msword";
    public static final String APPLICATION_OCTET_STREAM     = "application/octet-stream";
    public static final String APPLICATION_PDF              = "application/pdf";
    public static final String APPLICATION_POSTSCRIPT       = "application/postscript";
    public static final String APPLICATION_RTF              = "application/rtf";
    public static final String APPLICATION_X_COMPRESSED     = "application/x-compressed";
    public static final String APPLICATION_X_DVI            = "application/x-dvi";
    public static final String APPLICATION_X_GTAR           = "application/x-gtar";
    public static final String APPLICATION_X_GZIP           = "application/x-gzip";
    public static final String APPLICATION_X_JAVASCRIPT     = "application/x-javascript";
    public static final String APPLICATION_OGG              = "application/ogg";
    public static final String APPLICATION_X_SH             = "application/x-sh";
    public static final String APPLICATION_X_SHOCKWAVE_FLASH= "application/x-shockwave-flash";
    public static final String APPLICATION_X_TAR            = "application/x-tar";
    public static final String APPLICATION_X_X509_CA_CERT   = "application/x-x509-ca-cert";
    
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_CHART         = "application/vnd.oasis.opendocument.chart";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_CHART_TEMPLATE= "application/vnd.oasis.opendocument.chart-template";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_DATABASE      = "application/vnd.oasis.opendocument.database";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_FORMULA       = "application/vnd.oasis.opendocument.formula";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_FORMULA_TEMPLATE = "application/vnd.oasis.opendocument.formula-template";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_GRAPHICS      = "application/vnd.oasis.opendocument.graphics";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_GRAPHICS_TEMPLATE = "application/vnd.oasis.opendocument.graphics-template";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_IMAGE         = "application/vnd.oasis.opendocument.image";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_IMAGE_TEMPLATE = "application/vnd.oasis.opendocument.image-template";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_PRESENTATION  = "application/vnd.oasis.opendocument.presentation";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_PRESENTATION_TEMPLATE = "application/vnd.oasis.opendocument.presentation-template";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_SPREADSHEET   = "application/vnd.oasis.opendocument.spreadsheet";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_SPREADSHEET_TEMPLATE = "application/vnd.oasis.opendocument.spreadsheet-template";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_TEXT          = "application/vnd.oasis.opendocument.text";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_TEXT_MASTER   = "application/vnd.oasis.opendocument.text-master";
    public static final String APPLICATION_VND_OASIS_OPENDOCUMENT_TEXT_TEMPLATE = "application/vnd.oasis.opendocument.text-template";
    
    public static final String APPLICATION_ZIP              = "application/zip";
    public static final String AUDIO_MPEG                   = "audio/mpeg";
    public static final String AUDIO_MPEG_URL               = "audio/mpeg-url";
    public static final String AUDIO_X_WAV                  = "audio/x-wav";
    public static final String IMAGE_BMP                    = "image/bmp";
    public static final String IMAGE_GIF                    = "image/gif";
    public static final String IMAGE_JPEG                   = "image/jpeg";
    public static final String IMAGE_PNG                    = "image/png";
    public static final String IMAGE_SVG_XML                = "image/svg+xml";
    public static final String IMAGE_TIFF                   = "image/tiff";
    public static final String IMAGE_X_ICON                 = "image/x-icon";
    public static final String TEXT_CSS                     = "text/css";
    public static final String TEXT_HTML                    = "text/html";
    public static final String TEXT_PLAIN                   = "text/plain";
    public static final String TEXT_RICHTEXT                = "text/richtext";
    public static final String TEXT_X_VCARD                 = "text/x-vcard";
    public static final String TEXT_XML                     = "text/xml";
    public static final String VIDEO_MP4                    = "video/mp4";
    public static final String VIDEO_MPEG                   = "video/mpeg";
    public static final String VIDEO_OGG                    = "video/ogg";
    public static final String VIDEO_QUICKTIME              = "video/quicktime";
    public static final String VIDEO_X_FLV                  = "video/x-flv";
    public static final String VIDEO_X_MS_ASF               = "video/x-ms-asf";
    public static final String VIDEO_X_MSVIDEO              = "video/x-msvideo";
    public static final String VIDEO_X_SGI_MOVIE            = "video/x-sgi-movie";
    
    public static final String MIME_DEFAULT_BINARY = APPLICATION_OCTET_STREAM;    

    /**
     *
     */
    static final String[][] EXT_TYPES =
    {
        //verify following types using http://filext.com/
        {"ai",  APPLICATION_POSTSCRIPT},
        {"aif", "audio/x-aiff"},
        {"aifc", "audio/x-aiff"},
        {"aiff", "audio/x-aiff"},
        {"asc",  TEXT_PLAIN},
        {"asf", "video/x.ms.asf"},
        {"asx", "video/x.ms.asx"},
        {"au", "audio/basic"},
        {"avi",	 VIDEO_X_MSVIDEO},

        {"bcpio", "application/x-bcpio"},
        {"bin", "application/octet-stream"},
        {"bmp",  IMAGE_BMP},

        {"cab", "application/x-cabinet"},
        {"cdf", "application/x-netcdf"},
        {"class", "application/java-vm"},
        {"cpio", "application/x-cpio"},
        {"cpt", "application/mac-compactpro"},
        {"crt", "application/x-x509-ca-cert"},
        {"csh", "application/x-csh"},
        {"css",  TEXT_CSS},
        {"csv", "text/comma-separated-values"},
        
        {"dcr", "application/x-director"},
        {"dir", "application/x-director"},
        {"dll", "application/x-msdownload"},
        {"dms", "application/octet-stream"},
        {"doc",  APPLICATION_MSWORD},
        {"dtd", "application/xml-dtd"},
        {"dvi",  APPLICATION_X_DVI},
        {"dxr", "application/x-director"},
        
        {"eps", "application/postscript"},
        {"etx", "text/x-setext"},
        {"exe",  APPLICATION_OCTET_STREAM},
        {"ez", "application/andrew-inset"},
        
        {"flv",  VIDEO_X_FLV},
        
        {"gif",  IMAGE_GIF},
        {"gtar", APPLICATION_X_GTAR},
        {"gz",   "application/gzip"},
        {"gzip", "application/gzip"},
        
        {"hdf", "application/x-hdf"},
        {"hqx", "application/mac-binhex40"},
        {"htc", "text/x-component"},
        {"html", TEXT_HTML},
        {"htm",  TEXT_HTML},
        
        {"ice", "x-conference/x-cooltalk"},
        {"ico",  IMAGE_X_ICON},
        {"ief", "image/ief"},
        {"iges", "model/iges"},
        {"igs", "model/iges"},
        
        {"jar", "application/java-archive"},
        {"java", "text/plain"},
        {"jnlp", "application/x-java-jnlp-file"},
        {"jpeg", IMAGE_JPEG},
        {"jpe",  IMAGE_JPEG},
        {"jpg",  IMAGE_JPEG},
        {"js",   APPLICATION_JAVASCRIPT},
        {"jsp", "text/plain"},
        {"kar", "audio/midi"},
        
        {"latex", "application/x-latex"},
        {"lha",   "application/octet-stream"},
        {"lzh",   "application/octet-stream"},
        
        {"m3u",  AUDIO_MPEG_URL},
        {"man", "application/x-troff-man"},
        {"mathml", "application/mathml+xml"},
        {"me", "application/x-troff-me"},
        {"mesh", "model/mesh"},
        {"mid", "audio/midi"},
        {"midi", "audio/midi"},
        {"mif", "application/vnd.mif"},
        {"mol", "chemical/x-mdl-molfile"},
        {"movie", "video/x-sgi-movie"},
        {"mov",  VIDEO_QUICKTIME},
        {"mp2", AUDIO_MPEG},
        {"mp3",  AUDIO_MPEG},
        {"mp4",  VIDEO_MP4},
        {"mpeg", VIDEO_MPEG},
        {"mpe", "video/mpeg"},
        {"mpga", AUDIO_MPEG},
        {"mpg",  VIDEO_MPEG},
        {"ms", "application/x-troff-ms"},
        {"msh", "model/mesh"},
        {"msi", "application/octet-stream"},
        
        {"nc", "application/x-netcdf"},
        
        {"oda", "application/oda"},
        {"odb",  APPLICATION_VND_OASIS_OPENDOCUMENT_DATABASE},
        {"odc",  APPLICATION_VND_OASIS_OPENDOCUMENT_CHART},
        {"odf",  APPLICATION_VND_OASIS_OPENDOCUMENT_FORMULA},
        {"odft", APPLICATION_VND_OASIS_OPENDOCUMENT_FORMULA_TEMPLATE},
        {"odg",  APPLICATION_VND_OASIS_OPENDOCUMENT_GRAPHICS},
        {"odi",  APPLICATION_VND_OASIS_OPENDOCUMENT_IMAGE},
        {"odm",  APPLICATION_VND_OASIS_OPENDOCUMENT_TEXT_MASTER},
        {"odp",  APPLICATION_VND_OASIS_OPENDOCUMENT_PRESENTATION},
        {"ods",  APPLICATION_VND_OASIS_OPENDOCUMENT_SPREADSHEET},
        {"odt",  APPLICATION_VND_OASIS_OPENDOCUMENT_TEXT},
        {"ogg",  APPLICATION_OGG},
        {"ogv",  VIDEO_OGG},
        {"otc",  APPLICATION_VND_OASIS_OPENDOCUMENT_CHART_TEMPLATE},
        {"otg",  APPLICATION_VND_OASIS_OPENDOCUMENT_GRAPHICS_TEMPLATE},
        {"oti",  APPLICATION_VND_OASIS_OPENDOCUMENT_IMAGE_TEMPLATE},
        {"otp",  APPLICATION_VND_OASIS_OPENDOCUMENT_PRESENTATION_TEMPLATE},
        {"ots",  APPLICATION_VND_OASIS_OPENDOCUMENT_SPREADSHEET_TEMPLATE},
        {"ott",  APPLICATION_VND_OASIS_OPENDOCUMENT_TEXT_TEMPLATE},	
        
        {"pbm", "image/x-portable-bitmap"},
        {"pdb", "chemical/x-pdb"},
        {"pdf",  APPLICATION_PDF},
        {"pgm", "image/x-portable-graymap"},
        {"pgn", "application/x-chess-pgn"},
        {"png",  IMAGE_PNG},
        {"pnm", "image/x-portable-anymap"},
        {"ppm", "image/x-portable-pixmap"},
        {"ppt", "application/vnd.ms-powerpoint"},
        {"properties", TEXT_PLAIN},
        
        {"ps", "application/postscript"},

        {"qt", "video/quicktime"},
        
        {"ra", "audio/x-pn-realaudio"},
        {"ram", "audio/x-pn-realaudio"},
        {"ras", "image/x-cmu-raster"},
        {"rdf", "application/rdf+xml"},
        {"rgb", "image/x-rgb"},
        {"rm", "audio/x-pn-realaudio"},
        {"roff", "application/x-troff"},
        {"rpm", "application/x-rpm"},
        {"rtf",  APPLICATION_RTF},
        {"rtx", "text/richtext"},
        
        {"ser", "application/java-serialized-object"},
        {"sgml", "text/sgml"},
        {"sgm", "text/sgml"},
        {"sh", "application/x-sh"},
        {"shar", "application/x-shar"},
        {"silo", "model/mesh"},
        {"sit", "application/x-stuffit"},
        {"skd", "application/x-koan"},
        {"skm", "application/x-koan"},
        {"skp", "application/x-koan"},
        {"skt", "application/x-koan"},
        {"smi", "application/smil"},
        {"smil", "application/smil"},
        {"snd", "audio/basic"},
        {"spl", "application/x-futuresplash"},
        {"src", "application/x-wais-source"},
        {"sv4cpio", "application/x-sv4cpio"},
        {"sv4crc", "application/x-sv4crc"},
        {"svg",  IMAGE_SVG_XML},
        {"swf",  APPLICATION_X_SHOCKWAVE_FLASH},
        
        {"t", "application/x-troff"},
        {"tar","application/x-tar"},
        {"tar.gz", "application/x-gtar"},
        {"tcl", "application/x-tcl"},
        {"tex", "application/x-tex"},
        {"texi", "application/x-texinfo"},
        {"texinfo", "application/x-texinfo"},
        {"tgz", "application/x-gtar"},
        {"tif",  IMAGE_TIFF},
        {"tiff", IMAGE_TIFF},
        {"tr", "application/x-troff"},
        {"tsv", "text/tab-separated-values"},
        {"txt",  TEXT_PLAIN},
        
        {"ustar", "application/x-ustar"},
        
        {"vcd", "application/x-cdlink"},
        {"vrml", "model/vrml"},
        {"vxml", "application/voicexml+xml"},
        
        {"wav", "audio/x-wav"},
        {"wbmp", "image/vnd.wap.wbmp"},
        {"wmlc", "application/vnd.wap.wmlc"},
        {"wmlsc", "application/vnd.wap.wmlscriptc"},
        {"wmls", "text/vnd.wap.wmlscript"},
        {"wml", "text/vnd.wap.wml"},
        {"wrl", "model/vrml"},
        {"wtls-ca-certificate", "application/vnd.wap.wtls-ca-certificate"},
        
        {"xbm", "image/x-xbitmap"},
        {"xht", "application/xhtml+xml"},
        {"xhtml", "application/xhtml+xml"},
        {"xls", "application/vnd.ms-excel"},
        {"xml",  TEXT_XML},//text/xml implies display. application/xml implies download
        {"xsd",  TEXT_XML},//text/xml implies display. application/xml implies download
        {"xpm", "image/x-xpixmap"},
        {"xsl", "application/xml"},
        {"xslt", "application/xslt+xml"},
        {"xul", "application/vnd.mozilla.xul+xml"},
        {"xwd", "image/x-xwindowdump"},
        {"xyz", "chemical/x-xyz"},
        {"zip", APPLICATION_ZIP},        
    };

    private static final HashMap<String,String> extMap = new HashMap<>();
    private static final ArrayList<String> extDots = new ArrayList<>();
    static 
    {
        for (String[] type : EXT_TYPES)
        {
            String v = extMap.put(type[0], type[1]);
            assert (v==null) : "repeated "+type[0];
            if(type[0].contains("."))
            {
                extDots.add(type[0]);
            }
        }
    }

    public static String getMimeType(String fileName)
    {
        int start = fileName.lastIndexOf(".");
        
        if(start>=0)
        {
            String name = fileName.toLowerCase();
            for (String ext : extDots)
            {
                String dot = "."+ext;
                if(name.equals(ext) || name.endsWith(dot))
                {
                    return extMap.get(ext);
                }
            }
        }
        String ext = (start>=0 && start+1<fileName.length()) ? fileName.substring(start+1) : fileName;
        String type= (ext!=null) ? extMap.get(ext.toLowerCase()) : null;
        
        return (type!=null) ? type : MIME_DEFAULT_BINARY;
    }
    
    public static String getMimeType(File file)
    {
        return getMimeType(file.getName());
    }
    
    public static String getMimeExtension(String fileName)
    {
        int start = fileName.lastIndexOf(".");
        
        if(start>=0)
        {
            String name = fileName.toLowerCase();
            for (String ext : extDots)
            {
                String dot = "."+ext;
                if(name.equals(ext))
                {
                    return fileName;
                }
                if(name.endsWith(dot))
                {
                    return Strings.right(fileName, ext.length());
                }
            }
        }
        String ext = (start>=0 && start+1<fileName.length()) ? fileName.substring(start+1) : fileName;
        String type= (ext!=null) ? extMap.get(ext.toLowerCase()) : null;
        return (type!=null) ? ext : null;
    }
    
}
