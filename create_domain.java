// Simcenter STAR-CCM+ macro: create_domain.java
// Written by Simcenter STAR-CCM+ 17.04.008

// This subroutine creates the box fluid domain and subtracts the car from it.
// Half car: -y side kept, y-max face (y=0) is symmetry plane L0.
// Full car: both side walls are slip walls L3.

package macro;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import star.common.*;
import star.base.neo.*;
import star.meshing.*;
import star.cadmodeler.*;

public class create_domain extends StarMacro {

    public void execute() {
        // Read in case.json
        JsonObject config = verify_json();
        String partName = config.getString("partName");
        JsonObject domain = config.getJsonObject("domain");
        if (domain == null) {
            throw new IllegalArgumentException("case.json is missing the 'domain' block");
        }

        String type = domain.getString("type");
        double[] minCorner = read_corner(domain, "minCorner");
        double[] maxCorner = read_corner(domain, "maxCorner");
        validate_domain(type, minCorner, maxCorner);

        Simulation simulation = getActiveSimulation();
        simulation.println("(II) Creating " + type + " car domain . . .");

        // Car parts are all CAD parts present before the domain is created
        List<GeometryPart> carParts = new ArrayList<>();
        for (GeometryPart part : simulation.get(SimulationPartManager.class).getParts()) {
            if (part instanceof SolidModelPart) {
                carParts.add(part);
            }
        }
        if (carParts.isEmpty()) {
            throw new IllegalStateException("No car parts found to subtract from domain");
        }

        SimpleBlockPart block = create_block(simulation, minCorner, maxCorner);
        name_block_faces(simulation, block, type);
        subtract_car(simulation, block, carParts, partName);

        simulation.println("(II) Domain created: " + partName);
    }

    private JsonObject verify_json() {
        Path configPath = Paths.get(resolvePath("case.json")).toAbsolutePath().normalize();
        JsonObject json;

        try (InputStream input = Files.newInputStream(configPath);
                JsonReader reader = Json.createReader(input)) {
            json = reader.readObject();
            } catch (IOException | RuntimeException exception) {
                throw new IllegalArgumentException("Could not read case.json: " + exception.getMessage(), exception);
            }

        return json;
    }

    private double[] read_corner(JsonObject domain, String key) {
        JsonArray corner = domain.getJsonArray(key);
        if (corner == null || corner.size() != 3) {
            throw new IllegalArgumentException("domain." + key + " must be an [x, y, z] array");
        }
        return new double[] {
            corner.getJsonNumber(0).doubleValue(),
            corner.getJsonNumber(1).doubleValue(),
            corner.getJsonNumber(2).doubleValue()};
    }

    private void validate_domain(String type, double[] minCorner, double[] maxCorner) {
        if (!type.equals("half") && !type.equals("full")) {
            throw new IllegalArgumentException("domain.type must be 'half' or 'full', got '" + type + "'");
        }
        for (int i = 0; i < 3; i++) {
            if (minCorner[i] >= maxCorner[i]) {
                throw new IllegalArgumentException("domain.minCorner must be less than maxCorner on every axis");
            }
        }
        if (type.equals("half") && maxCorner[1] != 0.0) {
            throw new IllegalArgumentException("Half car domain requires maxCorner y = 0 (symmetry plane)");
        }
        if (minCorner[2] != 0.0) {
            getActiveSimulation().println("(WW) domain.minCorner z is not 0, road will not be at ground level");
        }
    }

    private SimpleBlockPart create_block(Simulation simulation, double[] minCorner, double[] maxCorner) {
        MeshPartFactory meshPartFactory = simulation.get(MeshPartFactory.class);
        SimulationPartManager partManager = simulation.get(SimulationPartManager.class);
        LabCoordinateSystem labCoordinateSystem = simulation.getCoordinateSystemManager().getLabCoordinateSystem();
        Units lengthUnits = (Units) simulation.getUnitsManager().getObject("m");

        SimpleBlockPart block = meshPartFactory.createNewBlockPart(partManager);
        block.setDoNotRetessellate(true);
        block.setCoordinateSystem(labCoordinateSystem);
        block.getCorner1().setCoordinateSystem(labCoordinateSystem);
        block.getCorner1().setCoordinate(lengthUnits, lengthUnits, lengthUnits, new DoubleVector(minCorner));
        block.getCorner2().setCoordinateSystem(labCoordinateSystem);
        block.getCorner2().setCoordinate(lengthUnits, lengthUnits, lengthUnits, new DoubleVector(maxCorner));
        block.rebuildSimpleShapePart();
        block.setDoNotRetessellate(false);
        block.setPresentationName("Domain");

        return block;
    }

    private void name_block_faces(Simulation simulation, SimpleBlockPart block, String type) {
        // Same layer names as the CAD-defined domain so create_regions is unchanged
        PartSurface blockSurface = block.getPartSurfaces().iterator().next();
        Vector<Integer> patches = block.getPartSurfacePatches(blockSurface);
        simulation.println("(II) Domain block patches: " + patches);
        if (patches.size() != 6) {
            throw new IllegalStateException("Expected 6 patches on domain block, found " + patches.size());
        }

        // Block part reports the patch id of each face directly
        block.splitPartSurfaceByPatch(blockSurface, Arrays.asList(block.getMinXFace()), "L1", simulation);
        block.splitPartSurfaceByPatch(blockSurface, Arrays.asList(block.getMaxXFace()), "L2", simulation);
        block.splitPartSurfaceByPatch(blockSurface, Arrays.asList(block.getMinZFace()), "L4", simulation);
        if (type.equals("half")) {
            block.splitPartSurfaceByPatch(blockSurface, Arrays.asList(block.getMaxYFace()), "L0", simulation);
        }

        int expectedSurfaces = type.equals("half") ? 5 : 4;
        if (block.getPartSurfaces().size() != expectedSurfaces) {
            throw new IllegalStateException("Domain block split into " + block.getPartSurfaces().size()
                + " surfaces, expected " + expectedSurfaces);
        }

        // Remaining faces are slip walls: y-min and z-max (plus y-max for full car)
        blockSurface.setPresentationName("L3");
    }

    private void subtract_car(Simulation simulation, SimpleBlockPart block, List<GeometryPart> carParts, String partName) {
        List<Object> inputs = new ArrayList<>();
        inputs.add(block);
        inputs.addAll(carParts);

        SubtractPartsOperation subtractOp = (SubtractPartsOperation) simulation.get(MeshOperationManager.class)
            .createSubtractPartsOperation(new NeoObjectVector(inputs.toArray()));
        subtractOp.setTargetPart(block);
        subtractOp.setPresentationName("IFR_Subtract");
        subtractOp.execute();

        // Output part takes partName so regions and meshing find it
        Collection<GeometryPart> outputs = subtractOp.getOutputParts().getObjects();
        if (outputs.isEmpty()) {
            throw new IllegalStateException("Subtract operation produced no output part");
        }
        outputs.iterator().next().setPresentationName(partName);
    }

}
