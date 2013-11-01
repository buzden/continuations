package de.matthiasmann.continuations.instrument;

import com.sun.istack.internal.*;

import java.io.*;
import java.util.*;

/**
 * @author Denis Buzdalov
 */
public class FileInstrumenter {
    protected boolean check = false;
    protected boolean writeClasses = true;

    protected boolean debug = false;
    protected boolean verbose = false;
    protected boolean allowMonitors = false;
    protected boolean allowBlocking = false;

    public void instrument(final File target) throws IOException, UnableToInstrumentException {
        MethodDatabase db = new MethodDatabase(getClass().getClassLoader());

        db.setVerbose(verbose);
        db.setDebug(debug);
        db.setAllowMonitors(allowMonitors);
        db.setAllowBlocking(allowBlocking);
        db.setLog(new Log() {
            public void log(final LogLevel level, final String msg, final Object... args) {
                logMessage(level, msg, args);
            }

            public void error(String msg, Exception ex) {
                logError(msg, ex);
            }
        });

        for (final File file : getAllFiles(target)) {
            if (file.getName().endsWith(".class")) {
                db.checkClass(file);
            }
        }

        db.log(LogLevel.INFO, "Instrumenting " + db.getWorkList().size() + " classes");

        for (final File f : db.getWorkList()) {
            InstrumentationHelper.instrumentClass(db, f, check, writeClasses);
        }
    }

    protected static Iterable<File> getAllFiles(final File file) {
        if (file.isFile()) {
            return Collections.singleton(file);
        } else if (file.isDirectory()) {
            return new Iterable<File>() {
                @Override
                public Iterator<File> iterator() {
                    return new Iterator<File>() {
                        //@ invariant nestedFiles != null;
                        final @NotNull File[] nestedFiles = file.listFiles();

                        //@ invariant 0 <= currIndex <= nestedFiles.length;
                        int currIndex = 0;

                        //@ invariant (nextFile == null || nextIterator == null);
                        File nextFile = null;
                        Iterator<File> nextIterator = null;

                        /*@ ensures
                                (!\result <==> nextFile == null && nextIterator == null) &&
                                (nextIterator != null ==> nextIterator.hasNext());
                            modifies nextFile, nextIterator when
                                \old(nextIterator == null || nextIterator == null || !nextIterator.hasNext());
                         */
                        @Override
                        public boolean hasNext() {
                            if ((nextFile != null) || (nextIterator != null) && (nextIterator.hasNext())) {
                                return true;
                            }

                            nextIterator = null;

                            while (true) {
                                //noinspection StatementWithEmptyBody
                                while (
                                    ++currIndex < nestedFiles.length &&
                                    !nestedFiles[currIndex].isDirectory() &&
                                    !nestedFiles[currIndex].isFile()
                                );

                                if (currIndex >= nestedFiles.length) {
                                    return false;
                                }

                                File currFile = nestedFiles[currIndex];
                                if (currFile.isFile()) {
                                    nextFile = currFile;
                                    return true;
                                } else if (currFile.isDirectory()) {
                                    nextIterator = getAllFiles(currFile).iterator();

                                    if (nextIterator.hasNext()) {
                                        return true;
                                    }
                                }
                            }
                        } // hasNext()

                        @Override
                        public File next() {
                            if (hasNext()) {
                                if (nextFile != null) {
                                    final File res = nextFile;
                                    nextFile = null;
                                    return res;
                                } else if (nextIterator != null) {
                                    return nextIterator.next();
                                } else {
                                    throw new IllegalStateException("Both next file and next iterator are null");
                                }
                            } else {
                                throw new IllegalStateException("No next element");
                            }
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        } else {
            return Collections.emptySet();
        }
    }

    protected void logMessage(final LogLevel logLevel, final String message, final Object... args) {
        System.out.println(logLevel + ": " + String.format(message, args));
    }

    protected void logError(final String message, final Exception ex) {
        System.out.println("ERROR: " + message);
        ex.printStackTrace();
    }
}
