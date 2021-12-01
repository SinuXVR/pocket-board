package com.sinux.pocketboard.input.mapping;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

public class KeyboardMappingParserTest {

    @Test
    public void parseMappingTest() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<KeyboardMapping>\n" +
                "    <Key code=\"1\" value=\"a\" shiftValue=\"A\">\n" +
                "        <Alt value=\"0\" />\n" +
                "    </Key>\n" +
                "    <Key code=\"2\" value=\"b\" />\n" +
                "    <Key code=\"3\" value=\"c\" shiftValue=\"C\">\n" +
                "        <Add value=\"d\" shiftValue=\"D\" />\n" +
                "        <Alt value=\"*\" />\n" +
                "        <Alt value=\"=\" />\n" +
                "    </Key>\n" +
                "</KeyboardMapping>";

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));

        KeyboardMapping mapping = new KeyboardMappingParser(parser).parseMapping();

        KeyMapping keyMapping = mapping.getKeyMapping(-1);
        Assert.assertNull(keyMapping);

        keyMapping = mapping.getKeyMapping(1);
        Assert.assertEquals('a', keyMapping.getValue(false, false, (byte) 0));
        Assert.assertEquals('A', keyMapping.getValue(true, false, (byte) 0));
        Assert.assertEquals('0', keyMapping.getValue(true, true, (byte) 0));
        Assert.assertEquals('0', keyMapping.getValue(false, true, (byte) 0));
        Assert.assertFalse(keyMapping.hasAdditionalValues(false));
        Assert.assertFalse(keyMapping.hasAdditionalValues(true));

        keyMapping = mapping.getKeyMapping(2);
        Assert.assertEquals('b', keyMapping.getValue(false, false, (byte) 0));
        Assert.assertEquals('b', keyMapping.getValue(true, false, (byte) 0));
        Assert.assertEquals('b', keyMapping.getValue(true, true, (byte) 0));
        Assert.assertEquals('b', keyMapping.getValue(false, true, (byte) 0));
        Assert.assertFalse(keyMapping.hasAdditionalValues(false));
        Assert.assertFalse(keyMapping.hasAdditionalValues(true));

        keyMapping = mapping.getKeyMapping(3);
        Assert.assertEquals('c', keyMapping.getValue(false, false, (byte) 0));
        Assert.assertEquals('C', keyMapping.getValue(true, false, (byte) 0));
        Assert.assertEquals('d', keyMapping.getValue(false, false, (byte) 1));
        Assert.assertEquals('D', keyMapping.getValue(true, false, (byte) 1));
        Assert.assertEquals('*', keyMapping.getValue(false, true, (byte) 0));
        Assert.assertEquals('=', keyMapping.getValue(false, true, (byte) 1));
        Assert.assertTrue(keyMapping.hasAdditionalValues(false));
        Assert.assertTrue(keyMapping.hasAdditionalValues(true));
    }
}
