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
 * Cancellable should be used by any task that wants to be cancellable. Task
 * implementing this interface don't actually have to supply a thread. They
 * simply be able to be used from any thread or not even use a thread but be
 * truely asynchronous. Implementors of this task should be aware that cancel()
 * can be called from any thread and that it may be called twice. The task
 * should stop executing as soon as possible and free up any resources when this
 * method is called. Other than those two conditions the implementor is not
 * required to do anything else although it is polite and sensible to specify
 * exactly what the implementor will do when this method is called.
 */
public interface Cancellable
{
    /**
     * Tells the task represented by this object to stop and free its resources
     * as soon as possible. This method may be called on any thread and may be
     * called more than once.
     * <p>
     * NOTE: It is a very, very good idea to have the first line of the
     * cancellable task check to see if the task was cancelled before starting
     * the rest of the task.
     */
    public void cancel();
    
    public static final Cancellable NULL_INSTANCE = new Cancellable()
    {
        public void cancel() {}
    };
}
