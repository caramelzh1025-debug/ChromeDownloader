// PlatformUtils.java
/**
 * Utility class to detect the current operating system and architecture.
 */
public class PlatformUtils {

    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String ARCH = System.getProperty("os.arch").toLowerCase();

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isMac() {
        return OS.contains("mac") || OS.contains("darwin");
    }

    /**
     * Returns the architecture string used in Google Update requests.
     * - Windows: "x64"
     * - macOS: "x64" or "arm64" (we'll use "arm64" to get universal dmg that supports both)
     */
    public static String getArchForUpdateService() {
        if (isWindows()) {
            return "x64";
        } else if (isMac()) {
            // Google returns universal .dmg for arm64 request on macOS
            return "arm64";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + OS);
        }
    }

    /**
     * Returns the platform string for Google Update service.
     */
    public static String getPlatformForUpdateService() {
        if (isWindows()) {
            return "win";
        } else if (isMac()) {
            return "mac";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + OS);
        }
    }

    /**
     * Get version-to-appid/channel mapping based on current OS.
     */
    public static java.util.Map<String, java.util.Map<String, String>> getVersionMapping() {
        java.util.Map<String, java.util.Map<String, String>> map = new java.util.HashMap<>();
        if (isWindows()) {
            String stableAppId = "{8A69D345-D564-463C-AFF1-A69D9E530F96}";
            String canaryAppId = "{4EA16AC7-FD5A-47C3-875B-DBF4A2008C20}";
            map.put("Stable", java.util.Map.of("channel", "x64-stable-multi-chrome", "appid", stableAppId));
            map.put("Beta", java.util.Map.of("channel", "x64-beta-multi-chrome", "appid", stableAppId));
            map.put("Dev", java.util.Map.of("channel", "x64-dev-statsdef_1", "appid", stableAppId));
            map.put("Canary", java.util.Map.of("channel", "x64-canary", "appid", canaryAppId));
        } else if (isMac()) {
            map.put("Stable", java.util.Map.of("channel", "", "appid", "com.google.Chrome"));
            map.put("Beta", java.util.Map.of("channel", "betachannel", "appid", "com.google.Chrome.Beta"));
            map.put("Dev", java.util.Map.of("channel", "devchannel", "appid", "com.google.Chrome.Dev"));
            map.put("Canary", java.util.Map.of("channel", "canarychannel", "appid", "com.google.Chrome.Canary"));
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + OS);
        }
        return map;
    }
}