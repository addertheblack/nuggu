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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * The class where all the threading magic happens! This class is responsible
 * for implementing the TaskController interface <I>AND</I> making sure that
 * the CallListener callback is done properly. (The CallListener's constraints
 * are explained in that class. The taskController's interface is explained in
 * that class).
 * <p>
 * To use this method, init it with the CallListener you need to callback, and invoker representing the
 * thread to callback on (no synchronous invokers please!)
 */
public class BasicCompositeTaskController implements TaskController
{
    private BasicTaskController mBasicTaskController;
    private CompositeCallListener mListener;
    private List mResults;
    private Invoker mInvoker;
    
    public BasicCompositeTaskController( Cancellable cancellable, CompositeCallListener callListener, Invoker invoker )
    {
        mResults = Collections.synchronizedList( new LinkedList() );
        mListener = callListener;
        mInvoker = invoker;
        CallListener internalCallListener = new CallAdapter()
        {
            public void handleCancel() {
                mListener.handleCancel();
            }
            public void handleFinally() {
                mListener.handleFinally();
            };
        };
        
        mBasicTaskController = new BasicTaskController( cancellable, internalCallListener, invoker );
    }
    
    public void addPartialSuccess( final Object result, final Object context )
    {
        synchronized ( mResults )
        {
            if ( mBasicTaskController.hasTaskReturnedAResult() )
                return;
            mResults.add( new FunctionResultBundle( FunctionResult.createResult( result ), context ) );
            Runnable runnable = new Runnable()
            {
                public void run()
                {
                    /*
                     * isCancelled is the only case where we want to stop all queued callbacks from happening.
                     * if task has returned a result, it may be that the handleFinally() callback has been queued,
                     * which means we still should return any queued handleSuccess() callbacks. 
                     */
                    if ( mBasicTaskController.isCancelled() )
                        return;
                    
                    mListener.handlePartialSuccess( result, context );
                }
            };
            dispatchRunnable( runnable );
        }
    }
    
    public void addPartialException( final Exception ex, final Object context )
    {
        synchronized ( mResults )
        {
            if ( mBasicTaskController.hasTaskReturnedAResult() )
                return;
            mResults.add( new FunctionResultBundle( FunctionResult.createException( ex ), context ) );
            Runnable runnable = new Runnable()
            {
                public void run()
                {
                    if ( mBasicTaskController.isCancelled() )
                        return;
                    
                    mListener.handlePartialException( ex, context );
                }
            };
            
            dispatchRunnable( runnable );
        }
    }
    
    private void dispatchRunnable( Runnable runnable )
    {
        mInvoker.invoke( runnable );
    }
    
    public void finished()
    {
        synchronized ( mResults )
        {
            /*
             * Done to trigger a handleFinally()..
             */
            mBasicTaskController.setResult( new ArrayList( mResults ) );
        }
    }
    
    
    //Task controller stuff..
    public void cancel()
    {
        mBasicTaskController.cancel();
    }

    public Object get() throws ExecutionException, InterruptedException, CancellationException
    {
        throw new UnsupportedOperationException( "This method cannot be invoked for a this object." );
        //mBasicTaskController.get() will deadlock.
    }

    public boolean isCancelled()
    {
        return mBasicTaskController.isCancelled();
    }

    public boolean isDone()
    {
        return mBasicTaskController.isDone();
    }
}
