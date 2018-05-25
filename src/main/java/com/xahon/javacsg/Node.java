/**
 * Node.java
 *
 * <p>Copyright 2014-2014 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer
 * <info@michaelhoffer.de> OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * <p>The views and conclusions contained in the software and documentation are those of the authors
 * and should not be interpreted as representing official policies, either expressed or implied, of
 * Michael Hoffer <info@michaelhoffer.de>.
 */
package com.xahon.javacsg;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.util.Pair;

/**
 * Holds a node in a BSP tree. A BSP tree is built from a collection of polygons by picking a
 * polygon to split along. That polygon (and all other coplanar polygons) are added directly to that
 * node and the other polygons are added to the front and/or back subtrees. This is not a leafy BSP
 * tree since there is no distinction between internal and leaf nodes.
 */
final class Node {

  /** Polygons. */
  private List<Polygon> polygons;
  /** Plane used for BSP. */
  private Plane plane;
  /** Polygons in front of the plane. */
  private Node front;
  /** Polygons in back of the plane. */
  private Node back;

  /**
   * Constructor.
   *
   * <p>Creates a BSP node consisting of the specified polygons.
   *
   * @param polygons polygons
   */
  public Node(List<Polygon> polygons) {
    this.polygons = new ArrayList<>();
    if (polygons != null) {
      this.build(polygons);
    }
  }

  /** Constructor. Creates a node without polygons. */
  public Node() {
    this(null);
  }

  @Override
  public Node clone() {
    Node node = new Node();
    ArrayList<Pair<Node, Node>> nodes = new ArrayList<>(3);
    nodes.add(new Pair<>(this, node));

    while (nodes.size() > 0) {
      Pair<Node, Node> pair = nodes.get(0);
      Node currentNode = pair.getKey();
      Node nodeToClone = pair.getValue();
      nodes.remove(0);

      nodeToClone.plane = currentNode.plane == null ? null : currentNode.plane.clone();
      nodeToClone.front = currentNode.front == null ? null : currentNode.front.clone();
      nodeToClone.back = currentNode.back == null ? null : currentNode.back.clone();
      //        nodeToClone.polygons = new ArrayList<>();
      //        polygons.parallelStream().forEach((Polygon p) -> {
      //            nodeToClone.polygons.add(p.clone());
      //        });

      Stream<Polygon> polygonStream;

      if (currentNode.polygons.size() > 200) {
        polygonStream = currentNode.polygons.parallelStream();
      } else {
        polygonStream = currentNode.polygons.stream();
      }

      nodeToClone.polygons = polygonStream.map(p -> p.clone()).collect(Collectors.toList());
    }

    return node;
  }

  /**
   * Converts solid space to empty space and vice verca.
   *
   * @implNote This method uses loop instead of recursion
   */
  public void invert() {
    ArrayList<Node> nodes = new ArrayList<>(3);
    nodes.add(this);

    while (nodes.size() > 0) {
      Node currentNode = nodes.get(0);
      nodes.remove(0);

      Stream<Polygon> polygonStream;

      if (currentNode.polygons.size() > 200) {
        polygonStream = currentNode.polygons.parallelStream();
      } else {
        polygonStream = currentNode.polygons.stream();
      }

      polygonStream.forEach(
          (polygon) -> {
            polygon.flip();
          });

      if (currentNode.plane == null && !currentNode.polygons.isEmpty()) {
        currentNode.plane = currentNode.polygons.get(0)._csg_plane.clone();
      } else if (currentNode.plane == null && currentNode.polygons.isEmpty()) {

        System.err.println("Please fix me! I don't know what to do?");

        // throw new RuntimeException("Please fix me! I don't know what to do?");

        return;
      }

      currentNode.plane.flip();

      if (currentNode.front != null) {
        nodes.add(currentNode.front);
      }
      if (currentNode.back != null) {
        nodes.add(currentNode.back);
      }

      Node temp = currentNode.front;
      currentNode.front = currentNode.back;
      currentNode.back = temp;
    }
  }

  /**
   * Recursively removes all polygons in the {@link polygons} list that are contained within this
   * BSP tree.
   *
   * <p><b>Note:</b> polygons are splitted if necessary.
   *
   * @implNote This method uses loop instead of recursion
   * @param polygons the polygons to clip
   * @return the cliped list of polygons
   */
  private List<Polygon> clipPolygons(List<Polygon> polygons) {
    ArrayList<Polygon> result = new ArrayList<>(10);
    ArrayList<Pair<Node, List<Polygon>>> nodes = new ArrayList<>(3);
    nodes.add(new Pair<>(this, polygons));

    while (nodes.size() > 0) {
      Pair<Node, List<Polygon>> pair = nodes.get(0);
      Node currentNode = pair.getKey();
      List<Polygon> currentPolygons = pair.getValue();
      nodes.remove(0);

      if (currentNode.plane == null) {
        result.addAll(currentPolygons);
        continue;
      }

      List<Polygon> frontP = new ArrayList<>();
      List<Polygon> backP = new ArrayList<>();

      for (Polygon polygon : currentPolygons) {
        currentNode.plane.splitPolygon(polygon, frontP, backP, frontP, backP);
      }

      if (currentNode.front != null) {
        nodes.add(new Pair<>(currentNode.front, frontP));
      } else {
        result.addAll(frontP);
      }

      if (currentNode.back != null) {
        nodes.add(new Pair<>(currentNode.back, backP));
        //      } else {
        //        backP = new ArrayList<>(0);
      }

      //      frontP.addAll(backP);
    }

    return result;
  }

  // Remove all polygons in this BSP tree that are inside the other BSP tree
  // `bsp`.
  /**
   * Removes all polygons in this BSP tree that are inside the specified BSP tree ({@code other}).
   *
   * <p><b>Note:</b> polygons are splitted if necessary.
   *
   * @implNote This method uses loop instead of recursion
   * @param other other that shall be used for clipping
   */
  public void clipTo(Node other) {
    ArrayList<Node> nodes = new ArrayList<>(3);
    nodes.add(this);

    while (nodes.size() > 0) {
      Node currentNode = nodes.get(0);
      nodes.remove(0);

      currentNode.polygons = other.clipPolygons(currentNode.polygons);
      if (currentNode.front != null) {
        nodes.add(currentNode.front);
      }
      if (currentNode.back != null) {
        nodes.add(currentNode.back);
      }
    }
  }

  /**
   * Returns a list of all polygons in this BSP tree.
   *
   * @implNote This method uses loop instead of recursion
   * @return a list of all polygons in this BSP tree
   */
  public List<Polygon> allPolygons() {
    List<Polygon> polygonList = new ArrayList<>(10);

    ArrayList<Node> nodes = new ArrayList<>(3);
    nodes.add(this);

    while (nodes.size() > 0) {
      Node currentNode = nodes.get(0);
      nodes.remove(0);

      polygonList.addAll(currentNode.polygons);

      if (currentNode.front != null) {
        nodes.add(currentNode.front);
      }
      if (currentNode.back != null) {
        nodes.add(currentNode.back);
      }
    }

    return polygonList;
  }

  /**
   * Build a BSP tree out of {@code polygons}. When called on an existing tree, the new polygons are
   * filtered down to the bottom of the tree and become new nodes there. Each set of polygons is
   * partitioned using the first polygon (no heuristic is used to pick a good split).
   *
   * @param polygons polygons used to build the BSP
   */
  public final void build(List<Polygon> polygons) {
    List<Pair<Node, List<Polygon>>> nodes = new ArrayList<>(3);
    nodes.add(new Pair<>(this, polygons));

    while (nodes.size() > 0) {
      Pair<Node, List<Polygon>> pair = nodes.get(0);
      Node currentNode = pair.getKey();
      List<Polygon> currentPolygons = pair.getValue();
      nodes.remove(0);

      if (currentPolygons.isEmpty()) return;

      if (currentNode.plane == null) {
        currentNode.plane = currentPolygons.get(0)._csg_plane.clone();
      }

      currentPolygons =
          currentPolygons.stream().filter(p -> p.isValid()).distinct().collect(Collectors.toList());

      List<Polygon> frontP = new ArrayList<>();
      List<Polygon> backP = new ArrayList<>();

      // parellel version does not work here
      for (Polygon polygon : currentPolygons) {
        currentNode.plane.splitPolygon(
            polygon, currentNode.polygons, currentNode.polygons, frontP, backP);
      }

      if (frontP.size() > 0) {
        if (currentNode.front == null) {
          currentNode.front = new Node();
        }
        nodes.add(new Pair<>(currentNode.front, frontP));
      }
      if (backP.size() > 0) {
        if (currentNode.back == null) {
          currentNode.back = new Node();
        }
        nodes.add(new Pair<>(currentNode.back, backP));
      }
    }
  }
}
