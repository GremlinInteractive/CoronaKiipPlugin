#### Overview

Sends a potentially rewardable moment indicator to Kiip. This function initiates a rewards sequence, starting with a customizable "poptart" or popup. This poptart will open as a customized popup window or as a simple interstitial window if the reward is available from Kiip.

`````
Kiip.saveMoment( options )
`````

This function takes a single argument, `options`, which is a table that accepts the following parameters:

##### momentName - (required)

__[String]__ The name of the moment that you are rewarding. The moment name will be displayed both in the dashboard and in the poptart, so it's recommended that you give it a logical name. For more information on moments and moment names, please see the [Kiip Glossary](http://docs.kiip.com/en/glossary.html).

##### value - (optional)

__[Number]__ The value of the moment. If your moment is associated with a monetary or point value, set it here as part of your gameplay. This value is a number and supports up to 2 decimal places, for example: `value = 1.0`, `value = 1.01` or `value = 10.02`.

##### listener - (required)

__[Listener]__ This callback function is recommended to check a poptart's status via `event.phase` which returns possible values of `"shown"`, `"dismissed"`, `"noPoptart"` or `"error"`. If `event.phase` is `"error"`, the error message can be retrieved via `event.errorDescription`.

#### Example

	-- Require the Kiip library
	local Kiip = require( "plugin.kiip" )

	Kiip.saveMoment(
	{
	    momentName = "A Super Prize!",
	    value = 5,
	    listener = function( event )
	        -- Print the event table items
	        for k,v in pairs( event ) do
	            print( k, ":", v )
	        end
	    end,
	})