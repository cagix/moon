(ns cdq.create.assets
  (:require [clojure.gdx :as gdx]))

(defn do!
  [{:keys [ctx/files] :as ctx}
   [assets-to-load params]]
  (assoc ctx :ctx/assets (gdx/asset-manager (assets-to-load files params))))
