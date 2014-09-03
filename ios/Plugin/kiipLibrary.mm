// ----------------------------------------------------------------------------
// kiipLibrary.mm
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

#import "kiipLibrary.h"
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <MediaPlayer/MediaPlayer.h>
#import <Accounts/Accounts.h>
#import <AVFoundation/AVFoundation.h>
#import <KiipSDK/KiipSDK.h>

// Corona imports
#import "CoronaRuntime.h"
#include "CoronaAssert.h"
#include "CoronaEvent.h"
#include "CoronaLua.h"
#include "CoronaLibrary.h"

// Kiip delegate
@interface KiipDelegate:UIResponder <UIApplicationDelegate, KiipDelegate, KPPoptartDelegate>
@property (nonatomic) lua_State *L;
@property (nonatomic) Corona::Lua::Ref listenerRef;
@property (nonatomic, assign) Kiip *kiip;
@property (nonatomic, assign) bool isShowingPoptart;
@end

// Pointer to our kiip delegate
KiipDelegate *kiipDelegate;

// ----------------------------------------------------------------------------

@class UIViewController;

namespace Corona
{
	// ----------------------------------------------------------------------------
	
	class kiipLibrary
	{
	public:
		typedef kiipLibrary Self;
		
	public:
		static const char kName[];
		
	public:
		static int Open( lua_State *L );
		static int Finalizer( lua_State *L );
		static Self *ToLibrary( lua_State *L );
		
	protected:
		kiipLibrary();
		bool Initialize( void *platformContext );
		
	public:
		UIViewController* GetAppViewController() const { return fAppViewController; }
		
	public:
		static int init( lua_State *L );
		static int saveMoment( lua_State *L );
		
	private:
		UIViewController *fAppViewController;
		CoronaLuaRef listenerRef;
	};
	
// ----------------------------------------------------------------------------

// This corresponds to the name of the library, e.g. [Lua] require "plugin.library"
const char kiipLibrary::kName[] = "plugin.kiip";

int
kiipLibrary::Open( lua_State *L )
{
	// Register __gc callback
	const char kMetatableName[] = __FILE__; // Globally unique string to prevent collision
	CoronaLuaInitializeGCMetatable( L, kMetatableName, Finalizer );
	
	void *platformContext = CoronaLuaGetContext( L );
	
	// Set library as upvalue for each library function
	Self *library = new Self;
	
	if ( library->Initialize( platformContext ) )
	{
		// Functions in library
		static const luaL_Reg kFunctions[] =
		{
			{ "init", init },
			{ "saveMoment", saveMoment},
			{ NULL, NULL }
		};
		
		// Register functions as closures, giving each access to the
		// 'library' instance via ToLibrary()
		{
			CoronaLuaPushUserdata( L, library, kMetatableName );
			luaL_openlib( L, kName, kFunctions, 1 ); // leave "library" on top of stack
		}
	}
	
	return 1;
}

int
kiipLibrary::Finalizer( lua_State *L )
{
	Self *library = (Self *)CoronaLuaToUserdata( L, 1 );
	delete library;
	
	// Cleanup the kiip delegate
	[kiipDelegate release];
	kiipDelegate = nil;
	
	return 0;
}

kiipLibrary *
kiipLibrary::ToLibrary( lua_State *L )
{
	// library is pushed as part of the closure
	Self *library = (Self *)CoronaLuaToUserdata( L, lua_upvalueindex( 1 ) );
	return library;
}

kiipLibrary::kiipLibrary()
:	fAppViewController( nil )
{
}

bool
kiipLibrary::Initialize( void *platformContext )
{
	bool result = ( ! fAppViewController );
	
	if ( result )
	{
		id<CoronaRuntime> runtime = (id<CoronaRuntime>)platformContext;
		fAppViewController = runtime.appViewController; // TODO: Should we retain?
	}
	
	return result;
}

// Save moment result
static
int kiipSaveMomentResult( lua_State *L, Corona::Lua::Ref listenerRef, const char *phase, bool removeListener, NSError *error = nil )
{
	// Execute the listener
	if ( listenerRef != NULL )
	{
		// Create the event
		Corona::Lua::NewEvent( L, "kiip" );
		lua_pushstring( L, "saveMoment" );
		lua_setfield( L, -2, CoronaEventTypeKey() );
		
		// Push the result
		lua_pushstring( L, phase );
		lua_setfield( L, -2, "phase" );
		
		// If there was an error return the actual error message
		if ( error != nil )
		{
			lua_pushstring( L, [[error localizedDescription] UTF8String] );
			lua_setfield( L, -2, "errorDescription" );
		}
		
		// Dispatch the event with 1 result
		Corona::Lua::DispatchEvent( L, listenerRef, 1 );
		
		// Remove the listener if specified
		if ( removeListener == true )
		{
			// Free native reference to listener
			Corona::Lua::DeleteRef( L, listenerRef );
		
			// Null the reference
			listenerRef = NULL;
		}
	}
	return 0;
}

// [lua] kiip.init()
int
kiipLibrary::init( lua_State *L )
{
	// The app key
	const char *appKey = NULL;
	// The app secret
	const char *appSecret = NULL;
	// Person gender
	const char *gender = NULL;
	// Person email
	const char *email = NULL;
	// Person birthday
	const char *birthday = NULL;
	// Person alias
	const char *alias = NULL;
	
	// Listener reference
	Corona::Lua::Ref listenerRef = NULL;
	
	// If an options table has been passed
	if ( lua_type( L, -1 ) == LUA_TTABLE )
	{
		// Get listener key
		lua_getfield( L, -1, "listener" );
		
		// Set the delegate's listenerRef to reference the Lua listener function (if it exists)
		if ( Lua::IsListener( L, -1, "kiip" ) )
		{
			listenerRef = Corona::Lua::NewRef( L, -1 );
		}
		lua_pop( L, 1 );
		
		// Get the app id
		lua_getfield( L, -1, "appKey" );
		if ( lua_type( L, -1 ) == LUA_TSTRING )
		{
			appKey = lua_tostring( L, -1 );
		}
		lua_pop( L, 1 );
		
		// Get the app secret
		lua_getfield( L, -1, "appSecret" );
		if ( lua_type( L, -1 ) == LUA_TSTRING )
		{
			appSecret = lua_tostring( L, -1 );
		}
		lua_pop( L, 1 );
		
		// Get the gender
		lua_getfield( L, -1, "gender" );
		if ( lua_type( L, -1 ) == LUA_TSTRING )
		{
			gender = lua_tostring( L, -1 );
		}
		lua_pop( L, 1 );
		
		// Get the email
		lua_getfield( L, -1, "email" );
		if ( lua_type( L, -1 ) == LUA_TSTRING )
		{
			email = lua_tostring( L, -1 );
		}
		lua_pop( L, 1 );
		
		// Get the birthday
		lua_getfield( L, -1, "birthday" );
		if ( lua_type( L, -1 ) == LUA_TSTRING )
		{
			birthday = lua_tostring( L,-1 );
		}
		lua_pop( L, 1 );
		
		// Get the alias
		lua_getfield( L, -1, "alias" );
		if ( lua_type( L, -1 ) == LUA_TSTRING )
		{
			alias = lua_tostring( L,-1 );
		}
		lua_pop( L, 1 );
	}
	// No options table passed in
	else
	{
		luaL_error( L, "Error: kiip.init(), options table expected, got %s", luaL_typename( L, -1 ) );
	}
	lua_pop( L, 1 );
	
	// Initialize the kiip delegate
	if ( kiipDelegate == nil )
	{
		// Initialise the delegate
		kiipDelegate = [[KiipDelegate alloc] init];
		
		// We are not showing a poptart by default
		kiipDelegate.isShowingPoptart = false;
	}
	// Create the kiip object
	kiipDelegate.kiip = [[Kiip alloc] initWithAppKey:[NSString stringWithUTF8String:appKey] andSecret:[NSString stringWithUTF8String:appSecret]];
	// Set the kiip delegate
	kiipDelegate.kiip.delegate = kiipDelegate;
	
	// Set properties
	if ( alias != NULL ) kiipDelegate.kiip.alias = [NSString stringWithUTF8String:alias];
	if ( gender != NULL ) kiipDelegate.kiip.gender = [NSString stringWithUTF8String:gender];
	if ( email != NULL ) kiipDelegate.kiip.email = [NSString stringWithUTF8String:email];
	if ( birthday != NULL )
	{
		NSString *value = [NSString stringWithUTF8String:birthday];
		NSError *dateError = nil;
		NSDate *date = nil;
		NSDataDetector *detector = [NSDataDetector dataDetectorWithTypes:NSTextCheckingTypeDate error:&dateError];
		NSArray *matches = [detector matchesInString:value options:0 range:NSMakeRange(0, [value length])];
		
		// Get the date
		for ( NSTextCheckingResult *match in matches )
		{
			date = match.date;
			//NSLog( @"Got date: %@", match.date );
		}
		
		// If we found a date, set it
		if ( date )
		{
			kiipDelegate.kiip.birthday = date;
		}
		else
		{
			NSLog( @"Error: %@", dateError );
		}
	}
	
	// Set kiip's shared instance
	[Kiip setSharedInstance:kiipDelegate.kiip];
	
	// Execute the license listener callback (for backward compat)
	if ( listenerRef != NULL )
	{
		// Create the event
		Corona::Lua::NewEvent( L, "license" );
		lua_pushstring( L, "check" );
		lua_setfield( L, -2, CoronaEventTypeKey() );
		
		// Push the status string
		lua_pushstring( L, "valid" );
		lua_setfield( L, -2, "status" );
		
		// Dispatch the event
		Corona::Lua::DispatchEvent( L, listenerRef, 1 );
		
		// Free native reference to listener
		Corona::Lua::DeleteRef( L, listenerRef );
		
		// Null the reference
		listenerRef = NULL;
	}

	return 0;
}

// [lua] kiip.saveMoment()
int
kiipLibrary::saveMoment( lua_State *L )
{
	// If Kiip has not been initialized
	if ( kiipDelegate == nil )
	{
		luaL_error( L, "Error: You must call first call kiip.init() before calling kiip.saveMoment()\n" );
		return 0;
	}

	// Moment name
	const char *momentName = NULL;
	// The moment value
	double momentValue = 0.0;
	// Has a moment value been specified?
	bool hasMomentValue = false;
	
	// Set the delegates lua state
	kiipDelegate.L = L;
	
	// If an options table was passed in
	if ( lua_type( L, -1 ) == LUA_TTABLE )
	{
		// Get listener key
		lua_getfield( L, -1, "listener" );
		
		// Set the delegate's listenerRef to reference the Lua listener function (if it exists)
		if ( Lua::IsListener( L, -1, "kiip" ) )
		{
			kiipDelegate.listenerRef = Corona::Lua::NewRef( L, -1 );
		}
		lua_pop( L, 1 );
	
		// Get the moment name
		lua_getfield( L, -1, "momentName" );
		if ( lua_type( L, -1 ) == LUA_TSTRING )
		{
			momentName = lua_tostring( L, -1 );
		}
		lua_pop( L, 1 );
		
		// Get the value
		lua_getfield( L, -1, "value" );
		if ( lua_type( L, -1 ) == LUA_TNUMBER )
		{
			momentValue = lua_tonumber( L, -1 );
			hasMomentValue = true;
		}
		lua_pop( L, 1 );
		
		// Get the custom banner
		lua_getfield( L, -1, "customBanner" );		
		if ( lua_type( L, -1 ) == LUA_TTABLE )
		{
			printf( "Kiip Plugin: Customer banner support has been removed, due to Kiip no longer supporting it. If you still wish to provide a custom banner before showing a Kiip interstitial, please use corona's display.* api's to do so.\n" );
		}
		lua_pop( L, 1 );
	}
	lua_pop( L, 1 );
	
	// Only attempt to show a poptart if we aren't currently trying to show one
	if ( kiipDelegate.isShowingPoptart == false )
	{
		// Set showing a poptart to true
		kiipDelegate.isShowingPoptart = true;
		
		// If a moment has a value
		if ( hasMomentValue == true )
		{
			// Save a moment
			[[Kiip sharedInstance] saveMoment:[NSString stringWithUTF8String: momentName ] value:momentValue withCompletionHandler:^(KPPoptart *poptart, NSError *error)
			{
				// If we got an error
				if ( error )
				{
					// Execute the listener
					kiipSaveMomentResult( L, kiipDelegate.listenerRef, "error", true, error );
					// Set showing a poptart to false
					kiipDelegate.isShowingPoptart = false;
				}
				else
				{
					// If the poptart can be displayed
					if ( poptart )
					{
						// Set the poptart delegate
						poptart.delegate = kiipDelegate;
						
						// Show the poptart
						[poptart show];
					}
					// No poptart to show
					else
					{
						// Execute the listener
						kiipSaveMomentResult( L, kiipDelegate.listenerRef, "noPoptart", true, error );
						// Set showing a poptart to false
						kiipDelegate.isShowingPoptart = false;
					}
				}
			}];
		}
		// No value, save the moment without one
		else
		{
			[[Kiip sharedInstance] saveMoment:[NSString stringWithUTF8String: momentName ]withCompletionHandler:^(KPPoptart *poptart, NSError *error)
			 {
				// If we got an error
				if ( error )
				{
					// Execute the listener
					kiipSaveMomentResult( L, kiipDelegate.listenerRef, "error", true, error );
					// Set showing a poptart to false
					kiipDelegate.isShowingPoptart = false;
				}
				else
				{
					// If the poptart can be displayed
					if ( poptart )
					{
						// Set the poptart delegate
						poptart.delegate = kiipDelegate;
						
						// Show the poptart
						[poptart show];
					}
					// No poptart to show
					else
					{
						// Execute the listener
						kiipSaveMomentResult( L, kiipDelegate.listenerRef, "noPoptart", true, error );
						// Set showing a poptart to false
						kiipDelegate.isShowingPoptart = false;
					}
				}
			 }];
		}
	}
		
	return 0;
}

// ----------------------------------------------------------------------------
	
} // namespace Corona


@implementation KiipDelegate

- (void)willPresentPoptart:(KPPoptart *) poptart
{
    //NSLog( @"poptart shown");
	
	// Execute the completion listener
	Corona::kiipSaveMomentResult( self.L, self.listenerRef, "shown", false );
}

- (void)didDismissPoptart:(KPPoptart *) poptart
{
    //NSLog( @"poptart dismissed");
	
	// Execute the completion listener
	Corona::kiipSaveMomentResult( self.L, self.listenerRef, "dismissed", true );
	
	// Set showing a poptart to false
	kiipDelegate.isShowingPoptart = false;
}
@end

// ----------------------------------------------------------------------------

CORONA_EXPORT
int luaopen_plugin_kiip( lua_State *L )
{
	return Corona::kiipLibrary::Open( L );
}
