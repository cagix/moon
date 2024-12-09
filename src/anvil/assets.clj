(ns anvil.assets
  (:require [clojure.gdx.asset-manager :as manager]))

(defn load-all [assets]
  (doto (manager/create)
    (manager/load assets)))
