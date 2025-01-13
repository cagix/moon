(ns clojure.application
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (com.badlogic.gdx.utils SharedLibraryLoader)))

(defn execute! [pipeline]
  (println "execute! " (count pipeline))
  (doseq [[qualified-symbol & params] pipeline]
    (println "~")
    (println qualified-symbol)
    (println params)
    (require (symbol (namespace qualified-symbol)))
    (let [f (resolve qualified-symbol)]
      (println f)
      (apply f params))))

(defn -main []
  (-> "clojure.application.edn"
      io/resource
      slurp
      edn/read-string
      execute!))

(defn dispatch-on-os [mapping]
  (execute!
   (get mapping (cond
                 (= SharedLibraryLoader/os com.badlogic.gdx.utils.Os/MacOsX)
                 :mac-osx))))
