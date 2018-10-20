package monkeylord.XServer.objectparser;

import org.junit.Test;

import static org.junit.Assert.*;

public class ByteArrayParserTest {

    @Test
    public void URLEncode() {
        byte[] toEncode={3,' ','a',56,21,'A','.','#',0x33,-80};
        System.out.println("Encode:");
        System.out.println(ByteArrayParser.URLEncode(toEncode));
    }

    @Test
    public void URLDecode() {
        String toDecode="%f0%23 1234.sa%20a8%2aA.%233";
        System.out.println("Decode:");
        System.out.println(new String(ByteArrayParser.URLDecode(toDecode)));
        System.out.println(ByteArrayParser.URLEncode(ByteArrayParser.URLDecode(toDecode)));
    }
}