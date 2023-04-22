package com.hupubao.fabric.mod.worker;

import cn.hutool.core.img.ColorUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.hupubao.fabric.mod.MainScreen;
import com.hupubao.fabric.mod.bean.BlockBuildTask;
import com.hupubao.fabric.mod.bean.BuildTask;
import com.hupubao.fabric.mod.utils.ColorUtils;
import com.hupubao.fabric.mod.utils.GaussianBlurUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.Chunk;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildingWorker {

    private BuildTask buildTask;

    private final Queue<BlockBuildTask> queue = new LinkedBlockingDeque<>();

    private boolean building = true;

    public static final Logger LOGGER = LoggerFactory.getLogger("photo2building");

    public BuildingWorker(BuildTask buildTask) {
        this.buildTask = buildTask;
    }

    public void sliceAndEnqueue(BufferedImage bufferedImage) {

        int rows = buildTask.getRows();

        // 图片宽高
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        // 计算每个切片大小(以图片高度为基准)超出部分不要了
        int pieceSize = height / rows;

        int column = width / pieceSize;

        // 计算总共方块数
        int blockNums = rows * column;


        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < column; j++) {
                BufferedImage sliceImg = bufferedImage.getSubimage(j * pieceSize, i * pieceSize, pieceSize, pieceSize);

                BlockPos startPos = buildTask.getStartPos();

                BlockPos playerOnPos = buildTask.getPlayerOnPos();

                int x = startPos.getX();
                int y = startPos.getY();
                int z = startPos.getZ();


                if (playerOnPos.getX() - startPos.getX() > 0) {
                    z = z - j;
                    if (buildTask.getHorizontal()){
                        x = x + i;
                    }
                } else if (playerOnPos.getX() - startPos.getX() < 0) {
                    z = z + j;
                    if (buildTask.getHorizontal()) {
                        x = x - i;
                    }
                } else if (playerOnPos.getZ() - startPos.getZ() > 0) {
                    x = x + j;
                    if (buildTask.getHorizontal()) {
                        z = z + i;
                    }
                } else if (playerOnPos.getZ() - startPos.getZ() < 0) {
                    x = x - j;
                    if (buildTask.getHorizontal()) {
                        z = z - i;
                    }
                }

                if (!buildTask.getHorizontal()) {
                    y = y - i;
                }

                BlockPos blockPos = new BlockPos(x, y, z);



                // 检查位置是否合法
                ClientWorld world = MinecraftClient.getInstance().world;

                if (world != null) {
                    Chunk chunk = world.getChunk(blockPos);
                    if (world.isOutOfHeightLimit(blockPos)) {
                        // 超出世界范围
                        blockPos = null;
                    }

                    // 区块未加载
                    if (!world.isChunkLoaded(chunk.getPos().x, chunk.getPos().z)) {
                        blockPos = null;
                    }
                }



                int currentNums = i * column + (j + 1);
                BlockBuildTask blockBuildTask = new BlockBuildTask(sliceImg, blockPos, blockNums, currentNums);
                queue.offer(blockBuildTask);

            }
        }

    }

    public void startBuild() {

        sliceAndEnqueue(buildTask.getBufferedImage());

        while (building) {

            if (queue.isEmpty()) {
                ThreadUtil.sleep(20);
                continue;
            }


            BlockBuildTask blockBuildTask = queue.poll();
            if (blockBuildTask == null) {
                continue;
            }

            int currentRow = blockBuildTask.getCurrentNums() % buildTask.getRows() == 0 ?
                    blockBuildTask.getCurrentNums() / buildTask.getRows() : blockBuildTask.getCurrentNums() / buildTask.getRows() + 1;
            if (buildTask.getRows() >= 50 && currentRow < 4) {
                ThreadUtil.sleep(15);
            }

            convertAndPlaceBlock(blockBuildTask);

            if (blockBuildTask.getCurrentNums() == blockBuildTask.getBlockNums()) {
                stopBuild();
            }
        }


        if (MainScreen.executorServiceDoBuild.getActiveCount() == 1) {
            buildTask.getPlayer().sendChatMessage("/gamerule sendCommandFeedback true");
        }
        buildTask.getPlayer().sendMessage(new LiteralText("建造结束..."), false);

    }




    private void convertAndPlaceBlock(BlockBuildTask blockBuildTask) {

        ClientPlayerEntity player = buildTask.getPlayer();
        try {

            int radius = Math.min(blockBuildTask.getImagePiece().getWidth(),
                    blockBuildTask.getImagePiece().getHeight()) / 3;
            BufferedImage image = blockBuildTask.getImagePiece();
            GaussianBlurUtils.blur(image, radius);

            Color color = ColorUtil.hexToColor(ColorUtil.getMainColor(image));


            Block block = getNearestColorBlock(color);


            BlockPos position = blockBuildTask.getBlockPos();

            if (position == null) {
                return;
            }


            System.out.println("主色调：" + ColorUtil.toHex(color) +
                    ",选择的方块是：" + block.getName().getString() + "," + block.getTranslationKey() +
                    ",位置：" + position.getX() + "," + position.getY() + "," + position.getZ());

            Identifier blockIdentifier = Registry.BLOCK.getId(block);
            String setBlockCommand = String.format("/fill %d %d %d %d %d %d %s",
                    position.getX(), position.getY(), position.getZ(),
                    position.getX(), position.getY(), position.getZ(),
                    blockIdentifier);
            if (buildTask.getReplaceBlock()) {
                setBlockCommand += " replace";
            }

            player.sendChatMessage(setBlockCommand);


        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(new LiteralText("出错啦：" + e.getMessage()), false);
        }
    }


    private Block getNearestColorBlock(Color color) {

        Map<Color, Block> colorBlockMap = buildTask.getColorBlockMap();

        Color nearestColor = ColorUtils.findClosestPaletteColorTo(color, colorBlockMap.keySet());

        return colorBlockMap.get(nearestColor);
    }


    public BuildTask getBuildTask() {
        return buildTask;
    }

    public void setBuildTask(BuildTask buildTask) {
        this.buildTask = buildTask;
    }

    public void stopBuild() {
        building = false;
    }
}
