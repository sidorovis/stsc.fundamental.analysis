package stsc.fundamental.analysis;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import stsc.algorithms.fundamental.analysis.statistics.eod.MovingPearsonCorrelation;
import stsc.common.BadSignalException;
import stsc.common.FromToPeriod;
import stsc.common.algorithms.BadAlgorithmException;
import stsc.common.stocks.UnitedFormatStock;
import stsc.common.storage.SignalsStorage;
import stsc.common.storage.StockStorage;
import stsc.general.simulator.Simulator;
import stsc.general.simulator.SimulatorSettings;
import stsc.general.trading.TradeProcessorInit;
import stsc.signals.MapKeyPairToDoubleSignal;
import stsc.signals.commons.KeyPair;
import stsc.stocks.indexes.MarketIndex;
import stsc.stocks.repo.MetaIndicesRepository;
import stsc.stocks.repo.MetaIndicesRepositoryIncodeImpl;
import stsc.yahoo.YahooFileStockStorage;

import com.google.common.collect.Ordering;

/**
 * This application calculate correlations between different indexes from
 * {@link MetaIndicesRepository}.
 */
final class CorrelationCalculator {

	private final StockStorage stockStorage;
	private long id = 0;

	public CorrelationCalculator(final CorrelationCalculatorSettings settings, final MetaIndicesRepository metaIndicesRepository) throws IOException,
			ClassNotFoundException, InterruptedException, BadAlgorithmException, BadSignalException, ParseException {
		final ArrayList<String> allNames = new ArrayList<>();
		fillNames(metaIndicesRepository.getCountryMarketIndices(), allNames);
		fillNames(metaIndicesRepository.getGlobalMarketIndices(), allNames);
		fillNames(metaIndicesRepository.getRegionMarketIndices(), allNames);
		allNames.sort(Ordering.natural());
		final String dataFolder = settings.getDatafeedFolder().getCanonicalPath() + "/" + YahooFileStockStorage.DATA_FOLDER;
		final String filteredDataFolder = settings.getDatafeedFolder().getCanonicalPath() + "/" + YahooFileStockStorage.FILTER_DATA_FOLDER;
		this.stockStorage = new YahooFileStockStorage(dataFolder, filteredDataFolder).waitForLoad();
		System.out.println(allNames.size());
		// calculate(stockStorage.getStockNames());
	}

	private void calculate(Collection<String> allNames) throws BadAlgorithmException, BadSignalException, ParseException {
		final Map<KeyPair, Double> cc = getCorrelationCoefficient(allNames);
		for (Entry<KeyPair, Double> e : cc.entrySet()) {
			System.out.println(e);
		}
	}

	private Map<KeyPair, Double> getCorrelationCoefficient(Collection<String> allNames) throws BadAlgorithmException, ParseException, BadSignalException {
		final String executionName = "correlation";
		final TradeProcessorInit tradeProcessorInit = new TradeProcessorInit(stockStorage, new FromToPeriod("01-01-1900", "01-01-2100"), //
				"EodExecutions = " + executionName + "\n" + //
						executionName + ".loadLine = ." + MovingPearsonCorrelation.class.getSimpleName() + "(size=10000i, N=52i)\n");
		final SimulatorSettings simulatorSettings = new SimulatorSettings(id++, tradeProcessorInit);
		final Set<String> stockNames = new HashSet<>(allNames);
		final Simulator simulator = new Simulator(simulatorSettings, stockNames);
		final SignalsStorage signalsStorage = simulator.getSignalsStorage();
		final int size = signalsStorage.getIndexSize(executionName);
		final HashMap<KeyPair, Double> result = new HashMap<KeyPair, Double>();
		collectData(executionName, signalsStorage, size, result);
		return result;
	}

	private void collectData(final String executionName, final SignalsStorage signalsStorage, final int size, final HashMap<KeyPair, Double> result) {
		if (size > 0) {
			for (int i = size - 1; i >= 0; --i) {
				final Map<KeyPair, Double> v = signalsStorage.getEodSignal(executionName, i).getContent(MapKeyPairToDoubleSignal.class).getValues();
				for (Entry<KeyPair, Double> e : v.entrySet()) {
					if (!result.containsKey(e.getKey())) {
						result.put(e.getKey(), e.getValue());
					}
				}
			}
		}
	}

	private <E extends MarketIndex<E>> void fillNames(final List<E> listOfIndexes, List<String> namesToFill) {
		for (final E e : listOfIndexes) {
			namesToFill.add(UnitedFormatStock.fromFilesystem(e.getFilesystemName()));
		}
	}

	public static void main(final String[] args) {
		try {
			final CorrelationCalculatorSettings settings = new CorrelationCalculatorSettings(args);
			new CorrelationCalculator(settings, new MetaIndicesRepositoryIncodeImpl());
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
