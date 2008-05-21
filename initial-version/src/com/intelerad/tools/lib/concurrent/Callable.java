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
 * Taken from java 1.5.
 * 
 * Callable represents a computable result. In general you make callables
 * because you have a function that can return a result but don't want to
 * actually execute it because it takes too long to execute on the current
 * thread.
 * <p>
 * You build Callables to send to a CallableExecutor. CallableExecutors have a
 * method that looks like this:
 * 
 * <pre>
 * public TaskController execute( Callable toExecute, CallListener toNotify, Invoker toUseToNotify );
 * </pre>
 * 
 * Generally you will want to make your Callable cancellable so that it will
 * free resources (like the CPU or the network connections) as quickly as
 * possible when TaskController.cancel() is called. To do this use
 * CancellableCallable.
 * 
 * @see CancellableCallable
 * @see TaskController
 * @see CallableUtilities
 * @see CallListener
 * 
 */
public interface Callable
{
    /**
     * Call this method to start this Callable's Execution on the current thread.
     * 
     * @return an Object of any sort or null
     * @throws Exception
     */
    public Object call() throws Exception;
}
