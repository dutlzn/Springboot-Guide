package com.example.demo;

import lombok.Data;
/**
 * @author esbk
 */
@Data
public class FastDfsFile {
    /**
     * 文件名字
     */
    private String name;
    /**
     * 文件内容
     */
    private byte[] content;
    /**
     * 文件扩展名
     */
    private String ext;
    /**
     * 文件MD5摘要值
     */
    private String md5;
    /**
     * 文件创建作者
     */
    private String author;

    public FastDfsFile(String name, byte[] content, String ext, String height,
                       String width, String author) {
        super();
        this.name = name;
        this.content = content;
        this.ext = ext;
        this.author = author;
    }
    public FastDfsFile(String name, byte[] content, String ext) {
        super();
        this.name = name;
        this.content = content;
        this.ext = ext;
    }
}
