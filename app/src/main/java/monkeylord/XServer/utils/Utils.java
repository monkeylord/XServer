package monkeylord.XServer.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static java.lang.reflect.Modifier.STATIC;

public class Utils {
    private static final Map<Class<?>, String> PRIMITIVE_TO_SIGNATURE;

    static {
        PRIMITIVE_TO_SIGNATURE = new HashMap<Class<?>, String>(9);
        PRIMITIVE_TO_SIGNATURE.put(byte.class, "B");
        PRIMITIVE_TO_SIGNATURE.put(char.class, "C");
        PRIMITIVE_TO_SIGNATURE.put(short.class, "S");
        PRIMITIVE_TO_SIGNATURE.put(int.class, "I");
        PRIMITIVE_TO_SIGNATURE.put(long.class, "J");
        PRIMITIVE_TO_SIGNATURE.put(float.class, "F");
        PRIMITIVE_TO_SIGNATURE.put(double.class, "D");
        PRIMITIVE_TO_SIGNATURE.put(void.class, "V");
        PRIMITIVE_TO_SIGNATURE.put(boolean.class, "Z");
    }

    public static String MethodDescription(Method m) {
        StringBuilder sb = new StringBuilder();
        sb.append(Modifier.toString(m.getModifiers()));
        sb.append(" ");
        sb.append(m.getReturnType().getName());
        sb.append(" ");
        sb.append(m.getName());
        sb.append("(");
        for (int i = 0; i < m.getParameterTypes().length; i++) {
            if (i != 0) sb.append(",");
            sb.append(m.getParameterTypes()[i].getName());
            sb.append(" param" + i);
        }
        sb.append(")");
        if (m.getExceptionTypes().length > 0) {
            sb.append("throws ");
            boolean first = true;
            for (Class<?> type : m.getExceptionTypes()) {
                if (!first) sb.append(",");
                else first = false;
                sb.append(type.getName());
            }
        }
        return sb.toString();
    }

    public static String FieldDescription(Field field) {
        String s = Modifier.toString(field.getModifiers()) + " " + field.getType() + " " + field.getName();

        if ((field.getModifiers() & STATIC) != 0) {
            try {
                field.setAccessible(true);
                s += " :: " + field.get(null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return s;
    }

    public static String getJavaName(Method method) {
        StringBuilder result = new StringBuilder();
        result.append(getTypeSignature(method.getDeclaringClass()));
        result.append("->");
        result.append(method.getName());
        result.append(getMethodSignature(method));
        return result.toString();
    }

    public static String getMethodSignature(Method method) {
        StringBuilder result = new StringBuilder();

        result.append('(');
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (Class<?> parameterType : parameterTypes) {
            result.append(getTypeSignature(parameterType));
        }
        result.append(')');
        result.append(getTypeSignature(method.getReturnType()));

        return result.toString();
    }

    public static String getTypeSignature(Class<?> clazz) {
        String primitiveSignature = PRIMITIVE_TO_SIGNATURE.get(clazz);
        if (primitiveSignature != null) {
            return primitiveSignature;
        } else if (clazz.isArray()) {
            return "[" + getTypeSignature(clazz.getComponentType());
        } else {
            // TODO: this separates packages with '.' rather than '/'
            return "L" + clazz.getName() + ";";
        }
    }
}
