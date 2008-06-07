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

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;



/**
 * CallableUtilities contains a series of methods for using, creating or
 * manipulating Callables. It also contains a static method for accessing the default thread pool.
 * 
 * @see java.util.concurrent.Executor
 */
public class CallableUtilities
{
    private CallableUtilities() {}
    
    //////////////////// STATIC \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    private static Executor mSimpleThreadPool;
    
    public static synchronized Executor getDefaultThreadCache()
    {
        if ( mSimpleThreadPool == null )
            mSimpleThreadPool =  Executors.newFixedThreadPool( 5 );
        return mSimpleThreadPool;
    }
    
    /**
     * See other execute method. (Uses the default "thread cache" implementation of a thread pool)
     * 
     * @param callable
     * @param listener
     * @param invoker
     * @return
     */
    public static TaskController execute( Callable callable, CallListener listener, Invoker invoker )
    {
        return execute( callable, listener, invoker, getDefaultThreadCache() );
    }
    
    
    /**
     * Executes the callable using the execute and calls back the CallListenener
     * listener using the invoker.
     * <p>
     * <b>NOTE:</b> This method supports making use of the cancel() on
     * CancellableCallables and <b>will</b> make use of it if the Callable
     * implements cancellable.
     * 
     * @param callable
     *            to execute (can be a CancellableCallable).
     * @param listener
     *            to callback (notify)
     * @param invoker
     *            to notify with
     * @param executor
     *            to use to execute the callable (usually some form of thread pool)
     * @return a TaskController representing the asynchronous task
     */
    public static TaskController execute( Callable callable, CallListener listener, Invoker invoker, Executor executor )
    {
        final CallableRunner callableRunner = new CallableRunner( callable, listener, invoker );
        executor.execute( callableRunner );
        return callableRunner.getTaskController();
    }
    
    public static AsynchronousCallable buildAsynchronousCallable( final Callable callable, final Invoker invoker )
    {
        return new AsynchronousCallable()
        {
            public TaskController call( CallListener callListener )
            {
                return execute( callable, callListener, invoker );
            }
        };
    }
    
    //////////////////// CLASSES \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    private static class CallableRunner implements Runnable
    {
        private Callable mCallable;
        private BasicTaskController mBasicTaskController;

        private CallableRunner( Callable callable, CallListener listener, Invoker invoker )
        {
            mCallable = callable;
            Cancellable cancellable = new Cancellable()
            {
                public void cancel()
                {
                    if ( mCallable instanceof Cancellable )
                        ( (Cancellable) mCallable ).cancel();
                }
            };
            mBasicTaskController = new BasicTaskController( cancellable, listener, invoker );
        }

        public void run()
        {
            try
            {
                mBasicTaskController.setResult( mCallable.call() );
            }
            catch ( final Throwable error )
            {
                /*
                 * We are not allowed to setException() to something other than
                 * an Error or Exception but this is fine because mCallable
                 * isn't supposed to give use anything other than Exception or
                 * Error.
                 */
                mBasicTaskController.setException( error ); 
            }
        }

        public TaskController getTaskController()
        {
            return mBasicTaskController;
        }
    }
}
