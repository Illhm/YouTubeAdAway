package ma.wanam.youtubeadaway;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public class FieldLookupTest {

    // Dummy classes for testing hierarchy
    private static class Parent {
        private StringBuilder sbField;
    }

    private static class Child extends Parent {
        private String strField;
    }

    private static class GrandChild extends Child {
        // Empty
    }

    @Test
    public void testFindFieldInHierarchy() throws Exception {
        BFAsync bfAsync = new BFAsync();

        // Access private method
        Method method = BFAsync.class.getDeclaredMethod("findFieldInHierarchy", Class.class, Class.class);
        method.setAccessible(true);

        // Test finding StringBuilder in Parent
        Optional<Field> sbResult = (Optional<Field>) method.invoke(bfAsync, GrandChild.class, StringBuilder.class);
        assertTrue("Should find StringBuilder in hierarchy", sbResult.isPresent());
        assertEquals("Should find correct field", StringBuilder.class, sbResult.get().getType());

        // Test finding String in Child
        Optional<Field> strResult = (Optional<Field>) method.invoke(bfAsync, GrandChild.class, String.class);
        assertTrue("Should find String in hierarchy", strResult.isPresent());
        assertEquals("Should find correct field", String.class, strResult.get().getType());

        // Test finding non-existent type
        Optional<Field> intResult = (Optional<Field>) method.invoke(bfAsync, GrandChild.class, Integer.class);
        assertFalse("Should not find Integer", intResult.isPresent());
    }
}
