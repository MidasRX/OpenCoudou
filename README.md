# OpenCoudou

**OpenCoudou** is a Minecraft (Forge 1.20.1) mod that brings back the classic **[OpenComputers]**
experience inside the **OpenComputers II: Reborn (OC2R)** codebase.

The original OC2R emulated a 64‑bit **RISC‑V CPU booting Linux** (via the *Sedna* virtual machine).
OpenCoudou **rips that out** and replaces it with a **native, sandboxed Lua machine** — exactly like
the original OpenComputers: your computers boot **OpenOS**, expose **components** (`gpu`, `screen`,
`keyboard`, `filesystem`, `eeprom`, `internet`, redstone, …), and run Lua programs you can download
straight off the internet.

> No more Linux emulation. Just place a computer, drop in a CPU + RAM, and you boot into a Lua shell.

## Features

- **Native Lua VM** powered by [LuaJ] (pure Java, no native libraries) running OpenComputers'
  real `machine.lua` kernel + `bios.lua` — sandboxed, with a cooperative signal/coroutine scheduler.
- **OpenOS bundled** — every computer boots straight to the OpenOS shell (`/home #`).
- **OpenComputers component model** built on OC2R's existing device bus:
  `component.list / invoke / methods`, `computer.pullSignal`, UUID‑addressed components, events.
- **Built‑in components:** `computer`, `eeprom` (Lua BIOS), `gpu`, `screen`, `keyboard`,
  `filesystem` (OpenOS + a writable tmpfs), and `internet`.
- **Internet card** (`internet.request` HTTP + `internet.connect` TCP) — so `wget`, `pastebin`,
  `oppm`, and OS installers work. Enabled by default; outbound hosts are filtered by the config's
  allow/deny lists.
- **RAM scales with installed memory sticks**; **a CPU is required** to run.
- **160×50 screen** (tier‑3 equivalent), so heavier Lua OSes such as **MineOS** can run.

## Getting started

1. Install **Forge 1.20.1** and drop the OpenCoudou jar into `mods/`, together with its dependencies:
   - [MarkdownManual] (required, for the in‑game Manual)
   - [Architectury API]
2. Craft/obtain a **Computer**, open it, and install at least a **CPU** and some **Memory**.
3. Power it on — it boots **OpenOS** to a shell.
4. Download programs:
   ```
   wget https://raw.githubusercontent.com/<user>/<repo>/<branch>/<file>.lua /home/<file>.lua
   pastebin run <id>
   oppm install <package>
   ```
5. **Tip:** the screen is 160×50; set **Options → Video Settings → GUI Scale → 1** for the most
   readable text.

## Building

Requires a **JDK 17** toolchain (auto‑provisioned via the Foojay resolver in `settings.gradle`).

```bash
./gradlew build
```

The mod jar (with embedded LuaJ) is produced at
`build/libs/oc2r-1.20.1-forge-<version>-all.jar`.

## Documentation

See [DOCUMENTATION.md](DOCUMENTATION.md) for the architecture overview, the host↔Lua component
contract, and how to add your own components.

## Branches

- **`main`** — OpenCoudou (the native Lua machine rewrite).
- **`old`** — the previous (RISC‑V/Linux, Sedna‑based) code, preserved for reference.

## Credits

- Built on **OpenComputers II: Reborn** by North Western Development, itself a successor to
  **OpenComputers II** by [Sangar (fnuecke)].
- Bundles **OpenOS** and the Lua kernel from the original [OpenComputers] (MIT licensed).
- Lua execution by [LuaJ].

## License

MIT. See `LICENSE`. Vendored OpenComputers Lua assets retain their original MIT license
(`src/main/resources/assets/oc2r/lua/LICENSE-OpenComputers`).

[OpenComputers]: https://github.com/MightyPirates/OpenComputers
[LuaJ]: https://sourceforge.net/projects/luaj/
[Sangar (fnuecke)]: https://github.com/fnuecke
[MarkdownManual]: https://www.curseforge.com/minecraft/mc-mods/markdownmanual
[Architectury API]: https://www.curseforge.com/minecraft/mc-mods/architectury-api
