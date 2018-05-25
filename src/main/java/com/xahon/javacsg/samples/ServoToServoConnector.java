/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xahon.javacsg.samples;

import com.xahon.javacsg.CSG;
import com.xahon.javacsg.Extrude;
import com.xahon.javacsg.FileUtil;
import eu.mihosoft.vvecmath.Transform;
import eu.mihosoft.vvecmath.Vector3d;
import java.io.IOException;
import java.nio.file.Paths;

/**
 *
 * @author miho
 */
public class ServoToServoConnector {

    //standard servo
    private double servoWidth = 40.0;
    private double servoThickness = 19.0;
    private double borderThickness = 2;
    private double connectorThickness = 4;
    private double servoMountHeight = 10;
    
    private double servoDistance = 17;
    private double height=12;
    

    public CSG toCSG() {
        
        double sth = servoThickness;
        double sd = servoDistance;
        double th = borderThickness;
        double th2 = connectorThickness;
        
        double h = height;
        
        CSG fork = Extrude.points(Vector3d.xyz(0, 0,servoMountHeight),
                Vector3d.xy(0,0),
                Vector3d.xy(sth,0),
                Vector3d.xy(sth,h),
                Vector3d.xy(sth+th,h),
                Vector3d.xy(sth+th,-th),
                Vector3d.xy(sth/2+th2/2,-th),
                Vector3d.xy(sth/2+th2/4,-th-sd/2),
                Vector3d.xy(sth/2-th2/4,-th-sd/2),
                Vector3d.xy(sth/2-th2/2,-th),
                Vector3d.xy(-th,-th),
                Vector3d.xy(-th,h),
                Vector3d.xy(0,h)
        );
        
        CSG fork2 = fork.transformed(Transform.unity().rotZ(180).translateX(-sth).translateY(sd+th*2));
        
        return fork.union(fork2);
    }

    public static void main(String[] args) throws IOException {

        ServoToServoConnector servo2ServoConnector = new ServoToServoConnector();

        // save union as stl
//        FileUtil.write(Paths.get("sample.stl"), new ServoHead().servoHeadFemale().transformed(Transform.unity().scale(1.0)).toStlString());
        FileUtil.write(Paths.get("sample.stl"), servo2ServoConnector.toCSG().toStlString());

    }
}
