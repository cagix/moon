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

(doseq [[f k] '[[clojure.vis-ui.select-box/create         :actor.type/select-box]
                [cdq.ui.label/create                      :actor.type/label]
                [cdq.ui.stack/create                      :actor.type/stack]
                [cdq.ui.text-field/create                 :actor.type/text-field]
                [cdq.ui.widget/create                     :actor.type/widget]
                [clojure.vis-ui.check-box/create          :actor.type/check-box]
                [cdq.ui.table/create                      :actor.type/table]
                [cdq.ui.image-button/create               :actor.type/image-button]
                [cdq.ui.text-button/create                :actor.type/text-button]
                [cdq.ui.property-editor/create            :actor.type/property-editor]
                [cdq.ui.data-viewer/create                :actor.type/data-viewer]]]
  (clojure.lang.MultiFn/.addMethod
   clojure.gdx.scenes.scene2d.actor/build
   k
   (requiring-resolve f)))
