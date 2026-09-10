package com.mazhar.mediapocket
import java.net.URI
object LinkRules {
 private val domains=listOf("instagram.com","facebook.com","fb.watch","tiktok.com","pinterest.com","pin.it","youtube.com","youtu.be")
 fun parse(text:String):String {
  val candidate=Regex("https://[^\\s<>]+").find(text)?.value?.trimEnd('.',',',')') ?: error("Paste an HTTPS link")
  val uri=URI(candidate);val host=uri.host?.lowercase() ?: error("Invalid link")
  require(uri.userInfo==null && (uri.port==-1 || uri.port==443)) { "Invalid link" }
  require(domains.any { host==it || host.endsWith(".$it") }) { "Use Instagram, Facebook, TikTok, Pinterest or YouTube" }
  return candidate
 }
}
