package com.mazhar.jarvis;
import org.json.*;
import okhttp3.*;
import java.util.concurrent.TimeUnit;
final class Ai {
 static final OkHttpClient HTTP=new OkHttpClient.Builder().connectTimeout(25,TimeUnit.SECONDS).callTimeout(180,TimeUnit.SECONDS).followRedirects(false).build();
 static JSONObject obj(Object...v)throws JSONException {JSONObject o=new JSONObject();for(int i=0;i<v.length;i+=2)o.put((String)v[i],v[i+1]);return o;}
 static String endpoint(JSONObject p)throws Exception {String b=p.getString("url").replaceAll("/+$","");HttpUrl u=HttpUrl.parse(b);if(u==null||!u.isHttps()||!u.username().isEmpty()||!u.password().isEmpty())throw new Exception("Use a valid HTTPS API base URL");return b;}
 static JSONArray messages(JSONArray history,String system)throws Exception {JSONArray a=new JSONArray().put(obj("role","system","content",system));for(int i=Math.max(0,history.length()-24);i<history.length();i++)a.put(history.get(i));return a;}
 static JSONObject payload(JSONObject p,JSONArray history,String system,String image)throws Exception {
  String type=p.optString("type","chat"),model=p.getString("model");JSONArray a=messages(history,system);
  if(type.equals("chat")) {if(!image.isEmpty()){JSONObject last=a.getJSONObject(a.length()-1);last.put("content",new JSONArray().put(obj("type","text","text",last.getString("content"))).put(obj("type","image_url","image_url",obj("url","data:image/jpeg;base64,"+image))));}return obj("model",model,"messages",a,"max_tokens",8192);}
  if(type.equals("responses")){JSONArray input=new JSONArray();for(int i=1;i<a.length();i++){JSONObject m=a.getJSONObject(i);String role=m.getString("role");JSONArray parts=new JSONArray().put(obj("type",role.equals("assistant")?"output_text":"input_text","text",m.getString("content")));if(i==a.length()-1&&!image.isEmpty())parts.put(obj("type","input_image","image_url","data:image/jpeg;base64,"+image));input.put(obj("role",role,"content",parts));}return obj("model",model,"instructions",system,"input",input,"max_output_tokens",8192,"store",false);}
  if(type.equals("messages")){JSONArray input=new JSONArray();for(int i=1;i<a.length();i++){JSONObject m=a.getJSONObject(i);JSONArray parts=new JSONArray().put(obj("type","text","text",m.getString("content")));if(i==a.length()-1&&!image.isEmpty())parts.put(obj("type","image","source",obj("type","base64","media_type","image/jpeg","data",image)));input.put(obj("role",m.getString("role"),"content",parts));}return obj("model",model,"system",system,"messages",input,"max_tokens",8192);}
  if(type.equals("gemini")){JSONArray input=new JSONArray();for(int i=1;i<a.length();i++){JSONObject m=a.getJSONObject(i);JSONArray parts=new JSONArray().put(obj("text",m.getString("content")));if(i==a.length()-1&&!image.isEmpty())parts.put(obj("inline_data",obj("mime_type","image/jpeg","data",image)));input.put(obj("role",m.getString("role").equals("assistant")?"model":"user","parts",parts));}return obj("system_instruction",obj("parts",new JSONArray().put(obj("text",system))),"contents",input,"generationConfig",obj("maxOutputTokens",8192));}
  throw new Exception("Unsupported API protocol");
 }
 static String ask(JSONObject p,JSONArray history,String system,String image)throws Exception {
  String type=p.optString("type","chat"),base=endpoint(p),model=p.getString("model");
  if(!model.matches("[a-zA-Z0-9_./:-]+"))throw new Exception("Invalid model ID");
  String path=type.equals("responses")?"/responses":type.equals("messages")?"/messages":type.equals("gemini")?"/models/"+model+":generateContent":"/chat/completions";
  Request.Builder r=new Request.Builder().url(base+path).post(RequestBody.create(payload(p,history,system,image).toString(),MediaType.get("application/json")));
  if(type.equals("messages"))r.header("x-api-key",p.getString("key")).header("anthropic-version","2023-06-01");else if(type.equals("gemini"))r.header("x-goog-api-key",p.getString("key"));else r.header("Authorization","Bearer "+p.getString("key"));
  try(Response response=HTTP.newCall(r.build()).execute()){if(!response.isSuccessful())throw new Exception("API HTTP "+response.code()+": "+hint(response.code()));JSONObject json=new JSONObject(response.body().string());return extract(type,json);}
 }
 static String hint(int c){return c==401||c==403?"Check this provider's key and model access":c==429?"Rate limit or credits exhausted. Wait or select another saved provider.":c==404?"Check Base URL, API protocol and Model ID":"Provider request failed; retry or change connection";}
 static String extract(String type,JSONObject j)throws Exception {
  StringBuilder s=new StringBuilder();
  if(type.equals("chat"))s.append(j.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content",""));
  if(type.equals("messages"))append(s,j.getJSONArray("content"),"text");
  if(type.equals("gemini"))append(s,j.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts"),"text");
  if(type.equals("responses")){JSONArray a=j.getJSONArray("output");for(int i=0;i<a.length();i++){JSONArray c=a.getJSONObject(i).optJSONArray("content");if(c!=null)append(s,c,"text");}}
  if(s.toString().isBlank())throw new Exception("No text returned. Check model support or refusal details in the provider console.");return s.toString();
 }
 static void append(StringBuilder s,JSONArray a,String field)throws Exception{for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);if(!o.optBoolean("thought",false))s.append(o.optString(field,""));}}
 static String html(String text)throws Exception {int start=text.toLowerCase().indexOf("<!doctype html");if(start<0)start=text.toLowerCase().indexOf("<html");int end=text.toLowerCase().lastIndexOf("</html>");if(start<0||end<start)throw new Exception("Model did not return a complete HTML project. Ask it to finish the file.");return text.substring(start,end+7);}
}
