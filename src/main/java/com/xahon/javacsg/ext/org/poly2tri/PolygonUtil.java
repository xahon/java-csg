/**
 * PolygonUtil.java
 *
 * Copyright 2014-2014 Michael Hoffer <info@michaelhoffer.de>. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Michael Hoffer
 * <info@michaelhoffer.de>.
 */
package com.xahon.javacsg.ext.org.poly2tri;

import com.xahon.javacsg.Edge;
import com.xahon.javacsg.Polygon;
import com.xahon.javacsg.Extrude;
import eu.mihosoft.vvecmath.Vector3d;
import com.xahon.javacsg.Vertex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class PolygonUtil {

    private PolygonUtil() {
        throw new AssertionError("Don't instantiate me!", null);
    }

    /**
     * Converts a CSG polygon to a poly2tri polygon (including holes)
     * @param polygon the polygon to convert
     * @return a CSG polygon to a poly2tri polygon (including holes)
     */
    private static com.xahon.javacsg.ext.org.poly2tri.Polygon fromCSGPolygon(
            Polygon polygon) {
        
        // convert polygon
        List< PolygonPoint> points = new ArrayList<>();
        for (Vertex v : polygon.vertices) {
            PolygonPoint vp = new PolygonPoint(v.pos.x(), v.pos.y(), v.pos.z());
            points.add(vp);
        }

        com.xahon.javacsg.ext.org.poly2tri.Polygon result
                = new com.xahon.javacsg.ext.org.poly2tri.Polygon(points);

        // convert holes
        Optional<List<Polygon>> holesOfPresult
                = polygon.
                getStorage().getValue(Edge.KEY_POLYGON_HOLES);
        if (holesOfPresult.isPresent()) {
            List<Polygon> holesOfP = holesOfPresult.get();

            holesOfP.stream().forEach((hP) -> {
                result.addHole(fromCSGPolygon(hP));
            });
        }

        return result;
    }

    public static List<Polygon> concaveToConvex(
            Polygon concave) {

        List<Polygon> result = new ArrayList<>();

        Vector3d normal = concave.vertices.get(0).normal.clone();

        boolean cw = !Extrude.isCCW(concave);

        com.xahon.javacsg.ext.org.poly2tri.Polygon p
                = fromCSGPolygon(concave);
        
        Poly2Tri.triangulate(p);

        List<DelaunayTriangle> triangles = p.getTriangles();

        List<Vertex> triPoints = new ArrayList<>();

        for (DelaunayTriangle t : triangles) {

            int counter = 0;
            for (TriangulationPoint tp : t.points) {

                triPoints.add(new Vertex(
                        Vector3d.xyz(tp.getX(), tp.getY(), tp.getZ()),
                        normal));

                if (counter == 2) {
                    if (!cw) {
                        Collections.reverse(triPoints);
                    }
                    Polygon poly =
                            new Polygon(
                                    triPoints, concave.getStorage());
                    result.add(poly);
                    counter = 0;
                    triPoints = new ArrayList<>();

                } else {
                    counter++;
                }
            }
        }

        return result;
    }
}
