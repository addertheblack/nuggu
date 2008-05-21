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

import java.util.concurrent.Semaphore;

/**
 * A grab bag of misc utilities having to do with manipulating asynchronous
 * functions.
 */
public class AsynchronousUtilities
{
    /**
     * Turns an asynchronous function into a blocking function.
     * <p>
     * DEADLOCK WARNING: This should not be used on the same thread that the
     * asynchronous function is returning its result on.
     * 
     * @param call
     *            to block on
     * @return the result of the call (as would be returned by handleSuccess())
     * @throws ExecutionException if the asynchronous call returns an exception
     * @throws CancellationException if the current thread is interrupted.
     */
    public static Object blockForResult( AsynchronousCallable call ) throws ExecutionException,
                                                                            CancellationException
    {
        Semaphore semaphore = new Semaphore( 1 );

        ResultListener resultListener = new ResultListener( semaphore );

        TaskController taskController = call.call( resultListener );
        try
        {
            semaphore.acquire();
        }
        catch ( InterruptedException ex )
        {
            taskController.cancel();
            throw new CancellationException();
        }

        if ( resultListener.mCancelled )
            throw new CancellationException(); // this should be IMPOSSIBLE!
        else if ( resultListener.mException != null )
            throw new ExecutionException( resultListener.mException );
        else
            return resultListener.mResult;
    }

    /**
     * Made for internal use by blockForResult. Is a glorified handler/struct
     * hybrid. This class should not be used to demonstrate proper encapsulation
     * techniques.
     */
    private static class ResultListener implements CallListener
    {
        Exception mException;
        Object mResult;
        boolean mCancelled;

        private final Semaphore mSemaphore;

        public ResultListener( Semaphore semaphore )
        {
            mSemaphore = semaphore;
        }

        public void handleCancel()
        {
            mCancelled = true;
        }

        public void handleException( Exception exception )
        {
            mException = exception;
        }

        public void handleFinally()
        {
            mSemaphore.release();
        }

        public void handleSuccess( Object result )
        {
            mResult = result;
        }
    }
}
