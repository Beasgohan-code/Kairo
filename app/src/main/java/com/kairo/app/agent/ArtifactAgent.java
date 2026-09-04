package com.kairo.app.agent;

import com.kairo.app.data.Artifact;

/** Clear boundary used by the UI when an agent wants to turn code into a reviewable artifact. */
public interface ArtifactAgent {
    Artifact create(String name, String language, String content) throws Exception;
}
