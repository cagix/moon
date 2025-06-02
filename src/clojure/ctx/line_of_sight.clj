(ns clojure.ctx.line-of-sight
  (:require [clojure.entity :as entity]
            [clojure.raycaster :as raycaster]
            [clojure.gdx.graphics.camera :as camera]))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [viewport position]
  (let [[x y] position
        x (float x)
        y (float y)
        [cx cy] (camera/position (:camera viewport))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (:width  viewport)) 2)))
     (<= ydist (inc (/ (float (:height viewport)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [{:keys [ctx/raycaster
                              ctx/world-viewport]}
                      source
                      target]
  (and (or (not (:entity/player? source))
           (on-screen? world-viewport (entity/position target)))
       (not (and los-checks?
                 (raycaster/blocked? raycaster
                                     (entity/position source)
                                     (entity/position target))))))
