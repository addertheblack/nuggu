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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class implements all of the tedious, error prone, mind bending stuff
 * required to implement a TaskController as well as ensuring that the
 * CallListener callback is done correctly. For the low, low price of using this
 * class, you too can have your asynchronous function return a TaskController
 * instead of a cancellable. Here's how:
 * <p>
 * First create and instance of this call with the CallListener the client sent
 * to your async function. (this class takes care of dispatching handleXXX()
 * events for you).
 * <p>
 * Second step is to associate a cancellable with this AbstractTaskConroller
 * instance by means of the addCancellable(). (If your asynchronous function is
 * implemented by calling its own set of asynchronous functions you can make use
 * of the incredibly convenient startnewTask().
 * <p>
 * Third, if you used addCancellable() remember to use removeCancellable() when
 * the task is over to remove the Cancellable from the list of currently active
 * cancellables.
 * <p>
 * Forth: When you asynchronous task has finished its thing or thrown an
 * exception, using setResult() or setException() to tell the
 * AbstractTaskController about it. The AbstractTaskController will call the
 * right methods on the CallListener.
 * <p>
 * <b>NOTE:</b> When setResult() or setException() is called - all currently
 * running tasks will be cancelled and all tasks started afterwards will be
 * immediately cancelled. It is not polite to add a task to the
 * TaskControllerHelper after setResult() or setException has been called.
 * Client are encouraged to check the "isDone()" to see if the task has been
 * completed <b>if</b> there is a chance that setResult() or setException()
 * will be called before the task has run its course. *
 * <p>
 * <b>NOTE 2:</b> If the task is open-ended, clients can call isEmpty() to
 * check if there are no currently running tasks. Depending on the organization
 * of the task represented by this TaskControllerHelper,
 * 
 * FIXME: document this class, and its methods
 */
public final class TaskControllerHelper
{
    /** CallListener (or CompositeCallListener) to Cancellable */
    private Map mOutstandingTasks = new HashMap();
    
    private TaskControllerHelperListener mHelperListener;

    private BasicTaskController mBasicTaskController;
    
    public TaskControllerHelper( CallListener callListener, Invoker invoker, TaskControllerHelperListener listener )
    {
        Cancellable cancellable = new Cancellable()
        {
            public void cancel()
            {
                TaskControllerHelper.this.cancel();
            }
        };
        mBasicTaskController = new BasicTaskController( cancellable, callListener, invoker );
        mHelperListener = listener;
    }
    
    public TaskControllerHelper( CallListener callListener, Invoker invoker )
    {
        this( callListener, invoker, null );
    }
    
    public CompositeCallListener wrapComposite( final CompositeCallListener listener )
    {
        CompositeCallListener internalCallListener = new CompositeCallListenerWrapper( listener )
        {
            public void handleFinally() 
            {
                try 
                {
                    super.handleFinally();
                }
                finally
                {
                    removeCancellablePrivate( this );
                }
            };
        };
        
        return internalCallListener;
    }
    
    public CallListener wrap( final CallListener listener )
    {
        CallListener internalCallListener = new CallListenerWrapper( listener )
        {
            public void handleFinally() 
            {
                try 
                {
                    super.handleFinally();
                }
                finally
                {
                    removeCancellablePrivate( this );
                }
            };
        };
        
        return internalCallListener;
    }
    
    /**
     * This method is, by far, the easiest way to use TaskControllerHelper.
     * Simply pass the asynchronous task to be executed and everything else is
     * done for you - the nasty, tricky, error prone problem of gluing together
     * the CallListener with code to remove the related TaskController as well
     * as adding the TaskController to the list of cancellable items. *
     * <p>
     * Is this TaskControllerHelper is "done", the task is cancelled as soon as
     * it is started.
     * 
     * @param callable
     * @param listener
     * @return
     */
    public TaskController startNewTask( AsynchronousCallable callable, CallListener listener )
    {
        CallListener wrapperCallListener = wrap( listener );
        
        TaskController taskController = callable.call( wrapperCallListener );
        addCancellablePrivate( wrapperCallListener, taskController );
        return taskController;
    }
    
    /**
     * This method is, by far, the easiest way to use TaskControllerHelper.
     * Simply pass the asynchronous task to be executed and everything else is
     * done for you - the nasty, tricky, error prone problem of gluing together
     * the CallListener with code to remove the related TaskController as well
     * as adding the TaskController to the list of cancellable items.
     * <p>
     * Is this TaskControllerHelper is "done", the task is cancelled as soon as
     * it is started.
     * 
     * @param callable
     * @param listener
     * @return
     */
    public TaskController startNewTask( CompositeAsynchronousCallable callable,
                                        CompositeCallListener listener )
    {
        CompositeCallListener wrapperCallListener = wrapComposite( listener );
        
        TaskController taskController = callable.compositeCall( wrapperCallListener );
        addCancellablePrivate( wrapperCallListener, taskController );
        return taskController;
    }
    
    public boolean addCancellable( CallListener key, Cancellable cancellable )
    {
        return addCancellablePrivate( key, cancellable );
    }
    
    public boolean addCancellable( CompositeCallListener key, Cancellable cancellable )
    {
        return addCancellablePrivate( key, cancellable );
    }
    
    private boolean addCancellablePrivate( Object key, Cancellable cancellable )
    {
        synchronized ( mBasicTaskController )
        {
            if ( mOutstandingTasks.containsKey( key ) )
                throw new IllegalStateException( "Duplicate key" );
            mOutstandingTasks.put( key, cancellable );
            if ( mBasicTaskController.isCancelled() )
                cancellable.cancel();
            return mBasicTaskController.isCancelled();
        }
    }

    private boolean removeCancellablePrivate( Object key )
    {
        synchronized ( mBasicTaskController )
        {
            mOutstandingTasks.remove( key );
            /*
             * isCommited() is needed here because we need to know if
             * setResult(), setException() or cancel() has been called so that
             * we can obey the contract for endOfTasks()
             */
            if ( isEmpty() && !mBasicTaskController.isCancelled()
                 && !mBasicTaskController.hasTaskReturnedAResult() )
            {
                /*
                 * Throwing an exception in endOfTasks() can mask an exception
                 * thrown in a CallListener. :(
                 */
                if ( mHelperListener != null )
                    mHelperListener.endOfTasks( this );

                /*
                 * Do not throw an exception on this error. This method is
                 * called in a finally block. it's common to get this error
                 * because an unexpected exception was thrown. Throwing an
                 * exception in here simply masks that exception.
                 */
                if ( !mBasicTaskController.hasTaskReturnedAResult() )
                    ConcurrencyLogManager.getDefault()
                                .printException( "",
                                                 new IllegalStateException( "setResult(), setException() or cancel() must be called in endOfTasks()!" ) );
            }
            return mBasicTaskController.isCancelled();
        }
    }
    
    public boolean removeCancellable( CompositeCallListener key )
    {
        return removeCancellablePrivate( key );
    }
    
    public boolean removeCancellable( CallListener key )
    {
        return removeCancellablePrivate( key );
    }
    
    public boolean isEmpty()
    {
        synchronized ( mBasicTaskController )
        {
            return mOutstandingTasks.size() == 0;
        }
    }
    
    /**
     * Call setResult() when the task using this AbstractTaskController has
     * computed its result. Note, the method can be called even when there are
     * still tasks outstanding. In the case it acts as a "return" statement.
     * When setResult() is called, those tasks are immediately cancelled and the
     * task's state becomes "done".
     * <p>
     * use of this method guarantees that handleSuccess() will be called (unless
     * task is already done).
     * 
     * @param results
     *            result to send to handleSuccess(). "null" is perfectly
     *            acceptable.
     */
    public void setResult( Object result )
    {
        synchronized ( mBasicTaskController )
        {
            if ( mBasicTaskController.hasTaskReturnedAResult() )
                return;
            mBasicTaskController.setResult( result );
            cancelOutstanding();
        }
    }
    
    /**
     * Call setException() when the task using this AbstractTaskController has
     * thrown an unrecoverable exception. Note, the method can be called even
     * when there are still tasks outstanding. In the case it acts as a "throws"
     * statement. When setResult() is called, those tasks are immediately
     * cancelled and the task's state becomes "done".
     * <p>
     * use of this method guarantees that handelException() will be called
     * (unless task is already done).
     * 
     * @param exception
     *            exception to send to handelException().
     */
    public void setException( Exception exception )
    {
        synchronized ( mBasicTaskController )
        {
            if ( mBasicTaskController.hasTaskReturnedAResult() )
                return;
            mBasicTaskController.setException( exception );
            cancelOutstanding();
        }
    }

    /**
     * Call cancel() when the task using this AbstractTaskController has been
     * cancelled. Generally this method is called through the TaskController
     * returned by the method getTaskController() and should generally not be
     * called by the asynchronous task code itself. Note, the method can be
     * called even when there are still tasks outstanding. In the case it acts
     * as some sort of unconditional break (return as soon as possible!). When
     * cancel() is called, those tasks are immediately cancelled and the task's
     * state becomes "done". in addition the cancelled state is set to true.
     * <p>
     * use of this method guarantees that handelCancel() will be called (unless
     * task is already done).
     * 
     * @param exception
     *            exception to send to handelException().
     */
    private void cancel()
    {
        synchronized ( mBasicTaskController )
        {
            cancelOutstanding();
        }
    }

    private void cancelOutstanding()
    {
        for ( Iterator iter = new ArrayList( mOutstandingTasks.values() ).iterator(); iter.hasNext(); )
        {
            try
            {
                ( (Cancellable) iter.next() ).cancel();
            }
            catch ( Throwable t )
            {
                ConcurrencyLogManager.getDefault().printException( "Error cancelling" , t );
            }
        }
    }
    
    /**
     * @return The TaskController associated with this TaskControllerHelper.
     */
    public TaskController getTaskController()
    {
        return mBasicTaskController;
    }
    
    public interface TaskControllerHelperListener
    {
        /**
         * This method of this interface is called when the TaskController has
         * been "started" (ie: has had a cancellable task added) and all tasks
         * are finished (ie: isEmpty() returns true. Clients who implement this
         * method *must* call either setException() or setResult() during the
         * course of this method's execution. Clients are not permitted to start
         * a new task after this method has been called. This method will only
         * be called once by TaskControllerHelper when it realise that there
         * are not outstanding tasks. Its call is triggered from
         * removeCancellable() (if used) or after handleFinally() has been
         * called on the last task's (composite or otherwise) CallListener. This
         * method will not be called if either setException() or setResult()
         * were called before the last task finished.
         * 
         * @param taskControllerHelper
         */
        public void endOfTasks( TaskControllerHelper taskControllerHelper );
    }
}
