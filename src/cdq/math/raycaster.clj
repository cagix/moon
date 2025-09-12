(ns cdq.math.raycaster
  (:import (cdq.math RayCaster)))

(defn create
  "Creates the raycaster data structure, given the `width` and `height` of the two
  dimensional grid.

  `cells` refers to a collection of `[[x y] blocked?]`."
  [width height cells]
  (let [arr (make-array Boolean/TYPE width height)]
    (doseq [[[x y] blocked?] cells]
      (aset arr x y (boolean blocked?)))
    [arr width height]))

(defn blocked? [[arr width height] [start-x start-y] [target-x target-y]]
  (RayCaster/rayBlocked (double start-x)
                        (double start-y)
                        (double target-x)
                        (double target-y)
                        width
                        height
                        arr))
