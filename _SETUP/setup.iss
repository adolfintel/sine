[Setup]
AppId={{1E6C2953-D449-4B16-A7EA-FA50E0512B8F}
AppName="SINE Isochronic Entrainer"
AppVersion="1.8.3"
AppPublisher="Federico Dossena"
AppPublisherURL="http://sine.adolfintel.com"
AppSupportURL="http://sine.adolfintel.com"
AppUpdatesURL="http://sine.adolfintel.com"
DefaultDirName={pf}\SINE
DefaultGroupName="SINE Isochronic Entrainer"
DisableProgramGroupPage=yes
LicenseFile=gpl-3.0.txt
OutputDir=.
OutputBaseFilename=setup
Compression=lzma2/ultra64
LZMAAlgorithm=1
LZMAMatchFinder=BT
SolidCompression=yes
LZMANumBlockThreads=1
LZMANumFastBytes=273
LZMADictionarySize=262144
LZMAUseSeparateProcess=yes
InternalCompressLevel=ultra64
SetupIconFile="player.ico"
UninstallDisplayIcon="player.ico"

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "italian"; MessagesFile: "compiler:Languages\Italian.isl"

[Files]
Source: "setupFiles\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension;

[Icons]
Name: "{group}\SINE Isochronic Entrainer"; Filename: "{app}\SINE.exe"
Name: "{group}\SINE Editor"; Filename: "{app}\SINE-Editor.exe"
Name: "{userdesktop}\SINE Isochronic Entrainer"; Filename: "{app}\SINE.exe"
Name: "{userdesktop}\SINE Editor"; Filename: "{app}\SINE-Editor.exe"

[Registry]
Root: HKCR; Subkey: ".sin"; ValueType: string; ValueName: ""; ValueData: "SINEPreset"; Flags: uninsdeletevalue 
Root: HKCR; Subkey: "SINEPreset"; ValueType: string; ValueName: ""; ValueData: "SINE Preset"; Flags: uninsdeletekey 
Root: HKCR; Subkey: "SINEPreset\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\presetIcon.ico,0" 
Root: HKCR; Subkey: "SINEPreset\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\SINE.exe"" ""%1""" 
Root: HKCR; Subkey: "SINEPreset\shell\Edit"; ValueType: string; ValueName: ""; ValueData: "Edit"; Languages: english; Flags: uninsdeletekey 
Root: HKCR; Subkey: "SINEPreset\shell\Edit"; ValueType: string; ValueName: ""; ValueData: "Modifica"; Languages: italian; Flags: uninsdeletekey 
Root: HKCR; Subkey: "SINEPreset\shell\Edit\command"; ValueType: string; ValueName: ""; ValueData: """{app}\SINE-Editor.exe"" ""%1""" 
Root: HKLM; Subkey: "SYSTEM\CurrentControlSet\Control\Session Manager\Environment"; ValueType: expandsz; ValueName: "Path"; ValueData: "{olddata};""{app}"";"
