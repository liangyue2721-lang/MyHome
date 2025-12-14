package com.make.quartz.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体比较器：支持深度比较对象字段值，包括继承字段和数组类型字段，并优化性能。
 */
public class EntityComparator {

    // 缓存类及其所有字段（包括父类），避免重复反射获取
    private static final Map<Class<?>, List<Field>> CLASS_FIELDS_CACHE = new ConcurrentHashMap<>();

    /**
     * 比较两个对象实例是否相等。
     *
     * @param obj1 第一个对象，可为 null
     * @param obj2 第二个对象，可为 null
     * @return 若所有字段值相等返回 true，否则返回 false
     * @throws RuntimeException 如果反射访问字段时发生异常
     */
    public static boolean areInstancesEqual(Object obj1, Object obj2) {
        // 1. 处理相同引用或双 null 情况
        if (obj1 == obj2) {
            return true;
        }

        // 2. 处理单一 null 情况
        if (obj1 == null || obj2 == null) {
            return false;
        }

        // 3. 确保 obj2 兼容 obj1 的类型（子类可与父类比较）
        if (!obj1.getClass().isAssignableFrom(obj2.getClass()) &&
                !obj2.getClass().isAssignableFrom(obj1.getClass())) {
            return false;
        }

        // 4. 获取类所有字段（含父类），使用缓存优化
        List<Field> fields = CLASS_FIELDS_CACHE.computeIfAbsent(obj1.getClass(), clazz -> {
            List<Field> fieldList = new ArrayList<>();
            Class<?> currentClass = clazz;
            while (currentClass != null && currentClass != Object.class) {
                for (Field field : currentClass.getDeclaredFields()) {
                    field.setAccessible(true); // 提前设置可访问
                    fieldList.add(field);
                }
                currentClass = currentClass.getSuperclass();
            }
            return Collections.unmodifiableList(fieldList); // 返回不可变列表
        });

        // 5. 遍历所有字段进行深度比较
        try {
            for (Field field : fields) {
                if (!compareField(field, obj1, obj2)) {
                    return false; // 任意字段不等则立即返回
                }
            }
            return true; // 所有字段均相等
        } catch (IllegalAccessException e) {
            throw new RuntimeException("字段访问异常: " + e.getMessage(), e);
        }
    }

    /**
     * 比较单个字段的值是否相等。
     */
    private static boolean compareField(Field field, Object obj1, Object obj2) throws IllegalAccessException {
        Object value1 = field.get(obj1);
        Object value2 = field.get(obj2);

        // 1. 双 null 视为相等
        if (value1 == null && value2 == null) {
            return true;
        }

        // 2. 单一 null 视为不等
        if (value1 == null || value2 == null) {
            return false;
        }

        // 3. 处理数组类型
        if (value1.getClass().isArray() && value2.getClass().isArray()) {
            return compareArrays(value1, value2);
        }

        // 4. 非数组类型使用 equals 比较
        return value1.equals(value2);
    }

    /**
     * 深度比较两个数组的值是否相等。
     */
    private static boolean compareArrays(Object array1, Object array2) {
        if (array1 instanceof byte[] && array2 instanceof byte[]) {
            return Arrays.equals((byte[]) array1, (byte[]) array2);
        } else if (array1 instanceof short[] && array2 instanceof short[]) {
            return Arrays.equals((short[]) array1, (short[]) array2);
        } else if (array1 instanceof int[] && array2 instanceof int[]) {
            return Arrays.equals((int[]) array1, (int[]) array2);
        } else if (array1 instanceof long[] && array2 instanceof long[]) {
            return Arrays.equals((long[]) array1, (long[]) array2);
        } else if (array1 instanceof float[] && array2 instanceof float[]) {
            return Arrays.equals((float[]) array1, (float[]) array2);
        } else if (array1 instanceof double[] && array2 instanceof double[]) {
            return Arrays.equals((double[]) array1, (double[]) array2);
        } else if (array1 instanceof char[] && array2 instanceof char[]) {
            return Arrays.equals((char[]) array1, (char[]) array2);
        } else if (array1 instanceof boolean[] && array2 instanceof boolean[]) {
            return Arrays.equals((boolean[]) array1, (boolean[]) array2);
        }

        // 处理对象数组（含多维数组）
        return Arrays.deepEquals((Object[]) array1, (Object[]) array2);
    }

    /**
     * 生成对象的哈希码，确保相等对象有相同哈希值。
     */
    public static int generateHashCode(Object obj) {
        if (obj == null) {
            return 0;
        }

        List<Field> fields = CLASS_FIELDS_CACHE.computeIfAbsent(obj.getClass(), clazz -> {
            List<Field> fieldList = new ArrayList<>();
            Class<?> currentClass = clazz;
            while (currentClass != null && currentClass != Object.class) {
                for (Field field : currentClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    fieldList.add(field);
                }
                currentClass = currentClass.getSuperclass();
            }
            return Collections.unmodifiableList(fieldList);
        });

        int result = 1;
        try {
            for (Field field : fields) {
                Object value = field.get(obj);
                result = 31 * result + (value == null ? 0 : value.hashCode());
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("字段访问异常: " + e.getMessage(), e);
        }
        return result;
    }
}
