(ns gdl.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [gdl.application :as application])
  (:import (clojure.lang ILookup)))

(defn- load-config [path]
  (let [m (-> path
              io/resource
              slurp
              edn/read-string
              require-symbols)]
    (reify ILookup
      (valAt [_ k]
        (assert (contains? m k)
                (str "Config key not found: " k))
        (get m k)))))

(defn -main [config-path]
  (-> config-path
      load-config
      application/start!))
