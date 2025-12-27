// DownloadFileTask.java — already cross-platform compatible
import javafx.concurrent.Task;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.io.File;

/**
 * Downloads a file from URL to local disk with progress reporting.
 * Works on any platform.
 */
public class DownloadFileTask extends Task<Void> {
    private final String url;
    private final File outputFile;

    public DownloadFileTask(String url, File outputFile) {
        this.url = url;
        this.outputFile = outputFile;
    }

    @Override
    protected Void call() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
        int fileSize = conn.getContentLength();

        try (ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024); // 1MB
            long totalRead = 0;
            int bytesRead;

            while ((bytesRead = rbc.read(buffer)) != -1) {
                buffer.flip();
                fos.getChannel().write(buffer);
                buffer.clear();
                totalRead += bytesRead;

                if (fileSize > 0) {
                    double progress = (double) totalRead / fileSize;
                    updateProgress(progress, 1.0);
                    updateMessage(String.format("Download progress: %.2f%%", progress * 100));
                }
            }
        } catch (IOException e) {
            throw e; // Will trigger setOnFailed
        }
        return null;
    }
}