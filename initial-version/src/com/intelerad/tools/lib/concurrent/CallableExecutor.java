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

import java.util.LinkedList;

/**
 * The CallableExecutor currently is:
 * <p>
 * 1) An interface.<br>
 * 2) An implementation of that interface which uses a "thread cache" style
 * thread pool.<br>
 * 3) A set of static, asychronous method(s) for running Callables in said
 * thread pool.<br>
 * 4) Confused.
 * <p>
 * It has, in a sense, inherited WorkerController's identity crisis.
 * <p>
 * From the outside (currently) it can be treated as a single, static method:
 * 
 * <pre>
 *   CallableExecutor.execute( Callable callable, CallListener listener, Invoker invoker )
 * </pre>
 * 
 * ...which really should be..
 * 
 * <pre>
 *   CallableExecutor.execute( Callable callable, CallListener listener, Invoker invoker, Executor executor )
 * </pre>
 * 
 * but isn't because of the lack of Java 1.5 and the fact we haven't need that
 * last argument yet.
 * <p>
 * Future expansion notes:
 * <p>
 * Conceptually a CallableExecutor (as an interface) implements:
 * 
 * <pre>
 * public TaskController execute( Callable callable, CallaListener listener, Invoker invoker );
 * </pre>
 * 
 * With a specific Executor ( in the Java 1.5 sense ) to run the callable with
 * (which in turn decides which thread to use). Since I haven't needed to use
 * this interface I haven't written it out explicitly, I've just been using the
 * static method version.
 * <p>
 * CallableExecutor is <b>not</b> a Java 1.5 class.
 * 
 * @see java.util.concurrent.Executor
 */
public class CallableExecutor implements TaskController
{
    //////////////////// STATIC \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    
    private static SimpleThreadPool mSimpleThreadPool;
    
    public static synchronized Executor getThreadPool()
    {
        if ( mSimpleThreadPool == null )
            mSimpleThreadPool = new SimpleThreadPool( 5 );
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
        return execute( callable, listener, invoker, getThreadPool() );
    }
    
    
    /**
     * Executes the callable using the execute and calls back the CallListenener
     * listener using the invoker.
     * <p>
     * <b>NOTE:</b> This method supports making use of the cancel() on
     * CancellableCallables and <b>will</b> make use of it if the Callable
     * implements cancellable.
     * <p>
     * (The method is private because we don't need it yet.)
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
        final CallableExecutor task = new CallableExecutor( callable, listener, invoker );
        executor.execute( new Runnable()
        {
            public void run()
            {
                task.run();
            }
        } );
        return task;
    }
    
    
    
    
    //////////////////// INSTANCE \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    private Callable mCallable;
    private CallListener mListener;
    private Invoker mInvoker;
    
    private boolean mDone;
    private boolean mCancelled;
    
    private Object mResult;
    private Throwable mException;
    
    private CallableExecutor( Callable callable, CallListener listener, Invoker invoker )
    {
        mCallable = callable;
        mListener = listener;
        mInvoker= invoker;
    }
    
    private void run()
    {
        try
        {
            mResult = mCallable.call();
            Runnable runnable = new Runnable()
            {
                public void run()
                {
                    mListener.handleSuccess( mResult );
                }
            };
            checkConditionsAndFinish( runnable );
        }
        catch ( final Exception error )
        {
            mException = error;
            Runnable runnable = new Runnable()
            {
                public void run()
                {
                    mListener.handleException( error );
                }
            };
            checkConditionsAndFinish( runnable );
        }
        catch( Throwable error )
        {
            ConcurrencyLogManager.getDefault().printException( "Uncaught error in Worker.call()", error );
        }
    }
    
    private synchronized void checkConditionsAndFinish( final Runnable runnable )
    {
        if ( mDone )
            return; //we've already called one of the three Worker methods.
        mDone     = true;
        notifyAll();
        
        Runnable finallyRunnable = new Runnable()
        {
            public void run()
            {
                try
                {
                    runnable.run();
                }
                finally
                {
                    mListener.handleFinally();
                }
            }
        };
        mInvoker.invoke( finallyRunnable );
    }

    
    /////////////////// TaskController \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public synchronized void cancel()
    {
        Runnable runnable = new Runnable() 
        {
            public void run()
            {
                mListener.handleCancel();
            }
        };
        if ( mCallable instanceof Cancellable )
            ( (Cancellable) mCallable ).cancel();
        
        checkConditionsAndFinish( runnable );
        mCancelled    = true;
    }
    
    public synchronized Object get() throws ExecutionException, InterruptedException
    {
        while ( ! isDone() )
            wait();
        
        if ( isCancelled() )
            throw new CancellationException();
        
        if ( mException != null )
            throw new ExecutionException( mException );
        
        return mResult;
    }

    public synchronized boolean isCancelled()
    {
        return mCancelled;
    }

    public synchronized boolean isDone()
    {
        return mDone;
    }
    
    
    /**
     * This can be replaced in Java 1.5 with an Executor from the API.
     */
    private static class SimpleThreadPool implements Executor
    {
        /** For use in generating thread names */
        private static volatile int mThreadCount = 0;
        
        private LinkedList mAvailableThreads = new LinkedList();
        private final int mThreadsToCache;
        
        SimpleThreadPool( int threadToCache )
        {
            mThreadsToCache = threadToCache;
        }
        
        public synchronized void execute( Runnable runnable )
        {
            ExecutorThread executorThread;
            if ( mAvailableThreads.size() > 0 )
                executorThread = (ExecutorThread) mAvailableThreads.removeLast();
            else
                executorThread = buildNewExecutorThread();
            
            executorThread.execute( runnable );
        }
        
        
        private synchronized void returnThread( ExecutorThread thread )
        {
            if ( mAvailableThreads.size() >= mThreadsToCache )
                thread.close();
            else
                mAvailableThreads.addFirst( thread );
        }
        
        
        private ExecutorThread buildNewExecutorThread()
        {
            ExecutorThread executorThread = new ExecutorThread();
            Thread thread = new Thread( executorThread, "Worker-" + ( mThreadCount++ ) );
            thread.start();
            return executorThread;
        }
        
        private class ExecutorThread implements Runnable
        {
            private Runnable mToExecute;
            private boolean mEnd = false;
            
            public void run()
            {
                for (;;)
                {
                    try
                    {
                        Runnable toRun = getRunnable();
                        if ( toRun == null )
                            return;
                        toRun.run();
                    } 
                    catch (Throwable throwable )
                    {
                        ConcurrencyLogManager.getDefault().printException( "ExecutorThread failed.", throwable );
                        return;
                    }
                    returnThread( this );
                }
            }
            
            private synchronized Runnable getRunnable() throws InterruptedException
            {
                for (;;)
                {
                    if ( mEnd )
                        return null;
                    if ( mToExecute != null )
                    {
                        Runnable toReturn = mToExecute;
                        mToExecute = null;
                        return toReturn;
                    }
                    else
                    {
                        wait();
                    }
                }
            }
            
            public synchronized void close()
            {
                mEnd = true;
                notifyAll();
            }
            
            public synchronized void execute( Runnable runnable )
            {
                assert mToExecute == null;
                
                mToExecute = runnable;
                notifyAll();
            }
        }
    }
}
