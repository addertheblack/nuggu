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
 * The AsynchronousCallable is a class represents an asynchronous function call.
 * In general, you make AsynchronousCallables when you want to wrap up an
 * asynchronous function call to be executed at some point in the future.
 *
 * One of the many uses of this class is in conjunction with the AsynchronousCallableExecutor.
 * Another common use is with the TaskControllerHelper.
 * 
 * @see com.intelerad.tools.lib.concurrent.CallListener
 * @see com.intelerad.tools.lib.concurrent.Callable
 * @see com.intelerad.tools.lib.concurrent.TaskControllerHelper
 * @see com.intelerad.tools.lib.concurrent.AsynchronousCallableExecutor
 */
public interface AsynchronousCallable
{
    /**
     * Implementors of this method must obey the "Asynchronous function call" semantics as outlined 
     * in CallListener's class description.
     * 
     * @see CallListener
     * @param callListener
     * @return a TaskController representing the asynchronous task in progress
     */
    public TaskController call( CallListener callListener );
}
