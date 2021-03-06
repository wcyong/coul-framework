
package com.coul.common.utils.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.coul.common.exception.IException;
import com.coul.common.exception.LogicException;
import com.coul.common.log.ILogger;
import com.coul.common.log.LoggerFactory;

/**
 * 反射工具类
 * 
 * @author chillming
 */
public class ReflectUtil {
    
    private static final String SETTER_PREFIX         = "set";
    
    private static final String GETTER_PREFIX         = "get";
    
    private static final String CGLIB_CLASS_SEPARATOR = "$$";
    
    private static ILogger      logger                = LoggerFactory.getLogger(ReflectUtil.class);
    
    /**
     * 调用Getter方法.
     */
    public static Object invokeGetter(Object obj, String propertyName) {
        String getterMethodName = GETTER_PREFIX + StringUtils.capitalize(propertyName);
        return invokeMethod(obj, getterMethodName, new Class[] {}, new Object[] {});
    }
    
    /**
     * 调用Setter方法, 仅匹配方法名。
     */
    public static void invokeSetter(Object obj, String propertyName, Object value) {
        String setterMethodName = SETTER_PREFIX + StringUtils.capitalize(propertyName);
        invokeMethodByName(obj, setterMethodName, new Object[] {value });
    }
    
    /**
     * 直接读取对象属性值, 无视private/protected修饰符, 不经过getter函数.
     */
    public static Object getFieldValue(final Object obj, final String fieldName) {
        Field field = getAccessibleField(obj, fieldName);
        
        if (field == null) {
            throw new IllegalArgumentException("Could not find field [" + fieldName
                + "] on target [" + obj + "]");
        }
        
        Object result = null;
        try {
            result = field.get(obj);
        } catch (IllegalAccessException e) {
            logger.error("不可能抛出的异常{}", e.getMessage());
        }
        return result;
    }
    
    /**
     * 直接设置对象属性值, 无视private/protected修饰符, 不经过setter函数.
     */
    public static void setFieldValue(final Object obj, final String fieldName, final Object value) {
        Field field = getAccessibleField(obj, fieldName);
        
        if (field == null) {
            throw new IllegalArgumentException("Could not find field [" + fieldName
                + "] on target [" + obj + "]");
        }
        
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            logger.error("不可能抛出的异常:{}", e.getMessage());
        }
    }
    
    /**
     * 直接调用对象方法, 无视private/protected修饰符.
     * 用于一次性调用的情况，否则应使用getAccessibleMethod()函数获得Method后反复调用.
     * 同时匹配方法名+参数类型
     */
    public static Object invokeMethod(final Object obj, final String methodName,
        final Class<?>[] parameterTypes, final Object[] args) {
        Method method = getAccessibleMethod(obj, methodName, parameterTypes);
        if (method == null) {
            throw new IllegalArgumentException("Could not find method [" + methodName
                + "] on target [" + obj + "]");
        }
        
        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            throw convertReflectionExceptionToUnchecked(e);
        }
    }
    
    /**
     * 直接调用对象方法, 无视private/protected修饰符，
     * 用于一次性调用的情况，否则应使用getAccessibleMethodByName()函数获得Method后反复调用.
     * 只匹配函数名，如果有多个同名函数调用第一个。
     */
    public static Object invokeMethodByName(final Object obj, final String methodName,
        final Object[] args) {
        Method method = getAccessibleMethodByName(obj, methodName);
        if (method == null) {
            throw new IllegalArgumentException("Could not find method [" + methodName
                + "] on target [" + obj + "]");
        }
        
        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            throw convertReflectionExceptionToUnchecked(e);
        }
    }
    
    /**
     * 循环向上转型, 获取对象的DeclaredField, 并强制设置为可访问.
     * 
     * 如向上转型到Object仍无法找到, 返回null.
     */
    public static Field getAccessibleField(final Object obj, final String fieldName) {
        Validate.notNull(obj, "object can't be null");
        Validate.notBlank(fieldName, "fieldName can't be blank");
        for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass
            .getSuperclass()) {
            try {
                Field field = superClass.getDeclaredField(fieldName);
                makeAccessible(field);
                return field;
            } catch (NoSuchFieldException e) {// NOSONAR
                // Field不在当前类定义,继续向上转型
            }
        }
        return null;
    }
    
    /**
     * 循环向上转型, 获取对象的DeclaredMethod,并强制设置为可访问.
     * 如向上转型到Object仍无法找到, 返回null.
     * 匹配函数名+参数类型。
     * 
     * 用于方法需要被多次调用的情况. 先使用本函数先取得Method,然后调用Method.invoke(Object obj, Object... args)
     */
    public static Method getAccessibleMethod(final Object obj, final String methodName,
        final Class<?>... parameterTypes) {
        Validate.notNull(obj, "object can't be null");
        Validate.notBlank(methodName, "methodName can't be blank");
        
        for (Class<?> searchType = obj.getClass(); searchType != Object.class; searchType = searchType
            .getSuperclass()) {
            try {
                Method method = searchType.getDeclaredMethod(methodName, parameterTypes);
                makeAccessible(method);
                return method;
            } catch (NoSuchMethodException e) {
                // Method不在当前类定义,继续向上转型
            }
        }
        return null;
    }
    
    /**
     * 循环向上转型, 获取对象的DeclaredMethod,并强制设置为可访问.
     * 如向上转型到Object仍无法找到, 返回null.
     * 只匹配函数名。
     * 
     * 用于方法需要被多次调用的情况. 先使用本函数先取得Method,然后调用Method.invoke(Object obj, Object... args)
     */
    public static Method getAccessibleMethodByName(final Object obj, final String methodName) {
        Validate.notNull(obj, "object can't be null");
        Validate.notBlank(methodName, "methodName can't be blank");
        
        for (Class<?> searchType = obj.getClass(); searchType != Object.class; searchType = searchType
            .getSuperclass()) {
            Method[] methods = searchType.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    makeAccessible(method);
                    return method;
                }
            }
        }
        return null;
    }
    
    /**
     * 改变private/protected的方法为public，尽量不调用实际改动的语句，避免JDK的SecurityManager抱怨。
     */
    public static void makeAccessible(Method method) {
        if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method
            .getDeclaringClass().getModifiers())) && !method.isAccessible()) {
            method.setAccessible(true);
        }
    }
    
    /**
     * 改变private/protected的成员变量为public，尽量不调用实际改动的语句，避免JDK的SecurityManager抱怨。
     */
    public static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers())
            || !Modifier.isPublic(field.getDeclaringClass().getModifiers()) || Modifier
                .isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }
    
    /**
     * 通过反射, 获得Class定义中声明的泛型参数的类型, 注意泛型必须定义在父类处
     * 如无法找到, 返回Object.class.
     * eg.
     * public UserDao extends HibernateDao<User>
     * 
     * @param clazz The class to introspect
     * @return the first generic declaration, or Object.class if cannot be determined
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    public static <T> Class<T> getClassGenricType(final Class clazz) {
        return getClassGenricType(clazz, 0);
    }
    
    /**
     * 通过反射, 获得Class定义中声明的父类的泛型参数的类型.
     * 如无法找到, 返回Object.class.
     * 
     * 如public UserDao extends HibernateDao<User,Long>
     * 
     * @param clazz clazz The class to introspect
     * @param index the Index of the generic ddeclaration,start from 0.
     * @return the index generic declaration, or Object.class if cannot be determined
     */
    @SuppressWarnings("rawtypes")
    public static Class getClassGenricType(final Class clazz, final int index) {
        
        Type genType = clazz.getGenericSuperclass();
        
        if (!(genType instanceof ParameterizedType)) {
            logger.warn(clazz.getSimpleName() + "'s superclass not ParameterizedType");
            return Object.class;
        }
        
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        
        if ((index >= params.length) || (index < 0)) {
            logger.warn("Index: " + index + ", Size of " + clazz.getSimpleName()
                + "'s Parameterized Type: " + params.length);
            return Object.class;
        }
        if (!(params[index] instanceof Class)) {
            logger.warn(clazz.getSimpleName()
                + " not set the actual class on superclass generic parameter");
            return Object.class;
        }
        
        return (Class) params[index];
    }
    
    public static Class<?> getUserClass(Object instance) {
        Validate.notNull(instance, "Instance must not be null");
        Class<?> clazz = instance.getClass();
        if ((clazz != null) && clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
            Class<?> superClass = clazz.getSuperclass();
            if ((superClass != null) && !Object.class.equals(superClass)) {
                return superClass;
            }
        }
        return clazz;
        
    }
    
    /**
     * 将反射时的checked exception转换为unchecked exception.
     */
    public static RuntimeException convertReflectionExceptionToUnchecked(Exception e) {
        if ((e instanceof IllegalAccessException) || (e instanceof IllegalArgumentException)
            || (e instanceof NoSuchMethodException)) {
            return new IllegalArgumentException(e);
        } else if (e instanceof InvocationTargetException) {
            return new RuntimeException(((InvocationTargetException) e).getTargetException());
        } else if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException("Unexpected Checked Exception.", e);
    }
    
    /**
     * 实例化类对象
     * 
     * @param className
     *            类名
     * @return 实例对象
     */
    public static final Object newInstance(String className) {
        return newInstance(findClass(className));
    }
    
    /**
     * 实例化类对象
     * 
     * @param aClass
     *            java类
     * @return 实例对象
     */
    public static final Object newInstance(Class<?> aClass) {
        try {
            return aClass.newInstance();
        } catch (InstantiationException e) {
            // 实例化异常
            throw new LogicException(IException.CLASS_INSTANCE_ERROR, "实例化异常", e).addParam(
                "className", aClass.getName());
        } catch (IllegalAccessException e) {
            // 实例化异常
            throw new LogicException(IException.CLASS_INSTANCE_ERROR, "实例化异常", e).addParam(
                "className", aClass.getName());
        }
    }
    
    /**
     * 根据类名（string）找java类
     * 
     * @param className
     *            类名（string）
     * @return java类
     */
    public static final Class<?> findClass(String className) {
        try {
            return Class.forName(className.trim());
        } catch (ClassNotFoundException e) {
            throw new LogicException(IException.CLASS_NOTFOUND, "类名不存在", e).addParam("className",
                className);
        }
    }
    
    /**
     * @param className
     *            如果是String类型，则由此方法进行实例化（要求传入全路径的类名称） 否则传入实例化的类对象
     * @param method
     *            方法名称
     * @param methodParams
     *            方法的参数数组，如果没有参数则填 null
     * @return 方法返回的结果
     * @throws Exception
     */
    public static Object invokeClassMethod(Object className, String methodName,
        Object[] methodParams) throws Exception {
        Object ref = null;
        if (className instanceof String) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> myClass = cl.loadClass(className.toString());
            ref = myClass.newInstance();
        } else {
            ref = className;
        }
        
        Object result = null;
        @SuppressWarnings("rawtypes")
        Class[] arrClass = null;
        if (methodParams != null) {
            int len = methodParams.length;
            arrClass = new Class[len];
            for (int i = 0; i < len; i++) {
                arrClass[i] = methodParams[i].getClass();
            }
        }
        Method method = ref.getClass().getMethod(methodName, arrClass);
        result = method.invoke(ref, methodParams);
        return result;
    }
}
