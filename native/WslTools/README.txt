This project creates two tools:

wslproxy
To avoid connecting from WSL to Windows (such connections may be blocked by firewall) we connect from Windows to WSL instead.
This proxy accepts two clients: one for external (eth0) and one for local (loopback). It then passes data between them with two threads.
Client disconnection, signal or any byte written to the stdin kills process.

To build tool use Makefile. We link it statically because WSL may lack glibc. Kernel ABI is backward compatible, so use some old Linux

We use musl libc: https://musl.libc.org/
Not only it produces smaller binaries, but also it is MIT licenced, and we can't link statically with LGPL

Get musl automatically:
1. Run "make" (you must have `wget` installed)
2. Open project in CLion if you want

Getting musl manually:
1. Download .tar.gz from here: https://musl.libc.org/
2. unpack to "musl": ``tar xfz musl-1.2.2.tar.gz && mv musl-1.2.2 musl``
3. run "make"
4. Open project in CLion if you want

See https://wiki.musl-libc.org/getting-started.html for more info

Q: I got error opening project in CLion:
A: Make sure you run "make". Then, tools->Makefile->Reload