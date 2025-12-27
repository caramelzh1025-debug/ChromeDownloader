// FetchLinksTask.java
import javafx.concurrent.Task;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Background task to fetch Chrome offline installer download URLs from Google's update service,
 * automatically adapting to Windows or macOS.
 */
public class FetchLinksTask extends Task<List<String>> {
    private final String versionLabel; // e.g., "Stable", "Beta"

    public FetchLinksTask(String versionLabel) {
        this.versionLabel = versionLabel;
    }

    @Override
    protected List<String> call() throws Exception {
        String sessionid = UUID.randomUUID().toString().toUpperCase();
        String requestid = UUID.randomUUID().toString().toUpperCase();

        // Get platform-specific config
        Map<String, String> config = PlatformUtils.getVersionMapping().get(versionLabel);
        if (config == null) {
            throw new IllegalArgumentException("Unsupported version: " + versionLabel);
        }

        String channel = config.get("channel");
        String appid = config.get("appid");
        String platform = PlatformUtils.getPlatformForUpdateService();
        String arch = PlatformUtils.getArchForUpdateService();

        // Simulate a recent OS version (required by Google)
        String osVersion = PlatformUtils.isWindows() ? "10.0" : "13.0"; // macOS 13+

        URI uri = new URI("https://tools.google.com/service/update2");
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", "Google Update/1.3.32.7;winhttp;cup-ecdsa");
        connection.setRequestProperty("Host", "tools.google.com");
        connection.setDoOutput(true);

        String requestBody = """
            <?xml version="1.0" encoding="UTF-8"?>
            <request protocol="3.0" version="1.3.23.9" shell_version="1.3.21.103" ismachine="0" \
            sessionid="%s" installsource="ondemandcheckforupdate" requestid="%s" dedup="cr">
            <hw physmemory="1200000" sse="1" sse2="1" sse3="1" ssse3="1" sse41="1" sse42="1" avx="1"/>
            <os platform="%s" version="%s" arch="%s"/>
            <app appid="%s" version="" nextversion="" ap="%s" lang="en-US">
                <updatecheck/>
            </app>
            </request>
            """.formatted(sessionid, requestid, platform, osVersion, arch, appid, channel);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes("UTF-8"));
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(connection.getInputStream());

        NodeList urlNodes = doc.getElementsByTagName("url");
        NodeList packageNodes = doc.getElementsByTagName("package");

        List<String> links = new ArrayList<>();
        for (int i = 0; i < urlNodes.getLength(); i++) {
            Element urlEl = (Element) urlNodes.item(i);
            String codebase = urlEl.getAttribute("codebase");
            for (int j = 0; j < packageNodes.getLength(); j++) {
                Element pkgEl = (Element) packageNodes.item(j);
                String name = pkgEl.getAttribute("name");
                links.add(codebase + name);
            }
        }
        return links;
    }
}