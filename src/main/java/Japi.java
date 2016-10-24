import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Created by Dan on 10/10/2016.
 */
public class Japi {

    private static final String USERAGENT = "Japi (A Web API Tool) https://github.com/fusebox13/Japi";
    private String Japiresponse = null;
    private String documentType;
    private boolean isParsed = false;

    private Document XmlDocument = null;
    private JSONObject JSONDocument = null;

    /**
     * Japi is instantiated using an Factory design pattern.  Japi.get(url) creates a new instance of Japi that stores the
     * response returned from the get request.
     */
    private Japi() { }

    private Japi(String response) {

        this.Japiresponse = response;
        this.setDocumentType();
        this.parse();
    }

    /**
     * Gets the contents of a URL.  Mapped parameters must be in the format of [parameter] in the URL string.
     * eg. http://api.openweathermap.org/data/2.5/weather?zip=[zip]&appid=[appid]
     * @param url - Url to request contents from
     * @param parameters - Api Parameters (Optional)
     * @param headerProperties - Allows you to set Header properties.  This is useful for when API keys must be written to
     *                         the header instead of being passed as a parameter.
     * @return
     */
    public static Japi get(String url, Map<String, String> parameters, Map<String, String> headerProperties) {
        StringBuilder request = new StringBuilder(url);
        StringBuilder response = new StringBuilder();
        URLConnection connection = null;

        if (parameters != null ){
            addParameters(request, parameters);
        }
        try {
            URL requestURL = new URL(request.toString());
            connection = requestURL.openConnection();
            if (headerProperties != null) {
                setHeaderProperties(connection, headerProperties);
                System.out.println(connection.getRequestProperty("User-Agent").toString());
            }
            connection.connect();

            BufferedReader rawResponse = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String temp;
            while ((temp = rawResponse.readLine()) != null) {
                response.append(temp);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Japi(response.toString());
    }

    public static Japi get(String url) {
        return get(url, null, null);
    }
    public static Japi get(String url, Map<String, String> parameters) {
        return get(url, parameters, null);
    }

    /**
     * Helper Method used to stuff the parameters into the request URL
     * @param request
     * @param parameters
     */
    private static void addParameters(StringBuilder request, Map<String, String> parameters) {
        Iterator iterator = parameters.keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            String value = parameters.get(key);
            String target = "[" + key + "]";
            int targetIndex = request.indexOf(target);
            if (targetIndex != -1) {
                request.replace(targetIndex, targetIndex + target.length(), value);
            } else {
                throw new RuntimeException("Parameter [" + key + "] is not defined in the URL.  Please add [" + key +  "] to the URL.");

            }

        }
    }

    /**
     * Helper Method used write properties to the request header.
     * @param connection - URLConnection to be modified
     * @param headerProperties - Map of properties used to update the URLConnections request properties.
     */
    private static void setHeaderProperties(URLConnection connection, Map<String, String> headerProperties) {
        Iterator i = headerProperties.keySet().iterator();

        while (i.hasNext()) {
            String key = i.next().toString();
            String value = headerProperties.get(key);
            connection.setRequestProperty(key, value);
        }
    }


    public void print() {
        System.out.println(this.Japiresponse);
    }

    /**
     * Parses the Japiresponse string and converts it to an XML document which can be searched
     * @return - Returns nulls if there is nothing to parse
     */

    private void parse() {

        if (documentType.equals("xml"))
            this.parseXML();
         else if (documentType.equals("json"))
            this.parseJSON();
         else if (documentType.equals("html"))
            this.parseHTML();
         else
            this.isParsed = false;
    }

    private void parseJSON(){
        this.JSONDocument = new JSONObject(this.Japiresponse);
        this.isParsed = true;
    }

    private void parseHTML() {

        this.isParsed = true;
    }
    private void parseXML() {
        Document document;

        if (this.Japiresponse.equals(null)) {
            return;
        }
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(this.Japiresponse.toString()));
            document = dBuilder.parse(is);

            this.XmlDocument = document;
            this.isParsed = true;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch(ParserConfigurationException e) {
            e.printStackTrace();
        }

    }



    private void setDocumentType(){

        if(this.Japiresponse == null) {
            this.documentType = null;
        }

        String[] splitResponse = Japiresponse.split(" ",3);
        String docType = splitResponse[0];
        String possibleHTMLToken = splitResponse[1];

        if (docType.startsWith("<") && docType.contains("xml")){

            this.documentType = "xml";
        } else if (docType.startsWith("<") && possibleHTMLToken.toLowerCase().contains("html")){
            this.documentType = "html";
        } else if (docType.startsWith("{") || docType.startsWith("[")) {
            this.documentType = "json";
        } else
            this.documentType = "unknown";

    }

    public String getDocumentType() {
        return this.documentType;
    }

    public boolean isParsed(){
        return this.isParsed;
    }

    public void printDocumentType() {
        System.out.print(this.documentType);
    }

    public void printNodes(){
        if (this.documentType.equals("xml"))
            this.printXMLNodes(this.XmlDocument.getDocumentElement());
        else if (this.documentType.equals("json"))
            this.printJSONNodes(this.JSONDocument);
        else
            System.out.println("Unable to print");
    }

    private void printXMLNodes(Node node) {

        System.out.println(node.getNodeName());

        NodeList nodeList = node.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node current = nodeList.item(i);
            if (current.getNodeType() == Node.ELEMENT_NODE){
                printXMLNodes(current);
            }
        }
    }

    private void printJSONNodes(JSONObject root) {


            Iterator<String> i = root.keys();

            while (i.hasNext()) {
                String key = i.next();
                System.out.println(key);
                try {
                    if (root.get(key) instanceof JSONObject || root.get(key) instanceof JSONArray) {
                        //System.out.println(JSONDocument.get(key) + " " + JSONDocument.get(key).getClass());
                        printJSONNodes(JSONDocument.getJSONObject(key));
                    }

                } catch (Throwable e) {

                }
            }


    }

}
