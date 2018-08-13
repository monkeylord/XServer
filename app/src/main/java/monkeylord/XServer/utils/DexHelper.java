package monkeylord.XServer.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

public class DexHelper {
    public static String[] getClassesInDex(ClassLoader CL) {
        String[] result = {};
        try {
            Object pathList = getField(CL.getClass().getSuperclass(), "pathList", CL);
            Object elements = getField(pathList.getClass(), "dexElements", pathList);
            for (int i = 0; i < Array.getLength(elements); i++) {
                Object element = Array.get(elements, i);
                Object DexFile = getField(element.getClass(), "dexFile", element);
                XposedBridge.log(DexFile.getClass().getName());
                for (Method m : DexFile.getClass().getDeclaredMethods()) {
                    // XposedBridge.log("DEXFILE:"+m.getName());
                    if (m.getName().equalsIgnoreCase("getClassNameList")) {
                        m.setAccessible(true);
                        Object clist = m.invoke(DexFile,
                                getField(DexFile.getClass(), "mCookie", DexFile));
                        result = mergeArray2(result, (String[]) clist);
                        //return (String[]) clist;
                    }
                }
            }
        } catch (Exception e) {

        } finally {
            return result;
        }
    }

    public static Object getField(Class<?> cl, String fieldName, Object object)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = cl.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }

    public static String[] mergeArray2(String[] arr1, String[] arr2) {
        int length1 = arr1.length;
        int length2 = arr2.length;
        int totalLength = length1 + length2;
        String[] totalArr = new String[totalLength];
        for (int i = 0; i < length1; i++) {
            totalArr[i] = arr1[i];
        }
        for (int i = 0; i < length2; i++) {
            totalArr[i + length1] = arr2[i];
        }
        return totalArr;
    }

}