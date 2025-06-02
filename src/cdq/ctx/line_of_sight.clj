(ns cdq.ctx.line-of-sight
  (:require [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [cdq.raycaster :as raycaster]))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [ctx position]
  (let [[x y] position
        x (float x)
        y (float y)
        [cx cy] (graphics/camera-position ctx)
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (graphics/world-viewport-width  ctx))  2)))
     (<= ydist (inc (/ (float (graphics/world-viewport-height ctx)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [{:keys [ctx/raycaster] :as ctx}
                      source
                      target]
  (and (or (not (:entity/player? source))
           (on-screen? ctx (entity/position target)))
       (not (and los-checks?
                 (raycaster/blocked? raycaster
                                     (entity/position source)
                                     (entity/position target))))))
