package com.sinux.pocketboard.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CharacterUtilsTest {

    @Test
    public void splitToCharactersTest() {
        String emojiStr = "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F\uD83C\uDFC3\uD83C\uDFFB\u200D♀\uD83D\uDC68\uD83C\uDFFB\u200D\uD83E\uDDB0\uD83D\uDC41\u200D\uD83D\uDDE8\uD83D\uDC68\uD83C\uDFFE\u200D\uD83E\uDDB3\uD83D\uDC68\uD83C\uDFFE\u200D\uD83C\uDF3E\uD83C\uDDFB\uD83C\uDDEC";
        List<CharSequence> emojiList = CharacterUtils.splitToCharacters(emojiStr);
        String emojiStrExt = String.join("", emojiList);
        Assert.assertEquals(emojiStr, emojiStrExt);
    }

    @Test
    public void getAllFitzpatrickVariantsTest() {
        int[] fitzpatrickAware = {0x1F3C3, 0x1F468};
        Assert.assertEquals(CharacterUtils.EMOJI_FITZPATRICK_MODIFIERS.size() + 1,
                CharacterUtils.getAllFitzpatrickVariants("\uD83C\uDFC3", fitzpatrickAware).size());
        Assert.assertEquals(CharacterUtils.EMOJI_FITZPATRICK_MODIFIERS.size() + 1,
                CharacterUtils.getAllFitzpatrickVariants("\uD83C\uDFC3\uD83C\uDFFB\u200D♀", fitzpatrickAware).size());
        Assert.assertEquals(CharacterUtils.EMOJI_FITZPATRICK_MODIFIERS.size() + 1,
                CharacterUtils.getAllFitzpatrickVariants("\uD83E\uDDB3\u200D\uD83D\uDC68\uD83C\uDFFE\uD83C\uDF3E", fitzpatrickAware).size());
        Assert.assertEquals(0,
                CharacterUtils.getAllFitzpatrickVariants("\uD83C\uDF3E", fitzpatrickAware).size());
    }

    @Test
    public void getLastCharacterLengthTest() {
        String str = "Running woman emoji: \uD83C\uDFC3\uD83C\uDFFB\u200D♀";
        Assert.assertEquals(6, CharacterUtils.getLastCharacterLength(str));
    }

    @Test
    public void getLastWordStartIndexTest() {
        Assert.assertEquals(0, CharacterUtils.getLastWordStartIndex("word", ""));
        Assert.assertEquals(7, CharacterUtils.getLastWordStartIndex("second word", ""));
        Assert.assertEquals(7, CharacterUtils.getLastWordStartIndex("emoji\uD83D\uDE03", ""));
    }
}
