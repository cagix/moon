(ns cdq.create.assets
  (:require [gdl.assets :as assets]
            [gdl.c :as c]))

(def ^:private -k :ctx/assets)

(defn add-assets [ctx config]
  {:pre [(nil? (-k ctx))]}
  (assoc ctx -k (assets/create (:assets config))))

(extend-type gdl.application.Context
  c/Assets
  (sound [ctx path]
    (assets/sound (-k ctx) path))

  (texture [ctx path]
    (assets/texture (-k ctx) path))

  (all-sounds [ctx]
    (assets/all-sounds (-k ctx)))

  (all-textures [ctx]
    (assets/all-textures (-k ctx))))
