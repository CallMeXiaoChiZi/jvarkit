/*
 * The MIT License
 *
 * Copyright (c) 2009 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.lindenb.jvarkit.util.picard.cmdline;


/**
 * Print out the usage for one or more CommandLinePrograms in a form close to what is used in the Sourceforge Picard website.
 *
 * @author alecw@broadinstitute.org
 */
public class CreateHtmlDocForProgram {
    public static void main(final String[] args) throws Exception {
        for (final String clazz : args) {
            CommandLineProgram mainClass = (CommandLineProgram)Class.forName(clazz).newInstance();
            CommandLineParser clp = new CommandLineParser(mainClass);
            clp.htmlUsage(System.out, mainClass.getClass().getSimpleName(), false);
        }
    }
}
