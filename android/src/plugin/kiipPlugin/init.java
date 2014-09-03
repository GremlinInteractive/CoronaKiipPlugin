//
//  init.java
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

// Java/Misc Imports
import java.math.BigDecimal;
import org.json.JSONException;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.LinkedList;

// Android Imports
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

// Kiip Imports
import me.kiip.sdk.Kiip;
import me.kiip.sdk.Notification;
import me.kiip.sdk.Poptart;

/**
 * Implements the init() function in Lua.
 * <p>
 * Used for initializing the Kiip Plugin.
 */
public class init implements com.naef.jnlua.NamedJavaFunction 
{
    /**
     * Gets the name of the Lua function as it would appear in the Lua script.
     * @return Returns the name of the custom Lua function.
     */
    @Override
    public String getName()
    {
        return "init";
    }

    // Event task
    private static class LuaCallBackListenerTask implements CoronaRuntimeTask 
    {
        private int fLuaListenerRegistryId;
        private String fStatus = null;

        public LuaCallBackListenerTask( int luaListenerRegistryId, String status ) 
        {
            fLuaListenerRegistryId = luaListenerRegistryId;
            fStatus = status;
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
                    CoronaLua.newEvent( L, "license" );

                    // Event type
                    L.pushString( "check" );
                    L.setField( -2, "type" );

                    // Status
                    L.pushString( fStatus );
                    L.setField( -2, "status" );

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
            // This requires an options table with the following params
            /*
                appKey = APPKEY,
                appSecret = APPSECRET,
                email = email,
                gender = gender,
                birthday = birthday,
                alias = alias,
            */

            // Get the corona application context
            Context coronaApplication = CoronaEnvironment.getApplicationContext();

            // Parameters
            String appKey = null;
            String appSecret = null;
            String gender = null;
            String email = null;
            String birthday = null;
            String alias = null;

            // If an options table has been passed
            if ( luaState.isTable( -1 ) )
            {
                //System.out.println( "options table exists" );
                // Get the listener field
                luaState.getField( -1, "listener" );
                if ( CoronaLua.isListener( luaState, -1, "kiip" ) ) 
                {
                    // Assign the callback listener to a new lua ref
                    listenerRef = CoronaLua.newRef( luaState, -1 );
                }
                else
                {
                    // Assign the listener to a nil ref
                    listenerRef = CoronaLua.REFNIL;
                }
                luaState.pop( 1 );

                // Get the app key
                luaState.getField( -1, "appKey" );
                if ( luaState.isString( -1 ) )
                {
                    appKey = luaState.checkString( -1 );
                }
                else
                {
                    System.out.println( "Error: appKey expected, got " + luaState.typeName( -1 ) );
                }
                luaState.pop( 1 );

                // Get the app secret
                luaState.getField( -1, "appSecret" );
                if ( luaState.isString( -1 ) )
                {
                    appSecret = luaState.checkString( -1 );
                }
                else
                {
                    System.out.println( "Error: appSecret expected, got " + luaState.typeName( -1 ) );
                }
                luaState.pop( 1 );
                
                // Get the gender
                luaState.getField( -1, "gender" );
                if ( luaState.isString( -1 ) )
                {
                    gender = luaState.checkString( -1 );
                }
                luaState.pop( 1 );
                
                // Get the email
                luaState.getField( -1, "email" );
                if ( luaState.isString( -1 ) )
                {
                    email = luaState.checkString( -1 );
                }
                luaState.pop( 1 );
                
                // Get the birthday
                luaState.getField( -1, "birthday" );
                if ( luaState.isString( -1 ) )
                {
                    birthday = luaState.checkString( -1 );
                }
                luaState.pop( 1 );
                
                // Get the alias
                luaState.getField( -1, "alias" );
                if ( luaState.isString( -1 ) )
                {
                    alias = luaState.checkString( -1 );
                }
                luaState.pop( 1 );

                // Pop the options table
                luaState.pop( 1 );
            }
            // No options table passed in
            else
            {
                System.out.println( "Error: kiip.init(), options table expected, got " + luaState.typeName( -1 ) );
            }

            // Corona Activity
            CoronaActivity coronaActivity = null;
            if ( CoronaEnvironment.getCoronaActivity() != null )
            {
                coronaActivity = CoronaEnvironment.getCoronaActivity();
            }

            // Corona runtime task dispatcher
            final CoronaRuntimeTaskDispatcher dispatcher = new CoronaRuntimeTaskDispatcher( luaState );

            // Set variables to pass to kiip (need to be final as they are accesed from within an inner class)
            final String kiipAppKey = appKey;
            final String kiipAppSecret = appSecret;
            final String kiipAlias = alias;
            final String kiipGender = gender;
            final String kiipEmail = email;
            final String kiipBirthday = birthday;          

            // Create a new runnable object to invoke our activity
            Runnable runnableActivity = new Runnable()
            {
                public void run()
                {
                    Context context = CoronaEnvironment.getApplicationContext();

                    // Init Kiip
                    Kiip kiip = Kiip.init( (android.app.Application)context, kiipAppKey, kiipAppSecret );
                    // Set the Kiip instance
                    Kiip.setInstance( kiip );

                    // Set Kiip properties
                    // Alias
                    if ( kiipAlias != null ) Kiip.getInstance().setAlias( kiipAlias );
                    // Gender
                    if ( kiipGender != null ) Kiip.getInstance().setGender( kiipGender );
                    // Email
                    if ( kiipEmail != null ) Kiip.getInstance().setEmail( kiipEmail );
                    // Birthday
                    if ( kiipBirthday != null ) 
                    {
                        try
                        {
                            final Date formattedBirthday = new SimpleDateFormat("MM/dd/yyyy").parse( kiipBirthday );
                            Kiip.getInstance().setBirthday( formattedBirthday );
                        }
                        catch ( Exception e )
                        {
                        }
                    }

                    // Start a Kiip session
                    Kiip.getInstance().startSession( new Kiip.Callback()
                    {
                        @Override
                        public void onFailed( Kiip arg0, Exception exception )
                        {   
                        }

                        @Override
                        public void onFinished( Kiip arg0, Poptart poptart )
                        {
                        }
                    });

                    // Create the task
                    LuaCallBackListenerTask task = new LuaCallBackListenerTask( listenerRef, "valid" );

                    // Send the task to the Corona runtime asynchronously.
                    dispatcher.send( task );                    
                }
            };

            // Run the activity on the uiThread
            if ( coronaActivity != null )
            {
                coronaActivity.runOnUiThread( runnableActivity );
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
