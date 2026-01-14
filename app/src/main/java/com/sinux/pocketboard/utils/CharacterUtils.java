package com.sinux.pocketboard.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class CharacterUtils {

    public static final int ZWJ = 0x200D;
    public static final List<Integer> EMOJI_FITZPATRICK_MODIFIERS = Arrays.asList(0x1F3FB, 0x1F3FC, 0x1F3FD, 0x1F3FE, 0x1F3FF);
    public static final List<Integer> EMOJI_VARIANT_SELECTORS = Arrays.asList(0xFE0E, 0xFE0F);
    private static final List<Integer> SUBDIVISION_FLAG_PARTS = Arrays.asList(0xE0062, 0xE0063, 0xE0065, 0xE0067, 0xE006C, 0xE006E, 0xE0073, 0xE0074, 0xE0077, 0xE007F);
    private static final int HANDS_EMOJI = 0x1F91D;

    /**
     * Split string to separate Unicode characters (helps to extract emoji and UTF-16 surrogate symbols)
     */
    public static List<CharSequence> splitToCharacters(CharSequence str) {
        List<CharSequence> result = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        final AtomicBoolean afterZWJ = new AtomicBoolean(false);
        final AtomicBoolean afterCommonFlagPart = new AtomicBoolean(false);

        str.codePoints().forEach(codePoint -> {
            if (builder.length() != 0 && !afterZWJ.get() && !afterCommonFlagPart.get() &&
                    !isEmojiModifier(codePoint) && !isSubdivisionFlagPart(codePoint)) {
                result.add(builder.toString());
                builder.setLength(0);
            }
            afterZWJ.set(codePoint == ZWJ);
            afterCommonFlagPart.set(builder.length() == 0 && isCommonFlagPart(codePoint));
            builder.appendCodePoint(codePoint);
        });

        if (builder.length() > 0) {
            result.add(builder.toString());
        }

        return result;
    }

    public static boolean isEmojiModifier(int codePoint) {
        return codePoint == ZWJ ||
                EMOJI_VARIANT_SELECTORS.contains(codePoint) ||
                EMOJI_FITZPATRICK_MODIFIERS.contains(codePoint) ||
                Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.COMBINING_MARKS_FOR_SYMBOLS;
    }

    public static boolean isCommonFlagPart(int codePoint) {
        return codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF;
    }

    public static boolean isSubdivisionFlagPart(int codePoint) {
        return SUBDIVISION_FLAG_PARTS.contains(codePoint);
    }

    /**
     * Builds collection of given emojis string with all possible Fitzpatrick skin tone variants applied
     *
     * @param str input string with or without emoji
     * @param fitzpatrickAwareEmojis an array of emoji codePoints which skin tone modifiers can be applied to
     * @return collection containing emojis string with skin tone modifiers applied or empty collection (if there is no emoji that can be used with modifiers)
     */
    public static List<CharSequence> getAllFitzpatrickVariants(CharSequence str, Set<Integer> fitzpatrickAwareEmojis) {
        List<CharSequence> result = new ArrayList<>(EMOJI_FITZPATRICK_MODIFIERS.size());

        // Put default variant without modifiers
        int[] cleanCodePoints = str.codePoints()
                .filter(codePoint -> !EMOJI_FITZPATRICK_MODIFIERS.contains(codePoint))
                .toArray();
        result.add(new String(cleanCodePoints, 0, cleanCodePoints.length));

        // Put modified versions
        AtomicInteger fitzpatrickModifiersInserted = new AtomicInteger(0);
        StringBuilder[] fitzpatrickVariants = Stream
                .generate(StringBuilder::new)
                .limit(EMOJI_FITZPATRICK_MODIFIERS.size())
                .toArray(StringBuilder[]::new);

        for (int i = 0; i < cleanCodePoints.length; i++) {
            int codePoint = cleanCodePoints[i];
            for (int j = 0; j < fitzpatrickVariants.length; j++) {
                fitzpatrickVariants[j].appendCodePoint(codePoint);
                // Skip hands if not first codePoint (workaround for "People Holding Hands" emoji)
                if (codePoint == HANDS_EMOJI && i > 0) {
                    continue;
                }
                if (fitzpatrickAwareEmojis.contains(codePoint)) {
                    fitzpatrickModifiersInserted.incrementAndGet();
                    fitzpatrickVariants[j].appendCodePoint(EMOJI_FITZPATRICK_MODIFIERS.get(j));
                }
            }
        }

        if (fitzpatrickModifiersInserted.get() > 0) {
            result.addAll(Arrays.asList(fitzpatrickVariants));
            return result;
        }

        return Collections.emptyList();
    }

    /**
     * Returns length of last UTF-16 characters
     * Can handle ZWJ-combined emoji as well
     */
    public static int getLastCharacterLength(CharSequence str) {
        int result = 0;
        int[] codePoints = str.codePoints().toArray();
        boolean afterModifier = false;
        boolean afterFlagPart = false;

        for (int i = codePoints.length - 1; i >= 0; i--) {
            int codePoint = codePoints[i];
            if (i == codePoints.length - 1 || afterModifier || afterFlagPart || codePoint == ZWJ) {
                result += Character.charCount(codePoint);
                afterModifier = isEmojiModifier(codePoint);
                afterFlagPart = ((i == codePoints.length - 1) && isCommonFlagPart(codePoint)) || isSubdivisionFlagPart(codePoint);
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Lookup for last non-digit and non-letter character in string
     *
     * @return 0 if there is no separators in given string, otherwise returns index of the first char of the word
     */
    public static int getLastWordStartIndex(CharSequence str, String nonLetterAndDigitExclusions) {
        final int[] codePoints = str.codePoints().toArray();
        int charCounter = str.length();

        for (int i = codePoints.length - 1; i >= 0; i--) {
            int codePoint = codePoints[i];
            if (!Character.isLetterOrDigit(codePoint) &&
                    !nonLetterAndDigitExclusions.contains(new String(Character.toChars(codePoint)))) {
                return charCounter;
            }
            charCounter -= Character.charCount(codePoint);
        }

        return 0;
    }

    /**
     * Check if given char is a punctuation symbol
     */
    public static boolean isPunctuationCharacter(int codePoint) {
        return codePoint == ',' || codePoint == '.' || codePoint == '?' || codePoint == '!';
    }

    public static boolean isLetterOrDigitAndSpace(CharSequence str) {
        int lastCharPos = str != null ? str.length() - 1 : 0;

        if (lastCharPos > 0 && Character.isSpaceChar(str.charAt(lastCharPos))) {
            return Character.isLetterOrDigit(str.charAt(lastCharPos - 1)) ||
                    (str.length() > 2 &&
                            Character.isLetterOrDigit(Character.toCodePoint(str.charAt(lastCharPos - 2),
                                    str.charAt(lastCharPos - 1))));
        }

        return false;
    }

    public static String capitalizeFirstLetter(CharSequence str) {
        if (str == null || str.length() == 0) {
            return null;
        }

        int firstCodePoint = Character.codePointAt(str, 0);
        int firstCharCount = Character.charCount(firstCodePoint);

        StringBuilder sb = new StringBuilder();
        sb.appendCodePoint(Character.toUpperCase(firstCodePoint));
        sb.append(str.subSequence(firstCharCount, str.length()));

        return sb.toString();
    }

    public static CapitalizationType getCapitalizationType(CharSequence str) {
        if (str == null || str.length() == 0) {
            return CapitalizationType.NONE;
        }

        int firstCp = Character.codePointAt(str, 0);
        if (!Character.isUpperCase(firstCp)) {
            return CapitalizationType.NONE;
        }

        if (str.codePoints().allMatch(Character::isUpperCase)) {
            return CapitalizationType.ALL_UPPER;
        } else {
            return CapitalizationType.FIRST_UPPER;
        }
    }

    public enum CapitalizationType {
        ALL_UPPER, FIRST_UPPER, NONE
    }
}
