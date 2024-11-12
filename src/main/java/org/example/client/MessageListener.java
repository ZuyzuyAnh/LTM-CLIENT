package org.example.client;

import org.example.dto.Message;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

public interface MessageListener {
    void onMessageReceived(Message message) throws IOException, UnsupportedAudioFileException, LineUnavailableException;
}

