package ma.wanam.youtubeadaway;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

import ma.wanam.youtubeadaway.utils.Class3C;

public class AdBlockingLogicTest {

    @Test
    public void testClass3CGenerator() {
        Class3C generator = new Class3C();

        assertTrue("Generator should have next item", generator.hasNext());
        assertEquals("First item should be aaa", "aaa", generator.next());
        assertEquals("Second item should be aab", "aab", generator.next());

        // Fast forward a bit to check rollover
        // 'aab' was just consumed.
        // We need to verify 'aaz' -> 'aba' logic if we want, but let's just count total.

        int count = 2; // already consumed 2
        while (generator.hasNext()) {
            generator.next();
            count++;
        }

        // 26 * 26 * 26 = 17576
        assertEquals("Total combinations should be 26^3", 17576, count);
    }

    @Test
    public void testAdBlockingRegex() throws Exception {
        // Access private static final fields from BFAsync using Reflection
        Field filterAdsField = BFAsync.class.getDeclaredField("filterAds");
        filterAdsField.setAccessible(true);
        String filterAdsRegex = (String) filterAdsField.get(null);

        Field filterIgnoreField = BFAsync.class.getDeclaredField("filterIgnore");
        filterIgnoreField.setAccessible(true);
        String filterIgnoreRegex = (String) filterIgnoreField.get(null);

        assertNotNull("filterAds regex should not be null", filterAdsRegex);
        assertNotNull("filterIgnore regex should not be null", filterIgnoreRegex);

        // Test Filter Ads Regex
        // The regex is designed to match the entire string due to .* at start and end
        assertTrue("Should match banner_text_icon", "some_prefix_banner_text_icon_suffix".matches(filterAdsRegex));
        assertTrue("Should match ads_video_with_context", "ads_video_with_context".matches(filterAdsRegex));
        assertTrue("Should match carousel_ad", "prefix_carousel_ad".matches(filterAdsRegex));

        assertFalse("Should not match random string", "random_string_element".matches(filterAdsRegex));
        assertFalse("Should not match partial without context if regex is strict", "ban_ner_text_icon".matches(filterAdsRegex));

        // Test Filter Ignore Regex
        assertTrue("Should match related_video_with_context", "related_video_with_context_something".matches(filterIgnoreRegex));
        assertTrue("Should match comment_thread", "comment_thread_renderer".matches(filterIgnoreRegex));
        assertTrue("Should match comment.", "comment.renderer".matches(filterIgnoreRegex));

        assertFalse("Should not match ad keyword", "banner_text_icon".matches(filterIgnoreRegex));
    }
}
