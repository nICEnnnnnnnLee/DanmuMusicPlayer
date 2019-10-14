package nicelee.common.annotation;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ TYPE, PACKAGE })
public @interface Autoload {
	
	String source();// default "bili"
	
	String desc() default "";
}
