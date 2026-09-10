package com.mazhar.jarvis;
import java.nio.file.*;import java.nio.charset.StandardCharsets;
final class Fs {
 static String readString(Path p)throws java.io.IOException{return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);}
 static void writeString(Path p,String s)throws java.io.IOException{Files.write(p,s.getBytes(StandardCharsets.UTF_8));}
}
