# ClickTele

## Features

Example image

### Configuration

Rudimentary configuration exists to modify the cooldown time between teleports. 
Use the `/ctdt <seconds>` command to change the cooldown on a running server.

For example, to set the cooldown to 5 seconds:
```
/ctcd 5
```

## Development

### Setup

For setup instructions please see the [fabric wiki page](https://fabricmc.net/wiki/tutorial:setup) that relates to the IDE that you are using.
Run the `runClient` task.

### Packaging

Run the `build` task, and use the `.jar` output without sources, and not ending in `-dev`, contrary to the Fabric docs.