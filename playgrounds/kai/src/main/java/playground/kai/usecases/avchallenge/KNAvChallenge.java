/**
 * 
 */
package playground.kai.usecases.avchallenge;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiQSimProvider;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
class KNAvChallenge {
	private static final String MODE = "taxi";
	private static final boolean otfvis = true ;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig() ;

		// ---

		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
        final FleetImpl fleet = new FleetImpl();
        new VehicleReader(scenario.getNetwork(), fleet).readFile("filename");
        
        // ---

		Controler controler = new Controler( scenario ) ;
		
		controler.addOverridingModule(new TaxiModule( fleet ) ) ;
		
		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				addRoutingModuleBinding(MODE).toInstance(new DynRoutingModule(MODE));
				bind(VrpData.class).toInstance(vrpData);
			}
		});

		controler.addOverridingModule(new DynQSimModule<>(OneTaxiQSimProvider.class));

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		// ---

		controler.run();
	}

}
