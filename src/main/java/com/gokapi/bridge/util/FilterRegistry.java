package com.gokapi.bridge.util;

import com.gokapi.bridge.model.FilterInfo;
import net.sf.okapi.common.filters.IFilter;

import java.util.*;

/**
 * Registry of known Okapi filter classes.
 * Maps fully-qualified class names to metadata and provides filter instantiation.
 */
public class FilterRegistry {

    private static final Map<String, FilterInfo> FILTERS = new LinkedHashMap<>();

    static {
        register(new FilterInfo(
                "net.sf.okapi.filters.openxml.OpenXMLFilter",
                "openxml",
                "Microsoft Office (OpenXML)",
                Arrays.asList(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                ),
                Arrays.asList(".docx", ".xlsx", ".pptx")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.html.HtmlFilter",
                "html",
                "HTML",
                Collections.singletonList("text/html"),
                Arrays.asList(".html", ".htm")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.xliff.XLIFFFilter",
                "xliff",
                "XLIFF",
                Collections.singletonList("application/xliff+xml"),
                Arrays.asList(".xlf", ".xliff")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.its.xml.ITSFilter",
                "xml",
                "XML (ITS)",
                Arrays.asList("text/xml", "application/xml"),
                Collections.singletonList(".xml")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.properties.PropertiesFilter",
                "properties",
                "Java Properties",
                Collections.emptyList(),
                Collections.singletonList(".properties")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.po.POFilter",
                "po",
                "PO (Gettext)",
                Collections.singletonList("text/x-po"),
                Arrays.asList(".po", ".pot")
        ));

        // Expanded filters (34 additional)
        register(new FilterInfo(
                "net.sf.okapi.filters.archive.ArchiveFilter",
                "archive",
                "Archive (ZIP)",
                Collections.singletonList("application/zip"),
                Arrays.asList(".zip", ".jar")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.autoxliff.AutoXLIFFFilter",
                "autoxliff",
                "Auto-detect XLIFF version",
                Collections.singletonList("application/xliff+xml"),
                Arrays.asList(".xlf", ".xliff")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.doxygen.DoxygenFilter",
                "doxygen",
                "Doxygen",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.dtd.DTDFilter",
                "dtd",
                "DTD",
                Collections.singletonList("application/xml-dtd"),
                Collections.singletonList(".dtd")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.epub.EPUBFilter",
                "epub",
                "EPUB",
                Collections.singletonList("application/epub+zip"),
                Collections.singletonList(".epub")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.icml.ICMLFilter",
                "icml",
                "InCopy ICML",
                Collections.emptyList(),
                Collections.singletonList(".icml")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.idml.IDMLFilter",
                "idml",
                "InDesign IDML",
                Collections.emptyList(),
                Collections.singletonList(".idml")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.json.JSONFilter",
                "json",
                "JSON",
                Collections.singletonList("application/json"),
                Collections.singletonList(".json")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.markdown.MarkdownFilter",
                "markdown",
                "Markdown",
                Collections.singletonList("text/markdown"),
                Arrays.asList(".md", ".markdown")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.mif.MIFFilter",
                "mif",
                "FrameMaker MIF",
                Collections.emptyList(),
                Collections.singletonList(".mif")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.mosestext.MosesTextFilter",
                "mosestext",
                "Moses Text",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.openoffice.OpenOfficeFilter",
                "openoffice",
                "OpenDocument (ODF)",
                Arrays.asList(
                        "application/vnd.oasis.opendocument.text",
                        "application/vnd.oasis.opendocument.spreadsheet",
                        "application/vnd.oasis.opendocument.presentation"
                ),
                Arrays.asList(".odt", ".ods", ".odp")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.pdf.PdfFilter",
                "pdf",
                "PDF",
                Collections.singletonList("application/pdf"),
                Collections.singletonList(".pdf")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.pensieve.PensieveFilter",
                "pensieve",
                "Pensieve TM",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.php.PHPContentFilter",
                "php",
                "PHP Content",
                Collections.emptyList(),
                Collections.singletonList(".php")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.plaintext.PlainTextFilter",
                "plaintext",
                "Plain Text",
                Collections.singletonList("text/plain"),
                Collections.singletonList(".txt")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.rainbowkit.RainbowKitFilter",
                "rainbowkit",
                "Rainbow Kit",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.regex.RegexFilter",
                "regex",
                "Regex",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.rtf.RTFFilter",
                "rtf",
                "RTF",
                Collections.singletonList("application/rtf"),
                Collections.singletonList(".rtf")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.sdlpackage.SdlPackageFilter",
                "sdlpackage",
                "SDL Package (SDLPPX)",
                Collections.emptyList(),
                Arrays.asList(".sdlppx", ".sdlrpx")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.vtt.VTTFilter",
                "srt",
                "SRT Subtitles",
                Collections.emptyList(),
                Collections.singletonList(".srt")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.table.TableFilter",
                "table",
                "Table (CSV/TSV)",
                Arrays.asList("text/csv", "text/tab-separated-values"),
                Arrays.asList(".csv", ".tsv")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.tex.TEXFilter",
                "tex",
                "LaTeX",
                Collections.singletonList("application/x-latex"),
                Arrays.asList(".tex", ".latex")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.tmx.TmxFilter",
                "tmx",
                "TMX",
                Collections.emptyList(),
                Collections.singletonList(".tmx")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.transtable.TransTableFilter",
                "transtable",
                "Translation Table",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.transifex.TransifexFilter",
                "transifex",
                "Transifex",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.ts.TsFilter",
                "ts",
                "TS (Qt)",
                Collections.emptyList(),
                Collections.singletonList(".ts")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.ttx.TTXFilter",
                "ttx",
                "TTX (Trados Tag Exchange)",
                Collections.emptyList(),
                Collections.singletonList(".ttx")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.txml.TXMLFilter",
                "txml",
                "TXML (WordFast)",
                Collections.emptyList(),
                Collections.singletonList(".txml")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.vignette.VignetteFilter",
                "vignette",
                "Vignette",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.wiki.WikiFilter",
                "wiki",
                "MediaWiki",
                Collections.emptyList(),
                Collections.singletonList(".wiki")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.xliff2.XLIFF2Filter",
                "xliff2",
                "XLIFF 2.x",
                Collections.singletonList("application/xliff+xml"),
                Collections.singletonList(".xlf")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.xmlstream.XmlStreamFilter",
                "xmlstream",
                "XML Stream",
                Arrays.asList("text/xml", "application/xml"),
                Collections.singletonList(".xml")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.yaml.YamlFilter",
                "yaml",
                "YAML",
                Collections.emptyList(),
                Arrays.asList(".yml", ".yaml")
        ));

        // Additional filters
        register(new FilterInfo(
                "net.sf.okapi.filters.messageformat.MessageFormatFilter",
                "messageformat",
                "ICU MessageFormat",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.xini.XINIFilter",
                "xini",
                "XINI (Across)",
                Collections.emptyList(),
                Collections.singletonList(".xini")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.wsxzpackage.WsxzPackageFilter",
                "wsxz",
                "WSXZ (WorldServer)",
                Collections.emptyList(),
                Collections.singletonList(".wsxz")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.cascadingfilter.CascadingFilter",
                "cascading",
                "Cascading Filter",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.multiparsers.MultiParsersFilter",
                "multiparsers",
                "Multi-Parsers Filter",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        // Additional subtitle formats from subtitles module
        register(new FilterInfo(
                "net.sf.okapi.filters.vtt.VTTFilter",
                "vtt",
                "WebVTT Subtitles",
                Collections.singletonList("text/vtt"),
                Collections.singletonList(".vtt")
        ));

        register(new FilterInfo(
                "net.sf.okapi.filters.ttml.TTMLFilter",
                "ttml",
                "TTML Subtitles",
                Collections.singletonList("application/ttml+xml"),
                Arrays.asList(".ttml", ".xml")
        ));
    }

    private static void register(FilterInfo info) {
        FILTERS.put(info.getFilterClass(), info);
    }

    /**
     * Get metadata for a filter class.
     *
     * @param filterClass fully-qualified Java class name
     * @return FilterInfo or null if not found
     */
    public static FilterInfo getFilterInfo(String filterClass) {
        return FILTERS.get(filterClass);
    }

    /**
     * Create a new instance of the specified filter.
     *
     * @param filterClass fully-qualified Java class name
     * @return new IFilter instance or null
     */
    public static IFilter createFilter(String filterClass) {
        try {
            Class<?> clazz = Class.forName(filterClass);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof IFilter) {
                return (IFilter) instance;
            }
            return null;
        } catch (Exception e) {
            System.err.println("[bridge] Failed to instantiate filter " + filterClass + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * List all registered filters.
     */
    public static List<FilterInfo> listFilters() {
        return new ArrayList<>(FILTERS.values());
    }
}
