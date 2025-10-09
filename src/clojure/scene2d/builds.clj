(ns clojure.scene2d.builds
  (:require [clojure.scene2d :as scene2d]))

(doseq [[k method-sym] '{:actor.type/image-button clojure.scene2d.vis-ui.image-button/create
                         :actor.type/text-button  clojure.scene2d.vis-ui.text-button/create
                         :actor.type/image        clojure.scene2d.vis-ui.image/create}
        :let [method-var (requiring-resolve method-sym)]]
  (assert (keyword? k))
  (clojure.lang.MultiFn/.addMethod clojure.scene2d/build k method-var))
