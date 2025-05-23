(ns cdq.g.line-of-sight
  (:require cdq.gdx
            [cdq.c :as c]
            [cdq.g :as g]))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [ctx position]
  (let [[x y] position
        x (float x)
        y (float y)
        [cx cy] (c/camera-position ctx)
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (c/world-viewport-width ctx))  2)))
     (<= ydist (inc (/ (float (c/world-viewport-height ctx)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

(extend-type cdq.gdx.Gdx
  g/LineOfSight
  ; does not take into account size of entity ...
  ; => assert bodies <1 width then
  (line-of-sight? [ctx source target]
    (and (or (not (:entity/player? source))
             (on-screen? ctx (:position target)))
         (not (and los-checks?
                   (g/ray-blocked? ctx
                                   (:position source)
                                   (:position target)))))))
