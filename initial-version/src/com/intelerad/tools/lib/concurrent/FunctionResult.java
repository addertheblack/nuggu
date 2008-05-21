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
 * Executes a Callable then stores that function result. Unlike simply storing
 * the returned value, this object can store the exception as well and "play
 * back" the function returning its result at will. This is useful for caching
 * the result of some function.
 * <p>
 * Immutable
 */
public final class FunctionResult implements Callable
{
    private final Object mResult;
    private final Throwable mException;
    
    public static FunctionResult saveFunctionResult( Callable callable )
    {
        try
        {
            return new FunctionResult( callable.call(), null );
        }
        catch ( Throwable ex )
        {
            return new FunctionResult( null, ex );
        }
    }
    
    public static FunctionResult createResult( Object result )
    {
        return new FunctionResult( result, null );
    }
    
    public static FunctionResult createException( Throwable exception )
    {
        return new FunctionResult( null, exception );
    }
    
    /**
     * @param result from the async function
     * @param exception from the async function
     */
    private FunctionResult( Object result, Throwable exception )
    {
        if ( result != null && exception != null ) 
            throw new IllegalArgumentException( "It's not possible to result a result and exception at the same time." );
        
        /* 
         * Exceptions that directly subclass throwable but aren't part of the Error or exception
         * hierarchy are too bizarre for this code to handle
         */
        if ( exception != null && ! (exception instanceof Exception) && !( exception instanceof Error ) )
            throw new IllegalArgumentException( "Exception must be an instance of Error or Exception." );
        
        mException = exception;
        mResult = result;
    }
    
    public boolean isException()
    {
        return ( mException != null );
    }
    
    public Object getResult()
    {
        return mResult;
    }
    
    public Throwable getException()
    {
        return mException;
    }
    
    public Object call() throws Exception
    {
        if ( mException != null )
        {
            if ( mException instanceof Error )
                throw (Error)mException;
            else if ( mException instanceof Exception )
                throw (Exception) mException;
            else
                throw new IllegalStateException( "Function result was a sub-class of Throwable that was not Exception or Error!" );
        }
        
        return mResult;
    }
}
