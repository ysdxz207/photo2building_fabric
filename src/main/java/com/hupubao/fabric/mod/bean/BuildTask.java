package com.hupubao.fabric.mod.bean;

import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

public class BuildTask {

    private BufferedImage bufferedImage;

    private BlockPos startPos;

    private BlockPos playerOnPos;

    private int rows;

    private ClientPlayerEntity player;

    private Map<Color, Block> colorBlockMap;

    private boolean replaceBlock;

    private boolean horizontal;

    public BuildTask(BufferedImage bufferedImage,
                     BlockPos startPos,
                     BlockPos playerOnPos,
                     int rows,
                     ClientPlayerEntity player,
                     Map<Color, Block> colorBlockMap,
                     boolean replaceBlock,
                     boolean horizontal) {
        this.bufferedImage = bufferedImage;
        this.startPos = startPos;
        this.playerOnPos = playerOnPos;
        this.rows = rows;
        this.player = player;
        this.colorBlockMap = colorBlockMap;
        this.replaceBlock = replaceBlock;
        this.horizontal = horizontal;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public void setBufferedImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
    }

    public BlockPos getStartPos() {
        return startPos;
    }

    public void setStartPos(BlockPos startPos) {
        this.startPos = startPos;
    }

    public BlockPos getPlayerOnPos() {
        return playerOnPos;
    }

    public void setPlayerOnPos(BlockPos playerOnPos) {
        this.playerOnPos = playerOnPos;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public ClientPlayerEntity getPlayer() {
        return player;
    }

    public void setPlayer(ClientPlayerEntity player) {
        this.player = player;
    }

    public Map<Color, Block> getColorBlockMap() {
        return colorBlockMap;
    }

    public void setColorBlockMap(Map<Color, Block> colorBlockMap) {
        this.colorBlockMap = colorBlockMap;
    }


    public boolean getReplaceBlock() {
        return replaceBlock;
    }

    public void setReplaceBlock(boolean replaceBlock) {
        this.replaceBlock = replaceBlock;
    }


    public boolean getHorizontal() {
        return horizontal;
    }

    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
    }
}