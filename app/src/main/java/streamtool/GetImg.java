package streamtool;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class GetImg {
    public static void getImg(String name) throws MalformedURLException, IOException, URISyntaxException {
        BufferedInputStream in = new BufferedInputStream(new URI("https://mc-heads.net/avatar/" + name).toURL().openStream());
        FileOutputStream out = new FileOutputStream("out_heads/" + name + ".png");
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }

        out.close();
    }
}
