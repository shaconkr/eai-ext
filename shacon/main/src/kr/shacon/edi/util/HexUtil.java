package kr.shacon.edi.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.StringUtil;

import java.io.UnsupportedEncodingException;

public class HexUtil    {

        public static String dump(String s, String charset) throws UnsupportedEncodingException {
            ByteBuf buf = Unpooled.buffer(0,66535);
            buf.writeBytes(s.getBytes(charset));
//            System.out.println(s);
            System.out.println(StringUtil.toHexStringPadded(s.getBytes(charset)));
            return ByteBufUtil.prettyHexDump(buf);
        }


}
