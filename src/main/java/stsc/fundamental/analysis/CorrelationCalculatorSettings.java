package stsc.fundamental.analysis;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

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

	private final Path datafeedFolder;

	public CorrelationCalculatorSettings(final String[] args) throws ParseException {
		final Options options = new Options();
		options.addOption("f", true, "datafeed folder");
		final CommandLineParser parser = new BasicParser();
		final CommandLine cmd = parser.parse(options, args);
		final String folder = cmd.getOptionValue("f");
		if (folder == null) {
			throw new IllegalArgumentException("please provide -f= parameter");
		}
		this.datafeedFolder = FileSystems.getDefault().getPath(folder);
		final File file = datafeedFolder.toFile();
		if (!file.exists() || !file.isDirectory()) {
			throw new IllegalArgumentException("datafeed folder should exists and be folder");
		}
	}

	public Path getDatafeedFolder() {
		return datafeedFolder;
	}

}
