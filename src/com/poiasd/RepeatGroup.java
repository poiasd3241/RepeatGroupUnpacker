package com.poiasd;

/**
 * Stores text that must be repeated.
 *
 * @param count                    How many times the body must be repeated.
 * @param body                     The text that must be repeated.
 * @param hasInnerRepeatGroup      {@code true} when the body contains text that can be presented as a {@code RepeatGroup}; otherwise, {@code false}.
 * @param textLengthBeforeGrouping The length of the text that this {@code RepeatGroup} was created from.
 */
public record RepeatGroup(int count, String body, Boolean hasInnerRepeatGroup, int textLengthBeforeGrouping) {
}
