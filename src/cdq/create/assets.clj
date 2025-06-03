(ns cdq.create.assets
  (:require [clojure.gdx :as gdx]))

(defn do! [{:keys [ctx/config
                   ctx/files]
            :as ctx}]
  (let [[f params] (::get-assets-to-load config)
        assets-to-load (f files params)]
    (assoc ctx :ctx/assets (gdx/asset-manager assets-to-load))))
