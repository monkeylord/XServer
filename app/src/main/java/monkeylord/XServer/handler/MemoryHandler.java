package monkeylord.XServer.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class MemoryHandler {
    static HashMap<String, Method> Memory = new HashMap<>();
    static {
        try {
            // http://androidxref.com/4.4_r1/xref/libcore/luni/src/main/java/libcore/io/Memory.java
            Class clzMemory = Class.forName("libcore.io.Memory");
            for (Method m:clzMemory.getDeclaredMethods()) {
                Memory.put(m.getName(),m);
            }
        } catch (ClassNotFoundException e) {
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
