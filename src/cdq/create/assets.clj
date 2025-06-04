(ns cdq.create.assets
  (:require [clojure.gdx :as gdx]))

(defn do!
  [{:keys [ctx/files]} [assets-to-load params]]
  (gdx/asset-manager (assets-to-load files params)))
