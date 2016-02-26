package stsc.fundamental.analysis;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import stsc.algorithms.fundamental.analysis.statistics.eod.LeftToRightMovingPearsonCorrelation;
import stsc.common.BadSignalException;
import stsc.common.FromToPeriod;
import stsc.common.algorithms.BadAlgorithmException;
import stsc.common.signals.SerieSignal;
import stsc.common.signals.SignalContainer;
import stsc.common.storage.SignalsStorage;
import stsc.common.storage.StockStorage;
import stsc.general.simulator.Simulator;
import stsc.general.simulator.SimulatorImpl;
import stsc.general.simulator.Execution;
import stsc.general.simulator.ExecutionImpl;
import stsc.general.trading.TradeProcessorInit;
import stsc.signals.MapKeyPairToDoubleSignal;
import stsc.signals.commons.KeyPair;
import stsc.stocks.indexes.CountryMarketIndex;
import stsc.stocks.indexes.GlobalMarketIndex;
import stsc.stocks.indexes.MarketIndex;
import stsc.stocks.indexes.RegionMarketIndex;
import stsc.stocks.repo.MetaIndicesRepository;
import stsc.stocks.repo.MetaIndicesRepositoryIncodeImpl;
import stsc.yahoo.YahooDatafeedSettings;
import stsc.yahoo.YahooFileStockStorage;

/**
 * This application calculate correlations between different indexes from {@link MetaIndicesRepository}.
 */
public final class CorrelationCalculator {

	public static final String DATA_FOLDER = "./data/";
	public static final String FILTER_DATA_FOLDER = "./filtered_data/";

	private final MetaIndicesRepository metaIndicesRepository;
	private final StockStorage stockStorage;
	private int id = 0;

	public CorrelationCalculator(final CorrelationCalculatorSettings settings, final MetaIndicesRepository metaIndicesRepository)
			throws IOException, InterruptedException, BadAlgorithmException, BadSignalException, ParseException {
		this.metaIndicesRepository = metaIndicesRepository;
		final Path dataFolder = settings.getDatafeedFolder().resolve(DATA_FOLDER);
		final Path filteredDataFolder = settings.getDatafeedFolder().resolve(FILTER_DATA_FOLDER);
		this.stockStorage = new YahooFileStockStorage(new YahooDatafeedSettings(dataFolder, filteredDataFolder), true).waitForBackgroundProcess();
		calculate();
	}

	private void calculate() throws BadAlgorithmException, BadSignalException, ParseException {
		final Map<KeyPair, Double> cc = getCorrelationCoefficient();
		for (Entry<KeyPair, Double> e : cc.entrySet()) {
			System.out.print(e + " ||| ");
			findType(e.getKey().getLeft());
			System.out.print("- ");
			findType(e.getKey().getRight());
			System.out.println();
		}
	}

	private void findType(final String instrumentName) {
		final int leftCountryIndex = Collections.binarySearch(metaIndicesRepository.getCountryMarketIndices(), CountryMarketIndex.createForSearch(instrumentName));
		if (leftCountryIndex >= 0) {
			final CountryMarketIndex index = metaIndicesRepository.getCountryMarketIndices().get(leftCountryIndex);
			System.out.print(index.getFilesystemName() + " (" + index.getCountry().name() + ") ");
			return;
		}
		final int leftRegionIndex = Collections.binarySearch(metaIndicesRepository.getRegionMarketIndices(), RegionMarketIndex.createForSearch(instrumentName));
		if (leftRegionIndex >= 0) {
			final RegionMarketIndex index = metaIndicesRepository.getRegionMarketIndices().get(leftRegionIndex);
			System.out.print(index.getWorldSector().name() + " ");
			return;
		}
		final int leftGlobalIndex = Collections.binarySearch(metaIndicesRepository.getGlobalMarketIndices(), GlobalMarketIndex.createForSearch(instrumentName));
		if (leftGlobalIndex >= 0) {
			System.out.print("GL ");
			return;
		}
	}

	private <T extends MarketIndex<T>> String joinForParameter(Collection<T> col) {
		String r = "";
		for (MarketIndex<T> s : col) {
			r += s.getInstrumentName() + "|";
		}
		return r;
	}

	private Map<KeyPair, Double> getCorrelationCoefficient() throws BadAlgorithmException, ParseException, BadSignalException {
		final String executionName = "correlation";
		final String leftElements = "spy|^n225|^ftse|^ixic|msci|efa";
		final String rightElements = joinForParameter(metaIndicesRepository.getCountryMarketIndices());

		final TradeProcessorInit tradeProcessorInit = new TradeProcessorInit(stockStorage, new FromToPeriod("01-01-1900", "01-01-2100"), //
				"EodExecutions = " + executionName + "\n" + //
						executionName + ".loadLine = ." + LeftToRightMovingPearsonCorrelation.class.getSimpleName() + //
						"(size=10000i, N=104i, " + //
						"LE=" + leftElements + ", " + //
						"RE=" + rightElements + ")\n");
		final Execution simulatorSettings = new ExecutionImpl(id++, tradeProcessorInit);
		final Simulator simulator = new SimulatorImpl();
		simulator.simulateMarketTrading(simulatorSettings);
		final SignalsStorage signalsStorage = simulator.getSignalsStorage();
		final int size = signalsStorage.getIndexSize(executionName);
		final Map<KeyPair, Double> result = new TreeMap<KeyPair, Double>();
		collectData(executionName, signalsStorage, size, result);
		return result;
	}

	private void collectData(final String executionName, final SignalsStorage signalsStorage, final int size, final Map<KeyPair, Double> result) {
		if (size > 0) {
			for (int i = size - 1; i >= 0; --i) {
				final SignalContainer<? extends SerieSignal> sc = signalsStorage.getEodSignal(executionName, i);
				if (!sc.isPresent()) {
					continue;
				}
				final Map<KeyPair, Double> v = sc.getContent(MapKeyPairToDoubleSignal.class).getValues();
				for (Entry<KeyPair, Double> e : v.entrySet()) {
					if (!result.containsKey(e.getKey())) {
						result.put(e.getKey(), e.getValue());
					}
				}
			}
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
