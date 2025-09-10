(ns cdq.raycaster
  (:require [cdq.math.raycaster :as raycaster]
            [cdq.path-rays :as path-rays]))

(defn create [width height cells]
  (let [arr (make-array Boolean/TYPE width height)]
    (doseq [[[x y] blocks-vision?] cells]
      (aset arr x y (boolean blocks-vision?)))
    [arr width height]))

(defn blocked? [raycaster start end]
  (raycaster/blocked? raycaster start end))

(defn path-blocked? [raycaster start target path-w]
  (let [[start1,target1,start2,target2] (path-rays/create-double-ray-endpositions start target path-w)]
    (or
     (raycaster/blocked? raycaster start1 target1)
     (raycaster/blocked? raycaster start2 target2))))

(defn line-of-sight? [raycaster source target]
  (not (raycaster/blocked? raycaster
                           (:body/position (:entity/body source))
                           (:body/position (:entity/body target)))))
