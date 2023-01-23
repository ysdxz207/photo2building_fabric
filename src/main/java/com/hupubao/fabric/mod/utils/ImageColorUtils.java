package com.hupubao.fabric.mod.utils;

import cn.hutool.core.img.ColorUtil;
import cn.hutool.core.img.ImgUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class ImageColorUtils {


    /**
     * 获取图片的均值颜色
     * @param file
     * @return
     */
    public static Color getAvgRGB(File file){
        BufferedImage bi = ImgUtil.toBufferedImage(ImgUtil.scale(ImgUtil.read(file),0.3f));
        return getAvgRGB(bi);
    }
    public static Color getAvgRGB(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        float[] dots = new float[]{0.15f, 0.35f, 0.5f, 0.7f, 0.85f};
        int R = 0;
        int G = 0;
        int B = 0;
        for(float dw : dots){
            for(float dh : dots){
                int rgbVal = image.getRGB((int)(w*dw), (int)(h*dh));
                Color color = ImgUtil.getColor(rgbVal);
                R += color.getRed();
                G += color.getGreen();
                B += color.getBlue();
            }
        }
        int cn = dots.length * dots.length;
        return  new Color(R/cn, G/cn, B/cn);
    }

    public static void main(String[] args) throws IOException {
        File file = new File("C:\\Users\\hupubao\\Desktop\\01.png");
        BufferedImage bufferedImage = ImageIO.read(file);
        System.out.println(ColorUtil.toHex(getAvgRGB(file)));

//        ImgUtil.slice(FileUtil.file("C:\\Users\\hupubao\\Desktop\\test.jpg"), FileUtil.file("d:/dest/"), 27, 27);
    }

}