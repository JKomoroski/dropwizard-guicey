# Junit 4

!!! warning
    JUnit 4 support is deprecated. Migrate to [JUnit 5](#migrating-to-junit-5), if possible. 

Required dependencies (assuming BOM used for versions management):

```groovy
testImplementation 'io.dropwizard:dropwizard-testing'
testImplementation 'junit:junit'
```

OR you can use it with junit 5 vintage engine:

```groovy
testImplementation 'io.dropwizard:dropwizard-testing'
testImplementation 'org.junit.jupiter:junit-jupiter-api'
testRuntimeOnly 'org.junit.jupiter:junit-jupiter'
testRuntimeOnly 'org.junit.vintage:junit-vintage-engine'
```

This way all existing junit 4 tests would work and new tests could use junit 5 extensions.

## Testing core logic

For integration testing of guice specific logic you can use `GuiceyAppRule`. It works almost like 
[DropwizardAppRule](https://www.dropwizard.io/en/release-2.0.x/manual/testing.html#id2),
but *doesn't start jetty* (and so jersey and guice web modules will not be initialized). 
Managed and lifecycle objects supported.

```java
public class MyTest {

    @Rule
    GuiceyAppRule<MyConfiguration> RULE = new GuiceyAppRule<>(MyApplication.class, "path/to/configuration.yaml");
    
    public void testSomething() {
        RULE.getBean(MyService.class).doSomething();
        ...
    }
}
```

As with dropwizard rule, configuration is optional

```java
new GuiceyAppRule<>(MyApplication.class, null)
```

## Testing web logic

For web component tests (servlets, filters, resources) use 
[DropwizardAppRule](https://www.dropwizard.io/en/release-2.0.x/manual/testing.html#id2).

To access guice beans use injector lookup:

```java
InjectorLookup.getInstance(RULE.getApplication(), MyService.class).get();
```

## Customizing guicey configuration

As [described above](#configuration-hooks) guicey provides a way to modify it's configuration in tests.
You can apply configuration hook using rule:

```java
// there may be exact class instead of lambda
new GuiceyHooksRule((builder) -> builder.modules(...))
```

To use it with `DropwizardAppRule` or `GuiceyAppRule` you will have to apply explicit order:

```java
static GuiceyAppRule RULE = new GuiceyAppRule(App.class, null);
@ClassRule
public static RuleChain chain = RuleChain
       .outerRule(new GuiceyHooksRule((builder) -> builder.modules(...)))
       .around(RULE);
```

!!! attention
    RuleChain is required because rules execution order is not guaranteed and
    configuration rule must obviously be executed before application rule. 

If you need to declare configurations common for all tests then declare rule instace
in base test class and use it in chain (at each test):

```java
public class BaseTest {
    // IMPORTANT no @ClassRule annotation here!
     static GuiceyHooksRule BASE = new GuiceyHooksRule((builder) -> builder.modules(...))
 }

 public class SomeTest extends BaseTest {
     static GuiceyAppRule RULE = new GuiceyAppRule(App.class, null);
     @ClassRule
     public static RuleChain chain = RuleChain
        .outerRule(BASE)
        // optional test-specific staff
        .around(new GuiceyHooksRule((builder) -> builder.modules(...)) 
        .around(RULE);
 }
``` 

!!! warning
    Don't use configuration rule with spock because it will not work. Use special spock extension instead.

## Access guice beans

When using `DropwizardAppRule` the only way to obtain guice managed beans is through:

```java
InjectorLookup.getInjector(RULE.getApplication()).getBean(MyService.class);
```         

Also, the following trick may be used to inject test fields:

```java
public class MyTest {
    
    @ClassRule
    static DropwizardAppRule<TestConfiguration> RULE = ...

    @Inject MyService service;
    @Inject MyOtherService otherService;
    
    @Before
    public void setUp() {
        InjectorLookup.get(RULE.getApplication()).get().injectMemebers(this)
    }                    
}
```

## Testing startup errors

If exception occur on startup dropwizard will call `#!java System.exit(1)` instead of throwing exception (as it was before 1.1.0).
System exit could be intercepted with [system rules](http://stefanbirkner.github.io/system-rules/index.html).

Special rule is provided to simplify work with system rules: `StartupErrorRule`.
It's a combination of exit and out/err outputs interception rules.

To use this rule add dependency: `com.github.stefanbirkner:system-rules:1.16.0`


```java
public class MyErrTest {

    @Rule
    public StartupErrorRule RULE = StartupErrorRule.create();
    
    public void testSomething() {
        new MyErrApp().main('server');
    }
}
```

This test will pass only if application will throw exception during startup.

In junit it is impossible to apply checks after exit statement, so such checks
must be registered as a special callback:

```java
public class MyErrTest {

    @Rule
    public StartupErrorRule RULE = StartupErrorRule.create((out, err) -> {
        Assert.assertTrue(out.contains("some log line"));
        Assert.assertTrue(err.contains("expected exception message"));
    });
    
    public void testSomething() {
        new MyErrApp().main('server');
    }
}
```

Note that err will contain full exception stack trace and so you can check exception type too
by using contains statement like above.

Check callback(s) may be added after rule creation:

```java
@Rule
public StartupErrorRule RULE = StartupErrorRule.create();

public void testSomething() throws Exception {
    RULE.checkAfterExit((out, err) -> {
            Assert.assertTrue(err.contains("expected exception message"));
        });
    ...
}
``` 

Multiple check callbacks may be registered (even if the first one was registered in rule's 
create call).

!!! note ""
    Rule works a bit differently with spock (see below).

## Migrating to JUnit 5

* Instead of GuiceyAppRule use [@TestGuiceyApp](junit5.md#testguiceyapp) extension.
* Instead of DropwizardAppRule use [@TestDropwizardApp](junit5.md#testdropwizardapp) extension.
* GuiceyHooksRule can be substituted with hooks declaration [in extensions](junit5.md#application-test-modification) or as [test fields](junit5.md#hook-fields)
* There is no direct substitution for StartupErrorRule, but something similar could be achieved 
with [3rd party extensions](junit5.md#dropwizard-startup-error).

In essence:

* Use annotations instead of rules (and forget about RuleChain difficulties)
* Test fields injection will work out of the box, so no need for additional hacks
* JUnit 5 propose parameter injection, which may be not common at first, but its actually very handy    