package com.robertx22.age_of_exile.mmorpg.registers.client;

public class ClientSetup {

    public static void setup() {

        RenderLayersRegister.setup();
        ContainerGuiRegisters.reg();
        S2CPacketRegister.register();

    }
}
