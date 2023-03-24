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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Only allows a certain number of threads to run at the same time. Uses the
 * executor provided to actual run the threads (used instead of a thread
 * factory).
 * 
 * Exists in Java 1.5. sort of.. (The newFixedThreadPool ALLOCATES thread for
 * the task. This implementation uses cached threads if available)
 * 
 * @see Executors newFixedThreadPool(...);
 */
public class BoundedExecutor implements Executor
{
    private LinkedList mQueue = new LinkedList();
    private int mRunnableCount;
    private int mConcurrency;
    private Executor mExecutor;
    
    public BoundedExecutor( int concurrency )
    {
        this( concurrency, new Executor()
        {
            public void execute( Runnable command )
            {
                new Thread( command, "Bounded Executor Thread" ).start();
            }
        } );
    }
    
    public BoundedExecutor( int concurrency, Executor executor )
    {
        mConcurrency = concurrency;
        mExecutor = executor;
    }
    
    public synchronized void execute( Runnable command )
    {
        mQueue.add( command );
        startNewRunnable();
    }
    
    public synchronized void startNewRunnable()
    {
        while ( !mQueue.isEmpty() && mRunnableCount < mConcurrency )
        {
            final Runnable toRun = (Runnable) mQueue.removeFirst();
            mRunnableCount++;

            Runnable wrappingRunnable = new Runnable()
            {
                public void run()
                {
                    try
                    {
                        toRun.run();
                    }
                    finally
                    {
                        endRunnable();
                    }
                }
            };
            mExecutor.execute( wrappingRunnable );
        }
    }
    
    public synchronized void endRunnable()
    {
        mRunnableCount--;
        startNewRunnable();
    }
}
