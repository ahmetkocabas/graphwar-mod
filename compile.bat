dir /s /b src\*.java > sources.txt
mkdir bin
javac -d bin @sources.txt
robocopy bin/Graphwar Graphwar /e
robocopy bin/GraphServer GraphServer /e
robocopy bin/GlobalServer GlobalServer /e
robocopy bin/RoomServer RoomServer /e
jar cfe graphwar.jar Graphwar.Graphwar GraphServer Graphwar rsc
jar cfe roomServer.jar RoomServer.RoomServer GraphServer RoomServer rsc
jar cfe globalServer.jar GlobalServer.GlobalServer GraphServer GlobalServer rsc
java -jar graphwar.jar 26.175.161.1