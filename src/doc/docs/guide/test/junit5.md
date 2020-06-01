# JUnit 5

Junit 5 [user guide](https://junit.org/junit5/docs/current/user-guide/)

You will need the following dependencies (assuming BOM used for versions management):

```groovy
testImplementation 'io.dropwizard:dropwizard-testing'
testImplementation 'org.junit.jupiter:junit-jupiter-api'
testRuntimeOnly 'org.junit.jupiter:junit-jupiter'
```

!!! tip
    If you already have junit4 or spock tests, you can activate [vintage engine](https://junit.org/junit5/docs/current/user-guide/#migrating-from-junit4) 
    so all tests could work  **together** with junit 5: 
    ```groovy    
    testRuntimeOnly 'org.junit.vintage:junit-vintage-engine'
    ```

## Dropwizard extensions compatibility

Guicey extensions can be used with dropwizard extenssions. But this may be required only in edge cases
when multiple applications startup is required.

!!! info
    There is a difference in extensions implementation. 
    
    Dropwizard extensions work as:
    junit extension `@ExtendWith(DropwizardExtensionsSupport.class)` looks for fields 
    implementing `DropwizardExtension` (like `DropwizardAppExtension`) and start/stop them according to test lifecycle.
    
    Guicey extensions completely implemented as junit extensions (and only hook fields are manually searched). 
    Also, guciey extension rely on junit parameters injection. Both options has pros and cons.
    

## Extensions

Provided extensions:

* `@TestGuiceyApp` - for lightweight tests (without starting web part, only guice context)
* `@TestDropwizardApp` - for complete integration tests

Both extensions allow using injections directly in test fields.

Extensions are compatible with [parallel execution](#parallel-execution) (no side effects).

[Alternative declaration](#alternative-declaration) is possible.

!!! note
    Spock and junit5 extensions are almost equivalent in features and behaviour. 
    
## @TestGuiceyApp

`@TestGuiceyApp` runs all guice logic without starting jetty (so resources, servlets and filters will not be available).
`Managed` objects will still be handled correctly.

```java
@TestGuiceyApp(MyApplication.class)
public class AutoScanModeTest {

    @Inject 
    MyService service;
    
    @Test
    public void testMyService() {        
        Assertions.assertEquals("hello", service.getSmth());     
    }
```

Also, injections work as method parameters:

```java
@TestGuiceyApp(MyApplication.class)
public class AutoScanModeTest {
    
    public void testMyService(MyService service) {        
        Assertions.assertEquals("hello", service.getSmth());     
    }
```

Application started before all tests in annotated class and stopped after them.

## @TestDropwizardApp

`@TestDropwizardApp` is useful for complete integration testing (when web part is required):

```groovy
@TestDropwizardApp(MyApplication.class)
class WebModuleTest extends Specification {

    @Inject 
    MyService service

    @Test
    public void checkWebBindings(ClientSupport client) {

        Assertions.assertEquals("Sample filter and service called", 
            client.targetMain("servlet").request().buildGet().invoke().readEntity(String.class));
        
        Assertions.assertTrur(service.isCalled());
```

### Random ports

In order to start application on random port you can use configuration shortcut:

```groovy
@TestDropwizardApp(value = MyApplication.class, randomPorts = true)
```

!!! note
    Random ports will be applied even if configuration with exact ports provided:
    ```groovy
    @TestDropwizardApp(value = MyApplication, 
                      config = 'path/to/my/config.yml', 
                      randomPorts = true)
    ```
    Also, random ports support both server types (default and simple)
    
Real ports could be resolved with [ClientSupport](#client) object.

### Rest mapping

Normally, rest mapping configured with `server.rootMapping=/something/*` configuration, but
if you don't use custom configuration class, but still want to re-map rest, shortcut could be used:

```groovy
@TestDropwizardApp(value = MyApplication.class, restMapping="something")
```

In contrast to config declaration, attribute value may not start with '/' and end with '/*' -
it would be appended automatically. 

This option is only intended to simplify cases when custom configuration file is not yet used in tests
(usually early PoC phase). It allows you to map servlet into application root in test (because rest is no
more resides in root). When used with existing configuration file, this parameter will override file definition.

## Guice injections

Any gucie bean may be injected directly into test field:

```groovy
@Inject
SomeBean bean
```

This may be even bean not declared in guice modules (JIT injection will occur).

To better understand injection scopes look the following test:

```groovy
@TestGuiceyApp(AutoScanApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InjectionScopeTest {

    // new instance injected on each test
    @Inject
    TestBean bean;

    // the same context used for all tests (in class), so the same bean instance inserted before each test
    @Inject
    TestSingletonBean singletonBean;

    @Test
    @Order(1)
    public void testInjection() {
        bean.value = 5;
        singletonBean.value = 15;

        Assertions.assertEquals(5, bean.value);
        Assertions.assertEquals(15, singletonBean.value);

    }

    @Test
    @Order(2)
    public void testSharedState() {

        Assertions.assertEquals(0, bean.value);
        Assertions.assertEquals(15, singletonBean.value);
    }

    // bean is in prototype scope
    public static class TestBean {
        int value;
    }

    @Singleton
    public static class TestSingletonBean {
        int value;
    }
}
```


!!! note
    Guice AOP will not work on test methods (because test instances not created by guice).

## Parameter injection

Any **declared** guice bean may be injected as method parameter:

```java
@Test
public void testSomthing(DummyBean bean) 
```

(where `DummyBean` is manually declared in some module or JIT-instantiated during injector creation).

For not declared beans injection (JIT) special annotation must be used:

```java
@Test
public void testSomthing(@Jit TestBean bean) 
```

!!! info
    Additional annotation required because you may use other junit extensions providing their own
    parameters, which guicey extension should not try to handle. That's why not annotated parameters
    verified with existing injector bindings.
    
Qualified and generified injections will also work:

```java
@Test
public void testSomthing(@Named("qual") SomeBean bean,
                         TestBean<String> generifiedBean,
                         Provider<OtherBean> provider) 
```    

Also, there are special objects available as parameters:

* `Application` or exact application class (`MyApplication`)
* `ObjectMapper`
* `ClientSupport` application web client helper

!!! note
    Parameter injection will work not only in test, but also in lifecyle methods (beforeAll, afterEach etc.) 

Example:

```java
@TestDropwizardApp(AutoScanApplication.class)
public class ParametersInjectionDwTest {

    public ParametersInjectionDwTest(Environment env, DummyService service) {
        Preconditions.checkNotNull(env);
        Preconditions.checkNotNull(service);
    }

    @BeforeAll
    static void before(Application app, DummyService service) {
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(service);
    }

    @BeforeEach
    void setUp(Application app, DummyService service) {
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(service);
    }

    @AfterEach
    void tearDown(Application app, DummyService service) {
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(service);
    }

    @AfterAll
    static void after(Application app, DummyService service) {
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(service);
    }

    @Test
    void checkAllPossibleParams(Application app,
                                AutoScanApplication app2,
                                Configuration conf,
                                TestConfiguration conf2,
                                Environment env,
                                ObjectMapper mapper,
                                Injector injector,
                                ClientSupport client,
                                DummyService service,
                                @Jit JitService jit) {
        assertNotNull(app);
        assertNotNull(app2);
        assertNotNull(conf);
        assertNotNull(conf2);
        assertNotNull(env);
        assertNotNull(mapper);
        assertNotNull(injector);
        assertNotNull(client);
        assertNotNull(service);
        assertNotNull(jit);
        assertEquals(client.getPort(), 8080);
        assertEquals(client.getAdminPort(), 8081);
    }

    public static class JitService {

        private final DummyService service;

        @Inject
        public JitService(DummyService service) {
            this.service = service;
        }
    }
}
```

## Client

Both extensions prepare special jersey client instance which could be used for web calls.
It is mostly useful for complete web tests to call rest services and servlets.

```java
@Test
void checkRandomPorts(ClientSupport client) {
    Assertions.assertNotEquals(8080, client.getPort());
    Assertions.assertNotEquals(8081, client.getAdminPort());
}
```

Client object provides:

* Access to [JerseyClient](https://eclipse-ee4j.github.io/jersey.github.io/documentation/2.29.1/client.html) object (for raw calls)
* Shortcuts for querying main, admin or rest contexts (it will count the current configuration automatically)
* Shortcuts for base main, admin or rest contexts base urls (and application ports)

Example usages:

```java
// GET {rest path}/some
client.targetRest("some").request().buildGet().invoke()

// GET {main context path}/servlet
client.targetMain("servlet").request().buildGet().invoke()

// GET {admin context path}/adminServlet
client.targetAdmin("adminServlet").request().buildGet().invoke()
```

!!! tip
    All methods above accepts any number of strings which would be automatically combined into correct path:
    ```groovy
    client.targetRest("some", "other/", "/part")
    ``` 
    would be correctly combined as "/some/other/part/"

As you can see test code is abstracted from actual configuration: it may be default or simple server
with any contexts mapping on any ports - target urls will always be correct.

```java
Response res = client.targetRest("some").request().buildGet().invoke()

Assertions.assertEquals(200, res.getStatus())
Assertions.assertEquals("response text", res.readEntity(String)) 
```

Also, if you want to use other client, client object can simply provide required info:

```groovy
client.getPort()        // app port (8080)
client.getAdminPort()   // app admin port (8081)
client.basePathMain()   // main context path (http://localhost:8080/)
client.basePathAdmin()  // admin context path (http://localhost:8081/)
client.basePathRest()   // rest context path (http://localhost:8080/)
```

Raw client usage:

```java
// call completely external url
client.target("http://somedomain:8080/dummy/").request().buildGet().invoke()
```

!!! warning 
    Client object could be injected with both dropwizard and guicey extensions, but in case of guicey extension,
    only raw client could be used (because web part not started all other methods will throw NPE)

## Configuration

For both extensions you can configure application with external configuration file:

```java
@TestGuiceyApp(value = MyApplication.class,
    config = "path/to/my/config.yml"
public class ConfigOverrideTest {
```

Or just declare required values:

```java
@TestGuiceyApp(value = MyApplication.class,
    configOverride = {
            "foo: 2",
            "bar: 12"
    })
public class ConfigOverrideTest {
```

(note that overriding declaration follows yaml format "key: value")

Or use both at once (here overrides will override file values):

```java
@TestGuiceyApp(value = MyApplication.class,
    config = 'path/to/my/config.yml',
    configOverride = {
            "foo: 2",
            "bar: 12"
    })
class ConfigOverrideTest {
```

## Application test modification

You can use [hooks to customize application](overview.md#configuration-hooks).

In both extensions annotation hooks could be declared with attribute:

```java
@TestDropwizardApp(value = MyApplication.class, hooks = MyHook.class)
```

or

```java
@TestGuiceyApp(value = MyApplication.class, hooks = MyHook.class)
```

Where MyHook is:

```java
public class MyHook implements GuiceyConfigurationHook {}
```

### Hook fields

Alternatively, you can declare hook directly in test static field:

```java
@EnableHook
static GuiceyConfigurationHook HOOK = builder -> builder.modules(new DebugModule());
```

Any number of fields could be declared. The same way hook could be declared in base test class:

```java
public class BaseTest {
    
    // hook in base class
    @EnableHook
    static GuiceyConfigurationHook BASE_HOOK = builder -> builder.modules(new DebugModule());
}

@TestGuiceyApp(value = App.class, hooks = SomeOtherHook.class)
public class SomeTest extends BaseTest {
    
    // Another hook
    @EnableHook
    static GuiceyConfigurationHook HOOK = builder -> builder.modules(new DebugModule2());
}
```

All 3 hooks will work.

## Parallel execution
    
Junit [parallel tests execution](https://junit.org/junit5/docs/current/user-guide/#writing-tests-parallel-execution)
could be activated with properties file `junit-platform.properties` located at test resources root:

```properties
junit.jupiter.execution.parallel.enabled = true
junit.jupiter.execution.parallel.mode.default = concurrent
```

To avoid port collisions in dropwizard tests use [randomPorts option](#random-ports).

## Alternative declaration

Both extensions could be declared in static fields:

```java
@RegisterExtension
static TestDropwizardAppExtension app = TestDropwizardAppExtension.forApp(AutoScanApplication.class)
        .config("src/test/resources/ru/vyarus/dropwizard/guice/config.yml")
        .configOverrides("foo: 2", "bar: 12")
        .randomPorts()
        .hooks(Hook.class)
        .hooks(builder -> builder.disableExtensions(DummyManaged.class))
        .create();
```

The only difference with annotations is that you can declare hooks as lambdas directly 
(still hooks in static fields will also work).

```java
@RegisterExtension
static TestGuiceyAppExtension app = TestGuiceyAppExtension.forApp(AutoScanApplication.class)
        ...
```

This alternative declaration is intended to be used in cases when guicey exensions need to be aligned with
other 3rd party extensions: in junit you can order extensions declared with annotations (by annotation order)
and extensions declared with `@RegisterExtension` (by declaration order). But there is no way
to order extension registered with `@RegisterExtension` before annotation extension.

So if you have 3rd party extension which needs to be executed BEFORE guicey extensions, you can use field declaration.

## Junit nested classes

Junit natively supports [nested tests](https://junit.org/junit5/docs/current/user-guide/#writing-tests-nested).

Guicey extensions affects all nested tests below declaration (nesting level is not limited):

```java
@TestGuiceyApp(AutoScanApplication.class)
public class NestedPropagationTest {

    @Inject
    Environment environment;

    @Test
    void checkInjection() {
        Assertions.assertNotNull(environment);
    }

    @Nested
    class Inner {

        @Inject
        Environment env; // intentionally different name

        @Test
        void checkInjection() {
            Assertions.assertNotNull(env);
        }
    }
}
```

!!! note
    Nested tests will use exactly the same guice context as root test (application started only once).

Extension declared on nested test will affect all sub-tests:

```java
public class NestedTreeTest {

    @TestGuiceyApp(AutoScanApplication.class)
    @Nested
    class Level1 {

        @Inject
        Environment environment;

        @Test
        void checkExtensionApplied() {
            Assertions.assertNotNull(environment);
        }

        @Nested
        class Level2 {
            @Inject
            Environment env;

            @Test
            void checkExtensionApplied() {
                Assertions.assertNotNull(env);
            }

            @Nested
            class Level3 {

                @Inject
                Environment envr;

                @Test
                void checkExtensionApplied() {
                    Assertions.assertNotNull(envr);
                }
            }
        }
    }

    @Nested
    class NotAffected {
        @Inject
        Environment environment;

        @Test
        void extensionNotApplied() {
            Assertions.assertNull(environment);
        }
    }
}
```

This way nested tests allows you to use different extension configurations in one (root) class.

### Use interfaces to share tests

This is just a tip on how to execute same test method in different environments.

```java
public class ClientSupportDwTest {

    interface ClientCallTest {
        // test to apply for multiple environments
        @Test
        default void callClient(ClientSupport client) {
            Assertions.assertEquals("main", client.targetMain("servlet")
                    .request().buildGet().invoke().readEntity(String.class));
        }
    }

    @TestDropwizardApp(App.class)
    @Nested
    class DefaultConfig implements ClientCallTest {

        @Test
        void testClient(ClientSupport client) {
            Assertions.assertEquals("http://localhost:8080/", client.basePathMain());
        }
    }

    @TestDropwizardApp(value = App.class, configOverride = {
            "server.applicationContextPath: /app",
            "server.adminContextPath: /admin",
    }, restMapping = "api")
    @Nested
    class ChangedDefaultConfig implements ClientCallTest {

        @Test
        void testClient(ClientSupport client) {
            Assertions.assertEquals("http://localhost:8080/app/", client.basePathMain());
        }
    }
}
```

Here test declared in `ClientCallTest` interface will be called for each nested test 
(one declaration - two executions in different environments).
 
## Meta annotation

You can prepare meta annotation (possibly combining multiple 3rd party extensions): 

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@TestDropwizardApp(AutoScanApplication.class)
public @interface MyApp {
}

@MyApp
public class MetaAnnotationDwTest {

    @Test
    void checkAnnotationRecognized(Application app) {
        Assertions.assertNotNull(app);
    }   
}
```

OR you can simply use base test class and configure annotation there:

```java
@TestDropwizardApp(AutoScanApplication.class)
public class BaseTest {}

public class ActualTest extends BaseTest {} 
```

## Dropwizard startup error

!!! warning
    Tests written in such way CAN'T run in parallel due to `System.*` modifications.
       
To test application startup fails you can use extensions:

* [junit5-system-exit](https://github.com/tginsberg/junit5-system-exit)
* [junit5-capture-system-output-extension](https://github.com/blindpirate/junit5-capture-system-output-extension)

```groovy
testImplementation 'com.ginsberg:junit5-system-exit:1.0.0'
testImplementation 'com.github.blindpirate:junit5-capture-system-output-extension:0.1.1'
```

Testing app startup fail:

```java
@Test
@ExpectSystemExit
void checkStartupFail() throws Exception {
    ErrorApplication.main("server");
}
```

With error message validation:

```java
@Test
@ExpectSystemExit
@CaptureSystemOutput
void checkStartupFailWithOutput(CaptureSystemOutput.OutputCapture output) throws Exception {
    // assertion declared before!
    output.expect(Matchers.containsString(
            "No implementation for java.lang.String annotated with @com.google.inject.name.Named(value="));

    ErrorApplication.main("server");
}
```

## 3rd party extensions integration

It is extremely simple in JUnit 5 to [write extensions](https://junit.org/junit5/docs/current/user-guide/#extensions).
If you do your own extension, you can easily integrate with guicey or dropwizard extensions: there
are special static methods allowing you to obtain main test objects:
   
* `GuiceyExtensionsSupport.lookupSupport(extensionContext)` -> `Optional<DropwizardTestSupport>`
* `GuiceyExtensionsSupport.lookupInjector(extensionContext)` -> `Optional<Injector>`
* `GuiceyExtensionsSupport.lookupClient(extensionContext)` -> `Optional<ClientSupport>`

For example:

```java
public class MyExtension implements BeforeEachCallback {
    
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Injector injector = GuiceyExtensionsSupport.lookupInjector(context).get();
        ...
    }
}
```

(guicey holds test state in junit test-specific storages and that's why test context is required)

!!! warning
    There is no way in junit to order extensions, so you will have to make sure that your extension
    will be declared after guicey extension (`@TestGuiceyApp` or `@TestDropwizardApp`).