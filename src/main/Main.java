package main;

import alfabeta.AlfaBetaAIDeepening;
import app.StartDesktopApp;
import manager.ai.AIRegistry;

public class Main {

    public static void main(String[] args) {

        // Register AIs

        if (!AIRegistry.registerAI("AlfaBetaDeepening AI", () -> {
            return new AlfaBetaAIDeepening();
        }, (game) -> {
            return new AlfaBetaAIDeepening().supportsGame(game);
        }))
            System.err.println("WARNING! Failed to register AI because one with that name already existed!");


        StartDesktopApp.main(new String[0]);

    }

}
