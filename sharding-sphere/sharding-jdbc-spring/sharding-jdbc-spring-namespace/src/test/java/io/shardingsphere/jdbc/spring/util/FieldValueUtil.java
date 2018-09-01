/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.jdbc.spring.util;

import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FieldValueUtil {
    
    /**
     * Get field value.
     * 
     * @param obj obj
     * @param fieldName field name
     * @param isFromSuperClass is from super class
     * @return field value
     */
    public static Object getFieldValue(final Object obj, final String fieldName, final boolean isFromSuperClass) {
        if (null == obj || Strings.isNullOrEmpty(fieldName)) {
            return null;
        }
        Class<?> clazz = isFromSuperClass ? obj.getClass().getSuperclass() : obj.getClass();
        return getFieldValue(clazz, obj, fieldName);
    }
    
    private static Object getFieldValue(final Class<?> clazz, final Object obj, final String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return field.get(obj);
        } catch (final NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get field value.
     * 
     * @param obj obj
     * @param fieldName field name
     * @return field value
     */
    public static Object getFieldValue(final Object obj, final String fieldName) {
        return getFieldValue(obj, fieldName, false);
    }
}
