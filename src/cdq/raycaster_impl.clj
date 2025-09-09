(ns cdq.raycaster-impl
  (:require [cdq.grid.cell :as cell]
            [cdq.grid2d :as g2d]
            [cdq.math.raycaster :as raycaster]
            [cdq.raycaster]
            [cdq.path-rays :as path-rays]))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- create-raycaster-arr [grid]
  (let [width  (g2d/width  (.g2d grid))
        height (g2d/height (.g2d grid))
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells (.g2d grid))]
      (set-arr arr @cell cell/blocks-vision?))
    [arr width height]))

(defn create [grid]
  (let [arr (create-raycaster-arr grid)]
    (reify cdq.raycaster/Raycaster
      (blocked? [_ start end]
        (raycaster/blocked? arr start end))

      (path-blocked? [_ start target path-w]
        (let [[start1,target1,start2,target2] (path-rays/create-double-ray-endpositions start target path-w)]
          (or
           (raycaster/blocked? arr start1 target1)
           (raycaster/blocked? arr start2 target2))))

      (line-of-sight? [_ source target]
        (not (raycaster/blocked? arr
                                 (:body/position (:entity/body source))
                                 (:body/position (:entity/body target))))))))
