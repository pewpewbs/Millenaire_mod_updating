package org.millenaire.client.gui.text;

import com.mojang.blaze3d.vertex.PoseStack;
import io.netty.buffer.Unpooled;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.chat.TextComponent;
import org.millenaire.common.config.TestTuning;
import org.millenaire.common.network.MillNetwork;
import org.millenaire.common.network.ServerReceiver;

/** Server-confirmed session-only controls; no client config writes. */
public final class GuiTestTuning extends Screen {
    private static final String PREFIX = "millenaire.test_tuning.";
    private static final String[] LABELS = {"move","work","yield","build"};
    private final Screen parent;
    private final Button[] factors = new Button[4];
    private Button reset;
    private TestTuning.Settings settings = TestTuning.DEFAULT;
    private boolean authorized, pending = true;
    public GuiTestTuning(Screen parent) {
        super(new TranslatableComponent(PREFIX+"title"));
        this.parent = parent;
    }
    @Override protected void init() {
        int w = Math.min(260, this.width - 20), x = (this.width - w) / 2;
        int y = Math.max(32, (this.height - 150) / 2);
        for (int i = 0; i < 4; i++) {
            final int field = i;
            factors[i] = addRenderableWidget(new Button(x,y+i*23,w,20,new TextComponent(""),
                    button -> send(field, TestTuning.next(field, settings.value(field)))));
        }
        reset = addRenderableWidget(new Button(x,y+96,w,20,new TranslatableComponent(PREFIX+"reset"),
                button -> send(TestTuning.RESET,100)));
        addRenderableWidget(new Button(x,y+119,w,20,new TranslatableComponent("gui.back"),
                button -> onClose()));
        send(TestTuning.QUERY,100);
    }
    private void send(int field,int value) {
        if (field != TestTuning.QUERY && (!authorized || pending)) return;
        pending = true;
        refresh();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeInt(ServerReceiver.PACKET_TEST_TUNING_REQUEST);
            buf.writeInt(field);
            buf.writeInt(value);
            MillNetwork.sendToServer(buf);
        } finally { buf.release(); }
    }
    public void receiveState(boolean allowed,TestTuning.Settings state) {
        authorized = allowed;
        settings = state;
        pending = false;
        refresh();
    }
    private void refresh() {
        for (int i=0;i<4;i++) if (factors[i]!=null) {
            String multiplier = java.math.BigDecimal.valueOf(settings.value(i),2).stripTrailingZeros().toPlainString();
            factors[i].setMessage(new TranslatableComponent(PREFIX+LABELS[i]).append(": "+multiplier+"×"));
            factors[i].active = authorized && !pending;
        }
        if (reset != null) reset.active = authorized && !pending;
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void render(PoseStack pose,int mouseX,int mouseY,float partialTick) {
        renderBackground(pose);
        drawCenteredString(pose,font,title,width/2,9,0xFFFFFF);
        String status = pending ? "waiting" : authorized ? "session" : "denied";
        drawCenteredString(pose,font,new TranslatableComponent(PREFIX+status),width/2,21,0xCCCCCC);
        super.render(pose,mouseX,mouseY,partialTick);
    }
}
