package com.exiledradio.rlcraftvillagertomes.client;

import com.exiledradio.rlcraftvillagertomes.Feedback;

/**
 * A screen that shows {@link Feedback} lines itself, so a message about what you just did
 * appears where you are looking rather than on the action bar behind the screen.
 */
public interface NoticeTarget {

    /** @param tone one of the {@link Feedback} tones; {@code text} is plain, no color codes */
    void showNotice(byte tone, String text);

    /** Colors for each tone, on the action bar (light, on the dark world). */
    static String actionBarColor(byte tone) {
        switch (tone) {
            case Feedback.GOOD:
                return "§a";
            case Feedback.WARN:
                return "§e";
            case Feedback.BAD:
                return "§c";
            default:
                return "§f";
        }
    }
}
