(ns cdq.create.assets
  (:require [cdq.assets]
            [gdl.assets :as assets]))

(def ^:private -k :ctx/assets)

(defn do! [{:keys [ctx/config] :as ctx}]
  (extend (class ctx)
    cdq.assets/Assets
    {:sound        (fn [ctx path] (assets/sound        (-k ctx) path))
     :texture      (fn [ctx path] (assets/texture      (-k ctx) path))
     :all-sounds   (fn [ctx]      (assets/all-sounds   (-k ctx)))
     :all-textures (fn [ctx]      (assets/all-textures (-k ctx)))})
  (assoc ctx -k (assets/create (:assets config))))
