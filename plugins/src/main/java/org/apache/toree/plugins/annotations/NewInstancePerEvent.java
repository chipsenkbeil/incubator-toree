package org.apache.toree.plugins.annotations;

import java.lang.annotation.*;

/**
 * Marks a plugin to be created and destroyed once per event rather than
 * persisting across the lifecycle of all events.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface NewInstancePerEvent {}
