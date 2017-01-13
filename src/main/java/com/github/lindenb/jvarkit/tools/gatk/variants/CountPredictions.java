package com.github.lindenb.jvarkit.tools.gatk.variants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.broadinstitute.gatk.engine.CommandLineGATK;
import org.broadinstitute.gatk.utils.commandline.Argument;
import org.broadinstitute.gatk.utils.contexts.AlignmentContext;
import org.broadinstitute.gatk.utils.contexts.ReferenceContext;
import org.broadinstitute.gatk.utils.help.DocumentedGATKFeature;
import org.broadinstitute.gatk.utils.help.HelpConstants;
import org.broadinstitute.gatk.utils.refdata.RefMetaDataTracker;
import org.broadinstitute.gatk.utils.report.GATKReportTable;

import com.github.lindenb.jvarkit.util.vcf.predictions.AnnPredictionParser;

import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextUtils;

/**
 * Test Documentation
 *
 * 
 * 
 * 
 */
@DocumentedGATKFeature(
		summary="Count Predictions",
		groupName = HelpConstants.DOCS_CAT_VARMANIP,
		extraDocs = {CommandLineGATK.class} )
public class CountPredictions 
	extends AbstractVariantCounter
	{    
    @Argument(fullName="minQuality",shortName="mq",required=false,doc="Group by Quality. Set the treshold for Minimum Quality")
    public double minQuality = -1;
    @Argument(fullName="chrom",shortName="chrom",required=false,doc="Group by Chromosome/Contig")
    public boolean bychrom = false;
    @Argument(fullName="ID",shortName="ID",required=false,doc="Group by ID")
    public boolean byID = false;
    @Argument(fullName="variantType",shortName="variantType",required=false,doc="Group by VariantType")
    public boolean byType = false;
    @Argument(fullName="filter",shortName="filter",required=false,doc="Group by FILTER")
    public boolean byFilter = false;
    @Argument(fullName="impact",shortName="impact",required=false,doc="Group by ANN/IMPACT")
    public boolean byImpact = false;
    @Argument(fullName="biotype",shortName="biotype",required=false,doc="Group by ANN/biotype")
    public boolean bybiotype = false;
    @Argument(fullName="nalts",shortName="nalts",required=false,doc="Group by number of ALTS")
    public boolean bynalts = false;
    @Argument(fullName="affected",shortName="affected",required=false,doc="Group by number of Samples called and not HOMREF")
    public boolean byAffected = false;
    @Argument(fullName="called",shortName="called",required=false,doc="Group by number of Samples called")
    public boolean byCalled = false;
    @Argument(fullName="maxSamples",shortName="maxSamples",required=false,doc="if the number of samples affected is greater than --maxSamples use the label \"GT_MAX_SAMPLES\"")
    public int maxSamples = Integer.MAX_VALUE;
    @Argument(fullName="tsv",shortName="tsv",required=false,doc="Group by Transition/Transversion")
    public boolean byTsv = false;
    
    
    
	@Override
	public Map<CountPredictions.Category,Long> map(
				final RefMetaDataTracker tracker,
				final ReferenceContext ref,
				final AlignmentContext context
				) {
		if ( tracker == null )return Collections.emptyMap();
		final Map<CountPredictions.Category,Long> count = new HashMap<>();
		for(final VariantContext ctx: tracker.getValues(this.variants,context.getLocation()))
			{
			final List<Object> labels=new ArrayList<>();
			if(bychrom) labels.add(ctx.getContig());
			if(byID) labels.add(ctx.hasID()?"Y":".");
			if(byType) labels.add(ctx.getType().name());
			if(byFilter) labels.add(ctx.isFiltered()?"F":".");
			if(minQuality>=0) {
				labels.add(ctx.hasLog10PError() && ctx.getPhredScaledQual()>=this.minQuality ?
						".":"LOWQUAL"
						);
				}
			
			if(byImpact || bybiotype)
				{
				String biotype=null;
				AnnPredictionParser.Impact impact=null;
				for(final AnnPredictionParser.AnnPrediction pred: super.annParser.getPredictions(ctx))
					{
					final AnnPredictionParser.Impact  currImpact = pred.getPutativeImpact();
					if(impact!=null && currImpact.compareTo(impact)<0) continue;
					impact=currImpact;
					biotype= pred.getTranscriptBioType();
					}
				if(byImpact) labels.add(impact==null?".":impact);
				if(bybiotype) labels.add(biotype==null?".":biotype);
				}
			if(bynalts)labels.add(String.valueOf(ctx.getAlternateAlleles().size()));
			if(byAffected || byCalled) 
				{
				int nc=0;
				int ng=0;
				for(int i=0;i< ctx.getNSamples();++i)
					{
					final Genotype g= ctx.getGenotype(i);
					if(!(g.isNoCall() || g.isHomRef()))
						{
						ng++;
						}
					if(g.isCalled())
						{
						nc++;
						}
					}
				if(byCalled) labels.add(nc< maxSamples?nc:"GE_"+maxSamples);
				if(byAffected) labels.add(ng< maxSamples?ng:"GE_"+maxSamples);
				}
			if(byTsv) {
				if(ctx.getType()==VariantContext.Type.SNP && ctx.getAlternateAlleles().size()==1) {
					boolean b=(VariantContextUtils.isTransition(ctx));
					labels.add(b?"Transition":"Transversion");
					}
				else
					{
					labels.add(".");
					}
				
				}	
			final Category cat=new Category(labels);
			Long n=count.get(cat);
			count.put(cat, n==null?1L:n+1);
				
			}
		return count;
	}
	@Override
	public Map<CountPredictions.Category,Long> treeReduce(Map<CountPredictions.Category,Long> value, Map<CountPredictions.Category,Long> sum) {
		return this.reduce(value,sum);
		}

	@Override
	public Map<CountPredictions.Category,Long> reduce(Map<CountPredictions.Category,Long> value, Map<CountPredictions.Category,Long> sum) {
		final Map<CountPredictions.Category,Long> newmap = new HashMap<>(sum);
		for(final Category cat:value.keySet()) {
			final Long sv = sum.get(cat);
			final Long vv = value.get(cat);
			newmap.put(cat, sv==null?vv:sv+vv);
		}
		return newmap;
	}

	@Override
	public Map<CountPredictions.Category,Long> reduceInit() {
		return Collections.emptyMap();
	}
   
	@Override
	protected GATKReportTable createGATKReportTable() {
		final GATKReportTable table=new GATKReportTable(
				"Variants", "Variants "+variants.getSource(),0);
		if(bychrom) table.addColumn("CONTIG");
		if(byID) table.addColumn("IN_DBSNP");
		if(byType) table.addColumn("TYPE");
		if(byFilter) table.addColumn("FILTER");
		if(minQuality>=0) table.addColumn("QUAL_GE_"+this.minQuality);
		if(byImpact) table.addColumn("IMPACT");
		if(bybiotype) table.addColumn("BIOTYPE");
		if(bynalts) table.addColumn("N_ALT_ALLELES");
		if(byCalled) table.addColumn("CALLED_SAMPLES");
		if(byAffected) table.addColumn("AFFECTED_SAMPLES");
		if(byTsv) table.addColumn("Ts/Tv");

		return table;
		}

	
	}