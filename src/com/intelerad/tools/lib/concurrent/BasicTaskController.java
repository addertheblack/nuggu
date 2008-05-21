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


public final class BasicTaskController implements TaskController
{
    private CallListener mListener;
    private Invoker mInvoker;
    
    /** The task has finished and returned a result or exception */
    private boolean mTaskHasReturnedAResult;

    /**
     * The task has finished and returned a result or exception and the result
     * has been returned using the callback or the get() thereby making it
     * impossible to cancel this task
     */
    private boolean mCommittedToAResult;
    
    private boolean mCancelled;
    
    private Object mResult;
    private Throwable mException;
    private Cancellable mCancellable;
    private FunctionResult mGetFunctionResult;
    
    
    /**
     * @param cancellable is called when cancel() is called on this TaskController.
     * @param listener
     * @param invoker
     */
    public BasicTaskController( Cancellable cancellable, CallListener listener, Invoker invoker )
    {
        if ( invoker == Invoker.SYNCHRONOUS_INVOKER )
            throw new IllegalArgumentException( "The synchronous invoker can never, ever, under" +
                    " any circumstances be used with the BasicTaskController. Ever." +
                    " See Invoker and CallListener javadocs." );
        
        mListener = listener;
        mCancellable = cancellable;
        mInvoker = invoker;
    }
    
    /**
     * Call this when your asynchronous function has computed its result. 
     * 
     * @param result
     */
    public final synchronized void setResult( Object result )
    {
        if ( hasTaskReturnedAResult() ) 
            return;
        mResult = result;
        fireRunnableIfNeeded();
    }
    
    public final synchronized void setException( Throwable ex )
    {
        if ( hasTaskReturnedAResult() ) 
            return;
        
        boolean isAnError = ( ex instanceof Error );
        
        if ( !isAnError && !( ex instanceof Exception ) )
            throw new IllegalArgumentException( "Throwable argument must be either an exception or an Error - " + ex );
        
        if ( isAnError )
            mException = new UnexpectedErrorException( ex ); //part of a fix to stop Errors from causing havoc in CallableUtilities.execute
        else
            mException = ex;
        fireRunnableIfNeeded();
    }

    /**
     * @return true if a setResult() or setException() or cancel() has been called. 
     */
    final synchronized boolean hasTaskReturnedAResult()
    {
        return mTaskHasReturnedAResult;
    }
    
    private final synchronized void fireRunnableIfNeeded()
    {
        if ( hasTaskReturnedAResult() ) 
            return;

        mTaskHasReturnedAResult = true;
        notifyAll();
        Runnable finallyRunnable = new Runnable()
        {
            public void run()
            {
                try
                {
                    /*
                     * we only choose which of the handleXXX() to call at the
                     * time when the callback is being made because someone
                     * might have cancelled during the time when the callback
                     * was made and when it executes. This allows us to
                     * guarantee that if you are cancelling on the invoker
                     * thread that you will get handleCancel() called.
                     */

                    getRunnable().run(); 
                }
                finally
                {
                    mListener.handleFinally();
                }
            }
        };
        mInvoker.invoke( finallyRunnable );
    }
    
    private final Runnable getRunnable()
    {
        try 
        {
            final Object result = getPrivate();
            return new Runnable() 
            {
                public void run()
                {
                    mListener.handleSuccess( result );
                }
            };
        }
        catch ( CancellationException ex )
        {
            return new Runnable() 
            {
                public void run()
                {
                    mListener.handleCancel();
                }
            };
        }
        catch ( final ExecutionException ex )
        {
            return new Runnable()
            {
                public void run()
                {
                    if ( ex.getCause() instanceof Exception )
                        mListener.handleException( (Exception) ex.getCause() );
                    else
                        mListener.handleException( ex );
                }
            };
        }
        catch ( Throwable ex )
        {
            throw new RuntimeException( ex );
        }
    }

    /////////////////// TaskController \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
    public final void cancel()
    {
        synchronized ( this )
        {
            if ( mCommittedToAResult )
                return;
            if ( mCancelled )
                return;
            mCancelled    = true;
            fireRunnableIfNeeded();
        }
        
        try
        {
            mCancellable.cancel();
        }
        catch ( Throwable t )
        {
            ConcurrencyLogManager.getDefault().printException( "Unexpected exception while cancelling", t );
        }
    }
    
    public final synchronized Object get() throws ExecutionException, InterruptedException
    {
        while ( ! hasTaskReturnedAResult() )
            wait();
        
        return getPrivate();
    }

    /**
     * @return
     * @throws ExecutionException
     */
    private final synchronized Object getPrivate() throws ExecutionException, CancellationException
    {
        mCommittedToAResult = true;
        
        /*
         * This bizarre mess to to make sure that both the CallListener and get()
         * function return the same thing. We use the result of whoever asked
         * first. The contract is the result is cancellable until it is
         * delivered. Being delivered via a get() and being delivered through a
         * callback are of equivalent worth.
         */
        if ( mGetFunctionResult == null )
        {
            Callable callable = new Callable()
            {
                public Object call() throws Exception
                {
                    if ( isCancelled() )
                        throw new CancellationException();

                    if ( mException != null )
                        throw new ExecutionException( mException );

                    return mResult;
                }
            };

            /* Execute and save the function's result */
            mGetFunctionResult = FunctionResult.saveFunctionResult( callable );
        }


        try 
        {
            /* Replay the function result */
            return mGetFunctionResult.call();
        }
        catch ( ExecutionException ex )
        {
            throw ex;
        }
        catch ( CancellationException ex )
        {
            throw ex;
        }
        catch ( Exception ex ) //should never happen
        {
            throw new RuntimeException( ex );
        }
    }
    


    public synchronized boolean isCancelled()
    {
        return mCancelled;
    }

    public synchronized boolean isDone()
    {
        return mTaskHasReturnedAResult;
    }
    
    /**
     * Cheap hack to stop Errors from causing massive internal problems.
     */
    private static class UnexpectedErrorException extends RuntimeException
    {
        public UnexpectedErrorException( Throwable ex )
        {
            super( ex );   
        }
    }
}

