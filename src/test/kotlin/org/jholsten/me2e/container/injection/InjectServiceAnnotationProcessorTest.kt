package org.jholsten.me2e.container.injection

import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import kotlin.test.*

internal class InjectServiceAnnotationProcessorTest {

    private val invalidFieldTypeMessage =
        "@InjectService annotation can only be applied to fields of type org.jholsten.me2e.container.Container and org.jholsten.me2e.mock.MockServer"
    private val invalidEnclosingClassMessage =
        "@InjectService annotation can only be applied to fields of classes which extend class org.jholsten.me2e.Me2eTest"

    @Test
    fun `Compiling test class with valid annotations should succeed`() {
        val compilation = javac()
            .withProcessors(InjectServiceAnnotationProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "com.example.E2ETest",
                    """
                    package com.example;
                        
                    import org.jholsten.me2e.Me2eTest;
                    import org.jholsten.me2e.container.Container;
                    import org.jholsten.me2e.container.injection.InjectService;
                    import org.jholsten.me2e.container.database.DatabaseContainer;
                    import org.jholsten.me2e.container.microservice.MicroserviceContainer;
                    import org.jholsten.me2e.mock.MockServer;
                    
                    class E2ETest extends Me2eTest {
                        @InjectService
                        private Container container;

                        @InjectService
                        private MicroserviceContainer microserviceContainer;
                    
                        @InjectService
                        private DatabaseContainer databaseContainer;
                    
                        @InjectService
                        private MockServer mockServer;
                        
                        private Object obj;
                    }
                    """.trimIndent()
                )
            )

        assertThat(compilation).succeeded()
    }

    @Test
    fun `Compiling test class with invalid datatype should fail`() {
        val compilation = javac()
            .withProcessors(InjectServiceAnnotationProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "com.example.E2ETest",
                    """
                    package com.example;
                        
                    import org.jholsten.me2e.Me2eTest;
                    import org.jholsten.me2e.container.injection.InjectService;
                    
                    class E2ETest extends Me2eTest {
                        @InjectService
                        private Object obj;

                        @InjectService
                        private String string;
                    }
                    """.trimIndent()
                )
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorCount(2)
        assertThat(compilation).hadErrorContaining(invalidFieldTypeMessage)
    }

    @Test
    fun `Compiling test class with invalid enclosing class should fail`() {
        val compilation = javac()
            .withProcessors(InjectServiceAnnotationProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "com.example.E2ETest",
                    """
                    package com.example;
                        
                    import org.jholsten.me2e.container.Container;
                    import org.jholsten.me2e.container.injection.InjectService;
                    
                    class E2ETest {
                        @InjectService
                        private Container container;
                    }
                    """.trimIndent()
                )
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorCount(1)
        assertThat(compilation).hadErrorContaining(invalidEnclosingClassMessage)
    }

    @Test
    fun `Compiling test class with invalid field types and enclosing class should fail`() {
        val compilation = javac()
            .withProcessors(InjectServiceAnnotationProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "com.example.E2ETest",
                    """
                    package com.example;
                        
                    import org.jholsten.me2e.container.injection.InjectService;
                    
                    class E2ETest {
                        @InjectService
                        private Object obj;
                    }
                    """.trimIndent()
                )
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorCount(2)
        assertThat(compilation).hadErrorContaining(invalidFieldTypeMessage)
        assertThat(compilation).hadErrorContaining(invalidEnclosingClassMessage)
    }

    @Test
    fun `Compiling test class with invalid field types of inner class should fail`() {
        val compilation = javac()
            .withProcessors(InjectServiceAnnotationProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "com.example.E2ETest",
                    """
                    package com.example;
                        
                    import org.jholsten.me2e.Me2eTest;
                    import org.jholsten.me2e.container.injection.InjectService;
                    
                    class E2ETest extends Me2eTest {
                        @InjectService
                        private InnerClass obj;
                        
                        class InnerClass { }
                    }
                    """.trimIndent()
                )
            )

        assertThat(compilation).failed()
        assertThat(compilation).hadErrorCount(1)
        assertThat(compilation).hadErrorContaining(invalidFieldTypeMessage)
    }
}
