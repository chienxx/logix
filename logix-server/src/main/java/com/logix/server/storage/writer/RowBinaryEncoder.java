package com.logix.server.storage.writer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * RowBinary 格式编码器
 * 封装 ClickHouse RowBinary 格式的编码方法
 *
 * @author Kanade
 * @since 2025/11/23
 */
public final class RowBinaryEncoder {

    private final DataOutputStream out;

    public RowBinaryEncoder(DataOutputStream out) {
        this.out = out;
    }

    public void writeUInt64(Long value) throws IOException {
        long v = value != null ? value : 0L;
        out.writeLong(Long.reverseBytes(v));
    }

    public void writeUInt16(Integer value) throws IOException {
        int v = value != null ? value : 0;
        out.writeShort(Short.reverseBytes((short) v));
    }

    public void writeEnum8(int value) throws IOException {
        out.writeByte(value & 0xFF);
    }

    public void writeDateTime64Millis(Long epochMillis) throws IOException {
        long v = epochMillis != null ? epochMillis : System.currentTimeMillis();
        out.writeLong(Long.reverseBytes(v));
    }

    public void writeString(String value) throws IOException {
        String s = value != null ? value : "";
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        out.write(bytes);
    }

    private void writeVarInt(int value) throws IOException {
        int remaining = value;
        while ((remaining & 0xFFFFFF80) != 0) {
            out.writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        out.writeByte(remaining & 0x7F);
    }
}
