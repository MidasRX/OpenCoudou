# OpenOS Command Reference

Complete reference for all commands available in OpenOS shell.

## File System Commands

### `ls` - List Directory Contents
```bash
ls [path]
ls -l [path]    # Long format with details
ls -a [path]    # Include hidden files
```

### `cd` - Change Directory
```bash
cd /home        # Change to /home
cd ..           # Go up one directory
cd              # Go to home directory
cd /            # Go to root
```

### `pwd` - Print Working Directory
```bash
pwd             # Show current directory path
```

### `mkdir` - Make Directory
```bash
mkdir dirname
mkdir -p path/to/dir    # Create parent directories
```

### `cp` - Copy Files
```bash
cp source.txt dest.txt
cp -r dir1 dir2         # Copy directory recursively
```

### `mv` - Move/Rename Files
```bash
mv old.txt new.txt      # Rename
mv file.txt /home/      # Move to directory
```

### `rm` - Remove Files
```bash
rm file.txt
rm -r directory         # Remove directory recursively
rm -f file.txt          # Force removal
```

### `touch` - Create Empty File
```bash
touch newfile.txt
```

### `cat` - Concatenate and Display Files
```bash
cat file.txt            # Display entire file
cat file1 file2         # Concatenate multiple files
```

### `more` - Page Through Files
```bash
more file.txt           # View file page by page
# Space = next page, Q = quit
```

### `head` - Show First Lines
```bash
head file.txt           # First 10 lines
head -n 20 file.txt     # First 20 lines
```

### `tail` - Show Last Lines
```bash
tail file.txt           # Last 10 lines
tail -n 20 file.txt     # Last 20 lines
tail -f file.txt        # Follow file (watch for changes)
```

### `find` - Search for Files
```bash
find /home              # List all files in /home
find . -name "*.lua"    # Find all .lua files
```

### `tree` - Display Directory Tree
```bash
tree                    # Show tree from current dir
tree /home              # Show tree from /home
```

## Text Processing Commands

### `grep` - Search Text Patterns
```bash
grep "pattern" file.txt
grep -r "pattern" /home     # Recursive search
grep -i "pattern" file.txt  # Case-insensitive
grep -n "pattern" file.txt  # Show line numbers
```

### `cut` - Extract Sections from Lines
```bash
cut -f 1 file.txt           # Extract field 1
cut -d ":" -f 2 file.txt    # Use : as delimiter
cut -c 1-10 file.txt        # Characters 1-10
```

### `sort` - Sort Lines
```bash
sort file.txt               # Sort alphabetically
sort -n file.txt            # Numeric sort
sort -r file.txt            # Reverse sort
```

### `uniq` - Remove Duplicate Lines
```bash
uniq file.txt               # Remove consecutive duplicates
uniq -c file.txt            # Count occurrences
uniq -d file.txt            # Show only duplicates
uniq -u file.txt            # Show only unique lines
```

### `wc` - Word Count
```bash
wc file.txt                 # Lines, words, bytes
wc -l file.txt              # Count lines only
wc -w file.txt              # Count words only
```

### `tr` - Translate Characters
```bash
cat file.txt | tr 'a-z' 'A-Z'   # Lowercase to uppercase
cat file.txt | tr -d '\n'       # Delete newlines
```

### `sed` - Stream Editor
```bash
sed 's/old/new/' file.txt       # Replace first occurrence
sed 's/old/new/g' file.txt      # Replace all occurrences
sed -n '1,10p' file.txt         # Print lines 1-10
```

### `awk` - Pattern Scanning and Processing
```bash
awk '{print $1}' file.txt       # Print first field
awk -F: '{print $2}' file.txt   # Use : as delimiter
awk '/pattern/ {print}' file.txt # Print matching lines
```

## System Commands

### `df` - Disk Space Usage
```bash
df                      # Show all filesystems
df -h                   # Human-readable sizes
```

### `du` - Directory Space Usage
```bash
du                      # Current directory usage
du /home                # Specific directory
du -h                   # Human-readable
du -s                   # Summary only
```

### `free` - Memory Usage
```bash
free                    # Show memory stats
free -h                 # Human-readable
```

### `ps` - Process Status
```bash
ps                      # List running processes
ps -a                   # All processes
```

### `kill` - Terminate Process
```bash
kill PID                # Kill process by ID
kill -9 PID             # Force kill
```

### `sleep` - Delay Execution
```bash
sleep 5                 # Sleep for 5 seconds
```

### `date` - Display Date/Time
```bash
date                    # Show current date and time
```

### `uptime` - Show System Uptime
```bash
uptime                  # How long system has been running
```

## System Management

### `shutdown` - Power Off Computer
```bash
shutdown                # Shutdown immediately
shutdown -r             # Reboot
```

### `reboot` - Restart Computer
```bash
reboot                  # Restart immediately
```

### `components` - List Components
```bash
components              # List all connected components
```

### `dmesg` - System Messages
```bash
dmesg                   # Show system boot messages
```

## Programming & Development

### `lua` - Lua Interpreter
```bash
lua                     # Start interactive Lua REPL
lua script.lua          # Run Lua script
lua -e "print('hi')"    # Execute Lua code
```

### `edit` - Text Editor
```bash
edit file.lua           # Open file in editor
# Ctrl+S = Save, Ctrl+W = Exit
# Ctrl+X = Cut, Ctrl+C = Copy, Ctrl+V = Paste
```

### `sh` - Shell Script Interpreter
```bash
sh script.sh            # Run shell script
```

## Network Commands

### `wget` - Download Files
```bash
wget http://example.com/file.txt
wget -O output.txt http://example.com/file.txt
```

### `ping` - Test Network Connectivity
```bash
ping address            # Ping a network address
```

### `ifconfig` - Network Interface Configuration
```bash
ifconfig                # Show network interfaces
```

## Information & Help

### `help` - Show Help
```bash
help                    # List all commands
help command            # Help for specific command
```

### `man` - Manual Pages
```bash
man ls                  # Manual for ls command
man lua                 # Lua documentation
```

### `which` - Locate Command
```bash
which ls                # Show path to ls command
```

### `echo` - Display Text
```bash
echo "Hello World"      # Print text
echo $PATH              # Print variable
```

## File Utilities

### `basename` - Extract Filename
```bash
basename /home/user/file.txt    # Output: file.txt
```

### `dirname` - Extract Directory
```bash
dirname /home/user/file.txt     # Output: /home/user
```

### `ln` - Create Links
```bash
ln -s /path/to/file link_name   # Create symbolic link
```

### `chmod` - Change Permissions (no-op in OC, for script compatibility)
```bash
chmod +x script.sh
```

### `mktemp` - Create Temporary File
```bash
mktemp                  # Create temp file in /tmp
```

## Miscellaneous

### `clear` - Clear Screen
```bash
clear                   # Clear terminal screen
```

### `yes` - Output String Repeatedly
```bash
yes                     # Print 'y' repeatedly
yes "hello"             # Print 'hello' repeatedly
# Ctrl+C to stop
```

### `true` - Return Success
```bash
true                    # Always returns 0 (success)
```

### `false` - Return Failure
```bash
false                   # Always returns 1 (failure)
```

### `test` - Evaluate Conditions
```bash
test -e /home/file.txt  # Test if file exists
test -d /home           # Test if directory
test -f /home/file.txt  # Test if regular file
test "a" = "a"          # String comparison
```

### `seq` - Generate Number Sequence
```bash
seq 10                  # 1 to 10
seq 5 10                # 5 to 10
seq 1 2 10              # 1 to 10, step 2
```

### `printf` - Formatted Output
```bash
printf "Hello %s\n" "world"
printf "%d + %d = %d\n" 1 2 3
```

### `xargs` - Build Command from Input
```bash
ls | xargs rm           # Remove all files in list
find . -name "*.tmp" | xargs rm
```

### `od` - Octal/Hex Dump
```bash
od file.bin             # Octal dump
od -x file.bin          # Hex dump
```

### `xxd` - Hexdump Utility
```bash
xxd file.bin            # Hex dump with ASCII
xxd -r file.hex         # Reverse hex dump
```

## I/O Redirection & Pipes

### Output Redirection
```bash
command > file.txt      # Redirect stdout to file (overwrite)
command >> file.txt     # Redirect stdout to file (append)
command 2> error.txt    # Redirect stderr to file
command &> all.txt      # Redirect stdout and stderr
```

### Input Redirection
```bash
command < input.txt     # Read stdin from file
```

### Pipes
```bash
command1 | command2     # Pipe output of cmd1 to input of cmd2
ls | grep ".lua"        # List files, filter for .lua
cat file.txt | sort | uniq
```

## Shell Features

### Variables
```bash
VAR="value"             # Set variable
echo $VAR               # Use variable
```

### Command Substitution
```bash
FILES=$(ls)             # Capture command output
CURRENT_DIR=$(pwd)
```

### Background Jobs
```bash
command &               # Run in background
```

### Exit Status
```bash
$?                      # Exit code of last command
# 0 = success, non-zero = error
```

## Tips & Tricks

### Command History
- **Up Arrow**: Previous command
- **Down Arrow**: Next command
- **Ctrl+R**: Search command history

### Tab Completion
- **Tab**: Auto-complete filenames and commands

### Shortcuts
- **Ctrl+C**: Cancel current command
- **Ctrl+D**: Exit shell (EOF)
- **Ctrl+L**: Clear screen (same as `clear`)

### Advanced Usage

**Find large files:**
```bash
du /home | sort -n | tail -10
```

**Count files recursively:**
```bash
find /home -type f | wc -l
```

**Search and replace in files:**
```bash
sed -i 's/old/new/g' *.txt
```

**Monitor log file:**
```bash
tail -f /var/log/system.log
```

**Process data pipeline:**
```bash
cat data.txt | grep "ERROR" | sort | uniq -c | sort -nr
```

---

For more detailed help on any command, use `man <command>` or `help <command>`.
