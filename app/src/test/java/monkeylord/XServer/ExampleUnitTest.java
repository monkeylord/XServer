package monkeylord.XServer;

import org.junit.Test;

import monkeylord.XServer.handler.ObjectHandler;
import monkeylord.XServer.objectparser.IntParser;
import monkeylord.XServer.utils.Utils;

import static monkeylord.XServer.XServer.parsers;
import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        parsers.put("I",new IntParser());
        System.out.print(Utils.getTypeSignature(int.class));
        System.out.print(Utils.getTypeSignature(byte[].class));
        System.out.print(Utils.getTypeSignature(String.class));
        System.out.println(ObjectHandler.parseObject("I#24"));
        assertEquals(4, 2 + 2);
    }

}