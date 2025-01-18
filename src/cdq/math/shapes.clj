(ns cdq.math.shapes ; cdq !!
  (:require [cdq.math.circle :as circle]
            [cdq.math.intersector :as intersector]
            [cdq.math.rectangle :as rectangle]))

(defn- rectangle? [{[x y] :left-bottom :keys [width height]}]
  (and x y width height))

(defn- circle? [{[x y] :position :keys [radius]}]
  (and x y radius))

(defn- m->shape [m]
  (cond
   (rectangle? m) (let [{:keys [left-bottom width height]} m
                        [x y] left-bottom]
                    (rectangle/create x y width height))

   (circle? m) (let [{:keys [position radius]} m
                     [x y] position]
                 (circle/create x y radius))

   :else (throw (Error. (str m)))))

(defn overlaps? [shape-a shape-b]
  (intersector/overlaps? (m->shape shape-a)
                         (m->shape shape-b)))

(defn rect-contains? [rectangle [x y]]
  (rectangle/contains? (m->shape rectangle) x y))

(defn circle->outer-rectangle [{[x y] :position :keys [radius] :as circle}]
  {:pre [(circle? circle)]}
  (let [radius (float radius)
        size (* radius 2)]
    {:left-bottom [(- (float x) radius)
                   (- (float y) radius)]
     :width  size
     :height size}))

(defn rectangle->tiles
  [{[x y] :left-bottom :keys [left-bottom width height]}]
  {:pre [left-bottom width height]}
  (let [x       (float x)
        y       (float y)
        width   (float width)
        height  (float height)
        l (int x)
        b (int y)
        r (int (+ x width))
        t (int (+ y height))]
    (set
     (if (or (> width 1) (> height 1))
       (for [x (range l (inc r))
             y (range b (inc t))]
         [x y])
       [[l b] [l t] [r b] [r t]]))))
