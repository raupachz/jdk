/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @bug 8262891
 * @summary Check exhaustiveness of switches over sealed types.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main Exhaustiveness
*/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import toolbox.TestRunner;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class Exhaustiveness extends TestRunner {

    private static final String JAVA_VERSION = System.getProperty("java.specification.version");

    ToolBox tb;

    public static void main(String... args) throws Exception {
        new Exhaustiveness().runTests();
    }

    Exhaustiveness() {
        super(System.err);
        tb = new ToolBox();
    }

    public void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    //TODO: interfaces/classes (abstract non-abstract)/exhaustive non-exhaustive/inaccessible permitted classes
    @Test
    public void testExhaustiveSealedClasses(Path base) throws Exception {
        doTest(base,
               new String[]{"""
                            package lib;
                            public sealed interface S permits A, B {}
                            """,
                            """
                            package lib;
                            public final class A implements S {}
                            """,
                            """
                            package lib;
                            public final class B implements S {}
                            """},
               """
               package test;
               import lib.*;
               public class Test {
                   private int test(S obj) {
                       return switch (obj) {
                           case A a -> 0;
                           case B b -> 1;
                       };
                   }
               }
               """);
    }

    @Test
    public void testNonExhaustiveSealedClasses(Path base) throws Exception {
        doTest(base,
               new String[]{"""
                            package lib;
                            public sealed interface S permits A, B {}
                            """,
                            """
                            package lib;
                            public final class A implements S {}
                            """,
                            """
                            package lib;
                            public final class B implements S {}
                            """},
               """
               package test;
               import lib.*;
               public class Test {
                   private int test(S obj) {
                       return switch (obj) {
                           case A a -> 0;
                       };
                   }
               }
               """,
               "Test.java:5:16: compiler.err.not.exhaustive",
               "- compiler.note.preview.filename: Test.java, DEFAULT",
               "- compiler.note.preview.recompile",
               "1 error");
    }

    @Test
    public void testAbstractSealedClasses(Path base) throws Exception {
        doTest(base,
               new String[]{"""
                            package lib;
                            public sealed abstract class S permits A, B {}
                            """,
                            """
                            package lib;
                            public final class A extends S {}
                            """,
                            """
                            package lib;
                            public final class B extends S {}
                            """},
               """
               package test;
               import lib.*;
               public class Test {
                   private int test(S obj) {
                       return switch (obj) {
                           case A a -> 0;
                           case B b -> 1;
                       };
                   }
               }
               """);
    }

    @Test
    public void testConcreteSealedClasses(Path base) throws Exception {
        doTest(base,
               new String[]{"""
                            package lib;
                            public sealed class S permits A, B {}
                            """,
                            """
                            package lib;
                            public final class A extends S {}
                            """,
                            """
                            package lib;
                            public final class B extends S {}
                            """},
               """
               package test;
               import lib.*;
               public class Test {
                   private int test(S obj) {
                       return switch (obj) {
                           case A a -> 0;
                           case B b -> 1;
                       };
                   }
               }
               """,
               "Test.java:5:16: compiler.err.not.exhaustive",
               "- compiler.note.preview.filename: Test.java, DEFAULT",
               "- compiler.note.preview.recompile",
               "1 error");
    }

    @Test
    public void testGuards1(Path base) throws Exception {
        doTest(base,
               new String[]{"""
                            package lib;
                            public sealed interface S permits A, B {}
                            """,
                            """
                            package lib;
                            public final class A implements S {}
                            """,
                            """
                            package lib;
                            public final class B implements S {}
                            """},
               """
               package test;
               import lib.*;
               public class Test {
                   private int test(S obj) {
                       return switch (obj) {
                           case A a && a.toString().isEmpty() -> 0;
                           case B b -> 1;
                       };
                   }
               }
               """,
               "Test.java:5:16: compiler.err.not.exhaustive",
               "- compiler.note.preview.filename: Test.java, DEFAULT",
               "- compiler.note.preview.recompile",
               "1 error");
    }

    @Test
    public void testGuards2(Path base) throws Exception {
        doTest(base,
               new String[]{"""
                            package lib;
                            public sealed interface S permits A, B {}
                            """,
                            """
                            package lib;
                            public final class A implements S {}
                            """,
                            """
                            package lib;
                            public final class B implements S {}
                            """},
               """
               package test;
               import lib.*;
               public class Test {
                   private static final boolean TEST = true;
                   private int test(S obj) {
                       return switch (obj) {
                           case A a && !(!(TEST)) -> 0;
                           case B b -> 1;
                       };
                   }
               }
               """);
    }

    @Test
    public void testGuards3(Path base) throws Exception {
        doTest(base,
               new String[]{"""
                            package lib;
                            public sealed interface S permits A, B {}
                            """,
                            """
                            package lib;
                            public final class A implements S {}
                            """,
                            """
                            package lib;
                            public final class B implements S {}
                            """},
               """
               package test;
               import lib.*;
               public class Test {
                   private int test(S obj) {
                       return switch (obj) {
                           case A a && false -> 0;
                           case B b -> 1;
                       };
                   }
               }
               """,
               "Test.java:5:16: compiler.err.not.exhaustive",
               "- compiler.note.preview.filename: Test.java, DEFAULT",
               "- compiler.note.preview.recompile",
               "1 error");
    }

    @Test
    public void testCoversType1(Path base) throws Exception {
        doTest(base,
               new String[]{"""
                            package lib;
                            public sealed interface S permits A, B {}
                            """,
                            """
                            package lib;
                            public final class A implements S {}
                            """,
                            """
                            package lib;
                            public final class B implements S {}
                            """},
               """
               package test;
               import lib.*;
               public class Test {
                   private int test(S obj) {
                       return switch (obj) {
                           case A a -> 0;
                           case S s -> 1;
                       };
                   }
               }
               """);
    }

    @Test
    public void testCoversType2(Path base) throws Exception {
        doTest(base,
               new String[]{"""
                            package lib;
                            public interface S {}
                            """,
                            """
                            package lib;
                            public final class A implements S {}
                            """,
                            """
                            package lib;
                            public final class B implements S {}
                            """},
               """
               package test;
               import lib.*;
               public class Test {
                   private int test(S obj) {
                       return switch (obj) {
                           case A a -> 0;
                           case S s -> 1;
                       };
                   }
               }
               """);
    }

    @Test
    public void testCoversType3(Path base) throws Exception {
        doTest(base,
               new String[]{"""
                            package lib;
                            public interface S<T> {}
                            """,
                            """
                            package lib;
                            public final class A implements S<A> {}
                            """,
                            """
                            package lib;
                            public final class B implements S<B> {}
                            """},
               """
               package test;
               import lib.*;
               public class Test {
                   private int test(S obj) {
                       return switch (obj) {
                           case A a -> 0;
                           case S<?> s -> 1;
                       };
                   }
               }
               """);
    }

    private void doTest(Path base, String[] libraryCode, String testCode, String... expectedErrors) throws IOException {
        Path current = base.resolve(".");
        Path libSrc = current.resolve("lib-src");
        for (String code : libraryCode) {
            tb.writeJavaFiles(libSrc, code);
        }

        Path libClasses = current.resolve("libClasses");

        Files.createDirectories(libClasses);

        new JavacTask(tb)
                .options("--enable-preview",
                         "-source", JAVA_VERSION)
                .outdir(libClasses)
                .files(tb.findJavaFiles(libSrc))
                .run();

        Path src = current.resolve("src");
        tb.writeJavaFiles(src, testCode);

        Path classes = current.resolve("libClasses");

        Files.createDirectories(libClasses);

        var log =
                new JavacTask(tb)
                    .options("--enable-preview",
                             "-source", JAVA_VERSION,
                             "-XDrawDiagnostics",
                             "-Xlint:-preview",
                             "--class-path", libClasses.toString())
                    .outdir(classes)
                    .files(tb.findJavaFiles(src))
                    .run(expectedErrors.length > 0 ? Task.Expect.FAIL : Task.Expect.SUCCESS)
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);
        if (expectedErrors.length > 0 && !List.of(expectedErrors).equals(log)) {
            throw new AssertionError("Incorrect errors, expected: " + List.of(expectedErrors) +
                                      ", actual: " + log);
        }
    }

    @Test
    public void testInaccessiblePermitted(Path base) throws IOException {
        Path current = base.resolve(".");
        Path libSrc = current.resolve("lib-src");

        tb.writeJavaFiles(libSrc,
                           """
                           package lib;
                           public sealed interface S permits A, B {}
                           """,
                           """
                           package lib;
                           public final class A implements S {}
                           """,
                           """
                           package lib;
                           final class B implements S {}
                           """);

        Path libClasses = current.resolve("libClasses");

        Files.createDirectories(libClasses);

        new JavacTask(tb)
                .options("--enable-preview",
                         "-source", JAVA_VERSION)
                .outdir(libClasses)
                .files(tb.findJavaFiles(libSrc))
                .run();

        Path src = current.resolve("src");
        tb.writeJavaFiles(src,
                           """
                           package test;
                           import lib.*;
                           public class Test {
                               private int test(S obj) {
                                   return switch (obj) {
                                       case A a -> 0;
                                   };
                               }
                           }
                           """);

        Path classes = current.resolve("libClasses");

        Files.createDirectories(libClasses);

        var log =
                new JavacTask(tb)
                    .options("--enable-preview",
                             "-source", JAVA_VERSION,
                             "-XDrawDiagnostics",
                             "-Xlint:-preview",
                             "--class-path", libClasses.toString())
                    .outdir(classes)
                    .files(tb.findJavaFiles(src))
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expectedErrors = List.of(
               "Test.java:5:16: compiler.err.not.exhaustive",
               "- compiler.note.preview.filename: Test.java, DEFAULT",
               "- compiler.note.preview.recompile",
               "1 error");

        if (!expectedErrors.equals(log)) {
            throw new AssertionError("Incorrect errors, expected: " + expectedErrors +
                                      ", actual: " + log);
        }

        Path bClass = libClasses.resolve("lib").resolve("B.class");

        Files.delete(bClass);

        var log2 =
                new JavacTask(tb)
                    .options("--enable-preview",
                             "-source", JAVA_VERSION,
                             "-XDrawDiagnostics",
                             "-Xlint:-preview",
                             "--class-path", libClasses.toString())
                    .outdir(classes)
                    .files(tb.findJavaFiles(src))
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);

        if (!expectedErrors.equals(log2)) {
            throw new AssertionError("Incorrect errors, expected: " + expectedErrors +
                                      ", actual: " + log2);
        }

    }
}
