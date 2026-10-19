
package macro;
import star.common.*;
import star.base.neo.*;
import star.cadmodeler.*;
import star.vis.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;


public class import_geom extends StarMacro {

    public void execute() {
        Path configPath = Paths.get(resolvePath("case.json")).toAbsolutePath().normalize();
        String stepFile;

        try (InputStream input = Files.newInputStream(configPath);
                JsonReader reader = Json.createReader(input)) {
            JsonObject json = reader.readObject();
            stepFile = configPath.getParent().resolve(json.getString("stepFile")).normalize().toString();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Could not read case.json: " + exception.getMessage(), exception);
        }

        Simulation simulation = getActiveSimulation();

        CadModel cadModel = simulation.get(SolidModelManager.class).createSolidModel();
        cadModel.setPresentationName("IFR_CAD");
        cadModel.resetSystemOptions();

        ImportCadFileFeature importFeature = cadModel.importCadFile(
            stepFile,
            true, false, false, false, false, false, false, true, false, true,
            NeoProperty.fromString("{ 'NX': 0, 'STEP': 0, 'SE': 0, 'CGR': 0, 'SW': 0, 'IFC': 0, 'ACIS': 0, 'JT': 0, 'IGES': 0, 'CATIAV5': 0, 'CATIAV4': 0, 'CREO': 0, 'INV': 0 }"),
            false);

        // Car-only STEP contains multiple watertight surface bodies, keep all of them with their STEP names.
        // The fluid domain (created in create_domain.java) takes partName.
        Collection<star.cadmodeler.Body> bodies = cadModel.getBodyManager().getBodies();
        if (bodies.isEmpty()) {
            throw new IllegalStateException("No bodies found in STEP file: " + stepFile);
        }
        NeoObjectVector bodyVector = new NeoObjectVector(bodies.toArray());
        simulation.println("(II) Imported " + bodies.size() + " bodies from " + stepFile);

        CadRepairFeature repairFeature = cadModel.getFeatureManager().createCadRepairFeature(
            bodyVector,
            new NeoObjectVector(new Object[] {}),
            new NeoObjectVector(new Object[] {}));
        repairFeature.setAutoPreview(true);
        cadModel.allowMakingPartDirty(false);
        cadModel.getFeatureManager().startCadRepairEdit(repairFeature, true);
        repairFeature.setIsBodyGroupCreation(false);
        cadModel.getFeatureManager().markDependentNotUptodate(repairFeature);
        cadModel.allowMakingPartDirty(true);
        repairFeature.markFeatureForEdit();
        cadModel.getFeatureManager().stopCadRepairEdit(repairFeature, false);
        cadModel.getFeatureManager().execute(repairFeature);
        simulation.get(SolidModelManager.class).endEditCadModel(cadModel);
        cadModel.createParts(bodyVector, new NeoObjectVector(new Object[] {}),
            true, false, 1, false, false, 3, "SharpEdges", 30.0, 2, true, 1.0E-5, false);
    }
}
