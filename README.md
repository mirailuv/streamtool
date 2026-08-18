INSTALL

clone the repository

build the program with\
$ gradle build

rename obs_layout.json.example to obs_layout.json, it should work with the setup without additional config.\
if new things have been added to the config you'll need to update the file manually.

make sure obs websocket is enabled with port 4455 and authentication disabled.\
create a symbolic link to spectate_match.json in the folder where you run the program. it must be named spectate_match.json as well.\
$ ln -s path/to/spectate_match.json spectate_match.json

there's a different command for this on windows you can look that up.

now you can run the program with\
$ java -jar app/build/libs/app.jar

how to use: usage.txt\
command list: commands.txt
