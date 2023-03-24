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

import java.awt.EventQueue;

/**
 * Invoker is used to decide which thread (and how) a runnable representing a
 * callback should be dispatched. In the concurrent library it's used so that
 * those using making a call to an sync. Function can decide how they want to
 * receive the callback.
 */
public interface Invoker
{
    /**
     * Special invoker to communicate that you don't want a callback gosh darn
     * it! Using this in conjunction with a BasicTaskController (Which is the
     * chunk of code that will probably end up doing the callback anyway)
     * without using "get()" will result in a task that is "committed" but never
     * "done"..
     */
    public static final Invoker NULL_INVOKER = new Invoker() { public void invoke( Runnable runnable ) {} };
    
    public static final Invoker EVENT_THREAD_INVOKER = new Invoker()
    {
        public void invoke( Runnable runnable )
        {
            EventQueue.invokeLater( runnable );
        }
    };

    /**
     * Do not use with the BasicTaskController (or with anything that goes
     * anywhere near it) as using it with the BasicTaskController would violate
     * the CallListener's callback semantics! And that would cause all sort of
     * library code to fail in surprising and very confusing ways!..
     * 
     * @see CallListener
     */
    public static final Invoker SYNCHRONOUS_INVOKER = new Invoker()
    {
        public void invoke( Runnable runnable )
        {
            runnable.run();
        }
    };
    
    /**
     * Call to plunk a Runnable on a different thread.
     * 
     * @param runnable
     */
    public void invoke( Runnable runnable );
}
