/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xahon.javacsg.samples;

import com.xahon.javacsg.Extrude;
import com.xahon.javacsg.CSG;
import com.xahon.javacsg.Cylinder;
import com.xahon.javacsg.FileUtil;

import static eu.mihosoft.vvecmath.Transform.unity;
import eu.mihosoft.vvecmath.Vector3d;
import java.io.IOException;
import java.nio.file.Paths;

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class ServoWheel {

    private double toothLength = 0.7;
    private double toothWidth = 0.1;
    private double toothHeight = 0.3;
    private int toothCount = 25;
    private double headHeight = 4;
    private double headDiameter = 5.92;
    private double headScrewDiameter = 2.5;
    private double headThickness = 1.1;

    private ServoHead servoHead = new ServoHead(toothLength, toothWidth, toothHeight, toothCount, headHeight, headDiameter, headScrewDiameter, headThickness);

    private int numberOfArms = 3;
    double innerWidth = 7;
    double outerWidth = 3.5;
    double thickness = 2;
    double radius = 40;
    double ringThickness = 3;
    double wheelThickness = 5;

    double minorArmLength = radius * 0.75;
    double minorArmHeight = headHeight;
    double minorArmThickness = 2.5;
    
    double outerRingThickness = wheelThickness/3.0*2;
    double outerRingDepth = 0.5;

    public CSG toCSG() {

        double dt = 360.0 / numberOfArms;

        CSG arms = null;

        for (int i = 0; i < numberOfArms; i++) {

            CSG arm = servoArm(innerWidth, outerWidth, thickness, radius, ringThickness, minorArmThickness, minorArmLength, minorArmHeight).transformed(unity().rotZ(dt * i));

            if (arms == null) {
                arms = arm;
            } else {
                arms = arms.union(arm);
            }
        }

        CSG sHead = servoHead.servoHeadFemale();

        CSG screwHole = new Cylinder(headScrewDiameter / 2.0, ringThickness * 2, 16).toCSG();

        if (arms != null) {
            sHead = sHead.union(arms);
        }

        sHead = sHead.difference(screwHole);

        CSG outerWheelCylinder = new Cylinder(radius, wheelThickness, 64).toCSG();
        CSG innerWheelCylinder = new Cylinder(radius - ringThickness, wheelThickness, 64).toCSG();

        CSG ring = outerWheelCylinder.difference(innerWheelCylinder);

        CSG wheel = ring.union(sHead);
        
       
        
        CSG outerRingOutCylinder = new Cylinder(radius, outerRingThickness, 64).toCSG();
        CSG outerRingInnerCylinder = new Cylinder(radius-outerRingDepth, outerRingThickness, 64).toCSG();
        
        CSG outerRing = outerRingOutCylinder.difference(outerRingInnerCylinder).
                transformed(unity().translateZ(wheelThickness*0.5-outerRingThickness*0.5));
        
        wheel = wheel.difference(outerRing);
        
        return wheel;
    }

    public CSG servoArm(
            double innerWidth, double outerWidth, double thickness, double radius, double wheelThickness, double minorArmThickness, double minorArmLegth, double minorArmHeight) {
        CSG mainArm = Extrude.points(Vector3d.z(thickness),
                Vector3d.xy(-innerWidth * 0.5, 0),
                Vector3d.xy(innerWidth * 0.5, 0),
                Vector3d.xy(outerWidth * 0.5, radius - wheelThickness),
                Vector3d.xy(-outerWidth * 0.5, radius - wheelThickness)
        );

        CSG minorArm = Extrude.points(Vector3d.z(minorArmThickness),
                Vector3d.xy(headDiameter * 0.5 + headThickness * 0.5, thickness),
                Vector3d.xy(minorArmLegth - headDiameter * 0.5 - headThickness * 0.5, thickness),
                Vector3d.xy(headDiameter * 0.5 + headThickness * 0.5, minorArmHeight + thickness * 0.5)).transformed(unity().rot(-90, 0, 0).translateZ(-minorArmThickness * 0.5));

        minorArm = minorArm.transformed(unity().rotZ(-90));

        return mainArm.union(minorArm);

    }

    public static void main(String[] args) throws IOException {

        System.out.println("RUNNING");

        FileUtil.write(Paths.get("servo-wheel.stl"), new ServoWheel().toCSG().toStlString());

    }
}
