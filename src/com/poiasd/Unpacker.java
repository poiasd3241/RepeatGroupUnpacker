package com.poiasd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles unpacking of text that has the following structure:<br/>
 * Any combination of appended LETTER and/or NUMBER[TEXT], where TEXT is a recursive implementation of this rule.<br/>
 * Example of unpacking:<br/>
 * Input: a3[bc]4[d]e<br/>
 * Unpacked: abcbcbcdddde<br/>
 * <p/>
 * The validation rules that determine whether the text input can be unpacked:
 * - One repeat group can contain another one. Example: 2[3[x]y] = xxxyxxxy.<br/>
 * - Input text can only contain English alphabet letters, digits and [ ] characters (open and closed square brackets).<br/>
 * - Numbers must only denote how many times a part of input text has to be repeated.<br/>
 * - Open and closed square brackets must only denote a part of input text to repeat.<br/>
 * <p/>
 * Note: {@link #unpack(String)} method assumes the input text is valid for unpacking.
 * To make sure the input text is valid, pass it to the {@link #isValidForUnpacking(String)}.<br/>
 */
public class Unpacker {
    /**
     * A simple container for a validation check result.<br/>
     *
     * @param isValid {@code true} if the subject of the validation check is valid; otherwise, {@code false}.
     * @param message The validation check result details. Useful for storing the reason the check's subject is invalid.
     */
    public record ValidationResult(boolean isValid, String message) {
    }

    //region Private members

    private static final char openBracket = '[';
    private static final char closedBracket = ']';

    //endregion

    //region Public Methods

    /**
     * Checks if the input text is valid for unpacking.
     * See validation rules: {@link Unpacker}.
     *
     * @param input The input text to validate.
     * @return The validation check result.
     */
    public static ValidationResult isValidForUnpacking(String input) {
        if (input == null || input.trim().length() == 0) {
            return new ValidationResult(false, "The input text must contain non-whitespace characters.");
        }

        // 1. Check the first character.
        char firstChar = input.charAt(0);
        if (firstChar == openBracket || firstChar == closedBracket) {
            return new ValidationResult(false, "First character must be either a letter or a digit.");
        }

        // 2. Check character set.
        {
            if (isCharacterSetValid(input) == false) {
                return new ValidationResult(false, "Can only contain English alphabet letters, digits and [ ] characters (open and closed square brackets).");
            }
        }

        // 3. Check brackets positioning (open-closed balance and order).
        if (isBracketsPositioningValid(input) == false) {
            return new ValidationResult(false, "Invalid open and closed brackets positioning.");
        }

        // 4. Check the next-previous positioning of letters, digits and brackets
        if (isPreviousNextCharacterPositioningValid(input) == false) {
            return new ValidationResult(false, "Invalid positioning of characters.");
        } else {
            return new ValidationResult(true, null);
        }
    }

    /**
     * Unpacks the input text that is assumed valid for unpacking.
     * Unpack behavior is described in {@link Unpacker}.
     *
     * @param input The input text to unpack.
     * @return Unpacked input text.
     */
    public static String unpack(String input) {
        StringBuilder sbUnpacked = new StringBuilder();
        int inputLength = input.length();
        int position = 0;

        while (position < inputLength) {
            if (Character.isDigit(input.charAt(position))) {
                // Create, unpack and append the repeat group.

                RepeatGroup repeatGroup = createRepeatGroup(input, position);
                position += repeatGroup.textLengthBeforeGrouping();

                String repeatGroupBody;

                if (repeatGroup.hasInnerRepeatGroup()) {
                    // Unpack further until there are no inner repeat groups.
                    repeatGroupBody = unpack(repeatGroup.body());
                } else {
                    repeatGroupBody = repeatGroup.body();
                }
                sbUnpacked.append(repeatGroupBody.repeat(repeatGroup.count()));
            } else {
                // Append non-repeat text.
                sbUnpacked.append(input.charAt(position));
                position++;
            }
        }
        return sbUnpacked.toString();
    }

    //endregion

    //region Private Methods

    /**
     * Checks if the characters of the input text belong to a character set that is defined in unpacking rules in {@link Unpacker}.
     *
     * @param input The input text to validate.
     * @return {@code true} all characters are supported; otherwise, {@code false}.
     */
    private static boolean isCharacterSetValid(String input) {
        String regex = String.format("%s{%s}", "[0-9a-zA-Z\\[\\]]", input.length());
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.find();
    }

    /**
     * Checks if open and closed brackets are positioned correctly (open-closed balance and order) for unpacking.<br/>
     * See detailed positioning rules: {@link Unpacker}.
     *
     * @param input The input text to validate.
     * @return {@code true} if all brackets are positioned correctly (open-closed balance and order); otherwise, {@code false}.
     */
    private static boolean isBracketsPositioningValid(String input) {
        int openClosedBracketsBalance = 0;

        for (char currentChar : input.toCharArray()) {
            if (currentChar == openBracket) {
                openClosedBracketsBalance++;
            } else if (currentChar == closedBracket) {
                if (openClosedBracketsBalance == 0) {
                    // Can't close without opening first.
                    return false;
                }
                openClosedBracketsBalance--;
            }
        }
        return openClosedBracketsBalance == 0;
    }

    /**
     * Checks if each next character of the input text can follow a previous one for unpacking.<br/>
     * See positioning rules: {@link Unpacker}.
     *
     * @param input The input text to validate.
     * @return {@code true} if each next character of the input text can follow a previous one; otherwise, {@code false}.
     */
    private static boolean isPreviousNextCharacterPositioningValid(String input) {
        char currentChar;
        char nextChar;
        int inputLength = input.length();

        // Check if the next char can be placed after the current one.
        for (int i = 0; i < inputLength - 1; i++) {
            currentChar = input.charAt(i);
            nextChar = input.charAt(i + 1);

            if (Character.isDigit(currentChar)) {
                if (canFollowDigit(nextChar) == false) {
                    return false;
                }
            } else if (Character.isLetter(currentChar)) {
                if (canFollowLetter(nextChar) == false) {
                    return false;
                }
            } else if (currentChar == openBracket) {
                if (canFollowOpenBracket(nextChar) == false) {
                    return false;
                }
            } else {
                if (canFollowClosedBracket(nextChar) == false) {
                    return false;
                }
            }
        }
        // Last character cannot be a digit.
        currentChar = input.charAt(inputLength - 1);
        if (Character.isDigit(currentChar)) {
            return false;
        }
        // It also cannot be an open bracket, but that check is handled in bracket open-closed balance and order.
        return true;
    }

    /**
     * Checks if the character can follow a digit.<br/>
     * Rules: only a digit or an open bracket can follow a digit.<br/>
     * See detailed positioning rules: {@link Unpacker}.
     *
     * @param charAfterDigit The character following a digit.
     * @return {@code true} if the character can follow a digit; otherwise, {@code false}.
     */
    private static boolean canFollowDigit(char charAfterDigit) {
        return Character.isDigit(charAfterDigit) || charAfterDigit == openBracket;
    }

    /**
     * Checks if the character can follow a letter.<br/>
     * Rules: anything other than an open bracket can follow a letter.<br/>
     * See detailed positioning rules: {@link Unpacker}.
     *
     * @param charAfterLetter The character following a letter.
     * @return {@code true} if the character can follow a letter; otherwise, {@code false}.
     */
    private static boolean canFollowLetter(char charAfterLetter) {
        return charAfterLetter != openBracket;
    }

    /**
     * Checks if the character can follow an open bracket.<br/>
     * Rules: only a digit or a letter can follow an open bracket.<br/>
     * See detailed positioning rules: {@link Unpacker}.
     *
     * @param charAfterOpenBracket The character following an open bracket.
     * @return {@code true} if the character can follow an open bracket; otherwise, {@code false}.
     */
    private static boolean canFollowOpenBracket(char charAfterOpenBracket) {
        return Character.isDigit(charAfterOpenBracket) || Character.isLetter(charAfterOpenBracket);
    }

    /**
     * Checks if the character can follow a closed bracket.<br/>
     * Rules: anything other than an open bracket can follow a closed bracket.<br/>
     * See detailed positioning rules: {@link Unpacker}.
     *
     * @param charAfterClosedBracket The character following a closed bracket.
     * @return {@code true} if the character can follow a closed bracket; otherwise, {@code false}.
     */
    private static boolean canFollowClosedBracket(char charAfterClosedBracket) {
        return charAfterClosedBracket != openBracket;
    }

    /**
     * Creates a repeat group starting at the specified position(index) in the input text.
     *
     * @param input           The input text to create a repeat group from.
     * @param countStartIndex The index (in the input text) of the first digit of the to-be-created repeat group.
     * @return A repeat group.
     */
    private static RepeatGroup createRepeatGroup(String input, int countStartIndex) {
        // First, obtain the count (how many times the body must be repeated).
        int count;
        int position = countStartIndex;

        while (Character.isDigit(input.charAt(position + 1))) {
            position++;
        }
        count = Integer.parseInt(input.substring(countStartIndex, position + 1));

        // Next, obtain the repeat body between outermost brackets, including inner groups.

        // Skip the last digit of the count and the opening bracket.
        position += 2;

        boolean hasInnerRepeatGroup = false;
        StringBuilder sbBody = new StringBuilder();
        char currentChar;
        // Skipped opening bracket! Set open-closed balance to 1.
        int openClosedBracketsBalance = 1;
        do {
            currentChar = input.charAt(position);
            if (currentChar == openBracket) {
                openClosedBracketsBalance++;
                if (openClosedBracketsBalance > 1) {
                    hasInnerRepeatGroup = true;
                }
            } else if (currentChar == closedBracket) {
                openClosedBracketsBalance--;
            }

            // Append anything other than the outermost closed bracket.
            if (openClosedBracketsBalance > 0) {
                sbBody.append(currentChar);
            }
            position++;
        } while (openClosedBracketsBalance > 0);

        return new RepeatGroup(count, sbBody.toString(), hasInnerRepeatGroup,
                position - countStartIndex);
    }

    //endregion
}
