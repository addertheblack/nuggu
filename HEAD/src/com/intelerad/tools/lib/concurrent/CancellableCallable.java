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
 * Version of Callable which can be Cancelled.
 * <p>
 * the intention of this class is to be used with something like
 * WorkerController, so that "tasks" can be cancelled. If it is used alone be
 * aware that:
 * <p>
 * 1) cancel() may be called on any arbitrairy thread and more than once.<br>
 * 2) Only one thread of execution should be running in call()<br>
 * 3) When cancel() is called, the call method (and the 1 thread running inside
 * it) should return as soon as possible with a CancellationException on a best
 * effort basis.<br>
 * 4) CancellableCallables are 1 shot. Once they are cancelled, they aren't ment
 * to be restarted.<br>
 * 5) There is no guarentee that a callable won't return with a result or
 * specific exception/error after it has been cancelled owning to the difficulty
 * in honouring such a contract and the uselessness in practice of doing so
 * (it's only usfull to tell exactly when a task has been cancelled from the
 * thread (or the lock) that has done the cancelling.. otherwise there is some
 * ambiguity about when the task was told to cancel vs when it finished and was
 * simply pre-empted before being able to do something usefull with the result.
 * This means you might end up with a callable (or task using the callable) that
 * returns (or does something usefull with) a valid result after it has been
 * told to cancel since it was processing the result when it was pre-empted and
 * told to cancel. This means that from a practical standpoint having some hard
 * constraint about what the call method should return when it's been cancelled
 * is meaningless since a race condition will exist in the caller of the
 * callable that invalidates the utility of such a guarentee anyway.)<br>
 */
public interface CancellableCallable extends Callable, Cancellable
{

}
