(ns cdq.math)

(defn circle->outer-rectangle [{[x y] :position :keys [radius]}]
  (let [radius (float radius)
        size (* radius 2)]
    {:left-bottom [(- (float x) radius)
                   (- (float y) radius)]
     :width  size
     :height size}))
