Nakamura Wink
=============

Nakamura Wink allows you to register any component in the Nakamura OSGi container as a JAX-RS application by using JAX-RS annotations. Here is how to do that:

Suppose you have a standard OSGi component:

```java
@Service
@Component
public class HelloServiceImpl implements HelloService {
	
	@Override
	public String helloWorld() {
		return hello("World");
	}
	
	@Override
	public String hello(String who) {
		return String.format("Hello, %s!", who);
	}
}
```

If you've installed the Nakamura Wink bundle, you can annotate it like so:

```java
@Service({HelloService.class, Object.class})
@Component
@Property(name="javax.ws.rs", boolValue=true)
@Path("say")
public class HelloServiceImpl implements HelloService {
	
	@Override
	@GET @Path("helloworld")
	public String helloWorld() {
		return hello("World");
	}
	
	@Override
	@GET @Path("hello")
	public String hello(@String who) {
		return String.format("Hello, %s!", who);
	}
}
```

1. Using `Object.class` as a @Service export is necessary to let the Wink servlet import your service as a reference
2. The javax.ws.rs property registers this particular export of Object.class as a JAX-RS resource
3. The `@Path("say")` class annotation will register this resource beneath the configured top-level JAX-RS services context (e.g., `/api/say`); this is configurable VIA the NakamuraWinkServlet component in the felix console, defaults to `/api`
4. The rest falls under the realm of standard JAX-RS, here is a more thorough example of how JAX-RS can be used: http://www.ibm.com/developerworks/web/library/wa-apachewink1/#wink_implementation

Here's a demonstration of using the Jackson serialization to produce JSON from simple Java objects: http://www.youtube.com/watch?v=WeUePcMOP5c

