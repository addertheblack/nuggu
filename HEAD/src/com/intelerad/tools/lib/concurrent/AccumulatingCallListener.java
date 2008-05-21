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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.intelerad.tools.lib.concurrent.ConcurrencyLogManager.Logger;


/**
 * Converts between a CompositeCallListeners and a CallListener interface. The
 * "result" object consists of a Map of objects from "context" to result.
 * <p>
 * NOTE: for this object to wrap correctly, source objects need to be unique per
 * asynchronous callable else results from one Asychronous function will
 * over-write the results from a previous one.
 * <p>
 * NOTE: Exceptions are returned wrapped in ExecutionExceptions in the list of
 * results!
 * <p>
 * NOTE 2: handleException() of the CallListener interface is never called.
 * 
 * @see CallAdapter
 * @see CallListener
 * @see CompositeCallListener
 */

public class AccumulatingCallListener implements CompositeCallListener
{
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
    * 
    * 
    */
    private Map mResults;
    private CallListener mFinalListener;
    
    public AccumulatingCallListener( CallListener listener )
    {
        mResults = new HashMap();
        mFinalListener = listener;
    }
    
    public void handlePartialSuccess( Object result, Object context )
    {
        mResults.put( context, result );
    }
    
    public void handlePartialException( Exception exception, Object context )
    {
        mResults.put( context, new ExecutionException( exception ) );
    }
    
    public void handleCancel()
    {
        mResults = null;
        mFinalListener.handleCancel();
    }
    
    public void handleFinally()
    {
        if ( mResults != null )
            mFinalListener.handleSuccess( mResults );
        mFinalListener.handleFinally();
    }

    /**
     * Can get you the list of all ExecutionExceptions. If the returned list is
     * of length 0, no errors occurred in the getting of the series so a little
     * party is probably in order.
     * 
     * @param resultMap
     * @return list of exceptions
     */
    public static List extractExceptions( Map resultMap )
    {
        ArrayList exceptions = new ArrayList();
        for ( Iterator iter = resultMap.values().iterator(); iter.hasNext(); )
        {
            Object partialresult = iter.next();
            if ( ! ( partialresult instanceof ExecutionException ) )
                continue;
            exceptions.add( partialresult );
        }    
        
        return exceptions;
    }
    
    /**
     * Iterate through the results returned by an AccumulatingCallListener 
     * and log any exceptions found.  Return true if any exceptions were found.
     * 
     * @param resultMap Map of results as returned by AccumulatingCallListener
     *        
     * @return true if the resultMap includes any exceptions
     */
    public static boolean logErrors( String message, Map resultMap )
    {
        boolean errorsFound = false;
        
        Logger logger = ConcurrencyLogManager.getDefault();
        for ( Iterator iter = resultMap.entrySet().iterator(); iter.hasNext(); )
        {
            Entry entry = (Entry) iter.next();
            if ( entry.getValue() instanceof ExecutionException )
            {
                errorsFound= true;
                logger.printException( message + " : " + entry.getKey(), (Throwable) entry.getValue() );
            }
        }
        
        return errorsFound;
    }

    /**
     * Can get you the list of all results in a result map (it will exclude
     * ExecutionExceptions).
     * <p>
     * If the result map is <code>key -> Collection</code> then the
     * Collections will be merged into one List and returned (no recursive
     * merging!). Otherwise the list returned will contain a list of results.
     * 
     * @param resultMap
     * @return list of results
     */
    public static List extractResults( Map resultMap )
    {
        ArrayList results = new ArrayList();
        for ( Iterator iter = resultMap.values().iterator(); iter.hasNext(); )
        {
            Object partialResult = iter.next();
            if ( partialResult instanceof Throwable )
                continue;
            else if ( partialResult instanceof Collection )
                results.addAll( (Collection) partialResult );
            else
                results.add( partialResult );
        }    
        
        return results;
    }
}
