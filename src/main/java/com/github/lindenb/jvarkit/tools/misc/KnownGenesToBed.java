/*
The MIT License (MIT)

Copyright (c) 2015 Pierre Lindenbaum

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


*/
package com.github.lindenb.jvarkit.tools.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import htsjdk.samtools.util.CloserUtil;


import com.github.lindenb.jvarkit.io.IOUtils;
import com.github.lindenb.jvarkit.util.command.Command;
import com.github.lindenb.jvarkit.util.ucsc.KnownGene;

public class KnownGenesToBed extends AbstractKnownGenesToBed
	{
	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(KnownGenesToBed.class);

	 @Override
	public  Command createCommand() {
			return new MyCommand();
		}
		 
	public  class MyCommand extends AbstractKnownGenesToBed.AbstractKnownGenesToBedCommand
	 	{
		private PrintStream out =null;
	

	
	private void print(KnownGene kg,int start,int end,String type,String name)
		{
		if(start>=end) return;
		out.print(kg.getChromosome());
		out.print('\t');
		out.print(start);
		out.print('\t');
		out.print(end);
		out.print('\t');
		out.print(kg.isPositiveStrand()?'+':'-');
		out.print('\t');
		out.print(kg.getName());
		out.print('\t');
		out.print(type);
		out.print('\t');
		out.print(name);
		out.println();
		}
	
	private void scan(BufferedReader r) throws IOException
		{
		String line=null;
		Pattern tab=Pattern.compile("[\t]");
		while((line=r.readLine())!=null)
			{
			if(out.checkError()) break;
			String tokens[]=tab.split(line);
			KnownGene kg=new KnownGene(tokens);
			if(!hide_transcripts) print(kg,kg.getTxStart(),kg.getTxEnd(),"TRANSCRIPT",kg.getName());
			for(int i=0;i< kg.getExonCount();++i)
				{
				KnownGene.Exon exon=kg.getExon(i);
				if(!hide_exons) print(kg,exon.getStart(),exon.getEnd(),"EXON",exon.getName());
				
				if(!hide_utrs && kg.getCdsStart()>exon.getStart())
					{
					print(kg,exon.getStart(),
							Math.min(kg.getCdsStart(),exon.getEnd()),"UTR","UTR"+(kg.isPositiveStrand()?"5":"3"));
					}
				
				if(!hide_cds && !(kg.getCdsStart()>=exon.getEnd() || kg.getCdsEnd()<exon.getStart()))
					{
					print(kg,
							Math.max(kg.getCdsStart(),exon.getStart()),
							Math.min(kg.getCdsEnd(),exon.getEnd()),
							"CDS",exon.getName()
							);
					}
				
				KnownGene.Intron intron=exon.getNextIntron();
				if(!hide_exons && intron!=null)
					{
					print(kg,intron.getStart(),intron.getEnd(),"INTRON",intron.getName());
					}
				
				if(!hide_utrs && kg.getCdsEnd()<exon.getEnd())
					{
					print(kg,Math.max(kg.getCdsEnd(),exon.getStart()),
							exon.getEnd(),
							"UTR","UTR"+(kg.isPositiveStrand()?"3":"5"));
					}
				
				}
			}
		}
	
	@Override
	public Collection<Throwable> call() throws Exception
		{
		final List<String> args = getInputFiles();
			
			
			BufferedReader r=null;
			try
				{
				this.out = openFileOrStdoutAsPrintStream();
				if(args.isEmpty())
					{
					r= new BufferedReader(new InputStreamReader(stdin()));
					scan(r);
					CloserUtil.close(r);
					}
				else
					{
					for(String filename:args)
						{
						LOG.info("Reading from "+filename);
						r=IOUtils.openURIForBufferedReading(filename);
						scan(r);
						CloserUtil.close(r);
						}
					}
				return RETURN_OK;
				}
			catch(Exception err)
				{
				return wrapException(err);
				}
			finally
				{
				CloserUtil.close(out);
				out = null;
				}
			}
	 	}
	/**
	 * @param args
	 */
	public static void main(String[] args)
		{
		new KnownGenesToBed().instanceMainWithExit(args);
		}
	}
