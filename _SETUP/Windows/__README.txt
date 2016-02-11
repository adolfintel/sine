To build the installer, you'll need Inno Setup (http://www.jrsoftware.org/isinfo.php).

Steps to build installer:
-Compile SINE, SINE-Editor and SINE-CLI
-Inside each project's directory, you'll find a folder named dist: copy all the files from these directories into setupFiles, don't worry about overwriting existing files
-Copy editor_manual from SINE-Editor directoryinto setupFiles
-Extract a Java JRE into the jre folder
-Create the 3 launchers using launch4j (http://launch4j.sourceforge.net/) and the projects in the launch4j folder, and copy the 3 .exe files into setupFiles
-At this point, setupFiles should contain SINE.exe, SINE.jar, SINE-Editor.exe, SINE-Editor.jar, SINE-CLI.exe, SINE-CLI.jar, presetIcon.ico, a folder named lib with a bunch of jar files in it, editor_manual with a bunch of html and png files, and jre with a java runtime into it (bin, lib, ...)
-Compile setup.iss with Inno Setup Compiler
-OPTIONAL: sign the installer exe file using your pkf certificate
