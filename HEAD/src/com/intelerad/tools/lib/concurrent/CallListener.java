/*
 * Copyright (c) 2008, Intelerad Medical Systems Incorporated
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Intelerad Medical Systems Incorporated nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY Intelerad Medical Systems Incorporated ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Intelerad Medical Systems Incorporated BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.intelerad.tools.lib.concurrent;

/**
 * This interface is used by clients who wish to implement an asynchronous
 * function call. This is usually done by using a Callable and some sort of
 * Executor (WorkerController is such an Executor). The use of Callable and an
 * Executor should be considered an <b>implementation detail</b> of the
 * asynchronous function since some functions may actually wrap a truly
 * asynchronous operation!
 * <p>
 * This interface is made to be used with CancelableCallable, "Executors" and
 * TaskControllers although it can be used by anyone who wants to create an
 * "asynchronous function".
 * <p>
 * Asynchronous functions have the form:
 * 
 * <pre>
 * public TaskController asychronousFunctionName( int parameter1,
 *                                                String parameter2,
 *                                                CallListener toNotify )
 * {
 *     //...
 * }
 * 
 * </pre>
 * 
 * A simple asynchronous function would be implemented thusly:
 * 
 * <pre>
 * public static TaskController doSomethingCool( int foo, CallListener listener )
 * {
 *     Callable callable = new CancellableCallable()
 *     {
 *         public Object call() throws Exception
 *         {
 *             //...
 *             return null;
 *         }
 * 
 *         public void cancel()
 *         {
 *             //..
 *         }
 *     };
 * 
 *     return CallableExecutor.execute( callable, listener, Invoker.EVENT_THREAD_INVOKER );
 * }
 * </pre>
 * 
 * <p>
 * They will return a TaskController. They will not block for any length of
 * time. They will take a CallListener as the last parameter. They should
 * specify specify which thread the CallListener is notified (called back) on.
 * They should specify the Object type passed back to CallListener's
 * "handleSuccess()" and the Exception types that should be expected in
 * CallListener's handleException(). They may take an "Invoker" as the last
 * parameter which they should use to call to CallListener (the second to last
 * in this case) with.
 * <p>
 * The implementation of the asynchronous function call will guarantee that only
 * one of handleCancel(), handleResult() and handleException() will be called,
 * followed by handleFinally(). Both of these calls will be on the same thread
 * and dispatched to that thread on the same Runnable (in order to ensure that
 * there is no possibility of a race condition between them). Apart from that
 * there is no requirement that the calls will arrive on any particular thread
 * (unless an Invoker instance is passed). It's possible to get these call backs
 * on the event thread or some other arbitrary thread or the thread used by the
 * optional Invoker argument. Asynchronous calls that don't require an Invoker
 * should mention which thread the reply will be dispatched on.
 * <p>
 * Guarantees:
 * <p>
 * The implementation of the asynchronous function call will guarantee that only
 * one of handleCancel(), handleResult() and handleException() will be called,
 * followed by handleFinally(). Both of these calls will be on the same thread
 * and dispatched to that thread on the same Runnable (in order to ensure that
 * there is no possibility of a race condition between them). Asynchronous calls
 * must either require an Invoker or should mention which thread the reply will
 * be dispatched on.
 * <p>
 * If the TaskController's cancel() is called on the callback thread,
 * handleCancel() then handleFinally() will be called. Otherwise, due to race
 * conditions, it's possible that cancel() will be ignored.
 * <p>
 * The callback will be done in a separate Runnable. The callback will not be
 * done synchronously with the invocation of the asynchronous function or a call
 * to cancel().
 * <p>
 * <b>NOTE:</b> If the thought of implementing the asynchronous function call
 * interface gives you a head-ache, you might want to consider making use of
 * TaskControllerHelper; whose sole purpose in being is to help wary coders in
 * implementing the callback semantics of CallListener.
 * 
 * @see com.intelerad.tools.lib.concurrent.TaskControllerHelper
 * @see com.intelerad.tools.lib.concurrent.TaskController
 * @see com.intelerad.tools.lib.concurrent.Invoker
 */
public interface CallListener
{
    /**
     * Usefull if you want to ignore the callback.
     */
    public static final CallListener NULL_LISTENER = new CallAdapter();
    
    /**
     * Called when the asynchronous function returns its result.
     * 
     * @param result returned by the callable.
     */
    public void handleSuccess( Object result );
    
    /**
     * Called when asynchronous function returns an exception.
     * 
     * @param exception
     */
    public void handleException( Exception exception );
    
    /**
     * Called when the execution of the async function call is cancelled.
     */
    public void handleCancel();
    
    /**
     * Called after any of handleCancel(), handleSuccess() or handleException()
     * is called.
     * <p>
     * <b>Will be called in the same runnable as handleSuccess(),
     * handleException(), handleCancel()</b>
     */
    public void handleFinally();
}
