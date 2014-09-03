#### Overview

 Initializes the Kiip library. This function is required and must be executed before making other Kiip calls such as `Kiip.saveMoment()`.

## Syntax

`````
Kiip.init( options )
`````

    This function takes a single argument, `options`, which is a table that accepts the following parameters:

##### appKey - (required)

__[String]__ Your Kiip app key. You can generate your app key from the [](https://app.kiip.me)Kiip Dashboard.

##### appSecret - (required)

__[String]__ Your Kiip app secret. You can generate your app secret from the [](https://app.kiip.me)Kiip Dashboard.

##### email - (optional)

__[String]__ The user's email address. Entering this will result in more fine-grained reward targeting. This email address appears in the Kiip interstitial, so specifying this spares the user from having to enter an email address manually.

##### gender - (optional)

__[String]__ The user's gender. Entering this will result in more fine-grained reward targeting.

##### birthday - (optional)

__[String]__ The user's birthday. Entering this will result in more fine-grained reward targeting. This should be formatted in the American date format of `MM/DD/YYYY`, for example `birthday = "05/01/1970"`.

##### alias - (optional)

__[String]__ The user's alias, such as a membership in a group. Entering this will result in more fine-grained reward targeting.

##### listener - (optional)

__[Listener]__ This function is here for backwards compatibility only. it is not required. This function returns `event.status` with a fixed value of `valid`. As there is no more licensing validation being performed.

#### Example


    -- Require the Kiip library
    local Kiip = require( "plugin.kiip" )

    -- Initialize the Kiip library
    Kiip.init(
    {
        appKey = "app_key_generated_from_kiip_here",
        appSecret = "app_secret_generated_from_kiip_here",  
        email = "test@test.com",
        gender = "female",
        birthday = "05/01/1970",
        alias = "game_player",
        listener = function( event )
            -- Print the events key/pair values
            for k,v in pairs( event ) do
                print( k, ":", v )
            end
        end
    })