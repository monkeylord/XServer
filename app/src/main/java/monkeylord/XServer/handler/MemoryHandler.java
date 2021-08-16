package monkeylord.XServer.handler;

import android.system.ErrnoException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class MemoryHandler {
    static HashMap<String, Method> Memory = new HashMap<>();
    static HashMap<String, Method> OS = new HashMap<>();
    static Object osObject = null;
    static {
        try {
            // http://androidxref.com/4.4_r1/xref/libcore/luni/src/main/java/libcore/io/Memory.java
            Class clzMemory = Class.forName("libcore.io.Memory");
            for (Method m:clzMemory.getDeclaredMethods()) {
                Memory.put(m.getName(),m);
            }
            // http://androidxref.com/9.0.0_r3/xref/libcore/luni/src/main/java/android/system/Os.java
            Class clzOS = null;
            try {
                clzOS = Class.forName("libcore.io.Linux");
            }catch (ClassNotFoundException e){
                // for Android 4.4
                clzOS = Class.forName("libcore.io.Posix");
            }
            Constructor osConstructor = clzOS.getDeclaredConstructor();
            osConstructor.setAccessible(true);
            osObject = osConstructor.newInstance();
            for (Method m:clzOS.getDeclaredMethods()) {
                OS.put(m.getName(),m);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    public static byte[] readMemory(long address,int count) throws InvocationTargetException, IllegalAccessException {
        if(Memory.get("peekByteArray")==null)throw new IllegalAccessException();
        byte[] data=new byte[count];
        Memory.get("peekByteArray").invoke(null,address, data, 0, count);
        return data;
    }
    public static void writeMemory(long address,byte[] data) throws IllegalAccessException, InvocationTargetException {
        if(Memory.get("pokeByteArray")==null)throw new IllegalAccessException();
        byte[] mem = readMemory(address,data.length);
        for (int i = 0; i < mem.length; i++) {
            if(mem[i]!=data[i]){
                Memory.get("pokeByte").invoke(null,address+i,data[i]);
            }
        }
        //Memory.get("pokeByteArray").invoke(null,address, mem, 0, mem.length);
    }

    public static long mmap(long address, long byteCount, int prot, int flags, FileDescriptor fd, long offset) throws IllegalAccessException, InvocationTargetException {
        if(OS.get("mmap")==null)throw new IllegalAccessException();
        return (long)OS.get("mmap").invoke(osObject,address,byteCount,prot,flags,fd,offset);
    }
    public static void munmap(long address, long byteCount) throws IllegalAccessException, InvocationTargetException {
        if(OS.get("munmap")==null)throw new IllegalAccessException();
        OS.get("munmap").invoke(osObject,address,byteCount);
    }

    public static String[] getMaps(){
        File maps=new File("/proc/self/maps");
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream is = new FileInputStream(maps);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String str;
            while((str=reader.readLine())!=null){
                sb.append(str+"\r\n");
            }
            /*
            Process p = Runtime.getRuntime().exec("cat /proc/self/maps");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String str;
            while((str=reader.readLine())!=null){
                sb.append(str+"\r\n");
            }
            */
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] mapstrs=sb.toString().split("\r\n");
        for (int i = 0; i < mapstrs.length; i++) {
            String[] addrs =mapstrs[i].substring(0,17).split("-");
            long size = Long.parseLong(addrs[1],16)-Long.parseLong(addrs[0],16);
            mapstrs[i]=mapstrs[i].replaceFirst(" ",":"+Long.toHexString(size)+" ");
        }
        return mapstrs;
    }

}
