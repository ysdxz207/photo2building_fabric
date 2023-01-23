/*
 * Decompiled with CFR 0.1.1 (FabricMC 57d88659).
 */
package com.hupubao.fabric.mod;

import cn.hutool.core.thread.ThreadUtil;
import com.hupubao.fabric.mod.bean.BuildTask;
import com.hupubao.fabric.mod.utils.GaussianBlurUtils;
import com.hupubao.fabric.mod.utils.ImageColorUtils;
import com.hupubao.fabric.mod.worker.BuildingWorker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.LockButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.Resource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Environment(value=EnvType.CLIENT)
public class MainScreen
extends Screen {
    private static final Text ENTER_NAME_TEXT = new TranslatableText("main.filepath");
    private static final Text ENTER_IP_TEXT = new TranslatableText("main.rows");

    private ButtonWidget addButton;
    private TextFieldWidget filepathField;
    private TextFieldWidget rowsField;
    private CheckboxWidget checkboxReplaceBlock;
    private CheckboxWidget checkboxHorizontal;

    private final Screen parent;


    private final Map<Color, Block> COLOR_BLOCK_MAP = new HashMap<>();

    public static final ThreadPoolExecutor executorServiceDoBuild = ThreadUtil.newExecutor(4, 10);


    public MainScreen(Screen parent) {
        super(new TranslatableText("main.title"));
        this.parent = parent;
    }

    @Override
    public void tick() {
        this.rowsField.tick();
        this.filepathField.tick();
    }

    @Override
    protected void init() {
        this.client.keyboard.setRepeatEvents(true);
        this.filepathField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 50, 200, 20, new TranslatableText("main.filepath"));
        this.filepathField.setTextFieldFocused(true);
        this.filepathField.setMaxLength(500);
        this.filepathField.setSuggestion("支持网络和本地图片");
        this.filepathField.setChangedListener(filepath -> this.updateGenerateButton());
        this.addSelectableChild(this.filepathField);


        this.rowsField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 90, 200, 20, new TranslatableText("main.rows"));
        this.rowsField.setMaxLength(128);
        this.rowsField.setSuggestion("0 ~ 255");
        this.rowsField.setChangedListener(rows -> this.updateGenerateButton());
        this.addSelectableChild(this.rowsField);

        this.checkboxReplaceBlock = new CheckboxWidget(this.width / 2 - 100, 130, 200, 20, new TranslatableText("main.replaceBlock"), true);
        this.checkboxReplaceBlock.active = false;
        this.addSelectableChild(this.checkboxReplaceBlock);

        this.checkboxHorizontal = new CheckboxWidget(this.width / 2 - 100, 160, 200, 20, new TranslatableText("main.horizontal"), false);
        this.addSelectableChild(checkboxHorizontal);

        this.addButton = this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, this.height / 4 + 140, 200, 20, new TranslatableText("main.generate"), button -> this.generate()));



        this.updateGenerateButton();
    }

    private void generate() {

        if (!checkPlayerState()) {
            return;
        }

        int rows = getRows();

        // 检查照片
        BufferedImage bufferedImage = getPhoto();

        if (bufferedImage == null) {
            return;
        }

        getPlayer().sendMessage(new LiteralText("开始建造..."), false);

        boolean isHorizontal = checkboxHorizontal.isChecked();

        BlockPos startPos = getStartPos(rows, isHorizontal);

        BlockPos playerOnPos = getPlayer().getLandingPos();


        // 解析颜色和方块映射
        parseColorBlockMap();


        BuildTask buildTask = new BuildTask(bufferedImage, startPos,
                playerOnPos, rows, getPlayer(),
                COLOR_BLOCK_MAP, getReplaceBlock(),
                isHorizontal);


        BuildingWorker buildingWorker = new BuildingWorker(buildTask);
        buildTask.getPlayer().sendChatMessage("/gamerule sendCommandFeedback false");
        executorServiceDoBuild.execute(buildingWorker::startBuild);

        // 关闭对话框
        close();

    }

    private boolean getReplaceBlock() {
        return checkboxReplaceBlock.isChecked();
    }

    private BlockPos getStartPos(int rows, boolean isHorizontal) {
        BlockPos playerPointPos = getPlayerPointPos();

        if (isHorizontal) {
            return playerPointPos;
        }
        return new BlockPos(playerPointPos.getX(), playerPointPos.getY() + rows - 1, playerPointPos.getZ());

    }

    private int getRows() {
        try {
            int rows = Integer.parseInt(rowsField.getText());
            if (rows > 0 && rows <= 255) {
                return rows;
            }
            getPlayer().sendMessage(new LiteralText("行数范围：0 ~ 255"), false);
        } catch (NumberFormatException e) {
            return -1;
        }

        return -1;
    }

    private boolean checkPlayerState() {
        BlockPos startPos = getPlayerPointPos();

        if (startPos == null) {
            getPlayer().sendMessage(new LiteralText("请对准实体方块，不要对准天空"), false);
            return false;
        }
        return true;
    }

    private void parseColorBlockMap() {
        // 初始化颜色和方块映射
        try {
            for (Field declaredField : Blocks.class.getDeclaredFields()) {

                if (declaredField.getType() != Block.class) {
                    continue;
                }

                Block block = (Block) declaredField.get(declaredField.getName());

                // 非空气
                if (block instanceof AirBlock) {
                    continue;
                }

                boolean isLiquid = block instanceof FluidBlock;

                // 非液体
                if (isLiquid) {
                    continue;
                }

                Identifier resourceLocation = block.getLootTableId();

                if (resourceLocation == null) {
                    continue;
                }

                // 珊瑚
                if (block.getTranslationKey().contains("coral")) {
                    continue;
                }

                // 床
                if (block.getTranslationKey().contains("bed")) {
                    continue;
                }

                // 门
                if (block instanceof TrapdoorBlock || block instanceof DoorBlock) {
                    continue;
                }

                // 荧石
                if (Blocks.GLOWSTONE.getLootTableId().compareTo(resourceLocation) == 0) {
                    continue;
                }

                if (block.getClass() != Block.class) {
                    continue;
                }

                String resourcePath = resourceLocation.getPath();
                String resourceName = resourcePath.substring(resourcePath.indexOf("/") + 1);
                Identifier location = new Identifier("textures/block/" + resourceName + ".png");

                if (!MinecraftClient.getInstance().getResourceManager().containsResource(location)) {
                    // 资源不存在,可能是grass_block_side.png等，因为有些方块侧面和正面不一样
                    continue;
                }

                Resource resource = MinecraftClient.getInstance().getResourceManager().getResource(location);
                BufferedImage image = ImageIO.read(resource.getInputStream());

                int radius = Math.min(image.getWidth(), image.getHeight()) / 3;
                GaussianBlurUtils.blur(image, radius);

                Color color = ImageColorUtils.getAvgRGB(image);
//                Color color = ColorUtil.hexToColor(ImgUtil.getMainColor(image));

                COLOR_BLOCK_MAP.put(color, block);
            }
        } catch (IOException | IllegalAccessException e) {
            e.printStackTrace();
            getPlayer().sendMessage(new LiteralText("解析颜色-方块映射失败：" + e.getMessage()), false);
        }

    }


    /**
     * 获取玩家指向的位置
     *
     * @return
     */
    private BlockPos getPlayerPointPos() {

        if (MinecraftClient.getInstance().crosshairTarget.getType() == HitResult.Type.MISS) {
            return null;
        }
        return new BlockPos(MinecraftClient.getInstance().crosshairTarget.getPos());
    }


    private BufferedImage getPhoto() {


        try {

            String filepath = filepathField.getText().replace("\"", "");

            try {
                return ImageIO.read(new File(filepath));
            } catch (Exception e) {
                return ImageIO.read(new URL(filepath));
            }

        } catch (IOException e) {
            getPlayer().sendMessage(new LiteralText("图片路径不正确"), false);
            return null;
        }

    }


    private ClientPlayerEntity getPlayer() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        return player;
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        String string = this.filepathField.getText();
        String string2 = this.rowsField.getText();
        this.init(client, width, height);
        this.filepathField.setText(string);
        this.rowsField.setText(string2);
    }

    @Override
    public void removed() {
        this.client.keyboard.setRepeatEvents(false);
    }


    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    private void updateGenerateButton() {
        this.addButton.active = getRows() > 0 && !this.filepathField.getText().isEmpty();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        MainScreen.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 17, Color.GREEN.getRGB());
        MainScreen.drawTextWithShadow(matrices, this.textRenderer, ENTER_NAME_TEXT, this.width / 2 - 100, 38, Color.LIGHT_GRAY.getRGB());
        MainScreen.drawTextWithShadow(matrices, this.textRenderer, ENTER_IP_TEXT, this.width / 2 - 100, 78, Color.LIGHT_GRAY.getRGB());
        this.rowsField.render(matrices, mouseX, mouseY, delta);
        this.filepathField.render(matrices, mouseX, mouseY, delta);
        this.checkboxReplaceBlock.render(matrices, mouseX, mouseY, delta);
        this.checkboxHorizontal.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }
}

