// Simcenter STAR-CCM+ macro: regions_bcs.java
// Written by Simcenter STAR-CCM+ 17.04.008

// This subroutine sets up the regions and BCs

package macro;

import java.util.*;

import star.common.*;
import star.base.neo.*;
import star.turbulence.*;
import star.cadmodeler.*;
import star.flow.*;
import star.kwturb.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;



public class create_regions extends StarMacro {
    
    public void execute() {
        JsonObject config = verify_json();
        String partName = config.getString("partName");
        boolean halfCar = config.getJsonObject("domain").getString("type").equals("half");

        assign_regions(partName);

        Simulation simulation = getActiveSimulation();
        Region region = simulation.getRegionManager().getRegion(partName);
        set_standard_regions(region, simulation, halfCar);

        // Add logic for wheels in future, will also need a dedicated layer name
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

    private Boundary find_boundary(Region region, String layer) {
        // Subtract output prefixes surface names (e.g. "Domain.L1"), so match on suffix
        for (Boundary boundary : region.getBoundaryManager().getBoundaries()) {
            String name = boundary.getPresentationName();
            if (name.equals(layer) || name.endsWith("." + layer)) {
                return boundary;
            }
        }
        throw new IllegalStateException("No boundary found for layer " + layer + " in region " + region.getPresentationName());
    }

    private void assign_regions(String partName) {
        // Assign parts to regions, each surface individual region
        Simulation simulation_0 = getActiveSimulation();
        GeometryPart domainPart_0 = simulation_0.get(SimulationPartManager.class).getPart(partName);
        simulation_0.getRegionManager().newRegionsFromParts(new NeoObjectVector(new Object[] {domainPart_0}), "OneRegionPerPart", null, "OneBoundaryPerPartSurface", null, "OneFeatureCurve", null, RegionManager.CreateInterfaceMode.BOUNDARY, "OneEdgeBoundaryPerPart", null);
    }

    private void set_standard_regions(Region region, Simulation simulation, boolean halfCar) {
        // Define units
        Units units_0 = simulation.getUnitsManager().getPreferredUnits(Dimensions.Builder().build());
        Units units_1 = simulation.getUnitsManager().getPreferredUnits(Dimensions.Builder().length(1).build());
        Units units_2 = simulation.getUnitsManager().getPreferredUnits(Dimensions.Builder().velocity(1).build());

        // Symmetry plane (half car only, full car side walls are L3 slip walls)
        // (need to implement logic for two sided with cornering cases in future)
        if (halfCar) {
            Boundary boundary_1 = find_boundary(region, "L0");
            SymmetryBoundary symmetryBoundary_0 = ((SymmetryBoundary) simulation.get(ConditionTypeManager.class).get(SymmetryBoundary.class));
            boundary_1.setBoundaryType(symmetryBoundary_0);
        }

        // Velocity Inlet
        Boundary boundary_2 = find_boundary(region, "L1");
        InletBoundary inletBoundary_0 = ((InletBoundary) simulation.get(ConditionTypeManager.class).get(InletBoundary.class));
        boundary_2.setBoundaryType(inletBoundary_0);

        boundary_2.getConditions().get(InletVelocityOption.class).setSelected(InletVelocityOption.Type.COMPONENTS);
        boundary_2.getConditions().get(KwTurbSpecOption.class).setSelected(KwTurbSpecOption.Type.INTENSITY_LENGTH_SCALE);

        VelocityProfile velocityProfile_0 = boundary_2.getValues().get(VelocityProfile.class);
        velocityProfile_0.getMethod(ConstantVectorProfileMethod.class).getQuantity().setDefinition("$${IFR_vel}");

        TurbulenceIntensityProfile turbulenceIntensityProfile_0 = boundary_2.getValues().get(TurbulenceIntensityProfile.class);
        turbulenceIntensityProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("${IFR_turb_intensity}");
        
        TurbulentLengthScaleProfile turbulentLengthScaleProfile_0 = boundary_2.getValues().get(TurbulentLengthScaleProfile.class);
        turbulentLengthScaleProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().setDefinition("${IFR_turb_length}");

        // Pressure Outlet
        Boundary boundary_3 = find_boundary(region, "L2");
        PressureBoundary pressureBoundary_0 = ((PressureBoundary) simulation.get(ConditionTypeManager.class).get(PressureBoundary.class));
        boundary_3.setBoundaryType(pressureBoundary_0);

        // Slip Walls
        // (might need to add more layers here if walls and roof different layer)
        Boundary boundary_4 = find_boundary(region, "L3");
        boundary_4.getConditions().get(WallShearStressOption.class).setSelected(WallShearStressOption.Type.SLIP);
        
        // Road
        Boundary boundary_5 = find_boundary(region, "L4");
        boundary_5.getConditions().get(WallSlidingOption.class).setSelected(WallSlidingOption.Type.VECTOR);
        WallRelativeVelocityProfile wallRelativeVelocityProfile_0 = boundary_5.getValues().get(WallRelativeVelocityProfile.class);
        wallRelativeVelocityProfile_0.getMethod(ConstantVectorProfileMethod.class).getQuantity().setDefinition("$${IFR_vel}");
        
    }

}
