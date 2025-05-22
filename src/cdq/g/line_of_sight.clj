(ns cdq.g.line-of-sight
  (:require [cdq.g :as g]
            [cdq.raycaster :as raycaster]
            [gdl.graphics.camera :as camera]))

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

(extend-type cdq.g.Game
  g/LineOfSight
  ; does not take into account size of entity ...
  ; => assert bodies <1 width then
  (line-of-sight? [{:keys [ctx/world-viewport
                           ctx/raycaster]}
                   source
                   target]
    (and (or (not (:entity/player? source))
             (on-screen? world-viewport target))
         (not (and los-checks?
                   (raycaster/blocked? raycaster
                                       (:position source)
                                       (:position target)))))))
