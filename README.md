# DirectMessages

Private messages for Paper and Folia, with your own chat formats, sounds, an action bar notice, ignore, a PM toggle
and social spy.

Players message each other with `/msg` and answer with `/r`. The receiver sees the message in chat and on the action
bar and hears a sound; the sender hears a different one, and a third sound plays when something goes wrong. Hovering a player's name shows a card with their stats, and
clicking it starts a message to them.

## Features

- `/msg` with the aliases `/tell`, `/whisper`, `/message` and `/w`, and `/r` (`/reply`) to answer
- Vanilla `/minecraft:msg`, `/minecraft:tell` and `/minecraft:w` go through the plugin too, so nobody can skip ignore,
  the PM toggle or social spy
- Separate formats for sent, received and social spy messages, with `&` colors and `&#RRGGBB` hex colors
- A line on the action bar when a message arrives
- One sound for sending, one for receiving and one for errors, each with its own volume and pitch
- A hover card on player names with PlaceholderAPI placeholders; clicking a name types `/msg <name>` in the chat box
- `/ignore` (or `/block`) blocks private messages both ways and hides the ignored player's public chat
- `/msgtoggle` turns off receiving private messages; the player can still send them
- `/socialspy` for staff
- Vanish support: players you can't see look offline to `/msg` and tab completion
- Cooldown between messages, with a bypass permission
- Every private message written to a daily log file
- Ignore lists and social spy saved in SQLite or MySQL
- Folia support
- `/directmessages reload` to reload the config without a restart

## Requirements

- Paper 1.21.7 or newer (tested on 1.21.7 and 26.1.2)
- Folia 26.x (tested on Canvas 26.3, a Folia fork)
- Java 25 or newer on 26.x servers, Java 21 or newer on 1.21.x servers
- [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) for placeholders (optional)

The default config uses these PlaceholderAPI expansions:

```
/papi ecloud download Player
/papi ecloud download Vault
/papi ecloud download Statistic
```

Then run `/papi reload`.

`%vault_prefix%` in `name` shows the player's rank prefix. It needs Vault and a permissions plugin with prefixes, such
as LuckPerms. Without PlaceholderAPI, placeholders are shown as typed, so set `name` to `"{name}"` and remove the
placeholder lines from `hover.lines`.

## Installation

1. Put `DirectMessages-<version>.jar` in your `plugins` folder.
2. Start the server. The plugin downloads its libraries (Kotlin, HikariCP, SQLite and MySQL drivers) on first start.
3. Edit `plugins/DirectMessages/config.yml` and run `/directmessages reload`.

## Commands and permissions

| Command | Permission | Description |
|---|---|---|
| `/msg <player> <message>` | none | Sends a private message. Aliases: `/tell`, `/whisper`, `/message`, `/w` |
| `/r <message>` | none | Replies to the last player you messaged or who messaged you. Alias: `/reply` |
| `/ignore <player>` | none | Ignores the player, or stops ignoring them if you already do. Alias: `/block` |
| `/ignore` | none | Lists the players you ignore. Alias: `/block` |
| `/msgtoggle` | none | Turns receiving private messages off, or back on |
| `/socialspy` | `directmessages.socialspy` (op) | Shows you every private message between other players |
| `/directmessages reload` | `directmessages.reload` (op) | Reloads the config |
| | `directmessages.cooldown.bypass` (op) | Sends messages without the cooldown |

## Configuration

```yaml
name: "%vault_prefix%{name}"

formats:
  sent: "&#C77DFFTo &f{receiver}&7: &f{message}"
  received: "&#C77DFFFrom &f{sender}&7: &f{message}"
  spy: "&8[&#C77DFFSpy&8] &7{sender} &8→ &7{receiver}&8: &7{message}"
  action-bar: "&#C77DFF✉ &fNew message from {sender}"

hover:
  lines:
    - "&f{name}"
    - "&7------------"
    - "&#00FF11$ &fMoney: &#00FF11%vault_eco_balance_formatted%"
    - "&#FF0000🗡 &fKills: &#FF0000%statistic_player_kills%"
    - "&#FFE400⌚ &fPlaytime: &#FFE400%statistic_time_played%"
    - "&#FA7600☠ &fDeaths: &#FA7600%statistic_deaths%"
    - "&7------------"
    - "&7Click To Message!"
  click: "/msg {name} "

sounds:
  sent:
    sound: entity.experience_orb.pickup
    volume: 0.6
    pitch: 1.4
  received:
    sound: block.note_block.pling
    volume: 1.0
    pitch: 2.0
  error:
    sound: entity.villager.no
    volume: 1.0
    pitch: 1.0

cooldown: 1.0
log: true

storage:
  type: sqlite
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: ""

messages:
  msg-usage: "&cUsage: /msg <player> <message>"
  reply-usage: "&cUsage: /r <message>"
  not-online: "&c{player} is not online."
  no-reply: "&cYou have nobody to reply to."
  self: "&cYou can't message yourself."
  not-accepting: "&cThat user is not accepting private messages."
  you-ignore: "&cYou are ignoring {player}."
  cooldown: "&cWait {seconds}s before sending another message."
  ignore-on: "&7You now ignore &f{player}&7."
  ignore-off: "&7You no longer ignore &f{player}&7."
  ignore-self: "&cYou can't ignore yourself."
  ignore-list: "&7Ignored players: &f{players}"
  ignore-empty: "&7You don't ignore anyone."
  never-joined: "&c{player} has never joined."
  spy-on: "&aSocial spy on."
  spy-off: "&cSocial spy off."
  toggle-on: "&aPrivate messages on."
  toggle-off: "&cPrivate messages off."
  reloaded: "&aDirectMessages reloaded."
```

| Key | Description |
|---|---|
| `name` | How a player's name looks in messages. `{name}` is the player's name; placeholders are filled for that player |
| `formats.sent` | What the sender sees |
| `formats.received` | What the receiver sees |
| `formats.spy` | What staff with social spy on see |
| `formats.action-bar` | The action bar line the receiver sees |
| `hover.lines` | The card shown when hovering a name. Placeholders are filled for the player whose name it is |
| `hover.click` | Typed into the chat box when a name is clicked. `{name}` is the player's name |
| `sounds.sent` / `sounds.received` | Any sound key, including resource pack sounds. Volume and pitch are numbers |
| `sounds.error` | Plays with every error message, like "You can't message yourself." or a player not accepting messages |
| `cooldown` | Seconds between two messages from the same player. `0` turns it off |
| `log` | Writes every message to `plugins/DirectMessages/logs/<date>.log` |
| `storage.type` | `sqlite` (saved in `plugins/DirectMessages/data.db`) or `mysql` |

In `formats`, `{sender}` and `{receiver}` are the two players' names, drawn with `name`, with the hover card and click.
`{message}` is the message text. It is never parsed for colors or placeholders, so players can't use them. A token
takes the color written before it.

When someone ignores you or has `/msgtoggle` off, you get the same `not-accepting` message, so you can't tell which.

An empty format, action bar, sound, `hover.lines` or `hover.click` turns that part off. An empty message in
`messages` is not sent. Changing `storage` needs a restart; everything else applies with `/directmessages reload`.

## Building

Requires JDK 25 or newer and Maven.

```
mvn package
```

The jar is written to `target/`.

## Support

Ask on my Discord: https://discord.gg/nRjDw9k2vJ

## License

[MIT](LICENSE)
