(ns cdq.entity.body
  (:require [cdq.body]
            [cdq.gdx.math.geom :as geom]
            [qrecord.core :as q]))

(q/defrecord Body [body/position
                   body/width
                   body/height
                   body/collides?
                   body/z-order
                   body/rotation-angle]
  cdq.body/Body
  (overlaps? [body other-body]
    (geom/overlaps? (geom/body->gdx-rectangle body)
                    (geom/body->gdx-rectangle other-body)))

  (touched-tiles [body]
    (geom/body->touched-tiles body)))

(defn create
  [{[x y] :position
    :keys [position
           width
           height
           collides?
           z-order
           rotation-angle]}
   {:keys [ctx/world]}]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? (:world/minimum-size world) 0)))
  (assert (>= height (if collides? (:world/minimum-size world) 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set (:world/z-orders world)) z-order))
  (assert (or (nil? rotation-angle)
              (<= 0 rotation-angle 360)))
  (map->Body
   {:position (mapv float position)
    :width  (float width)
    :height (float height)
    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))
