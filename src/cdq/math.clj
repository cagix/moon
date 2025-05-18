(ns cdq.math
  (:require [gdl.math :as math]))

(defn- rectangle? [{[x y] :left-bottom :keys [width height]}]
  (and x y width height))

(defn- circle? [{[x y] :position :keys [radius]}]
  (and x y radius))

(defn- m->shape [m]
  (cond
   (rectangle? m) (let [{:keys [left-bottom width height]} m
                        [x y] left-bottom]
                    (math/rectangle x y width height))

   (circle? m) (let [{:keys [position radius]} m
                     [x y] position]
                 (math/circle x y radius))

   :else (throw (Error. (str m)))))

(defn overlaps? [shape-a shape-b]
  (math/overlaps? (m->shape shape-a)
                  (m->shape shape-b)))

(defn rect-contains? [rectangle [x y]]
  (math/contains? (m->shape rectangle) x y))

(defn circle->outer-rectangle [{[x y] :position :keys [radius] :as circle}]
  {:pre [(circle? circle)]}
  (let [radius (float radius)
        size (* radius 2)]
    {:left-bottom [(- (float x) radius)
                   (- (float y) radius)]
     :width  size
     :height size}))
