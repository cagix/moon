(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            clojure.gdx.scenes.scene2d.actor)
  (:gen-class))

(defn -main []
  (doseq [[f config] (-> "cdq.start.edn"
                         io/resource
                         slurp
                         edn/read-string)]
    ((requiring-resolve f) config)))

(doseq [[f k] '[
                [cdq.ui.property-editor/create            :actor.type/property-editor]
                [cdq.ui.data-viewer/create                :actor.type/data-viewer]]]
  (clojure.lang.MultiFn/.addMethod
   clojure.gdx.scenes.scene2d.actor/build
   k
   (requiring-resolve f)))

(require 'clojure.gdx.scenes.scene2d.ui)
