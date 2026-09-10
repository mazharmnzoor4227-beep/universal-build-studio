package com.mazhar.jarvis;
import org.junit.Test;import org.json.*;import static org.junit.Assert.*;
public class AiTest {
 @Test public void protocolsPreserveConversation()throws Exception {JSONArray h=new JSONArray().put(Ai.obj("role","user","content","hello")).put(Ai.obj("role","assistant","content","hi")).put(Ai.obj("role","user","content","describe"));for(String type:new String[]{"chat","responses","messages","gemini"}){JSONObject p=Ai.obj("type",type,"model","test");String s=Ai.payload(p,new JSONArray(h.toString()),"rules","YWJj").toString();assertTrue(s.contains("hello"));assertTrue(s.contains("YWJj"));assertTrue(s.contains("rules"));}}
 @Test public void extractsOnlyUserFacingResponse()throws Exception{assertEquals("hello",Ai.extract("responses",new JSONObject("{\"output\":[{\"type\":\"reasoning\"},{\"content\":[{\"type\":\"output_text\",\"text\":\"hello\"}]}]}")));assertEquals("hello",Ai.extract("gemini",new JSONObject("{\"candidates\":[{\"content\":{\"parts\":[{\"thought\":true,\"text\":\"hidden\"},{\"text\":\"hello\"}]}}]}")));}
 @Test(expected=Exception.class) public void rejectsPartialProjects()throws Exception{Ai.html("<html><body>unfinished");}
 @Test public void stripsMarkdownFromProject()throws Exception{assertEquals("<html>ok</html>",Ai.html("```html\n<html>ok</html>\n```"));}
 @Test(expected=Exception.class)public void rejectsInsecureApi()throws Exception{Ai.endpoint(Ai.obj("url","http://example.com"));}
}
