#include <SoftwareSerial.h>

// I2C device class (I2Cdev) demonstration Arduino sketch for ADXL345 class
// 10/7/2011 by Jeff Rowberg <jeff@rowberg.net>
// Updates should (hopefully) always be available at https://github.com/jrowberg/i2cdevlib
//
// Changelog:
//     2011-10-07 - initial release

/* ============================================
I2Cdev device library code is placed under the MIT license
Copyright (c) 2011 Jeff Rowberg

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
===============================================
*/

// Arduino Wire library is required if I2Cdev I2CDEV_ARDUINO_WIRE implementation
// is used in I2Cdev.h
#include "Wire.h"

// I2Cdev and ADXL345 must be installed as libraries, or else the .cpp/.h files
// for both classes must be in the include path of your project
#include "I2Cdev.h"
#include "ADXL345.h"

// class default I2C address is 0x53
// specific I2C addresses may be passed as a parameter here
// ALT low = 0x53 (default for SparkFun 6DOF board)
// ALT high = 0x1D
ADXL345 accel;

int16_t ax, ay, az;
int16_t axp, ayp, azp;
double ac;
double ac_sum;

int16_t max_ac, min_ac, jump_cnt, jump_cnt_flg = 0;

int8_t risingMode = 0;

int Tx = 6; //전송 보내는핀  
int Rx = 7; //수신 받는핀
SoftwareSerial BtSerial(Tx,Rx);

//#define LED_PIN 13 // (Arduino is 13, Teensy is 6)
//bool blinkState = false;

void setup() {
    // join I2C bus (I2Cdev library doesn't do this automatically)
    Wire.begin();

    // initialize serial communication
    // (38400 chosen because it works as well at 8MHz as it does at 16MHz, but
    // it's really up to you depending on your project)
    Serial.begin(9600);

    // initialize device
    Serial.println("Initializing I2C devices...");
    accel.initialize();

    // verify connection
    Serial.println("Testing device connections...");
    Serial.println(accel.testConnection() ? "ADXL345 connection successful" : "ADXL345 connection failed");
    Serial.println( "Sign Count" );
    // configure LED for output
    //pinMode(LED_PIN, OUTPUT);

    BtSerial.begin(9600);

    BtSerial.write((char)0x00);
    
}

void loop() {
    // read raw accel measurements from device
    accel.getAcceleration(&ax, &ay, &az);

    // display tab-separated accel x/y/z values
   // Serial.print("accel:\t");
   // Serial.print(ax); Serial.print("\t");
   // Serial.print(ay); Serial.print("\t");
   // Serial.print(az); Serial.print("\t");
    //ac = sqrt((double)ax * (double)ax);
    //Serial.println(ac);
    ac = sqrt((double)ay * (double)ay);
    //Serial.println(ac);
    ac += sqrt((double)az * (double)az);

    ac_sum = ac_sum * 9 + ac;
    ac_sum /= 10;

    if( max_ac <= ac_sum ){
       max_ac = ac_sum;
       if( (max_ac - min_ac) > 400 && jump_cnt_flg == 0 ) {
          jump_cnt++;
          jump_cnt_flg = 1;
          min_ac = ac_sum;
          BtSerial.write((char)jump_cnt);
       }     
    }
    if( min_ac > ac_sum ){
      min_ac = ac_sum;
      max_ac = ac_sum;
      jump_cnt_flg = 0;
    }

 
 

    Serial.print(ac_sum);Serial.print("\t");
    Serial.println(jump_cnt);

   // BtSerial.write("5");
    // blink LED to indicate activity
    //blinkState = !blinkState;
    //digitalWrite(LED_PIN, blinkState);


    if (BtSerial.available()) {       
      Serial.write(BtSerial.read());
    }
    if (Serial.available()) {
      //Serial.write(Serial.read());     
      BtSerial.write(Serial.read());
    }
    
}
