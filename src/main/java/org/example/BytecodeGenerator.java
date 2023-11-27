package org.example;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;

import java.io.File;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class BytecodeGenerator {

    public void generateBytecode(String name, File outFile) {
        ClassWriter cl = new ClassWriter(outFile);
        try (ClassCreator creator = ClassCreator.builder()
                                                .classOutput(cl)
                                                .className(name)
                                                .build()) {

            MethodCreator main = creator.getMethodCreator("main", void.class, String[].class);
            main.setModifiers(ACC_PUBLIC + ACC_STATIC);

            Gizmo.systemOutPrintln(main, main.load("Hello World"));
            main.returnVoid();
        }
    }
}