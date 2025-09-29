(ns gdl.math.raycaster
  (:import (gdl.math RayCaster)))

(defn blocked? [[arr width height] [start-x start-y] [target-x target-y]]
  (RayCaster/rayBlocked (double start-x)
                        (double start-y)
                        (double target-x)
                        (double target-y)
                        width
                        height
                        arr))
