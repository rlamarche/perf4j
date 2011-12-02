package org.perf4j.aop;

import java.lang.annotation.Annotation;

import org.perf4j.StopWatch;

/**
 * This concrete implementation of this Profiled annotation interface is used for cases where some
 * interception frameworks may want to profile methods that DON'T have a profiled annotation (for example, EJB 3.0
 * interceptors). See the code for {@link AbstractEjbTimingAspect} for an example of how this is
 * used.
 */
@SuppressWarnings("all")
public class CustomizableProfiled implements Profiled {

	private String tag;

	public String tag() {
		return tag;
	}

	public void setTag(final String tag) {
		this.tag = tag;
	}

	public String message() {
		return "";
	}

	public String logger() {
		return StopWatch.DEFAULT_LOGGER_NAME;
	}

	public String level() {
		return "INFO";
	}

	public boolean el() {
		return true;
	}

	public boolean logFailuresSeparately() {
		return false;
	}

	public long timeThreshold() {
		return 0;
	}

	public boolean normalAndSlowSuffixesEnabled() {
		return false;
	}

	public Class<? extends Annotation> annotationType() {
		return getClass();
	}
}
