(ns anvil.stage
  (:require [anvil.app :as app]
            [anvil.graphics :as g])
  (:refer-clojure :exclude [get]))

(defn get []
  (:stage (app/current-screen)))

(defn add-actor [actor]
  (.addActor (get) actor))

(defn reset [new-actors]
  (.clear (get))
  (run! add-actor new-actors))

(defn mouse-on-actor? []
  (let [[x y] (g/gui-mouse-position)]
    (.hit (get) x y true)))
