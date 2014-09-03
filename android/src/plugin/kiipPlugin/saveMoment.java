//
//  saveMoment.java
//  Kiip Plugin
//
/*
The MIT License (MIT)

Copyright (c) 2014 Ladeez First Media

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
// ----------------------------------------------------------------------------

// Package name
package plugin.kiip;

// Android Imports
import android.content.Context;

// JNLua imports
import com.naef.jnlua.LuaState;
import com.naef.jnlua.LuaType;

// Corona Imports
import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeTask;
import com.ansca.corona.CoronaRuntimeTaskDispatcher;
import com.ansca.corona.storage.FileContentProvider;

// Java/Misc Imports
import java.math.BigDecimal;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.io.File;

// Android Imports
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.R;
import android.R.drawable;

// Kiip Imports
import me.kiip.sdk.Kiip;
import me.kiip.sdk.Notification;
import me.kiip.sdk.Kiip.Callback;
import me.kiip.sdk.Kiip.OnContentListener;
import me.kiip.sdk.Kiip.OnSwarmListener;
import me.kiip.sdk.Poptart;
import me.kiip.sdk.Modal;
import me.kiip.sdk.Notification;
import me.kiip.sdk.Notification.OnClickListener;
import me.kiip.sdk.Notification.OnDismissListener;
import me.kiip.sdk.Notification.OnShowListener;
import android.content.DialogInterface;
import me.kiip.sdk.*;
import me.kiip.*;

/**
 * Implements the saveMoment() function in Lua.
 * <p>
 * Used for initializing the Kiip Plugin.
 */
public class saveMoment implements com.naef.jnlua.NamedJavaFunction 
{
    /**
     * Gets the name of the Lua function as it would appear in the Lua script.
     * @return Returns the name of the custom Lua function.
     */
    @Override
    public String getName()
    {
        return "saveMoment";
    }

    // Pointer to the lua state
    LuaState theLuaState = null;

    // Save Moment Event task
    private static class saveMomentLuaCallBackListenerTask implements CoronaRuntimeTask 
    {
        private int fLuaListenerRegistryId;
        private String fPhase = null;

        public saveMomentLuaCallBackListenerTask( int luaListenerRegistryId, String phase ) 
        {
            fLuaListenerRegistryId = luaListenerRegistryId;
            fPhase = phase;
        }

        @Override
        public void executeUsing( CoronaRuntime runtime )
        {
            try 
            {
                // Fetch the Corona runtime's Lua state.
                final LuaState L = runtime.getLuaState();

                // Dispatch the lua callback
                if ( CoronaLua.REFNIL != fLuaListenerRegistryId ) 
                {
                    // Setup the event
                    CoronaLua.newEvent( L, "kiip" );

                    // Event type
                    L.pushString( "saveMoment" );
                    L.setField( -2, "type" );

                    // Status
                    L.pushString( fPhase );
                    L.setField( -2, "phase" );

                    // Dispatch the event
                    CoronaLua.dispatchEvent( L, fLuaListenerRegistryId, 0 );
                }
            }
            catch ( Exception ex ) 
            {
                ex.printStackTrace();
            }
        }
    }


    // Our kiip callback class
    public class KiipCallback implements Callback, OnContentListener, OnSwarmListener, OnShowListener, OnClickListener, OnDismissListener, me.kiip.sdk.Modal.OnShowListener, me.kiip.sdk.Modal.OnDismissListener, me.kiip.sdk.Poptart.OnShowListener, me.kiip.sdk.Poptart.OnDismissListener
    {
        // Callback interface
        @Override
        public void onFailed( Kiip arg0, Exception exception )
        {
        }

        @Override
        public void onFinished( Kiip arg0, Poptart poptart )
        {
        }

        // OnContentListener interface
        @Override
        public void onContent( Kiip arg0, String momentId, int quantity, String transactionId, String signature )
        {
        }

        // OnSwarmListener interface
        @Override
        public void onSwarm( Kiip arg0, String id )
        {
        }

        // Notification OnShowListener
        @Override
        public void onShow( Notification note )
        {
        }

        // Notification OnClickListener
        @Override
        public void onClick( Notification note )
        {
            //System.out.println( "notification shown" );
        }

        // Notification OnDismissListener
        @Override
        public void onDismiss( Notification note )
        {
            //System.out.println( "notification dismissed" );
        }

        // Modal OnShowListener
        @Override
        public void onShow( Modal modal )
        {
            //System.out.println( "modal shown" );
            /* 
            We don't listen for this event as kiip classes the poptart as the "whole" event, so listening for this event just 
            adds needless stacked events, since they fire at exactly the same time.
            */
        }

        // Modal OnDismissListener
        @Override
        public void onDismiss( Modal modal )
        {
            // Debug
            //System.out.println( "modal dismissed" );

            /* 
            We don't listen for this event as kiip classes the poptart as the "whole" event, so listening for this event just 
            adds needless stacked events, since they fire at exactly the same time.
            */
        }
        
        // Poptart OnShowListener
        @Override
        public void onShow( DialogInterface dialog )
        {
            // Corona runtime task dispatcher
            final CoronaRuntimeTaskDispatcher dispatcher = new CoronaRuntimeTaskDispatcher( theLuaState );

            // Create the task
            saveMomentLuaCallBackListenerTask task = new saveMomentLuaCallBackListenerTask( listenerRef, "shown" );

            // Send the task to the Corona runtime asynchronously.
            dispatcher.send( task );
        }
        
        // Poptart OnDismissListener
        @Override
        public void onDismiss( DialogInterface dialog )
        {
            // Corona runtime task dispatcher
            final CoronaRuntimeTaskDispatcher dispatcher = new CoronaRuntimeTaskDispatcher( theLuaState );

            // Create the task
            saveMomentLuaCallBackListenerTask task = new saveMomentLuaCallBackListenerTask( listenerRef, "dismissed" );

            // Send the task to the Corona runtime asynchronously.
            dispatcher.send( task );

            // We are not showing a poptart
            kiipPluginStatus.isShowingPoptart = false;
        }
    }

    // Our lua callback listener
    private int listenerRef;

    /**
     * This method is called when the Lua function is called.
     * <p>
     * Warning! This method is not called on the main UI thread.
     * @param luaState Reference to the Lua state.
     *                 Needed to retrieve the Lua function's parameters and to return values back to Lua.
     * @return Returns the number of values to be returned by the Lua function.
     */
    @Override
    public int invoke( LuaState luaState ) 
    {
        try
        {
            // If we are not already showing a poptart
            if ( kiipPluginStatus.isShowingPoptart == false )
            {
                // We are attempting showing a poptart
                kiipPluginStatus.isShowingPoptart = true;
                // This requires an options table with the following params:
                theLuaState = luaState;

                // Get the corona application context
                final Context coronaApplication = CoronaEnvironment.getApplicationContext();
                // Get the corona activity
                final CoronaActivity activity = CoronaEnvironment.getCoronaActivity();

                // The name of the moment
                String momentName = null;
                // The moment value
                double value = 0.0;
                // Has a moment value been specified?
                boolean hasMomentValue = false;

                // If an options table has been passed
                if ( luaState.isTable( -1 ) )
                {
                    // Get the listener field
                    luaState.getField( -1, "listener" );
                    if ( CoronaLua.isListener( luaState, -1, "kiip" ) ) 
                    {
                        // Assign the callback listener to a new lua ref
                        listenerRef = CoronaLua.newRef( luaState, -1 );
                    }
                    luaState.pop( 1 );

                    // Moment name
                    luaState.getField( -1, "momentName" );
                    if ( luaState.isString( -1 ) )
                    {
                        momentName = luaState.checkString( -1 );
                        hasMomentValue = true;
                    }
                    luaState.pop( 1 ); 

                    // value
                    luaState.getField( -1, "value" );
                    if ( luaState.isNumber( -1 ) )
                    {
                        value = luaState.checkNumber( -1 );
                    }
                    luaState.pop( 1 );
                     
                    // Pop the options table
                    luaState.pop( 1 );
                }
                // No options table passed in
                else
                {
                    System.out.println( "Error: kiip.saveMoment(), options table expected, got " + luaState.typeName( -1 ) );
                }

                // Corona Activity
                CoronaActivity coronaActivity = null;
                if ( CoronaEnvironment.getCoronaActivity() != null )
                {
                    coronaActivity = CoronaEnvironment.getCoronaActivity();
                }

                // Corona runtime task dispatcher
                final CoronaRuntimeTaskDispatcher dispatcher = new CoronaRuntimeTaskDispatcher( luaState );

                // Kiip moment properties
                final String kiipMomentName = momentName;
                final double kiipMomentValue = value;

                // Create a new runnable object to invoke our activity
                Runnable runnableActivity = new Runnable()
                {
                    public void run()
                    {
    				    // Create a moment
    					Kiip.getInstance().saveMoment( kiipMomentName, kiipMomentValue, new Kiip.Callback()
    					{
    					    @Override
    						public void onFinished( Kiip kiip, Poptart poptart ) 
    						{
                                // If the poptart isn't null
    						    if ( poptart != null )
    		   					{
                                    // Create an instance of the kiip callback class
                                    KiipCallback kiipCallback = new KiipCallback();
                                    // Set the poptart listeners
                                    poptart.setTag( activity.hashCode() ); // Is this needed?
                                    poptart.setOnShowListener( kiipCallback );
                                    poptart.setOnDismissListener( kiipCallback );

                                    // Notification
                                    final Notification notification = poptart.getNotification();
                                    
                                    // Modal
                                    final Modal modal = poptart.getModal();

                                    // If there is a notification, set the listeners
                                    if ( notification != null )
                                    {
                                        notification.setOnShowListener( kiipCallback );
                                        notification.setOnDismissListener( kiipCallback );
                                    }

                                    // If there is a modal, set the listeners
                                    if ( modal != null )
                                    {
                                        modal.setOnShowListener( kiipCallback );
                                        modal.setOnDismissListener( kiipCallback );
                                    }

                                    // Show the poptart
    						   	 	poptart.show( activity );
    		                    }
    		                    else
    		                    {
                                    // We are not showing a poptart
                                    kiipPluginStatus.isShowingPoptart = false;

                                    // Create the task
                                    saveMomentLuaCallBackListenerTask task = new saveMomentLuaCallBackListenerTask( listenerRef, "noPoptart" );

                                    // Send the task to the Corona runtime asynchronously.
                                    dispatcher.send( task );
    		                    }
    						}

    						@Override
    						public void onFailed( Kiip kiip, Exception exception ) 
    						{
                                // We are not showing a poptart
                                kiipPluginStatus.isShowingPoptart = false;
    						}
    					});               
                    }
                };

                // Run the activity on the uiThread
                if ( coronaActivity != null )
                {
                    coronaActivity.runOnUiThread( runnableActivity );
                }
            }
        }
        catch( Exception ex )
        {
            // An exception will occur if given an invalid argument or no argument. Print the error.
            ex.printStackTrace();
        }
        
        return 0;
    }
}
