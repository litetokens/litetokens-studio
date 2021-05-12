## Litetokens Studio
LitetokensStudio is an IDE to develop/deploy/debug smart contract based on TVM.

## Disable auto complete:
change studio.autocomplete to false in src/main/resources/studio.properties
and re-build amd run

## Screenshot
![](image/screenshot.png)

## System Requirement
Oracle JDK 1.8

- Windows 64Bit
- Linux 64Bit
- Mac


## Compile & Run
```
./gradlew build -x test -x check
cd  build/libs
java -jar LitetokensStudio.jar
```
## Compile contract Nullpointer quick fix:
 https://gitub.com/tronprotocol/tron-studio/issues/8 
