(ns cdq.create.assets
  (:require [gdl.assets :as assets]
            [cdq.g :as g]))

(def ^:private -k :ctx/assets)

(defn- extend-class [class]
  (extend class
    g/Assets
    {:sound        (fn [ctx path] (assets/sound        (-k ctx) path))
     :texture      (fn [ctx path] (assets/texture      (-k ctx) path))
     :all-sounds   (fn [ctx]      (assets/all-sounds   (-k ctx)))
     :all-textures (fn [ctx]      (assets/all-textures (-k ctx)))}))

(defn add-assets [ctx config]
  {:pre [(nil? (-k ctx))]}
  (extend-class (class ctx))
  (assoc ctx -k (assets/create (:assets config))))


