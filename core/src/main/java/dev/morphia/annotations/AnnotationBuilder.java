/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia.annotations;

import dev.morphia.mapping.MappingException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * @param <T>
 * @morphia.internal
 */
public abstract class AnnotationBuilder<T extends Annotation> implements Annotation {
    /**
     * Special name that can never be used. Used as default for some fields to indicate default state.
     *
     * @morphia.internal
     */
    public static final String DEFAULT = ".";
    private final Map<String, Object> values = new HashMap<String, Object>();

    protected AnnotationBuilder() {
        for (Method method : annotationType().getDeclaredMethods()) {
            values.put(method.getName(), method.getDefaultValue());
        }
    }

    protected AnnotationBuilder(T original) {
        try {
            for (Method method : annotationType().getDeclaredMethods()) {
                values.put(method.getName(), method.invoke(original));
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <V> V get(String key) {
        return (V) values.get(key);
    }

    protected void put(String key, Object value) {
        if (value != null) {
            values.put(key, value);
        }
    }

    void putAll(Map<String, Object> map) {
        values.putAll(map);
    }

    @Override
    public String toString() {
        return format("@%s %s", annotationType().getName(), values);
    }

    @Override
    public abstract Class<T> annotationType();

    @SuppressWarnings("unchecked")
    static <A extends Annotation> Map<String, Object> toMap(A annotation) {
        final Map<String, Object> map = new HashMap<String, Object>();
        try {
            Class<A> annotationType = (Class<A>) annotation.annotationType();
            for (Method method : annotationType.getDeclaredMethods()) {
                Object value = unwrapAnnotation(method.invoke(annotation));
                final Object defaultValue = unwrapAnnotation(method.getDefaultValue());
                if (value != null && !value.equals(defaultValue)) {
                    map.put(method.getName(), value);
                }
            }
        } catch (Exception e) {
            throw new MappingException(e.getMessage(), e);
        }
        return map;
    }

    private static Object unwrapAnnotation(Object o) {
        if (o instanceof Annotation) {
            return toMap((Annotation) o);
        } else {
            return o;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AnnotationBuilder)) {
            return false;
        }

        return values.equals(((AnnotationBuilder<?>) o).values);

    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
