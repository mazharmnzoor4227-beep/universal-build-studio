package com.mazhar.mediapocket
import org.junit.Test
import org.junit.Assert.*
class LinkRulesTest {
 @Test fun acceptsSharedText(){assertEquals("https://youtu.be/test",LinkRules.parse("Watch https://youtu.be/test"))}
 @Test fun rejectsLookalike(){assertTrue(runCatching {LinkRules.parse("https://youtube.com.evil.example/a")}.isFailure)}
 @Test fun rejectsCredentials(){assertTrue(runCatching {LinkRules.parse("https://user@youtube.com/a")}.isFailure)}
 @Test fun rejectsLocal(){assertTrue(runCatching {LinkRules.parse("https://127.0.0.1/a")}.isFailure)}
}
