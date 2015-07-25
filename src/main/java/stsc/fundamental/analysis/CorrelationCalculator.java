package stsc.fundamental.analysis;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Ordering;

import stsc.stocks.repo.MetaIndicesRepository;
import stsc.stocks.repo.MetaIndicesRepositoryIncodeImpl;

/**
 * This application calculate correlations between different indexes from
 * {@link MetaIndicesRepository}.
 */
final class CorrelationCalculator {

	private CorrelationCalculatorSettings settings;

	public CorrelationCalculator(final CorrelationCalculatorSettings settings, final MetaIndicesRepository metaIndicesRepository) {
		this.settings = settings;
		final ArrayList<String> allNames = new ArrayList<>();
		fillNames(metaIndicesRepository.getCountryMarketIndices(), allNames);
		fillNames(metaIndicesRepository.getGlobalMarketIndices(), allNames);
		fillNames(metaIndicesRepository.getRegionMarketIndices(), allNames);
		allNames.sort(Ordering.natural());
		startIndexByIndexCorrelation(allNames);
	}

	private void startIndexByIndexCorrelation(ArrayList<String> allNames) {
		for (int i = 0; i < allNames.size(); ++i) {
			for (int u = i + 1; u < allNames.size(); ++u) {
				final String left = allNames.get(i);
				final String right = allNames.get(u);
				if (!left.equals(right)) {
					final double cc = getCorrelationCoefficient(left, right);
				}
			}
		}

	}

	private double getCorrelationCoefficient(String left, String right) {
		System.out.println("The datafeed folder " + settings.getDatafeedFolder());
		return 0;
	}

	private <E extends Enum<?>> void fillNames(final List<E> listOfIndexes, List<String> namesToFill) {
		for (E e : listOfIndexes) {
			namesToFill.add(e.name());
		}
	}

	public static void main(String[] args) {
		try {
			final CorrelationCalculatorSettings settings = new CorrelationCalculatorSettings(args);
			new CorrelationCalculator(settings, new MetaIndicesRepositoryIncodeImpl());
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
