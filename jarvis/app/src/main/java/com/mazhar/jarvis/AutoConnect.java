package com.mazhar.jarvis;
import org.json.*;
final class AutoConnect {
 static JSONObject detect(String key){try{
  if(key.startsWith("AIza")) return Ai.obj("known",true,"name","Google Gemini","key",key,"url","https://generativelanguage.googleapis.com/v1beta","model","gemini-2.5-flash","type","gemini");
  if(key.startsWith("sk-or-")) return Ai.obj("known",true,"name","OpenRouter","key",key,"url","https://openrouter.ai/api/v1","model","openai/gpt-4o-mini","type","chat");
  if(key.startsWith("gsk_")) return Ai.obj("known",true,"name","Groq","key",key,"url","https://api.groq.com/openai/v1","model","llama-3.3-70b-versatile","type","chat");
  if(key.startsWith("xai-")) return Ai.obj("known",true,"name","Grok / xAI","key",key,"url","https://api.x.ai/v1","model","grok-3-mini","type","responses");
  if(key.startsWith("sk-")||key.startsWith("sess-")) return Ai.obj("known",true,"name","OpenAI","key",key,"url","https://api.openai.com/v1","model","gpt-4o-mini","type","chat");
  return Ai.obj("known",false,"name","Unknown");
 }catch(Exception e){return new JSONObject();}}
}
