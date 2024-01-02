package org.phone2webcam.custom;

import java.io.*;

public class CustomObjectInputStream extends ObjectInputStream {
    private String classPath;

    public CustomObjectInputStream(InputStream in, String classPath) throws IOException {
        super(in);
        this.classPath = classPath;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        try {
            return Class.forName(this.classPath);
        } catch (ClassNotFoundException e) {
            return super.resolveClass(desc);
        }
    }
}