package ru.mystringbuilder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Stack;

public class MyStringBuilder {

    private static class StringBuilderSnapShot{
        private final byte[] sequence;
        private final int count;
        private final byte coder;

        private StringBuilderSnapShot(byte[] sequence, int count, byte coder){

            int usefulLength = count << coder;
            this.sequence = new byte[usefulLength];
            System.arraycopy(sequence, 0, this.sequence, 0, usefulLength);

            this.count = count;
            this.coder = coder;
        }

        public byte[] getSequence(){
            return Arrays.copyOf(sequence, sequence.length);
        }

        public int getCount(){
            return count;
        }

        public byte getCoder(){
            return coder;
        }
    }

    public static final int LATIN1 = 0;
    public static final int UTF16 = 1;

    private byte[] sequence;
    private int count;
    private byte coder;

    private final Stack<StringBuilderSnapShot> memoryStack;

    public MyStringBuilder(){
        this(16);
    }
    public MyStringBuilder(int capacity){
        sequence = new byte[capacity];
        coder = 1;
        memoryStack = new Stack<>();
    }
    public MyStringBuilder(String string){
        int length = string.length();
        int capacity = (length < Integer.MAX_VALUE - 16)
                ? length + 16 : Integer.MAX_VALUE;

        sequence = new byte[capacity];
        coder = getInternalCoder(string);
        memoryStack = new Stack<>();

        append(string);
    }

    private static byte getInternalCoder(String s) {

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 255) {
                return UTF16;
            }
        }
        return LATIN1;
    }

    public MyStringBuilder append(String string){
        save();

        int length = string.length();

        ensureCapacityInternal(count + length);
        putStringAt(string);

        return this;
    }

    private void ensureCapacityInternal(int minCapacity){
        int oldCapacity = sequence.length >> coder;
        if (minCapacity - oldCapacity > 0) {
            sequence = Arrays.copyOf(sequence,
                    newCapacity(minCapacity));
        }
    }

    private int newCapacity(int minCapacity) {

        int oldLength = sequence.length;
        int newLength = (oldLength << 1) + 2;

        if (newLength - minCapacity < 0) {
            newLength = minCapacity;
        }

        int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

        if (newLength <= 0 || newLength > MAX_ARRAY_SIZE) {
            if (minCapacity < 0) {
                throw new OutOfMemoryError();
            }
            return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
        }

        return newLength;
    }

    private void putStringAt(String string) {
        byte strCoder = getInternalCoder(string);
        byte[] strBytes = string.getBytes(strCoder == LATIN1 ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_16BE);

        if (coder != strCoder) {
            inflate();
        }

        if (this.coder == strCoder) {
            System.arraycopy(strBytes, 0, this.sequence, this.count << coder, strBytes.length);
        } else {
            for (int i = 0; i < strBytes.length; i++) {
                int bytePos = (this.count + i) << 1;

//                if(this.sequence[bytePos + 1] != 0){
//                    throw new RuntimeException("Cell is not empty!");
//                }

                this.sequence[bytePos] = 0;
                this.sequence[bytePos + 1] = strBytes[i];
            }
        }

        this.count += string.length();
    }

    private void inflate() {
        if (coder == UTF16) {
            return;
        }
        byte[] buf = new byte[sequence.length * 2];

        for (int i = 0; i < sequence.length; i++) {
            buf[i * 2] = 0;
            buf[i * 2 + 1] = sequence[i];
        }

        this.sequence = buf;
        this.coder = UTF16;
    }

    private void save(){
        memoryStack.push(new StringBuilderSnapShot(sequence, count, coder));
    }
    public MyStringBuilder undo(){
        if(memoryStack.isEmpty()) return this;

        StringBuilderSnapShot snapShot = memoryStack.pop();

        sequence = snapShot.getSequence();
        count = snapShot.getCount();
        coder = snapShot.getCoder();

        return this;
    }

    public int length(){
        return count;
    }

    @Override
    public String toString() {
        int lengthInBytes = this.count << coder;
        return new String(this.sequence, 0, lengthInBytes, coder == LATIN1 ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_16);
    }
}
