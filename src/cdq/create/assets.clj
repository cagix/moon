(ns cdq.create.assets
  (:require [gdl.assets :as assets]))

(defn do! [{:keys [ctx/gdx
                   ctx/config]
            :as ctx}]
  (assoc ctx :ctx/assets (assets/create (:clojure.gdx/files gdx)
                                        (:assets config))))
