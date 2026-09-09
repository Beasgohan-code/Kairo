package com.kairo.app;

import com.kairo.app.core.VoiceCommandRouter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VoiceCommandRouterTest {
    @Test
    public void routesExplicitCommandsWithoutHijackingOrdinaryDictation() {
        assertEquals(VoiceCommandRouter.Action.NEW_CHAT, VoiceCommandRouter.route("new chat").getAction());
        assertEquals(VoiceCommandRouter.Action.DEEP, VoiceCommandRouter.route("deep mode").getAction());
        assertEquals(VoiceCommandRouter.Action.ARENA, VoiceCommandRouter.route("open arena").getAction());
        assertEquals(VoiceCommandRouter.Action.CAMERA, VoiceCommandRouter.route("take photo").getAction());
        assertEquals(VoiceCommandRouter.Action.CREATE_FILE, VoiceCommandRouter.route("create file").getAction());
        assertEquals(VoiceCommandRouter.Action.CREATE_SKILL, VoiceCommandRouter.route("skill creator").getAction());
        assertEquals(VoiceCommandRouter.Action.SKILLS, VoiceCommandRouter.route("open skills").getAction());

        VoiceCommandRouter.Result dictation = VoiceCommandRouter.route("please list the files in src");
        assertEquals(VoiceCommandRouter.Action.DICTATE, dictation.getAction());
        assertTrue(dictation.isDictate());
        assertEquals("please list the files in src", dictation.getDictated());

        assertEquals(VoiceCommandRouter.Action.DICTATE, VoiceCommandRouter.route("send this later").getAction());
        assertEquals(VoiceCommandRouter.Action.SEND, VoiceCommandRouter.route("send").getAction());
    }
}
