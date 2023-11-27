package org.example;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class BytecodeGenerator implements Opcodes {
    public byte[] generateBytecode() {

        ClassWriter cw = new ClassWriter(0);

        cw.visitEnd();

        return cw.toByteArray();
    }
}