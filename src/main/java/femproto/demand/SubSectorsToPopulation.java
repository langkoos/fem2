package femproto.demand;

import femproto.gis.Globals;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public class SubSectorsToPopulation {
    Scenario scenario;

    public SubSectorsToPopulation() {
        this.scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
    }

    public static void main(String[] args) throws IOException {
        SubSectorsToPopulation subSectorsToPopulation = new SubSectorsToPopulation();
        subSectorsToPopulation.readSubSectorsShapeFile(args[0]);
        subSectorsToPopulation.writePopulation(args[1]);

    }

    public void readSubSectorsShapeFile(String fileName) throws IOException {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName);
        String wkt = IOUtils.getBufferedReader(fileName.replaceAll("shp$", "prj")).readLine().toString();
        CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(wkt, Globals.EPSG28356);
        PopulationFactory factory = scenario.getPopulation().getFactory();
        //iterate through features and generate pax by subsector
        Iterator<SimpleFeature> iterator = features.iterator();
        long id=0L;
        while (iterator.hasNext()){
            SimpleFeature feature = iterator.next();
            String subsector = feature.getAttribute("Subsector").toString();
            int totalVehicles = (int)(double)feature.getAttribute("Totalvehic");
            for (int i = 0; i < totalVehicles; i++) {
                Person person = factory.createPerson(Id.createPersonId(id++));
                person.getAttributes().putAttribute("subsector",subsector);
                scenario.getPopulation().addPerson(person);
            }
        }
    }

    private void writePopulation(String filename) {
        PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation());
        populationWriter.write(filename);
    }

}
