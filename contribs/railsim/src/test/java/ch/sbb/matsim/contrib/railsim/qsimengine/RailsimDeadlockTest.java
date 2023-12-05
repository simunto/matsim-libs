package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.DeadlockAvoidance;
import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.NoDeadlockAvoidance;
import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.SimpleDeadlockAvoidance;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.SimpleDisposition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.router.TrainRouter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.util.function.Consumer;

public class RailsimDeadlockTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private EventsManager eventsManager;
	private RailsimTestUtils.EventCollector collector;

	@Before
	public void setUp() {
		eventsManager = EventsUtils.createEventsManager();
		collector = new RailsimTestUtils.EventCollector();

		eventsManager.addHandler(collector);
		eventsManager.initProcessing();
	}

	private RailsimTestUtils.Holder getTestEngine(String network, DeadlockAvoidance dla, @Nullable Consumer<Link> f) {
		Network net = NetworkUtils.readNetwork(new File(utils.getPackageInputDirectory(), network).toString());
		RailsimConfigGroup config = new RailsimConfigGroup();

		collector.clear();

		if (f != null) {
			for (Link link : net.getLinks().values()) {
				f.accept(link);
			}
		}

		RailResourceManager res = new RailResourceManager(eventsManager, config, net, dla);
		TrainRouter router = new TrainRouter(net, res);

		return new RailsimTestUtils.Holder(new RailsimEngine(eventsManager, config, res, new SimpleDisposition(res, router)), net);
	}

	@Test
	public void deadlock() {

		RailsimTestUtils.Holder test = getTestEngine("networkDeadlocksFixedBlocks.xml", new NoDeadlockAvoidance(), null);
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "AB", "EF");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2", 0, "HG", "DC");

		test.doSimStepUntil(150);
		test.debugFiles(collector, "deadLock");

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio", 140, "y1y", 0)
			.hasTrainState("regio2", 140, "zy", 0);

	}
}
