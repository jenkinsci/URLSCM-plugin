package hudson.plugins.URLSCM;

import java.util.HashMap;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link URLParameter}
 */
public class URLParameterTests {

    /**
     * Tests an exception is thrown if the parameter name contains invalid characters
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfParameterContainsInvalidCharacters() {
        URLParameter p = new URLParameter("invalid!par ameter");
    }

    /**
     * Tests a parameter substitutes itself in a parameterised string.
     */
    @Test
    public void shouldSubstituteParameter() {
        URLParameter param = new URLParameter("host");

        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("host", "example");

        String result = param.substitute("http://${host}.com/path", parameters);

        assertEquals("http://example.com/path", result);
    }

    /**
     * Tests a parameter does not try to substitute itself if the corresponding build parameter does not exist.
     */
    @Test
    public void shouldNotSubstituteIfParameterNotFound() {
        URLParameter param = new URLParameter("test");

        String parameterised = "http://example.com/${test}";
        String result = param.substitute(parameterised, new HashMap<String, String>());

        assertEquals(parameterised, result);
    }

    /**
     * Tests build parameters are found in a parameterised string.
     */
    @Test
    public void shouldFindParameters() {
        String url = "http://${host}.com/${testPath}";
        Set<URLParameter> parameters = URLParameter.getParameters(url);

        assertEquals(2, parameters.size());
    }

    /**
     * Tests no parameters are found in a string without any valid parameters.
     */
    @Test
    public void shouldNotFindAnyParameters() {
        String str = "String ${with no parameters!}${}";
        Set<URLParameter> parameters = URLParameter.getParameters(str);

        assertEquals(0, parameters.size());
    }

    /**
     * Tests repeated parameters are only returned once.
     */
    @Test
    public void shouldNotDuplicateParameters() {
        String parameterised = "${param} followed by ${param}";
        Set<URLParameter> parameters = URLParameter.getParameters(parameterised);

        assertEquals(1, parameters.size());
    }

    /**
     * Tests all parameters are substituted into a parameterised string.
     */
    @Test
    public void shouldSubstituteAll() {
        String parameterised = "http://${host}.com/${path1}/${path2}";

        HashMap<String, String> buildParameters = new HashMap<String, String>();
        buildParameters.put("host", "example");
        buildParameters.put("path1", "first");
        buildParameters.put("path2", "second");

        String sub = URLParameter.substituteAll(parameterised, buildParameters);

        assertEquals("http://example.com/first/second", sub);
    }

    /**
     * Tests a parameterless string is not modified.
     */
    @Test
    public void shouldNotModifyUrlWithNoParameters() {
        String unparameterised = "http://example.com/some/path/without/parameters";

        HashMap<String, String> buildParameters = new HashMap<String, String>();
        buildParameters.put("param1", "first");
        buildParameters.put("param2", "second");

        String sub = URLParameter.substituteAll(unparameterised, buildParameters);
        assertEquals(unparameterised, sub);
    }
}
