// Simcenter STAR-CCM+ macro: macro.java
// Written by Simcenter STAR-CCM+ 20.06.010
package macro;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javax.swing.Action;


import star.common.*;
import star.base.neo.*;
import star.screenplay.operations.*;
import star.vis.*;
import star.cadmodeler.*;
import star.screenplay.*;
import star.screenplay.operations.vis.*;
import star.screenplay.vis.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonArray;

import star.flow.*;

public class post_processing extends StarMacro {

  private Simulation simulation;                  
  private Region region_FormulaStudentCar;
  //Boundaries
  private Boundary boundary_bodyworkL100;
  private Boundary boundary_bodyworkL101;
  private Boundary boundary_wheelsL20;
  private Boundary[] boundaries;
  //Plane Sections
  private PlaneSection Plane_XY;
  private PlaneSection Plane_XZ;
  private PlaneSection Plane_YZ;
  private static class SceneParam
  {
    String sceneName;
    Boundary[] boundaryName;
    PlaneSection[] planeName;
    String fieldFunction;
    double licLength;
    double licNoiseFrequency;
    double licIntensity;
    boolean symmetry;

    double[] viewRight;
    double[] viewUp;

    double[] camPos;
    double[] camUp;
    double[] camFocus;
    double parallelScale;

    SceneParam(String sceneName, Boundary[] boundaryName, PlaneSection[] planeName,String fieldFunction, double licLength, double licNoiseFrequency, double licIntensity, boolean symmetry, double[] viewRight, double[] viewUp, double[] camPos, double[] camUp,
               double[] camFocus, double parallelScale)
    {
      this.sceneName = sceneName;
      this.boundaryName = boundaryName;
      this.planeName = planeName;
      this.fieldFunction = fieldFunction;
      this.licLength = licLength;
      this.licNoiseFrequency = licNoiseFrequency;
      this.licIntensity = licIntensity;
      this.symmetry = symmetry;

      this.viewRight = viewRight;
      this.viewUp = viewUp;

      this.camPos = camPos;
      this.camUp = camUp;
      this.camFocus = camFocus;
      this.parallelScale = parallelScale;
    }
  }


  public void execute() {
        JsonObject config = verify_json();
        simulation = getActiveSimulation();            
        region_FormulaStudentCar = simulation.getRegionManager().getRegion("FormulaStudentCar");

        initBoundaries();
        boundaries = new Boundary[] {boundary_bodyworkL100,boundary_bodyworkL101,boundary_wheelsL20};

        FieldFunctions(config);

        SetDerivedPart(config);
        Geometry_Scene(config);
        SceneParam p1 = new SceneParam("Scalar_PC_Plane_XY_Sweep",
              new Boundary[] {}, new PlaneSection[] {Plane_XY},
              "PressureCoefficient",1,0.317,0.449,true,
              new double[] {1.0, 0.0, 0.0},                               
              new double[] {0.0, 1.0, 0.0},                                  
              new double[] {1.966440256229979, 0.06035306756886705, 0.029999973712259376}, 
              new double[] {0.0, 1.0, 0.0},
              new double[] {1.966440256229979, 0.06035306756886705, 9.605823827550937},1.8608723771684792);       
        SceneParam p2 = new SceneParam("Scalar_PC_Plane_XZ_Sweep",
              new Boundary[] {}, new PlaneSection[] {Plane_XZ},
              "PressureCoefficient",1,0.317,0.449,false,
              new double[] {1.0, 0.0, 0.0},    
              new double[] {0.0, 0.0, 1.0},   
              new double[] {-0.026332151265919118, 17.74828759089466, 1.4235777041461664},  
              new double[] {0.0, 0.0, 1.0},
              new double[] {-0.026332151265919118, -0.500000057103339, 1.4235777041461664},1.8608723771684792);   
        SceneParam p3 = new SceneParam("Scalar_PC_Plane_YZ_Sweep",
              new Boundary[] {}, new PlaneSection[] {Plane_YZ},
              "PressureCoefficient",1,0.317,0.449,true, 
              new double[] {0.0, 1.0, 0.0},   
              new double[] {0.0, 0.0, 1.0},    
              new double[] {6.407300692987433, 0.06634924768046527, 0.7160578966233986},
              new double[] {0.0, 0.0, 1.0},
              new double[] {-0.3861792438850876, 0.06634924768046527, 0.4916695678563186},0.5322977739075953);  
        Scalar_Scene(p1,config);
        Scalar_Scene(p2,config);
        Scalar_Scene(p3,config);

        SceneParam tp1 = new SceneParam("Scalar_TPC_Plane_XY_Sweep",
              new Boundary[] {}, new PlaneSection[] {Plane_XY},
              "TotalPressureCoefficient",1,0.317,0.449,true,
              new double[] {1.0, 0.0, 0.0},                                   
              new double[] {0.0, 1.0, 0.0},                                 
              new double[] {1.966440256229979, 0.06035306756886705, 0.029999973712259376}, 
              new double[] {0.0, 1.0, 0.0},
              new double[] {1.966440256229979, 0.06035306756886705, 9.605823827550937},1.8608723771684792);       
        SceneParam tp2 = new SceneParam("Scalar_TPC_Plane_XZ_Sweep",
              new Boundary[] {}, new PlaneSection[] {Plane_XZ},
              "TotalPressureCoefficient",1,0.317,0.449,false,
              new double[] {1.0, 0.0, 0.0},    
              new double[] {0.0, 0.0, 1.0},    
              new double[] {-0.026332151265919118, 17.74828759089466, 1.4235777041461664}, 
              new double[] {0.0, 0.0, 1.0},
              new double[] {-0.026332151265919118, -0.500000057103339, 1.4235777041461664},1.8608723771684792);   
        SceneParam tp3 = new SceneParam("Scalar_TPC_Plane_YZ_Sweep",
              new Boundary[] {}, new PlaneSection[] {Plane_YZ},
              "TotalPressureCoefficient",1,0.317,0.449,true, 
              new double[] {0.0, 1.0, 0.0},    
              new double[] {0.0, 0.0, 1.0},   
              new double[] {6.407300692987433, 0.06634924768046527, 0.7160578966233986},  
              new double[] {0.0, 0.0, 1.0},
              new double[] {-0.3861792438850876, 0.06634924768046527, 0.4916695678563186},0.5322977739075953);   
        Scalar_Scene(tp1,config);
        Scalar_Scene(tp2,config);
        Scalar_Scene(tp3,config);

        SceneParam pc_s = new SceneParam("Scalar_PC_Surface",
            boundaries, new PlaneSection[] {},
            "PressureCoefficient",1,0.317,0.449,true,
            new double[] {1.0, 0.0, 0.0},                                   
            new double[] {0.0, 1.0, 0.0},                                 
            new double[] {1.966440256229979, 0.06035306756886705, 0.029999973712259376}, 
            new double[] {0.0, 1.0, 0.0},
            new double[] {1.966440256229979, 0.06035306756886705, 9.605823827550937},1.8608723771684792);       
        Scalar_Scene(pc_s,config);

        SceneParam tpc_s = new SceneParam("Scalar_TPC_Surface",
            boundaries, new PlaneSection[] {},
            "TotalPressureCoefficient",1,0.317,0.449,true,
            new double[] {1.0, 0.0, 0.0},                                   
            new double[] {0.0, 1.0, 0.0},                                 
            new double[] {1.966440256229979, 0.06035306756886705, 0.029999973712259376}, 
            new double[] {0.0, 1.0, 0.0},
            new double[] {1.966440256229979, 0.06035306756886705, 9.605823827550937},1.8608723771684792);       
        Scalar_Scene(tpc_s,config);


        
  }

 private void initBoundaries() {
    boundary_bodyworkL100 = region_FormulaStudentCar.getBoundaryManager().getBoundary("BODYWORK.L100");
    boundary_bodyworkL101 = region_FormulaStudentCar.getBoundaryManager().getBoundary("BODYWORK.L101");
    boundary_wheelsL20   = region_FormulaStudentCar.getBoundaryManager().getBoundary("WHEELS.L20");
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



  //set derived parts
  private void SetDerivedPart(JsonObject config)
  {
    JsonObject sections = config.getJsonObject("PostProcessing").getJsonObject("PlaneSections");
    JsonObject xy = sections.getJsonObject("Plane_XY");
    JsonObject xz = sections.getJsonObject("Plane_XZ");
    JsonObject yz = sections.getJsonObject("Plane_YZ");


    double min_z = xy.getJsonNumber("min").doubleValue();
    double max_z = xy.getJsonNumber("max").doubleValue();

    double min_y = xz.getJsonNumber("min").doubleValue();
    double max_y = xz.getJsonNumber("max").doubleValue();

    double min_x = yz.getJsonNumber("min").doubleValue();
    double max_x = yz.getJsonNumber("max").doubleValue();

    Plane_XY = 
      (PlaneSection) simulation.getPartManager().createImplicitPart(new ArrayList<>(Collections.<NamedObject>emptyList()), new DoubleVector(new double[] {0.0, 0.0, 1.0}), new DoubleVector(new double[] {0.0, 0.0, min_z}), 0, 1, new DoubleVector(new double[] {0.0}), null);
    Plane_XY.getInputParts().setQuery(null);
    Plane_XY.getInputParts().setObjects(region_FormulaStudentCar);
    

    Plane_XZ = 
      (PlaneSection) simulation.getPartManager().createImplicitPart(new ArrayList<>(Collections.<NamedObject>emptyList()), new DoubleVector(new double[] {0.0, 1.0, 0.0}), new DoubleVector(new double[] {0.0, min_y, 0.0}), 0, 1, new DoubleVector(new double[] {0.0}), null);

    Plane_XZ.getInputParts().setQuery(null);
    Plane_XZ.getInputParts().setObjects(region_FormulaStudentCar);

    Plane_YZ = 
      (PlaneSection) simulation.getPartManager().createImplicitPart(new ArrayList<>(Collections.<NamedObject>emptyList()), new DoubleVector(new double[] {1.0, 0.0, 0.0}), new DoubleVector(new double[] {min_x, 0.0, 0.0}), 0, 1, new DoubleVector(new double[] {0.0}), null);

    Plane_YZ.getInputParts().setQuery(null);
    Plane_YZ.getInputParts().setObjects(region_FormulaStudentCar);

    Plane_XY.setPresentationName("Plane_XY");
    Plane_XZ.setPresentationName("Plane_XZ");
    Plane_YZ.setPresentationName("Plane_YZ"); 

  }
  private void Geometry_Scene(JsonObject config)
  {
    Scene Geo_Scene_Planes = 
        simulation.getSceneManager().createGeometryScene("Geometry Scene","Outline","Surface",0,null);

    Geo_Scene_Planes.setPresentationName("Geo_Planes");
    PartDisplayer Outline_1 = 
    ((PartDisplayer) Geo_Scene_Planes.getDisplayerManager().getObject("Outline 1"));
    Outline_1.setVisibilityOverrideMode(DisplayerVisibilityOverride.HIDE_ALL_PARTS);
    PartDisplayer Surface_1 = 
    ((PartDisplayer) Geo_Scene_Planes.getDisplayerManager().getObject("Surface 1"));
    Surface_1.getInputParts().setQuery(null);
    Surface_1.getInputParts().setObjects(boundary_bodyworkL100, boundary_bodyworkL101, boundary_wheelsL20, Plane_XY, Plane_XZ, Plane_YZ);
    Geo_Scene_Planes.getCurrentView().setInput(new DoubleVector(new double[] {1.2250540246782942, 0.7592745041476743, 0.7966067948106756}), new DoubleVector(new double[] {10.25645524801995, -22.91452925054066, 7.316052195332163}), new DoubleVector(new double[] {-0.08732152603594914, 0.2333868801547426, 0.9684552210931716}), 4.959849764688727, 1, 30.0);
  }


  private void Scalar_Scene(SceneParam p, JsonObject config)
  {
    Scene scene = 
    simulation.getSceneManager().createVectorScene("Vector Scene","Outline","Vector",null);
      scene.setPresentationName(p.sceneName);
    PartDisplayer Outline_1 = 
      ((PartDisplayer) scene.getDisplayerManager().getObject("Outline 1"));
    Outline_1.setVisibilityOverrideMode(DisplayerVisibilityOverride.HIDE_ALL_PARTS);
    VectorDisplayer Vector_1 = 
      ((VectorDisplayer) scene.getDisplayerManager().getObject("Vector 1"));
    Vector_1.getInputParts().setQuery(null);
    
    
    List<NamedObject> allParts = new ArrayList<>();
    if (p.boundaryName != null) {
        allParts.addAll(Arrays.asList(p.boundaryName));
    }
    if (p.planeName != null) {
        allParts.addAll(Arrays.asList(p.planeName));
    }
    Vector_1.getInputParts().setObjects(allParts.toArray(new NamedObject[0]));


    Vector_1.setDisplayMode(VectorDisplayMode.VECTOR_DISPLAY_MODE_LIC);
    Vector_1.setColorMode(VectorColorMode.SCALAR);
    //Surface_1.getInputParts().setObjects(boundary_bodyworkL100, boundary_bodyworkL101, boundary_wheelsL20, Plane_XY, Plane_XZ, Plane_YZ);    
    if (p.symmetry)
    {
      SymmetricRepeat symmetricRepeat_0 = 
        ((SymmetricRepeat) simulation.getTransformManager().getObject("Domain.L0 1"));
        Vector_1.setVisTransform(symmetricRepeat_0);
    }
    FieldFunction pressureCoefficientFunction_0 = simulation.getFieldFunctionManager().getFunction(p.fieldFunction);
      Vector_1.getColoringScalar().setFieldFunction(pressureCoefficientFunction_0);
    //LIC Setting
    LICSettings lICSettings_0 = 
    Vector_1.getLICSettings();
    lICSettings_0.setLineLength(p.licLength);
    lICSettings_0.setNoiseFrequency(p.licNoiseFrequency);
    lICSettings_0.setIntensity(p.licIntensity);
    CurrentView currentView_6 = 
      scene.getCurrentView();
    scene.setViewOrientation(new DoubleVector(p.viewRight), new DoubleVector(p.viewUp));
    currentView_6.setInput(new DoubleVector(p.camPos), new DoubleVector(p.camFocus), new DoubleVector(p.camUp), p.parallelScale, 1, 30.0);
  }

  private void FieldFunctions(JsonObject config)
  {

    PressureCoefficientFunction pressureCoefficientFunction_0 = 
      ((PressureCoefficientFunction) simulation.getFieldFunctionManager().getFunction("PressureCoefficient"));
    pressureCoefficientFunction_0.getReferenceDensity().setDefinition("${IFR_ref_density}");
    pressureCoefficientFunction_0.getReferencePressure().setDefinition("${IFR_ref_pressure}");
    pressureCoefficientFunction_0.getReferenceVelocity().setDefinition("${IFR_vel_mag}");
    
    TotalPressureCoefficientFunction TotalPressureCoefficientFunction_0 = 
      ((TotalPressureCoefficientFunction) simulation.getFieldFunctionManager().getFunction("TotalPressureCoefficient"));
    TotalPressureCoefficientFunction_0.getReferenceDensity().setDefinition("${IFR_ref_density}");
    TotalPressureCoefficientFunction_0.getReferencePressure().setDefinition("${IFR_ref_pressure}");
    TotalPressureCoefficientFunction_0.getReferenceVelocity().setDefinition("${IFR_vel_mag}");
  }

}

