package com.hupubao.fabric.mod.bean;

import net.minecraft.util.math.BlockPos;

import java.awt.image.BufferedImage;
import java.io.File;

public class BlockBuildTask {

    private BufferedImage imagePiece;

    private BlockPos blockPos;

    /**
     * 总计方块数
     */
    private int blockNums;

    /**
     * 当前方块数
     */
    private int currentNums;

    public BlockBuildTask(BufferedImage imagePiece, BlockPos blockPos, int blockNums, int currentNums) {
        this.imagePiece = imagePiece;
        this.blockPos = blockPos;
        this.blockNums = blockNums;
        this.currentNums = currentNums;
    }

    public BufferedImage getImagePiece() {
        return imagePiece;
    }

    public void setImagePiece(BufferedImage imagePiece) {
        this.imagePiece = imagePiece;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public int getBlockNums() {
        return blockNums;
    }

    public void setBlockNums(int blockNums) {
        this.blockNums = blockNums;
    }

    public int getCurrentNums() {
        return currentNums;
    }

    public void setCurrentNums(int currentNums) {
        this.currentNums = currentNums;
    }
}
