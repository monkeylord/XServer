/*
 * Copyright (c) 2017 Lukas Morawietz
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

import java.io.File;
import java.io.IOException;

import dalvik.system.PathClassLoader;

/**
 * @author F43nd1r
 * @since 24.10.2017
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
class FileAndroidClassLoader extends BaseAndroidClassLoader {
    private static int instanceCounter = 0;
    private final File dexFile;

    /**
     * Create a new instance with the given parent classloader
     *
     * @param parent the parent
     */
    public FileAndroidClassLoader(ClassLoader parent, File cacheDir) {
        super(parent);
        int id = instanceCounter++;
        dexFile = new File(cacheDir, id + ".dex");
        cacheDir.mkdirs();
        reset();
    }

    @Override
    protected Class<?> loadClass(@NonNull Dex dex, @NonNull String name) throws ClassNotFoundException {
        try {
            dex.writeTo(dexFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new PathClassLoader(dexFile.getPath(), getParent()).loadClass(name);
    }

    @Nullable
    @Override
    protected Dex getLastDex() {
        if (dexFile.exists()) {
            try {
                return new Dex(dexFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void reset() {
        dexFile.delete();
    }
}
