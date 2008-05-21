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
 * This class is a version of Java 1.5's "Future" interface. It has most of the
 * methods but drops thing that cant be done in java 1.4. Also it uses the
 * "Cancellable" interface which allows asynchronous tasks to be cancelled. The
 * parts of this class that overlap Java 1.5 should be harmonized.
 */
public interface TaskController extends Cancellable
{
    /** Attempts to cancel execution of this task. */
    public void cancel();

    /**
     * Waits if necessary for the computation to complete, and then retrieves
     * its result. If the computation threw an Exception then this method will
     * throw an ExecutionException wrapping the original exception. If the
     * computation was cancelled this function will throw a
     * CancellationException etc..
     * <p>
     * NOTE: calling this function means the result will be returned and as such
     * the task will be "done" irrespective of whether a result has been
     * returned via a callback.
     */
    public Object get() throws ExecutionException, InterruptedException, CancellationException;

    /** Returns true if this task was cancelled before it completed normally. */
    public boolean isCancelled();

    /**
     * Returns true if this task completed. Completion may be due to normal
     * termination, an exception, or cancellation -- in all of these cases, this
     * method will return true.
     * <p>
     * "done" means that there is a result (ergo that get() has been called or
     * the callback has been called). A TASK IS DONE ONLY AFTER IT RETURNS A
     * RESULT! This means you can't "poll" the TaskController until the task is
     * done.
     */
    public boolean isDone();
}
