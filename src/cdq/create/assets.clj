(ns cdq.create.assets
  (:require [gdl.assets :as assets]
            [cdq.g :as g]))

(def ^:private -k :ctx/assets)

(defn do! [{:keys [ctx/config] :as ctx}]
  (extend (class ctx)
    g/Assets
    {:sound        (fn [ctx path] (assets/sound        (-k ctx) path))
     :texture      (fn [ctx path] (assets/texture      (-k ctx) path))
     :all-sounds   (fn [ctx]      (assets/all-sounds   (-k ctx)))
     :all-textures (fn [ctx]      (assets/all-textures (-k ctx)))})
  (assoc ctx -k (assets/create (:assets config))))
