package org.millenaire.client.gui.text;

import com.mojang.blaze3d.vertex.PoseStack;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.network.MillNetwork;
import org.millenaire.common.network.ServerReceiver;

/** Nearby villager selector with read-only, server-confirmed AI observations. */
public final class GuiGoalDiagnostics extends Screen {
    private static final String PREFIX = "millenaire.goal_diagnostics.";
    private final Screen parent;
    private final List<MillVillager> nearby = new ArrayList<>();
    private List<String> rows = List.of();
    private int selectedId = -1, page = 0, ticks = 0;
    private Button previous, next, previousPage, nextPage;
    public GuiGoalDiagnostics(Screen parent) {
        super(new TranslatableComponent(PREFIX+"title"));
        this.parent = parent;
    }
    @Override protected void init() {
        int cx = width / 2, y = height - 27;
        previous = addRenderableWidget(new Button(cx-106,32,32,20,new TextComponent("<"), b -> select(-1)));
        next = addRenderableWidget(new Button(cx+74,32,32,20,new TextComponent(">"), b -> select(1)));
        addRenderableWidget(new Button(cx-66,32,132,20,new TranslatableComponent(PREFIX+"rescan"), b -> rescan()));
        previousPage = addRenderableWidget(new Button(cx-106,y,45,20,new TextComponent("<"), b -> { if (page>0) page--; }));
        nextPage = addRenderableWidget(new Button(cx+61,y,45,20,new TextComponent(">"), b -> page++));
        addRenderableWidget(new Button(cx-55,y,110,20,new TranslatableComponent("gui.back"), b -> onClose()));
        rescan();
    }
    private void rescan() {
        nearby.clear();
        if (minecraft != null && minecraft.player != null && minecraft.level != null && minecraft.player.hasPermissions(2)) {
            nearby.addAll(minecraft.level.getEntitiesOfClass(MillVillager.class,
                    minecraft.player.getBoundingBox().inflate(16),
                    v -> v.isAlive() && minecraft.player.distanceToSqr(v) <= 256.0));
            nearby.sort(Comparator.comparingDouble(v -> minecraft.player.distanceToSqr(v)));
        }
        if (nearby.stream().noneMatch(v -> v.getId() == selectedId)) {
            selectedId = nearby.isEmpty() ? -1 : nearby.get(0).getId();
            rows = List.of();
            page = 0;
        }
        previous.active = next.active = nearby.size() > 1;
        request();
    }
    private void select(int delta) {
        if (nearby.isEmpty()) return;
        int index = 0;
        for (int i=0;i<nearby.size();i++) if (nearby.get(i).getId()==selectedId) { index=i; break; }
        selectedId = nearby.get(Math.floorMod(index+delta,nearby.size())).getId();
        rows = List.of();
        page = 0;
        request();
    }
    private void request() {
        if (selectedId < 0 || minecraft == null || minecraft.player == null || !minecraft.player.hasPermissions(2)) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeInt(ServerReceiver.PACKET_GOAL_DIAGNOSTICS_REQUEST);
            buf.writeInt(selectedId);
            MillNetwork.sendToServer(buf);
        } finally { buf.release(); }
    }
    public void receiveState(int entityId, List<String> received) {
        if (entityId != selectedId) return; // stale response for another selected villager
        rows = List.copyOf(received);
    }
    @Override public void tick() {
        super.tick();
        if (++ticks % 40 == 0) request();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void render(PoseStack pose,int mouseX,int mouseY,float partialTick) {
        renderBackground(pose);
        drawCenteredString(pose,font,title,width/2,8,0xFFFFFF);
        String name = nearby.stream().filter(v -> v.getId()==selectedId)
                .map(v -> v.getName().getString()).findFirst().orElse("-");
        drawCenteredString(pose,font,new TextComponent(name),width/2,20,0xDDDDDD);
        int left = Math.max(10,(width-420)/2), usable = width-2*left;
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        if (rows.isEmpty()) {
            wrapped.addAll(font.split(new TranslatableComponent(PREFIX+(selectedId<0?"none":"waiting")),usable));
        } else {
            for (String row : rows) wrapped.addAll(font.split(new TextComponent(row),usable));
        }
        int capacity = Math.max(1,(height-96)/10);
        int lastPage = Math.max(0,(wrapped.size()-1)/capacity);
        page = Math.max(0,Math.min(page,lastPage));
        int start=page*capacity;
        for (int i=start;i<Math.min(wrapped.size(),start+capacity);i++)
            font.draw(pose,wrapped.get(i),left,59+(i-start)*10,0xFFFFFF);
        previousPage.active=page>0;
        nextPage.active=page<lastPage;
        drawCenteredString(pose,font,new TextComponent((page+1)+"/"+(lastPage+1)),width/2,height-38,0xCCCCCC);
        super.render(pose,mouseX,mouseY,partialTick);
    }
}
