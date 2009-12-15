package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.util.Collection;
import java.util.HashSet;

/**
 * For now this supports sweeping over C and different kernels (which may have e.g. different gamma).
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class ImmutableSvmParameterGrid<L extends Comparable, P> extends ImmutableSvmParameter<L, P>
	{
	private final Collection<ImmutableSvmParameterPoint<L, P>> gridParams;

	public ImmutableSvmParameterGrid(Builder<L, P> copyFrom)
		{
		super(copyFrom);
		gridParams = copyFrom.gridParams;
		}

	public Collection<ImmutableSvmParameterPoint<L, P>> getGridParams()
		{
		return gridParams;
		}

	public static <L extends Comparable, P> Builder<L, P> builder()
		{
		return new Builder<L, P>();
		}

	public static class Builder<L extends Comparable, P> extends ImmutableSvmParameter.Builder
		{
		public Collection<Float> Cset;
		public Collection<KernelFunction<P>> kernelSet;
		private Collection<ImmutableSvmParameterPoint<L, P>> gridParams;

		public Builder(ImmutableSvmParameter.Builder copyFrom)
			{
			super(copyFrom);

			//default
			Cset = new HashSet<Float>();
			Cset.add(1f);
			}

		public Builder(ImmutableSvmParameterGrid<L, P> copyFrom)
			{
			super(copyFrom);

			//default
			//Cset = new HashSet<Float>();
			//Cset.add(1f);

			//Cset = copyFrom.Cset;
			//kernelSet = copyFrom.kernelSet;
			gridParams = copyFrom.gridParams;
			}

		public Builder()
			{
			super();

			//default
			Cset = new HashSet<Float>();
			Cset.add(1f);
			}

		public ImmutableSvmParameter<L, P> build()
			{
			ImmutableSvmParameterPoint.Builder<L, P> builder = ImmutableSvmParameterPoint.asBuilder(this);

			if (Cset == null || Cset.isEmpty())
				{
				throw new SvmException("Can't build a grid with no C values");
				}

			if (kernelSet == null || kernelSet.isEmpty())
				{
				throw new SvmException("Can't build a grid with no kernels");
				}

			if (Cset.size() == 1 && kernelSet.size() == 1)
				{
				builder.C = Cset.iterator().next();
				builder.kernel = kernelSet.iterator().next();
				return builder.build();
				//	return new ImmutableSvmParameterPoint<L,P>(this);
				}
			gridParams = new HashSet<ImmutableSvmParameterPoint<L, P>>();

			// the C and kernel set here are ignored; we just overwrite them with the grid points

			for (Float gridC : Cset)
				{
				for (KernelFunction<P> gridKernel : kernelSet)
					{
					builder.C = gridC;
					builder.kernel = gridKernel;
					builder.gridsearchBinaryMachinesIndependently = false;

					// this copies all the params so we can safely continue modifying the builder
					gridParams.add(builder.build());
					}
				}

			return new ImmutableSvmParameterGrid<L, P>(this);
			}
		}

	public Builder<L, P> asBuilder()
		{
		return new Builder<L, P>(this);
		}
	}
