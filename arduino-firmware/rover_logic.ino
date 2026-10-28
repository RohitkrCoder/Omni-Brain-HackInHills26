#include <SoftwareSerial.h>

const int ENA = 5;
const int IN1 = 6;
const int IN2 = 7;
const int IN3 = 8;
const int IN4 = 9;
const int ENB = 10;
const int TRIG = 11;
const int ECHO = 12;
const int BT_RX = 2;
const int BT_TX = 3;
const int MOTOR_SPEED = 200;
const int OBSTACLE_CM = 15;

SoftwareSerial bt(BT_RX, BT_TX);

char currentCmd = 'F';

void setMotors(int a1, int a2, int b1, int b2) {
  digitalWrite(IN1, a1);
  digitalWrite(IN2, a2);
  digitalWrite(IN3, b1);
  digitalWrite(IN4, b2);
}

void forward() {
  analogWrite(ENA, MOTOR_SPEED);
  analogWrite(ENB, MOTOR_SPEED);
  setMotors(HIGH, LOW, HIGH, LOW);
}

void backward() {
  analogWrite(ENA, MOTOR_SPEED);
  analogWrite(ENB, MOTOR_SPEED);
  setMotors(LOW, HIGH, LOW, HIGH);
}

void turnLeft() {
  analogWrite(ENA, MOTOR_SPEED);
  analogWrite(ENB, MOTOR_SPEED);
  setMotors(LOW, HIGH, HIGH, LOW);
}

void turnRight() {
  analogWrite(ENA, MOTOR_SPEED);
  analogWrite(ENB, MOTOR_SPEED);
  setMotors(HIGH, LOW, LOW, HIGH);
}

void stopMotors() {
  analogWrite(ENA, 0);
  analogWrite(ENB, 0);
  setMotors(LOW, LOW, LOW, LOW);
}

long readDistanceCm() {
  digitalWrite(TRIG, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG, LOW);
  long duration = pulseIn(ECHO, HIGH, 30000UL);
  if (duration == 0) {
    return 400;
  }
  return duration / 58;
}

void applyCommand(char c) {
  switch (c) {
    case 'F':
      forward();
      break;
    case 'B':
      backward();
      break;
    case 'L':
      turnLeft();
      break;
    case 'R':
      turnRight();
      break;
    case 'S':
      stopMotors();
      break;
  }
}

void setup() {
  pinMode(ENA, OUTPUT);
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);
  pinMode(ENB, OUTPUT);
  pinMode(TRIG, OUTPUT);
  pinMode(ECHO, INPUT);
  bt.begin(9600);
  stopMotors();
  forward();
}

void loop() {
  while (bt.available() > 0) {
    char c = toupper(bt.read());
    if (c == 'F' || c == 'B' || c == 'L' || c == 'R' || c == 'S') {
      currentCmd = c;
      applyCommand(currentCmd);
    }
  }

  if (currentCmd == 'F') {
    long distance = readDistanceCm();
    if (distance > 0 && distance < OBSTACLE_CM) {
      stopMotors();
      delay(200);
      turnLeft();
      delay(500);
      stopMotors();
      delay(100);
      currentCmd = 'F';
      forward();
    }
  }

  delay(30);
}