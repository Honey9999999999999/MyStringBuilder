package ru.mystringbuilder;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        MyStringBuilder stringBuilder = new MyStringBuilder();

        System.out.println(stringBuilder.append("Hello").append(" World").append(" Опа").append(" о.о") + "| count = " + stringBuilder.length());

        stringBuilder.undo().undo();

        System.out.println(stringBuilder);
    }
}
