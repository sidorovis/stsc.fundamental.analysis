package stsc.fundamental.analysis;

import java.io.File;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This class parse and store all necessary settings for
 * {@link CorrelationCalculator} that should be provided by command line.
 *
 */
final class CorrelationCalculatorSettings {

	private final File datafeedFolder;

	public CorrelationCalculatorSettings(final String[] args) throws ParseException {
		final Options options = new Options();
		options.addOption("f", true, "datafeed folder");
		final CommandLineParser parser = new BasicParser();
		final CommandLine cmd = parser.parse(options, args);
		final String folder = cmd.getOptionValue("f");
		if (folder == null) {
			throw new IllegalArgumentException("please provide -f= parameter");
		}
		final File file = new File(folder);
		if (!file.exists() || !file.isDirectory()) {
			throw new IllegalArgumentException("datafeed folder should exists and be folder");
		}
		this.datafeedFolder = file;
	}

	public File getDatafeedFolder() {
		return datafeedFolder;
	}

}
