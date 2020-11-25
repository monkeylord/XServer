/*
 * Copyright (c) 2016 Lukas Morawietz
 *
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
 */

package cn.vove7.rhino.common;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.dex.Dex;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.DexFile;
import com.android.dx.merge.CollisionPolicy;
import com.android.dx.merge.DexMerger;

import org.mozilla.javascript.GeneratedClassLoader;

import java.io.IOException;

/**
 * Compiles java bytecode to dex bytecode and loads it
 *
 * @author F43nd1r
 * @since 11.01.2016
 */
abstract class BaseAndroidClassLoader extends ClassLoader implements GeneratedClassLoader {

    /**
     * Create a new instance with the given parent classloader
     *
     * @param parent the parent
     */
    public BaseAndroidClassLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> defineClass(String name, byte[] data) {
        try {
            DexOptions dexOptions = new DexOptions();
            DexFile dexFile = new DexFile(dexOptions);
            DirectClassFile classFile = new DirectClassFile(data, name.replace('.', '/') + ".class", true);
            classFile.setAttributeFactory(StdAttributeFactory.THE_ONE);
            classFile.getMagic();
            dexFile.add(CfTranslator.translate(classFile, null, new CfOptions(), dexOptions, dexFile));
            Dex dex = new Dex(dexFile.toDex(null, false));
            Dex oldDex = getLastDex();
            if (oldDex != null) {
                dex = new DexMerger(new Dex[]{dex, oldDex}, CollisionPolicy.KEEP_FIRST).merge();
            }
            return loadClass(dex, name);
        } catch (IOException | ClassNotFoundException e) {
            throw new FatalLoadingException(e);
        }
    }

    protected abstract Class<?> loadClass(@NonNull Dex dex, @NonNull String name) throws ClassNotFoundException;

    @Nullable
    protected abstract Dex getLastDex();

    protected abstract void reset();

    /**
     * Does nothing
     *
     * @param aClass ignored
     */
    @Override
    public void linkClass(Class<?> aClass) {
        //doesn't make sense on android
    }

    /**
     * Try to load a class. This will search all defined classes, all loaded jars and the parent class loader.
     *
     * @param name    the name of the class to load
     * @param resolve ignored
     * @return the class
     * @throws ClassNotFoundException if the class could not be found in any of the locations
     */
    @Override
    public Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass == null) {
            Dex dex = getLastDex();
            if (dex != null) {
                loadedClass = loadClass(dex, name);
            }
            if (loadedClass == null) {
                loadedClass = getParent().loadClass(name);
            }
        }
        return loadedClass;
    }

    /**
     * Might be thrown in any Rhino method that loads bytecode if the loading failed
     */
    public static class FatalLoadingException extends RuntimeException {
        FatalLoadingException(Throwable t) {
            super("Failed to define class", t);
        }
    }
}
