(ns clojure.scene2d.builds
  (:require [clojure.scene2d :as scene2d]))

(doseq [[k method-sym] '{:actor.type/menu-bar     clojure.scene2d.vis-ui.menu/create
                         :actor.type/select-box   clojure.gdx.vis-ui.widget.vis-select-box/create
                         :actor.type/text-field   clojure.scene2d.vis-ui.text-field/create
                         :actor.type/table        clojure.scene2d.build.table/create
                         :actor.type/image-button clojure.scene2d.vis-ui.image-button/create
                         :actor.type/text-button  clojure.scene2d.vis-ui.text-button/create
                         :actor.type/window       clojure.scene2d.vis-ui.window/create
                         :actor.type/image        clojure.scene2d.vis-ui.image/create}
        :let [method-var (requiring-resolve method-sym)]]
  (assert (keyword? k))
  (clojure.lang.MultiFn/.addMethod clojure.scene2d/build k method-var))
