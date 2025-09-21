(ns gdl.backends.desktop.main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.backends.desktop :as desktop])
  (:gen-class))

(defn -main [edn-resource]
  (-> edn-resource
      io/resource
      slurp
      edn/read-string
      (update :listener update-vals (fn [sym]
                                      (let [avar (requiring-resolve sym)]
                                        (assert avar sym)
                                        avar)))
      desktop/application))
