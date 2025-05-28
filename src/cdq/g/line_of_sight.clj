(ns cdq.g.line-of-sight
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            gdl.application))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [graphics position]
  (let [[x y] position
        x (float x)
        y (float y)
        [cx cy] (graphics/camera-position graphics)
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (graphics/world-viewport-width graphics))  2)))
     (<= ydist (inc (/ (float (graphics/world-viewport-height graphics)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

(extend-type gdl.application.Context
  g/LineOfSight
  ; does not take into account size of entity ...
  ; => assert bodies <1 width then
  (line-of-sight? [{:keys [ctx/graphics] :as ctx} source target]
    (and (or (not (:entity/player? source))
             (on-screen? graphics (entity/position target)))
         (not (and los-checks?
                   (g/ray-blocked? ctx
                                   (entity/position source)
                                   (entity/position target)))))))
