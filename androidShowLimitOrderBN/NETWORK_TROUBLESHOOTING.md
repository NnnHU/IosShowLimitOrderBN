# Network Troubleshooting Guide for Android Emulator

## Common DNS Resolution Issues

The app is experiencing DNS resolution failures for Binance domains. This is a common issue with Android emulators.

### Emulator Network Configuration

1. **Check Emulator Internet Access**:
   - Open a web browser in the emulator
   - Try to visit google.com
   - If this fails, the emulator has no internet access

2. **Emulator DNS Settings**:
   - The emulator uses the host machine's DNS settings
   - If your host machine has DNS issues, the emulator will too

3. **Firewall/Proxy Issues**:
   - Corporate firewalls may block WebSocket connections
   - Proxy settings may interfere with DNS resolution

### Solutions to Try

#### Option 1: Restart Emulator with DNS Settings
```bash
emulator -avd YOUR_AVD_NAME -dns-server 8.8.8.8,8.8.4.4
```

#### Option 2: Use Cold Boot
- In Android Studio: Tools > AVD Manager
- Click dropdown next to your emulator
- Select "Cold Boot Now"

#### Option 3: Check Host Machine DNS
- On Windows: `nslookup api.binance.com`
- On Mac/Linux: `dig api.binance.com`
- If this fails, fix your host machine's DNS first

#### Option 4: Use Different Emulator
- Try a different API level (API 30, 31, 33)
- Try x86_64 instead of ARM64 or vice versa

#### Option 5: Use Physical Device
- Connect an Android device via USB
- Enable USB debugging
- Run the app on the physical device

### Network Security Configuration

The app includes:
- `android:usesCleartextTraffic="true"` for debugging
- Network security config allowing cleartext traffic
- Additional network permissions

### Testing Network Connectivity

The app now includes comprehensive network testing:
- DNS resolution tests for all Binance domains
- Connectivity tests on port 443
- Baseline tests (Google DNS, google.com)

### Logs to Check

Look for these log messages:
- "Network available: true/false"
- "DNS lookup successful for..."
- "DNS resolution failed for..."
- "=== Network Connectivity Report ==="

### If All Else Fails

1. **Use a VPN**: Sometimes changing your IP location helps
2. **Try Mobile Hotspot**: Use your phone's hotspot for internet
3. **Check with IT**: Corporate networks may block cryptocurrency-related domains
4. **Use Different Network**: Try from home instead of office network

### Emulator-Specific Commands

```bash
# Start emulator with specific DNS
emulator -avd Pixel_4_API_30 -dns-server 8.8.8.8,1.1.1.1

# Start with no audio (sometimes helps with performance)
emulator -avd Pixel_4_API_30 -no-audio

# Start with writable system (for advanced debugging)
emulator -avd Pixel_4_API_30 -writable-system
```

### Testing from ADB Shell

```bash
# Connect to emulator
adb shell

# Test DNS resolution
nslookup api.binance.com

# Test connectivity
ping google.com

# Check network interfaces
ifconfig
```