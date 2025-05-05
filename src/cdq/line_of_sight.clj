(ns cdq.line-of-sight
  (:require [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera]
            [cdq.math.raycaster :as raycaster]
            [cdq.world :as world]))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [viewport entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (camera/position (:camera viewport))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (:width viewport))  2)))
     (<= ydist (inc (/ (float (:height viewport)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn exists? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? graphics/world-viewport target))
       (not (and los-checks?
                 (raycaster/blocked? world/raycaster (:position source) (:position target))))))

(defn creatures-in-los-of-player []
  (->> world/active-entities
       (filter #(:entity/species @%))
       (filter #(exists? @world/player-eid @%))
       (remove #(:entity/player? @%))))
