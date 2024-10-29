(ns moon.world
  (:require [gdl.graphics.camera :as cam]
            [gdl.utils :refer [dispose]]
            [moon.graphics.world-view :as world-view]
            [moon.world.raycaster :refer [ray-blocked?]]))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (cam/position (world-view/camera))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (world-view/width))  2)))
     (<= ydist (inc (/ (float (world-view/height)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? target))
       (not (and los-checks?
                 (ray-blocked? (:position source) (:position target))))))

(def mouseover-eid nil)

(defn mouseover-entity []
  (when-let [eid mouseover-eid]
    @eid))

(declare tiled-map)

(defn clear-tiled-map []
  (when (bound? #'tiled-map)
    (dispose tiled-map)))

(declare explored-tile-corners)
