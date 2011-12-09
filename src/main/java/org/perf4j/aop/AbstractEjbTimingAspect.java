package org.perf4j.aop;

import org.perf4j.LoggingStopWatch;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is the base class for TimingAspects that use the EJB interceptor framework.
 * Subclasses just need to implement the {@link #newStopWatch} method to use their logging framework of choice
 * (e.g. log4j or java.logging) to persist the StopWatch log message.
 *
 * @author Alex Devine
 */
public abstract class AbstractEjbTimingAspect extends AgnosticTimingAspect {

	/**
	 * This ThreadLocal is used to keep the depth of call
	 */
	private static final ThreadLocal<AtomicLong> TL_DEEP = new ThreadLocal<AtomicLong>() {
		@Override
		protected AtomicLong initialValue() {
			return new AtomicLong();
		};
	};
	

    /**
     * This is the interceptor that runs the target method, surrounding it with stop watch start and stop calls.
     *
     * @param ctx The InvocationContext will be passed in by the Java EE server.
     * @return The return value from the executed method.
     * @throws Exception Any exceptions thrown by the executed method will bubble up.
     */
    @AroundInvoke
    public Object doPerfLogging(final InvocationContext ctx) throws Exception {
		long depth = TL_DEEP.get().incrementAndGet();
        final Method executingMethod = ctx.getMethod();

        //need to get the Profiled annotation off the method, otherwise use a default
        Profiled profiled = (executingMethod == null) ?
                            DefaultProfiled.INSTANCE :
                            ctx.getMethod().getAnnotation(Profiled.class);
        if (profiled == null) {
            profiled = DefaultProfiled.INSTANCE;
        }

        //note - the EJB 3.0 Interceptor spec requires that we only throw Exception, NOT throwable, but
        //runProfiledMethod throws Throwable.
		
		final Map<String, Object> additionnalVars = new HashMap<String, Object>();
		additionnalVars.put("$depth", depth);
		additionnalVars.put("$threadName", Thread.currentThread().getName());
		additionnalVars.put("$threadId", Thread.currentThread().getId());

        try {
            return runProfiledMethod(
                    new AbstractJoinPoint() {
                        public Object proceed() throws Throwable { return ctx.proceed(); }

                        public Object getExecutingObject() { return ctx.getTarget(); }

                        public Object[] getParameters() { return ctx.getParameters(); }

                        public String getMethodName() {
                            return (executingMethod == null) ? "null" : executingMethod.getName();
                        }
                        
                        public Class<?> getDeclaringClass() { return (executingMethod == null) ? null : executingMethod.getDeclaringClass() ; }

						public Map<String, Object> getAdditionnalVars() {
							return additionnalVars;
						}
					},
                    profiled,
                    newStopWatch(profiled.logger(), profiled.level())
            );
        } catch (Exception e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            //in practice this should rarely happen, as in standard usage Throwables fall under Exception or Error.
            //However, by the Java spec, ONLY RuntimeExceptions and Errors are unchecked, so the compiler prevents us
            //from executing "throw t" at this point. This seems like a poor design decision of the EJB spec, as all
            //interceptors that wish to call a method that throws Throwable need to do something special like this.
            //The best we can do here is wrap the Throwable as a RuntimeException, even though this could potentially
            //change the semantics from the callers point of view.
            throw new RuntimeException(t);
        } finally {
			TL_DEEP.get().decrementAndGet();
		}
    }

    /**
     * Subclasses should implement this method to return a LoggingStopWatch that should be used to time the wrapped
     * code block.
     *
     * @param loggerName The name of the logger to use for persisting StopWatch messages.
     * @param levelName  The level at which the message should be logged.
     * @return The new LoggingStopWatch.
     */
    protected abstract LoggingStopWatch newStopWatch(String loggerName, String levelName);
}
