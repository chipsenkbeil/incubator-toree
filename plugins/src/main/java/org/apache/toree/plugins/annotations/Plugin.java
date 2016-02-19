package org.apache.toree.plugins.annotations;

import java.lang.annotation.*;

/**
 * Represents a generic plugin.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Plugin {}
