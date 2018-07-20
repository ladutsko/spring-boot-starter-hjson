/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 George Ladutsko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.ladutsko.springframework.boot.env;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonObject.Member;
import org.hjson.JsonValue;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;

/**
 * Strategy to load Hjson files into a {@link PropertySource}.
 *
 * @author <a href="mailto:ladutsko@gmail.com">George Ladutsko</a>
 */
public class HjsonPropertySourceLoader implements PropertySourceLoader {

    /**
     * Returns the file extensions that the loader supports (excluding the '.').
     *
     * @return the file extensions
     */
    @Override
    public String[] getFileExtensions() {
        return new String[] { "hjson" };
    }

    /**
     * Load the resource into a property source.
     *
     * @param name     the name of the property source
     * @param resource the resource to load
     * @param profile  the name of the profile to load or {@code null}. The profile can be
     *                 used to load multi-document files (such as YAML). Simple property formats should
     *                 {@code null} when asked to load a profile.
     * @return a property source or {@code null}
     * @throws IOException if the source cannot be loaded
     */
    @Override
    public PropertySource<?> load(String name, Resource resource, String profile) throws IOException {
        if (null == profile) {
            try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
                Map<String, Object> result = new LinkedHashMap<>();
                buildFlattenedMap(result, JsonValue.readHjson(reader), null);
                if (!result.isEmpty()) {
                    return new MapPropertySource(name, result);
                }
            }
        }

        return null;
    }

    private void buildFlattenedMap(Map<String, Object> result, JsonValue hjson, String root) {
        switch (hjson.getType()) {
            case OBJECT:
                for (Member member : (JsonObject) hjson) {
                    String name = member.getName();
                    buildFlattenedMap(result, member.getValue(), null == root ? name : root + "." + name);
                }
                break;

            case ARRAY:
                int index = 0;
                for (JsonValue value : (JsonArray) hjson) {
                    buildFlattenedMap(result, value, root + "[" + (index++) + "]");
                }
                break;

            case STRING:
                result.put(root, hjson.asString());
                break;

            case NULL:
                result.put(root, "");
                break;

            default:
                result.put(root, hjson.toString());
        }
    }
}
