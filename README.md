# Omni-Brain: Edge AI Rescue Rover
**Hack in Hills '26 - Prototype Submission**

Omni-Brain is a rescue rover we built around a normal Android phone and an Arduino. The idea is simple: after an earthquake or building collapse, you can't send a $10,000 robot into every pile of rubble. But almost everyone has an old phone lying around. So we made the phone the brain and kept the body as cheap as possible. The whole thing comes in under $50.

## Why we made this

Most rescue robots out there are expensive and need a cloud connection or a base station to work properly. In a disaster zone you usually have neither. And if one robot gets stuck or crushed, the whole mission suffers because there's no backup.

## What we did

The phone does all the thinking. Its camera is the eyes, and it runs YOLOv8 (nano) directly on the device, so no internet is needed. It spots obstacles and people in the frame, then sends simple commands (F, B, L, R, S) over Bluetooth to an HC-05 module connected to the Arduino. The Arduino drives the motors through an L298N driver.

There's also an HC-SR04 ultrasonic sensor on the rover as a second safety layer. If something is closer than 15 cm, the rover stops and turns left by itself, even if the phone hasn't reacted yet.

### What it can do right now
* Works fully offline, since detection runs on the phone itself
* Avoids obstacles using both the ultrasonic sensor and camera vision
* Can be controlled manually over Bluetooth with F / B / L / R / S
* Cheap enough that you could build several and send them in together

## Tech stack

**Software:** Android (Java, Bluetooth serial, camera preview), YOLOv8 nano / TensorFlow Lite, Arduino C++

**Hardware:**
* Arduino Uno or Nano
* HC-05 Bluetooth module
* L298N motor driver
* DC gear motors with chassis
* HC-SR04 ultrasonic sensor
* Android smartphone

## How to run it

1. Open `/arduino-firmware/rover_logic.ino` in Arduino IDE and upload it to your board.
2. Build the Android app from `/android-app` and install it on your phone.
3. Power on the rover and pair the HC-05 with your phone first (default pairing code is usually 1234 or 0000).
4. Open the app and tap connect. Once it says connected, the rover starts moving and the AI takes over.

## Known limitations

* Detection speed depends a lot on the phone. Older phones will lag a bit.
* Bluetooth range is short, around 10 m in open space and less with walls in between.
* Swarm coordination is still an idea we're working toward, not something fully built yet.

## Team
* Rohit Kumar
