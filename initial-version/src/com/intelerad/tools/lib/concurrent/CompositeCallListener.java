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
 * Note: This interface can be used by any asynchronous function that returns
 * partial results.
 * 
 * @see com.intelerad.tools.lib.concurrent.AsynchronousCallableExecutor
 */
public interface CompositeCallListener
{
    public static final CompositeCallListener NULL_LISTENER = new CompositeCallAdapter();
    
    /**
     * Is called by sub-tasks as they finish their work. Only one of handlePartialSuccess() or
     * handlePartialFailure() is called per task.
     * 
     * @param partialResult of one of the sub tasks
     * @param context object associated with this partial result
     */
    public void handlePartialSuccess( Object partialResult, Object context );
    
    /**
     * Is called by sub-tasks if it gets an exception. Only one of handlePartialSuccess() or
     * handlePartialFailure() is called per task.
     * 
     * @param ex throw by the sub-task
     * @param context
     */
    public void handlePartialException( Exception ex, Object context );
    
    /**
     * Is called if the CompositeAsynchronousFunction is cancelled. After this
     * function is called, handleFinally() will be called once and nothing else
     * will be called.
     * <p>
     * handleCancel() may be called EVEN IF ALL RESULTS HAVE COME IN (but before handleFinally())!!
     */
    public void handleCancel();
    
    /**
     * This is called once. When this is called there will be no more calls to
     * any more methods in this interface. It effectively signals the end of all
     * tasks associated with this interface.
     * <p>
     * <b> Will be called on the same runnable as handleCancel() (if handleCancel() is called)</b>
     */
    public void handleFinally();
}