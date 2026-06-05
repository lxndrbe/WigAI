# WigAI — Bitwig Studio Extension for AI Control

WigAI is a [Model Context Protocol](https://modelcontextprotocol.io) (MCP) server implemented as a Bitwig Studio extension. It lets AI assistants such as Claude control Bitwig Studio through natural language — transport, devices, tracks, clips, scenes, and inserting Bitwig devices.

> **This fork targets Bitwig Studio 6.0.6.** It is updated to Bitwig Extension **API version 25** (the API shipped with Bitwig 6.0.6) and adds programmatic Bitwig-device insertion by UUID plus a one-click Claude Desktop installer. See [What's new in this fork](#whats-new-in-this-fork).

## Overview

The WigAI extension runs an embedded MCP server inside Bitwig Studio (default `http://localhost:61169/mcp`). An external AI agent connects to that endpoint and drives Bitwig hands-free via text commands.

## Key Features

-   **Transport** — start/stop playback, project & transport status
-   **Devices** — read and write parameters of the selected device, inspect remote controls
-   **Tracks** — list tracks (with type filter), detailed track info incl. sends and clips, list devices on a track
-   **Clips & Scenes** — launch clips, launch scenes by index or name, list scenes, inspect clip slots in a scene
-   **Bitwig device insertion** — list known Bitwig devices with their UUIDs and insert them into a track's device chain by UUID

## What's new in this fork

-   **Bitwig 6.0.6 / API 25** — upgraded from API 19; the track bank now uses `TrackBankContentFilter.ALL_CHANNELS` so hidden and nested tracks are reachable.
-   **`list_bitwig_devices`** — discover the UUIDs of every Bitwig-native device (and CLAP/VST reference entries).
-   **`insert_bitwig_device`** — insert a Bitwig-native device into a track's device chain by UUID, via the Controller API's `InsertionPoint`.
-   **Device registry** — a verified registry of 500+ device UUIDs (`bitwig-device-uuids-full.json`), bundled into the extension.
-   **One-click Claude Desktop install** — a `.mcpb` Desktop Extension bundle (see below), no JSON configuration required.

## Requirements

-   **Bitwig Studio 6.0.6** or later (Extension API 25)
-   **Java 21 LTS** — to build the extension
-   An AI agent that speaks MCP (Claude Desktop, Claude Code, etc.)

## Building the extension

```bash
./gradlew build
```

This produces `build/extensions/WigAI.bwextension`.

> On Windows, ensure `JAVA_HOME` points to a JDK 21 install. Gradle 8.13 does not support JDK 25.

## Installing the extension in Bitwig

1. Copy `build/extensions/WigAI.bwextension` into your Bitwig Studio extensions directory
    (Windows: `%USERPROFILE%\Documents\Bitwig Studio\Extensions`).
2. Launch Bitwig Studio.
3. Open **Settings → Controllers**, add the **WigAI MCP Server**, and activate it.

Once active, the MCP server is reachable at `http://localhost:61169/mcp`.

## Connecting Claude

### Option A — Install via Claude Desktop Extensions (Recommended)

WigAI ships a `.mcpb` [Desktop Extension](https://github.com/anthropics/mcpb) bundle that connects Claude Desktop to the running WigAI server without editing your configuration files manually.

> [!IMPORTANT]
> **Prerequisite:** Bitwig Studio must be open and the WigAI extension activated (orange checkmark in Controller Settings) *before* installing the extension or starting Claude Desktop. Otherwise, the proxy will fail to establish the connection and Claude will show a "Server disconnected" error.

1. Make sure Bitwig Studio is running with the WigAI extension activated.
2. Download `WigAI.mcpb` from this repository or the releases.
3. Open **Claude Desktop** and click the gear icon in the bottom-left corner to open **Settings** (Einstellungen).
4. Navigate to the **Extensions** (Erweiterungen) tab.
5. Click on **Extension Settings** (Erweiterungseinstellungen / Entwicklereinstellungen).
6. At the bottom, click **Install Extension** (Erweiterung installieren) and choose the downloaded `WigAI.mcpb` file.
7. If you changed the port in Bitwig's WigAI preferences, update the **WigAI Server URL** field during the installation and click **Install**.

The bundle packages a small stdio↔HTTP proxy ([`mcp-remote`](https://www.npmjs.com/package/mcp-remote)); it requires Node.js 18+ on your system.

#### Building the `.mcpb` bundle

```bash
cd mcpb/server && npm install && cd ../..
npx -y @anthropic-ai/mcpb pack mcpb WigAI.mcpb
```

### Option B — Manual MCP configuration (Claude Code / other clients)

For Claude Code, register the running server (Streamable HTTP transport):

```bash
claude mcp add --transport http WigAI http://localhost:61169/mcp
```

Other clients can point at `http://localhost:61169/mcp` directly using an HTTP/SSE MCP transport.

## MCP tools

| Tool | Description |
|------|-------------|
| `status` | Full snapshot: transport, project, selected track & device |
| `transport_start` / `transport_stop` | Start / stop playback |
| `configure_transport` | Set tempo (BPM), metronome, arranger loop, or record-arm |
| `get_selected_device_parameters` | Read parameters of the selected device |
| `set_selected_device_parameter` | Set one parameter (index + value 0.0–1.0) |
| `set_selected_device_parameters` | Set multiple parameters at once |
| `get_device_details` | Device details and remote controls |
| `list_tracks` | List tracks, optional type filter |
| `get_track_details` | Track details incl. sends and clips |
| `list_devices_on_track` | Devices on a track |
| `launch_clip` | Launch a clip by track name + clip index |
| `session_launchSceneByIndex` | Launch a scene by index |
| `session_launchSceneByName` | Launch a scene by name |
| `list_scenes` | List all scenes |
| `get_clips_in_scene` | Clip slots in a scene |
| `list_bitwig_devices` | Known Bitwig devices with UUIDs |
| `insert_bitwig_device` | Insert a device (native, VST3, CLAP, or VST2) into a track by ID/UUID |
| `create_track` | Create a new track (audio, instrument, or effect) |
| `rename_track` | Rename a track by its index |
| `delete_selected_device` | Delete the currently selected device |
| `select_next_device` / `select_previous_device` / `select_first_device` | Navigate device selection in the track chain |
| `select_track` | Select/focus a track by index |
| `set_track_parameter` | Set track volume, pan, mute, solo, or arm |
| `set_device_bypass` | Bypass or activate a device by track and device index |

## Compatible AI Agents

WigAI works with any AI agent that supports the Model Context Protocol — not just Claude. As long as the agent can connect to an MCP server over HTTP or stdio, it can control Bitwig through WigAI.

| Agent | How to connect |
|-------|---------------|
| **Claude Desktop** | Install `WigAI.mcpb` (one-click, see above) |
| **Claude Code** | `claude mcp add --transport http WigAI http://localhost:61169/mcp` |
| **Cursor** | Add to `.cursor/mcp.json`: `{ "mcpServers": { "WigAI": { "url": "http://localhost:61169/mcp" } } }` |
| **VS Code Copilot** | Add to VS Code MCP settings with transport `http` and URL `http://localhost:61169/mcp` |
| **Any other MCP client** | Point it at `http://localhost:61169/mcp` using an HTTP or Streamable HTTP transport |

The WigAI server speaks the standard [MCP protocol](https://modelcontextprotocol.io) — no vendor-specific extensions. If your agent supports MCP, it works with WigAI.

## Development

This project is developed using the [BMAD method](https://github.com/bmadcode/BMAD-METHOD) with AI agents.

## Releases

This project uses [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` — new features (minor version bump)
- `fix:` — bug fixes (patch version bump)
- `feat!:` / `BREAKING CHANGE:` — breaking changes (major version bump)
- `chore:`, `docs:`, `style:`, `refactor:`, `test:` — no version bump

## Troubleshooting

**Claude Desktop shows "Server disconnected":**
- Is Bitwig Studio running? The WigAI MCP server only runs when Bitwig Studio is open and the extension is initialized.
- Is the extension added and active under **Settings → Controllers** in Bitwig? It should have an orange active light.
- If Bitwig was opened after Claude Desktop started, click "Retry" or restart Claude Desktop.

**Extension doesn't load/activate in Bitwig:**
- Do you have Java 21 installed? Run `java -version` in terminal.
- Is `WigAI.bwextension` in the correct folder (`%USERPROFILE%\Documents\Bitwig Studio\Extensions`)?

## License

[MIT License](LICENSE)

## Author

Original project by **fabb**. Bitwig 6.0.6 / API 25 fork and Desktop Extension by **lxndrbe**.
