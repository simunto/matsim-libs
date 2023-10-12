package org.matsim.contrib.commercialTrafficApplications.jointDemand;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.FreightConfigGroup;
import org.matsim.freight.carriers.carrier.CarrierUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


public class CommercialTrafficIntegrationTest {

    @Test
    public void runCommercialTrafficIT() {
        Config config = ConfigUtils.loadConfig("./scenarios/grid/jointDemand_config.xml");
        config.controller().setLastIteration(5);
        ConfigUtils.addOrGetModule(config, JointDemandConfigGroup.class);
        FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
        freightConfigGroup.setCarriersFile("jointDemand_carriers_car.xml");
        freightConfigGroup.setCarriersVehicleTypesFile("jointDemand_vehicleTypes.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        CarrierUtils.loadCarriersAccordingToFreightConfig(scenario);
        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new JointDemandModule());
        controler.run();
    }


}
