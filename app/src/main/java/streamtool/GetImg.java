package streamtool;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class GetImg {
    public static void getImg(String name) throws MalformedURLException, IOException, URISyntaxException {
        BufferedInputStream in = new BufferedInputStream(new URI("https://mc-heads.net/head/" + name).toURL().openStream());

        FileOutputStream out = new FileOutputStream(Paths.get("out_heads", name+".png").toFile());
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }

        out.close();
    }

    public static void getInterviewImg(String name) throws IOException, URISyntaxException {
        BufferedInputStream in = new BufferedInputStream(new URI("https://mc-heads.net/body/" + name).toURL().openStream());
        FileOutputStream out = new FileOutputStream(Paths.get("interview.png").toFile());
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }

        out.close();
    }
}
