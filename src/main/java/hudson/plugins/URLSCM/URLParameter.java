package hudson.plugins.URLSCM;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Represents a parameter in a URL
 */
public class URLParameter {

    private static String PARAMETER_NAME_REGEX = "[\\w.-]+";
    private static Pattern PARAMETER_DEFINITION_PATTERN = Pattern.compile("\\$\\{(" + PARAMETER_NAME_REGEX + ")\\}");
    private final String parameterName;

    /**
     * Creates a parameter with the given name.
     * @param parameterName The name of the parameter.
     * @exception IllegalArgumentException If parameterName contains any illegal characters
     */
    public URLParameter(String parameterName) {
        if(Pattern.matches("^" + PARAMETER_NAME_REGEX + "$", parameterName)) {
            this.parameterName = parameterName;
        }
        else throw new IllegalArgumentException(parameterName + " contains invalid characters");
    }

    /**
     * Substitutes this parameter into a parameterised url based on its build parameter value.
     * @param url The url to substitute this parameter into
     * @param buildParameters The current collection of build parameters
     * @return The url with the current parameter substituted with the value specified in buildParameters. If
     * the this parameter does not exist in the collection of build parameters, the url will not be modified.
     */
    public String substitute(final String url, final Map<String, String> buildParameters) {
        String marker = "${" + this.parameterName + "}";
        String value = buildParameters.get(this.parameterName);

        return value == null ? url : url.replace(marker, value);
    }

    /**
     * Whether this instance is equal to another object.
     * @param other The object to compare to this parameter.
     * @return True if other is a URLParameter with the same name.
     */
    @Override public boolean equals(Object other) {
        return other instanceof URLParameter && ((URLParameter)other).parameterName.equals(this.parameterName);
    }

    /**
     * Gets the hash code for this parameter.
     * @return
     */
    @Override public int hashCode() {
        return this.parameterName.hashCode();
    }

    /**
     * Finds all parameters in a URL.
     * @param url The URL to search.
     * @return A collection of all the parameters found in the input string.
     */
    public static Set<URLParameter> getParameters(final String url) {
        HashSet<URLParameter> parameters = new HashSet<URLParameter>();

        if(url == null) return parameters;

        Matcher matcher = PARAMETER_DEFINITION_PATTERN.matcher(url);
        while(matcher.find()) {
            parameters.add(new URLParameter(matcher.group(1)));
        }

        return parameters;
    }

    /**
     * Substitutes all parameters in a url based on their current values for the build.
     * @param url The URL to substitute parameters into.
     * @param buildParameters The current collection of build parameters.
     * @return
     */
    public static String substituteAll(String url, Map<String, String> buildParameters) {
        Collection<URLParameter> parameters = getParameters(url);

        for(URLParameter param : parameters) {
            url = param.substitute(url, buildParameters);
        }

        return url;
    }
}
