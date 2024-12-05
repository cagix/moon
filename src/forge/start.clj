(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.app :as app]))

(defn -main []
  (let [{:keys [requires] :as config} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (app/start config)))
