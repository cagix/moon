(ns cdq.ctx.world
  (:require [cdq.math.raycaster :as raycaster]
            [cdq.math.path-rays :as path-rays]
            [clojure.gdx.utils.disposable :as disposable]))

(def active-eids :world/active-entities)

(defn dispose! [{:keys [world/tiled-map]}]
  (disposable/dispose! tiled-map))

(defn path-blocked? [{:keys [world/raycaster]} start target path-w]
  (let [[start1,target1,start2,target2] (path-rays/create-double-ray-endpositions start target path-w)]
    (or
     (raycaster/blocked? raycaster start1 target1)
     (raycaster/blocked? raycaster start2 target2))))

(defn line-of-sight? [{:keys [world/raycaster]} source target]
  (not (raycaster/blocked? raycaster
                           (:body/position (:entity/body source))
                           (:body/position (:entity/body target)))))
