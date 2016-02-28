### To build the SINE.app package:
- download a Mac JRE
- extract it into SINE.app/Contents/MacOS/jre. You should have bin, lib, man, ... folders into it.
- copy SINE.jar and the lib folder into SINE.app/Contents/MacOS



### To build the SINE-Editor.app package:
- download a Mac JRE
- extract it into SINE Editor.app/Contents/MacOS/jre. You should have bin, lib, man, ... folders into it.
- copy SINE-Editor.jar, the lib folder and the editor_manual folder into SINE Editor.app/Contents/MacOS

Finally, to create the .dmg file, use this script on a Mac https://github.com/andreyvit/create-dmg

### Hold on, what's that "SINE" blob I see in Contents/MacOS?
It's a native launcher, taken from this project https://github.com/libgdx/packr
