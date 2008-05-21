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
 * This can be replaced in Java 1.5 with an Executor from the API.
 */
class SimpleThreadPool implements Executor
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
    
    
    private synchronized void returnThread( ExecutorThread thread, String threadName )
    {
        if ( mAvailableThreads.size() >= mThreadsToCache )
            thread.close();
        else
        {
            Thread currentThread = Thread.currentThread();
            currentThread.setPriority( Thread.NORM_PRIORITY );
            currentThread.setName( threadName );
            Thread.interrupted();
            mAvailableThreads.addFirst( thread );
        }
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
            String threadName = Thread.currentThread().getName();
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
                returnThread( this, threadName );
            }
        }
        
        synchronized Runnable getRunnable() throws InterruptedException
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