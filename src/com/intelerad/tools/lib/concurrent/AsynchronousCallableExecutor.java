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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * AsynchronousCallableExecutor is a class that implements an execution policy
 * on a fixed List of AsynchronousCallables. It also provides a modified
 * "listener" callback interface to make managing the results easier. After each
 * AsychronousCallable finishes the next one is started.
 */
public class AsynchronousCallableExecutor
{
    /**
     * Pass this to doAllFunctions as the concurrency value to allow all
     * AsynchronouseCallables to run at the same time
     */
    public static final int UNLIMITED = -1;

    private final List mAsynchronousCallableBundles;
    
    /** Index into mAsynchronousCallableBundles indicating the next task to start. */
    private int mCurrentIndex = 0;
    
    /** Map CallListener to TaskController */
    private final Map mCurrentTasks; 
    private final int mConcurrency;

    private BasicCompositeTaskController mCompositeTaskController;
    
    /**
     * Invokes each AsynchronousCallable in turn one at a time.
     * 
     * @param asynchronousCallableBundles to execute
     * @param listener to notify
     * @param invoker to use to notify
     * @return a TaskController representing this asynchronous task
     */
    public static TaskController doAllFunctions( List asynchronousCallableBundles, CompositeCallListener listener, Invoker invoker )
    {
        return doAllFunctions( asynchronousCallableBundles, listener, invoker, 1 );
    }
    
    /**
     * Invokes each AsynchronousCallable in turn allowing for allowedParelellism
     * number of tasks to be running concurrently.
     * 
     * @param asynchronousCallableBundles
     *            to execute
     * @param listener
     *            to notify
     * @param invoker
     *            to use to notify
     * @param concurrency
     *            number of allowed concurrent functions or UNLIMITED to allow
     *            all tasks to run at the same time.
     * @return a TaskController representing this asynchronous task
     */
    public static TaskController doAllFunctions( List asynchronousCallableBundles,
                                                 CompositeCallListener listener,
                                                 Invoker invoker,
                                                 int concurrency )
    {
        AsynchronousCallableExecutor compositeFunction = new AsynchronousCallableExecutor( asynchronousCallableBundles,
                                                                                           listener,
                                                                                           invoker,
                                                                                           concurrency );
        compositeFunction.start();
        return compositeFunction.getTaskController();
    }
    
    public AsynchronousCallableExecutor( List asynchronousCallableBundles, CompositeCallListener listener, Invoker invoker, int concurrency )
    {
        mCurrentTasks = new HashMap();
        mAsynchronousCallableBundles = new ArrayList( asynchronousCallableBundles );
        mConcurrency =  concurrency == UNLIMITED ? mAsynchronousCallableBundles.size() : concurrency;
        
        Cancellable cancellable = new Cancellable()
        {
            public void cancel() 
            {
                cancelOutstanding();
            }
        };
        mCompositeTaskController = new BasicCompositeTaskController( cancellable, listener, invoker );
    }
    
    public TaskController getTaskController()
    {
        return mCompositeTaskController;
    }
    
    public synchronized void start()
    {
        for ( int index = 0; index < mConcurrency; index++ )
        {
            startTask();
        }
        checkForDone();
    }
    
    private synchronized void taskFinished( CallListener listener )
    {
        mCurrentTasks.remove( listener );
        
        startTask();
        checkForDone();
    }
    
    private synchronized void checkForDone()
    {
        if ( mCurrentTasks.size() == 0 )
            mCompositeTaskController.finished();
    }
    
    private synchronized void startTask()
    {
        if ( mCompositeTaskController.isCancelled() )
            return;
        if ( mCurrentIndex >= mAsynchronousCallableBundles.size() )
            return;
        
        final AsynchronousCallableBundle currentCallableBundle = (AsynchronousCallableBundle) mAsynchronousCallableBundles.get( mCurrentIndex );
        mCurrentIndex ++;
        CallListener callListener = new CallAdapter()
        {
            public void handleSuccess( final Object result )
            {
                mCompositeTaskController.addPartialSuccess( result, currentCallableBundle.getContext() );
            }
            
            public void handleException( final Exception ex )
            {
                mCompositeTaskController.addPartialException( ex, currentCallableBundle.getContext() );
            }
            
            public void handleFinally()
            {
                taskFinished( this );
            }  
        };
            
        mCurrentTasks.put( callListener, currentCallableBundle.getAsynchronousCallable().call( callListener ) );
    }

    private synchronized void cancelOutstanding()
    {
        for ( Iterator iterator = mCurrentTasks.values().iterator(); iterator.hasNext(); )
        {
            try 
            {
                ( (TaskController) iterator.next() ).cancel();
            } 
            catch ( Throwable t )
            {
                ConcurrencyLogManager.getDefault().printException( "unexpected exception in cancel().", t );
            }
        }
    }
}
