/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/
package org.oakinger.concordion;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Scans the given concordion output folder for reports and creates an index document
 *
 * @author bangroot
 */
public class ConcordionSummaryReporter
{

    public static void main(String[] args) throws Exception {
        XmlSink sink = new XmlSink();
        File concordionDir = new File( args[0] );
        ConcordionSummaryReporter cr = new ConcordionSummaryReporter( concordionDir );
        cr.generateReport(sink);
        File concordionIndex = new File(concordionDir, "concordion-index.xml");
        concordionIndex.createNewFile();
        sink.writeTo(new FileOutputStream(concordionIndex));
        System.out.println( "concordion index written to " + concordionIndex.getCanonicalPath() );
    }

    private final File concordionDir;

    public ConcordionSummaryReporter(File concordionDir) {
        this.concordionDir = concordionDir;
    }

    void generateReport(Sink sink) {
        if (concordionDir.exists() && concordionDir.isDirectory()) {
            File[] children = concordionDir.listFiles(new FilenameFilterImpl());
            if (0 != children.length) {
                processChildren(children, sink);
            }
        }
    }

    private String findRelativeParentPath(File child) {
        String relative = concordionDir.toURI().relativize(child.getParentFile().toURI()).getPath();
        if (relative.endsWith("/")) {
            relative = relative.substring(0, relative.length()-1);
        }
        if (relative.length() == 0) {
            relative = ".";
        }
        return relative;
    }

    private void processChildren(File[] children, Sink sink) {
        Arrays.sort(children, new Comparator<File>() {
            public int compare(File arg0, File arg1) {
                if (arg0.isFile() && arg1.isFile()) {
                    return arg0.getName().compareTo(arg1.getName());
                } else if (arg0.isDirectory() && arg1.isDirectory()) {
                    return arg0.getName().compareTo(arg1.getName());
                } else {
                    if (arg0.isFile()) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }
        });

        boolean sectionStarted = false;
        boolean sectionEnded = false;
        for (File child : children) {
            String relative = findRelativeParentPath(child);
            if (child.isFile()) {
                if (!sectionStarted) {
                    sink.beginSection(relative);
                    sectionStarted = true;
                }
                sink.addReport(child.getName(), relative + "/" + child.getName(), new XmlReportInfo(child));
            }
            else {
                if (sectionStarted && !sectionEnded) {
                    sink.endSection();
                    sectionEnded = true;
                }
                processChildren( child.listFiles( new FilenameFilterImpl() ), sink );
            }
        }
        if (sectionStarted && !sectionEnded) {
            sink.endSection();
        }
    }

    private static class XmlReportInfo implements Sink.ReportInfo
    {
        final XPath xpath;
        final Document xmlDoc;

        public XmlReportInfo(File fileToEval) {
            try {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                xmlDoc = docBuilder.parse(fileToEval);
                XPathFactory factory = XPathFactory.newInstance();
                xpath = factory.newXPath();
            } catch (Exception e) {
                throw new RuntimeException("Error parsing file " + fileToEval, e);
            }
        }

        public String getIssueNumber() {
            return evaluate("//*[@class='issuenumber']");
        }

        public String getTitle() {
            return evaluate("/html/head/title");
        }

        public boolean isSuccessful() {
            String failures = evaluate("//*[@class='failure']");
            return failures.equals("");
        }

        private String evaluate(String expression)
        {
            try {
                return xpath.evaluate( expression, xmlDoc);
            } catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class FilenameFilterImpl implements FileFilter {

        public FilenameFilterImpl() {
        }

        public boolean accept(File file) {
            if (file.isDirectory()) {
                return !file.getName().equals(".svn");
            } else {
                return file.getName().endsWith(".html");
            }
        }
    }

    /**
     * @author Erich Eichinger
     * @since 15/08/12
     */
    public static interface Sink
    {
        public interface ReportInfo
        {
            boolean isSuccessful();
            String getTitle();
            String getIssueNumber();
        }

        void beginSection(String text);
        void endSection();
        void addReport(String name, String path, ReportInfo reportInfo);
    }

    /**
     * @author Erich Eichinger
     * @since 15/08/12
     */
    public static class XmlSink implements Sink
    {
        private final Document doc;
        private final Transformer transformer;

        private Element currentElement;

        public XmlSink()
        {
            try {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty( OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                doc = docBuilder.newDocument();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Element rootElement = doc.createElement( "concordion-summary" );
            doc.appendChild( rootElement );

            currentElement = rootElement;
        }


        private Element createElement(String name)
        {
            return doc.createElement(name);
        }

        @Override public void beginSection(String text)
        {
            Element sectionElement = createElement( "section" );
            sectionElement.setAttribute("name", text);
            currentElement.appendChild(sectionElement);
            currentElement = sectionElement;
        }

        @Override public void endSection()
        {
            currentElement = (Element) currentElement.getParentNode();
        }

        @Override public void addReport(String name, String path, ReportInfo reportInfo)
        {
            Element reportElement = createElement( "report" );
            reportElement.setAttribute("name", name);
            reportElement.setAttribute("success", String.valueOf( reportInfo.isSuccessful() ));
            reportElement.setAttribute("issuenumber", reportInfo.getIssueNumber());
            reportElement.setAttribute("title", reportInfo.getTitle());
            reportElement.setAttribute("path", path);
            currentElement.appendChild(reportElement);
        }

        public void writeTo(OutputStream stream)
        {
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(stream);

            try {
                transformer.transform( source, result );
            } catch (TransformerException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
