package de.matthiasmann.continuations.instrument;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

import java.io.*;

/**
 * @author Denis Buzdalov
 */
public class InstrumentationHelper {
    private InstrumentationHelper() {}

    public static void instrumentClass(
            final MethodDatabase db,
            final File f,
            final boolean check,
            final boolean writeClasses) throws IOException {
        db.log(LogLevel.INFO, "Instrumenting class %s", f);

        ClassReader r;

        FileInputStream fis = new FileInputStream(f);
        try {
            r = new ClassReader(fis);
        } finally {
            fis.close();
        }

        ClassWriter cw = new DBClassWriter(db, r);
        ClassVisitor cv = check ? new CheckClassAdapter(cw) : cw;
        InstrumentClass ic = new InstrumentClass(cv, db, false);
        r.accept(ic, ClassReader.SKIP_FRAMES);

        byte[] newClass = cw.toByteArray();

        if(writeClasses) {
            FileOutputStream fos = new FileOutputStream(f);
            try {
                fos.write(newClass);
            } finally {
                fos.close();
            }
        }
    }
}
