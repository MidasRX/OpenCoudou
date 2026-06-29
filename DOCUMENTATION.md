# OpenCoudou — Documentation

This document explains how OpenCoudou replaces OC2R's RISC‑V/Linux core with a native Lua machine,
and how the pieces fit together.

## Overview

A computer in OpenCoudou is a sandboxed **Lua virtual machine** (LuaJ) running OpenComputers'
`machine.lua` kernel. The kernel is driven cooperatively from the server thread and talks to the
in‑world world through **components**, reusing OC2R's existing device bus and RPC layer.

```
ComputerBlockEntity
  └─ LuaComputerVirtualMachine            (lifecycle, server-thread time-sliced run loop)
       ├─ LuaArchitecture                 (LuaJ sandbox + machine.lua kernel + signal protocol)
       │    └─ assets/oc2r/lua/{machine.lua, bios.lua}
       ├─ DeviceBusComponentBridge        (implements MachineHost over CommonDeviceBusController)
       │    └─ built-in + bus RPCDevices  → exposed to Lua as components
       └─ Terminal                        (80x… text grid; the computer's screen)
```

## Key classes (`src/main/java/li/cil/oc2/common/machine/`)

| Class | Responsibility |
|-------|----------------|
| `LuaComputerVirtualMachine` | Owns the bus, bridge, architecture and the per‑tick run loop. Reboot‑on‑reload persistence. Requires a CPU; RAM scales with installed sticks. |
| `lua/LuaArchitecture` | Builds the sandboxed `Globals`, installs the host APIs (`computer`, `component`, `system`, `unicode`, `userdata`), loads `machine.lua`, and drives the kernel coroutine (number = sleep, boolean = reboot/shutdown). Also installs a `string.format` Lua‑5.3 compatibility shim. |
| `lua/MachineHost` | Interface the sandbox calls back into (component ops, signals, memory, time). |
| `lua/DeviceBusComponentBridge` | Implements `MachineHost` over `CommonDeviceBusController`. Reuses the RPC `findOverload`/`invoke` machinery; pads omitted optional args with `nil`; converts values via Gson. Supports built‑in components and `LuaUserdata` handles. |
| `lua/LuaConversions`, `lua/LuaRPCInvocation`, `lua/LuaUserdata` | Java↔Lua value conversion, RPC invocation backing, and rich callable handles (e.g. internet requests). |
| `component/*` | The built‑in components: `ComputerDevice`, `EepromDevice`, `GpuDevice`, `ScreenDevice`, `KeyboardDevice`, `FileSystemDevice`, `InternetDevice`. |
| `fs/*` | `InMemoryFileSystem` + `ResourceFileSystems` (loads bundled OpenOS from a manifest). |
| `KeyInputDecoder`, `LuaMachineLog` | Terminal input → `key_down` signals; a plain‑text debug log (`oc2r-lua-debug.log`). |

## The host ↔ Lua contract

`machine.lua` expects host globals `computer`, `component`, `system`, `unicode`, `userdata`
(and `persistKey = nil`, selecting the non‑persisting LuaJ path). The most important rule:

> **`component.invoke(addr, method, …)` must return `(true, results…)` on success or `(false, error)`
> on failure** — the kernel consumes the leading boolean as a success flag.

Component methods are authored with OC2R's `@Callback`/`@Parameter` annotations and wrapped in
`ObjectDevice` (e.g. `new ObjectDevice(new GpuDevice(...), "gpu")`). A method may return an
`Object[]` to yield **multiple** Lua return values (e.g. `gpu.getResolution()` → `w, h`).

## Adding a component

```java
public final class MyDevice {
    @Callback(synchronize = false, description = "Does a thing.")
    public boolean doThing(@Parameter("value") final int value) { ... }
}
// then, in LuaComputerVirtualMachine:
bridge.addBuiltinDevice(new ObjectDevice(new MyDevice(), "my_type"));
```

Block/item devices that are already `RPCDevice`s on the bus are exposed automatically.

## Filesystem & OpenOS

OpenOS is bundled at `assets/oc2r/openos/` and enumerated by `assets/oc2r/openos.manifest`
(jar directories can't be listed at runtime). It is loaded into a writable `InMemoryFileSystem`
as the boot device; a second tmpfs is mounted for scratch. Storage is per‑session (reboot‑on‑reload);
persistent disk storage is future work.

## Internet

The built‑in `internet` component implements `request(url[, postData[, headers]])` (HTTP via
`java.net.http.HttpClient`, non‑blocking handle) and `connect(host[, port])` (raw TCP). It is gated on
`Config.internetCardEnabled` (default **true**) and filters hosts via `Config.deniedHosts` /
`Config.allowedHosts` (private ranges blocked by default).

## Notes & limitations

- The machine runs on the **server thread**, time‑sliced; well‑behaved (event‑driven) Lua yields via
  `computer.pullSignal`. A tight non‑yielding loop will hit `system.timeout` (~5 s) and error, matching
  OpenComputers' "too long without yielding".
- The screen is the computer's own 160×50 `Terminal`. External **Monitor** blocks from the old Sedna
  era are **not** wired to the Lua GPU yet.
- Lua state is **not** persisted across world reloads (the OS reboots); the disk is in‑memory.

## Offline testing

`scratchpad/BootSim.java` (developer tool) drives the real `LuaArchitecture` + components + OpenOS with
a Minecraft‑free `MachineHost`, so the whole boot can be tested without launching the game.
