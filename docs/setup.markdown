To use this plugin, add an entry into the plugins table of build.settings. When added, the build server will integrate the plugin during the build phase.

    settings =
    {
        plugins =
        {
            ["plugin.kiip"] =
            {
                publisherId = "com.gremlininteractive"
            },
        },      
    }

For Android, the following permissions/features are automatically added when using this plugin:

    android =
    {
        usesPermissions =
        {
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE"
        },
    },