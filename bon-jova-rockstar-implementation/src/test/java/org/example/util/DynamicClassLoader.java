package org.example.util;

import io.quarkus.gizmo.ClassOutput;

public class DynamicClassLoader extends ClassLoader implements ClassOutput {

    byte[] classDef = null;

    @Override
    public void write(String s, byte[] bytes) {
        this.classDef = bytes;
    }

    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        if (classDef != null) {
            return defineClass(name, classDef, 0, classDef.length);
        }
        return super.findClass(name);
    }
}