package monkeylord.XServer.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DexHelper {
    public static String[] getClassesInDex(ClassLoader CL) {
        String[] result = {};
        try {
            Field field3 = CL.getClass().getSuperclass().getDeclaredField("pathList");
            field3.setAccessible(true);
            Object pathList = field3.get(CL);
            Field field2 = pathList.getClass().getDeclaredField("dexElements");
            field2.setAccessible(true);
            Object elements = field2.get(pathList);
            for (int i = 0; i < Array.getLength(elements); i++) {
                Object element = Array.get(elements, i);
                Field field1 = element.getClass().getDeclaredField("dexFile");
                field1.setAccessible(true);
                Object DexFile = field1.get(element);
                //XposedBridge.log(DexFile.getClass().getName());
                for (Method m : DexFile.getClass().getDeclaredMethods()) {
                    // XposedBridge.log("DEXFILE:"+m.getName());
                    if (m.getName().equalsIgnoreCase("getClassNameList")) {
                        m.setAccessible(true);
                        Field field = DexFile.getClass().getDeclaredField("mCookie");
                        field.setAccessible(true);
                        Object clist = m.invoke(DexFile,
                                field.get(DexFile));
                        int length1 = result.length;
                        int length2 = ((String[]) clist).length;
                        int totalLength = length1 + length2;
                        String[] totalArr = new String[totalLength];
                        for (int i1 = 0; i1 < length1; i1++) {
                            totalArr[i1] = result[i1];
                        }
                        for (int i1 = 0; i1 < length2; i1++) {
                            totalArr[i1 + length1] = ((String[]) clist)[i1];
                        }
                        result = totalArr;
                        //return (String[]) clist;
                    }
                }
            }
        } catch (Exception e) {

        } finally {
            return result;
        }
    }

}