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
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Strategy to load Hjson files into a {@link PropertySource}.
 *
 * @author <a href="mailto:ladutsko@gmail.com">George Ladutsko</a>
 */
public class HjsonPropertySourceLoader implements PropertySourceLoader {

    /**
     * Returns the file extensions that the loader supports (excluding the '.').
     * @return the file extensions
     */
    @Override
    public String[] getFileExtensions() {
        return new String[] { "hjson" };
    }

    /**
     * Load the resource into one or more property sources. Implementations may either
     * return a list containing a single source, or in the case of a multi-document format
     * such as yaml a source for each document in the resource.
     * @param name the root name of the property source. If multiple documents are loaded
     * an additional suffix should be added to the name for each source loaded.
     * @param resource the resource to load
     * @return a list property sources
     * @throws IOException if the source cannot be loaded
     */
    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            Map<String, Object> result = new LinkedHashMap<>();
            buildFlattenedMap(result, JsonValue.readHjson(reader), null);
            if (result.isEmpty()) {
                return emptyList();
            }

            return singletonList(new MapPropertySource(name, result));
        }
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
